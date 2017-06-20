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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.MDC;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;

/**
 * A worker thread that can be used to read bulk works from ORCID.
 *
 * @see ORCIDWorker
 */
final class ORCIDGetBulkWorkWorker extends ORCIDWorker<Work> {

	private final List<WorkSummary> works;

	/**
	 * A threaded worker that can be launched in parallel to bulk read works
	 * with the ORCID API. The provided {@link ORCIDClient client} defines the
	 * communication channel.
	 * 
	 * @see ORCIDHelper#readWorker(List, Map)
	 *
	 * @param works
	 *            the list of work summaries specifying the full works to be
	 *            retrieved
	 * @param client
	 *            the ORCID communication client
	 * @param cb
	 *            the callback object to return results
	 * @param log
	 *            a logger
	 */
	public ORCIDGetBulkWorkWorker(List<WorkSummary> works, ORCIDClient client, Map<BigInteger, PTCRISyncResult<Work>> cb, Logger log) {
		super(client, cb, log);
		
		assert works != null && !works.isEmpty();
				
		this.works = works;
	}

	/**
	 * Retrieves a bulk of full works from an ORCID profile.
	 */
	@Override
	public void run() {
		try {
			MDC.setContextMap(mdcCtxMap);
		} catch (Exception e) {} // if the context is empty
		
		_log.debug("[getFullBulkWork] "+works.size());
		
		final Map<BigInteger,PTCRISyncResult<Work>> fulls = client.getWorks(works);
		
		for (WorkSummary w : works) {
			assert w.getPutCode() != null;
			
			PTCRISyncResult<Work> wrk = fulls.get(w.getPutCode());
			callback(w.getPutCode(), wrk);
		}
	}

}
