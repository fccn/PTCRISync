package pt.ptcris;

import org.orcid.jaxb.model.error_rc2.OrcidError;

public class ORCIDException extends Exception {

	private static final long serialVersionUID = 1L;

	public final OrcidError e;
	
	public ORCIDException(OrcidError e) {
		super(e.getDeveloperMessage());
		this.e = e;
	}

}
