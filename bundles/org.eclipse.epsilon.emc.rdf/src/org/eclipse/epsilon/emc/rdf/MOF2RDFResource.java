package org.eclipse.epsilon.emc.rdf;

import org.apache.jena.rdf.model.Resource;
import org.eclipse.epsilon.eol.execute.context.IEolContext;

public class MOF2RDFResource extends RDFResource {

	public MOF2RDFResource(Resource aResource, RDFModel rdfModel) {
		super(aResource, rdfModel);
	}
	
	//TODO MOF2RDFResource.getProperty()
	public Object getProperty(String property, IEolContext context) {
		return super.getProperty(property, context);
	}

}
