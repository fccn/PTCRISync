package pt.ptcris.workers;

import java.math.BigInteger;

import org.apache.logging.log4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.ORCIDClient;

public class ORCIDUpdWorker extends ORCIDWorker {

	private final Work work;
	
	public ORCIDUpdWorker(ORCIDClient client, Work work, Logger log) {
		super(client, log);
		this.work = work;
	}

	public void run() {
		try {
			client.updateWork(work.getPutCode(), work);
		} catch (OrcidClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
