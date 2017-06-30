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
import java.util.List;
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
public final class ORCIDBulkGetWorker extends ORCIDWorker {

	private final List<WorkSummary> works;

	/**
	 * A threaded worker that can be launched in parallel to GET works with the
	 * ORCID API. The provided {@link ORCIDClient client} defines the
	 * communication channel.
	 * 
	 * @see ORCIDHelper#getFullWork(WorkSummary, Map)
	 *
	 * @param works
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
	public ORCIDBulkGetWorker(List<WorkSummary> works, ORCIDClient client, Map<BigInteger, PTCRISyncResult> cb, Logger log)
			throws InvalidParameterException, NullPointerException {
		super(client, cb, log);
		if (works == null)
			throw new NullPointerException("Bulk GET: Work must not be null.");
		if (works.size() == 0)
			throw new InvalidParameterException("Bulk GET: Must have some put-code.");
		this.works = works;
	}

	/**
	 * Retrieves a full work from an ORCID profile.
	 */
	@Override
	public void run() {
		try {
			MDC.setContextMap(mdcCtxMap);
		} catch (Exception e) {} // if the context is empty
		
		_log.debug("[getFullWorks] " + works.size());
		final Map<BigInteger,PTCRISyncResult> fullWorks = client.getWorks(works);
		
		for (WorkSummary w : works) {
			PTCRISyncResult wrk = fullWorks.get(w.getPutCode());
			callback(w.getPutCode(), wrk);
		}
	}

}
