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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
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
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;

public class RDFGraphResourceUpdate {
	
	private RDFGraphResourceUpdate() {
		// This class is not meant to be instantiated
	}
	
	public static RDFGraphResourceImpl getGraphResourceFor(EObject eObject) {
		if(eObject.eResource() instanceof RDFGraphResourceImpl) {
			return (RDFGraphResourceImpl) eObject.eResource();
		}
		return null;
	}

	private static Statement createStatement(EObject eObject, EAttribute eAttribute, Object value) {
		// A statement is formed as "subject–predicate–object"

		// SUBJECT
		RDFGraphResourceImpl graphResource = getGraphResourceFor(eObject);
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
	
	private static Resource getObject (EObject eObject, EAttribute eAttribute) {
		//
		// SUBJECT
		RDFGraphResourceImpl graphResource = getGraphResourceFor(eObject);
		Resource rdfNode = graphResource.getRDFResource(eObject);

		//
		// PREDICATE
		Property property = getProperty(eAttribute);
		
		//
		// OBJECT
		Resource object = (Resource) rdfNode.getProperty(property).getObject();
		return object;
	}
	
	public static void updateSingleValueAttributeStatements(List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue) {
		assert oldValue != null : "old value must exist";
		assert newValue != null : "new value must exist";

		RDFGraphResourceImpl graphResource = getGraphResourceFor(onEObject);
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

	public static void removeSingleValueAttributeStatements(List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object oldValue) {
		// Object type values set a new value "null", remove the statement the deserializer uses the meta-model so we won't have missing attributes
		assert oldValue != null : "old value must exist";
		RDFGraphResourceImpl graphResource = getGraphResourceFor(onEObject);
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
	
	public static void newSingleValueAttributeStatements (List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue) {
		assert newValue != null : "new value must exist";
		RDFGraphResourceImpl graphResource = getGraphResourceFor(onEObject);
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
	
	private static void addToBag(Object values, Bag bag) {
		reportContainer("Before add to Bag ", bag);
		if (values.getClass().equals(ArrayList.class)) {
			ArrayList<Object> list = (ArrayList<Object>) values;
			list.forEach(v -> bag.add(v));
		} else {
			bag.add(values);
		}
		reportContainer("After add to Bag ", bag);
	}
	
	private static void addToContainer(Object values, Container container) {
		reportContainer("Before add to container ", container);
		if (values.getClass().equals(ArrayList.class)) {
			ArrayList<Object> list = (ArrayList<Object>) values;
			list.forEach(v -> container.add(v));
		} else {
			container.add(values);
		}
		reportContainer("After add to container ", container);
	}
	
	public static void addMultiValueAttribute (List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue) {
		RDFGraphResourceImpl graphResource = getGraphResourceFor(onEObject);
		Resource object = getObject(onEObject, eAttribute);
		
		eAttribute.isOrdered();
		eAttribute.isUnique();
		
		// Need to get at the Data models and check for the onEObject.	
		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
						
			if (object.hasProperty(RDF.type, RDF.List)) {
				RDFList list = object.as(RDFList.class);
				System.out.println("\nobject RDF.List:");
			}
			
			if (object.hasProperty(RDF.type, RDF.Bag)) {
				Bag bag = model.getBag(object);
				//addToBag(newValue, bag);
				addToContainer(newValue, bag);
			}
	
			if (object.hasProperty(RDF.type, RDF.Seq)) {
				Seq seq = model.getSeq(object);
				addToContainer(newValue, seq);
			}
			
			if (object.hasProperty(RDF.type, RDF.Alt)) {
				Alt alt = model.getAlt(object);
				addToContainer(newValue, alt);
			}
		}
	}

	
	private static void removeFromContainer(Object values, Container container) {
		reportContainer("Before remove", container);
		// System.out.println("Values class is " + values.getClass());
		if(values instanceof EList<?>) { 
			EList<?> valuesList = (EList<?>) values;
			if (valuesList.size() == container.size()) {
				Statement typeStatement = container.getProperty(RDF.type);
				container.removeProperties();
				container.addProperty(typeStatement.getPredicate(), typeStatement.getObject());
			} else {
				// remove some statements
			}
		} else {
			// not a list?
		}
		reportContainer("After remove", container);
	}
	
	public static void removeMultiValueAttribute (List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue) {
		RDFGraphResourceImpl graphResource = getGraphResourceFor(onEObject);
		Resource object = getObject(onEObject, eAttribute);
		
		// Need to get at the Data models and check for the onEObject.	
		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			if (object.hasProperty(RDF.type, RDF.List)) {
				RDFList list = object.as(RDFList.class);
				System.out.println("\nobject RDF.List:");
			}
			
			if (object.hasProperty(RDF.type, RDF.Bag)) {
				Bag bag = model.getBag(object);
				removeFromContainer(oldValue, bag);
			}
	
			if (object.hasProperty(RDF.type, RDF.Seq)) {
				Seq seq = model.getSeq(object);
				removeFromContainer(oldValue, seq);
			}
			
			if (object.hasProperty(RDF.type, RDF.Alt)) {
				Alt alt = model.getAlt(object);
				removeFromContainer(oldValue, alt);
				
				System.out.println("\nobject RDF.Alt - Size: " + alt.size());
				alt.iterator().forEach(i -> System.out.println("  * " + i));
				System.out.println("Default: " + alt.getDefault());			
			}			
		}
	}
	
	public static void setMultiValueAttribute() {
		
	}
	
	public static void unsetMultiValueAttribute() {
		
	}
	
	public static void addModelElement() {
		
	}
	
	public static void removeModelElement() {
		
	}

	private static void reportContainer(String label, Container container) {
		System.out.println(String.format("\n%s Containter: Type %s , Size %s",
				label, container.getProperty(RDF.type).getObject(), container.size()));
		container.iterator().forEach(i -> System.out.println("  * " + i));
	}

}
