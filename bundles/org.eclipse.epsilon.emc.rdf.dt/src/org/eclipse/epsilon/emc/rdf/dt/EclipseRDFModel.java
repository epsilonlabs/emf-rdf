package org.eclipse.epsilon.emc.rdf.dt;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.epsilon.emc.rdf.RDFModel;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;

public class EclipseRDFModel extends RDFModel {
	
	@Override
	protected void loadModel() throws EolModelLoadingException { 
		// Change any platform:/ URLs to file:/ URLs in these lists...
		processEclipsePlatformUrlsToFileUrls(schemaURIs);
		processEclipsePlatformUrlsToFileUrls(dataURIs);

		// Call the RDFModel load as normal, no platform URLs are passed to Jena
		super.loadModel();
	}
	
	// Pushes a list of URLs through a process to turn any Platform:/ into File:/ 
	private void processEclipsePlatformUrlsToFileUrls(List<String> urlList) {
		List<String> goodUrls = new ArrayList<String>();
		urlList.forEach(u -> {
			String newUrlString = null;
			try {
				newUrlString = processPlatformURLtoFileUrl(u);
				goodUrls.add(newUrlString);
			} catch (EolModelLoadingException e) {
				// System.err.println(e);
			}
		});
		urlList.clear();
		urlList.addAll(goodUrls);
	}	

	// A File:/ URL or relative path starting '/' or '.' is unchanged by this process, Platform:/ URLs become File:/ URLs
	// Attempts to resolve a String to a URI and then URL, then gets the File:/ URL and returns it
	private String processPlatformURLtoFileUrl(String urlString) throws EolModelLoadingException {

		// Any URLS starting . / are possibly relative paths, they should work with Jena if correct
		if ((urlString.startsWith(".")) | (urlString.startsWith("/"))) {
			return urlString;
		}

		URI fileUri = URI.create(urlString);
		try {
			URL fileUrl = fileUri.toURL();
			URL fileSystemPathUrl = FileLocator.toFileURL(fileUrl);
			return fileSystemPathUrl.toString();
		} catch (Exception ex) {
			System.err.println("Error processing URL: " + urlString);
			throw new EolModelLoadingException(ex, this);
		}
	}

	// Handy way to show the lists on the console
	/*
	private void showList (String listName, List<String> list) {
		System.out.println(listName + ": ");
		list.forEach(e -> System.out.println(e));
	}
	*/

}
