package pt.ptcris.utils;

import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

public class UpdateRecord {

	private Work localWork;
	private WorkSummary remoteWork;

	public UpdateRecord(Work localWork, WorkSummary workSummary) {
		this.localWork = localWork;
		this.remoteWork = workSummary;
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
}
