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

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.execute.context.EolContext;
import org.eclipse.epsilon.eol.execute.introspection.IPropertyGetter;
import org.eclipse.epsilon.eol.execute.operations.contributors.ModelElementOperationContributor;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RDFModelOWLReasonerModelAndSchemaTest {

	private static final String OWL_DEMO_DATAMODEL = "resources/OWL/owlDemoData.rdf";
	private static final String OWL_DEMO_SCHEMAMODEL = "resources/OWL/owlDemoSchema.rdf";

	private static final String LANGUAGE_PREFERENCE_INVALID_STRING = "e n,en-us,123,ja";
	private static final String LANGUAGE_PREFERENCE_JA_STRING = "en,en-us,ja";
	private static final String LANGUAGE_PREFERENCE_EN_STRING = "en";

	private static final String MAXCARDINALITY = "owl:maxCardinality";

	private static final String URI_MOTHERBOARD = "urn:x-hp:eg/MotherBoard";
	private static final String URI_BIGNAME42 = "urn:x-hp:eg/bigName42";
	private static final String URI_BIGNAMESPECIALMB = "urn:x-hp:eg/bigNameSpecialMB";
	private static final String URI_HASMOTHERBOARD = "urn:x-hp:eg/hasMotherBoard";

	private RDFModel model;
	private IPropertyGetter pGetter;
	private EolContext context;

	@After
	public void teardown() {
		if (model != null) {
			model.dispose();
		}
	}

	@Test
	public void loadModelDefaultDataSchemaLangPrefTest() throws EolModelLoadingException {
		loadModelDefaults();
		assertTrue(model != null);
	}
	private static final boolean DISPLAYLISTSOFMODEL = false;
	
	@Test
	public void listModelOntClassesTest() {
		final int EXPECTED_RESULT = 42;
		loadModelDefaults();
		Collection<RDFModelElement> classList = model.listOntClassesInModel();
		assertTrue( "Model should have "+EXPECTED_RESULT+" OntClasses" , classList.size()== EXPECTED_RESULT);
		if (DISPLAYLISTSOFMODEL) {
			System.out.println( "Model OntClasses : " + classList.size());
			classList.forEach(o -> System.out.println(" OntClass " + o));
		}
	}
	
	@Test
	public void listModelOntPropertiesTest() {
		final int EXPECTED_RESULT = 33;
		loadModelDefaults();
		Collection<RDFModelElement> ontPropList = model.listOntPropertiesInModel();
		assertTrue("Model should have " + EXPECTED_RESULT + " OntProperties", ontPropList.size() == EXPECTED_RESULT);
		if (DISPLAYLISTSOFMODEL) {
			System.out.println( "Model OntProperties : " + ontPropList.size());
			ontPropList.forEach(o -> System.out.println(" OntProperty " + o));
		}
	}

	@Test
	public void listModelRestictionsTest() {
		final int EXPECTED_RESULT = 3;
		loadModelDefaults();
		Collection<RDFModelElement> restrictionList = model.listRestrictionInModel();
		assertTrue("Model should have " + EXPECTED_RESULT + " Restrictions", restrictionList.size() == EXPECTED_RESULT);
		if (DISPLAYLISTSOFMODEL) {
			System.out.println("Model Restrictions : " + restrictionList.size());
			restrictionList.forEach(r -> System.out.println(" Restriction " + r));
		}
	}

	@Test
	public void getMotherBoard() {
		loadModelDefaults();

		RDFResource element = model.getElementById(URI_BIGNAME42);
		System.out.println("\nGet the MotherBoard for " + element);
		System.out.println("\nMax Cardinality should mean we get 1 motherboard (possibly random)");
		Collection<Object> motherBoard = element.listPropertyValues("eg:hasMotherBoard", context);
		motherBoard.forEach(motherboard -> {System.out.println(motherboard);});
		
		// Jena provides access to a list of Restrictions that can be found applied to the model 
		//listModelRestictions();

		// These are all the statements about the element selected, note there are some with "generated" URIs - these are inferred by Jena
		//System.out.println("\n" + element.getStatementsString());

	}

	// Functions not tests

	public void loadModel(String dataModelUri, String schemaModelUri, String languagePreference)
			throws EolModelLoadingException {

		this.model = new RDFModel();
		StringProperties props = new StringProperties();
		props.put(RDFModel.PROPERTY_DATA_URIS, dataModelUri);
		props.put(RDFModel.PROPERTY_SCHEMA_URIS, schemaModelUri);
		props.put(RDFModel.PROPERTY_LANGUAGE_PREFERENCE, languagePreference);
		model.load(props);

		this.pGetter = model.getPropertyGetter();
		this.context = new EolContext();
	}

	public void loadModelDefaults() {
		try {
			loadModel(OWL_DEMO_DATAMODEL, OWL_DEMO_SCHEMAMODEL, LANGUAGE_PREFERENCE_EN_STRING);
		} catch (EolModelLoadingException e) {
			System.out.println("default Load Model failed");
			e.printStackTrace();
		}
	}

	public void listModelRestictions() {
		Collection<RDFModelElement> restrictionList = model.listRestrictionInModel();
		System.out.println("\nListing Model Restrictions : " + restrictionList.size());
		restrictionList.forEach(r -> {
			RDFResource res = (RDFResource) r;
			System.out.println("\n" + r + " expresses restrictions as with these statements:\n " + res.getStatementsString());

		});

	}

}
