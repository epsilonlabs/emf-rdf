/********************************************************************************
 * Copyright (c) 2024 University of York
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Antonio Garcia-Dominguez - initial API and implementation
 ********************************************************************************/
package org.eclipse.epsilon.emc.rdf.dt;

import java.net.URL;
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
	private void processEclipsePlatformUrlsToFileUrls(List<String> urlList) throws EolModelLoadingException {
		for (int i = 0; i < urlList.size(); i++) {
			urlList.set(i, processPlatformURLtoFileUrl(urlList.get(i)));
		}
	}

	// A File:/ URL or relative path starting '/' or '.' is unchanged by this process, Platform:/ URLs become File:/ URLs
	// Attempts to resolve a String to a URI and then URL, then gets the File:/ URL and returns it
	private String processPlatformURLtoFileUrl(String urlString) throws EolModelLoadingException {
		if (!urlString.startsWith("platform:")) {
			return urlString;
		}

		try {
			URL url = new URL(urlString);
			URL fileSystemPathUrl = FileLocator.toFileURL(url);
			return fileSystemPathUrl.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new EolModelLoadingException(ex, this);
		}
	}

}
