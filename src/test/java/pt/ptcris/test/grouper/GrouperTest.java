package pt.ptcris.test.grouper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.PTCRISGrouper;
import pt.ptcris.merge.ActivityGroup;
import pt.ptcris.merge.StdWorkComparator;
import pt.ptcris.test.TestHelper;

public class GrouperTest {
	public static void main(String[] args) {
		Work work1 = TestHelper.workDOIEID(BigInteger.valueOf(1), "1", "1", "1");
		Work work2 = TestHelper.workDOIEID(BigInteger.valueOf(1), "1", "0", "0");
		Work work3 = TestHelper.workDOIEID(BigInteger.valueOf(1), "1", "1", "0");
		Work work4 = TestHelper.workDOIEID(BigInteger.valueOf(1), "1", "4", "3");
		Work work5 = TestHelper.workDOIEID(BigInteger.valueOf(1), "1", "4", "5");
		
		List<Work> works = new ArrayList<Work>();
		works.add(work1);
		works.add(work2);
		works.add(work3);
		works.add(work4);
		works.add(work5);
		
		List<ActivityGroup<Work>> groups = PTCRISGrouper.group(works, new StdWorkComparator());
		
		return;
	}
}
