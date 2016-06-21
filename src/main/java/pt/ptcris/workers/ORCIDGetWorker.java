package pt.ptcris.workers;

import java.math.BigInteger;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.ORCIDClient;

public class ORCIDGetWorker extends ORCIDWorker {

	private final Set<Work> works;
	private final BigInteger putCode;
	
	public ORCIDGetWorker(ORCIDClient client, Set<Work> works, BigInteger putCode, Logger log) {
		super(client, log);
		this.works = works;
		this.putCode = putCode;
	}

	public void run() {
		try {
			works.add(client.getWork(putCode));		
			} catch (OrcidClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
