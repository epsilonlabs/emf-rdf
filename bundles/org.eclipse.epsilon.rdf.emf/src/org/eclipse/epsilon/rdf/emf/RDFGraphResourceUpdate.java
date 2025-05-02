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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.jena.atlas.lib.DateTimeUtils;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
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

	private static Statement getStatement (EObject eObject, EAttribute eAttribute, Object value) {
		// A statement is formed as "subject–predicate–object"
		
		//
		// SUBJECT
		RDFGraphResourceImpl graphResource = getGraphResourceFor(eObject);
		Resource rdfNode = graphResource.getRDFResource(eObject);

		//
		// PREDICATE
		String nameSpace = eAttribute.getEContainingClass().getEPackage().getNsURI();
		String propertyURI = nameSpace + "#" + eAttribute.getName();
		Property property = ResourceFactory.createProperty(propertyURI);
		
		//
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
		
		//
		// STATEMENTS
		return ResourceFactory.createStatement(rdfNode, property, object);
			
	}
	
	public static void updateSingleValueAttributeStatements(List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue) {

		// Internal code guards
		assert oldValue != null : "old value must exist";
		assert newValue != null : "new value must exist";
		
		RDFGraphResourceImpl graphResource = getGraphResourceFor(onEObject);

		Statement newStatement = getStatement(onEObject, eAttribute, newValue);
		Statement oldStatement = getStatement(onEObject, eAttribute, oldValue);

		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			if (model.contains(oldStatement)) {
				model.remove(oldStatement);
				model.add(newStatement);
			}
			else {
				System.err.println(String.format("Old statement not found : %s ", oldStatement));
			}
			
			// TODO remove these debugging lines
			//System.out.println("oldStatement: " + oldStatement);
			//System.out.println("newStatement: " + newStatement);			
			//model.write(System.out, "ttl");
			//reportRDFnodeProperties("AFTER", model, (Resource) oldObject);
		}
	}

	public static void removeSingleValueAttributeStatements(List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object oldValue) {
		// Object type values set a new value "null", remove the statement the deserializer uses the meta-model so we won't have missing attributes
		assert oldValue != null : "old value must exist";
		RDFGraphResourceImpl graphResource = getGraphResourceFor(onEObject);
		Statement oldStatement = getStatement(onEObject, eAttribute, oldValue);

		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			if (model.contains(oldStatement)) {
				model.remove(oldStatement);
			} else {
				System.err.println(String.format("Old statement not found : %s ", oldStatement));
			}
		}
	}
	
	public static void newSingleValueAttributeStatements (List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue) {
		assert newValue != null : "new value must exist";
		RDFGraphResourceImpl graphResource = getGraphResourceFor(onEObject);
		Statement newStatement = getStatement(onEObject, eAttribute, newValue);

		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			if (model.contains(newStatement)) {
				model.add(newStatement);
			} else {
				System.err.println(String.format("New statement already exists? : %s ", newStatement));
			}
		}
	}
	
	public static void addMultiValueAttribute () {
		
	}
	
	public static void setMultiValueAttribute() {
		
	}
	
	public static void unsetMultiValueAttribute() {
		
	}
	
	public static void addModelElement() {
		
	}
	
	public static void removeModelElement() {
		
	}
}
