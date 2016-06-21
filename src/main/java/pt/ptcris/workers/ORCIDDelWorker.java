package pt.ptcris.workers;

import java.math.BigInteger;

import org.apache.logging.log4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;

import pt.ptcris.ORCIDClient;

public class ORCIDDelWorker extends ORCIDWorker {

	private final BigInteger putCode;
	
	public ORCIDDelWorker(ORCIDClient client, BigInteger putCode, Logger log) {
		super(client, log);
		this.putCode = putCode;
	}

	public void run() {
		try {
			client.deleteWork(putCode);
		} catch (OrcidClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
