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
import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;

/**
 * A worker thread that can be used to UPDATE works from ORCID.
 *
 * @see ORCIDWorker
 */
@Deprecated
class ORCIDUpdWorker extends ORCIDWorker {

	private final Work work;

	/**
	 * A threaded worker that can be launched in parallel to UPDATE works with
	 * the ORCID API. The provided {@link ORCIDClient client} defines the
	 * communication channel.
	 *
	 * @param work
	 *            the work that is to be updated
	 * @param client
	 *            the ORCID communication client
	 * @param cb
	 *            the callback object to return results
	 * @param log
	 *            a logger
	 * @throws InvalidParameterException
	 *             if the work's put-code is undefined
	 * @throws InvalidParameterException
	 *             if the work is null
	 */
	public ORCIDUpdWorker(Work work, ORCIDClient client, Map<BigInteger, PTCRISyncResult> cb, Logger log)
			throws NullPointerException, InvalidParameterException {
		super(client, cb, log);
		if (work == null)
			throw new NullPointerException("UPDATE: arguments must not be null.");
		if (work.getPutCode() == null)
			throw new InvalidParameterException("UPDATE: Work must have a put-code defined.");
		this.work = work;
	}

	/**
	 * Updates a work in an ORCID profile.
	 */
	@Override
	public void run() {
		_log.debug("[updateWork] " + work.getPutCode());
		MDC.setContextMap(mdcCtxMap);

		final PTCRISyncResult res = client.updateWork(work.getPutCode(), work);
		
		callback(work.getPutCode(), res);
	}

}
