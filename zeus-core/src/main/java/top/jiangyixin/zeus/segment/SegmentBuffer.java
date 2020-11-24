package top.jiangyixin.zeus.segment;

/**
 * SegmentBuffer
 * @author jiangyixin
 */
public class SegmentBuffer {
    private String bizType;
    private final Segment[] segments;
    private volatile int currentPos;
    private volatile boolean nextReady;
    private volatile boolean isInit;

    public SegmentBuffer() {
        segments = new Segment[]{new Segment(this), new Segment(this)};
    }


    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public Segment[] getSegments() {
        return segments;
    }

    public int getCurrentPos() {
        return currentPos;
    }

    public void setCurrentPos(int currentPos) {
        this.currentPos = currentPos;
    }

    public boolean isNextReady() {
        return nextReady;
    }

    public void setNextReady(boolean nextReady) {
        this.nextReady = nextReady;
    }

    public boolean isInit() {
        return isInit;
    }

    public void setInit(boolean init) {
        isInit = init;
    }
}
