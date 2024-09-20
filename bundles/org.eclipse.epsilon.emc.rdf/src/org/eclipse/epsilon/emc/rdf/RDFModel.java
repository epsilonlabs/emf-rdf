package org.eclipse.epsilon.emc.rdf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.epsilon.common.util.StringProperties;
import org.eclipse.epsilon.eol.exceptions.EolRuntimeException;
import org.eclipse.epsilon.eol.exceptions.models.EolEnumerationValueNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelElementTypeNotFoundException;
import org.eclipse.epsilon.eol.exceptions.models.EolModelLoadingException;
import org.eclipse.epsilon.eol.exceptions.models.EolNotInstantiableModelElementTypeException;
import org.eclipse.epsilon.eol.models.CachedModel;
import org.eclipse.epsilon.eol.models.IRelativePathResolver;

public class RDFModel extends CachedModel<RDFModelElement> {

	public static final String PROPERTY_URI = "file";

	protected String uri;

	private Model model;

	public RDFModel() {
		this.propertyGetter = new RDFPropertyGetter(this);
	}

	@Override
	public Object getEnumerationValue(String enumeration, String label) throws EolEnumerationValueNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getTypeNameOf(Object instance) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getElementById(String iri) {
		Resource res = model.getResource(iri);
		if (res != null) {
			return new RDFResource(res, this);
		}
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
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInstantiable(String type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasType(String type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void load(StringProperties properties, IRelativePathResolver resolver) throws EolModelLoadingException {
		super.load(properties, resolver);

		this.uri = properties.getProperty(PROPERTY_URI);

		load();
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
	protected Collection<RDFModelElement> allContentsFromModel() {
		final List<RDFModelElement> elems = new ArrayList<>();

		for (ResIterator it = model.listSubjects(); it.hasNext(); ) {
			Resource stmt = it.next();
			elems.add(new RDFResource(stmt, this));	
		}
		
		return elems;
	}

	@Override
	protected Collection<RDFModelElement> getAllOfTypeFromModel(String type)
			throws EolModelElementTypeNotFoundException {
		Resource typeR = getTypeResourceByName(type);

		ResIterator itInstances = model.listResourcesWithProperty(RDF.type, typeR);
		List<RDFModelElement> instances = new ArrayList<>();
		while (itInstances.hasNext()) {
			instances.add(new RDFResource(itInstances.next(), this));
		}

		return instances;
	}

	protected Resource getTypeResourceByName(String type) throws EolModelElementTypeNotFoundException {
		NodeIterator itAvailableTypes = model.listObjectsOfProperty(RDF.type);

		RDFQualifiedName qName = RDFQualifiedName.fromString(type);
		String nsURI = qName.prefix == null ? null : model.getNsPrefixURI(qName.prefix);
		while (itAvailableTypes.hasNext()) {
			RDFNode typeNode = itAvailableTypes.next();
			if (typeNode instanceof Resource) {
				Resource typeResource = (Resource) typeNode;
				if ((nsURI == null || nsURI.equals(typeResource.getNameSpace())) && qName.localName.equals(typeResource.getLocalName())) {
					return typeResource;
				}
			}
		}

		throw new EolModelElementTypeNotFoundException(this.getName(), type);
	}

	@Override
	protected Collection<RDFModelElement> getAllOfKindFromModel(String kind)
			throws EolModelElementTypeNotFoundException {
		// TODO investigate type hierarchies in RDF-related technologies
		return getAllOfTypeFromModel(kind);
	}

	@Override
	protected RDFResource createInstanceInModel(String type) throws EolModelElementTypeNotFoundException, EolNotInstantiableModelElementTypeException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void loadModel() throws EolModelLoadingException {
		if (!this.readOnLoad) {
			return;
		}

		try {
			if (uri == null) {
				throw new IllegalStateException("No file path has been set");
			}
			model = RDFDataMgr.loadModel(uri);			
		} catch (Exception ex) {
			throw new EolModelLoadingException(ex, this);
		}
	}

	@Override
	protected void disposeModel() {
		model = null;
	}

	@Override
	protected boolean deleteElementInModel(Object instance) throws EolRuntimeException {
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object getCacheKeyForType(String type) throws EolModelElementTypeNotFoundException {
		return getTypeResourceByName(type);
	}

	@Override
	protected Collection<String> getAllTypeNamesOf(Object instance) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

}
