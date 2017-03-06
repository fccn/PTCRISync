package pt.ptcris.merge;

import org.um.dsi.gavea.orcid.model.work.Work;

import pt.ptcris.utils.ExternalIdsDiff;

public class StdWorkComparator extends ActivityComparator<Work> {

	@Override
	int threshold() {
		return 0;
	}

	@Override
	float compare(Work w1, Work w2) {
		ExternalIdsDiff diff = new ExternalIdsDiff(w1.getExternalIds(),
				w2.getExternalIds());
		return diff.same.size();
	}

}
