package pt.ptcris;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;

import pt.ptcris.exceptions.InvalidWorkException;

/**
 * A class that encapsulates the result of the synchronization procedures for a
 * particular work. Currently only the
 * {@link PTCRISync#export(ORCIDClient, java.util.List, pt.ptcris.handlers.ProgressHandler)
 * export} methods report these results.
 */
public final class PTCRISyncResult {

	public static final int CLIENTERROR = 500;
	public static final int UPTODATE = 304;
	public static final int UPDATEOK = 200;
	public static final int ADDOK = 200;
	public static final int INVALID = -11;

	/**
	 * Creates a successful "add" message.
	 */
	public static final PTCRISyncResult OK_ADD_RESULT = new PTCRISyncResult(ADDOK);

	/**
	 * Creates a successful "update" message.
	 */
	public static final PTCRISyncResult OK_UPD_RESULT = new PTCRISyncResult(UPDATEOK);

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

	/**
	 * Constructs a PTCRISync result with a code and exception.
	 *
	 * @param code
	 *            the code that defines the outcome
	 * @param exception
	 *            the exception containing additional information if
	 *            unsuccessful
	 */
	private PTCRISyncResult(int code, Exception exception) {
		this.code = code;
		this.exception = exception;
	}

	/**
	 * Constructs a PTCRISync result with a sucess code (i.e., without an
	 * exception).
	 *
	 * @param code
	 *            the code that defines the outcome
	 */
	private PTCRISyncResult(int code) {
		this(code, null);
	}

}
