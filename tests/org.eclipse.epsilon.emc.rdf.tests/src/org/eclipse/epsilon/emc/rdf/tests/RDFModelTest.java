package org.eclipse.epsilon.emc.rdf.tests;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.epsilon.emc.rdf.RDFLiteral;
import org.eclipse.epsilon.emc.rdf.RDFModel;
import org.eclipse.epsilon.emc.rdf.RDFModelElement;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.execute.context.EolContext;
import org.eclipse.epsilon.eol.execute.introspection.IPropertyGetter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RDFModelTest {

	private RDFModel model;

	@Before
	public void setup() throws EolModelLoadingException {
		this.model = new RDFModel();
		model.setUri("resources/spiderman.ttl");
		model.load();
	}

	@After
	public void teardown() {
		if (model != null) {
			model.dispose();
		}
	}

	@Test
	public void listAll() throws EolModelLoadingException {
		assertEquals("allContents should produce one element per resource", 2, model.allContents().size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getNamesWithoutPrefix() throws Exception {
		IPropertyGetter pGetter = model.getPropertyGetter();

		EolContext ctx = new EolContext();
		Set<String> names = new HashSet<>();
		for (RDFModelElement o : model.allContents()) {
			for (RDFLiteral l : (Collection<RDFLiteral>) pGetter.invoke(o, "name", ctx)) {
				names.add((String) pGetter.invoke(l, "value", ctx));
			}
		}

		assertEquals(new HashSet<>(Arrays.asList("Spiderman", "Green Goblin", "Человек-паук")), names);
	}

}
