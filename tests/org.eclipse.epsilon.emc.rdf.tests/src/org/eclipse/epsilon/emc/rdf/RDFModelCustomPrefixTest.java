package org.eclipse.epsilon.emc.rdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.rdf.RDFModel;
import org.eclipse.epsilon.emc.rdf.RDFModelElement;
import org.eclipse.epsilon.eol.execute.context.EolContext;
import org.junit.Before;
import org.junit.Test;

public class RDFModelCustomPrefixTest {

	private EolContext context;

	@Before
	public void setup() {
		this.context = new EolContext();
	}

	@Test(expected=IllegalArgumentException.class)
	public void emptyString() throws Exception {
		try (RDFModel model = new RDFModel()) {
			StringProperties props = new StringProperties();
			props.put(RDFModel.PROPERTY_URI, "resources/spiderman.ttl");
			props.put(RDFModel.PROPERTY_PREFIXES, "");
			model.load(props);

			model.getAllOfType("custom:Person");
		}
	}

	@Test
	public void customOnePrefix() throws Exception {
		try (RDFModel model = new RDFModel()) {
			model.setUri("resources/spiderman.ttl");

			StringProperties props = new StringProperties();
			props.put(RDFModel.PROPERTY_URI, "resources/spiderman.ttl");
			props.put(RDFModel.PROPERTY_PREFIXES, "custom=http://xmlns.com/foaf/0.1/");
			model.load(props);

			Collection<RDFModelElement> people = model.getAllOfType("custom:Person");
			assertEquals("Two people should be found using the custom prefix", 2, people.size());
		}
	}

	@Test
	public void customTwoPrefixes() throws Exception {
		try (RDFModel model = new RDFModel()) {
			model.setUri("resources/spiderman.ttl");
			model.getCustomPrefixesMap().put("f", "http://xmlns.com/foaf/0.1/");
			model.getCustomPrefixesMap().put("r", "http://www.perceive.net/schemas/relationship/");
			model.load();

			Collection<RDFModelElement> people = model.getAllOfType("f:Person");
			assertEquals("Two people should be found using the custom prefix", 2, people.size());

			RDFModelElement element = people.iterator().next();
			assertNotNull("It should be possible to use the second prefix to fetch a relationship",
					model.getPropertyGetter().invoke(element, "r:enemyOf", context));
		}
	}

}
