package org.eclipse.epsilon.emc.rdf;

import org.eclipse.epsilon.common.module.ModuleElement;
import org.eclipse.epsilon.eol.exceptions.EolIllegalPropertyException;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.execute.context.IEolContext;
import org.eclipse.epsilon.eol.execute.introspection.java.JavaPropertyGetter;

public class RDFPropertyGetter extends JavaPropertyGetter {

	protected final RDFModel model;

	public RDFPropertyGetter(RDFModel model) {
		this.model = model;
	}

	@Override
	public Object invoke(Object object, String property, IEolContext context) throws EolRuntimeException {
		// Try to use built-in methods first (e.g. for ".uri")
		if (super.hasProperty(object, property, context)) {
			return super.invoke(object, property, context);
		}

		// Otherwise, use RDF information
		if (object instanceof RDFResource) {
			RDFResource res = (RDFResource) object;
			return res.getProperty(property, context);
		}

		ModuleElement ast = context.getExecutorFactory().getActiveModuleElement();
		throw new EolIllegalPropertyException(object, property, ast, context);
	}

}
