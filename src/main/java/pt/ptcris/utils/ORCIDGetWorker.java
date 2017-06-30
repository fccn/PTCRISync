/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.utils;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;

/**
 * A worker thread that can be used to GET works from ORCID.
 *
 * @see ORCIDWorker
 */
public final class ORCIDGetWorker extends ORCIDWorker {

	private final WorkSummary work;

	/**
	 * A threaded worker that can be launched in parallel to GET works with the
	 * ORCID API. The provided {@link ORCIDClient client} defines the
	 * communication channel.
	 * 
	 * @see ORCIDHelper#getFullWork(WorkSummary, Map)
	 *
	 * @param work
	 *            the summary specifying the full work to be retrieved
	 * @param client
	 *            the ORCID communication client
	 * @param cb
	 *            the callback object to return results
	 * @param log
	 *            a logger
	 * @throws NullPointerException
	 *             if the work is null
	 * @throws InvalidParameterException
	 *             if the work's put-code is undefined
	 */
	public ORCIDGetWorker(WorkSummary work, ORCIDClient client, Map<BigInteger, PTCRISyncResult> cb, Logger log)
			throws InvalidParameterException, NullPointerException {
		super(client, cb, log);
		if (work == null)
			throw new NullPointerException("GET: Work must not be null.");
		if (work.getPutCode() == null)
			throw new InvalidParameterException("GET: Work must have a put-code defined.");
		this.work = work;
	}

	/**
	 * Retrieves a full work from an ORCID profile.
	 */
	@Override
	public void run() {
		try {
			MDC.setContextMap(mdcCtxMap);
		} catch (Exception e) {} // if the context is empty
		
		_log.debug("[getFullWork] " + work.getPutCode());
		final PTCRISyncResult fullWork = client.getWork(work);

		callback(work.getPutCode(), fullWork);
	}

}
