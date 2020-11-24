package top.jiangyixin.zeus;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jianyixin
 */
public class ZeroIdGenerator implements IdGenerator {
    @Override
    public boolean init() {
        return true;
    }

    @Override
    public Long nextId(String bizType) {
        return 0L;
    }

    @Override
    public List<Long> nextId(String bizType, Integer batchSize) {
      return new ArrayList<Long>(){{add(0L);}};
    }
}
