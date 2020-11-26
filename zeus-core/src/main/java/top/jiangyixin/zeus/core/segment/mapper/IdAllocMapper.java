package top.jiangyixin.zeus.core.segment.mapper;

import org.apache.ibatis.annotations.*;
import top.jiangyixin.zeus.core.segment.model.IdAlloc;

import java.util.List;

/**
 * @author jiangyixin
 */
public interface IdAllocMapper {

    /**
     * 获取所有激活的 IdAlloc
     * @return      IdAlloc 列表
     */
    @Select("SELECT `biz_type`,`max_id`,`step`,`description`,`gmt_create`,`gmt_modified` " +
      "FROM `zeus_id_alloc` WHERE `active` = 1;")
    List<IdAlloc> selectAllIdAlloc();

    /**
     * 获取所有激活的 biz_type
     * @return      IdAlloc 列表
     */
    @Select("SELECT `biz_type` FROM  `zeus_id_alloc` WHERE `active` = 1;")
    List<String> selectAllBizType();

    /**
     * 根据业务获取对应的idAlloc
     * @param bizType   业务类型
     * @return          IdAlloc
     */
    @Select("SELECT `biz_type`,`max_id`,`step`,`description`,`gmt_create`,`gmt_modified` " +
      "FROM `zeus_id_alloc` WHERE `biz_type` = #{bizType};")
    IdAlloc selectIdAllocByBizType(@Param("bizType") String bizType);

    /**
     * 根据设置步长更新 max_id
     * @param bizType   业务类型
     */
    @Update("UPDATE `zeus_id_alloc` SET `max_id` = `max_id` + `step` WHERE `biz_type` = #{bizType};")
    void updateMaxId(@Param("bizType") String bizType);

    /**
     * 根据设置步长更新 max_id
     * @param idAlloc   idAlloc 对象
     */
    @Update("UPDATE `zeus_id_alloc` SET `max_id` = `max_id` + #{idAlloc.step}  WHERE `biz_type` = #{idAlloc.bizType};")
    void updateMaxIdByStep(@Param("idAlloc") IdAlloc idAlloc);
}
