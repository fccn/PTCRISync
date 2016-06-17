package pt.ptcris;

import org.um.dsi.gavea.orcid.model.error.Error;

public class ORCIDException extends Exception {

	private static final long serialVersionUID = 1L;

	public final Error e;
	
	public ORCIDException(Error e) {
		super(e.getDeveloperMessage());
		this.e = e;
	}

}
