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

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;
import pt.ptcris.handlers.ProgressHandler;

/**
 * A worker thread that can be used to ADD works from ORCID.
 *
 * @see ORCIDWorker
 */
@Deprecated
class ORCIDAddWorker extends ORCIDWorker<Work> {

	private final Work work;

	/**
	 * A threaded worker that can be launched in parallel to ADD works with the
	 * ORCID API. The provided {@link ORCIDClient client} defines the
	 * communication channel.
	 *
	 * @param work
	 *            the work that is to be add
	 * @param client
	 *            the ORCID communication client
	 * @param cb
	 *            the callback object to return results
	 * @param log
	 *            a logger
	 * @throws NullPointerException
	 *             if the work is null
	 */
	public ORCIDAddWorker(Work work, ORCIDClient client, Map<BigInteger, PTCRISyncResult<Work>> cb, Logger log, ProgressHandler handler) {
		super(client, cb, log, handler);
		if (work == null)
			throw new NullPointerException("UPDATE: arguments must not be null.");
		this.work = work;
	}

	/**
	 * Adds a work to an ORCID profile.
	 */
	@Override
	public void run() {
		_log.debug("[addWork] " + new ORCIDWorkHelper(null).getTitleE(work));
		MDC.setContextMap(mdcCtxMap);

		final PTCRISyncResult<Work> res = client.addWork(work);
		if (res.putcode == null)
			callback(BigInteger.valueOf(0), res);
		else
			callback(res.putcode, res);
	}

}
