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
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.util.EcoreUtil;

// Try to keep the logic flow in here the same as the RDFGraphResourceNotificationAdapterChangeRDF
public class RDFGraphResourceNotificationAdapterTrace extends EContentAdapter {

	StringBuilder processTrace;
	int i = 0;

	@Override
	public void notifyChanged(Notification notification) {
		// Decode the notification
		processTrace = new StringBuilder();
		processTrace.append(String.format("\n[ Notification ]"));

		Object feature = notification.getFeature();
		if (null != feature) {
			// Work out the change based on feature
			featureNotification(feature, notification);
		} else {
			// Notification is not for a feature
			processTrace.append(String.format("\n - Not a Feature notification \n - %s ", notification));
		}

		System.out.println(processTrace + "\n\n");
	}

	private void featureNotification (Object feature, Notification notification){		
		Class<? extends Object> featureClass = feature.getClass();

		if (feature instanceof EAttribute) {
			eStructuralFeatureNotification((EAttribute) feature, notification);
			return;
		}

		if (feature instanceof EReference) {
			eStructuralFeatureNotification((EReference) feature, notification);
			return;
		}

		if (feature instanceof EObject) {
			//eObjectNotification((EObject) feature, notification);
			return;
		}
		
		processTrace.append(String.format(" [!] - Feature class not handled:\n%s", featureClass.getName()));
		return;
	}
	
	private void eStructuralFeatureNotification(EStructuralFeature eStructuralFeature, Notification notification) {		
		EObject onEObject = (EObject) notification.getNotifier(); 	// RDF node
		EStructuralFeature changedFeature = eStructuralFeature; 	// RDF property
		// eAttribute's is value 									// RDF object (node/literal)


		
		Object oldValue = notification.getOldValue();
		Object newValue = notification.getNewValue();

		if (changedFeature instanceof EAttribute) {
			processTrace.append(String.format("\n EAttribute "));
		} else if (changedFeature instanceof EReference) {
			processTrace.append(String.format("\n EReference "));
		} else {
			processTrace.append(String.format("\n This is new?! -- %s ", changedFeature.getClass()));
		}
		
		boolean isOrdered = changedFeature.isOrdered(); // If this is set then there is Order to the values.
		boolean isUnique = changedFeature.isUnique();
		boolean isMany = changedFeature.isMany();
		processTrace.append(String.format( "(ordered is %s, unique is %s, many is %s) - ",
		isOrdered, isUnique, isMany));
		

		// Decode the notification event type
		switch (notification.getEventType()) {
		case Notification.ADD:
			processTrace.append("ADD");
			reportEObjectIdentity(onEObject);
			reportEStructuralFeatureChange(changedFeature, oldValue, newValue);
			break;
		case Notification.ADD_MANY:
			processTrace.append("ADD_MANY");
			if(isMany && null != onEObject) {
				reportEObjectIdentity(onEObject);
				reportEStructuralFeatureChange(changedFeature, oldValue, newValue);
			} else {
				
			}
			break;
		case Notification.SET:
			processTrace.append("SET");
			reportEObjectIdentity(onEObject);
			reportEStructuralFeatureChange(changedFeature, oldValue, newValue);
			if (null == oldValue) {
				// Create new statement
				if (null == newValue) {
					// Create new statement for null value
					processTrace.append(String.format("\n ** new statement set to null"));
				} else {
					// Create new statement for value
					processTrace.append(String.format("\n ** new statement set to value %s", newValue));
				}
			} else {
				// Update existing statement
				if (null == newValue) {
					// Update existing statement to null
					processTrace.append(String.format("\n ** existing statement set to null"));
				} else {
					// Update existing statement value
					processTrace.append(String.format("\n ** existing statement set to value %s", newValue));
				}
			}
			break;
		case Notification.REMOVE:
			processTrace.append("REMOVE");
			reportEObjectIdentity(onEObject);
			reportEStructuralFeatureChange(changedFeature, oldValue, newValue);
			break;
		case Notification.REMOVE_MANY:
			processTrace.append("REMOVE_MANY");			
			reportEObjectIdentity(onEObject);
			reportEStructuralFeatureChange(changedFeature, oldValue, newValue);
			break;
		case Notification.UNSET:
			processTrace.append("UNSET");
			if(isMany) { processTrace.append(" isMany() true"); } else { processTrace.append(" is Many false"); }
			if(isOrdered) {
				notImplmentedWarning(notification, isOrdered);
			}
			reportEObjectIdentity(onEObject);
			reportEStructuralFeatureChange(changedFeature, oldValue, newValue);
			break;
		default:
			break;
		}
	}
	
	private void notImplmentedWarning (Notification notification, boolean isOrdered) {
		String feature = notification.getFeature().getClass().getSimpleName();
		
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
			order = "ordered";
		} else {
			order = "unordered";
		}
		processTrace.append(String.format("\n [!] %s %s (%s) not implmented", feature, operation, order));
	}

	private Resource identifyEObjectsRDFnode(EObject eObject) {
		if (eObject instanceof RDFGraphResourceImpl) {
			RDFGraphResourceImpl rdfGraphResource = (RDFGraphResourceImpl) eObject.eResource();
			Resource rdfNode = rdfGraphResource.getRDFResource(eObject);

			processTrace.append(reportNamedModelsContaining(eObject));
			return rdfNode;
		}
		processTrace.append(String.format(" ! No RDFGraphResourceImpl for : %s ", eObject));
		return null;

	}
	
	// REPORTING code

	private void reportEStructuralFeatureChange(EStructuralFeature eStructuralFeature, Object oldValue, Object newValue) {		
		String multi = "";
		if(eStructuralFeature.isMany()) {
			multi = "many";
		} else {
			multi = "not many";
		}
		
		String order = "";
		if (eStructuralFeature.isOrdered()) {
			order = "ordered";
		} else {
			order = "not ordered";
		}
		
		String unique = "";
		if (eStructuralFeature.isUnique()) {
			unique = "unique";
		} else {
			unique = "not unique";
		}
		
		
		if (eStructuralFeature instanceof EAttribute) {
			EAttribute eAttributeChanged = (EAttribute) eStructuralFeature;
			processTrace.append(String.format("\n - EAttribute changed: (%s, %s, %s)\n\t%s - %s : %s -> %s", order, unique, multi,
					eAttributeChanged.getEAttributeType().getName(), eAttributeChanged.getName(), oldValue, newValue));	
			return;
		}
		
		if (eStructuralFeature instanceof EReference) {
			EReference eReferenceChanged = (EReference) eStructuralFeature;
			// If things blow up here, then something other than EObjects can be referenced...
			EObject oldValueEob = (EObject) oldValue;
			EObject newValueEob = (EObject) newValue;
			
			
			processTrace.append(String.format("\n - EReference changed: (%s, %s, %s)\n\t%s - %s \n\twas: %s\n\tnow: %s"
					, order, unique, multi,
					eReferenceChanged.getEReferenceType().getName(), eReferenceChanged.getName()
					, getEObjectLocation(oldValueEob), getEObjectLocation((newValueEob))));
			return;
		}
		
	}
	
	private String getEObjectLocation(EObject eobject) {
		if (null != eobject) {
			String frag = EcoreUtil.getURI(eobject).fragment();
			if (frag.equals("")) {
				frag = "'blank'";
			}
			return String.format("%s", frag);
		}
		return "null";
	}
	
	@SuppressWarnings("unchecked")
	private void reportReferences(EObject eObject, EReference reference, int pad) {
		String p = "\n";
		for (int i = 0; i < pad; i++) {
			p = p + "\t";
		}
		final String prefix = p;

		List<EObject> listOfreferences = (List<EObject>) eObject.eGet(reference);
		if (!listOfreferences.isEmpty()) {
			listOfreferences.forEach(r -> {
				processTrace.append(String.format("%s-> {", prefix));
				reportEObject(r, false, pad);
				processTrace.append(String.format("%s   }", prefix));

			});
		} else {
			processTrace.append(String.format("\n * Empty *"));
		}
		return;
	}

	private void reportEObjectIdentity(EObject eObject) {
		processTrace.append(  String.format("\n - EObject : %s - %s ", eObject.eClass().getName(),
				//EcoreUtil.getIdentification(eObject)));
				EcoreUtil.getURI(eObject)));
	}
	
	@SuppressWarnings("unchecked")
	private void reportEObject(EObject eObject, boolean followReference, int pad) {
		String p = "\n";
		for (int i = 0; i < pad; i++) {
			p = p + "\t";
		}
		final String prefix = p;

		reportEObjectRDFnode(eObject, pad);
		processTrace.append(String.format("%s - EObject : %s  %s ", prefix, eObject.eClass().getName(),
				EcoreUtil.getIdentification(eObject)));

		processTrace.append(String.format("%s   * All EAttributes: ", prefix));

		eObject.eClass().getEAllAttributes().forEach(e -> {
			processTrace.append(String.format("%s\t%s  %s  %s", prefix, e.getEAttributeType().getName(), e.getName(),
					eObject.eGet(e)));
		});

		processTrace.append(String.format("%s   * All EReferences: ", prefix));
		eObject.eClass().getEAllReferences().forEach(e -> {
			processTrace.append(String.format("%s\t%s  %s:", prefix, e.getEReferenceType().getName(), e.getName()));
			try {
				List<EObject> listOfreferences = (List<EObject>) eObject.eGet(e);
				if (!listOfreferences.isEmpty()) {
					if (followReference) {
						reportReferences(eObject, e, pad + 1);
					} else {
						listOfreferences.forEach(r -> {
							processTrace.append(String.format("%s\t-> {", prefix));
							processTrace.append(String.format("%s\t - %s ", prefix, EcoreUtil.getIdentification(r)));
							reportEObjectRDFnode(r, pad + 1);
							processTrace.append(String.format("%s\t   }", prefix));
						});
					}
				} else {
					processTrace.append(String.format("%s\t-> * Empty *", prefix));
				}
			} catch (Exception e2) {
				processTrace.append(String.format("%s\t Error with EReference (probably not an instance yet) : %s ",
						prefix, eObject.eGet(e)));
			}
		});
	}
	
	private void reportEObjectRDFnode(EObject eObject, int pad) {
		String p = "\n";
		for (int i = 0; i < pad; i++) {
			p = p + "\t";
		}
		final String prefix = p;

		if (eObject instanceof RDFGraphResourceImpl) {
			Resource rdfNode = identifyEObjectsRDFnode(eObject);
			processTrace.append(String.format("%s + RDF Node : %s ", prefix, rdfNode));
			processTrace.append(String.format("%s %s ", prefix, reportNamedModelsContaining(eObject)));
		} else {
			processTrace.append(String.format("%s + No RDFGraphResourceImpl for : %s ", prefix, eObject));
		}

	}
	
	private String reportNamedModelsContaining(EObject eObject) {
		StringBuilder namedModels = new StringBuilder();
		// List<Model> list = identifyNamedModelsContaining(eObject);

		if (eObject instanceof RDFGraphResourceImpl) {
			RDFGraphResourceImpl rdfGraphResource = (RDFGraphResourceImpl) eObject.eResource();
			List<Resource> list = rdfGraphResource.getResourcesForNamedModelsContaining(eObject);
			namedModels.append(String.format(" + On Named Model(s) : "));
			list.forEach(m -> namedModels.append(String.format(" [ %s ] ", m.getLocalName())));
			return namedModels.toString();
		}
		return "";

	}
}
