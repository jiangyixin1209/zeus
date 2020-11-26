package top.jiangyixin.zeus.server.vo;

/**
 * @author jiangyixin
 */
public class Result {

    private long id;
    private boolean status;

    public Result() {
    }

    public Result(long id, boolean status) {
        this.id = id;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
