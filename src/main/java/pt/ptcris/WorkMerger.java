package pt.ptcris;

import org.um.dsi.gavea.orcid.model.work.Work;

public abstract class WorkMerger {

	abstract boolean matched(Work w1, Work w2);
	
	abstract float similarity(Work w1, Work w2);
	
	abstract float threshold();

	
	
}
