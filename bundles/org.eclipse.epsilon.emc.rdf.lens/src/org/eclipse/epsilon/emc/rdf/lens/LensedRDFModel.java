package org.eclipse.epsilon.emc.rdf.lens;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.rdf.RDFModel;
import org.eclipse.epsilon.emc.rdf.RDFModelElement;
import org.eclipse.epsilon.emc.rdf.RDFResource;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolEnumerationValueNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolNotInstantiableModelElementTypeException;
import org.eclipse.epsilon.eol.models.CachedModel;
import org.eclipse.epsilon.eol.models.IModel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;
import org.eclipse.epsilon.eol.models.ModelRepository;

/**
 * Provides a lens over an RDF model, only showing the elements that match the types
 * in the referenced EPackages, and only exposing the properties that match the EClasses.
 */
public class LensedRDFModel extends CachedModel<LensedRDFResource> {

	protected class LensingEmfModel extends EmfModel {
		@Override
		public String getFullyQualifiedName(EClassifier eClassifier) {
			return super.getFullyQualifiedName(eClassifier);
		}
	}

	public static final String PROPERTY_RDF_MODEL_NAME = "lensed.rdf.model.name";

	private final ModelRepository modelRepository;
	private RDFModel rdfModel;
	private LensingEmfModel emfModel = new LensingEmfModel();

	public LensedRDFModel(ModelRepository modelRepo) {
		this.modelRepository = modelRepo;
	}

	@Override
	public Object getEnumerationValue(String enumeration, String label) throws EolEnumerationValueNotFoundException {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTypeNameOf(Object instance) {
		if (instance instanceof LensedRDFResource lensed) {
			return lensed.getEClass().getName();
		}
		throw new IllegalArgumentException("Cannot obtain type names of " + instance);
	}

	@Override
	public Object getElementById(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getElementId(Object instance) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setElementId(Object instance, String newId) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean owns(Object instance) {
		if (instance instanceof LensedRDFResource lensed) {
			return lensed.getModel() == this;
		}
		return false;
	}

	@Override
	public boolean isInstantiable(String type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasType(String type) {
		return emfModel.hasType(type);
	}

	@Override
	public boolean store(String location) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean store() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected Collection<LensedRDFResource> allContentsFromModel() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	protected Collection<LensedRDFResource> getAllOfTypeFromModel(String type)
			throws EolModelElementTypeNotFoundException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	protected Collection<LensedRDFResource> getAllOfKindFromModel(String kind) throws EolModelElementTypeNotFoundException {
		EClass eClass = emfModel.classForName(kind);
		String typeResourceUri = String.format("%s#%s", eClass.getEPackage().getNsURI(), eClass.getName());
		RDFResource typeResource = rdfModel.getElementById(typeResourceUri);

		List<RDFModelElement> instanceResources = rdfModel.getAllOfTypeFromModel(typeResource.getResource());
		List<LensedRDFResource> lensed = instanceResources.stream()
			.map(ir -> new LensedRDFResource(this, ir, eClass))
			.collect(Collectors.toList());

		return lensed;
	}

	@Override
	protected LensedRDFResource createInstanceInModel(String type)
			throws EolModelElementTypeNotFoundException, EolNotInstantiableModelElementTypeException {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException();
	}

	@Override
	public void load(StringProperties properties, IRelativePathResolver resolver) throws EolModelLoadingException {
		super.load(properties, resolver);

		String rdfModelName = properties.getProperty(PROPERTY_RDF_MODEL_NAME);
		if (rdfModelName == null) {
			throw new IllegalArgumentException(PROPERTY_RDF_MODEL_NAME + " is required but missing");
		}
		try {
			IModel referencedModel = modelRepository.getModelByName(rdfModelName);
			if (referencedModel instanceof RDFModel referencedRDF) {
				this.rdfModel = referencedRDF;
			} else {
				throw new EolModelLoadingException(
					new IllegalArgumentException(String.format("%s is not an RDF model", rdfModelName)), this);
			}
		} catch (EolModelNotFoundException e) {
			throw new EolModelLoadingException(e, this);
		}

		this.emfModel.load(properties, resolver);
	}

	@Override
	protected void loadModel() throws EolModelLoadingException {
		this.rdfModel.load();
		this.emfModel.load();
	}

	@Override
	protected void disposeModel() {
		this.rdfModel.dispose();
		this.emfModel.dispose();
	}

	@Override
	protected boolean deleteElementInModel(Object instance) throws EolRuntimeException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object getCacheKeyForType(String type) throws EolModelElementTypeNotFoundException {
		return type;
	}

	@Override
	protected Collection<String> getAllTypeNamesOf(Object instance) {
		if (instance instanceof LensedRDFResource lensed) {
			EClass metaClass = lensed.getEClass();
			Set<String> names = metaClass.getEAllSuperTypes()
				.stream().map(ec -> emfModel.getFullyQualifiedName(ec))
				.collect(Collectors.toSet());
			names.add(emfModel.getFullyQualifiedName(metaClass));
			return names;
		}
		throw new IllegalArgumentException("Cannot obtain type names from " + instance);
	}

}
