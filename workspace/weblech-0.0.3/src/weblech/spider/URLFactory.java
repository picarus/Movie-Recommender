package weblech.spider;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringEscapeUtils;

public abstract class URLFactory {
	
	public static URL createURL(URL sourceURL, String urlStr) throws MalformedURLException {
		
		URL u;
		
		if (urlStr.contains("&"))
			urlStr=StringEscapeUtils.unescapeHtml4(urlStr);
		
		if (sourceURL.getPath().startsWith("/D:/")) { // is a file
			if (!urlStr.startsWith("/") && !urlStr.startsWith("http:") && !urlStr.equals(""))
				urlStr="/en/"+urlStr;
			sourceURL= new URL("http://www.filmaffinity.com");
		}
			
		u = new URL(sourceURL, urlStr);
		
		return u;
	}
}