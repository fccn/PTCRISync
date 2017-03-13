package pt.ptcris.utils;

import java.math.BigInteger;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.MDC;

import pt.ptcris.ORCIDClient;
import pt.ptcris.PTCRISyncResult;

/**
 * A worker thread that can be used to DELETE works from ORCID.
 *
 * @see ORCIDWorker
 */
public class ORCIDDelWorker extends ORCIDWorker {

	private final BigInteger putcode;

	/**
	 * A threaded worker that can be launched in parallel to DELETE works with
	 * the ORCID API. The provided {@link ORCIDClient client} defines the
	 * communication channel.
	 *
	 * @param putcode
	 *            the put-code of the work that is to be deleted
	 * @param client
	 *            the ORCID communication client
	 * @param cb
	 *            the callback object to return results
	 * @param log
	 *            a logger
	 * @throws NullPointerException
	 *             if the put-code is null
	 */
	public ORCIDDelWorker(BigInteger putcode, ORCIDClient client, Map<BigInteger, Object> cb, Logger log)
			throws NullPointerException {
		super(client, cb, log);
		if (putcode == null)
			throw new NullPointerException("DELETE: arguments must not be null.");
		this.putcode = putcode;
	}

	/**
	 * Removes a work from an ORCID profile.
	 */
	@Override
	public void run() {
		_log.debug("[deleteWork] " + putcode);
		MDC.setContextMap(mdcCtxMap);

		PTCRISyncResult res = client.deleteWork(putcode);

		callback(putcode, res);
	}

}
