package pt.ptcris.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

/**
 * Normalise IDs, check if IDs follow a pattern and if so normalise the value. 
 * IDs included: issn, isbn, doi, arxiv and rrid.
 * Other IDs were not included since there was no effective changes. 
*/
public final class ExternalIdsNormalizer {

	  private static final Pattern patternISSN = Pattern.compile("(?:^|[^\\d])(\\d{4}\\ {0,1}[-–]{0,1}\\ {0,1}\\d{3}[\\dXx])(?:$|[^-\\d])");
	  
	  private static final Pattern patternISBN = Pattern.compile("([0-9][-0-9 ]{8,15}[0-9xX])(?:$|[^0-9])");
	  
	  private static final Pattern patternDOI = Pattern.compile("(10(\\.[0-9a-zA-Z]+)+\\/(?:(?![\"&\\'])\\S)+)");
	  
	  private static final Pattern patternARXIV = Pattern.compile("(?:(?i)arXiv:)?(\\d{4}\\.\\d{4,5}(v\\d)?|[\\w-\\.]+\\/\\d{7}(v\\d)?)\\b");
	  
	  private static final Pattern patternRRID = Pattern.compile("(?:(?i)RRID:)?(AB_\\d{6}|CVCL_[0-9A-Z]{4}|SCR_\\d{6}|IMSR_JAX\\:\\d{6}|Addgene_\\d{5}|SAMN\\d{8}|MMRRC_\\d{6}-UCD)");
	  
	  /**
	   * Function to normalise IDs based on ID type. 
	   * If value don't match ID type pattern then value is returned without any change. 
	   * @param type 
	   * 	string that represents ID type (eg. doi; issn)
	   * @param value 
	   * 	string with ID value (eg. 1234-2345)
	  */
	  public static String normaliseId(String type, String value) {
		  String valueNormalized = value;
		  switch (type) {
		    case "issn":
		      valueNormalized = normaliseISSN(value);
		      break;
		    case "isbn":
		    	valueNormalized = normaliseISBN(value);
		      break;
		    case "doi":
		    	valueNormalized = normaliseDOI(value);
		      break;
		    case "arxiv":
		    	valueNormalized = normaliseARXIV(value);
		      break;
		    case "rrid":
		    	valueNormalized = normaliseRRID(value);
		      break;  
		  }
		  return valueNormalized;
	  }
	  
	  private static String normaliseISBN(String value) {
	        Matcher m = patternISBN.matcher(value);
	        if (m.find()){
	            String n = m.group(1);
	            if (n != null){
	                n = n.replace("-", "");
	                n = n.replace(" ", "");
	                n = n.replace("x", "X");
	                if (n.length() == 10 || n.length() == 13)
	                    return n;
	            }
	        }
	        return value;
	  }
	  
	  private static String normaliseISSN(String value) {
	        Matcher m = patternISSN.matcher(value);
	        if (m.find()){
	            String n = m.group(1);
	            if (n != null){
	                n = n.replace(" ", "");
	                n = n.replace("-", "");
	                n = n.replace("–", "");
	                n = n.replace("x", "X");
	                n= n.substring(0,4) +"-"+n.substring(4,8);
	                return n;
	            }
	        }
	        return value;
	  }
	  
	  private static String normaliseDOI(String value) {
	        //could be html escaped, and more than once!
	        if (value.contains("&") && value.contains(";")){            
	            int length = 0;
	            do {
	                length = value.length();
	                value = StringEscapeUtils.unescapeXml(value);               
	            }while (value.length() < length);                
	        }
	        
	        Matcher m = patternDOI.matcher(value);
	        if (m.find()){
	            String n = m.group(1);
	            if (n != null){
	                return n;
	            }
	        }
	        return value;
	  }
	  
	  private static String normaliseARXIV(String value) {
	        Matcher m = patternARXIV.matcher(value);
	        if (m.find()){
	            String n = m.group(1);
	            if (n != null){
	                return "arXiv:"+n;
	            }
	        }
	        return value;
	  }
	  
	  private static String normaliseRRID(String value) {
	        Matcher m = patternRRID.matcher(value);
	        if (m.find()){
	            String n = m.group(1);
	            if (n != null){
	                return "RRID:"+n;
	            }
	        }
	        return value;
	  }
	  
	  
}
