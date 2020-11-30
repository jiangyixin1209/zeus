package top.jiangyixin.zeus.core;

import top.jiangyixin.zeus.core.common.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * Id 生成器接口
 * @author jiangyixin
 */
public interface IdGenerator {

    /**
     * Id 生成器初始化
     * @return        是否初始化成功
     */
    boolean init();

    /**
     * 根据业务获取最新的id
     * @param bizType       业务名称
     * @return              id
     */
    Result<Long> nextId(String bizType);

    /**
     * 根据业务获取多条id
     * @param bizType       业务名称
     * @param batchSize     获取条数
     * @return              id 列表
     */
    default Result<List<Long>> nextId(String bizType, Integer batchSize) {
        List<Long> idList = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            Result<Long> result = nextId(bizType);
            if (result.isStatus()) {
                idList.add(result.getData());
            }
        }
        return new Result<>(idList, true);
    }

}
