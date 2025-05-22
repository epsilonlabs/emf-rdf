package org.eclipse.epsilon.rdf.emf.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.emfatic.core.EmfaticResourceFactory;
import org.eclipse.epsilon.rdf.emf.RDFGraphResourceFactory;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class MultiEAttributeUpdateRDFTest {
	
	@BeforeClass
	public static void setupDrivers() {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("rdfres", new RDFGraphResourceFactory());
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("emf", new EmfaticResourceFactory());
	}

	private final File originalTTL = new File("resources/EAttribute_test/model_data.ttl.org");
	private final File workingTTL = new File("resources/EAttribute_test/model_data.ttl");

	private final File rdfGraphModel = new File("resources/EAttribute_test/model.rdfres");
	private final File xmiModelBefore = new File("resources/EAttribute_test/model_before.xmi");
	// This file is generated _AFTER_ changing the RDFGraphResource
	private final File xmiModelAfter = new File("resources/EAttribute_test/model_after.xmi");
	private final File metaModelFile = new File("resources/EAttribute_test/metamodel.emf");

	ResourceSet metaModel = null;
	ResourceSet rdf = null; 
	ResourceSet xmiBefore = null;
	ResourceSet xmiAfter = null;

	// Entity from the Model(s) used to test Attributes
	EObject rdfEntity = null;
	EObject xmiEntity = null;

	private String testType = "";
	
	
	@Test
	public void baselineBeforeTest() throws IOException {
		// Check we can load the beforeXMI and that it matched the RDF version
		testType = "baselineBeforeTest";	
		equivalentModels("baselineBeforeTest : ", rdf , xmiBefore);
	}
	
	public void addTo(Object value, EList<Object> eList) {
		eList.add("Bob");
	}
	
	@Test
	public void unordered () throws IOException {
		setUpWorkingTTL();
		Object multiValueXmi = xmiEntity.eGet(getEAttribute(xmiEntity, "names"));
		if(multiValueXmi instanceof EList) {
			addTo("Bob", (EList<Object>) multiValueXmi);
		}
		Object multiValueRdf = rdfEntity.eGet(getEAttribute(rdfEntity, "names"));
		if(multiValueRdf instanceof EList) {
			addTo("Bob", (EList<Object>) multiValueRdf);
		}
		saveReloadAndTest("unordered");
	}
	
	// Functions not tests
	
	@After
	public void cleanUp() throws IOException {
		if(workingTTL.exists()) {
			Files.delete(workingTTL.toPath());
		}
		if(xmiModelAfter.exists()) {
			Files.delete(xmiModelAfter.toPath());
		}
	}
	
	public void setUpWorkingTTL() throws IOException {
		copyFile(originalTTL, workingTTL);

		metaModel = getMetaModelResourceSet(metaModelFile);
		rdf = getGraphResourceSet(rdfGraphModel, metaModel);
		xmiBefore = getXMIResourceSet(xmiModelBefore, metaModel);

		rdfEntity = getRdfEntityForAttributeTest(rdf);
		xmiEntity = getRdfEntityForAttributeTest(xmiBefore);
	}
	
	public void saveReload() throws IOException {
		rdf.getResources().get(0).save(null);
		saveBeforeXmi(xmiModelAfter);
		xmiAfter = getXMIResourceSet(xmiModelAfter, metaModel);
		rdf = getGraphResourceSet(rdfGraphModel, metaModel);
	}

	public void saveReloadAndTest(String changeType) throws IOException {
		rdf.getResources().get(0).save(null);
		saveBeforeXmi(xmiModelAfter);

		xmiAfter = getXMIResourceSet(xmiModelAfter, metaModel);
		rdf = getGraphResourceSet(rdfGraphModel, metaModel);

		equivalentModels(testType + " After " + changeType, rdf , xmiAfter);
	}
		
	public void saveBeforeXmi (File destinationFile) throws FileNotFoundException, IOException {
		if (destinationFile.exists()) {
			destinationFile.delete();
		}
		if (destinationFile.createNewFile()) {
			try (OutputStream destinationStream = new FileOutputStream(destinationFile)) {
				xmiBefore.getResources().get(0).save(destinationStream, null);
				//delaySeconds(1);
			}
		} else {
			System.err.println("Failed to save XMI : " + destinationFile.toPath().toString());
		}
	}

	protected void copyFile(File source, File destination) throws FileNotFoundException, IOException {
		Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	protected EObject getRdfEntityForAttributeTest (ResourceSet rdf) {
		Resource rdfResource = rdf.getResources().get(0);
		EObject rdfModel = rdfResource.getContents().get(0);
		EObject rdfEntity = rdfModel.eContents().get(0);
		return rdfEntity;
	}

	public EAttribute getEAttribute(EObject eObject, String AttributeName ) {
		EList<EAttribute> attributes = eObject.eClass().getEAttributes();
		for (EAttribute eAttribute : attributes) {
			if(eAttribute.getName().equals(AttributeName)) {
				return eAttribute;
			}
		}
		return null;
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
		if (file.exists()) {
			ResourceSet rsXmi = new ResourceSetImpl();
			registerEPackages(rsMetamodels, rsXmi);
			loadFileIntoResourceSet(file, rsXmi);
			return rsXmi;
		} else {
			System.err.println("Missing file : " + file.toPath().toString());
			return null;
		}
	}
	
	protected void assertNoDifferences(String testLabel, Comparison cmp) {
		StringBuilder report = new StringBuilder();
		if (cmp.getDifferences().isEmpty()) {
			return;
		}

		report.append("Differences were found in " + testLabel + ": ");
		for (Diff diff : cmp.getDifferences()) {
			report.append("\n   - " + diff);
		}
		fail("Differences were reported: see error messages\n" + report);
	}

	protected void equivalentModels (String testLabel, ResourceSet rsXmi, ResourceSet rsRDF) {
		EMFCompare compareEngine = EMFCompare.builder().build();
		final IComparisonScope scope = new DefaultComparisonScope(rsXmi, rsRDF, null);
		final Comparison cmp = compareEngine.compare(scope);
		assertNoDifferences(testLabel, cmp);
	}
	

}
