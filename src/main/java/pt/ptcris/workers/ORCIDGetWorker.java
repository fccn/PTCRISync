package pt.ptcris.workers;

import java.util.Collection;

import org.apache.logging.log4j.Logger;
import org.um.dsi.gavea.orcid.client.exception.OrcidClientException;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

import pt.ptcris.ORCIDClient;
import pt.ptcris.ORCIDHelper;

public class ORCIDGetWorker extends ORCIDWorker {

	private final Collection<Work> works;
	private final WorkSummary work;

	public ORCIDGetWorker(ORCIDClient client, Collection<Work> works, WorkSummary work, Logger log) {
		super(client, log);
		this.works = works;
		this.work = work;
	}

	public void run() {
		try {
			Work fullWork = client.getWork(work.getPutCode());
			fullWork.setExternalIdentifiers(work.getExternalIdentifiers());
			ORCIDHelper.cleanWorkLocalKey(fullWork);
			works.add(fullWork);

		} catch (OrcidClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
