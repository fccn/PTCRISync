package pt.ptcris.utils;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.um.dsi.gavea.orcid.model.common.RelationshipType;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifier;
import org.um.dsi.gavea.orcid.model.work.WorkExternalIdentifiers;

/**
 * Calculates and stores the symmetric difference between two sets of
 * {@link ExternalIdentifier external identifiers}. Useful to detect matching
 * works and potential updates.
 */
public final class ExternalIdsDiff {

	/**
	 * External identifiers removed from the first set.
	 */
	public final Set<ExternalIdentifier> less = new HashSet<ExternalIdentifier>();

	/**
	 * External identifiers preserved in both sets.
	 */
	public final Set<ExternalIdentifier> same = new HashSet<ExternalIdentifier>();

	/**
	 * External identifiers inserted in the second set.
	 */
	public final Set<ExternalIdentifier> more = new HashSet<ExternalIdentifier>();

	/**
	 * Calculates and stores the symmetric difference between two sets of
	 * external identifiers.
	 *
	 * @param eids1
	 *            the first set of external identifiers
	 * @param eids2
	 *            the second set of external identifiers
	 */
	public ExternalIdsDiff(WorkExternalIdentifiers weids1, WorkExternalIdentifiers weids2) {
		List<ExternalIdentifier> eids1 = new LinkedList<ExternalIdentifier>();
		List<ExternalIdentifier> eids2 = new LinkedList<ExternalIdentifier>();

		if (weids1 != null && weids1.getWorkExternalIdentifier() != null)
			eids1.addAll(weids1.getWorkExternalIdentifier());

		if (weids2 != null && weids2.getWorkExternalIdentifier() != null)
			eids2.addAll(weids2.getWorkExternalIdentifier());
		
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
	private void calculateDifference(List<ExternalIdentifier> eids1, List<ExternalIdentifier> eids2) {
		less.addAll(eids1);
		more.addAll(eids2);
		for (final ExternalIdentifier eid2 : eids2) {
			for (final ExternalIdentifier eid1 : eids1) {
				if (sameButNotBothPartOf(eid2.getRelationship(),eid1.getRelationship())
						&& eid1.getExternalIdentifierId().equals(eid2.getExternalIdentifierId())
						&& eid1.getExternalIdentifierType().equals(eid2.getExternalIdentifierType())) {
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
