package org.eclipse.epsilon.emc.rdf;

public abstract class RDFModelElement {

	protected final RDFModel owningModel;

	public RDFModelElement(RDFModel rdfModel) {
		this.owningModel = rdfModel;
	}

	public RDFModel getModel() {
		return owningModel;
	}

}