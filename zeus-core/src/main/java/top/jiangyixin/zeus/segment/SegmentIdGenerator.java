package top.jiangyixin.zeus.segment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.jiangyixin.zeus.IdGenerator;
import top.jiangyixin.zeus.segment.dao.IdAllocDAO;

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
        return null;
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
                    logger.info("Add bizType {} from db to cache, SegmentBufffer {}", dbBizType, segmentBuffer);
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
}
