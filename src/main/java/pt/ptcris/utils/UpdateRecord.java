/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.utils;

import org.um.dsi.gavea.orcid.model.common.ElementSummary;
import org.um.dsi.gavea.orcid.model.common.ExternalId;

/**
 * An helper class that stores two versions of the same {@link Work work}, as
 * well as the {@link ExternalIdsDiff difference} between their
 * {@link ExternalId external identifiers}.
 */
public final class UpdateRecord<E extends ElementSummary, S extends ElementSummary> {

	public final E preWork;
	public final S posWork;
	public final ExternalIdsDiff eidsDiff;

	/**
	 * Constructs an object containing two versions of a work and the difference
	 * between their {@link ExternalId external identifiers}. The updated
	 * version is assumed to be only a summary, since the contained information
	 * is sufficient for the synchronization procedures.
	 * 
	 * @param preWork
	 *            the current version of the work
	 * @param posWork
	 *            the updated version of the work
	 * @param eidsDiff
	 *            the difference between the identifiers
	 * @throws NullPointerException
	 *             if any of the parameters is null
	 */
	public UpdateRecord(E preWork, S posWork, ExternalIdsDiff eidsDiff) 
			throws NullPointerException {
		if (preWork == null || posWork == null || eidsDiff == null)
			throw new NullPointerException("Null arguments.");
		this.preWork = preWork;
		this.posWork = posWork;
		this.eidsDiff = eidsDiff;
	}

}
