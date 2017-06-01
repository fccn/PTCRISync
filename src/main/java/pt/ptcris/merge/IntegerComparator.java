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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import pt.ptcris.PTCRISGrouper;

/**
 * An integer comparator, shows that activities can be generic objects.
 *
 */
public class IntegerComparator extends ActivityComparator<Integer> {

	@Override
	public int threshold() {
		return 90;
	}

	@Override
	public float compare(Integer i1, Integer i2) {
		float res =  Math.abs(i1-i2);
		res = res / ((i1+i2)/2);
		res = 100*(1-res);
		return res;
	}
	
	public static void main(String[] args) {
		Integer[] is = {50, 70, 75, 51, 73, 42};
		
		List<ActivityGroup<Integer>> groups = PTCRISGrouper.group(Arrays.asList(is), new IntegerComparator());
		
		assertEquals(groups.size(), 3);
	}

}
