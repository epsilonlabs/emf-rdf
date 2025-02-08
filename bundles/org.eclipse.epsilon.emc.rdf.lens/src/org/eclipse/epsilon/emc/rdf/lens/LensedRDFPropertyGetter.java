package org.eclipse.epsilon.emc.rdf.lens;

import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.introspection.java.JavaPropertyGetter;

public class LensedRDFPropertyGetter extends JavaPropertyGetter {

	@Override
	public Object invoke(Object object, String property, IEolContext context) throws EolRuntimeException {
		if (object instanceof LensedRDFResource lensed) {
			EStructuralFeature sf = lensed.getEClass().getEStructuralFeature(property);
			if (sf != null) {
				return lensed.eGet(sf);
			}
		}

		return super.invoke(object, property, context);
	}

}
