package top.jiangyixin.zeus.core.common;

/**
 * @author jiangyixin
 */
public class Result<T> {
	private T data;
	private boolean status;
	private String message;

	public Result() {
	}

	public Result(T data, boolean status) {
		this.data = data;
		this.status = status;
	}

	public Result(T data, boolean status, String message) {
		this(data, status);
		this.message = message;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	public boolean isStatus() {
		return status;
	}

	public void setStatus(boolean status) {
		this.status = status;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
