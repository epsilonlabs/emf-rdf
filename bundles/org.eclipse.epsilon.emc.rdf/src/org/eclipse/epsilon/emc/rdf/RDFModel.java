package org.eclipse.epsilon.emc.rdf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	/**
	 * One of the keys used to construct the first argument to
	 * {@link #load(StringProperties, String)}.
	 *
	 * This key should be set to a comma-separated list of prefix=uri pairs that
	 * should be added to the prefix->URI map of the loaded RDF resource. These
	 * pairs will take precedence over existing pairs in the resource.
	 */
	public static final String PROPERTY_PREFIXES = "prefixes";

	protected final Map<String, String> customPrefixesMap = new HashMap<>();
	protected String uri;
	protected Model model;

	public RDFModel() {
		this.propertyGetter = new RDFPropertyGetter(this);
	}

	@Override
	public Object getEnumerationValue(String enumeration, String label) throws EolEnumerationValueNotFoundException {
		throw new UnsupportedOperationException();
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
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean owns(Object instance) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInstantiable(String type) {
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

		/*
		 * This method assumes that the StringProperties replaces all previous
		 * configuration of this RDFModel. This is the same as in other popular
		 * EMC drivers (e.g. the EmfModel class).
		 */

		this.uri = properties.getProperty(PROPERTY_URI);

		this.customPrefixesMap.clear();
		String sPrefixes = properties.getProperty(PROPERTY_PREFIXES, "").strip();
		if (sPrefixes.length() > 0) {
			for (String sItem : sPrefixes.split(",")) {
				int idxEquals = sItem.indexOf('=');
				if (idxEquals == -1) {
					throw new IllegalArgumentException(String.format("Entry '%s' does not follow the prefix=uri format", sItem));
				}

				String sPrefix = sItem.substring(0, idxEquals);
				String sURI = sItem.substring(idxEquals + 1);
				customPrefixesMap.put(sPrefix, sURI);
			}
		}

		load();
	}

	@Override
	public boolean store(String location) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean store() {
		throw new UnsupportedOperationException();
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

		RDFQualifiedName qName = RDFQualifiedName.from(type, this::getNamespaceURI);
		while (itAvailableTypes.hasNext()) {
			RDFNode typeNode = itAvailableTypes.next();
			if (typeNode instanceof Resource) {
				Resource typeResource = (Resource) typeNode;
				if ((qName.namespaceURI == null || qName.namespaceURI.equals(typeResource.getNameSpace())) && qName.localName.equals(typeResource.getLocalName())) {
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

	public Map<String, String> getCustomPrefixesMap() {
		return this.customPrefixesMap;
	}

	/**
	 * <p>
	 * Returns the URI associated to a prefix.
	 * </p>
	 *
	 * <p>
	 * The prefix may be defined in the loaded RDF resource, or it may have been
	 * specified by modifying {@load #getCustomPrefixesMap()} or calling
	 * {@link #load(StringProperties)} with {@link #PROPERTY_PREFIXES} being set.
	 * </p>
	 */
	public String getNamespaceURI(String prefix) {
		String uri = customPrefixesMap.get(prefix);
		if (uri == null) {
			return model.getNsPrefixURI(prefix);
		}
		return uri;
	}
}
