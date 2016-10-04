package pt.ptcris.workers;

import java.math.BigInteger;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;

import pt.ptcris.ORCIDClient;

/**
 * A worker thread that can be used to DELETE works from ORCID.
 *
 * @see ORCIDWorker
 */
public class ORCIDDelWorker extends ORCIDWorker {

	private final BigInteger putCode;

	/**
	 * A threaded worker that can be launched in parallel to DELETE works with
	 * the ORCID API. The provided {@link ORCIDClient client} defines the
	 * communication channel.
	 *
	 * @param putCode
	 *            the put-code of the work that is to be deleted
	 * @param client
	 *            the ORCID communication client
	 * @param cb
	 *            the callback object to return results
	 * @param log
	 *            a logger
	 * @throws NullPointerException
	 *             if the putcode is null
	 */
	public ORCIDDelWorker(BigInteger putCode, ORCIDClient client, Map<BigInteger, Object> cb, Logger log) 
			throws NullPointerException {
		super(client, cb, log);
		if (putCode == null)
			throw new NullPointerException("DELETE: arguments must not be null.");
		this.putCode = putCode;
	}

	public void run() {
		try {
			client.deleteWork(putCode);
		} catch (OrcidClientException e) {
			callback(putCode,e);
		}
	}

}
