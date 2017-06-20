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
import org.um.dsi.gavea.orcid.model.common.ElementSummary;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;
import pt.ptcris.handlers.ProgressHandler;

/**
 * An abstract worker that can be used to parallelize calls to the ORCID API. A
 * {@link ORCIDClient client} should be provided to establish the communication
 * with the Member API, including the user profile being managed and the Member
 * API id being user to source activities. Communication is performed via callback.
 */
abstract class ORCIDWorker<E extends ElementSummary> extends Thread {

	protected final Logger _log;
	protected final Map<BigInteger, PTCRISyncResult<E>> cb;
	protected final ORCIDClient client;
	protected final ProgressHandler handler;
	protected final Map<String, String> mdcCtxMap;

	/**
	 * A threaded worker that can be launched in parallel to communicate with
	 * the ORCID API. The provided {@link ORCIDClient client} defines the
	 * communication channel.
	 *
	 * @param client
	 *            the ORCID communication client
	 * @param cb
	 *            the callback object to return results
	 * @param _log
	 *            a logger
	 */
	ORCIDWorker(ORCIDClient client, Map<BigInteger, PTCRISyncResult<E>> cb, Logger _log, ProgressHandler handler) {
		assert client != null;
		assert cb != null;
		assert handler != null;
		
		this.handler = handler;
		this.client = client;
		this.cb = cb;
		this._log = _log;
		this.mdcCtxMap = MDC.getCopyOfContextMap();
	}

	/**
	 * Calls back the owner with a result.
	 *
	 * @param res
	 *            the result to return
	 */
	protected void callback(BigInteger id, PTCRISyncResult<E> res) {
		cb.put(id, res);
	}
	
}
