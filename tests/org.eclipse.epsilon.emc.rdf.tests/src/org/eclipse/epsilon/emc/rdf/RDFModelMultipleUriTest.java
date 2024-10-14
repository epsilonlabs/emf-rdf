package org.eclipse.epsilon.emc.rdf;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RDFModelMultipleUriTest {

	private static final String SPIDERMAN_TTL = "resources/spiderman.ttl";
	private static final String FOAF_RDFXML = "resources/foaf.rdfxml";

	@Test
	public void classIsAvailable() throws Exception {
		try (RDFModel model = new RDFModel()) {
			model.getUris().add(SPIDERMAN_TTL);
			model.getUris().add(FOAF_RDFXML);
			model.load();

			assertEquals("The FOAF vocabulary has 13 classes", 13, model.getAllOfType("Class").size());
		}
	}

}
