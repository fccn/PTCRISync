package pt.ptcris.utils;

import org.um.dsi.gavea.orcid.model.work.Work;

public class UpdateRecord {

	private Work localWork;
	private Work remoteWork;

	public UpdateRecord(Work localWork, Work remoteWork) {
		this.localWork = localWork;
		this.remoteWork = remoteWork;
	}

	public Work getLocalWork() {
		return localWork;
	}

	public void setLocalWork(Work localWork) {
		this.localWork = localWork;
	}

	public Work getRemoteWork() {
		return remoteWork;
	}

	public void setRemoteWork(Work remoteWork) {
		this.remoteWork = remoteWork;
	}
}
