package top.jiangyixin.zeus.segment.dao.impl;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import top.jiangyixin.zeus.segment.dao.IdAllocDAO;
import top.jiangyixin.zeus.segment.mapper.IdAllocMapper;
import top.jiangyixin.zeus.segment.model.IdAlloc;

import javax.sql.DataSource;
import java.util.List;

/**
 * @author jiangyixin
 */
public class IdAllocDAOImpl implements IdAllocDAO {
    private final SqlSessionFactory sqlSessionFactory;

    public IdAllocDAOImpl(DataSource dataSource) {
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("dev", transactionFactory, dataSource);
        Configuration configuration = new Configuration();
        // 设置自动驼峰转换
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.addMapper(IdAllocMapper.class);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @Override
    public List<IdAlloc> selectAllIdAlloc() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()) {
            IdAllocMapper idAllocMapper = sqlSession.getMapper(IdAllocMapper.class);
            return idAllocMapper.selectAllIdAlloc();
        }
    }

    @Override
    public List<String> selectAllBizType() {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()){
            IdAllocMapper idAllocMapper = sqlSession.getMapper(IdAllocMapper.class);
            return idAllocMapper.selectAllBizType();
        }
    }

    @Override
    public IdAlloc selectIdAllocByBizType(String bizType) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()){
            IdAllocMapper idAllocMapper = sqlSession.getMapper(IdAllocMapper.class);
            return idAllocMapper.selectIdAllocByBizType(bizType);
        }
    }

    @Override
    public IdAlloc updateMaxIdAndGetIdAlloc(String bizType) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()){
            IdAllocMapper idAllocMapper = sqlSession.getMapper(IdAllocMapper.class);
            idAllocMapper.updateMaxId(bizType);
            return idAllocMapper.selectIdAllocByBizType(bizType);
        }
    }

    @Override
    public IdAlloc updateMaxIdByStep(IdAlloc idAlloc) {
        try (SqlSession sqlSession = sqlSessionFactory.openSession()){
            IdAllocMapper idAllocMapper = sqlSession.getMapper(IdAllocMapper.class);
            idAllocMapper.updateMaxIdByStep(idAlloc);
            return idAllocMapper.selectIdAllocByBizType(idAlloc.getBizType());
        }
    }
}
