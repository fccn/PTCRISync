package pt.ptcris.workers;

import java.math.BigInteger;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.ORCIDClient;

/**
 * A worker thread that can be used to ADD works from ORCID.
 *
 * @see ORCIDWorker
 *
 */
public class ORCIDAddWorker extends ORCIDWorker {

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
	public ORCIDAddWorker(Work work, ORCIDClient client,
			Map<BigInteger, Object> cb, Logger log) {
		super(client, cb, log);
		if (work == null)
			throw new NullPointerException(
					"UPDATE: arguments must not be null.");
		this.work = work;
	}

	public void run() {
		try {
			BigInteger putCode = client.addWork(work);
			work.setPutCode(putCode);
			_log.debug("[addWork] " + work.getPutCode());
		} catch (OrcidClientException e) {
			callback(work.getPutCode(), e);
		}
	}

}
