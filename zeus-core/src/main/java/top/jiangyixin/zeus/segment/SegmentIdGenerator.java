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
    private final static long ID_CACHE_BIZ_TYPE_NOT_FOUND = -3;
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
    private final Map<String, SegmentBuffer> cache = new ConcurrentHashMap<>();
    private volatile boolean initOk = false;
    private IdAllocDAO idAllocDAO;
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
        logger.info("start update {} segment from db ...", bizType);
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
}
