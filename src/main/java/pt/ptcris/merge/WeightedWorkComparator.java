package pt.ptcris.merge;

import org.simmetrics.metrics.StringMetrics;
import org.um.dsi.gavea.orcid.model.work.Work;

/**
 * A work comparator that uses weighted string similarity (using Levenshtein ratio) to compare two works. If the similarity is above the provided
 * threshold, the works are considered similar.
 * 
 * The weights have default values but can be provided upon creation of the comparator instance.
 *
 */
public class WeightedWorkComparator extends ActivityComparator<Work> {

	private static final int DEFAULT_THRESHOLD = 75; // Default threshold is 75%;
	private static final int DEFAULT_WEIGHT_TITLE = 50; // Default title weight is 50%;
	private static final int DEFAULT_WEIGHT_SOURCE = 20; // Default source weight is 20%;
	private static final int DEFAULT_WEIGHT_YEAR = 20; // Default year weight is 20%;
	private static final int DEFAULT_WEIGHT_TYPE = 10; // Default type weight is 10%;

	private int threshold;
	private int weight_title;
	private int weight_source;
	private int weight_year;
	private int weight_type;

	/**
	 * 
	 * @param threshold
	 *            the threshold to be considered when comparing works (default: 75%)
	 * @param weight_title
	 *            the weight considered for the comparison of work titles (default: 50%)
	 * @param weight_source
	 *            the weight considered for the comparison of work sources (the journal/conference/book titles - default: 20%)
	 * @param weight_year
	 *            the weight considered for the comparison of work years (default: 20%)
	 * @param weight_type
	 *            the weight considered for the comparison of work types (default: 10%)
	 * 
	 * @throw IllegalArgumentException when the sum of the weights is not 100%
	 */
	public WeightedWorkComparator(int threshold, int weight_title, int weight_source, int weight_year, int weight_type) throws IllegalArgumentException {
		this.threshold = threshold;

		this.weight_title = weight_title;
		this.weight_source = weight_source;
		this.weight_year = weight_year;
		this.weight_type = weight_type;

		if (this.weight_title + this.weight_source + this.weight_year + this.weight_type != 100) {
			throw new IllegalArgumentException("The sum of the weights must be 100.");
		}
	}

	/**
	 * 
	 * @param threshold
	 *            the threshold to be considered when comparing works (default: 75%)
	 */
	public WeightedWorkComparator(int threshold) throws IllegalArgumentException {
		this.threshold = threshold;

		this.weight_title = DEFAULT_WEIGHT_TITLE;
		this.weight_source = DEFAULT_WEIGHT_SOURCE;
		this.weight_year = DEFAULT_WEIGHT_YEAR;
		this.weight_type = DEFAULT_WEIGHT_TYPE;

		if (this.weight_title + this.weight_source + this.weight_year + this.weight_type != 100) {
			throw new IllegalArgumentException("The sum of the weights must be 100.");
		}
	}

	/**
	 * Constructor without parameters: all default values are used
	 */
	public WeightedWorkComparator() throws IllegalArgumentException {
		this.threshold = DEFAULT_THRESHOLD;

		this.weight_title = DEFAULT_WEIGHT_TITLE;
		this.weight_source = DEFAULT_WEIGHT_SOURCE;
		this.weight_year = DEFAULT_WEIGHT_YEAR;
		this.weight_type = DEFAULT_WEIGHT_TYPE;

		if (this.weight_title + this.weight_source + this.weight_year + this.weight_type != 100) {
			throw new IllegalArgumentException("The sum of the weights must be 100.");
		}
	}

	@Override
	public int threshold() {
		return this.threshold;
	}

	@Override
	public float compare(Work work1, Work work2) {
		float similarity = 0.0f;

		// Comparing work titles
		if (work1.getTitle() != null && work2.getTitle() != null) {
			similarity += getStringSimilarity(work1.getTitle().getTitle(), work2.getTitle().getTitle()) * this.weight_title;
		}

		// Comparing work sources (journal/conference/book titles)
		if (work1.getJournalTitle() != null && work2.getJournalTitle() != null) {
			similarity += getStringSimilarity(work1.getJournalTitle().getContent(), work2.getJournalTitle().getContent()) * this.weight_source;
		}

		// Comparing work years
		try {
			if (work1.getPublicationDate() != null && work2.getPublicationDate() != null) {
				if (work1.getPublicationDate().getYear() != null && work2.getPublicationDate().getYear() != null) {
					if (work1.getPublicationDate().getYear().getValue() != null && work2.getPublicationDate().getYear().getValue() != null) {
						similarity += (1.0 / ((float) Math.abs(Integer.parseInt(work1.getPublicationDate().getYear().getValue()) - Integer.parseInt(work2.getPublicationDate().getYear().getValue())) + 1)) * this.weight_year;
					}
				}
			}
		} catch (NumberFormatException ignore) {
		}

		// Comparing work types
		if (work1.getType() != null && work2.getType() != null) {
			similarity += getStringSimilarity(work1.getType().name(), work2.getType().name()) * this.weight_type;
		}

		return similarity;
	}

	public float getStringSimilarity(String s1, String s2) {
		if (s1 == null || s2 == null) {
			return 0.0f;
		}
		return StringMetrics.levenshtein().compare(s1, s2);
	}
}
