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

		processEclipsePlatformUrlsInList(schemaURIs);
		processEclipsePlatformUrlsInList(dataURIs);
		
		// Call the RDFModel load as normal with only File:/ URLs in the lists
		super.loadModel();
	}
	
	// Pushes a list of URLs through a process to turn any Platform:/ into File:/ which Jena can use
	private void processEclipsePlatformUrlsInList(List<String> urlList) {
		List<String> goodUrls = new ArrayList<String>();
		
		urlList.forEach(u -> {
			String newUrlString = processPlatformURLtoFileUrl(u);
			if (newUrlString != null) {
				goodUrls.add(newUrlString);
			}
		});
		urlList.clear();
		urlList.addAll(goodUrls);
	}
	

	// Will attempt to resolve a String to a URI and then URL, gets the file system path and returns it
	// A File:/ URL is unchanged by this process, Platform:/ URLs become File:/ URLs
	private String processPlatformURLtoFileUrl(String urlString) {
		
		URI fileUri = URI.create(urlString);
		
		URL fileUrl = null;
		try {
			fileUrl = fileUri.toURL();		
			// System.err.println("[OK] fileUrl: " + fileUrl);
		} catch (MalformedURLException e) {
			//e.printStackTrace();
			System.err.println("fileUri.toURL() - " + urlString);
		}
		
		URL fileSystemPathUrl = null;		
		try {
			fileSystemPathUrl = FileLocator.toFileURL(fileUrl);
			// System.err.println("[OK] fileSystemPathUrl: " + fileSystemPathUrl);
		} catch (IOException e) {
			//e.printStackTrace();
			System.err.println("FileLocator.toFileURL(fileUrl); - " + fileUrl);
		}
		
		if (null != fileSystemPathUrl) {
			return fileSystemPathUrl.toString();
		} else {
			return null;
		}
	}

	// Handy way to show the lists on the console
	private void showList (String listName, List<String> list) {
		System.out.println(listName + ": ");
		list.forEach(e -> System.out.println(e));
	}
	
	
	// Stuff to delete later
	
	private static final String PLATFORM_URL_OWLDATA = "platform:/resource/org.eclipse.epsilon.examples.emc.rdf.OWLdata/owlDemoData.ttl";
	private static final String PLATFORM_URL_OWLSCHEMA = "platform:/resource/org.eclipse.epsilon.examples.emc.rdf.OWLdata/owlDemoData.ttl";
	
	private void examplePlatformToFile() {
		System.out.println(Platform.getLocation());
		
		URI fileUri = URI.create(PLATFORM_URL_OWLDATA);
		
		URL fileUrl = null;
		try {
			fileUrl = fileUri.toURL();
				
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.err.println(readOnLoad);
		}
		
		URL fileSystemPathUrl = null;		
		try {
			fileSystemPathUrl = FileLocator.toFileURL(fileUrl);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		System.out.println("platform URL: " + PLATFORM_URL_OWLDATA);
		System.out.println("PATH: " + fileSystemPathUrl);
	}
}
