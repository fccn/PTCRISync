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
 * An helper class that stores two versions of the same {@link ElementSummary
 * activity}, as well as the {@link ExternalIdsDiff difference} between their
 * {@link ExternalId external identifiers}.
 */
public final class UpdateRecord<E extends ElementSummary, S extends ElementSummary> {

	public final E preElement;
	public final S posElement;
	public final ExternalIdsDiff eidsDiff;

	/**
	 * Constructs an object containing two versions of an activity and the
	 * difference between their {@link ExternalId external identifiers}. The
	 * updated version is assumed to be only a summary, since the contained
	 * information is sufficient for the synchronization procedures.
	 * 
	 * @param preActivity
	 *            the current version of the activity
	 * @param posActivity
	 *            the updated version of the activity
	 * @param eidsDiff
	 *            the difference between the identifiers
	 */
	public UpdateRecord(E preActivity, S posActivity, ExternalIdsDiff eidsDiff) {
		if (preActivity == null || posActivity == null || eidsDiff == null)
			throw new IllegalArgumentException("Null arguments.");
		this.preElement = preActivity;
		this.posElement = posActivity;
		this.eidsDiff = eidsDiff;
	}

}
