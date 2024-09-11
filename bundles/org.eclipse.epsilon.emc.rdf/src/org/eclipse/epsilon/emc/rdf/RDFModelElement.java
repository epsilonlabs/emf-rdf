package org.eclipse.epsilon.emc.rdf;

import org.apache.jena.rdf.model.Statement;

public class RDFModelElement {

	private Statement statement;

	public RDFModelElement(Statement stmt) {
		this.statement = stmt;
	}

	public Statement getStatement() {
		return statement;
	}

}
