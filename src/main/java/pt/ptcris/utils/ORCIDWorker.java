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

import pt.ptcris.ORCIDClient;

/**
 * An abstract worker that can be used to parallelize calls to the ORCID API. A
 * {@link ORCIDClient client} should be provided to establish the communication
 * with the Member API, including the user profile being managed and the Member
 * API id being user to source works. Communication is performed via callback.
 */
public abstract class ORCIDWorker extends Thread {

	protected final Logger _log;
	protected final Map<BigInteger, Object> cb;
	protected final ORCIDClient client;
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
	 * @param log
	 *            a logger
	 * @throws NullPointerException
	 *             if cb or client are null
	 */
	ORCIDWorker(ORCIDClient client, Map<BigInteger, Object> cb, Logger log)
			throws InvalidParameterException {
		if (cb == null || client == null)
			throw new NullPointerException("Client and callback must not be null.");
		this.client = client;
		this.cb = cb;
		this._log = log;
		this.mdcCtxMap = MDC.getCopyOfContextMap();
	}

	/**
	 * Calls back the owner with a result.
	 *
	 * @param res
	 *            the result to return
	 */
	protected void callback(BigInteger id, Object res) {
		cb.put(id, res);
	}

}
