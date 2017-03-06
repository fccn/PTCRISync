package pt.ptcris.merge;

import java.util.ArrayList;
import java.util.List;

import org.um.dsi.gavea.orcid.model.common.ElementSummary;

public class GroupGenerator<E extends ElementSummary> {

	private List<ActivityGroup<E>> groups = new ArrayList<ActivityGroup<E>>();

	private final ActivityComparator<E> comparator;

	/**
	 * Creates a new activity group generator, provided a comparator that will
	 * determine whether activities match.
	 * 
	 * @param comparator
	 *            the activity comparator
	 */
	public GroupGenerator(ActivityComparator<E> comparator) {
		this.comparator = comparator;
	}

	/**
	 * Adds an activity with the already existing groups. If the activity
	 * matches more than one group, then those groups are merged. If it matches
	 * none, a new group is created.
	 * 
	 * @param act the activity to be added to the groups
	 */
	public void group(E act) {
		List<ActivityGroup<E>> belongsTo = new ArrayList<ActivityGroup<E>>();

		// determine to which existing groups the activity belongs to
		for (ActivityGroup<E> g : groups) {
			if (comparator.belongs(act, g))
				belongsTo.add(g);
		}

		// if it doesn't belong to any, create a new group
		if (belongsTo.isEmpty()) {
			ActivityGroup<E> newGroup = new ActivityGroup<E>(act);
			groups.add(newGroup);
		// otherwise merge all those groups
		} else {
			ActivityGroup<E> base = belongsTo.get(0);
			base.add(act);

			if (belongsTo.size() > 1) {
				for (int i = 1; i < belongsTo.size(); i++) {
					base.merge(belongsTo.get(i));
					groups.remove(belongsTo.get(i));
				}
			}
		}
	}

	/**
	 * The activity groups generated so far. The order is currently meaningless.
	 * 
	 * @return the groups
	 */
	public List<ActivityGroup<E>> getGroups() {
		return groups;
	}
}
