package org.eclipse.epsilon.emc.rdf;

import org.apache.jena.rdf.model.Literal;

public class RDFLiteral extends RDFModelElement {

	private final Literal literal;

	public RDFLiteral(Literal node, RDFModel owningModel) {
		super(owningModel);
		this.literal = node;
	}

	public Object getValue() {
		return literal.getValue();
	}

	public String getLanguage() {
		return literal.getLanguage();
	}

	public String getDatatypeURI() {
		return literal.getDatatypeURI();
	}

	@Override
	public String toString() {
		return "RDFLiteral [literal=" + literal + "]";
	}

}
