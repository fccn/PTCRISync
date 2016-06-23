package pt.ptcris;

import java.math.BigInteger;

public abstract class PTCRISyncException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private int code;
	private String message;
	private BigInteger dev_code;
	private String dev_message;

	public PTCRISyncException(int code, String message, BigInteger dev_code, String dev_message) {
		super(code + " - " + message);
		this.code = code;
		this.message = message;
		this.dev_code = dev_code;
		this.dev_message = dev_message;
	}

}
