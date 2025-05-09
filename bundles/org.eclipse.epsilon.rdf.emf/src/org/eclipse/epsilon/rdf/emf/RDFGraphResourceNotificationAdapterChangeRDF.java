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
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.impl.EAttributeImpl;
import org.eclipse.emf.ecore.impl.EReferenceImpl;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.util.EcoreEList;
import org.eclipse.emf.ecore.util.EcoreUtil;

public class RDFGraphResourceNotificationAdapterChangeRDF extends EContentAdapter {

	@Override
	public void notifyChanged(Notification notification) {

		// Decode the notification type
		switch (notification.getEventType()) {
		case Notification.ADD:
			//additiveChange(notification);
			break;			
		case Notification.ADD_MANY:
			//additiveChange(notification);
			break;
		case Notification.SET:
			additiveChange(notification);
			break;
			
		case Notification.REMOVE:
			subtractiveChange (notification);
			break;			
		case Notification.REMOVE_MANY:
			subtractiveChange (notification);
			break;
		case Notification.UNSET:
			subtractiveChange (notification);
			break;
			
		default:
			break;			
		}
	}
	
	private void additiveChange (Notification notification) {
		Object feature = notification.getFeature();
		Object value = notification.getNewValue();
		
		if(null != feature) {
			// Work out the change based on feature
			Class<? extends Object> featureClass = feature.getClass();

			if(feature instanceof EAttribute) {
				additiveFeatureEAttribute(feature, value, notification);
				return;
			}

			if(feature instanceof EReference) {
				additiveFeatureEReference(feature, value, notification);
				return;
			}

			if(feature instanceof EObject) {
				addativeFeatureEObject(feature, value, notification);
				return;
			}
			System.err.println(String.format("\n unhandled additive change : %s ", featureClass.getName()));
			return;
		} else {
			// Work out the change base on newValue
			identifyByValue(value, notification);
		}		
	}
	
	private void additiveFeatureEAttribute(Object feature, Object value, Notification notification) {
		EObject onEObject = (EObject)notification.getNotifier();	// RDF node
		EAttribute eAttributeChanged = (EAttribute) feature;		// RDF property
		// eAttribute's is value									// RDF object (node/literal)
		
		// TODO How many, are they ordered?
		boolean isOrdered = eAttributeChanged.isOrdered();// If this is set then there is Order to the values.				
		int orderPosition = -1; // This is not notification.getPosition()
		
		// TODO Make a list of Named Models that should be checked for the statements (not just an rdfNode?), and update them
		RDFGraphResourceImpl graphResource = RDFGraphResourceUpdate.getGraphResourceFor(onEObject);				
		List<Resource> namedModelURIs = graphResource.getResourcesForNamedModelsContaining(onEObject);
		
		if(null == notification.getOldValue()) {
			// Create new statement
			if (null == notification.getNewValue()) {
				// Create statement null
				// TODO Handle creating a new statement for a single value with null
			} else {
				// Create statement value
				// TODO Handle creating a new statement for a single value with a value
			}
				
		} else {
			// Update existing statement
			 if(null == notification.getNewValue()) {
				 // Update existing statement to null
				 // TODO Update existing statement for single value being set to null 
			 } else {
				 // Update existing statement value
				 RDFGraphResourceUpdate.updateSingleValueAttributeStatements(namedModelURIs, onEObject, eAttributeChanged, value, notification.getOldValue());
			 }
		}

	}

	private void additiveFeatureEReference(Object feature, Object value, Notification notification) {
		EObject onEObject = (EObject)notification.getNotifier();	// RDF node	
		EReferenceImpl eReference = (EReferenceImpl) feature;		// RDF property
		
		RDFGraphResourceImpl graphResource = RDFGraphResourceUpdate.getGraphResourceFor(onEObject);
		if(null == graphResource) {
			// TODO what happens if there is no graph resource?
			return;
		}
		
		// TODO check for ordering and position information
		boolean isOrdered = eReference.isOrdered();
		int orderPosition = -1 ; // This is not notification.getPosition()
		
		// Single
		if (value instanceof EObject) {
			EObject referenced = (EObject) value;						// RDF object (node)
			// TODO Deal with ordering
			
			return;
		}
		
		// Array
		if (value instanceof ArrayList) {
			ArrayList<EObject> referenced = (ArrayList) value;						// RDF object (node)
			// TODO Deal with ordering
			return;
		}
		return;
	}

	private void addativeFeatureEObject(Object feature, Object value, Notification notification) {	
		EObject eObject = (EObject) feature;
	}

	private void subtractiveChange(Notification notification) {
		Object feature = notification.getFeature();
		Object value = notification.getOldValue();

		if (null != feature) {
			// Work out what was removed by Feature
			//identifyByFeature(feature, value, notification);
			Class<? extends Object> featureClass = feature.getClass();

			if(feature instanceof EAttribute) {
				//subtractiveFeatureEAttribute(feature, value, notification);
				return;
			}

			if(feature instanceof EReference) {
				//subtractiveFeatureEReference(feature, value, notification);
				return;
			}

			if(value instanceof EObject) {
				//subtractiveFeatureEObject(feature, value, notification);
				return;
			}
			
			System.err.println(String.format("\n unhandled subtractive change : %s ", featureClass.getName()));
			return;

		} else {
			// Work out what was removed by old Value?
			return;
		}
	}
	
 	private void identifyByValue(Object value, Notification notification) {
		if (null == value) {
			// TODO identify by value when incoming value is null
			return;
		} else {
			// TODO identify by value when incoming value is not null
			Object targetClass = this.target.getClass();
			Object notifier = notification.getNotifier();
			
			// Can we work out what the change was from the type of notifier?
			if (notifier instanceof RDFGraphResourceImpl) {
				RDFGraphResourceImpl notifierResource = (RDFGraphResourceImpl) notifier;				
				return;
			}
			
			// Can we work out what the change was is the value is an EObject?
			if ( (value instanceof EObject)
					|| (value instanceof DynamicEObjectImpl) ){
				// TODO Handle new value as a type of EObject
				EObject valueEObject = (EObject)value;
				
				// EObject is likely an RDF node?				
				return;
			}
		}
	}
	
	private Resource identifyEObjectsRDFnode (EObject eObject) {	
		if (eObject instanceof RDFGraphResourceImpl) {
			RDFGraphResourceImpl rdfGraphResource = (RDFGraphResourceImpl) eObject.eResource();
			Resource rdfNode = rdfGraphResource.getRDFResource(eObject);
			return rdfNode;
		}
		return null;
	}

}

	