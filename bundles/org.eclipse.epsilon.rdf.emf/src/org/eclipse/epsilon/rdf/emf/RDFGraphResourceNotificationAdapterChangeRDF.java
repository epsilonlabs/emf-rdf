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


import java.util.List;

import org.apache.jena.rdf.model.Resource;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EContentAdapter;

public class RDFGraphResourceNotificationAdapterChangeRDF extends EContentAdapter {

	@Override
	public void notifyChanged(Notification notification) {
		Object feature = notification.getFeature();
		if (null != feature) {
			featureNotification(feature, notification);
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

		System.err.printf("unhandled additive change : %s\n", featureClass.getName());
		return;
	}
	
	private void eAttributeFeatureNotification(EAttribute feature, Notification notification) {		
		
		EObject onEObject = (EObject) notification.getNotifier(); 	// RDF node
		EAttribute eAttributeChanged = (EAttribute) feature; 		// RDF property
		// eAttribute's values are the objects						// RDF object (node/literal)
		Object oldValue = notification.getOldValue();
		Object newValue = notification.getNewValue();
		boolean isOrdered = eAttributeChanged.isOrdered(); // If this is set then there is Order to the values.
		boolean isMany = eAttributeChanged.isOrdered();
		
		RDFGraphResourceImpl graphResource = RDFGraphResourceImpl.getRDFGraphResourceFor(onEObject);
		RDFGraphResourceUpdate rdfUpdater = graphResource.getRDFGraphUpdater();
		
		 List<Resource> namedModelURIs = graphResource.getResourcesForNamedModelsContaining(onEObject);		

		// Decode the notification event type
		switch (notification.getEventType()) {
		case Notification.ADD:	
			if(isMany) {
				rdfUpdater.addMultiValueAttribute(namedModelURIs, onEObject, eAttributeChanged, 
						newValue, oldValue);
			}
			break;
		case Notification.ADD_MANY:
			rdfUpdater.addMultiValueAttribute(namedModelURIs, onEObject, eAttributeChanged, 
					newValue, oldValue);
			break;
		case Notification.SET:
			// Single values, don't need to worry about order
			if (null == oldValue) {
				// Create new statement
				if (null == newValue) {
					// No old value and no new value - nothing to do
				} else {
					// Create new statement for value					
					namedModelURIs = graphResource.getResourcesForNamedModelsContaining(onEObject);
					if (namedModelURIs.isEmpty()) {
						// No named RDF models contain the object yet - fall back on the first one
						namedModelURIs = graphResource.getResourcesForAllNamedModels();
						if (!namedModelURIs.isEmpty()) {
							Resource first = namedModelURIs.get(0);
							namedModelURIs.clear();
							namedModelURIs.add(first);
						}
					}
					rdfUpdater.newSingleValueAttributeStatements(namedModelURIs, onEObject,
						eAttributeChanged, newValue);
				}
			} else {
				// Update existing statement
				if (null == newValue) {
					// Update existing statement to null
					namedModelURIs = graphResource.getResourcesForNamedModelsContaining(onEObject);
					rdfUpdater.removeSingleValueAttributeStatements(namedModelURIs, onEObject,
							eAttributeChanged, notification.getOldValue());
				} else {
					// Update existing statement value
					namedModelURIs = graphResource.getResourcesForNamedModelsContaining(onEObject);
					rdfUpdater.updateSingleValueAttributeStatements(namedModelURIs, onEObject,
							eAttributeChanged, newValue, oldValue);
				}
			}
			break;
		case Notification.REMOVE:
			if (eAttributeChanged.isMany()) {
				rdfUpdater.removeMultiValueAttribute(namedModelURIs, onEObject, eAttributeChanged, 
						newValue, oldValue);	
			}
			
			if (eAttributeChanged.isOrdered()) {

			} else {

			}
			break;
		case Notification.REMOVE_MANY:
			rdfUpdater.removeMultiValueAttribute(namedModelURIs, onEObject, eAttributeChanged, 
					newValue, oldValue);
			break;
		case Notification.UNSET:
			// Single values, don't need to worry about order
			namedModelURIs = graphResource.getResourcesForNamedModelsContaining(onEObject);
			rdfUpdater.removeSingleValueAttributeStatements(namedModelURIs, 
					onEObject, eAttributeChanged, notification.getOldValue());
			break;
		default:
			break;
		}
	}
		
}

	