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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;
import pt.ptcris.handlers.ProgressHandler;

/**
 * A worker thread that can be used to GET works from ORCID.
 *
 * @see ORCIDWorker
 */
final class ORCIDGetWorkWorker extends ORCIDWorker<Work> {

	private final WorkSummary work;

	/**
	 * A threaded worker that can be launched in parallel to GET works with the
	 * ORCID API. The provided {@link ORCIDClient client} defines the
	 * communication channel.
	 * 
	 * @see ORCIDHelper#readWorker(org.um.dsi.gavea.orcid.model.common.ElementSummary, Map)
	 *
	 * @param work
	 *            the summary specifying the full work to be retrieved
	 * @param client
	 *            the ORCID communication client
	 * @param cb
	 *            the callback object to return results
	 * @param log
	 *            a logger
	 */
	public ORCIDGetWorkWorker(WorkSummary work, ORCIDClient client, Map<BigInteger, PTCRISyncResult<Work>> cb, Logger log, ProgressHandler handler) {
		super(client, cb, log, handler);
		assert work != null;
		assert work.getPutCode() != null;
		
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
		
		final PTCRISyncResult<Work> full = client.getWork(work);
		handler.step();
		callback(work.getPutCode(), full);
	}

}
