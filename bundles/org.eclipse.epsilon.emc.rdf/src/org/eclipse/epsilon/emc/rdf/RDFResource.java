package org.eclipse.epsilon.emc.rdf;

import org.apache.jena.rdf.model.Resource;

public class RDFResource extends RDFModelElement {

	private Resource resource;

	public RDFResource(Resource resource, RDFModel rdfModel) {
		super(rdfModel);
		this.resource = resource;
	}

	public Resource getResource() {
		return resource;
	}

}
