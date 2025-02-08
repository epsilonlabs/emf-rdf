package org.eclipse.epsilon.emc.rdf.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.epsilon.emc.rdf.RDFQualifiedName;
import org.eclipse.epsilon.emc.rdf.RDFResource;
import org.eclipse.epsilon.emc.rdf.RDFResource.LiteralMode;

public class LensedRDFResource {

	private final LensedRDFModel model;
	private final EClass eClass;
	private final RDFResource resource;

	public LensedRDFResource(LensedRDFModel model, RDFResource res, EClass mc) {
		this.model = model;
		this.eClass = mc;
		this.resource = res;
	}

	public EClass getEClass() {
		return eClass;
	}

	public RDFResource getResource() {
		return resource;
	}

	public LensedRDFModel getModel() {
		return model;
	}

	@Override
	public String toString() {
		return String.format("%s [%s]", eClass.getName(), formatProperties());
	}

	protected Object formatProperties() {
		List<String> props = new ArrayList<>();
		for (EStructuralFeature sf : eClass.getEAllStructuralFeatures()) {
			Object realValue = eGet(sf);
			if (realValue != null) {
				props.add(String.format("%s=%s", sf.getName(), realValue));
			}
		}
		return String.join(", ", props);
	}

	public Object eGet(EStructuralFeature sf) {
		Collection<Object> rawValue = this.resource.listPropertyValues(
			RDFQualifiedName.from(null, sf.getEContainingClass().getEPackage().getNsURI() + "#", sf.getName()),
			null, LiteralMode.VALUES_ONLY);

		if (sf.isMany()) {
			return rawValue;
		} else if (rawValue.isEmpty()) {
			return null;
		} else {
			return rawValue.iterator().next();
		}
	}

}
