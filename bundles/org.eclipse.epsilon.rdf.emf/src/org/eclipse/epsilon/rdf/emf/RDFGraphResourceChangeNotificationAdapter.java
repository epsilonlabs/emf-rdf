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
		reportTarget(this.target);
		
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
		reportTarget(this.target);

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
		
		
		if(featureClass.equals(EAttributeImpl.class)) {
			// This is likely a property of the RDF node for the "onEobject"
			EObject onEObject = (EObject)notification.getNotifier();	// RDF node
			EAttribute eAttributeChanged = (EAttribute) feature;		// RDF property
			// eAttribute's is value									// RDF object (node/literal)
			
			boolean isOrdered = eAttributeChanged.isOrdered();// If this is set then there is Order to the values.				
			int orderPosition = -1; // This is not notification.getPosition()
			
			if(null == notification.getOldValue()) {
				// New statement
			} else {
				// Existing statement
				updateAttributeInRDFGraphs(onEObject, eAttributeChanged, value, notification.getOldValue());
			}
		
			// getGraphResourceFor(onEObject).ttlConsoleOntModel();			
			
			// Console output
			processTrace.append(String.format("\n ON THIS "));
			reportEObjectRDFnode(onEObject, 0);
			processTrace.append(String.format("\n - eAttribute is Ordered? %s %s",
					isOrdered,
					orderPosition)); // -1 "none" else > 0 position but does not imply order is required 
			processTrace.append(String.format("\n - eAttribute value : %s  %s  %s ", 
					eAttributeChanged.getEAttributeType().getName(), 
					eAttributeChanged.getName(),
					value ));

			return;
		}

		if(featureClass.equals(EReferenceImpl.class)) {
			EObject onEObject = (EObject)notification.getNotifier();	// RDF node	
			EReferenceImpl eReference = (EReferenceImpl) feature;		// RDF property
			EObject referenced = (EObject) value;						// RDF object (node) 

			// TODO Deal with ordering
			boolean isOrdered = eReference.isOrdered();
			int orderPosition = -1 ; // This is not notification.getPosition()			
			
			// Console output
			processTrace.append(String.format("\n ON THIS "));
			reportEObject(onEObject, false, 0);					
			processTrace.append(String.format("\n - eReference is Ordered? %s %s", 
					isOrdered, 
					orderPosition)); // -1 "none" else > 0 position but does not imply order is required
			processTrace.append(String.format("\n - eReference changed : %s  %s \n\t->", 
					eReference.getEReferenceType().getName(),
					eReference.getName()));
			reportEObject(referenced, false, 1);
			
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
		
		processTrace.append(String.format("\n UNKNOWN identifyByFeature() "));
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
				reportEObject(valueEObject, false, 0);
				
				// EObject is likely an RDF node
				
				return;
			}
		}
	}
	
	private Resource identifyEObjectsRDFnode (EObject eObject) {	
		if (eObject.eResource().getClass().equals(RDFGraphResourceImpl.class)) {
			RDFGraphResourceImpl rdfGraphResource = (RDFGraphResourceImpl) eObject.eResource();
			Resource rdfNode = rdfGraphResource.getRDFResource(eObject);
			
			processTrace.append(reportNamedModelsContaining(eObject));
			return rdfNode;
		}
		processTrace.append(String.format(" ! No RDFGraphResourceImpl for : %s ", eObject));
		return null;
		
	}
	
	// TODO RDF updates should be a separate class that does the RDF work
	private void updateAttributeInRDFGraphs(EObject onEObject, EAttribute eAttribute, Object newValue, Object oldValue) {
		// A statement is formed as "subject–predicate–object"
		
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
		
		// TODO Make a list of Named Models that should be checked for the statements (not just an rdfNode?), and changed.
		List<Resource> namedModelURIs = graphResource.getResourcesForNamedModelsContaining(rdfNode);
		if(namedModelURIs.size() > 1) {
			System.err.print(String.format("RDF node %s has been found on %s named models : %s",
					rdfNode.getLocalName(), namedModelURIs.size(), namedModelURIs));
			// Return here if you want to bail out from changing all the graphs
		}	

		// TODO Go through the list of Named models to update and make the changes
		List<Model> namedModelsToUpdate = graphResource.getNamedModels(namedModelURIs);
		for (Model model : namedModelsToUpdate) {
			// Update Attributes expressed as a single RDF statement
			Statement newStatement = model.createLiteralStatement(rdfNode, property, newObject);
			Statement oldStatement = model.createLiteralStatement(rdfNode, property, oldObject);
			
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
	
	private  RDFGraphResourceImpl getGraphResourceFor( EObject eObject) {
		if (eObject.eResource().getClass().equals(RDFGraphResourceImpl.class)) {
			return (RDFGraphResourceImpl) eObject.eResource();
			}
		return null;
	}
	
	// TODO Remove this experiment code
	private void reportRDFnodeProperties (String label, Model model, Resource rdfNode ) {
		System.err.println("\n"+label+"\nModel hashCode : " + model.hashCode());
		System.err.println("Size: " + model.size() + " isEmpty? " + model.isEmpty());
		System.err.println("listProperties() on : " + rdfNode.getLocalName());
		rdfNode.listProperties().forEach(s-> System.err.println(s));
	}
	
	private void reportReferences (EObject eObject, EReference reference, int pad) {
		String p = "\n";
		for (int i = 0; i < pad; i++) {
			p = p + "\t";
		}
		final String prefix = p;
		
		EcoreEList<EObject> listOfreferences = (EcoreEList<EObject>) eObject.eGet(reference);
		if(!listOfreferences.isEmpty()) {
			listOfreferences.forEach(r -> {
				processTrace.append(String.format("%s-> {", prefix));
				reportEObject(r, false , pad);
				processTrace.append(String.format("%s   }", prefix));
				
				});
		} else {
			processTrace.append(String.format("\n * Empty *"));
		}
		return;
	}

	private void reportTarget(Object target) {
		Object targetClass = this.target.getClass();
		
		if( targetClass.equals(EObject.class) || targetClass.equals(DynamicEObjectImpl.class) ) {
			processTrace.append(String.format("\n - Target (EObject) : %s ", EcoreUtil.getIdentification(((EObject) target)) ));
			return;
		}
		
		processTrace.append(String.format("\n - Target : %s  %s ", target.getClass() , target ));	
	}

	private void reportEObject(EObject eObject, boolean followReference, int pad) {
		String p = "\n";
		for (int i = 0; i < pad; i++) {
			p = p + "\t";
		}
		final String prefix = p;
		
		reportEObjectRDFnode(eObject, pad);
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
						reportReferences(eObject, e, pad + 1);
					} else {
						listOfreferences.forEach( r ->{
							processTrace.append(String.format("%s\t-> {", prefix));
							processTrace.append(String.format("%s\t - %s ", prefix, EcoreUtil.getIdentification(r)));
							reportEObjectRDFnode(r, pad + 1 );
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

	private void reportEObjectRDFnode (EObject eObject, int pad) {
		final int subPad = pad + 1;
		
		String p = "\n";
		for (int i = 0; i < pad; i++) {
			p = p + "\t";
		}
		final String prefix = p;
		
		if (eObject.eResource().getClass().equals(RDFGraphResourceImpl.class)) {
			Resource rdfNode = identifyEObjectsRDFnode(eObject);
			processTrace.append(String.format("%s + RDF Node : %s ", prefix, rdfNode));
			processTrace.append(String.format("%s %s ", prefix, reportNamedModelsContaining(eObject)));						
		} else {
			processTrace.append(String.format("%s + No RDFGraphResourceImpl for : %s ",
					prefix,
					eObject));
		}
		
	}

	private String reportNamedModelsContaining (EObject eObject) {
		StringBuilder namedModels = new StringBuilder();
		//List<Model> list = identifyNamedModelsContaining(eObject);

		if (eObject.eResource().getClass().equals(RDFGraphResourceImpl.class)) { 
			RDFGraphResourceImpl rdfGraphResource = (RDFGraphResourceImpl) eObject.eResource();
			List<Resource>  list = rdfGraphResource.getResourcesForNamedModelsContaining(eObject);
			namedModels.append(String.format(" + On Named Model(s) : "));
			list.forEach(m -> namedModels.append(String.format(" [ %s ] ", m.getLocalName() )));
			return namedModels.toString();
		}
		return "";

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
	