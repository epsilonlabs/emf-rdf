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

import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.OntResource;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.reasoner.ValidityReport;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.PrintUtil;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.OWL;
import org.junit.Before;
import org.junit.Test;

public class JenaOWLReasonerTest {

	private Model schema;
	private Model data;
	
	private Reasoner reasoner;
	
	private InfModel infmodel;
	private OntModel oInfmodel;
	
	@Before
	public void setup() {
		this.schema = RDFDataMgr.loadModel("file:resources/OWL/owlDemoSchema.rdf");
		this.data = RDFDataMgr.loadModel("file:resources/OWL/owlDemoData.rdf");
		this.reasoner = ReasonerRegistry.getOWLReasoner();
		this.infmodel = ModelFactory.createInfModel(reasoner, data, schema); // A basic RDF Model composed of scheme +
																				// data
		// Slightly longer but applicable when the reasoner will use the same scheme on
		// several data models
		// this.reasoner = reasoner.bindSchema(schema);
		// this.infmodel = ModelFactory.createInfModel(reasoner, data);
	}

	@Test
	public void getResourceNForceFromInfModel() throws Exception {
		Resource nForce = infmodel.getResource("urn:x-hp:eg/nForce");
		//System.out.println("nForce *:");
		//printStatements(infmodel, nForce, null, null);
		assertTrue(nForce != null);
	}


	@Test
	public void modelRestrictionMaxCardinality() {
		//Shows any statements containing OWL.maxCardinality in infmodel
		//System.out.println("Statements about property maxCardinality");
		//printStatements(infmodel, null, OWL.maxCardinality, null);

		// Handle the infmodel as an OntModel for the extended methods
		oInfmodel = ModelFactory.createOntologyModel();
		oInfmodel.add(infmodel);
		
		// Now handle Resources using OntResource 
		OntResource oRes = oInfmodel.getOntResource("urn:x-hp:eg/bigName42");

		// How many motherboards does bugName42 have?
		Statement hmbPro = oRes.getProperty(oInfmodel.getProperty("urn:x-hp:eg/hasMotherBoard"));
		int hmbProCar = oRes.getCardinality(oInfmodel.getProperty("urn:x-hp:eg/hasMotherBoard"));
		System.out.println("oRes: " + PrintUtil.print(oRes) + "\n" + hmbPro.toString() + "\n" + hmbProCar);
		assertEquals(2, hmbProCar); // There are 2 hasMotherBoards (even though maxCardinality is 1 for hasMotherBoard)

		// Get the Restrictions from the infModel 
		System.out.println("oData Restrictions:" + oInfmodel.listRestrictions().toList().toString());
		List<Restriction> oDataResList = oInfmodel.listRestrictions().toList();
		for (Restriction res : oDataResList) {
			System.out.print(res + PrintUtil.print(res));
			if (res.isMaxCardinalityRestriction()) {
				System.out.println(" is MaxCardinalityRestriction");
			} else {
				System.out.println(" NOT MaxCardinalityRestriction");
			}
		}

		// Gets all the Restriction types in the model (the referenced restrictions),
		// get one thats is MaxCardinality.
		Restriction maxCardRes = oInfmodel.listRestrictions().filterKeep(res -> res.isMaxCardinalityRestriction())
				.next();
		// This restriction node has an OWL maxCardinality edge pointing to a node with the "value"
		System.out.println("no: " + maxCardRes.getPropertyValue(OWL.maxCardinality)); 
		assertTrue (maxCardRes != null);


	}

	@Test
	public void propertyRestrictedCardinalityInSchema() {
		
		oInfmodel = ModelFactory.createOntologyModel();
		oInfmodel.add(schema);

		System.out.println("\n\nStatements about property maxCardinality exists: ");
		printStatements(oInfmodel, null, OWL.maxCardinality, null);

		// Cardinality is implied as a referring restriction.
		OntProperty oPro = oInfmodel.getOntProperty("urn:x-hp:eg/hasMotherBoard");
		System.out.println("what restrictions are referenced? " + oPro.listReferringRestrictions().toList().toString());

		// There is only one and it is maxCardinality (so this works without filtering)
		ExtendedIterator<Restriction> restrictionItr = oPro.listReferringRestrictions();
		Restriction res = restrictionItr.next();
		System.out.println("this restriction maxCardinatlity?");
		if (res.isMaxCardinalityRestriction()) {
			System.out.println("yes");
		} else {
			System.out.println("no");
		}

		// Get just the maxCardinalityRestriction from the referring restriction list.
		Restriction maxCres = oPro.listReferringRestrictions().filterKeep(maxC -> maxC.isMaxCardinalityRestriction())
				.next();
		RDFNode maxCvalue = maxCres.getPropertyValue(OWL.maxCardinality);
		System.out.println("maxCvalue " + maxCvalue);
		assertTrue("Failed to find maxCardinality", res.isMaxCardinalityRestriction());
		assertTrue("maxCardinality != 1", maxCvalue.toString().equals("\"1\"^^xsd:nonNegativeInteger"));

	}
	
	// Functions not tests

	public void printStatements(Model m, Resource s, Property p, Resource o) {
		for (StmtIterator i = m.listStatements(s, p, o); i.hasNext();) {
			Statement stmt = i.nextStatement();
			System.out.println(" - " + PrintUtil.print(stmt));
		}
	}
	
	public void validateInfModel() {
		ValidityReport validity = infmodel.validate();
		if (validity.isValid()) {
			System.out.println("OK");
		} else {
			System.out.println("Conflicts");
			for (Iterator i = validity.getReports(); i.hasNext();) {
				ValidityReport.Report report = (ValidityReport.Report) i.next();
				System.out.println(" - " + report);
			}
		}
	}	
	
	public void outputTTLFile() throws Exception {
		FileWriter file = new FileWriter("./resources/OWL/infModel.ttl");
		infmodel.write(file, "TTL");
		file.close();
	}
}