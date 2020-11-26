package top.jiangyixin.zeus.segment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.jiangyixin.zeus.IdGenerator;
import top.jiangyixin.zeus.segment.dao.IdAllocDAO;
import top.jiangyixin.zeus.segment.model.IdAlloc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Id生成器实现类
 * Segment 双buffer
 * @author jiangyixin
 */
public class SegmentIdGenerator implements IdGenerator {
    private final static Logger logger = LoggerFactory.getLogger(SegmentIdGenerator.class);
    /**
     * ID Cache未初始化成功异常码
     */
    private final static long ID_CACHE_INIT_ERROR = -1;
    /**
     * 业务未在ID Cache中异常码
     */
    private final static long ID_CACHE_BIZ_TYPE_NOT_FOUND = -2;
    /**
     * SegmentBuffer中的两个Segment均不可用异常码
     */
    private static final long ID_TWO_SEGMENTS_NOT_AVAILABLE = -3;
    /**
     * 默认一个Segment维持时间为15分钟（在15分钟内，其中id号段会被消耗完毕）
     */
    private static final long SEGMENT_DURATION = 15 * 60 * 1000L;
    /**
     * 最大步长不超过100,0000（一次获取id号段不超过10W）
     */
    private static final int MAX_STEP = 1000000;
    /**
     * 扩容缩容因子
     */
    private static final int GROWTH_FACTOR = 2;
    /**
     * 剩余容量
     */
    private static final double REMAIN_CAPACITY = 0.8;
    private final Map<String, SegmentBuffer> cache = new ConcurrentHashMap<>();
    private final ExecutorService executorService = new ThreadPoolExecutor(5, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new SegmentUpdateThreadFactory());
    private volatile boolean initOk = false;
    private IdAllocDAO idAllocDAO;

    /**
     * 自定义线程工厂
     */
    private static class SegmentUpdateThreadFactory implements ThreadFactory {

        private static int threadNumber = 0;

        private static synchronized int nextThreadNumber() {
            return threadNumber++;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "Update-Segment-Thread-" + nextThreadNumber());
        }
    }

    @Override
    public boolean init() {
        logger.info("start init segmentIdGenerator ...");
        updateCacheFromDb();
        initOk = true;
        updateCacheFromDbByEveryMinute();
        return initOk;
    }

    @Override
    public Long nextId(String bizType) {
        if (!initOk) {
            return ID_CACHE_INIT_ERROR;
        }
        if (cache.containsKey(bizType)) {
            SegmentBuffer segmentBuffer = cache.get(bizType);
            // 如果是第一次请求则需要进行 segment 初始化
            if (!segmentBuffer.isInitOk()) {
                synchronized (segmentBuffer) {
                    if (!segmentBuffer.isInitOk()) {
                        try {
                            updateSegmentFromDb(bizType, segmentBuffer.getCurrentSegment());
                            segmentBuffer.setInitOk(true);
                        } catch (Exception e) {
                            logger.error("update {} segment from db error: {}", bizType, e);
                        }
                    }
                }
            }
            return getIdFromSegmentBuffer(segmentBuffer);
        }
        return ID_CACHE_BIZ_TYPE_NOT_FOUND;
    }

    /**
     * 加载数据库中激活的业务类别至内存cache中
     */
    public void updateCacheFromDb() {
        logger.info("start update cache from db ...");
        try {
            List<String> dbBizTypes = idAllocDAO.selectAllBizType();
            if (dbBizTypes == null || dbBizTypes.isEmpty()) {
                return;
            }
            for (String dbBizType : dbBizTypes) {
                if (!cache.containsKey(dbBizType)) {
                    SegmentBuffer segmentBuffer = new SegmentBuffer();
                    segmentBuffer.setBizType(dbBizType);
                    Segment currentSegment = segmentBuffer.getCurrentSegment();
                    currentSegment.setValue(new AtomicLong(0));
                    currentSegment.setStep(0);
                    currentSegment.setMax(0);
                    cache.put(dbBizType, segmentBuffer);
                    logger.info("Add bizType {} from db to cache, SegmentBuffer {}", dbBizType, segmentBuffer);
                }
            }
            List<String> cacheBizTypes = new ArrayList<>(cache.keySet());
            for (String cacheBizType : cacheBizTypes) {
                if (!dbBizTypes.contains(cacheBizType)) {
                    cache.remove(cacheBizType);
                    logger.info("Remove bizType {} from cache", cacheBizType);
                }
            }
        } catch (Exception e) {
            logger.error("update cache from db exception", e);
        }
    }

    /**
     * 每分钟定时刷新cache
     */
    private void updateCacheFromDbByEveryMinute() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setName("update-cache-thread");
            thread.setDaemon(true);
            return thread;
        });
        service.scheduleWithFixedDelay(this::updateCacheFromDb, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 往Segment中加载id段
     *
     * 如果SegmentBuffer未初始化则优先初始化
     * 如果已初始化完毕则进行 step 动态增长减少
     * @param bizType     业务类别
     * @param segment     segment
     */
    public void updateSegmentFromDb(String bizType, Segment segment) {
        logger.info("start update {} segment from db, update segment {}", bizType, segment);
        SegmentBuffer segmentBuffer = segment.getSegmentBuffer();
        IdAlloc idAlloc;
        if (!segmentBuffer.isInitOk()) {
            // 如果SegmentBuffer未初始化则初始化
            idAlloc = idAllocDAO.updateMaxIdAndGetIdAlloc(bizType);
            segmentBuffer.setStep(idAlloc.getStep());
            segmentBuffer.setMinStep(idAlloc.getStep());
        } else if (segmentBuffer.getUpdateTimestamp() == 0){
            // 如果 segmentBuffer 上次更新时间为0则初始化为当前时间
            idAlloc = idAllocDAO.updateMaxIdAndGetIdAlloc(bizType);
            segmentBuffer.setStep(idAlloc.getStep());
            segmentBuffer.setMinStep(idAlloc.getStep());
            segmentBuffer.setUpdateTimestamp(System.currentTimeMillis());
        } else {
            // 根据 id 号段消耗速率动态增大或减少 step
            long duration = System.currentTimeMillis() - segmentBuffer.getUpdateTimestamp();
            int nextStep = segmentBuffer.getStep();

            if (duration < SEGMENT_DURATION && nextStep * GROWTH_FACTOR < MAX_STEP) {
                nextStep = nextStep * GROWTH_FACTOR;
            } else if (duration >= SEGMENT_DURATION * GROWTH_FACTOR){
                nextStep = nextStep / GROWTH_FACTOR >= segmentBuffer.getMinStep()
                  ? nextStep / GROWTH_FACTOR : nextStep;
            }
            logger.info("bizType[{}], step[{}], duration[{} min], nextStep[{}]",
              bizType, segmentBuffer.getStep(),
              String.format("%.2f",((double)duration / (1000 * 60))),
              nextStep
            );

            IdAlloc tmpIdAlloc = new IdAlloc();
            tmpIdAlloc.setBizType(bizType);
            tmpIdAlloc.setStep(nextStep);
            idAlloc = idAllocDAO.updateMaxIdByStep(tmpIdAlloc);
            segmentBuffer.setUpdateTimestamp(System.currentTimeMillis());
            segmentBuffer.setStep(nextStep);
            segmentBuffer.setMinStep(idAlloc.getStep());
        }
        long value = idAlloc.getMaxId() - segment.getStep();
        segment.setValue(new AtomicLong(value));
        segment.setMax(idAlloc.getMaxId ());
        segment.setStep(segmentBuffer.getStep());
    }

    public Long getIdFromSegmentBuffer(SegmentBuffer segmentBuffer) {
        while (true) {
            segmentBuffer.rLock().lock();
            try {
                Segment currentSegment = segmentBuffer.getCurrentSegment();
                // 判断是否需要预先加载下一个segment
                if (!segmentBuffer.isNextReady()
                    && (currentSegment.getIdle() < REMAIN_CAPACITY * currentSegment.getStep())
                    && segmentBuffer.getThreadRunning().compareAndSet(false, true)) {

                    executorService.execute(() -> {
                        Segment nextSegment = segmentBuffer.getSegments()[segmentBuffer.nextPos()];
                        boolean isUpdate = false;
                        try {
                            // 加载下一个segment
                            updateSegmentFromDb(segmentBuffer.getBizType(), nextSegment);
                            isUpdate = true;
                        } catch (Exception e) {
                            logger.error("update {} segment from db error: {}", segmentBuffer.getBizType(), e);
                        } finally {
                            if (isUpdate) {
                                segmentBuffer.wLock().lock();
                                segmentBuffer.setNextReady(true);
                                segmentBuffer.getThreadRunning().set(false);
                                segmentBuffer.wLock().unlock();
                            } else {
                                segmentBuffer.getThreadRunning().set(false);
                            }
                        }
                    });
                }
                // 如果下一个获取的Id在当前segment中，则直接返回
                long value = currentSegment.getValue().incrementAndGet();
                if (value < currentSegment.getMax()) {
                    return value;
                }
            } finally {
                segmentBuffer.rLock().unlock();
            }
            // 如果下一个获取的Id在当前不在segment中
            waitAndSleep(segmentBuffer);
            segmentBuffer.wLock().lock();
            try {
                Segment currentSegment = segmentBuffer.getCurrentSegment();
                long value = currentSegment.getValue().get();
                if (value < currentSegment.getMax()) {
                    return value;
                }
                // 切换下一个segment
                if (segmentBuffer.isNextReady()) {
                    segmentBuffer.switchPos();
                    segmentBuffer.setNextReady(false);
                } else {
                    logger.error("Both two segments in {} are not ready!", segmentBuffer);
                    return ID_TWO_SEGMENTS_NOT_AVAILABLE;
                }
            } finally {
                segmentBuffer.wLock().unlock();
            }
        }
    }

    /**
     * 等待 segmentBuffer 加载下一个 Segment 线程执行完毕
     * @param segmentBuffer     segmentBuffer
     */
    private void waitAndSleep(SegmentBuffer segmentBuffer) {
        int roll = 0;
        while (segmentBuffer.getThreadRunning().get()) {
            roll += 1;
            if (roll < 10) {
                try {
                    TimeUnit.MILLISECONDS.sleep(2);
                } catch (InterruptedException e) {
                    logger.warn("Thread {} InterruptedException", Thread.currentThread().getName());
                    break;
                }
            } else {
                break;
            }
        }
    }
}
