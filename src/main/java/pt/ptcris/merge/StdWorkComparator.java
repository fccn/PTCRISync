package pt.ptcris.merge;

import java.util.HashSet;
import java.util.Set;

import org.um.dsi.gavea.orcid.model.common.ExternalId;
import org.um.dsi.gavea.orcid.model.work.WorkSummary;

public class StdWorkComparator extends ActivityComparator<WorkSummary> {

	@Override
	int threshold() {
		return 0;
	}

	@Override
	float compare(WorkSummary w1, WorkSummary w2) {
		Set<ExternalId> eids1 = new HashSet<ExternalId>();
		for (ExternalId i : w1.getExternalIds().getExternalId())
			eids1.add(i);
		// TODO Auto-generated method stub
		return 0;
	}

}
