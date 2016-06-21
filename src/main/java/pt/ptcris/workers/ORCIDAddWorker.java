package pt.ptcris.workers;

import java.math.BigInteger;

import org.apache.logging.log4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.ORCIDClient;

public class ORCIDAddWorker extends ORCIDWorker {

	private final Work work;

	public ORCIDAddWorker(ORCIDClient client, Work work, Logger log) {
		super(client, log);
		this.work = work;
	}
	

	public void run() {
		try {
			BigInteger putCode = client.addWork(work);
			work.setPutCode(putCode);
			_log.debug("[addWork] " + work.getPutCode());
		} catch (OrcidClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
