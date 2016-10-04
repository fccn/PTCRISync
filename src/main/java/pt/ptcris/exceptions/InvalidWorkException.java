package pt.ptcris.exceptions;

import java.util.Set;

public class InvalidWorkException extends Exception {	
	private static final long serialVersionUID = 1L;
	private static Set<String> invalidResultTypes;

	public InvalidWorkException() {
        // TODO Auto-generated constructor stub
    }

    public InvalidWorkException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public InvalidWorkException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

    public InvalidWorkException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

	public InvalidWorkException(Set<String> res) {
		// TODO Auto-generated constructor stub
		super(res.toString());
		invalidResultTypes = res; 
	}
	
	public Set<String> getInvalidTypes() {
		// TODO Auto-generated method stub
		return invalidResultTypes;
	}
}
