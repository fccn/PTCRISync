package pt.ptcris;

import java.util.HashSet;
import java.util.Set;

import org.um.dsi.gavea.orcid.model.common.RelationshipType;
import org.um.dsi.gavea.orcid.model.work.ExternalIdentifier;
import org.um.dsi.gavea.orcid.model.work.WorkExternalIdentifiers;

/**
 * Calculates and stores the symmetric difference between two sets of
 * {@link ExternalIdentifier external identifiers}.
 * 
 * @author nmm
 *
 */
public class ExternalIdentifiersUpdate {

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
	 * @param uids1
	 *            the first set of external identifiers.
	 * @param uids2
	 *            the second set of external identifiers.
	 */
	ExternalIdentifiersUpdate(WorkExternalIdentifiers uids1, WorkExternalIdentifiers uids2) {
		calculateDifference(uids1, uids2);
	}

	/**
	 * Calculates the symmetric difference between two sets of external
	 * identifiers, i.e., the set of removed, preserved and inserted
	 * identifiers. The algorithm to detected preserved identifiers is the same
	 * as the one implemented by the ORCID service to detect overlapping
	 * identifiers. Only considered duplicate if UIDs have the same relationship
	 * and are not "part of".
	 * 
	 * TODO: optimize
	 * 
	 * @param uids1
	 *            a set of UIDs.
	 * @param uids2
	 *            another set of UIDs.
	 */
	private void calculateDifference(WorkExternalIdentifiers uids1, WorkExternalIdentifiers uids2) {
		less.addAll(uids1.getWorkExternalIdentifier());
		more.addAll(uids2.getWorkExternalIdentifier());
		if (uids2 != null && uids1 != null) {
			for (ExternalIdentifier uid2 : uids2.getWorkExternalIdentifier()) {
				for (ExternalIdentifier uid1 : uids1.getWorkExternalIdentifier()) {
					if (sameButNotBothPartOf(uid2.getRelationship(), uid1.getRelationship())
							&& uid1.getExternalIdentifierId().equals(uid2.getExternalIdentifierId())
							&& uid1.getExternalIdentifierType().equals(uid2.getExternalIdentifierType())) {
						same.add(uid2);
						less.remove(uid1);
						more.remove(uid2);
					}
				}
			}
		}
	}

	/**
	 * Tests whether two UID relationship types are the same but not part of.
	 * 
	 * @param r1
	 *            a UID relationship type.
	 * @param r2
	 *            another UID relationship type.
	 * @return whether UIDs are the same but not part of.
	 */
	private static boolean sameButNotBothPartOf(RelationshipType r1, RelationshipType r2) {
		if (r1 == null && r2 == null)
			return true;
		if (r1 != null && r1.equals(r2) && !r1.equals(RelationshipType.PART_OF))
			return true;
		return false;
	}

}
