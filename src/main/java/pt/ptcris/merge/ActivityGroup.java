package pt.ptcris.merge;

import java.util.HashSet;
import java.util.Set;

import org.um.dsi.gavea.orcid.model.common.ElementSummary;

/**
 * Represents groups of matched activities. Currently, simply encapsulates a set of
 * activities, there is no additional structure.
 * 
 * @param <A>
 *            the type of activities being grouped
 */
public class ActivityGroup<A extends ElementSummary> {
	private Set<A> activities;

	/**
	 * Create a new singleton group.
	 * 
	 * @param elem
	 *            the singleton activity
	 */
	public ActivityGroup(A elem) {
		activities = new HashSet<A>();
		activities.add(elem);
	}

	/**
	 * Retrieves the activities belonging to the group.
	 * 
	 * @return the activities of the group
	 */
	public Set<A> getActivities() {
		if (activities == null)
			activities = new HashSet<A>();
		return activities;
	}

	/**
	 * Adds a new (unique) activity to the group. Duplicates are ignored.
	 * 
	 * @param activity
	 *            the activity to be added
	 */
	public void add(A activity) {
		activities.add(activity);
	}

	/**
	 * Merges the current group with another. Duplicates are ignored.
	 * 
	 * @param group
	 *            the group to be merged
	 */
	public void merge(ActivityGroup<A> group) {
		for (A activity : group.getActivities()) {
			activities.add(activity);
		}
	}

}