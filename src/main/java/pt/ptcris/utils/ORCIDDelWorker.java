package pt.ptcris.utils;

import java.math.BigInteger;
import java.util.Map;

import org.slf4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;

import pt.ptcris.ORCIDClient;

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
		try {
			client.deleteWork(putcode);

			callback(putcode, null);
		} catch (final OrcidClientException e) {
			callback(putcode,e);
		}
	}

}
