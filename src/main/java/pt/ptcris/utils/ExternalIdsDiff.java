package pt.ptcris.utils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.common.ExternalIds;
import org.um.dsi.gavea.orcid.model.common.RelationshipType;

/**
 * Calculates and stores the symmetric difference between two sets of
 * {@link ExternalId external identifiers}. Useful to detect matching
 * works and potential updates.
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

		if (weids1 != null && weids1.getExternalId() != null)
			for (ExternalId eid : weids1.getExternalId())
				if (eid.getExternalIdRelationship() == RelationshipType.SELF)
					eids1.add(eid);

		if (weids2 != null && weids2.getExternalId() != null)
			for (ExternalId eid : weids2.getExternalId())
				if (eid.getExternalIdRelationship() == RelationshipType.SELF)
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
	 * TODO: optimize
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
				if (sameButNotBothPartOf(eid2.getExternalIdRelationship(),eid1.getExternalIdRelationship())
						&& eid1.getExternalIdValue().equals(eid2.getExternalIdValue())
						&& eid1.getExternalIdType().equals(eid2.getExternalIdType())) {
					same.add(eid2);
					less.remove(eid1);
					more.remove(eid2);
				}
			}
		}
	}

	/**
	 * Tests whether two external identifier {@link RelationshipType
	 * relationship types} are the same but not part of.
	 *
	 * @param r1
	 *            an external identifier relationship type
	 * @param r2
	 *            another external identifier relationship type
	 * @return whether external identifiers are the same but not part of
	 */
	private static boolean sameButNotBothPartOf(RelationshipType r1, RelationshipType r2) {
		if (r1 == null && r2 == null)
			return true;
		if (r1 != null && r1.equals(r2) && !r1.equals(RelationshipType.PART_OF))
			return true;
		return false;
	}

}
