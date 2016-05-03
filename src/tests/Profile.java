package tests;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import pt.ptcris.handlers.ProgressHandler;
import java.util.logging.Logger;

public class Profile {
	
	private static Logger logger = Logger.getLogger(Tester.class.getName());
	public ProgressHandler progressHandler;
	
	public String serviceSourceName;
	// An account that has provided read-limited and activities-update access to this source.
	// Full access to an account is granted through read-limited, activities-update and bio-update.
	public String orcidID;
	public String accessToken;
    
	
    public Profile(String orcidID, String accessToken, String source){
    	this.orcidID = orcidID;
    	this.accessToken = accessToken;
    	this.serviceSourceName = source;
    }
    
    public ProgressHandler handler(){
    	ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter());
		handler.setLevel(Level.ALL);
		logger.setLevel(Level.ALL);
		logger.addHandler(handler);
		Tester progressHandler = new Tester();
		
		return progressHandler;
    }
	
}
