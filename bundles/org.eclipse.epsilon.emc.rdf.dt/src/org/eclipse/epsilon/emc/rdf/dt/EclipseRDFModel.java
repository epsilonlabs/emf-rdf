package org.eclipse.epsilon.emc.rdf.dt;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.epsilon.emc.rdf.RDFModel;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;

public class EclipseRDFModel extends RDFModel {
	
	@Override
	protected void loadModel() throws EolModelLoadingException { 
		// Change any platform:/ URLs to file:/ URLs in these lists...
		System.err.println("EclipseRDFModel");
		schemaURIs.forEach(e -> System.out.println(e));
		dataURIs.forEach(e -> System.out.println(e));
		
		examplePlatformToFile();
		
		// Call the RDFModel load as normal with File URLs
		super.loadModel();
		
	}
	
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
