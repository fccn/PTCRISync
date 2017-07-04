/*
 * Copyright (c) 2016, 2017 PTCRIS - FCT|FCCN and others.
 * Licensed under MIT License
 * http://ptcris.pt
 *
 * This copyright and license information (including a link to the full license)
 * shall be included in its entirety in all copies or substantial portion of
 * the software.
 */
package pt.ptcris.exceptions;

import java.util.Set;

public class InvalidActivityException extends Exception {	
	private static final long serialVersionUID = 1L;
	private static Set<String> invalidResultTypes;

	public InvalidActivityException() {
        // TODO Auto-generated constructor stub
    }

    public InvalidActivityException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public InvalidActivityException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public InvalidActivityException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

	public InvalidActivityException(Set<String> res) {
		// TODO Auto-generated constructor stub
		super(res.toString());
		invalidResultTypes = res; 
	}
	
	public Set<String> getInvalidTypes() {
		// TODO Auto-generated method stub
		return invalidResultTypes;
	}
}
