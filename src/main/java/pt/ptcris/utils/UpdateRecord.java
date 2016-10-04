package pt.ptcris.utils;

import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

/**
 * An helper class that stores to versions of the same {@link Work work}, as
 * well as the {@link ExternalIdsDiff difference} between their
 * {@link org.um.dsi.gavea.orcid.model.work.ExternalIdentifier external
 * identifiers}.
 */
public final class UpdateRecord {

	public final Work preWork;
	public final WorkSummary posWork;
	public final ExternalIdsDiff eidsDiff;

	/**
	 * Constructs an object containing two versions of a work and the difference
	 * between their
	 * {@link org.um.dsi.gavea.orcid.model.work.ExternalIdentifier external
	 * identifiers}. The updated version is assumed to be only a summary, since
	 * the contained information is sufficient for the synchronization procedures.
	 * 
	 * @param preWork
	 *            the current version of the work
	 * @param posWork
	 *            the updated version of the work
	 * @param eidsDiff
	 *            the difference between the identifiers
	 * @throws NullPointerException if any of the parameters is null
	 */
	public UpdateRecord(Work preWork, WorkSummary posWork, ExternalIdsDiff eidsDiff) 
			throws NullPointerException {
		if (preWork == null || posWork == null || eidsDiff == null)
			throw new NullPointerException("Null arguments.");
		this.preWork = preWork;
		this.posWork = posWork;
		this.eidsDiff = eidsDiff;
	}

}
