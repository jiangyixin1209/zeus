package top.jiangyixin.zeus.core.segment.dao;

import org.apache.ibatis.annotations.Param;
import top.jiangyixin.zeus.core.segment.model.IdAlloc;

import java.util.List;

/**
 * @author mickle
 */
public interface IdAllocDAO {

    /**
     * 获取所有激活的 IdAlloc
     * @return  IdAlloc 列表
     */
    List<IdAlloc> selectAllIdAlloc();

    /**
     * 获取所有激活的 biz_type
     * @return  bizType 列表
     */
    List<String> selectAllBizType();

    /**
     * 根据设置步长更新 max_id
     * @param bizType   业务类型
     * @return          IdAlloc
     */
    IdAlloc updateMaxIdAndGetIdAlloc(String bizType);

    /**
     * 根据业务获取对应的idAlloc
     * @param bizType   业务类型
     * @return          IdAlloc
     */
    IdAlloc selectIdAllocByBizType(@Param("bizType") String bizType);

    /**
     * 根据设置步长更新 max_id
     * @param idAlloc   idAlloc 对象
     * @return          idAlloc 对象
     */
    IdAlloc updateMaxIdByStep(@Param("idAlloc") IdAlloc idAlloc);
}
