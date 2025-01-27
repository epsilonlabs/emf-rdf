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

import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.rdf.dt.EclipseRDFModel;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.execute.context.EolContext;
import org.junit.After;
import org.junit.Test;

public class EclipseRDFModelUrlTest extends EclipseProjectEnvTest {

	public EclipseRDFModelUrlTest() {
		super(PROJECT_URL);
	}

	private static final String PROJECT_URL = "myProject";

	private static final String OWL_DEMO_DATAMODEL = "/resources/OWL/owlDemoData.ttl";
	private static final String OWL_DEMO_SCHEMAMODEL = "/resources/OWL/owlDemoSchema.ttl";
	private static final String LANGUAGE_PREFERENCE_EN_STRING = "en";
	
	private static final String URI_BIGNAME42 = "urn:x-hp:eg/bigName42";
	private static final String URI_ALIENBOX51 = "urn:x-hp:eg/alienBox51";
	private static final String URI_WHITEBOX = "urn:x-hp:eg/whiteBoxZX";

	private EclipseRDFModel model;
	private EolContext context;

	//
	// NO ERRORS EXPECTED
	//
	
	@Test
	public void relativePathModelLoad() throws EolModelLoadingException {
		String dataUrl = new String("." + OWL_DEMO_DATAMODEL);
		String schemaUrl = new String("." + OWL_DEMO_SCHEMAMODEL);
		StringProperties props = createPropertyString(dataUrl, schemaUrl, LANGUAGE_PREFERENCE_EN_STRING);

		this.context = new EolContext();
		copyModelFiles();
		model.load(props);
	}

	@Test
	public void relativeFileUrlModelLoad() throws EolModelLoadingException {
		String dataUrl = new String("file:." + OWL_DEMO_DATAMODEL);
		String schemaUrl = new String("file:." + OWL_DEMO_SCHEMAMODEL);
		StringProperties props = createPropertyString(dataUrl, schemaUrl, LANGUAGE_PREFERENCE_EN_STRING);

		this.context = new EolContext();
		copyModelFiles();
		model.load(props);
		loadedModelTest();
	}

	@Test
	public void longFileUrlModelLoad() throws EolModelLoadingException {
		String dataUrl = new String(getTestProjectURIString() + OWL_DEMO_DATAMODEL);
		String schemaUrl = new String(getTestProjectURIString() + OWL_DEMO_SCHEMAMODEL);
		StringProperties props = createPropertyString(dataUrl, schemaUrl, LANGUAGE_PREFERENCE_EN_STRING);

		this.context = new EolContext();
		copyModelFiles();
		model.load(props);
		loadedModelTest();

	}

	@Test
	public void platformUrlModelLoad() throws EolModelLoadingException {
		String dataUrl = new String("platform:/resource/" + PROJECT_URL + "/" + OWL_DEMO_DATAMODEL);
		String schemaUrl = new String("platform:/resource/" + PROJECT_URL + "/" + OWL_DEMO_SCHEMAMODEL);
		StringProperties props = createPropertyString(dataUrl, schemaUrl, LANGUAGE_PREFERENCE_EN_STRING);

		this.context = new EolContext();
		copyModelFiles();

		model.load(props);
		loadedModelTest();
	}

	//
	//  EXPECTED ERRORS!
	//
	
	@Test
	public void missingResourceInPlatformUrlModelLoad() {
		String dataUrl = new String("platform:/-/" + PROJECT_URL + "/" + OWL_DEMO_DATAMODEL);
		String schemaUrl = new String("platform:/-/" + PROJECT_URL + "/" + OWL_DEMO_SCHEMAMODEL);
		StringProperties props = createPropertyString(dataUrl, schemaUrl, LANGUAGE_PREFERENCE_EN_STRING);

		this.context = new EolContext();
		copyModelFiles();
		
		try {
			model.load(props);
		} catch (EolModelLoadingException e) {
			System.err.println("Test internal message: " + e.getInternal().getMessage());
			assertEquals("Error whilst loading model null: No file path has been set", e.getMessage());
		}
	}

	@Test
	public void missingProjectUrlInPlatformUrlModelLoad() {
		String dataUrl = new String("platform:/resource" + OWL_DEMO_DATAMODEL);
		String schemaUrl = new String("platform:/resource" + OWL_DEMO_SCHEMAMODEL);
		StringProperties props = createPropertyString(dataUrl, schemaUrl, LANGUAGE_PREFERENCE_EN_STRING);

		this.context = new EolContext();
		copyModelFiles();
		
		try {
			model.load(props);
		} catch (EolModelLoadingException e) {
			System.err.println("Test internal message: " + e.getInternal().getMessage());
			assertEquals("Error whilst loading model null: No file path has been set",e.getMessage());
		}
	}

	@After
	public void teardown() {
		if (model != null) {
			model.dispose();
		}
	}
	
	private void loadedModelTest() {		
		assertTrue(model != null);
		RDFResource element = model.getElementById(URI_WHITEBOX);
		Object motherBoard = element.getProperty("eg:motherBoard", context);
		assertTrue("motherBoard has max cardinality of 1 should only have that value returned ",
			motherBoard instanceof RDFResource);
	}

	private void copyModelFiles() {
		try {
			super.copyIntoProject(OWL_DEMO_DATAMODEL);
			super.copyIntoProject(OWL_DEMO_SCHEMAMODEL);
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	protected StringProperties createPropertyString(String dataModelUri, String schemaModelUri,
			String languagePreference) {
		this.model = new EclipseRDFModel();
		StringProperties props = new StringProperties();
		props.put(RDFModel.PROPERTY_DATA_URIS, dataModelUri);
		props.put(RDFModel.PROPERTY_SCHEMA_URIS, schemaModelUri);
		props.put(RDFModel.PROPERTY_LANGUAGE_PREFERENCE, languagePreference);
		return props;
	}
}
