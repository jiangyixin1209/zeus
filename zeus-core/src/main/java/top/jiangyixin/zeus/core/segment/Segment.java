package top.jiangyixin.zeus.core.segment;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Segment
 * @author jiangyixin
 */
public class Segment {
    private AtomicLong value = new AtomicLong(0);
    private volatile long max;
    private volatile int step;
    private final SegmentBuffer segmentBuffer;

    public Segment(SegmentBuffer buffer) {
        this.segmentBuffer = buffer;
    }

    /**
     * 获取剩余容量
     * @return      剩余容量（最大id - 当前id）
     */
    public Long getIdle() {
        return this.getMax() - this.value.get();
    }

    public AtomicLong getValue() {
      return value;
    }

    public void setValue(AtomicLong value) {
      this.value = value;
    }

    public long getMax() {
      return max;
    }

    public void setMax(long max) {
      this.max = max;
    }

    public int getStep() {
      return step;
    }

    public void setStep(int step) {
      this.step = step;
    }

    public SegmentBuffer getSegmentBuffer() {
      return segmentBuffer;
    }

    @Override
    public String toString() {
        return "Segment{" +
            "value=" + value +
            ", max=" + max +
            ", step=" + step +
            ", segmentBuffer=" + segmentBuffer +
            '}';
    }
}
