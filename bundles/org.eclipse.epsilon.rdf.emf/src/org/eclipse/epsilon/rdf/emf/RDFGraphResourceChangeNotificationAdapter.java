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
import org.eclipse.emf.ecore.impl.DynamicEObjectImpl;
import org.eclipse.emf.ecore.impl.EAttributeImpl;
import org.eclipse.emf.ecore.impl.EReferenceImpl;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.util.EcoreEList;
import org.eclipse.emf.ecore.util.EcoreUtil;

public class RDFGraphResourceChangeNotificationAdapter extends EContentAdapter {

	StringBuilder processTrace;

	@Override
	public void notifyChanged(Notification notification) {
		super.notifyChanged(notification);
		
		// Decode the notification
		processTrace = new StringBuilder();
		processTrace.append(String.format("\n[ Notification ]"));
		
		switch (notification.getEventType()) {
		case Notification.ADD:
			processTrace.append("\n ADD ");
			incrementalChange(notification);
			break;			
		case Notification.ADD_MANY:
			processTrace.append("\n ADD_MANY ");
			incrementalChange(notification);
			break;
		case Notification.SET:
			processTrace.append("\n SET ");
			incrementalChange(notification);
			break;
			
		case Notification.REMOVE:
			processTrace.append("\n REMOVE ");
			decrementalChange (notification);
			break;			
		case Notification.REMOVE_MANY:
			processTrace.append("\n REMOVE_MANY ");
			decrementalChange (notification);
			break;
		case Notification.UNSET:
			processTrace.append("\n UNSET ");
			decrementalChange (notification);
			break;

		default:
			break;			
		}
		System.out.println(processTrace + "");
	}
	
	private void incrementalChange (Notification notification) {
		identifyTarget(this.target);
		
		Object feature = notification.getFeature();
		Object value = notification.getNewValue();
		
		if(null != feature) {
			// Work out the change based on feature
			identifyByFeature(feature, value, notification);
		} else {
			// Work out the change base on newValue
			processTrace.append(String.format("\n - Not a Feature"));
			identifyByValue(value, notification);
		}		
	}
	
	private void decrementalChange(Notification notification) {
		identifyTarget(this.target);

		Object feature = notification.getFeature();
		Object value = notification.getOldValue();

		if (null != feature) {
			// Work out what was removed by Feature
			identifyByFeature(feature, value, notification);

		} else {
			// Work out what was removed by old Value?
			processTrace.append(String.format("\n No Feature?"));
			return;
		}
	}

	private void identifyByFeature(Object feature, Object value, Notification notification) {

		Class<? extends Object> featureClass = feature.getClass();
		processTrace.append(String.format("\n - Feature class : %s", featureClass.getName()));
		processTrace.append(String.format("\n ON THIS "));
		
		if(featureClass.equals(EAttributeImpl.class)) {
			EObject onEObject = (EObject)notification.getNotifier();	// RDF node
			EAttribute eAttributeChanged = (EAttribute) feature;		// RDF property
			//value														// RDF object (node/literal)
			
			boolean isOrdered = eAttributeChanged.isOrdered();// If this is set then there is possibly Order for the values.				
			int orderPosition = -1; // This is not notification.getPosition()
			
			identifyEObject(onEObject, false, 0);
			processTrace.append(String.format("\n - eAttribute is Ordered? %s %s",
					isOrdered,
					orderPosition)); // -1 "none" else > 0 position but does not imply order is required 
			processTrace.append(String.format("\n - eAttribute value : %s  %s  %s ", 
					eAttributeChanged.getEAttributeType().getName(), 
					eAttributeChanged.getName(),
					value ));

			//processTrace.append(String.format("\n - eAttribute was : %s  %s  %s ", 
			//		eAttributeChanged.getEAttributeType().getName(), eAttributeChanged.getName(), notification.getOldValue() ));
			

			// This is likely a property of the RDF node for onEobject
			
			return;
		}

		if(featureClass.equals(EReferenceImpl.class)) {
			EObject onEObject = (EObject)notification.getNotifier();	// RDF node	
			EReferenceImpl eReference = (EReferenceImpl) feature;		// RDF property
			EObject referenced = (EObject) value;						// RDF object (node) 

			boolean isOrdered = eReference.isOrdered();
			int orderPosition = -1 ; // This is not notification.getPosition()			

			identifyEObject(onEObject, false, 0);
			processTrace.append(String.format("\n - eReference is Ordered? %s %s", 
					isOrdered, 
					orderPosition)); // -1 "none" else > 0 position but does not imply order is required
			processTrace.append(String.format("\n - eReference changed : %s  %s \n\t->", 
					eReference.getEReferenceType().getName(),
					eReference.getName()));
			identifyEObject(referenced, false, 1);
			
			// This is likely a property of the RDF node for onEobject
			
			return;
		}
		
		if( (featureClass.equals(EObject.class))
				|| (featureClass.equals(DynamicEObjectImpl.class)) ) {
			EObject eObject = (EObject) feature;
			processTrace.append(String.format("\n - eObject changed : %s  %s ", 
					eObject.eClass().getName(),
					EcoreUtil.getIdentification(eObject) ));			
			return;
		}
		
		processTrace.append(String.format("\n UNKNOWN identifyIncrementByFeature() "));
		return;

	}

 	private void identifyByValue(Object value, Notification notification) {
		if (null == value) {
			processTrace.append(String.format("\n No new value to process? : %s ", notification));
			return;
		} else {
			Object targetClass = this.target.getClass();
			Object notifier = notification.getNotifier();
			
			processTrace.append(String.format("\n - Notification : %s ", notification.getClass()));
			processTrace.append(String.format("\n Attempt to process the Value :  %s  %s",
					value.getClass(),
					value));
			
			// What kind of notifier is it?
			if (notifier.getClass().equals(RDFGraphResourceImpl.class)) {
				RDFGraphResourceImpl notifierResource = (RDFGraphResourceImpl) notifier;				
				processTrace.append(String.format("\n  - Notifier is RDFGraphResourceImpl : %s ", 
						notifierResource.getURI()));
				return;
			} else {
				processTrace.append(String.format("\n  - Notifier : \n    * %s \n    * %s", 
						notification.getNotifier().getClass(),
						notification.getNotifier() ));

			}
							
			if ( value.getClass().equals(EObject.class) 
					|| value.getClass().equals(DynamicEObjectImpl.class)) {
				processTrace.append(String.format("\n Handle new value as a type of EObject"));
				EObject valueEObject = (EObject)value;
				identifyEObject(valueEObject, false, 0);
				
				// EObject is likely an RDF node
				
				return;
			}
		}
	}
	
	private Resource identifyEObjectRDFnode (EObject eObject, int pad) {
		final int subPad = pad + 1;
		
		String p = "\n";
		for (int i = 0; i < pad; i++) {
			p = p + "\t";
		}
		final String prefix = p;
		
		if (eObject.eResource().getClass().equals(RDFGraphResourceImpl.class)) {
			RDFGraphResourceImpl rdfGraphResource = (RDFGraphResourceImpl) eObject.eResource();
			Resource rdfNode = rdfGraphResource.getRDFResource(eObject);
			processTrace.append(String.format("%s + RDF Node : %s ",
					prefix,
					rdfNode));
			rdfGraphResource.findNamedModelsContaining(rdfNode);
			getNamedModelsContaining(eObject);
			return rdfNode;
		} else {
			processTrace.append(String.format("%s + No RDFGraphResourceImpl for : %s ",
					prefix,
					eObject));
		}
		return null;
	}
	
	private List<Resource> getNamedModelsContaining(EObject eObject) {
		
		if (eObject.eResource().getClass().equals(RDFGraphResourceImpl.class)) { 
			RDFGraphResourceImpl rdfGraphResource = (RDFGraphResourceImpl) eObject.eResource();
			List<Resource> list = rdfGraphResource.findNamedModelsContaining(eObject);
			processTrace.append(String.format(" + On Named Model(s) : "));
			list.forEach(n -> processTrace.append(String.format(" [ %s ] ", n.getLocalName() )));
		}
		return null;
	}
	
	private void identifyReferences (EObject eObject, EReference reference, int pad) {
		String p = "\n";
		for (int i = 0; i < pad; i++) {
			p = p + "\t";
		}
		final String prefix = p;
		
		EcoreEList<EObject> listOfreferences = (EcoreEList<EObject>) eObject.eGet(reference);
		if(!listOfreferences.isEmpty()) {
			listOfreferences.forEach(r -> {
				processTrace.append(String.format("%s-> {", prefix));
				identifyEObject(r, false , pad);
				processTrace.append(String.format("%s   }", prefix));
				
				});
		} else {
			processTrace.append(String.format("\n * Empty *"));
		}
		return;
	}
	
	private void identifyTarget(Object target) {
		Object targetClass = this.target.getClass();
		
		if( targetClass.equals(EObject.class) || targetClass.equals(DynamicEObjectImpl.class) ) {
			processTrace.append(String.format("\n - Target (EObject) : %s ", EcoreUtil.getIdentification(((EObject) target)) ));
			return;
		}
		
		processTrace.append(String.format("\n - Target : %s  %s ", target.getClass() , target ));	
	}
	
	private void identifyEObject(EObject eObject, boolean followReference, int pad) {
		String p = "\n";
		for (int i = 0; i < pad; i++) {
			p = p + "\t";
		}
		final String prefix = p;
		
		identifyEObjectRDFnode(eObject, pad);
		processTrace.append(String.format("%s - EObject : %s  %s ", prefix, 
				eObject.eClass().getName(), EcoreUtil.getIdentification(eObject)));
		
		processTrace.append(String.format("%s   * All EAttributes: ", prefix));
		
		eObject.eClass().getEAllAttributes().forEach(e -> {
			processTrace.append(String.format("%s\t%s  %s  %s", prefix, 
					e.getEAttributeType().getName(), e.getName(), eObject.eGet(e)));
		});
		
		processTrace.append(String.format("%s   * All EReferences: ", prefix));
		eObject.eClass().getEAllReferences().forEach(e -> {
			processTrace.append(String.format("%s\t%s  %s:", prefix, e.getEReferenceType().getName(), e.getName()));
			try {
				EcoreEList<EObject> listOfreferences = (EcoreEList<EObject>) eObject.eGet(e);
				if(!listOfreferences.isEmpty()) {
					if (followReference) {
						identifyReferences(eObject, e, pad + 1);
					} else {
						listOfreferences.forEach( r ->{
							processTrace.append(String.format("%s\t-> {", prefix));
							processTrace.append(String.format("%s\t - %s ", prefix, EcoreUtil.getIdentification(r)));
							identifyEObjectRDFnode(r, pad + 1 );
							processTrace.append(String.format("%s\t   }", prefix));
						});
					}
				} else {
					processTrace.append(String.format("%s\t-> * Empty *", prefix));
				}
			} catch (Exception e2) {
				processTrace.append(String.format("%s\t Error with EReference (probably not an instance yet) : %s ",prefix , eObject.eGet(e)));
			} 
		});
	}

	/*
	 * @Override public Notifier getTarget() { // TODO Auto-generated method stub
	 * System.out.println("getTarget()"); return null; }
	 * 
	 * @Override public void setTarget(Notifier newTarget) { // TODO Auto-generated
	 * method stub System.out.println("setTarget() " + newTarget); }
	 * 
	 * @Override public boolean isAdapterForType(Object type) { // TODO
	 * Auto-generated method stub //System.out.println("isAdapterForType() " +
	 * type); return false; }
	 */
}


/*
private void identifyIncrementByFeature(Object feature, Object newValue, Notification notification) {

	Class<? extends Object> featureClass = feature.getClass();
	processTrace.append(String.format("\n - Feature class : %s", featureClass.getName()));
	processTrace.append(String.format("\n ON THIS "));
	
	if(featureClass.equals(EAttributeImpl.class)) {	
		EObject onEObject = (EObject)notification.getNotifier();	// RDF node
		EAttribute eAttributeChanged = (EAttribute) feature;		// RDF property
		
		boolean isOrdered = eAttributeChanged.isOrdered();			
		
		identifyEObject(onEObject, false, 0);
		processTrace.append(String.format("\n - eAttribute is Ordered? %s", isOrdered)); 
		processTrace.append(String.format("\n - eAttribute was : %s  %s  %s ", 
				eAttributeChanged.getEAttributeType().getName(), eAttributeChanged.getName(), notification.getOldValue() ));
		processTrace.append(String.format("\n - eAttribute changed : %s  %s  %s ", 
				eAttributeChanged.getEAttributeType().getName(), eAttributeChanged.getName(), newValue ));

		// This is likely a property of the RDF node for onEobject
		
		return;
	}

	if(featureClass.equals(EReferenceImpl.class)) {
		EObject onEObject = (EObject)notification.getNotifier();	// RDF node	
		EReferenceImpl eReference = (EReferenceImpl) feature;		// RDF property
		EObject value = (EObject) newValue;							// RDF node (object) 
				
		identifyEObject(onEObject, false, 0);
		processTrace.append(String.format("\n - eReference changed : %s  %s \n\t->", 
				eReference.getEReferenceType().getName(), eReference.getName() ));
		identifyEObject(value, false, 1);
		
		// This is likely a property of the RDF node for onEobject
		
		return;
	}
	
	if(featureClass.equals(EObject.class) || featureClass.equals(DynamicEObjectImpl.class)) {
		EObject eObject = (EObject) feature;
		processTrace.append(String.format("\n - eObject changed : %s  %s ", 
				eObject.eClass().getName(), EcoreUtil.getIdentification(eObject) ));			
		return;
	}
	
	processTrace.append(String.format("\n UNKNOWN identifyIncrementByFeature() "));
	return;

}
*/
	
	/*
	private void identifyIncrementByValue(Object newValue, Notification notification) {
	if (null == newValue) {
		processTrace.append(String.format("\n No new value to process? : %s ", notification));
		return;
	} else {
		Object targetClass = this.target.getClass();
		Object notifier = notification.getNotifier();
		
		processTrace.append(String.format("\n - Notification : %s ", notification.getClass()));
		processTrace.append(String.format("\n Attempt to process the NewValue :  %s  %s", newValue.getClass(), newValue));			
		
		
		if (notifier.getClass().equals(RDFGraphResourceImpl.class)) {
			RDFGraphResourceImpl notifierResource = (RDFGraphResourceImpl) notifier;				
			processTrace.append(String.format("\n  - Notifier is RDFGraphResourceImpl : %s ", notifierResource.getURI()));
		} else {
			processTrace.append(String.format("\n  - Notifier : \n    * %s \n    * %s", notification.getNotifier().getClass(), notification.getNotifier() ));
		}
						
		if ( newValue.getClass().equals(EObject.class) || newValue.getClass().equals(DynamicEObjectImpl.class)) {
			processTrace.append(String.format("\n Handle new value as a type of EObject"));
			EObject newValueEObject = (EObject)newValue;
			identifyEObject(newValueEObject, false, 0);
			
			// EObject is likely an RDF node
			
			return;
		}
		
	}
}
*/
	