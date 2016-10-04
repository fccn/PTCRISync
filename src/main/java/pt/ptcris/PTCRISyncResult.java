package pt.ptcris;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;

import pt.ptcris.exceptions.InvalidWorkException;

public final class PTCRISyncResult {

	public final int code;
	public final Exception exception;
	
	private PTCRISyncResult(int code, Exception exception) {
		this.code = code;
		this.exception = exception;
	}
	
	private PTCRISyncResult(int code) {
		this(code, null);
	}

	public static final int CLIENTERROR = 500;
	public static final int UPTODATE = 304;
	public static final int UPDATEOK = 200;
	public static final int ADDOK = 200;
	public static final int INVALID = -11;
	public static final int CONFLICT = 409;

	public static final PTCRISyncResult OK_ADD_RESULT = new PTCRISyncResult(ADDOK);
	public static final PTCRISyncResult OK_UPD_RESULT = new PTCRISyncResult(UPDATEOK);
	public static final PTCRISyncResult UPTODATE_RESULT = new PTCRISyncResult(UPTODATE);

	public static PTCRISyncResult fail(OrcidClientException exception) {
		return new PTCRISyncResult(exception.getCode(), exception);
	}
	
	public static PTCRISyncResult invalid(InvalidWorkException exception) {
		return new PTCRISyncResult(INVALID, exception);
	}
	
}
