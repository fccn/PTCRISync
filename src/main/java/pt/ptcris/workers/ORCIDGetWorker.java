package pt.ptcris.workers;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

import pt.ptcris.ORCIDClient;
import pt.ptcris.ORCIDHelper;

public class ORCIDGetWorker extends ORCIDWorker {

	private final Map<BigInteger,Work> works;
	private final WorkSummary work;

	public ORCIDGetWorker(ORCIDClient client, Map<BigInteger,Work> works, WorkSummary work, Logger log) {
		super(client, log);
		this.works = works;
		this.work = work;
	}

	public void run() {
		try {
			Work fullWork = client.getWork(work.getPutCode());
			fullWork.setExternalIdentifiers(work.getExternalIdentifiers());
			ORCIDHelper.cleanWorkLocalKey(fullWork);
			works.put(work.getPutCode(),fullWork);

		} catch (OrcidClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
