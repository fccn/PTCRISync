package pt.ptcris.utils;

import java.util.Set;

import org.um.dsi.gavea.orcid.model.work.ExternalIdentifier;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

import pt.ptcris.ExternalIdentifiersUpdate;

public class UpdateRecord {

	private Work localWork;
	private WorkSummary remoteWork;
	private ExternalIdentifiersUpdate matches;
	
	public UpdateRecord(Work localWork, WorkSummary workSummary, ExternalIdentifiersUpdate matches) {
		this.localWork = localWork;
		this.remoteWork = workSummary;
		this.matches = matches;
	}

	public Work getLocalWork() {
		return localWork;
	}

	public void setLocalWork(Work localWork) {
		this.localWork = localWork;
	}

	public WorkSummary getRemoteWork() {
		return remoteWork;
	}

	public void setRemoteWork(WorkSummary remoteWork) {
		this.remoteWork = remoteWork;
	}
	
	public ExternalIdentifiersUpdate getMatches() {
		return matches;
	}

}
