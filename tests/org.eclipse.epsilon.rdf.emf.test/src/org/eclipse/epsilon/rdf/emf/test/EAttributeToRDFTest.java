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
 package org.eclipse.epsilon.rdf.emf.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.emfatic.core.EmfaticResourceFactory;
import org.eclipse.epsilon.rdf.emf.RDFGraphResourceFactory;
import org.junit.BeforeClass;
import org.junit.Test;

public class EAttributeToRDFTest {
	
	@BeforeClass
	public static void setupDrivers() {
		Resource.Factory.Registry.INSTANCE
		.getExtensionToFactoryMap()
		.put("xmi", new XMIResourceFactoryImpl());
	Resource.Factory.Registry.INSTANCE
		.getExtensionToFactoryMap()
		.put("rdfres", new RDFGraphResourceFactory());
	Resource.Factory.Registry.INSTANCE
		.getExtensionToFactoryMap()
		.put("emf", new EmfaticResourceFactory());
	}
	
	private final File RDFGraphModel = new File("resources/EAttribute_test/model.rdfres");
	private final File EMFNativeModel = new File("resources/EAttribute_test/model.xmi");
	private final File MetaModel = new File("resources/EAttribute_test/metamodel.emf");
	
	@Test
	public void test1 () throws IOException {
		ResourceSet metaModel = getMetaModelResourceSet(MetaModel);
		ResourceSet rdf = getGraphResourceSet(RDFGraphModel, metaModel);
		ResourceSet xmi = getXMIResourceSet(EMFNativeModel, metaModel);
		
		equivalentModels("Test 1", rdf , xmi);
	}
	
	protected void registerEPackages(ResourceSet rsMetamodels, ResourceSet rsTarget) {
		for (Resource rMetamodel : rsMetamodels.getResources()) {
			for (EObject eob : rMetamodel.getContents()) {
				if (eob instanceof EPackage epkg) {
					rsTarget.getPackageRegistry().put(epkg.getNsURI(), epkg);
				}
			}
		}
	}

	protected void loadFileIntoResourceSet(File file, ResourceSet rs) throws IOException {
		Resource resource = rs.createResource(URI.createFileURI(file.getAbsolutePath()));		
		resource.load(null);
		rs.getResources().add(resource);
	}
	
	
	protected ResourceSet getMetaModelResourceSet (File file) throws IOException {
		ResourceSet resourceSet = new ResourceSetImpl();
		loadFileIntoResourceSet(file, resourceSet);
		return resourceSet;
	}
	
	
	protected ResourceSet getGraphResourceSet(File file, ResourceSet rsMetamodels) throws IOException {
		ResourceSet rsRDF = new ResourceSetImpl();
		registerEPackages(rsMetamodels, rsRDF);
		loadFileIntoResourceSet(file, rsRDF);
		return rsRDF;
	}
	
	protected ResourceSet getXMIResourceSet(File file, ResourceSet rsMetamodels) throws IOException {		
		ResourceSet rsXmi = new ResourceSetImpl();
		registerEPackages(rsMetamodels, rsXmi);
		loadFileIntoResourceSet(file, rsXmi);
		return rsXmi;
	}
	
	protected void assertNoDifferences(String testLabel, Comparison cmp) {
		if (cmp.getDifferences().isEmpty()) {
			return;
		}

		System.err.println("Differences were found in " + testLabel + ": ");
		for (Diff diff : cmp.getDifferences()) {
			System.err.println("* " + diff);
		}

		fail("Differences were reported: see error messages");
	}
	
	protected void equivalentModels (String testLabel, ResourceSet rsXmi, ResourceSet rsRDF) {
		EMFCompare compareEngine = EMFCompare.builder().build();
		final IComparisonScope scope = new DefaultComparisonScope(rsXmi, rsRDF, null);
		final Comparison cmp = compareEngine.compare(scope);
		assertNoDifferences(testLabel, cmp);
	}
}
