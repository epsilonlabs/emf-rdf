package org.eclipse.epsilon.emc.rdf.lens.dt;

import org.eclipse.epsilon.emc.rdf.dt.EclipseRDFModel;
import org.eclipse.epsilon.emc.rdf.lens.LensedRDFModel;

public class EclipseLensedRDFModel extends LensedRDFModel {

	public EclipseLensedRDFModel() {
		super(new EclipseRDFModel());
	}

}
