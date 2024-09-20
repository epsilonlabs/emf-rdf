package org.eclipse.epsilon.emc.rdf;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
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
		final RDFQualifiedName pName = RDFQualifiedName.fromString(property);

		// Filter statements by prefix and local name
		ExtendedIterator<Statement> itStatements = null;
		if (pName.prefix == null) {
			itStatements = resource.listProperties()
				.filterKeep(stmt -> pName.localName.equals(stmt.getPredicate().getLocalName()));
		} else {
			String prefixIri = resource.getModel().getNsPrefixMap().get(pName.prefix);
			Property prop = new PropertyImpl(prefixIri, pName.localName);
			itStatements = resource.listProperties(prop);
		}

		// If a language tag is used, only keep literals with that tag
		if (pName.languageTag != null) {
			itStatements = itStatements.filterKeep(stmt -> {
				if (stmt.getObject() instanceof Literal) {
					Literal l = (Literal) stmt.getObject();
					return pName.languageTag.equals(l.getLanguage());
				}
				return false;
			});
		}

		final List<Object> values = new ArrayList<>();
		while (itStatements.hasNext()) {
			Statement stmt = itStatements.next();
			values.add(convertToModelObject(stmt.getObject()));
		}
		return values;
	}

	public String getUri() {
		return resource.getURI();
	}

	protected Object convertToModelObject(RDFNode node) {
		if (node instanceof Literal) {
			return new RDFLiteral((Literal) node, this.owningModel);
		} else if (node instanceof Resource) {
			return new RDFResource((Resource) node, this.owningModel);
		}
		throw new IllegalArgumentException("Cannot convert " + node + " to a model object");
	}

	@Override
	public String toString() {
		return "RDFResource [resource=" + resource + "]";
	}

}
