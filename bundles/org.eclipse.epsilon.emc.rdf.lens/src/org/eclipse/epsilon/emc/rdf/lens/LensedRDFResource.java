package org.eclipse.epsilon.emc.rdf.lens;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.epsilon.emc.rdf.RDFModelElement;

public class LensedRDFResource {

	private final LensedRDFModel model;
	private final EClass eClass;
	private final RDFModelElement resource;

	public LensedRDFResource(LensedRDFModel model, RDFModelElement res, EClass mc) {
		this.model = model;
		this.eClass = mc;
		this.resource = res;
	}

	public EClass getEClass() {
		return eClass;
	}

	public RDFModelElement getResource() {
		return resource;
	}

	public LensedRDFModel getModel() {
		return model;
	}
}
