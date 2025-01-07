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
package org.eclipse.epsilon.emc.rdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

public class RDFModelMultipleUriTest {

	private static final String SPIDERMAN_TTL = "resources/spiderman.ttl";
	private static final String FOAF_RDFXML = "resources/foaf.rdf";

	@Test
	public void ttlThenRDFXML() throws Exception {
		try (RDFModel model = new RDFModel()) {
			model.getDataUris().add(getAbsoluteURI(SPIDERMAN_TTL));
			model.getDataUris().add(getAbsoluteURI(FOAF_RDFXML));
			model.load();

			assertTrue("The FOAF vocabulary has at least 13 classes (more are inferred)",
					model.getAllOfType("Class").size() >= 13);
		}
	}

	@Test
	public void rdfXMLThenTTL() throws Exception {
		try (RDFModel model = new RDFModel()) {
			model.getDataUris().add(getAbsoluteURI(FOAF_RDFXML));
			model.getDataUris().add(getAbsoluteURI(SPIDERMAN_TTL));
			model.load();

			assertTrue("The FOAF vocabulary has at least 13 classes (more are inferred)",
				model.getAllOfType("Class").size() >= 13);
		}
	}

	private String getAbsoluteURI(String path) {
		return new File(path).getAbsoluteFile().toURI().toString();
	}
}
