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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

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
import org.apache.jena.vocabulary.RDF;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

public class RDFGraphResourceUpdate {
	
	private RDFDeserializer deserializer;
	private RDFGraphResourceImpl rdfGraphResource;
	
	public RDFGraphResourceUpdate(RDFDeserializer deserializer, RDFGraphResourceImpl rdfGraphResource) {
		this.deserializer = deserializer;
		this.rdfGraphResource = rdfGraphResource;
	}

	private Statement createStatement(EObject eObject, EAttribute eAttribute, Object value) {
		// A statement is formed as "subject–predicate–object"

		// SUBJECT
		Resource rdfNode = rdfGraphResource.getRDFResource(eObject);
		// PREDICATE
		Property property = getProperty(eAttribute);
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

		// STATEMENT
		return ResourceFactory.createStatement(rdfNode, property, object);
	}
	
	private static Property getProperty (EAttribute eAttribute) {
		// PREDICATE
		String nameSpace = eAttribute.getEContainingClass().getEPackage().getNsURI();
		String propertyURI = nameSpace + "#" + eAttribute.getName();
		return ResourceFactory.createProperty(propertyURI);
	}	
	
	private Resource getStmtObjectFor (EObject eObject, EAttribute eAttribute) {
		// This method will assume a stmt object should come from the ontModel in the RDFGraphResource
		Model rdfResourceOntModel = rdfGraphResource.getRDFResource(eObject).getModel();
		return getStmtObjectFor(eObject, eAttribute, rdfResourceOntModel);
	}
	
	private Resource getStmtObjectFor (EObject eObject, EAttribute eAttribute, Model model) {
		// SUBJECT
		Resource rdfNode = rdfGraphResource.getRDFResource(eObject);
		// PREDICATE
		Property property = getProperty(eAttribute);
		// OBJECT
		if(model.contains(rdfNode, property)) {	
			Resource stmtObject = model.getProperty(rdfNode, property).getObject().asResource();
			System.out.println(" Returning stmtObject : " + stmtObject);
			return stmtObject;
		} else {
			System.out.println(String.format(" %s RDF Node missing property %s : ", rdfNode, property));
			model.getResource(rdfNode.getId()).listProperties().forEach(s -> System.out.println("  - " + s));;
			return null;
		}
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
		Statement oldStatement = createStatement(onEObject, eAttribute, oldValue);

		List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
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
		Statement newStatement = createStatement(onEObject, eAttribute, newValue);

		List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			if (!model.contains(newStatement)) {
				model.add(newStatement); 
			} else {
				System.err.println(String.format("New statement already exists? : %s", newStatement));
			}
		}
	}

	private Seq newSequence(Model model, EObject onEObject, EAttribute eAttribute) {
		// SUBJECT
		Resource subjectNode = rdfGraphResource.getRDFResource(onEObject);
		// PREDICATE
		Property property = getProperty(eAttribute);
		// OBJECT
		Seq objectNode = model.createSeq();
		model.add(subjectNode,property,objectNode);
		return objectNode;
	}
	
	private Bag newBag(Model model, EObject onEObject, EAttribute eAttribute) {
		// SUBJECT
		Resource subjectNode = rdfGraphResource.getRDFResource(onEObject);
		// PREDICATE
		Property property = getProperty(eAttribute);
		// OBJECT
		Bag objectNode = model.createBag();
		model.add(subjectNode, property, objectNode);
		return objectNode;
	}
	
	private void addToContainer(Object values, Bag container) {
		reportContainer("Before add to container ", container);
		try {
			Collection<?> list = (Collection<?>) values;
			list.forEach(v -> container.add(v));
		} catch(Exception e) {
			// Assume values is a single value
			container.add(values);
		}
		reportContainer("After add to container ", container);
	}
	
	private void addToContainer(Object value, Seq container, int position) {
		reportContainer("Before add to container ", container);
		try {
			List<?> values = (List<?>) value;
			if (0 == position) {
				values.forEach(v -> container.add(v));
			} else {
				for (Object v : values) {
					++position;
					System.out.println(String.format( "inserting: %s %s", position, v ) );
					container.add(position, v);					
				}
							
			}			
		} catch(Exception e) {
			// Assume values is a single value
			if (0 == position) {
				container.add(value);
			} else {
				container.add(++position, value);
			}
		}
		reportContainer("After add to container ", container);
	}
	
	public void addMultiValueAttribute (List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue, int position) {
		
		List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
		
		Resource onEObjectNode = rdfGraphResource.getRDFResource(onEObject);

		boolean isOrdered = eAttribute.isOrdered(); // sequence (ordered), bag (unordered)
		eAttribute.isUnique(); // check container before adding
		eAttribute.isMany(); // should be true always?
		
		// Work out if we are adding a NEW multi-value attribute with no existing RDF node.
		
		if(onEObjectNode.hasProperty(getProperty(eAttribute))) {
			// Exists on a model some where...
			for (Model model : namedModelsToUpdate) {			
				// If we have one of these types, then we are updating and existing
				Resource modelStmtObject = getStmtObjectFor(onEObject, eAttribute, model);
				if(null == modelStmtObject) {
					// no operation
				}
				else if (modelStmtObject.hasProperty(RDF.type, RDF.List)) {
					RDFList list = modelStmtObject.as(RDFList.class);
					System.out.println("\nobject RDF.List:");
					// TODO Handle a list
				}
				else if (modelStmtObject.hasProperty(RDF.type, RDF.Bag)) {
					Bag bag = model.getBag(modelStmtObject);
					addToContainer(newValue, bag);
				}
				else if (modelStmtObject.hasProperty(RDF.type, RDF.Seq)) {
					Seq seq = model.getSeq(modelStmtObject);
					addToContainer(newValue, seq, position);
				}
				else if (modelStmtObject.hasProperty(RDF.type, RDF.Alt)) {
					Alt alt = model.getAlt(modelStmtObject);
					// TODO Handle an ALT
				}
				else {
					// no operation
				}
			}
		} else {
			// NEW RDF representation
			Model model = namedModelsToUpdate.get(0);
			System.out.println("\n No existing container, making a new one");
			// Need to make the Blank node model.createSeq() and then need to make a statement to attach it to the onEobject - eAttribute			
			if (eAttribute.isOrdered()) {
				// Sequence
				addToContainer(newValue, newSequence(model, onEObject, eAttribute), 0);
			} else {
				// Bag
				addToContainer(newValue, newBag(model, onEObject, eAttribute));
			}
			return;
		}
	}
	
	private void removeValueFromContainer (Object value, Container container, EStructuralFeature sf) {
		System.out.println("Remove from container: " + value);

		List<Statement> containerPropertyStatements = container.listProperties().toList();
		Iterator<Statement> cpsItr = containerPropertyStatements.iterator();
		boolean done = false;
		while (!done) {
			if (cpsItr.hasNext()) {
				Statement statement = cpsItr.next();
				System.out.println(" statement check: " + statement);
				Object deserializedValue = deserializer.deserializeValue(statement.getObject(), sf);
				if (value.equals(deserializedValue)) {
					System.out.println("  removing statement " + statement);
					container.remove(statement);
					done = true;
				}
			} else {
				done = true;
			}		
		}		
	}
	
	private void checkAndRemoveEmptyContainers(Container container, EObject onEObject, EAttribute eAttribute) {
		Model model = container.asResource().getModel();
		if (model.containsResource(container)) {
			if (0 == container.size()) {
				System.out.println("\n Removing empty container: container");
				container.removeAll(RDF.type);
				Resource subjectNode = rdfGraphResource.getRDFResource(onEObject);
				Property property = getProperty(eAttribute);
				model.remove(subjectNode, property, container);
			}
		}
	}
	
	private void removeFromContainer(Object value, Container container, EObject onEObject, EAttribute eAttribute) {
		reportContainer("Before remove", container);
		
		EStructuralFeature sf = eAttribute.eContainingFeature();		
		if(value instanceof EList<?>) { 
			EList<?> values = (EList<?>) value;
			values.iterator().forEachRemaining(v -> 
				removeValueFromContainer(v, container, sf));
		} else {
			removeValueFromContainer(value, container, sf);
		}
		
		reportContainer("After remove", container);
		
		// Check if container is empty (size 0), remove the blank node if true
		checkAndRemoveEmptyContainers(container, onEObject, eAttribute);
	}
	
	public void removeMultiValueAttribute (List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue) {
		Resource onEObjectNode = rdfGraphResource.getRDFResource(onEObject);
		if (onEObjectNode.hasProperty(getProperty(eAttribute))) {
			// Exists on a model some where...
			List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
			for (Model model : namedModelsToUpdate) {
				// Try and the container from each model to be updated
				Resource modelStmtObject = getStmtObjectFor(onEObject, eAttribute, model);
				if (null == modelStmtObject) {
					// no operation
				} else if (modelStmtObject.hasProperty(RDF.type, RDF.List)) {
					RDFList list = modelStmtObject.as(RDFList.class);
					System.out.println("\nobject RDF.List:");
					// TODO Handle a list
				} else if (modelStmtObject.hasProperty(RDF.type, RDF.Bag)) {
					Bag bag = model.getBag(modelStmtObject);
					removeFromContainer(oldValue, bag, onEObject, eAttribute);
				} else if (modelStmtObject.hasProperty(RDF.type, RDF.Seq)) {
					Seq seq = model.getSeq(modelStmtObject);
					removeFromContainer(oldValue, seq, onEObject, eAttribute);
				} else if (modelStmtObject.hasProperty(RDF.type, RDF.Alt)) {
					Alt alt = model.getAlt(modelStmtObject);
					System.out.println("\nobject RDF.Alt - Size: " + alt.size());
					alt.iterator().forEach(i -> System.out.println("  * " + i));
					System.out.println("Default: " + alt.getDefault());
					// TODO Handle an ALT
				} else {
					// no operation
				}
			} 
		} else {
			System.err.println("Trying to remove a none existing RDFnode for a multivalue attribute");
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
