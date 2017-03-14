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

import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.utils.ExternalIdsDiff;

/**
 * A work comparator whose behavior should be the same as the standard ORCID
 * grouper: works are considered similar if they share any external identifier.
 *
 */
public class StdWorkComparator extends ActivityComparator<Work> {

	@Override
	int threshold() {
		return 0;
	}

	@Override
	float compare(Work w1, Work w2) {
		ExternalIdsDiff diff = new ExternalIdsDiff(
				w1.getExternalIds(),
				w2.getExternalIds());
		return diff.same.size();
	}

}
