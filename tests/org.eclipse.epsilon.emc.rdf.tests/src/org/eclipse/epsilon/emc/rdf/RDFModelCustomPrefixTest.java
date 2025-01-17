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
import static org.junit.Assert.assertNotNull;

import java.util.Collection;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.execute.context.EolContext;
import org.eclipse.epsilon.eol.execute.introspection.IPropertyGetter;
import org.junit.Before;
import org.junit.Test;

public class RDFModelCustomPrefixTest {

	private static final String SPIDERMAN_TTL = "resources/spiderman.ttl";

	private EolContext context;

	@Before
	public void setup() {
		this.context = new EolContext();
	}

	@Test(expected=IllegalArgumentException.class)
	public void emptyString() throws Exception {
		try (RDFModel model = new RDFModel()) {
			StringProperties props = new StringProperties();
			props.put(RDFModel.PROPERTY_DATA_URIS, SPIDERMAN_TTL);
			props.put(RDFModel.PROPERTY_PREFIXES, "");
			model.load(props);

			model.getAllOfType("custom:Person");
		}
	}

	@Test
	public void customOnePrefix() throws Exception {
		try (RDFModel model = new RDFModel()) {
			model.setDataUri(SPIDERMAN_TTL);

			StringProperties props = new StringProperties();
			props.put(RDFModel.PROPERTY_DATA_URIS, SPIDERMAN_TTL);
			props.put(RDFModel.PROPERTY_PREFIXES, "custom=http://xmlns.com/foaf/0.1/");
			model.load(props);

			Collection<RDFModelElement> people = model.getAllOfType("custom:Person");
			assertEquals("Two people should be found using the custom prefix", 2, people.size());
		}
	}

	@Test
	public void customTwoPrefixes() throws Exception {
		try (RDFModel model = new RDFModel()) {
			model.setDataUri(SPIDERMAN_TTL);
			model.getCustomPrefixesMap().put("f", "http://xmlns.com/foaf/0.1/");
			model.getCustomPrefixesMap().put("r", "http://www.perceive.net/schemas/relationship/");
			model.load();

			Collection<RDFModelElement> people = model.getAllOfType("f:Person");
			assertEquals("Two people should be found using the custom prefix", 2, people.size());

			RDFModelElement element = people.iterator().next();
			IPropertyGetter pGetter = model.getPropertyGetter();
			Object related = pGetter.invoke(element, "r:enemyOf", context);
			assertNotNull("It should be possible to use the second prefix to fetch a relationship", related);
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void missingEquals() throws Exception {
		try (RDFModel model = new RDFModel()) {
			StringProperties props = new StringProperties();
			props.put(RDFModel.PROPERTY_DATA_URIS, SPIDERMAN_TTL);
			props.put(RDFModel.PROPERTY_PREFIXES, "missingEqualsSide");
			model.load(props);
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void missingPrefix() throws Exception {
		try (RDFModel model = new RDFModel()) {
			StringProperties props = new StringProperties();
			props.put(RDFModel.PROPERTY_DATA_URIS, SPIDERMAN_TTL);
			props.put(RDFModel.PROPERTY_PREFIXES, "=foo");
			model.load(props);
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void missingNamespaceURI() throws Exception {
		try (RDFModel model = new RDFModel()) {
			StringProperties props = new StringProperties();
			props.put(RDFModel.PROPERTY_DATA_URIS, SPIDERMAN_TTL);
			props.put(RDFModel.PROPERTY_PREFIXES, "foo=");
			model.load(props);
		}
	}

}
