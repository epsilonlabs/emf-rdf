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

import java.util.Collection;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.execute.context.EolContext;
import org.junit.After;
import org.junit.Test;

public class RDFModelOWLReasonerTest {

	private static final String OWL_DEMO_DATAMODEL = "resources/OWL/owlDemoData.rdf";
	private static final String OWL_DEMO_SCHEMAMODEL = "resources/OWL/owlDemoSchema.rdf";

	private static final String LANGUAGE_PREFERENCE_EN_STRING = "en";
	private static final String URI_BIGNAME42 = "urn:x-hp:eg/bigName42";

	private RDFModel model;
	private EolContext context;

	@After
	public void teardown() {
		if (model != null) {
			model.dispose();
		}
	}

	@Test
	public void loadModelDefaultDataSchemaLangPref() throws EolModelLoadingException {
		loadModelDefaults();
		assertTrue(model != null);
	}
	
	@Test
	public void listModelOntClasses() {
		loadModelDefaults();
		Collection<RDFModelElement> classList = model.getOntClasses();
		assertEquals("Model should have the expected number of OntClasses", 42, classList.size());
	}
	
	@Test
	public void listModelOntProperties() {
		loadModelDefaults();
		Collection<RDFModelElement> ontPropList = model.getOntProperties();
		assertEquals("Model should have the expected number of OntProperties", 33, ontPropList.size());
	}

	@Test
	public void listModelRestictions() {
		loadModelDefaults();
		Collection<RDFModelElement> restrictionList = model.getRestriction();
		assertEquals("Model should have the expected number of Restrictions", 3, restrictionList.size());
	}

	@Test
	public void getMotherBoard() {
		loadModelDefaults();
		RDFResource element = model.getElementById(URI_BIGNAME42);
		Object motherBoard = element.getProperty("eg:hasMotherBoard", context);
		assertTrue("hasMotherBoard has max cardinality of 1 should only have that value returned ",
			motherBoard instanceof RDFResource);
	}

	// TODO need a similar test to getMotherBoard but for the scenario where null should be returned (i.e. you have no motherboard)

	// TODO need a test that would issue the warning

	// Functions not tests

	protected void loadModel(String dataModelUri, String schemaModelUri, String languagePreference) throws EolModelLoadingException {
		this.model = new RDFModel();
		StringProperties props = new StringProperties();
		props.put(RDFModel.PROPERTY_DATA_URIS, dataModelUri);
		props.put(RDFModel.PROPERTY_SCHEMA_URIS, schemaModelUri);
		props.put(RDFModel.PROPERTY_LANGUAGE_PREFERENCE, languagePreference);
		model.load(props);

		this.context = new EolContext();
	}

	protected void loadModelDefaults() {
		try {
			loadModel(OWL_DEMO_DATAMODEL, OWL_DEMO_SCHEMAMODEL, LANGUAGE_PREFERENCE_EN_STRING);
		} catch (EolModelLoadingException e) {
			System.out.println("default Load Model failed");
			e.printStackTrace();
		}
	}

}
