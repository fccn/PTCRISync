/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris;

import java.math.BigInteger;

import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.common.ElementSummary;

import pt.ptcris.exceptions.InvalidActivityException;

/**
 * A class that encapsulates the result of the synchronization procedures for a
 * particular activity. Currently only the
 * {@link PTCRISync#export(ORCIDClient, java.util.List, pt.ptcris.handlers.ProgressHandler)
 * export} methods report these results.
 */
public final class PTCRISyncResult<E extends ElementSummary> {

	public static final int GETOK = -1;
	public static final int ADDOK = -5;
	public static final int UPDATEOK = -10;
	public static final int DELETEOK = -15;
	public static final int UPTODATE = -20;
	public static final int INVALID = -30;
	public static final int CLIENTERROR = -40;
	
	/**
	 * Creates a successful "get" message with the assigned put-code.
	 * 
	 * @param <E> the activity type
	 * @param putcode the put-code of the get request
	 * @param element the retrieved activity
	 * @return an OK get message
	 */
	public static <E extends ElementSummary> PTCRISyncResult<E> ok_get(BigInteger putcode, E element) {
		return new PTCRISyncResult<E>(GETOK, element);
	}
	
	/**
	 * Creates a successful "add" message with the assigned put-code.
	 * 
	 * @param <E> the activity type
	 * @param putcode the put-code of the newly added activity
	 * @return an OK add message
	 */
	public static <E extends ElementSummary> PTCRISyncResult<E> ok_add(BigInteger putcode) {
		return new PTCRISyncResult<E>(ADDOK, putcode);
	}

	/**
	 * Creates a successful "update" message.
	 * 
	 * @param <E> the activity type
	 * @return an OK update message
	 */
	public static <E extends ElementSummary> PTCRISyncResult<E> ok_upd() {
		return new PTCRISyncResult<E>(UPDATEOK);
	}

	/**
	 * Creates a successful "delete" message.
	 * 
	 * @param <E> the activity type
	 * @return an OK delete message
	 */
	public static <E extends ElementSummary> PTCRISyncResult<E> ok_del() {
		return new PTCRISyncResult<E>(DELETEOK);
	}

	/**
	 * Creates an "already up-to-date" message.
	 * 
	 * @param <E> the activity type
	 * @return an already up to date message
	 */
	public static <E extends ElementSummary> PTCRISyncResult<E> uptodate() {
		return new PTCRISyncResult<E>(UPTODATE);
	}

	/**
	 * Creates message reporting a failure at the ORCID API level.
	 *
	 * @param <E> the activity type
	 * @param exception
	 *            the ORCID client exception
	 * @return the resulting message
	 */
	public static <E extends ElementSummary> PTCRISyncResult<E> fail(OrcidClientException exception) {
		return new PTCRISyncResult<E>(CLIENTERROR, exception);
	}

	/**
	 * Creates message reporting an invalid activity.
	 *
	 * @param <E> the activity type
	 * @param exception
	 *            the reasons for invalidity
	 * @return the resulting message
	 */
	public static <E extends ElementSummary> PTCRISyncResult<E> invalid(InvalidActivityException exception) {
		return new PTCRISyncResult<E>(INVALID, exception);
	}

	public final int code;
	public final Exception exception;
	public final BigInteger putcode;
	public final E act;

	/**
	 * Constructs a PTCRISync result with a result code, possible exception,
	 * possible assigned put-code and possible read activity.
	 *
	 * @param code
	 *            the code that defines the outcome
	 * @param exception
	 *            the exception containing additional information if
	 *            unsuccessful (may be null)
	 * @param putcode
	 *            the assigned put-code, if successful add (may be null)
	 * @param act
	 *            a read activity (may be null)
	 */
	private PTCRISyncResult(int code, Exception exception, BigInteger putcode,
			E act) {
		this.code = code;
		this.exception = exception;
		this.putcode = putcode;
		this.act = act;
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
		this(code, null, putcode, null);
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
		this(code, exception, null, null);
	}

	/**
	 * Constructs a PTCRISync result with a success code.
	 *
	 * @param code
	 *            the code that defines the outcome
	 */
	private PTCRISyncResult(int code) {
		this(code, null, null, null);
	}
	
	/**
	 * Constructs a PTCRISync result with a result code and an activity,, used
	 * for readings.
	 *
	 * @param code
	 *            the code that defines the outcome
	 * @param activity
	 *            the returned activity, if successfull read
	 */
	private PTCRISyncResult(int code, E act) {
		this(code, null, null, act);
	}

}
