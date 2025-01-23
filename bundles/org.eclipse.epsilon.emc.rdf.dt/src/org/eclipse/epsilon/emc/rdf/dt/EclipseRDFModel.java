package org.eclipse.epsilon.emc.rdf.dt;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
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
				System.err.println(e);
			}
		});
		urlList.clear();
		urlList.addAll(goodUrls);
	}
	

	// Will attempt to resolve a String to a URI and then URL, gets the file system path and returns it
	// A File:/ URL is unchanged by this process, Platform:/ URLs become File:/ URLs
	private String processPlatformURLtoFileUrl(String urlString) throws EolModelLoadingException {
		URI fileUri = URI.create(urlString);
		
		URL fileUrl = null;
		try {
			fileUrl = fileUri.toURL();		
		} catch (Exception ex) {
			throw new EolModelLoadingException(ex, this);
		}
		
		// Only transform platform to file urls, return all others unchanged
		if (fileUrl.getProtocol().contentEquals("platform"))
		{
			URL fileSystemPathUrl = null;		
			try {
				fileSystemPathUrl = FileLocator.toFileURL(fileUrl);
			} catch (Exception ex) {
				throw new EolModelLoadingException(ex, this);
			}
			
			try {
				return fileSystemPathUrl.toString();
			} catch (Exception ex) {
				throw new EolModelLoadingException(ex, this);	
			}
		}
		else
		{
			return urlString;
		}
	}

	// Handy way to show the lists on the console
	private void showList (String listName, List<String> list) {
		System.out.println(listName + ": ");
		list.forEach(e -> System.out.println(e));
	}

}
