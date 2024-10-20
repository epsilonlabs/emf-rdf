/********************************************************************************
 * Copyright (c) 2024 University of York
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
