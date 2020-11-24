package top.jiangyixin.zeus.segment;

import top.jiangyixin.zeus.IdGenerator;


/**
 * Id生成器实现类
 * Segment 双buffer
 * @author jiangyixin
 */
public class SegmentIdGenerator implements IdGenerator {

    @Override
    public boolean init() {
        return false;
    }

    @Override
    public Long nextId(String bizType) {
        return null;
    }
}
