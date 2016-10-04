package pt.ptcris;

public class PTCRISyncResult {
	Integer code;
	Exception exception;
	
	public PTCRISyncResult (Integer code) {
		this.setCode(code);
	}
	
	public PTCRISyncResult (Integer code, Exception exception) {
		this.setCode(code);
		this.setException(exception);
	}	
	
	public void setCode (Integer code) {
		this.code = code;
	}	
	
	public void setException (Exception exception) {
		this.exception = exception;
	}

	
	public Integer getCode () {
		return this.code;
	}	
	
	public Exception getException () {
		return this.exception;
	}	
	
}
