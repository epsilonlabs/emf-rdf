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
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.epsilon.rdf.emf.RDFGraphResourceImpl.MultiValueAttributeMode;

public class RDFGraphResourceUpdate {
	
	static final boolean CONSOLE_OUTPUT_ACTIVE = false;
	
	private boolean preferListsForMultiValues = false;
	private RDFDeserializer deserializer;
	private RDFGraphResourceImpl rdfGraphResource;
	
	public RDFGraphResourceUpdate(RDFDeserializer deserializer, RDFGraphResourceImpl rdfGraphResource, MultiValueAttributeMode multiValueMode) {
		this.deserializer = deserializer;
		this.rdfGraphResource = rdfGraphResource;
		this.preferListsForMultiValues = MultiValueAttributeMode.LIST == multiValueMode;
	}

	private Statement createStatement(EObject subject, EAttribute predicate, RDFNode object) {
		// SUBJECT
		Resource rdfNode = rdfGraphResource.getRDFResource(subject);
		// PREDICATE
		Property property = getProperty(predicate);
		// OBJECT
		return ResourceFactory.createStatement(rdfNode, property, object);
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

	private Property getProperty(EAttribute eAttribute) {
		// PREDICATE
		String nameSpace = eAttribute.getEContainingClass().getEPackage().getNsURI();
		String propertyURI = nameSpace + "#" + eAttribute.getName();
		return ResourceFactory.createProperty(propertyURI);
	}

	private Statement getStatementFor(EObject subject, EAttribute predicate, Model model) {
		// SUBJECT
		Resource rdfNode = rdfGraphResource.getRDFResource(subject);
		// PREDICATE
		Property property = getProperty(predicate);
		return model.getProperty(rdfNode, property);
	}

	private Resource getStmtObjectFor(EObject eObject, EAttribute eAttribute) {
		// This method will assume a stmt object should come from the ontModel in the RDFGraphResource
		Model model = rdfGraphResource.getRDFResource(eObject).getModel();
		return getStmtObjectFor(eObject, eAttribute, model);
	}

	private Resource getStmtObjectFor(EObject eObject, EAttribute eAttribute, Model model) {
		// SUBJECT
		Resource rdfNode = rdfGraphResource.getRDFResource(eObject);
		// PREDICATE
		Property property = getProperty(eAttribute);
		// OBJECT
		if(model.contains(rdfNode, property)) {	
			Resource stmtObject = model.getProperty(rdfNode, property).getObject().asResource();
			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println(" Returning stmtObject : " + stmtObject);
			}
			return stmtObject;
		} else {
			System.out.println(String.format(" %s RDF Node missing property %s : ", rdfNode, property));
			if (CONSOLE_OUTPUT_ACTIVE) {
				model.getResource(rdfNode.getId()).listProperties().forEach(s -> System.out.println("  - " + s));
			}
			return null;
		}
	}
	
	private RDFList createRDFListOnModel (Object values, Model model) {
		List<RDFNode> rdfNodes = new ArrayList<RDFNode>();
		if(values instanceof List<?> valuesList) {
			valuesList.forEach(v -> rdfNodes.add(ResourceFactory.createTypedLiteral(v)));
		} else {
			rdfNodes.add(ResourceFactory.createTypedLiteral(values));
		}
		return model.createList(rdfNodes.iterator());
	}

	//
	// Single-value Attributes
	
	public void updateSingleValueAttributeStatements(List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue) {
		assert oldValue != null : "old value must exist";
		assert newValue != null : "new value must exist";

		RDFGraphResourceImpl graphResource = (RDFGraphResourceImpl) onEObject.eResource();
		Statement newStatement = createStatement(onEObject, eAttribute, newValue);
		Statement oldStatement = createStatement(onEObject, eAttribute, oldValue);

		boolean found = false;
		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			if (model.contains(oldStatement)) {
				model.remove(oldStatement);
				model.add(newStatement);
				found = true;
			}
		}

		if (!found) {
			/*
			 * Could not find directly via object-to-literal conversion: try using the
			 * deserialiser (literal-to-object) and comparing instead.
			 */
			for (Model model : namedModelsToUpdate) {
				Statement stmtToRemove = findEquivalentStatement(model, onEObject, eAttribute, oldValue);
				if (stmtToRemove != null) {
					model.remove(stmtToRemove);
					model.add(newStatement);
					found = true;
				}
			}
		}

		if (!found) {
			/*
			 * Couldn't find old statement through either object-to-literal or literal-to-object conversion
			 */
			System.err.println(String.format("Old statement not found during single update: %s" , oldStatement));
		}
	}

	protected Statement findEquivalentStatement(Model model, EObject eob, EAttribute eAttribute, Object oldValue) {
		Statement stmtToRemove = null;
		StmtIterator itOldStmt = model.listStatements(
			rdfGraphResource.getRDFResource(eob), getProperty(eAttribute), (RDFNode) null);
		while (itOldStmt.hasNext() && stmtToRemove == null) {
			Statement stmt = itOldStmt.next();
			Object stmtObject = deserializer.deserializeProperty(stmt.getSubject(), eAttribute);
			if (Objects.equals(oldValue, stmtObject)) {
				stmtToRemove = stmt;
			}
		}
		return stmtToRemove;
	}

	public void removeSingleValueAttributeStatements(List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object oldValue) {
		// Object type values set a new value "null", remove the statement the deserializer uses the meta-model so we won't have missing attributes
		assert oldValue != null : "old value must exist";		
		Statement oldStatement = createStatement(onEObject, eAttribute, oldValue);

		// Same as above: try object-to-literal first, then literal-to-object
		boolean found = false;
		List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			if (model.contains(oldStatement)) {
				model.remove(oldStatement);
				found = true;
			}
		}
		if (!found) {
			for (Model model : namedModelsToUpdate) {
				Statement stmtToRemove = findEquivalentStatement(model, onEObject, eAttribute, oldValue);
				if (stmtToRemove != null) {
					model.remove(stmtToRemove);
					found = true;
				}
			}
		}
		if (!found) {
			/*
			 * Couldn't find old statement through either object-to-literal or literal-to-object conversion
			 */
			System.err.println(String.format("Old statement not found during single removal: %s" , oldStatement));
		}
	}

	public void newSingleValueAttributeStatements(List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue) {
		assert newValue != null : "new value must exist";
		Statement newStatement = createStatement(onEObject, eAttribute, newValue);

		// We default always to the first named model for a new statement.
		// In the future, we may use a rule-based system to decide which named model to use.
		List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			if (!model.contains(newStatement)) {
				model.add(newStatement);
			} else {
				System.err.println(String.format("New statement already exists? : %s", newStatement));
			}
		}
	}

	//
	// Multi-value Attributes

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
	
	private void addToSequence(Object values, Seq container, int position) {
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
				container.add(position, v);
			}
		} else {
			// Assume values is a single value
			container.add(++position, values);
		}

		if (CONSOLE_OUTPUT_ACTIVE) {
			reportContainer("After add to container ", container);
		}
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
	
	private RDFList newList(Model model, EObject onEObject, EAttribute eAttribute, Object values) {		
		// SUBJECT
		Resource subjectNode = rdfGraphResource.getRDFResource(onEObject);
		// PREDICATE
		Property property = getProperty(eAttribute);
		// OBJECT
		RDFList objectNode = createRDFListOnModel(values, model);
		model.add(subjectNode, property, objectNode);
		return objectNode;
	}
	
	private void addToList(Object values, RDFList container, int position, EAttribute eAttribute, EObject onEObject) {	
		if (CONSOLE_OUTPUT_ACTIVE) {
			System.out.println(String.format(
				"\n RDFList  ordered: %s position: %s size: %s",
				eAttribute.isOrdered(), position, container.size()));
			reportRDFList("Before add to container ", container);
		}
		Model model = container.getModel();

		RDFList newList = createRDFListOnModel(values, model);

		// Un-ordered lists should be handed with these two methods
		if (container.isEmpty()) {
			// This should never happen, empty is handled elsewhere
			container.concatenate(newList);
			return;
		} 

		if (!eAttribute.isOrdered()) {
			container.concatenate(newList);
			return;
		} 
		
		// Ordered lists that are not empty will be handled with the methods below.
		if(eAttribute.isOrdered()) {
			if (container.size() == position) {
				// Append new list to existing list on model
				container.concatenate(newList);
				return;
			}

			if (0 == position) {
				// Make a new list and append the old List
				// SUBJECT
				Resource subjectNode = rdfGraphResource.getRDFResource(onEObject);
				// PREDICATE
				Property property = getProperty(eAttribute);
				// OLD OBJECT - remove list statement
				model.remove(subjectNode,property,container);
				// NEW OBJECT - add new list statement
				RDFList objectNode = newList;
				model.add(subjectNode, property, objectNode);

				objectNode.concatenate(container);
				return;
			}
			
			// Split the existing list and insert the new list
			
			// EMF/Epsilon will complain if you try to add at a position beyond the size of the list
			int listIndex = 0;
			if (position > 0) {
				listIndex = position - 1;
			}

			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println(String.format("\n [ORDERED insert] headNode: %s -- listIndex: %s -- posNode %s \n",
					container.getHead(), listIndex, container.get(listIndex)));
			}
			
			// Run down the list via RDF.rest to the node at the index position
			int i = 0;
			Resource insertAtNode = container;
			while (i < listIndex) {
				insertAtNode = insertAtNode.getProperty(RDF.rest).getResource();
				++i;
			}
			
			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println("[Insert at node] " + insertAtNode);
			}
			
			// Get the tail end of the current container list after the node we insert at.
			RDFList oldTail = insertAtNode.getProperty(RDF.rest).getList();
			
			// Cut off the tail end of the current container list
			insertAtNode.getProperty(RDF.rest).changeObject(RDF.nil);
			
			// Append the new values to the current container values
			container.concatenate(newList);

			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println("[OLD Tail] " + oldTail);
			}

			// Append the tail end of values we saved above from the original container lists
			container.concatenate(oldTail);
			
			return;
		}

		if (CONSOLE_OUTPUT_ACTIVE) {reportRDFList("After add to container ", container);}
	}
	
	public void addMultiValueAttribute (List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue, int position) {
		// sequence (ordered), bag (unordered)
		
		List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
		Resource onEObjectNode = rdfGraphResource.getRDFResource(onEObject);
		
		// Work out if we are adding a NEW multi-value attribute with no existing RDF node.
		if(onEObjectNode.hasProperty(getProperty(eAttribute))) {
			// Exists on a model some where...
			for (Model model : namedModelsToUpdate) {
				Resource modelStmtObject = getStmtObjectFor(onEObject, eAttribute, model);
				Class<? extends Resource> type = modelStmtObject.getClass();

				// If we have one of these types, then we are updating an existing statement on a model
				if ( (modelStmtObject.hasProperty(RDF.rest) && modelStmtObject.hasProperty(RDF.first)) 
							|| modelStmtObject.hasProperty(RDF.type, RDF.List)) {
					// Lists can be ordered or unique, both or none.
					RDFList list = model.getList(modelStmtObject);
					addToList(newValue, list, position, eAttribute, onEObject);
				} else if (modelStmtObject.equals(RDF.nil)) {
					// List - Empty lists may be represented with an RDF.nil value
					Statement stmt = createStatement(onEObject, eAttribute, modelStmtObject);
					model.remove(stmt);
					newList(model, onEObject, eAttribute, newValue);
				} else if (modelStmtObject.hasProperty(RDF.type, RDF.Bag)) {
					Bag bag = model.getBag(modelStmtObject);
					addToBag(newValue, bag);
				} else if (modelStmtObject.hasProperty(RDF.type, RDF.Seq)) {
					Seq seq = model.getSeq(modelStmtObject);
					addToSequence(newValue, seq, position);
				} else {
					// An empty List may appear as a Resource 
					System.err.println(String.format("Failed to work out how to handle this multivalue: \n %s \n - %s \n- %s", getStatementFor(onEObject, eAttribute, model) , type , eAttribute.getName()));
					
					// Fall back is a list structure
					RDFList list = model.getList(modelStmtObject);
					addToList(newValue, list, position, eAttribute, onEObject);
				}
			}
			return;
		} else {
			// Does not exist anywhere so we need a NEW RDF representation			
			if (CONSOLE_OUTPUT_ACTIVE) {System.out.println("\n No existing container, making a new one");}
			
			for (Model model : namedModelsToUpdate) {
				if (preferListsForMultiValues) {
					newList(model, onEObject, eAttribute, newValue);
				} else {
					if (eAttribute.isOrdered()) {
						// Sequence
						addToSequence(newValue, newSequence(model, onEObject, eAttribute), 0);
					} else {
						// Bag
						addToBag(newValue, newBag(model, onEObject, eAttribute));
					}
				}
			}
			return;
		}
	}
	
	private void searchContainerAndRemoveValue(Object value, Container container, EStructuralFeature sf) {
		if (CONSOLE_OUTPUT_ACTIVE) {
			System.out.println("Remove from container: " + value);
		}

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
	
	private void removeFromBag(Object value, Container container, EObject onEObject, EAttribute eAttribute) {
		removeFromContainer(value, container, onEObject, eAttribute);
	}
	
	private void removeFromSeq(Object value, Container container, EObject onEObject, EAttribute eAttribute) {
		removeFromContainer(value, container, onEObject, eAttribute);
	}
	
	private void removeFromContainer(Object values, Container container, EObject onEObject, EAttribute eAttribute) {
		if (CONSOLE_OUTPUT_ACTIVE) {
			reportContainer("Before remove", container);
		}
		
		EStructuralFeature sf = eAttribute.eContainingFeature();
		if(values instanceof EList<?>) { 
			EList<?> valuesList = (EList<?>) values;
			valuesList.iterator().forEachRemaining(value -> 
				searchContainerAndRemoveValue(value, container, sf));
		} else {
			searchContainerAndRemoveValue(values, container, sf);
		}
		
		if (CONSOLE_OUTPUT_ACTIVE) {
			reportContainer("After remove", container);
		}
		
		// Check if container is empty (size 0), remove the blank node if true
		checkAndRemoveEmptyContainers(container, onEObject, eAttribute);
	}
	
	private void checkAndRemoveEmptyContainers(Container container, EObject onEObject, EAttribute eAttribute) {
		Model model = container.asResource().getModel();
		if (model.containsResource(container)) {
			if (0 == container.size()) {
				if (CONSOLE_OUTPUT_ACTIVE) {
					System.out.println("\n Removing empty container: container");
				}
				container.removeAll(RDF.type);
				Resource subjectNode = rdfGraphResource.getRDFResource(onEObject);
				Property property = getProperty(eAttribute);
				model.remove(subjectNode, property, container);
			}
		}
	}
	
	private void removeFromList(Object values, RDFList container, EObject onEObject, EAttribute eAttribute) {
		if(values instanceof List<?> valueList) {
			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println(String.format("list of values to remove: %s", valueList));
			}
			for (Object value : valueList) {
				container = removeOneFromList(value, container, eAttribute);
			}
		} else {
			container = removeOneFromList(values, container, eAttribute);
		}
		checkAndRemoveEmptyList(container, onEObject, eAttribute);
	}

	private RDFList removeOneFromList(Object value, RDFList container, EAttribute eAttribute) {
		Literal node = ResourceFactory.createTypedLiteral(value);
		if (CONSOLE_OUTPUT_ACTIVE) {
			System.out.println(String.format("removing: %s", node));
		}
		if (eAttribute.isUnique()) {
			while (container.contains(node)) {
				container = container.remove(node);
			}
		} else {
			container = container.remove(node);
		}
		return container;
	}
	
	private void checkAndRemoveEmptyList(RDFList container, EObject onEObject, EAttribute eAttribute) {
		Model model = container.getModel();
		if(container.isEmpty()) {
			Resource object = getStmtObjectFor(onEObject, eAttribute);
			Statement stmtToRemove = createStatement(onEObject, eAttribute, object);
			model.remove(stmtToRemove);
		}
	}

	public void removeMultiValueAttribute (List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue) {
		Resource onEObjectNode = rdfGraphResource.getRDFResource(onEObject);
		if (onEObjectNode.hasProperty(getProperty(eAttribute))) {
			// Exists on a model some where...
			List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
			for (Model model : namedModelsToUpdate) {
				// Try and the container from each model to be updated
				Resource modelStmtObject = getStmtObjectFor(onEObject, eAttribute, model);
				if ( (modelStmtObject.hasProperty(RDF.rest) && modelStmtObject.hasProperty(RDF.first)) 
						|| modelStmtObject.hasProperty(RDF.type, RDF.List) ) {
					RDFList list = modelStmtObject.as(RDFList.class);
					list.setStrict(true);
					removeFromList(oldValue, list, onEObject, eAttribute);
				} else if (modelStmtObject.hasProperty(RDF.type, RDF.Bag)) {
					Bag bag = model.getBag(modelStmtObject);
					removeFromBag(oldValue, bag, onEObject, eAttribute);
				} else if (modelStmtObject.hasProperty(RDF.type, RDF.Seq)) {
					Seq seq = model.getSeq(modelStmtObject);
					removeFromSeq(oldValue, seq, onEObject, eAttribute);
				} else {
					// no operation
					
					// An empty list would land here as it is an RDF.nil
				}
			}
		} else {
			System.err.println("Trying to remove a none existing RDFnode for a multivalue attribute");
		}
	}
	
	//
	// Model Elements
	
	public void addModelElement() {
		
	}
	
	public void removeModelElement() {
		
	}
	
	//
	// References
	
	
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
