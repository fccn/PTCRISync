package pt.ptcris.workers;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.ORCIDClient;

/**
 * A worker thread that can be used to UPDATE works from ORCID.
 *
 * @see ORCIDWorker
 *
 */
public class ORCIDUpdWorker extends ORCIDWorker {

	private final Work work;

	/**
	 * A threaded worker that can be launched in parallel to UPDATE works with
	 * the ORCID API. The provided {@link ORCIDClient client} defines the
	 * communication channel.
	 *
	 * @param work
	 *            the work that is to be updated
	 * @param client
	 *            the ORCID communication client
	 * @param cb
	 *            the callback object to return results
	 * @param log
	 *            a logger
	 * @throws InvalidParameterException
	 *             if the work's putcode is undefined
	 * @throws InvalidParameterException
	 *             if the work is null
	 */
	public ORCIDUpdWorker(Work work, ORCIDClient client,
			Map<BigInteger, Object> cb, Logger log)
			throws NullPointerException, InvalidParameterException {
		super(client, cb, log);
		if (work == null)
			throw new NullPointerException(
					"UPDATE: arguments must not be null.");
		if (work.getPutCode() == null)
			throw new InvalidParameterException(
					"UPDATE: Work must have a putcode defined.");
		this.work = work;
	}

	@Override
	public void run() {
		try {
			client.updateWork(work.getPutCode(), work);
		} catch (final OrcidClientException e) {
			callback(work.getPutCode(), e);
		}
	}

}
