package pt.ptcris.workers;

import org.apache.logging.log4j.Logger;
import pt.ptcris.ORCIDClient;

public abstract class ORCIDWorker extends Thread {

	protected final Logger _log;
	
	/**
	 * The client used to communicate with ORCID. Defines the ORCID user profile
	 * being managed and the Member API id being user to source works.
	 */
	protected final ORCIDClient client;


	ORCIDWorker(ORCIDClient client, Logger log) {
		this.client = client;
		this._log = log;
	}

}
