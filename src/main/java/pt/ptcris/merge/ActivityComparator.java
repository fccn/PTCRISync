/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.merge;

/**
 * An activity comparator used by the grouper to creates groups of matching
 * activities. Concrete implementations should be provided by the users through
 * a {@link #compare(ElementGroup, ElementGroup)} that calculates a similarity
 * metric between to activities, and a {@link #threshold()}.
 *
 * @param <A>
 *            the type of activities being compared
 */
public abstract class ActivityComparator<A> {

	/**
	 * A (non-inclusive) threshold on the similarity metric that defines
	 * matching activities.
	 * 
	 * @return the threshold value
	 */
	abstract public int threshold();

	/**
	 * Calculates a similarity metric between two activities.
	 * 
	 * @param act1
	 *            one activity
	 * @param act2
	 *            other activity
	 * @return the similarity metric between the activities
	 */
	abstract public float compare(A act1, A act2);

	/**
	 * Tests whether two activities match, given the
	 * {@link #compare(ElementSummary, ElementSummary)} method and the
	 * {@link #threshold()}.
	 * 
	 * @param act1
	 *            one activity
	 * @param act2
	 *            other activity
	 * @return whether the activities match
	 */
	public final boolean matches(A act1, A act2) {
		return compare(act1, act2) > threshold();
	}

	/**
	 * Tests whether an activity belongs to a group, according to the
	 * {@link #compare(ElementSummary, ElementSummary)} criterion.
	 * 
	 * @param act
	 *            the activity to be tested for membership
	 * @param group
	 *            the activity group
	 * @return whether the activity belongs to the group
	 */
	public final boolean belongs(A act, ActivityGroup<A> group) {
		for (A act1 : group.getActivities())
			if (matches(act1, act))
				return true;
		return false;
	}

}
