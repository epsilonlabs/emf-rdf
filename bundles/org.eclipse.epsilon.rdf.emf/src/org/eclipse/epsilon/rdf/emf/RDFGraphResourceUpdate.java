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
import java.util.Objects;

import org.apache.jena.atlas.lib.DateTimeUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Bag;
import org.apache.jena.rdf.model.Container;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Seq;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.epsilon.rdf.emf.RDFGraphResourceImpl.MultiValueAttributeMode;

public class RDFGraphResourceUpdate {
	
	private static final boolean CONSOLE_OUTPUT_ACTIVE = false;
	private static final boolean SINGLE_MULTIVALUES_AS_STATEMENT = true;
	
	private boolean preferListsForMultiValues = false;
	private RDFDeserializer deserializer;
	private RDFGraphResourceImpl rdfGraphResource;
	
	public RDFGraphResourceUpdate(RDFDeserializer deserializer, RDFGraphResourceImpl rdfGraphResource, MultiValueAttributeMode multiValueMode) {
		this.deserializer = deserializer;
		this.rdfGraphResource = rdfGraphResource;
		this.preferListsForMultiValues = multiValueMode.equals(MultiValueAttributeMode.LIST);
	}

	//
	// Model Elements
	
	public void addModelElement() {
		
	}
	
	public void removeModelElement() {
		
	}
	
	//
	// Generic Statement methods for EStructural Features
	
	private Literal createLiteral(Object value) {
		if (value.getClass().equals(Date.class)) {
			Calendar c = Calendar.getInstance();
			c.setTime((Date) value);
			String date = DateTimeUtils.calendarToXSDDateTimeString(c);
			return ResourceFactory.createTypedLiteral(date, XSDDatatype.XSDdateTime);
		} else {
			return ResourceFactory.createTypedLiteral(value);
		}
	}
	
	private RDFNode createValueRDFNode(Object value, Model model) {
		if(value instanceof Resource) {
			return (Resource) value;
		}
		if (value instanceof Literal) {
			return (Literal) value;
		}
		
		if(value instanceof EObject) {
			// get Resource for EObject from the model or make one
			Resource valueResource = rdfGraphResource.getRDFResource((EObject)value);
			if(null == valueResource) {
				// TODO create RDF statements for new EObjects
				Resource newResource = ResourceFactory.createResource();
				System.err.println("Created a new Resource node: " + newResource);
				return newResource;
			} else {
				return valueResource;
			}
		} else {
			// Literal values
			return createLiteral(value);
		}
	}
	
	private Property createProperty(EStructuralFeature eStructuralFeature) {
		// PREDICATE
		String nameSpace = eStructuralFeature.getEContainingClass().getEPackage().getNsURI();
		String propertyURI = nameSpace + "#" + eStructuralFeature.getName();
		return ResourceFactory.createProperty(propertyURI);
	}
	
	private RDFNode getObjectRDFNode(EObject eObject, EStructuralFeature eStructuralFeature, Model model) {
		// SUBJECT
		Resource rdfNode = rdfGraphResource.getRDFResource(eObject);
		// PREDICATE
		Property property = createProperty(eStructuralFeature);
		// OBJECT
		if(model.contains(rdfNode, property)) {
			// TODO what happens when there are multiple statements? Are there any?
			List<RDFNode> propertyObjects = model.listObjectsOfProperty(rdfNode, property).toList();
			if (propertyObjects.size() > 1) {
				System.err.println(String.format(
					"getObjectRDFNode(): there is more than one object for property %s in node %s", property, rdfNode));
			}
			return propertyObjects.get(0);
		} else {
			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println(String.format(" %s RDF Node missing property %s : ", rdfNode, property));
			}
			return null;
		}
	}
	
	private Statement findEquivalentStatement(Model model, EObject eob, EStructuralFeature eStructuralFeature, Object value) {		
		// Returns a statement for a value from similar model statements (Subject-Property) and the deserialized value matches
		List<Statement> matchedStatementList = new ArrayList<>();
		
		// SUBJECT
		Resource rdfNode = rdfGraphResource.getRDFResource(eob);
		// PREDICATE
		Property property = createProperty(eStructuralFeature);
		// OBJECT
		RDFNode object = (RDFNode) null;
		
		List<Statement> modelStatementList = model.listStatements(rdfNode, property, object).toList();
		for (Statement modelStatement : modelStatementList) {
			Object deserialisedValue = deserializer.deserializeProperty(modelStatement.getSubject(), eStructuralFeature);
			if (Objects.equals(value, deserialisedValue)) {
				matchedStatementList.add(modelStatement);
			}
		}
		
		if (matchedStatementList.isEmpty()) {
			return null;
		}
		
		// Warn if there is more than one statement matching
		if (matchedStatementList.size() > 1) {
			StringBuilder statementList = new StringBuilder();
			for (Statement statement : modelStatementList) {
				statementList.append(String.format("\n - %s", statement));
			}
			System.err.println(String.format(
				"Find equivalent statements method returned more than 1 statement. %s\n", statementList));
		}
		return matchedStatementList.get(0); // Only return the first statement found
	}

	private Statement createStatement(Model model, EObject eObject, EStructuralFeature eStructuralFeature, Object value) {
		// A statement is formed as "subject–predicate–object"
		// SUBJECT
		Resource rdfNode = rdfGraphResource.getRDFResource(eObject);
		// PREDICATE
		Property property = createProperty(eStructuralFeature);
		// OBJECT
		RDFNode object = createValueRDFNode(value, model);
		return ResourceFactory.createStatement(rdfNode, property, object);
	}

	//
	// Containers statement operations
	
	private void checkAndRemoveEmptyContainers(Container container, EObject onEObject, EStructuralFeature eStructuralFeature) {
		Model model = container.asResource().getModel();

		if (1 == container.size() && SINGLE_MULTIVALUES_AS_STATEMENT) {
			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println("\n Removing container with 1 item replacing with statement: " + container);
			}
			RDFNode firstValue = container.iterator().next();
			container.removeProperties();
			model.remove(createStatement(model, onEObject, eStructuralFeature, container));
			model.add(createStatement(model, onEObject, eStructuralFeature, firstValue));
			return;
		}
		
		if (0 == container.size()) {
			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println("\n Removing empty container: " + container);
			}
			container.removeProperties();
			model.remove(createStatement(model, onEObject, eStructuralFeature, container));
			return;
		}
	}
	
	private void searchContainerAndRemoveValue(Object value, Container container, EStructuralFeature sf) {
		if (CONSOLE_OUTPUT_ACTIVE) {System.out.println("Remove from container: " + value);}

		List<Statement> containerPropertyStatements = container.listProperties().toList();
		Iterator<Statement> cpsItr = containerPropertyStatements.iterator();
		boolean done = false;
		while (!done) {
			if (cpsItr.hasNext()) {
				Statement statement = cpsItr.next();
				if (CONSOLE_OUTPUT_ACTIVE) {
					System.out.println(" statement check: " + statement);
				}
				Object deserializedValue = deserializer.deserializeValue(statement.getObject(), sf);
				if (value.equals(deserializedValue)) {
					if (CONSOLE_OUTPUT_ACTIVE) {
						System.out.println(" removing statement " + statement);
					}
					container.remove(statement);
					if (!sf.isUnique()) {
						done = true;
					}
				}
			} else {
				done = true;
			}
		}
	}

	private void removeFromContainer(Object values, Container container, EObject onEObject, EStructuralFeature eStructuralFeature) {
		if (CONSOLE_OUTPUT_ACTIVE) {reportContainer("Before remove", container);}
		
		EStructuralFeature sf = eStructuralFeature.eContainingFeature();
		if(values instanceof EList<?>) { 
			EList<?> valuesList = (EList<?>) values;
			valuesList.iterator().forEachRemaining(value -> 
				searchContainerAndRemoveValue(value, container, sf));
		} else {
			searchContainerAndRemoveValue(values, container, sf);
		}
		
		if (CONSOLE_OUTPUT_ACTIVE) {reportContainer("After remove", container);}
		
		// Check if container is empty (size 0), remove the blank node if true
		checkAndRemoveEmptyContainers(container, onEObject, eStructuralFeature);
	}
	
	private void removeFromSeq(Object value, Container container, EObject onEObject, EStructuralFeature eStructuralFeature) {
		removeFromContainer(value, container, onEObject, eStructuralFeature);
	}
	
	private void removeFromBag(Object value, Container container, EObject onEObject, EStructuralFeature eStructuralFeature) {
		removeFromContainer(value, container, onEObject, eStructuralFeature);
	}
	
	private void addToSequence(Object values, Seq container, int position) {
		Model model = container.getModel();
		if (CONSOLE_OUTPUT_ACTIVE) {
			reportContainer("Before add to container ", container);
		}

		if(values instanceof List) {			
			List<?> list = (List<?>) values;
			for (Object v : list) {
				++position;
				if (CONSOLE_OUTPUT_ACTIVE) {
					System.out.println(String.format("inserting: %s %s", position, v));
				}
				container.add(position, createValueRDFNode(v, model));
			}
		} else {
			// Assume values is a single value
			container.add(++position, createValueRDFNode(values, model));
		}

		if (CONSOLE_OUTPUT_ACTIVE) {
			reportContainer("After add to container ", container);
		}
	}

	private void addToBag(Object values, Bag container) {
		if (CONSOLE_OUTPUT_ACTIVE) {
			reportContainer("Before add to container ", container);
		}

		if(values instanceof Collection) {
			Collection<?> list = (Collection<?>) values;
			list.forEach(v -> container.add(v));
		} else {
			// Assume values is a single value
			container.add(values);
		}

		if (CONSOLE_OUTPUT_ACTIVE) {
			reportContainer("After add to container ", container);
		}
	}
	
	private Seq newSequence(Model model, EObject onEObject, EStructuralFeature eStructuralFeature) {
		Seq objectNode = model.createSeq();
		model.add(createStatement(model, onEObject, eStructuralFeature, objectNode));
		return objectNode;
	}
	
	private Bag newBag(Model model, EObject onEObject, EStructuralFeature eStructuralFeature) {
		Bag objectNode = model.createBag();
		model.add(createStatement(model, onEObject, eStructuralFeature, objectNode));
		return objectNode;
	}
	
	//
	// List statement operations	
	
	private void checkAndRemoveEmptyList(RDFList container, EObject onEObject, EStructuralFeature eStructuralFeature) {
		Model model = container.getModel();

		if (!container.isValid()) {
			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println("Removing invalid (empty) container:" + container.asResource());
			}
			Statement stmtToRemove = createStatement(model, onEObject, eStructuralFeature, container.asResource());
			model.remove(stmtToRemove);
			return;
		}
		
		if (1 == container.size() && SINGLE_MULTIVALUES_AS_STATEMENT) {
			// TODO convert list with 1 item to single statement
			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println("Removing container with 1 item and making a statement:" + container.asResource());
			}
			RDFNode firstValue = container.iterator().next();
			container.removeList();
			model.remove(createStatement(model, onEObject, eStructuralFeature, container.asResource()));
			model.add(createStatement(model, onEObject, eStructuralFeature, firstValue));
			return;
		}
		
		if (container.isEmpty()) {
			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println("Removing empty container:" + container.asResource());
			}
			model.remove(createStatement(model, onEObject, eStructuralFeature, container.asResource()));
			return;
		}
	}
	
	private RDFList removeOneValueFromListHandleUnique(RDFList container, EStructuralFeature sf, RDFNode valueRDFNode) {
		RDFList newContainer = container;

		if (sf.isUnique()) {
			while (newContainer.isValid() && newContainer.contains(valueRDFNode)) {
				newContainer = newContainer.remove(valueRDFNode);
				System.out.println(newContainer);
			}
		} else {
			newContainer = container.remove(valueRDFNode);
		}

		return newContainer;
	}
	
	private RDFList removeOneValueFromList(Object value, RDFList container, EStructuralFeature eStructuralFeature) {
		if(value instanceof EObject && eStructuralFeature instanceof EReference) {		
			// References
			RDFNode valueRDFNode = rdfGraphResource.getRDFResource((EObject)value);
			return removeOneValueFromListHandleUnique(container, eStructuralFeature, valueRDFNode);
		} else {	
			// Attributes (Literal values)
			Iterator<RDFNode> containerItr = container.iterator();
			while (containerItr.hasNext()) {
				RDFNode rdfNode = containerItr.next();
				Object deserializedValue = deserializer.deserializeValue(rdfNode, eStructuralFeature);
				if(value.equals(deserializedValue)) {
					if (CONSOLE_OUTPUT_ACTIVE) {
						System.out.println(String.format("removing: %s == %s", value , deserializedValue));
					}
					return removeOneValueFromListHandleUnique(container, eStructuralFeature, rdfNode);
				}
			}
		}
		return container;
	}
	
	private void removeFromList(Object values, RDFList container, EObject onEObject, EStructuralFeature eStructuralFeature, Model model) {
		RDFList newContainer = container;
		if(values instanceof List<?> valueList) {
			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println(String.format("list of values to remove: %s", valueList));
			}
			for (Object value : valueList) {
				newContainer = removeOneValueFromList(value, newContainer, eStructuralFeature);
			}
		} else {
			newContainer = removeOneValueFromList(values, newContainer, eStructuralFeature);
		}

		if (container != newContainer) {
			model.remove(createStatement(model, onEObject, eStructuralFeature, container.asResource()));
			model.add(createStatement(model, onEObject, eStructuralFeature, newContainer.asResource()));
		}
		checkAndRemoveEmptyList(newContainer, onEObject, eStructuralFeature);
	}
	
	private void addToList(Object values, RDFList container, int position, EStructuralFeature eStructuralFeature, EObject onEObject) {	
		if (CONSOLE_OUTPUT_ACTIVE) {
			System.out.println(String.format(
				"\n RDFList  ordered: %s position: %s size: %s",
				eStructuralFeature.isOrdered(), position, container.size()));
			reportRDFList("Before add to container ", container);
		}
		Model model = container.getModel();
		RDFList newList = createRDFListOnModel(values, model);

		// Un-ordered lists should be handed with these two methods
		if (container.isEmpty()) {
			// This should never happen: empty is handled elsewhere
			container.concatenate(newList);
		} else if (!eStructuralFeature.isOrdered()) {
			container.concatenate(newList);
		} else if (container.size() == position) {
			// Append new list to existing list on model
			container.concatenate(newList);
		} else if (0 == position) {
			// Add new list at the beginning of existing list
			model.remove(createStatement(model, onEObject, eStructuralFeature, container));
			model.add(createStatement(model, onEObject, eStructuralFeature, newList));
			newList.concatenate(container);
		} else {
			// Splice new list into the middle of the list:
			// we find the splice point by looping head/tail
			// over the list
			RDFList head = container;
			RDFList tail = head.getTail();
			for (int i = 1; i < position; i++) {
				head = tail;
				tail = head.getTail();
			}

			// Updated list is head -- newList -- tail
			newList.concatenate(tail);
			head.setTail(newList);
		}
	}

	private RDFList createRDFListOnModel(Object values, Model model) {
		List<RDFNode> rdfNodes = new ArrayList<>();
		if(values instanceof List<?> valuesList) {
			valuesList.forEach(v -> rdfNodes.add(createValueRDFNode(v, model)));
		} else {
			rdfNodes.add(createValueRDFNode(values, model));
		}
		return model.createList(rdfNodes.iterator());
	}
	
	private RDFList newList(Model model, EObject onEObject, EStructuralFeature eStructuralFeature, Object values) {		
		RDFList objectNode = createRDFListOnModel(values, model);
		model.add(createStatement(model, onEObject, eStructuralFeature, objectNode));
		return objectNode;
	}
	
	//
	// Single-value Features operations
	
	public void newSingleValueEStructuralFeatureStatements(List<Resource> namedModelURIs, EObject onEObject, EStructuralFeature eStructuralFeature, Object newValue) {
		assert newValue != null : "new value must exist";

		// We default always to the first named model for a new statement.
		// In the future, we may use a rule-based system to decide which named model to use.
		List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			newSingleValueEStructuralFeatureStatements(model, onEObject, eStructuralFeature, newValue);
		}
	}
	
	public void newSingleValueEStructuralFeatureStatements(Model model, EObject onEObject,
			EStructuralFeature eStructuralFeature, Object newValue) {
		assert newValue != null : "new value must exist";
		Statement newStatement = createStatement(model, onEObject, eStructuralFeature, newValue);
		Statement existingStatements = findEquivalentStatement(model, onEObject, eStructuralFeature, newValue);

		if (!model.contains(newStatement) && null == existingStatements) {
			model.add(newStatement);
		} else {
			System.err.println(String.format("New statement already exists? : %s", newStatement));
		}

	}
	
	public void removeSingleValueEStructuralFeatureStatements(List<Resource> namedModelURIs, EObject onEObject, EStructuralFeature eStructuralFeature, Object oldValue) {
		// Object type values set a new value "null", remove the statement the deserializer uses the meta-model so we won't have missing attributes
		assert oldValue != null : "old value must exist";		
		List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			removeSingleValueEStructuralFeatureStatements(model,onEObject,eStructuralFeature,oldValue);
		}
	}
	
	public void removeSingleValueEStructuralFeatureStatements(Model model, EObject onEObject, EStructuralFeature eStructuralFeature, Object oldValue) {
		// Object type values set a new value "null", remove the statement the
		// deserializer uses the meta-model so we won't have missing attributes
		assert oldValue != null : "old value must exist";

		// try object-to-literal
		Statement oldStatement = createStatement(model, onEObject, eStructuralFeature, oldValue);
		if (model.contains(oldStatement)) {
			model.remove(oldStatement);
			return;
		}
		
		// try literal-to-object
		Statement stmtToRemove = findEquivalentStatement(model, onEObject, eStructuralFeature, oldValue);
		if (stmtToRemove != null) {
			model.remove(stmtToRemove);
			return;
		}
		
		// EAttribute has a default value no statements in the RDF to match
		if (oldValue.equals(eStructuralFeature.getDefaultValue())) {
			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println(String.format(
						"Old statement not found, but the oldvalue matches the models default value, so there might not be a statement."
						+ "\n Remove - default value %s - old value %s ",
						eStructuralFeature.getDefaultValue(), oldValue));					
			}
			return;
		}

		// Couldn't find old statement through either object-to-literal or
		// literal-to-object conversion, and there is no default value
		 
		System.err.println(String.format("Old statement not found during single removal: %s", oldStatement));
		return;
	}
	
	public void updateSingleValueEStructuralFeatureStatements(List<Resource> namedModelURIs, EObject onEObject, EStructuralFeature eStructuralFeature, Object newValue, Object oldValue) {
		assert oldValue != null : "old value must exist";
		assert newValue != null : "new value must exist";
		List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) { 
			updateSingleValueEStructuralFeatureStatements(model, onEObject, eStructuralFeature, newValue, oldValue);			
		};
	}
	
	public void updateSingleValueEStructuralFeatureStatements(Model model, EObject onEObject, EStructuralFeature eStructuralFeature, Object newValue, Object oldValue) {
		// Remove any existing statements and add a new one
		removeSingleValueEStructuralFeatureStatements(model, onEObject, eStructuralFeature, oldValue);
		newSingleValueEStructuralFeatureStatements(model, onEObject, eStructuralFeature, newValue);
	}
	
	//
	// Multi-value Feature operations
	
	public void removeMultiEStructuralFeature (List<Resource> namedModelURIs, EObject onEObject, EStructuralFeature eStructuralFeature, Object newValue, Object oldValue) { 
		List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			removeMultiEStructuralFeature(model, onEObject, eStructuralFeature, newValue, oldValue);
		}
	}
	
	public void removeMultiEStructuralFeature (Model model, EObject onEObject, EStructuralFeature eStructuralFeature, Object newValue, Object oldValue) {
		Resource onEObjectNode = rdfGraphResource.getRDFResource(onEObject);
		if (!onEObjectNode.hasProperty(createProperty(eStructuralFeature))) {
			System.err.println("Trying to remove a none existing RDFnode for a multivalue attribute");
		} else {
			// Exists on a model some where...
			RDFNode objectRDFNode = getObjectRDFNode(onEObject, eStructuralFeature, model);
			
			if(objectRDFNode.isLiteral()) {
				// A 1 multi-value exists as a statement with no container
				removeSingleValueEStructuralFeatureStatements(model, onEObject, eStructuralFeature, oldValue);
			} else if (objectRDFNode.isResource()) {
				Resource objectResource = objectRDFNode.asResource();
				// Try and the container from each model to be updated
				if ( (objectResource.hasProperty(RDF.rest) && objectResource.hasProperty(RDF.first)) 
						|| objectResource.hasProperty(RDF.type, RDF.List) ) {				
					RDFList list = model.getList(objectResource);
					list.setStrict(true);
					removeFromList(oldValue, list, onEObject, eStructuralFeature, model);
				} else if (objectResource.equals(RDF.nil)) {
					// Empty list
					System.err.println("Removing from Empty list");				
				} else if (objectResource.hasProperty(RDF.type, RDF.Bag)) {
					Bag bag = model.getBag(objectResource);
					removeFromBag(oldValue, bag, onEObject, eStructuralFeature);
				} else if (objectResource.hasProperty(RDF.type, RDF.Seq)) {
					Seq seq = model.getSeq(objectResource);
					removeFromSeq(oldValue, seq, onEObject, eStructuralFeature);
				} else {
					// The first item might look like a single value EAttribute
					removeSingleValueEStructuralFeatureStatements(model, onEObject, eStructuralFeature, oldValue);
				}
			}
		}
	}
	
	public void addMultiValueEStructuralFeature (List<Resource> namedModelURIs, EObject onEObject, EStructuralFeature eStructuralFeature, Object newValue, Object oldValue, int position) { 
		List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			addMultiValueEStructuralFeature(model, onEObject, eStructuralFeature, newValue, oldValue, position);
		}
	}
	
	public void addMultiValueEStructuralFeature (Model model, EObject onEObject, EStructuralFeature eStructuralFeature, Object newValue, Object oldValue, int position) {
		// sequence (ordered), bag (unordered), list (ordered/unordered)
		
		Resource onEObjectNode = rdfGraphResource.getRDFResource(onEObject);
				
		// Work out if we are adding a NEW multi-value attribute with no existing RDF node.
		if(!onEObjectNode.hasProperty(createProperty(eStructuralFeature))) {
			// Does not exist anywhere so we need a NEW RDF representation			
			if(newValue instanceof List<?>) {
				if (CONSOLE_OUTPUT_ACTIVE) {System.out.println("\n No existing container, multiple values added making container");}
				createContainerAndAdd(model, onEObject, eStructuralFeature, newValue, position, null);
			} else {
				if (CONSOLE_OUTPUT_ACTIVE) {System.out.println("\n No existing container, first single value is a statement");}
				newSingleValueEStructuralFeatureStatements(model, onEObject, eStructuralFeature, newValue);	
			}
		} else {
			// Exists on a model some where...
			RDFNode objectRDFNode = getObjectRDFNode(onEObject, eStructuralFeature, model);
			
			if(objectRDFNode.isLiteral()) {
				// There is a single value statement for the 1 multi-value
				if (CONSOLE_OUTPUT_ACTIVE) {System.out.println("\n No existing container, multiple values added making container");}
				createContainerAndAdd(model, onEObject, eStructuralFeature, newValue, position, objectRDFNode.asLiteral().getValue());		
			} else if(objectRDFNode.isResource()) {
				Resource objectResource = objectRDFNode.asResource();
				// If we have one of these types, then we are updating an existing statement on a model
				if ( objectResource.hasProperty(RDF.type, RDF.List)
						|| (objectResource.hasProperty(RDF.rest) && objectResource.hasProperty(RDF.first))) {
					// Lists can be ordered or unique, both or none.
					RDFList list = model.getList(objectResource);
					addToList(newValue, list, position, eStructuralFeature, onEObject);
				} else if (objectResource.equals(RDF.nil)) {
					// List - Empty lists may be represented with an RDF.nil value
					Statement stmt = createStatement(model, onEObject, eStructuralFeature, objectResource);
					model.remove(stmt);
					newList(model, onEObject, eStructuralFeature, newValue);
				} else if (objectResource.hasProperty(RDF.type, RDF.Bag)) {
					Bag bag = model.getBag(objectResource);
					addToBag(newValue, bag);
				} else if (objectResource.hasProperty(RDF.type, RDF.Seq)) {
					Seq seq = model.getSeq(objectResource);
					addToSequence(newValue, seq, position);
				} else {
					createContainerAndAdd(model, onEObject, eStructuralFeature, newValue, position, objectResource);
				}
			}
		}
	}

	private void createContainerAndAdd(Model model, EObject onEObject, EStructuralFeature eStructuralFeature,
			Object newValue, int position, Object firstValue) {

		if(null != firstValue) {
			// There is a statement for the first value, with no container structure
			removeSingleValueEStructuralFeatureStatements(model, onEObject, eStructuralFeature, firstValue);
		}
		
		if (preferListsForMultiValues) {
			// List
			RDFList list = null;
			if(null != firstValue) {
				list = newList(model, onEObject, eStructuralFeature, firstValue);
				addToList(newValue, list, position, eStructuralFeature, onEObject);
			} else {
				list = newList(model, onEObject, eStructuralFeature, newValue);
			}
		} else {
			if (eStructuralFeature.isOrdered()) {
				// Sequence
				Seq sequence = newSequence(model, onEObject, eStructuralFeature);
				if(null != firstValue) {
					addToSequence(firstValue, sequence , 0);
				}
				addToSequence(newValue, sequence, position);
			} else {
				// Bag
				Bag bag = newBag(model, onEObject, eStructuralFeature);
				if(null != firstValue) {
					addToBag(firstValue, bag);
				}
				addToBag(newValue, bag);
			}
		}
	}
	
	
	//
	// Reporting (these could return formatted strings for logging or console use)

	private static void reportContainer(String label, Container container) {
		boolean hasType = container.hasProperty(RDF.type);
		if (hasType) {
			System.out.println(String.format("\n%s Containter: Type %s, Size %s", 
					label, container.getProperty(RDF.type), container.size()));
			container.iterator().forEach(i -> System.out.println("  * " + i));
		}
	}
	
	private static void reportRDFList (String label, RDFList container) {
		System.out.println(String.format("\n%s List: Strict %s, Size: %s", 
				label, container.getStrict(), container.size() ));
		
		Resource item = container;
		while (null != item) {
			Statement restStatement = item.getProperty(RDF.rest);
			Statement firstStatement = item.getProperty(RDF.first);
			
			if(null != restStatement) {
				System.out.println(String.format(" * RDFnode %s \n\t--> rest: %s \n\t--> first: %s ",
					item, restStatement.getObject(), firstStatement.getObject()));
				item = item.getProperty(RDF.rest).getResource();
			} else {
				item = null;
			}
		}
	}

}
