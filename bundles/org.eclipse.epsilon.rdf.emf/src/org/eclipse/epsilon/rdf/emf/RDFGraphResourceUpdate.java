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
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.emf.common.util.EList;
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
	


	
	
	//
	// Multi-value Attributes


	//
	// Model Elements
	
	public void addModelElement() {
		
	}
	
	public void removeModelElement() {
		
	}	
	
	//
	// Generic Statement methods for EStructural Features
	
	private Literal createLiteral(Object value) {
		Literal object = null;
		if (value.getClass().equals(Date.class)) {
			Calendar c = Calendar.getInstance();
			c.setTime((Date) value);
			String date = DateTimeUtils.calendarToXSDDateTimeString(c);
			return ResourceFactory.createTypedLiteral(date, XSDDatatype.XSDdateTime);
		} else {
			return ResourceFactory.createTypedLiteral(value);
		}		
	}
	
	private Property createProperty(EStructuralFeature eStructuralFeature) {
		// PREDICATE
		String nameSpace = eStructuralFeature.getEContainingClass().getEPackage().getNsURI();
		String propertyURI = nameSpace + "#" + eStructuralFeature.getName();
		return ResourceFactory.createProperty(propertyURI);
	}
	
	private Resource getResourceObjectFor(EObject eObject, EStructuralFeature eStructuralFeature, Model model) {
		// SUBJECT
		Resource rdfNode = rdfGraphResource.getRDFResource(eObject);
		// PREDICATE
		Property property = createProperty(eStructuralFeature);
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
	
	private Statement findEquivalentStatement(Model model, EObject eob, EStructuralFeature eStructuralFeature, Object oldValue) {		
		Statement stmtToRemove = null;
		StmtIterator itOldStmt = model.listStatements(
			rdfGraphResource.getRDFResource(eob), createProperty(eStructuralFeature), (RDFNode) null);
		while (itOldStmt.hasNext() && stmtToRemove == null) {
			Statement stmt = itOldStmt.next();
			Object stmtObject = deserializer.deserializeProperty(stmt.getSubject(), eStructuralFeature);
			if (Objects.equals(oldValue, stmtObject)) {
				stmtToRemove = stmt;
			}
		}
		return stmtToRemove;
	}
	
	private Statement createStatement(EObject eObject, EStructuralFeature eStructuralFeature, Object value) {
		// A statement is formed as "subject–predicate–object"

		// Is the value an EObject? -- Statement should be a reference to something, statement object should be an RDF node
		if (value instanceof EObject) {
			Resource valueRDFNode = rdfGraphResource.getRDFResource((EObject) value);
			if (null != valueRDFNode) {
//				System.out.println("This value is has an RDF node: " + valueRDFNode);
				value = valueRDFNode;
			} else {
				// need to create an RDF node for a new EObject
				System.err.println("Creating a statement for an EObject that does not have an RDF node: " + value);
			}
		}
		
		// SUBJECT
		Resource rdfNode = rdfGraphResource.getRDFResource(eObject);
		// PREDICATE
		Property property = createProperty(eStructuralFeature);
		// OBJECT
		if (value instanceof RDFNode) {
			return ResourceFactory.createStatement(rdfNode, property, (RDFNode) value); // Reference
		} else {
			return ResourceFactory.createStatement(rdfNode, property, createLiteral(value)); // Literal value (we could add an option here to make a statement with a "String" value
		}
		
	}
	
	private Statement getStatementFor(EObject subject, EStructuralFeature eStructuralFeature, Model model) {
		// SUBJECT
		Resource rdfNode = rdfGraphResource.getRDFResource(subject);
		// PREDICATE
		Property property = createProperty(eStructuralFeature);
		return model.getProperty(rdfNode, property);
	}
	
	//
	// Containers statement operations
	
	private void checkAndRemoveEmptyContainers(Container container, EObject onEObject, EStructuralFeature eStructuralFeature) {
		Model model = container.asResource().getModel();
		if (model.containsResource(container)) {
			if (0 == container.size()) {
				if (CONSOLE_OUTPUT_ACTIVE) {
					System.out.println("\n Removing empty container: container");
				}
				container.removeAll(RDF.type);
				Resource subjectNode = rdfGraphResource.getRDFResource(onEObject);
				Property property = createProperty(eStructuralFeature);
				model.remove(subjectNode, property, container);
			}
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
	
	private void removeFromContainer(Object values, Container container, EObject onEObject, EStructuralFeature eStructuralFeature) {
		if (CONSOLE_OUTPUT_ACTIVE) {
			reportContainer("Before remove", container);
		}
		
		EStructuralFeature sf = eStructuralFeature.eContainingFeature();
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
		checkAndRemoveEmptyContainers(container, onEObject, eStructuralFeature);
	}
	
	private void removeFromSeq(Object value, Container container, EObject onEObject, EStructuralFeature eStructuralFeature) {
		removeFromContainer(value, container, onEObject, eStructuralFeature);
	}
	
	private void removeFromBag(Object value, Container container, EObject onEObject, EStructuralFeature eStructuralFeature) {
		removeFromContainer(value, container, onEObject, eStructuralFeature);
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
		// SUBJECT
		Resource subjectNode = rdfGraphResource.getRDFResource(onEObject);
		// PREDICATE
		Property property = createProperty(eStructuralFeature);
		// OBJECT
		Seq objectNode = model.createSeq();
		model.add(subjectNode,property,objectNode);
		return objectNode;
	}
	
	private Bag newBag(Model model, EObject onEObject, EStructuralFeature eStructuralFeature) {
		// SUBJECT
		Resource subjectNode = rdfGraphResource.getRDFResource(onEObject);
		// PREDICATE
		Property property = createProperty(eStructuralFeature);
		// OBJECT
		Bag objectNode = model.createBag();
		model.add(subjectNode, property, objectNode);
		return objectNode;
	}
	
	//
	// List statement operations	
	
	private void checkAndRemoveEmptyList(RDFList container, EObject onEObject, EStructuralFeature eStructuralFeature) {
		Model model = container.getModel();
		if(container.isEmpty()) {
			Resource object = getResourceObjectFor(onEObject, eStructuralFeature, model);
			Statement stmtToRemove = createStatement(onEObject, eStructuralFeature, object);
			model.remove(stmtToRemove);
		}
	}
	
	private RDFList removeOneFromList(Object value, RDFList container, EStructuralFeature eStructuralFeature) {
		ExtendedIterator<RDFNode> containerItr = container.iterator();
		while (containerItr.hasNext()) {
			RDFNode rdfNode = containerItr.next();
			Object deserializedValue = deserializer.deserializeValue(rdfNode, eStructuralFeature);
			if(value.equals(deserializedValue)) {
				if (CONSOLE_OUTPUT_ACTIVE) {
					System.out.println(String.format("removing: %s == %s", value , deserializedValue));
				}
				if (eStructuralFeature.isUnique()) {
					while (container.contains(rdfNode)) {
						container = container.remove(rdfNode);
					}
				} else {
					container = container.remove(rdfNode);
				}
				return container;
			}
		}
		return container;
	}
	
	private void removeFromList(Object values, RDFList container, EObject onEObject, EStructuralFeature eStructuralFeature) {
		if(values instanceof List<?> valueList) {
			if (CONSOLE_OUTPUT_ACTIVE) {
				System.out.println(String.format("list of values to remove: %s", valueList));
			}
			for (Object value : valueList) {
				container = removeOneFromList(value, container, eStructuralFeature);
			}
		} else {
			container = removeOneFromList(values, container, eStructuralFeature);
		}
		checkAndRemoveEmptyList(container, onEObject, eStructuralFeature);
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
			// This should never happen, empty is handled elsewhere
			container.concatenate(newList);
			return;
		} 

		if (!eStructuralFeature.isOrdered()) {
			container.concatenate(newList);
			return;
		} 
		
		// Ordered lists that are not empty will be handled with the methods below.
		if(eStructuralFeature.isOrdered()) {
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
				Property property = createProperty(eStructuralFeature);
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
	
	private RDFList createRDFListOnModel (Object values, Model model) {
		List<RDFNode> rdfNodes = new ArrayList<RDFNode>();
		if(values instanceof List<?> valuesList) {
			valuesList.forEach(v -> rdfNodes.add(ResourceFactory.createTypedLiteral(v)));
		} else {
			rdfNodes.add(ResourceFactory.createTypedLiteral(values));
		}
		return model.createList(rdfNodes.iterator());
	}
	
	private RDFList newList(Model model, EObject onEObject, EStructuralFeature eStructuralFeature, Object values) {		
		// SUBJECT
		Resource subjectNode = rdfGraphResource.getRDFResource(onEObject);
		// PREDICATE
		Property property = createProperty(eStructuralFeature);
		// OBJECT
		RDFList objectNode = createRDFListOnModel(values, model);
		model.add(subjectNode, property, objectNode);
		return objectNode;
	}
	
	//
	// Single-value Features operations
	
	public void newSingleValueEStructuralFeatureStatements(List<Resource> namedModelURIs, EObject onEObject, EStructuralFeature eStructuralFeature, Object newValue) {
		assert newValue != null : "new value must exist";
		Statement newStatement = createStatement(onEObject, eStructuralFeature, newValue);

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
	
	public void removeSingleValueEStructuralFeatureStatements(List<Resource> namedModelURIs, EObject onEObject, EStructuralFeature eStructuralFeature, Object oldValue) {
		// Object type values set a new value "null", remove the statement the deserializer uses the meta-model so we won't have missing attributes
		assert oldValue != null : "old value must exist";		
		Statement oldStatement = createStatement(onEObject, eStructuralFeature, oldValue);

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
				Statement stmtToRemove = findEquivalentStatement(model, onEObject, eStructuralFeature, oldValue);
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
	
	public void updateSingleValueEStructuralFeatureStatements(List<Resource> namedModelURIs, EObject onEObject, EStructuralFeature eStructuralFeature, Object newValue, Object oldValue) {
		assert oldValue != null : "old value must exist";
		assert newValue != null : "new value must exist";

		RDFGraphResourceImpl graphResource = (RDFGraphResourceImpl) onEObject.eResource();
		Statement newStatement = createStatement(onEObject, eStructuralFeature, newValue);
		Statement oldStatement = createStatement(onEObject, eStructuralFeature, oldValue);

		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		
		boolean found = false;
		/*
		 * First check for an old statement based on what we think the statement should be 
		 */
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
				Statement stmtToRemove = findEquivalentStatement(model, onEObject, eStructuralFeature, oldValue);
				if (stmtToRemove != null) {
					model.remove(stmtToRemove);
					model.add(newStatement);
					found = true;
				}
			}
		}

		if (!found) {
			/*
			 * There is a case when the EAttribute has a default value define and there are
			 * no statements in the RDF to match.
			 */
			if (oldValue.equals(eStructuralFeature.getDefaultValue())) {
				{
					System.out.println(String.format(
							"Old statement not found, but the oldvalue matches the models default value, so there might not be a statement.\nAdding a statement to the first model. default value %s - old value %s ",
							eStructuralFeature.getDefaultValue(), oldValue));					
					namedModelsToUpdate.get(0).add(newStatement);
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
	
	
	
	//
	// Multi-value Feature operations
	
	public void removeMultiEStructuralFeature (List<Resource> namedModelURIs, EObject onEObject, EStructuralFeature eStructuralFeature, Object newValue, Object oldValue) {
		Resource onEObjectNode = rdfGraphResource.getRDFResource(onEObject);
		if (onEObjectNode.hasProperty(createProperty(eStructuralFeature))) {
			// Exists on a model some where...
			List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
			for (Model model : namedModelsToUpdate) {
				// Try and the container from each model to be updated
				Resource objectResource = getResourceObjectFor(onEObject, eStructuralFeature, model);
				if ( (objectResource.hasProperty(RDF.rest) && objectResource.hasProperty(RDF.first)) 
						|| objectResource.hasProperty(RDF.type, RDF.List) ) {
					RDFList list = objectResource.as(RDFList.class);
					list.setStrict(true);
					removeFromList(oldValue, list, onEObject, eStructuralFeature);
				} else if (objectResource.hasProperty(RDF.type, RDF.Bag)) {
					Bag bag = model.getBag(objectResource);
					removeFromBag(oldValue, bag, onEObject, eStructuralFeature);
				} else if (objectResource.hasProperty(RDF.type, RDF.Seq)) {
					Seq seq = model.getSeq(objectResource);
					removeFromSeq(oldValue, seq, onEObject, eStructuralFeature);
				} else {
					// no operation
					
					// An empty list would land here as it is an RDF.nil
				}
			}
		} else {
			System.err.println("Trying to remove a none existing RDFnode for a multivalue attribute");
		}
	}
	
	public void addMultiValueEStructuralFeature (List<Resource> namedModelURIs, EObject onEObject, EStructuralFeature eStructuralFeature, Object newValue, Object oldValue, int position) {
		// sequence (ordered), bag (unordered)
		
		List<Model> namedModelsToUpdate = rdfGraphResource.getNamedModels(namedModelURIs);
		Resource onEObjectNode = rdfGraphResource.getRDFResource(onEObject);
		
		// Work out if we are adding a NEW multi-value attribute with no existing RDF node.
		if(onEObjectNode.hasProperty(createProperty(eStructuralFeature))) {
			// Exists on a model some where...
			for (Model model : namedModelsToUpdate) {
				Resource objectResource = getResourceObjectFor(onEObject, eStructuralFeature, model);
				Class<? extends Resource> type = objectResource.getClass();

				// If we have one of these types, then we are updating an existing statement on a model
				if ( (objectResource.hasProperty(RDF.rest) && objectResource.hasProperty(RDF.first)) 
							|| objectResource.hasProperty(RDF.type, RDF.List)) {
					// Lists can be ordered or unique, both or none.
					RDFList list = model.getList(objectResource);
					addToList(newValue, list, position, eStructuralFeature, onEObject);
				} else if (objectResource.equals(RDF.nil)) {
					// List - Empty lists may be represented with an RDF.nil value
					Statement stmt = createStatement(onEObject, eStructuralFeature, objectResource);
					model.remove(stmt);
					newList(model, onEObject, eStructuralFeature, newValue);
				} else if (objectResource.hasProperty(RDF.type, RDF.Bag)) {
					Bag bag = model.getBag(objectResource);
					addToBag(newValue, bag);
				} else if (objectResource.hasProperty(RDF.type, RDF.Seq)) {
					Seq seq = model.getSeq(objectResource);
					addToSequence(newValue, seq, position);
				} else {
					// An empty List may appear as a Resource 
					System.err.println(String.format("Failed to work out how to handle this multivalue: \n %s \n - %s \n- %s", getStatementFor(onEObject, eStructuralFeature, model) , type , eStructuralFeature.getName()));
					
					// Fall back is a list structure
					RDFList list = model.getList(objectResource);
					addToList(newValue, list, position, eStructuralFeature, onEObject);
				}
			}
			return;
		} else {
			// Does not exist anywhere so we need a NEW RDF representation			
			if (CONSOLE_OUTPUT_ACTIVE) {System.out.println("\n No existing container, making a new one");}
			
			for (Model model : namedModelsToUpdate) {
				if (preferListsForMultiValues) {
					newList(model, onEObject, eStructuralFeature, newValue);
				} else {
					if (eStructuralFeature.isOrdered()) {
						// Sequence
						addToSequence(newValue, newSequence(model, onEObject, eStructuralFeature), 0);
					} else {
						// Bag
						addToBag(newValue, newBag(model, onEObject, eStructuralFeature));
					}
				}
			}
			return;
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
