package pt.ptcris.test.grouper;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;
import org.um.dsi.gavea.orcid.model.common.FuzzyDate;
import org.um.dsi.gavea.orcid.model.common.FuzzyDate.Year;
import org.um.dsi.gavea.orcid.model.work.JournalTitle;
import org.um.dsi.gavea.orcid.model.work.Work;
import org.um.dsi.gavea.orcid.model.work.WorkTitle;
import org.um.dsi.gavea.orcid.model.work.WorkType;

import pt.ptcris.PTCRISGrouper;
import pt.ptcris.merge.ActivityGroup;
import pt.ptcris.merge.WeightedWorkComparator;

public class WeightedGrouperTest {

	private static Logger logger = Logger.getLogger(WeightedGrouperTest.class.getName());

	/**
	 * Testing two works that are exactly the same. Should be a 100% match
	 */
	@Test
	public void testSimilarWorks1() {
		Work work1 = createWork("Executing Semantic Web Services", "Semantic Web Journal", 2010, WorkType.JOURNAL_ARTICLE);
		Work work2 = createWork("Executing Semantic Web Services", "Semantic Web Journal", 2010, WorkType.JOURNAL_ARTICLE);

		List<Work> works = new ArrayList<Work>();
		works.add(work1);
		works.add(work2);

		WeightedWorkComparator comparator = new WeightedWorkComparator(75);
		List<ActivityGroup<Work>> work_groups = PTCRISGrouper.group(works, comparator);

		logger.log(Level.INFO, "Work Groups: " + work_groups.size());
		Assert.assertEquals(1, work_groups.size());

		logger.log(Level.INFO, "Work Group[0] activities: " + work_groups.get(0).getActivities().size());
		Assert.assertEquals(2, work_groups.get(0).getActivities().size());

		float similarity = comparator.compare(work1, work2);
		logger.log(Level.INFO, "Similarity: " + similarity);
		Assert.assertEquals(100, (int) similarity);
	}

	/**
	 * Testing two works that have a completely different Source Title. Considering that the Source Title weight is 20%, the similarity match for
	 * these two works should be 80%.
	 */
	@Test
	public void testSimilarWorks2() {
		Work work1 = createWork("Executing Semantic Web Services", "Semantic Web Journal", 2010, WorkType.JOURNAL_ARTICLE);
		Work work2 = createWork("Executing Semantic Web Services", "", 2010, WorkType.JOURNAL_ARTICLE);

		List<Work> works = new ArrayList<Work>();
		works.add(work1);
		works.add(work2);

		WeightedWorkComparator comparator = new WeightedWorkComparator(75);
		List<ActivityGroup<Work>> work_groups = PTCRISGrouper.group(works, comparator);

		logger.log(Level.INFO, "Work Groups: " + work_groups.size());
		Assert.assertEquals(1, work_groups.size());

		logger.log(Level.INFO, "Work Group[0] activities: " + work_groups.get(0).getActivities().size());
		Assert.assertEquals(2, work_groups.get(0).getActivities().size());

		float similarity = comparator.compare(work1, work2);
		logger.log(Level.INFO, "Similarity: " + similarity);
		Assert.assertEquals(80, (int) similarity);
	}

	/**
	 * Testing two works with Titles that are slightly different, just enough to not pass the 75% threshold. Therefore, this test should create two
	 * distinct groups, each with only one record.
	 */
	@Test
	public void testSimilarWorks3() {
		Work work1 = createWork("Executing Semantic Web Services", "Semantic Web Journal", 2010, WorkType.JOURNAL_ARTICLE);
		Work work2 = createWork("Semantic Web Services Execution", "Semantic Web Journal", 2010, WorkType.JOURNAL_ARTICLE);

		List<Work> works = new ArrayList<Work>();
		works.add(work1);
		works.add(work2);

		WeightedWorkComparator comparator = new WeightedWorkComparator(75);
		List<ActivityGroup<Work>> work_groups = PTCRISGrouper.group(works, comparator);

		logger.log(Level.INFO, "Work Groups: " + work_groups.size());
		Assert.assertEquals(2, work_groups.size());

		logger.log(Level.INFO, "Work Group[0] activities: " + work_groups.get(0).getActivities().size());
		Assert.assertEquals(1, work_groups.get(0).getActivities().size());

		logger.log(Level.INFO, "Work Group[1] activities: " + work_groups.get(1).getActivities().size());
		Assert.assertEquals(1, work_groups.get(1).getActivities().size());

		float similarity = comparator.compare(work1, work2);
		logger.log(Level.INFO, "Similarity: " + similarity);
		Assert.assertEquals(true, (int) similarity < 75);
	}

	/**
	 * Testing two works with Titles that are slightly different, just enough to not pass the 75% threshold. But considering that we're using a 50%
	 * threshold in this test, it should create only one group with two matching records.
	 */
	@Test
	public void testSimilarWorks4() {
		Work work1 = createWork("Executing Semantic Web Services", "Semantic Web Journal", 2010, WorkType.JOURNAL_ARTICLE);
		Work work2 = createWork("Semantic Web Services Execution", "Semantic Web Journal", 2010, WorkType.JOURNAL_ARTICLE);

		List<Work> works = new ArrayList<Work>();
		works.add(work1);
		works.add(work2);

		WeightedWorkComparator comparator = new WeightedWorkComparator(50);
		List<ActivityGroup<Work>> work_groups = PTCRISGrouper.group(works, comparator);

		logger.log(Level.INFO, "Work Groups: " + work_groups.size());
		Assert.assertEquals(1, work_groups.size());

		logger.log(Level.INFO, "Work Group[0] activities: " + work_groups.get(0).getActivities().size());
		Assert.assertEquals(2, work_groups.get(0).getActivities().size());

		float similarity = comparator.compare(work1, work2);
		logger.log(Level.INFO, "Similarity: " + similarity);
		Assert.assertEquals((int) similarity > 50, true);
	}

	/**
	 * Testing three works with a few differences, but that still should pass the 75% threshold (even though compared individually, the works may not
	 * all match above the 75% threshold). Therefore, this should create a single group with 3 works.
	 */
	@Test
	public void testSimilarWorks5() {
		Work work1 = createWork("Ultra-intense high orbital angular momentum harmonic generation in plasmas", "58th Annual Meeting of the APS Division of Plasma Physics", 2016, WorkType.CONFERENCE_PAPER);
		Work work2 = createWork("Exploring the orbital angular momentum of betatron radiation", "58th Annual Meeting of the APS Division of Plasma Physics", 2016, WorkType.CONFERENCE_PAPER);
		Work work3 = createWork("High orbital angular momentum harmonic generation in plasmas", "43rd EPS Conference on Plasma Physics", 2016, WorkType.CONFERENCE_PAPER);

		List<Work> works = new ArrayList<Work>();
		works.add(work1);
		works.add(work2);
		works.add(work3);

		WeightedWorkComparator comparator = new WeightedWorkComparator(75);
		List<ActivityGroup<Work>> work_groups = PTCRISGrouper.group(works, comparator);

		logger.log(Level.INFO, "Work Groups: " + work_groups.size());
		Assert.assertEquals(1, work_groups.size());

		logger.log(Level.INFO, "Work Group[0] activities: " + work_groups.get(0).getActivities().size());
		Assert.assertEquals(3, work_groups.get(0).getActivities().size());

		float similarity = comparator.compare(work1, work2);
		logger.log(Level.INFO, "Similarity (work1 - work2): " + similarity);
		Assert.assertEquals(true, (int) similarity > 60);

		similarity = comparator.compare(work2, work3);
		logger.log(Level.INFO, "Similarity (work2 - work3): " + similarity);
		Assert.assertEquals(true, (int) similarity > 60);

		similarity = comparator.compare(work1, work3);
		logger.log(Level.INFO, "Similarity (work1 - work3): " + similarity);
		Assert.assertEquals(true, (int) similarity > 60);
	}

	private Work createWork(String title, String source, int year, WorkType work_type) {
		WorkTitle work_title = new WorkTitle();
		work_title.setTitle(title);

		JournalTitle journal_title = new JournalTitle();
		journal_title.setContent(source);

		Year _year = new Year();
		_year.setValue(Integer.toString(year));
		FuzzyDate publication_date = new FuzzyDate();
		publication_date.setYear(_year);

		Work work = new Work();
		work.setTitle(work_title);
		work.setJournalTitle(journal_title);
		work.setPublicationDate(publication_date);
		work.setType(work_type);

		return work;
	}
}
