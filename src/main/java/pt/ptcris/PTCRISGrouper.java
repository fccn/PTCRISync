package pt.ptcris;

import java.util.Collection;
import java.util.List;

import pt.ptcris.merge.ActivityGroup;
import pt.ptcris.merge.GroupGenerator;
import pt.ptcris.merge.ActivityComparator;

/**
 * <p>
 * A generic activity grouper for the PTCRIS framework. The method is
 * customizable by a {@link ActivityComparator comparison criteria} that
 * determines when two activities match each other, in which case they are
 * grouped together. This relationship is transitive: two non-matching
 * activities may belong to the same group if they both match a third one.
 * </p>
 * 
 * <p>
 * The user should provide an {@link ActivityComparator activity comparator}
 * that defines matching activities by calculating a similarity metric, and the
 * respective threshold.
 * </p>
 * 
 * TODO: Activities within groups are not ordered, groups are sets. Should they
 * be? Specifically, ordered by meta-data quality (e.g., number of filled
 * fields).
 * 
 * TODO: Singleton groups are being returned. Does this encumber the process?
 * Should they be filtered by PTCRIS or by the users?
 */
public class PTCRISGrouper {

	/**
	 * Groups a list of local activities into groups of matching activities,
	 * according to the provided comparator.
	 * 
	 * @param locals
	 *            the local activities to be grouped
	 * @param comparator
	 *            the provided activity comparator
	 * @return the local activities, grouped
	 */
	public static <E> List<ActivityGroup<E>> group(
			Collection<E> locals, ActivityComparator<E> comparator) {
		GroupGenerator<E> gen = new GroupGenerator<E>(comparator);
		for (E e : locals)
			gen.group(e);
		return gen.getGroups();
	}

}
