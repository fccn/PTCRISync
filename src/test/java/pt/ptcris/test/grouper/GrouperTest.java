/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.test.grouper;

import static org.junit.Assert.*;

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
		Work work2 = TestHelper.workDOIEID(BigInteger.valueOf(2), "1", "0", "0");
		Work work3 = TestHelper.workDOIEID(BigInteger.valueOf(3), "1", "1", "0");
		Work work4 = TestHelper.workDOIEID(BigInteger.valueOf(4), "1", "4", "3");
		Work work5 = TestHelper.workDOIEID(BigInteger.valueOf(5), "1", "4", "5");
		Work work6 = TestHelper.work(BigInteger.valueOf(6), "1");
		Work work7 = TestHelper.work(BigInteger.valueOf(7), "2");
		
		List<Work> works = new ArrayList<Work>();
		works.add(work1);
		works.add(work2);
		works.add(work3);
		works.add(work4);
		works.add(work5);
		works.add(work6);
		works.add(work7);
		
		List<ActivityGroup<Work>> groups = PTCRISGrouper.group(works, new StdWorkComparator());
		
		assertEquals(groups.size(), 4);
		
		return;
	}
}
