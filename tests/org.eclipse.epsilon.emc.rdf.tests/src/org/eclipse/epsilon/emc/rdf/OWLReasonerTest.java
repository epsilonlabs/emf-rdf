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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.PrintUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OWLReasonerTest {

	private Model schema;
	private Model data;
	private Reasoner reasoner;
	private InfModel infmodel;

	@Before
	public void setup() {
		this.schema = RDFDataMgr.loadModel("file:resources/OWL/owlDemoSchema.rdf");
		this.data = RDFDataMgr.loadModel("file:resources/OWL/owlDemoData.rdf");
		this.reasoner = ReasonerRegistry.getOWLReasoner();
		this.reasoner = reasoner.bindSchema(schema);
		this.infmodel = ModelFactory.createInfModel(reasoner, data);
	}

	public void printStatements(Model m, Resource s, Property p, Resource o) {
		for (StmtIterator i = m.listStatements(s, p, o); i.hasNext();) {
			Statement stmt = i.nextStatement();
			System.out.println(" - " + PrintUtil.print(stmt));
		}
	}

	@Test
	public void example() throws Exception {
		Resource nForce = infmodel.getResource("urn:x-hp:eg/nForce");
		System.out.println("nForce *:");
		printStatements(infmodel, nForce, null, null);
		assertTrue(nForce != null);
	}

	@Test
	public void outputTTLFile() throws Exception {
		FileWriter file = new FileWriter("./resources/OWL/infModel.ttl");
		infmodel.write(file, "TTL");
		file.close();
	}

	@Test
	public void exploreCard() {
		Resource aResource = infmodel.getResource("urn:x-hp:eg/hasMotherBoard");
		for (Statement prop : aResource.listProperties().toList()) {
			System.out.println(prop.toString());
		}

	}

}