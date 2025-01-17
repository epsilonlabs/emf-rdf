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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.execute.context.EolContext;
import org.junit.After;
import org.junit.Test;

public class RDFModelOWLReasonerTest {

	private static final String OWL_DEMO_DATAMODEL = "resources/OWL/owlDemoData.ttl";
	private static final String OWL_DEMO_SCHEMAMODEL = "resources/OWL/owlDemoSchema.ttl";

	private static final String LANGUAGE_PREFERENCE_EN_STRING = "en";
	private static final String URI_BIGNAME42 = "urn:x-hp:eg/bigName42";
	private static final String URI_ALIENBOX51 = "urn:x-hp:eg/alienBox51";
	
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
	public void getMotherBoardTest() {
		loadModelDefaults();
		RDFResource element = model.getElementById(URI_BIGNAME42);
		Object motherBoard = element.getProperty("eg:motherBoard", context);
		assertTrue("motherBoard has max cardinality of 1 should only have that value returned ",
			motherBoard instanceof RDFResource);
	}

	// TODO Review this test with Antonio.
	// Proposed test: need a similar test to getMotherBoard but for the scenario where null should be returned (i.e. you have no motherboard)
	// Getting an empty list back, because there is not motherboard there is also no max cardinality which could be evaluated to 1 and thus a single value or null.
	
	@Test
	public void getPropertyThatDoesNotExistAsNullTest() {
		loadModelDefaults();
		RDFResource element = model.getElementById(URI_ALIENBOX51);
		Object motherBoard = element.getProperty("eg:motherBoard", context);
		Collection<RDFResource> listMotherBoards = (Collection <RDFResource>) motherBoard;
		//assertTrue("URI_ALIENBOX51 computer does not have motherBoard " + motherBoard, motherBoard == null);
		assertTrue("URI_ALIENBOX51 computer does not have motherBoard ", listMotherBoards.size() == 0);
	}
	
	@Test
	public void getMotherBoardTestIssuesWarning() throws IOException {
		ByteArrayOutputStream errors = new ByteArrayOutputStream(); 
		System.setErr(new PrintStream(errors));
		loadModelDefaults();
		RDFResource element = model.getElementById(URI_BIGNAME42);
		Object motherBoard = element.getProperty("eg:motherBoard", context);
		assertTrue("An error should be raised for max cardinality being raised ", errors.toString().contains("has a max cardinality 1, raw property values list contained"));
	}

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
