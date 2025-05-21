/********************************************************************************
 * Copyright (c) 2025 University of York
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
package org.eclipse.epsilon.rdf.emf;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.apache.jena.atlas.lib.DateTimeUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Alt;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Container;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

public class RDFGraphResourceUpdate {
	
	private RDFDeserializer deserializer;
	
	public RDFGraphResourceUpdate(RDFDeserializer deserializer) {
		this.deserializer = deserializer;
	}

	private static Statement createStatement(EObject eObject, EAttribute eAttribute, Object value) {
		// A statement is formed as "subject–predicate–object"

		// SUBJECT
		RDFGraphResourceImpl graphResource = RDFGraphResourceImpl.getRDFGraphResourceFor(eObject);
		Resource rdfNode = graphResource.getRDFResource(eObject);

		// PREDICATE
		String nameSpace = eAttribute.getEContainingClass().getEPackage().getNsURI();
		String propertyURI = nameSpace + "#" + eAttribute.getName();
		Property property = ResourceFactory.createProperty(propertyURI);

		// OBJECT
		Literal object = null;
		if (value.getClass().equals(Date.class)) {
			Calendar c = Calendar.getInstance();
			c.setTime((Date) value);
			String date = DateTimeUtils.calendarToXSDDateTimeString(c);
			object = ResourceFactory.createTypedLiteral(date, XSDDatatype.XSDdateTime);
		} else {
			object = ResourceFactory.createTypedLiteral(value);
		}

		// STATEMENTS
		return ResourceFactory.createStatement(rdfNode, property, object);
	}
	
	private static Property getProperty (EAttribute eAttribute) {
		//
		// PREDICATE
		String nameSpace = eAttribute.getEContainingClass().getEPackage().getNsURI();
		String propertyURI = nameSpace + "#" + eAttribute.getName();
		return ResourceFactory.createProperty(propertyURI);
	}	
	
	private static Resource getStmtObject (EObject eObject, EAttribute eAttribute) {
		//
		// SUBJECT
		RDFGraphResourceImpl graphResource = RDFGraphResourceImpl.getRDFGraphResourceFor(eObject);
		Resource rdfNode = graphResource.getRDFResource(eObject);

		//
		// PREDICATE
		Property property = getProperty(eAttribute);
		
		//
		// OBJECT
		Resource stmtObject = (Resource) rdfNode.getProperty(property).getObject();
		return stmtObject;
	}
	
	public void updateSingleValueAttributeStatements(List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue) {
		assert oldValue != null : "old value must exist";
		assert newValue != null : "new value must exist";

		RDFGraphResourceImpl graphResource = RDFGraphResourceImpl.getRDFGraphResourceFor(onEObject);
		Statement newStatement = createStatement(onEObject, eAttribute, newValue);
		Statement oldStatement = createStatement(onEObject, eAttribute, oldValue);

		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			if (model.contains(oldStatement)) {
				model.remove(oldStatement);
				model.add(newStatement);
			}
			else {
				System.err.println(String.format("Old statement not found : %s", oldStatement));
			}
		}
	}

	public void removeSingleValueAttributeStatements(List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object oldValue) {
		// Object type values set a new value "null", remove the statement the deserializer uses the meta-model so we won't have missing attributes
		assert oldValue != null : "old value must exist";
		RDFGraphResourceImpl graphResource = RDFGraphResourceImpl.getRDFGraphResourceFor(onEObject);
		Statement oldStatement = createStatement(onEObject, eAttribute, oldValue);

		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			if (model.contains(oldStatement)) {
				model.remove(oldStatement);
			} else {
				System.err.println(String.format("Old statement not found : %s", oldStatement));
			}
		}
	}
	
	public void newSingleValueAttributeStatements (List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue) {
		assert newValue != null : "new value must exist";
		RDFGraphResourceImpl graphResource = RDFGraphResourceImpl.getRDFGraphResourceFor(onEObject);
		Statement newStatement = createStatement(onEObject, eAttribute, newValue);

		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			if (!model.contains(newStatement)) {
				model.add(newStatement); 
			} else {
				System.err.println(String.format("New statement already exists? : %s", newStatement));
			}
		}
	}
	
	private void addToBag(Object values, Bag bag) {
		reportContainer("Before add to Bag ", bag);
		if (values.getClass().equals(ArrayList.class)) {
			ArrayList<Object> list = (ArrayList<Object>) values;
			list.forEach(v -> bag.add(v));
		} else {
			bag.add(values);
		}
		reportContainer("After add to Bag ", bag);
	}

	private void newSequence() {
		
	}
	
	private void newBag() {
		
	}
	
	private void addToContainer(Object values, Container container, boolean isUnique) {
		reportContainer("Before add to container ", container);
		if (values.getClass().equals(ArrayList.class)) {
			ArrayList<Object> list = (ArrayList<Object>) values;
			list.forEach(v -> container.add(v));
		} else {
			container.add(values);
		}
		reportContainer("After add to container ", container);
	}
	
	public void addMultiValueAttribute (List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue) {
		
		RDFGraphResourceImpl graphResource = RDFGraphResourceImpl.getRDFGraphResourceFor(onEObject);
		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		
		Resource object = getStmtObject(onEObject, eAttribute);
		boolean isOrdered = eAttribute.isOrdered(); // sequence (ordered), bag (unordered)
		eAttribute.isUnique(); // check container before adding
		eAttribute.isMany(); // should be true always?
		
		// Work out if we are adding a NEW multi-value attribute with no existing RDF node.
		Statement type = object.getProperty(RDF.type);
		
		if(null == type || null == object) {
			Model model = namedModelsToUpdate.get(0);
			// NEW RDF representation				
			if (eAttribute.isOrdered()) {
				// Sequence
				addToContainer(newValue, model.createSeq(), isOrdered);
			} else {
				// Bag
				addToContainer(newValue, model.createBag(), isOrdered);
			}
			return;
		}
		
		// Need to get at the Data models and check for the onEObject.	
		
		for (Model model : namedModelsToUpdate) {			
			// If we have one of these types, then we are updating and existing
			if (object.hasProperty(RDF.type, RDF.List)) {
				RDFList list = object.as(RDFList.class);
				System.out.println("\nobject RDF.List:");
			}
			else if (object.hasProperty(RDF.type, RDF.Bag)) {
				Bag bag = model.getBag(object);
				addToContainer(newValue, bag, eAttribute.isOrdered());
			}
			else if (object.hasProperty(RDF.type, RDF.Seq)) {
				Seq seq = model.getSeq(object);
				addToContainer(newValue, seq, eAttribute.isOrdered());
			}
			else if (object.hasProperty(RDF.type, RDF.Alt)) {
				Alt alt = model.getAlt(object);
				addToContainer(newValue, alt, eAttribute.isOrdered());
			}
			else {
				
			}
		}
	}
	
	private void removeListedFromContainer (List<Object> values, Container container, EStructuralFeature sf) {
		StmtIterator containerStatementItr = container.listProperties();
		ArrayList<Statement> containerStatementsToRemove = new ArrayList<Statement>();

		while (containerStatementItr.hasNext()) {
			Statement statement = containerStatementItr.next();
			Object deserializedValue = deserializer.deserializeValue(statement.getObject(), sf);
			if(values.remove(deserializedValue)) {				
				containerStatementsToRemove.add(statement);
			}
		}

		// Remove from the back to front as Jena renumbers ordinal properties (rdf:_nnn) after a statement is removed
		ListIterator<Statement> stmtItr = containerStatementsToRemove.listIterator();
		stmtItr.forEachRemaining(s->container.remove(s));

	}

	private void removeFromContainer(Object values, Container container, EStructuralFeature sf) {
		reportContainer("Before remove", container);

		if(values instanceof EList<?>) { 
			EList<?> valuesList = (EList<?>) values;
			removeListedFromContainer(new ArrayList<>(valuesList), container, sf);
			// Check if container is empty (size 0), remove the blank node if true
		} else {
			// not a list?
		}
		reportContainer("After remove", container);
	}	
	
	public void removeMultiValueAttribute (List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue) {
		RDFGraphResourceImpl graphResource = RDFGraphResourceImpl.getRDFGraphResourceFor(onEObject);
		Resource stmtObject = getStmtObject(onEObject, eAttribute);
		
		EStructuralFeature sf = eAttribute.eContainingFeature();
				
		// Need to get at the Data models and check for the onEObject.	
		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			if (stmtObject.hasProperty(RDF.type, RDF.List)) {
				RDFList list = stmtObject.as(RDFList.class);
				System.out.println("\nobject RDF.List:");
			}
			
			if (stmtObject.hasProperty(RDF.type, RDF.Bag)) {
				Bag bag = model.getBag(stmtObject);
				removeFromContainer(oldValue, bag, sf);
			}
	
			if (stmtObject.hasProperty(RDF.type, RDF.Seq)) {
				Seq seq = model.getSeq(stmtObject);
				removeFromContainer(oldValue, seq, sf);
			}
			
			if (stmtObject.hasProperty(RDF.type, RDF.Alt)) {
				Alt alt = model.getAlt(stmtObject);
				removeFromContainer(oldValue, alt, sf);
				
				System.out.println("\nobject RDF.Alt - Size: " + alt.size());
				alt.iterator().forEach(i -> System.out.println("  * " + i));
				System.out.println("Default: " + alt.getDefault());			
			}			
		}
	}
	
	public void setMultiValueAttribute() {
		
	}
	
	public void unsetMultiValueAttribute() {
		
	}
	
	public void addModelElement() {
		
	}
	
	public void removeModelElement() {
		
	}

	private static void reportContainer(String label, Container container) {
		 boolean hasType = container.hasProperty(RDF.type);
		if (hasType) {
			System.out.println(String.format("\n%s Containter: Type %s , Size %s",
					label, container.getProperty(RDF.type), container.size()));
			container.iterator().forEach(i -> System.out.println("  * " + i));
		}
	}

}
