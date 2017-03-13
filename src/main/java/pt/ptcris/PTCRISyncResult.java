package pt.ptcris;

import java.math.BigInteger;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;

import pt.ptcris.exceptions.InvalidWorkException;

/**
 * A class that encapsulates the result of the synchronization procedures for a
 * particular work. Currently only the
 * {@link PTCRISync#export(ORCIDClient, java.util.List, pt.ptcris.handlers.ProgressHandler)
 * export} methods report these results.
 */
public final class PTCRISyncResult {

	public static final int ADDOK = -5;
	public static final int UPDATEOK = -10;
	public static final int DELETEOK = -15;
	public static final int UPTODATE = -20;
	public static final int INVALID = -30;
	public static final int CLIENTERROR = -40;

	/**
	 * Creates a successful "add" message with the assigned put-code.
	 */
	public static PTCRISyncResult ok(BigInteger putcode) {
		return new PTCRISyncResult(ADDOK, putcode);
	}
	
	/**
	 * Creates a successful "update" message.
	 */
	public static final PTCRISyncResult OK_UPD_RESULT = new PTCRISyncResult(UPDATEOK);

	/**
	 * Creates a successful "delete" message.
	 */
	public static final PTCRISyncResult OK_DEL_RESULT = new PTCRISyncResult(DELETEOK);

	/**
	 * Creates an "already up-to-date" message.
	 */
	public static final PTCRISyncResult UPTODATE_RESULT = new PTCRISyncResult(UPTODATE);

	/**
	 * Creates message reporting a failure at the ORCID API level.
	 *
	 * @param exception
	 *            the ORCID client exception
	 * @return the resulting message
	 */
	public static PTCRISyncResult fail(OrcidClientException exception) {
		return new PTCRISyncResult(CLIENTERROR, exception);
	}

	/**
	 * Creates message reporting an invalid work.
	 *
	 * @param exception
	 *            the reasons for invalidity
	 * @return the resulting message
	 */
	public static PTCRISyncResult invalid(InvalidWorkException exception) {
		return new PTCRISyncResult(INVALID, exception);
	}

	public final int code;
	public final Exception exception;
	public final BigInteger putcode;

	/**
	 * Constructs a PTCRISync result with a result code, possible exception and
	 * possible assigned put-code.
	 *
	 * @param code
	 *            the code that defines the outcome
	 * @param exception
	 *            the exception containing additional information if
	 *            unsuccessful
	 * @param putcode
	 *            the assigned put-code, if successful add
	 */
	private PTCRISyncResult(int code, Exception exception, BigInteger putcode) {
		this.code = code;
		this.exception = exception;
		this.putcode = putcode;
	}
	
	/**
	 * Constructs a PTCRISync result with a result code and an assigned
	 * put-code, used for additions.
	 *
	 * @param code
	 *            the code that defines the outcome
	 * @param putcode
	 *            the assigned put-code, if successful add
	 */
	private PTCRISyncResult(int code, BigInteger putcode) {
		this(code, null, putcode);
	}

	/**
	 * Constructs a PTCRISync result with a result code and an exception, used
	 * for unsuccessful results.
	 *
	 * @param code
	 *            the code that defines the outcome
	 * @param exception
	 *            the exception containing additional information if
	 *            unsuccessful
	 */
	private PTCRISyncResult(int code, Exception exception) {
		this(code, exception, null);
	}

	/**
	 * Constructs a PTCRISync result with a success code.
	 *
	 * @param code
	 *            the code that defines the outcome
	 */
	private PTCRISyncResult(int code) {
		this(code, null, null);
	}

}
