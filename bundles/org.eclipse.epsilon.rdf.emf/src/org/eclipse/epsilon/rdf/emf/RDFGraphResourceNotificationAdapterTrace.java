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
import org.eclipse.emf.ecore.impl.EReferenceImpl;
import org.eclipse.emf.ecore.util.EContentAdapter;
import org.eclipse.emf.ecore.util.EcoreEList;
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
		
		switch (notification.getEventType()) {
		case Notification.ADD:
			processTrace.append("\n ADD ");
			//additiveChange(notification);
			break;			
		case Notification.ADD_MANY:
			processTrace.append("\n ADD_MANY ");
			//additiveChange(notification);
			break;
		case Notification.SET:
			processTrace.append("\n SET ");
			additiveChange(notification);
			break;
			
		case Notification.REMOVE:
			processTrace.append("\n REMOVE ");
			subtractiveChange (notification);
			break;			
		case Notification.REMOVE_MANY:
			processTrace.append("\n REMOVE_MANY ");
			subtractiveChange (notification);
			break;
		case Notification.UNSET:
			processTrace.append("\n UNSET ");
			subtractiveChange (notification);
			break;

		default:
			break;			
		}
		System.out.println(processTrace + "");
	}
	
	private void additiveChange (Notification notification) {
		reportTarget(this.target);
		
		Object feature = notification.getFeature();
		Object value = notification.getNewValue();
		
		if(null != feature) {
			// Work out the change based on feature
			Class<? extends Object> featureClass = feature.getClass();
			processTrace.append(String.format("\n - Feature class : %s", featureClass.getName()));

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
			
			processTrace.append(String.format("\n unhandled additive change : %s ", featureClass.getName()));
			return;
		} else {
			// Work out the change base on newValue
			processTrace.append(String.format("\n - Not a Feature"));
			identifyByValue(value, notification);
		}		
	}
	
	private void addativeFeatureEObject(Object feature, Object value, Notification notification) {
		processTrace.append(String.format("\n - EObject %s", value.hashCode()));
		
		EObject eObject = (EObject) feature;
		processTrace.append(String.format("\n - eObject changed : %s  %s ", 
				eObject.eClass().getName(),
				EcoreUtil.getIdentification(eObject) ));
	}

	private void additiveFeatureEReference(Object feature, Object value, Notification notification) {
		processTrace.append(String.format("\n - EReference"));
		
		EObject onEObject = (EObject)notification.getNotifier();	// RDF node	
		EReferenceImpl eReference = (EReferenceImpl) feature;		// RDF property
		
		RDFGraphResourceImpl graphResource = RDFGraphResourceUpdate.getGraphResourceFor(onEObject);
		if(null == graphResource) {
			processTrace.append(String.format("Still loading, no graphResource"));
			return;
		}
		
		// Single
		if (value instanceof EObject) {
			processTrace.append(String.format(" - single EObject"));
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
			return;
		}
		
		// Array
		if (value instanceof ArrayList) {
			processTrace.append(String.format(" - ArrayList"));	
			ArrayList<EObject> referenced = (ArrayList) value;						// RDF object (node)
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
			referenced.forEach(r -> reportEObject(r, false, 1) );
			return;
		}
		return;
	}

	private void additiveFeatureEAttribute(Object feature, Object value, Notification notification) {
		processTrace.append(String.format("\n - EAttribute"));
		EObject onEObject = (EObject)notification.getNotifier();	// RDF node
		EAttribute eAttributeChanged = (EAttribute) feature;		// RDF property
		// eAttribute's is value									// RDF object (node/literal)
		
		boolean isOrdered = eAttributeChanged.isOrdered();// If this is set then there is Order to the values.				
		int orderPosition = -1; // This is not notification.getPosition()
		
		// TODO Make a list of Named Models that should be checked for the statements (not just an rdfNode?), and update them
		RDFGraphResourceImpl graphResource = RDFGraphResourceUpdate.getGraphResourceFor(onEObject);				
		List<Resource> namedModelURIs = graphResource.getResourcesForNamedModelsContaining(onEObject);
		
		if(null == notification.getOldValue()) {
			// Create new statement
			if (null == notification.getNewValue()) {
				// Create statement null
				processTrace.append(String.format(" - new statement set to null", value));
			} else {
				// Create statement value
				processTrace.append(String.format(" - new statement set to value %s", value));
			}
				
		} else {
			// Update existing statement
			 if(null == notification.getNewValue()) {
				 // Update existing statement to null
				 processTrace.append(String.format(" - existing statement set to null"));
			 } else {
				 // Update existing statement value
				 processTrace.append(String.format(" - existing statement set to value %s", value));
			 }
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
			if (notifier instanceof RDFGraphResourceImpl) {
				RDFGraphResourceImpl notifierResource = (RDFGraphResourceImpl) notifier;				
				processTrace.append(String.format("\n  - Notifier is RDFGraphResourceImpl : %s ", 
						notifierResource.getURI()));
				return;
			} else {
				processTrace.append(String.format("\n  - Notifier : \n    * %s \n    * %s", 
						notification.getNotifier().getClass(),
						notification.getNotifier() ));
			}
							
			if ( (value instanceof EObject)
					|| (value instanceof DynamicEObjectImpl) ){
				processTrace.append(String.format("\n Handle new value as a type of EObject"));
				EObject valueEObject = (EObject)value;
				reportEObject(valueEObject, false, 0);
				
				// EObject is likely an RDF node
				
				return;
			}
		}
	}
	
	private Resource identifyEObjectsRDFnode (EObject eObject) {	
		if (eObject instanceof RDFGraphResourceImpl) {
			RDFGraphResourceImpl rdfGraphResource = (RDFGraphResourceImpl) eObject.eResource();
			Resource rdfNode = rdfGraphResource.getRDFResource(eObject);
			
			processTrace.append(reportNamedModelsContaining(eObject));
			return rdfNode;
		}
		processTrace.append(String.format(" ! No RDFGraphResourceImpl for : %s ", eObject));
		return null;
		
	}

	private void reportEObjectRDFnode (EObject eObject, int pad) {
		final int subPad = pad + 1;
		
		String p = "\n";
		for (int i = 0; i < pad; i++) {
			p = p + "\t";
		}
		final String prefix = p;
		
		//if (eObject.eResource().getClass().equals(RDFGraphResourceImpl.class)) {
		if (eObject instanceof RDFGraphResourceImpl) {
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

		if (eObject instanceof RDFGraphResourceImpl) { 
			RDFGraphResourceImpl rdfGraphResource = (RDFGraphResourceImpl) eObject.eResource();
			List<Resource>  list = rdfGraphResource.getResourcesForNamedModelsContaining(eObject);
			namedModels.append(String.format(" + On Named Model(s) : "));
			list.forEach(m -> namedModels.append(String.format(" [ %s ] ", m.getLocalName() )));
			return namedModels.toString();
		}
		return "";

	}
}
