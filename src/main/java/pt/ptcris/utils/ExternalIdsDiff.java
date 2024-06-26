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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;

/**
 * Calculates and stores the symmetric difference between two sets of
 * {@link ExternalId external identifiers}. Useful to detect matching
 * activities and potential updates.
 */
public final class ExternalIdsDiff {

	/**
	 * External identifiers removed from the first set.
	 */
	public final Set<ExternalId> less = new HashSet<ExternalId>();

	/**
	 * External identifiers preserved in both sets.
	 */
	public final Set<ExternalId> same = new HashSet<ExternalId>();

	/**
	 * External identifiers inserted in the second set.
	 */
	public final Set<ExternalId> more = new HashSet<ExternalId>();

	/**
	 * Calculates and stores the symmetric difference between two sets of
	 * external identifiers.
	 *
	 * @param weids1
	 *            the first set of external identifiers
	 * @param weids2
	 *            the second set of external identifiers
	 */
	public ExternalIdsDiff(ExternalIds weids1, ExternalIds weids2) {
		List<ExternalId> eids1 = new LinkedList<ExternalId>();
		List<ExternalId> eids2 = new LinkedList<ExternalId>();
		
		if (weids1 != null)
			for (ExternalId eid : weids1.getExternalId())
				eids1.add(eid);

		if (weids2 != null)
			for (ExternalId eid : weids2.getExternalId())
				eids2.add(eid);	
		
		calculateDifference(eids1, eids2);
	}

	/**
	 * Calculates the symmetric difference between two sets of external
	 * identifiers, i.e., the set of removed, preserved and inserted
	 * identifiers. The algorithm to detected preserved identifiers is the same
	 * as the one implemented by the ORCID service to detect overlapping
	 * identifiers. Only considered duplicate if external identifiers have the
	 * same relationship and are not "part of".
	 *
	 * TODO: the URLs assigned to the external identifiers are being ignored;
	 * this means that ids with different URLs are considered the same; also,
	 * the selection of the id to "same" when there is a match is arbitrary.
	 * TODO: optimize.
	 *
	 * @param eids1
	 *            a set of UIDs
	 * @param eids2
	 *            another set of UIDs
	 */
	private void calculateDifference(List<ExternalId> eids1, List<ExternalId> eids2) {
		less.addAll(eids1);
		more.addAll(eids2);
		
		for (final ExternalId eid2 : eids2) {
			for (final ExternalId eid1 : eids1) {
				if (eid2.getExternalIdRelationship().equals(eid1.getExternalIdRelationship())
						&& eid1.getExternalIdType().equals(eid2.getExternalIdType())
						&& ExternalIdsNormalizer.normaliseId(eid1.getExternalIdType(), eid1.getExternalIdValue()).equals(ExternalIdsNormalizer.normaliseId(eid2.getExternalIdType(), eid2.getExternalIdValue()))){
							same.add(eid2);
							less.remove(eid1);
							more.remove(eid2);
				}
			}
		}
	}

}
