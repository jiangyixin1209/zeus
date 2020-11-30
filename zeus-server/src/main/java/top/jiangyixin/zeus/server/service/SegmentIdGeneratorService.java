package top.jiangyixin.zeus.server.service;

import com.alibaba.druid.pool.DruidDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import top.jiangyixin.zeus.core.IdGenerator;
import top.jiangyixin.zeus.core.ZeroIdGenerator;
import top.jiangyixin.zeus.core.common.Result;
import top.jiangyixin.zeus.core.common.ZeusPropertiesFactory;
import top.jiangyixin.zeus.core.exception.ZeusException;
import top.jiangyixin.zeus.core.segment.SegmentIdGenerator;
import top.jiangyixin.zeus.core.segment.dao.IdAllocDAO;
import top.jiangyixin.zeus.core.segment.dao.impl.IdAllocDAOImpl;
import top.jiangyixin.zeus.server.Constants;
import top.jiangyixin.zeus.server.exception.InitException;

import java.util.Properties;

/**
 * @author jiangyixin
 */
@Service
public class SegmentIdGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(SegmentIdGeneratorService.class);

    private final IdGenerator idGenerator;

    public SegmentIdGeneratorService() {
        Properties properties = ZeusPropertiesFactory.getProperties();
        boolean enable = Boolean.parseBoolean(properties.getProperty(Constants.ZEUS_SEGMENT_ENABLE, "true"));
        if (enable) {
            DruidDataSource druidDataSource = new DruidDataSource();
            druidDataSource.setUrl(properties.getProperty(Constants.ZEUS_JDBC_URL));
            druidDataSource.setUsername(properties.getProperty(Constants.ZEUS_JDBC_USERNAME));
            druidDataSource.setPassword(properties.getProperty(Constants.ZEUS_JDBC_PASSWORD));

            IdAllocDAO idAllocDAO = new IdAllocDAOImpl(druidDataSource);
            idGenerator = new SegmentIdGenerator();
            ((SegmentIdGenerator) idGenerator).setIdAllocDAO(idAllocDAO);
            if (idGenerator.init()) {
                logger.info("Segment Id Service Init Successfully");
            } else {
                throw new ZeusException("Segment Id Service Init Fail");
            }
        } else {
            idGenerator = new ZeroIdGenerator();
            logger.info("Zero Id Service Init Successfully");
        }
    }

    public SegmentIdGenerator getIdGenerator() {
        if (idGenerator instanceof SegmentIdGenerator) {
           return (SegmentIdGenerator) idGenerator;
        }
        return null;
    }

    public Result<?> getId(String bizType) {
        return idGenerator.nextId(bizType);
    }

}
