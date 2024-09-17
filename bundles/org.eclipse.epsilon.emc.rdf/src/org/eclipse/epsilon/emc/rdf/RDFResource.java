package org.eclipse.epsilon.emc.rdf;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.eclipse.epsilon.eol.execute.context.IEolContext;

public class RDFResource extends RDFModelElement {

	private Resource resource;

	public RDFResource(Resource resource, RDFModel rdfModel) {
		super(rdfModel);
		this.resource = resource;
	}

	public Resource getResource() {
		return resource;
	}

	public List<Object> getProperty(String property, IEolContext context) {
		final List<Object> values = new ArrayList<>();

		for (StmtIterator it = resource.listProperties(); it.hasNext(); ) {
			Statement stmt = it.next();
			if (property.equals(stmt.getPredicate().getLocalName())) {
				values.add(convertToModelObject(stmt.getObject()));
			}
		}

		return values;
	}

	protected Object convertToModelObject(RDFNode node) {
		if (node instanceof Literal) {
			return new RDFLiteral((Literal) node, this.owningModel);
		}
		throw new IllegalArgumentException("Cannot convert " + node + " to a model object");
	}

	@Override
	public String toString() {
		return "RDFResource [resource=" + resource + "]";
	}

}
