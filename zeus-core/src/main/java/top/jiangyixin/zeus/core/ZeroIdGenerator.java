package top.jiangyixin.zeus.core;

import top.jiangyixin.zeus.core.common.Result;

/**
 * @author jianyixin
 */
public class ZeroIdGenerator implements IdGenerator {
    @Override
    public boolean init() {
        return true;
    }

    @Override
    public Result<Long> nextId(String bizType) {
        return new Result<>(0L, true);
    }
}
