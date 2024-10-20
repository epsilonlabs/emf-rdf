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

import java.io.File;

import org.junit.Test;

public class RDFModelMultipleUriTest {

	private static final String SPIDERMAN_TTL = "resources/spiderman.ttl";
	private static final String FOAF_RDFXML = "resources/foaf.rdfxml";

	@Test
	public void classIsAvailable() throws Exception {
		try (RDFModel model = new RDFModel()) {
			model.getUris().add(getAbsoluteURI(SPIDERMAN_TTL));
			model.getUris().add(getAbsoluteURI(FOAF_RDFXML));
			model.load();

			assertEquals("The FOAF vocabulary has 13 classes", 13, model.getAllOfType("Class").size());
		}
	}

	private String getAbsoluteURI(String path) {
		return new File(path).getAbsoluteFile().toURI().toString();
	}
}
