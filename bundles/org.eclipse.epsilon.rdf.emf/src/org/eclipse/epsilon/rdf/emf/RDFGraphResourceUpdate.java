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
	
	public static void setSingleValueAttribute(List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue) {
		// A statement is formed as "subject–predicate–object"
		
		// Internal code guards
		//assert oldValue != null : "old value must exist";
		//assert newValue != null : "new value must exist"; //  Can set things null
		
		//
		// SUBJECT
		RDFGraphResourceImpl graphResource = getGraphResourceFor(onEObject);
		Resource rdfNode = graphResource.getRDFResource(onEObject);

		//
		// PREDICATE
		String nameSpace = eAttribute.getEContainingClass().getEPackage().getNsURI();
		String propertyURI = nameSpace + "#" + eAttribute.getName();
		Property property = ResourceFactory.createProperty(propertyURI);
		
		//
		// OBJECT (old)
		Literal oldObject;
		if (oldValue.getClass().equals(Date.class)) {
			Calendar c = Calendar.getInstance();
			c.setTime((Date) oldValue);
			String date = DateTimeUtils.calendarToXSDDateTimeString(c);		
			oldObject = ResourceFactory.createTypedLiteral(date, XSDDatatype.XSDdateTime);
		} else {
			oldObject = ResourceFactory.createTypedLiteral(oldValue);
		}
		
		//
		// OBJECT (new)
		Literal newObject = null;
		if (newValue.getClass().equals(Date.class)) {
			Calendar c = Calendar.getInstance();
			c.setTime((Date) newValue);
			String date = DateTimeUtils.calendarToXSDDateTimeString(c);
			newObject = ResourceFactory.createTypedLiteral(date, XSDDatatype.XSDdateTime);
		} else {
			newObject = ResourceFactory.createTypedLiteral(newValue);
		}
		
		//
		// STATEMENTS
		Statement newStatement = ResourceFactory.createStatement(rdfNode, property, newObject);
		Statement oldStatement = ResourceFactory.createStatement(rdfNode, property, oldObject);

		// TODO Go through the list of Named models to update and make the changes
		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			// Update Attributes expressed as a single RDF statement

			// This is an update, so we only replace the statement if it exists
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

	
	public static void unsetSingleValueAttribute(List<Resource> namedModelURIs, EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue) {
		// Object type values set a new value "null", 
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
