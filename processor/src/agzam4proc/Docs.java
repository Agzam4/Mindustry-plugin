package agzam4proc;

import java.util.regex.Pattern;

import arc.struct.ObjectMap;

public class Docs {
	
	public static String desc(String docComment) {
		if (docComment == null || docComment.isEmpty()) {
			return "";
		}
		int tagIndex = findFirstTagIndex(docComment);
		String desc = (tagIndex != -1) ? docComment.substring(0, tagIndex) : docComment;
		return cleanText(desc);
	}
	 
	public static ObjectMap<String, String> parms(String docComment) {
	    ObjectMap<String, String> parms = ObjectMap.of();
	    if (docComment == null || docComment.isEmpty()) {
	        return parms;
	    }
	    var pattern = Pattern.compile("@param\\s+(\\w+)\\s+([^@]+)");
	    var matcher = pattern.matcher(docComment);
	    while (matcher.find()) {
	        String paramName = matcher.group(1).trim();
	        String paramDescription = matcher.group(2);
	        var index = paramDescription.indexOf('-');
	        if(index != -1 && index < paramDescription.length()) paramDescription = paramDescription.substring(index+1);
	        parms.put(paramName, paramDescription.trim());
	    }
	    return parms;
	}
	

    private static int findFirstTagIndex(String docComment) {
        var p = Pattern.compile("(?m)^\\s*@|\\s+@(?=param|return|throws|deprecated|see)");
        var m = p.matcher(docComment);
        return m.find() ? m.start() : -1;
    }

    private static String cleanText(String text) {
        if (text == null) return "";
        return text.replaceAll("(?m)^\\s*\\*\\s*", "") // leading stars "*"
                   .replaceAll("\\s+", " ")            // remove spaces
                   .trim();
    }
}
