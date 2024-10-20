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

public abstract class RDFModelElement {

	protected final RDFModel owningModel;

	public RDFModelElement(RDFModel rdfModel) {
		this.owningModel = rdfModel;
	}

	public RDFModel getModel() {
		return owningModel;
	}

}