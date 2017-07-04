/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.grouper;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents groups of matched activities. Currently, simply encapsulates a set of
 * activities, there is no additional structure.
 * 
 * @param <A>
 *            the type of activities being grouped
 */
public class ActivityGroup<A> {
	private Set<A> activities;

	/**
	 * Create a new singleton group.
	 * 
	 * @param elem
	 *            the singleton activity
	 */
	ActivityGroup(A elem) {
		activities = new HashSet<A>();
		activities.add(elem);
	}

	/**
	 * Retrieves the activities belonging to the group.
	 * 
	 * @return the activities of the group
	 */
	Set<A> getActivities() {
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
	void add(A activity) {
		activities.add(activity);
	}

	/**
	 * Merges the current group with another. Duplicates are ignored.
	 * 
	 * @param group
	 *            the group to be merged
	 */
	void merge(ActivityGroup<A> group) {
		for (A activity : group.getActivities()) {
			activities.add(activity);
		}
	}

}