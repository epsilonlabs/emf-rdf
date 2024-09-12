package org.eclipse.epsilon.emc.rdf.tests;

import static org.junit.Assert.assertEquals;

import org.eclipse.epsilon.emc.rdf.RDFModel;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.junit.Test;

public class RDFModelTest {

	@Test
	public void listAll() throws EolModelLoadingException {
		try (RDFModel model = new RDFModel()) {
			model.setUri("resources/spiderman.ttl");
			model.load();

			assertEquals("allContents should produce one element per resource", 2, model.allContents().size());
		}
	}

}
