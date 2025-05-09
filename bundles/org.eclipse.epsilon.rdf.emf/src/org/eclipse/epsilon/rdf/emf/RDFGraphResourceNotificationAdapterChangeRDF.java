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
		Object feature = notification.getFeature();
		if (null != feature) {
			// Work out the change based on feature
			featureNotification(feature, notification);
		} else {
			// Notification is not for a feature
		}
	}
	
	private void featureNotification (Object feature, Notification notification){		
		Class<? extends Object> featureClass = feature.getClass();

		if (feature instanceof EAttribute) {
			eAttributeFeatureNotification((EAttribute) feature, notification);
			return;
		}

		if (feature instanceof EReference) {
			//eReferenceFeatureNotification((EReference) feature, notification);
			return;
		}

		if (feature instanceof EObject) {
			//eObjectFeatureNotification((EObject) feature, notification);
			return;
		}

		System.err.println(String.format("\n unhandled additive change : %s ", featureClass.getName()));
		return;
	}
	
	private void eAttributeFeatureNotification(EAttribute feature, Notification notification) {		
		EObject onEObject = (EObject) notification.getNotifier(); 	// RDF node
		EAttribute eAttributeChanged = (EAttribute) feature; 		// RDF property
		// eAttribute's values are the objects						// RDF object (node/literal)
		Object oldValue = notification.getOldValue();
		Object newValue = notification.getNewValue();
		
		boolean isOrdered = eAttributeChanged.isOrdered(); // If this is set then there is Order to the values.
		int orderPosition = -1; // This is not notification.getPosition()
		
		RDFGraphResourceImpl graphResource = RDFGraphResourceUpdate.getGraphResourceFor(onEObject);				
		List<Resource> namedModelURIs = graphResource.getResourcesForNamedModelsContaining(onEObject);
				
		// Decode the notification event type
		switch (notification.getEventType()) {
		case Notification.ADD:
			if(isOrdered) {

			} else {

			}
			break;
		case Notification.ADD_MANY:
			if(isOrdered) {

			} else {

			}
			break;
		case Notification.SET:
			// Single values, don't need to worry about order?
			if (null == oldValue) {
				// Create new statement
				if (null == newValue) {
					// Create new statement for null value
				} else {
					// Create new statement for value
					RDFGraphResourceUpdate.newSingleValueAttributeStatements(namedModelURIs, onEObject,
							eAttributeChanged, newValue);
				}

			} else {
				// Update existing statement
				if (null == newValue) {
					// Update existing statement to null
					RDFGraphResourceUpdate.removeSingleValueAttributeStatements(namedModelURIs, onEObject,
							eAttributeChanged, notification.getOldValue());

				} else {
					// Update existing statement value
					RDFGraphResourceUpdate.updateSingleValueAttributeStatements(namedModelURIs, onEObject,
							eAttributeChanged, newValue, oldValue);
				}
			}

			break;

		case Notification.REMOVE:
			if(isOrdered) {

			} else {

			}

			break;
		case Notification.REMOVE_MANY:
			if(isOrdered) {

			} else {

			}

			break;
		case Notification.UNSET:
			// Single values, don't need to worry about order?
			RDFGraphResourceUpdate.removeSingleValueAttributeStatements(namedModelURIs, 
					onEObject, eAttributeChanged, notification.getOldValue());
			break;
		default:
			break;
		}
	}
	
	private void notImplmentedWarning (Notification notification, boolean isOrdered) {
		String feature = notification.getFeature().getClass().toString();
		String operation = "";
		switch (notification.getEventType()) {
		case Notification.ADD:
			operation = "ADD";
			break;
		case Notification.ADD_MANY:
			operation = "ADD_MANY";
			break;
		case Notification.SET:
			operation = "SET";
			break;
		case Notification.REMOVE:
			operation = "REMOVE";
			break;
		case Notification.REMOVE_MANY:
			operation = "REMOVE_MANY";
			break;
		case Notification.UNSET:
			operation = "UNSET";
			break;
		default:
			break;
		}
		String order = "";
		if(isOrdered) {
			order = "ordered ";
		} else {
			order = "unordered ";
		}
		System.err.println(String.format("%s %s %s not implmented", feature, operation, order));
	}
}

	