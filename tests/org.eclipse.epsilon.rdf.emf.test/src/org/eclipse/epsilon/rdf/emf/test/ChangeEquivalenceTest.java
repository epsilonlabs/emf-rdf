package org.eclipse.epsilon.rdf.emf.test;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import org.eclipse.epsilon.emc.emf.InMemoryEmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.eol.execute.context.Variable;
import org.eclipse.epsilon.eol.types.EolAnyType;
import org.eclipse.epsilon.eol.types.EolType;
import org.eclipse.epsilon.rdf.emf.RDFGraphResourceFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

// Add this link to the RESOURCE.MD, how to load custom resources https://eclipse.dev/epsilon/doc/articles/in-memory-emf-model/

/**
 * <p>
 * Parameterized test which uses EMF Compare to compare the {@code .xmi} version
 * of a given model with its {@code .rdfres} version. It fails if any differences
 * are found.
 * </p>
 *
 * <p>
 * Test cases are eol files in subfolders of resources/equivalence_multivalue, where metamodels are in .emf
 * format, XMI models use the .xmi extension, and RDF models use the .rdfres extension and RDF data models are .ttl.
 * </p>
 */

@RunWith(Parameterized.class)
public class ChangeEquivalenceTest {	
	
	static final boolean CONSOLE_OUTPUT_ACTIVE = false;
	
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
	
	// Folders for processing as tests
	final static File RESOURCES_FOLDER = new File("resources");
	final static File TEST_FOLDER = new File(RESOURCES_FOLDER, "changeEquivalence");
	
	@Parameters(name = "{0}")
	public static Object[] data() {
		List<File> fileList = new ArrayList<File>();
		final File baseFolder = TEST_FOLDER;
		File[] subdirs = baseFolder.listFiles(f -> f.isDirectory());
		for (File subdir : subdirs) {
			File[] eolTestFiles = subdir.listFiles(fn -> fn.getName().endsWith(".eol"));		
			Arrays.sort(eolTestFiles, (a, b) -> a.getName().compareTo(b.getName()));
			for (File file : eolTestFiles) {
				fileList.add(file);
			}
		}
		return fileList.toArray();
	}
	
	protected final File eolTestFile;
	protected final File eolTestFolder;
	
	public ChangeEquivalenceTest(File eolTestFile) {		
		this.eolTestFile = eolTestFile;
		this.eolTestFolder = eolTestFile.getParentFile();
	}
	
	protected void backupRdfFiles() throws FileNotFoundException, IOException {
		File[] rdfFiles = eolTestFolder.listFiles(fn -> fn.getName().endsWith(".ttl"));
		for (File file : rdfFiles) {
			String backupFilePath = file.getAbsolutePath().concat("_backup");
			File backupFile = new File(backupFilePath);
			Files.copy(file.toPath(), backupFile.toPath());
		}
	}
	
	protected void restoreRdfFiles() throws FileNotFoundException, IOException {
		File[] rdfFiles = eolTestFolder.listFiles(fn -> fn.getName().endsWith(".ttl_backup"));
		for (File file : rdfFiles) {
			String backupFilePath = file.getAbsolutePath().replace(".ttl_backup",".ttl");
			File backupFile = new File(backupFilePath);
			Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			Files.delete(file.toPath());
		}
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

	protected void loadModelsWithExtension(File modelFolder, String fileNameSuffix, ResourceSet rs) throws IOException {		
		for (File fEmf : modelFolder.listFiles(fn -> fn.getName().endsWith(fileNameSuffix))) {
			Resource rMetamodel = rs.createResource(URI.createFileURI(fEmf.getAbsolutePath()));
			rs.getResources().add(rMetamodel);
		}
		for (Resource r : rs.getResources()) {
			r.load(null);
		}
	}
	
	protected void assertNoDifferences(File testCase, Comparison cmp) {
		if (cmp.getDifferences().isEmpty()) {
			return;
		}

		System.err.println("Differences were found in " + testCase + ": ");
		for (Diff diff : cmp.getDifferences()) {
			System.err.println("* " + diff);
			// We could output the Turtle and XMI for the models here?
		}
		fail("Differences were reported: see error messages");
	}
	
	protected void executeEol(Resource resource, File eolCode) throws Exception {
		// Loads the EMF model from the resource
		InMemoryEmfModel model = new InMemoryEmfModel(resource);
		model.setName("theModel");
		// Parses and executes the EOL program
		EolModule module = new EolModule();
		module.parse(eolCode);
		
		module.getContext().getFrameStack().put(
				Variable.createReadOnlyVariable("consoleOutput", CONSOLE_OUTPUT_ACTIVE)
			);
		
		module.getContext().getModelRepository().addModel(model);
		Object result = module.execute();
		
		// Check result, we may expect something to fail like trying to remove things from an empty model
		
		// Resource has been changed, we can save it or stream it if needed...
		// resource.save(null);
		
		// remember to clear out the model 
		// model.dispose();
	}

	//Do not try and run this yet!
	@Test 
	public void loadModelsAndRunEolTest() throws Exception {
		
		if(CONSOLE_OUTPUT_ACTIVE) {System.out.println(String.format("\n ** TEST: %s ** ",eolTestFile.getName()));}
		
		restoreRdfFiles(); // Clean up any backup files from failed tests.
		
		// Load the meta model
		ResourceSet rsMetamodels = new ResourceSetImpl();		
		loadModelsWithExtension(eolTestFolder, ".emf", rsMetamodels);

		// Load and change XMI model resource, this is what we want the RDF to match
		if(CONSOLE_OUTPUT_ACTIVE) {System.out.println("\n == XMI ==");}
		ResourceSet rsXMI = new ResourceSetImpl();
		registerEPackages(rsMetamodels, rsXMI);
		loadModelsWithExtension(eolTestFolder, ".xmi", rsXMI);
		Resource xmiModelResource = rsXMI.getResources().get(0);
		executeEol(xmiModelResource, eolTestFile);
		
		// Load and change RDF model resource, this should match the XMI after save and reload		
		if(CONSOLE_OUTPUT_ACTIVE) {System.out.println("\n\n == RDF ==");}
		ResourceSet rsRDF = new ResourceSetImpl();
		registerEPackages(rsMetamodels, rsRDF);
		loadModelsWithExtension(eolTestFolder, ".rdfres", rsRDF);
		Resource rdfModelResource = rsRDF.getResources().get(0);
		executeEol(rdfModelResource, eolTestFile);
		
		// SAVE RDF resource, clear the resource set
		backupRdfFiles();
		rdfModelResource.save(null);
		rsRDF.getResources().remove(0);
		
		// Reload RDF model resource
		loadModelsWithExtension(eolTestFolder, ".rdfres", rsRDF);
		rdfModelResource = rsRDF.getResources().get(0);
		
		restoreRdfFiles(); // We may crash out on the test
		
		// Compare reloaded RDF and XMI models
		EMFCompare compareEngine = EMFCompare.builder().build();
		final IComparisonScope scope = new DefaultComparisonScope(rsXMI, rsRDF, null);
		final Comparison cmp = compareEngine.compare(scope);
		assertNoDifferences(eolTestFile, cmp);
		
		
	}
}