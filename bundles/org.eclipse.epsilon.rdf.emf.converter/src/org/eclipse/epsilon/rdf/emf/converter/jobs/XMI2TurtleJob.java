package org.eclipse.epsilon.rdf.emf.converter.jobs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.epsilon.rdf.emf.config.RDFResourceConfiguration;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

/**
 * Job which converts an XMI-based model to the Turtle format,
 * producing a minimal .rdfres file in the process.
 */
public class XMI2TurtleJob extends Job {

	private final IFile modelFile;

	public XMI2TurtleJob(IFile modelFile) {
		super("Convert XMI to Turtle");

		this.modelFile = modelFile;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		var rdfresFilename = replaceFileExtension(modelFile.getName(), "rdfres");
		var ttlFilename = replaceFileExtension(modelFile.getName(), "ttl");
		var config = new RDFResourceConfiguration();
		config.getDataModels().add(ttlFilename);

		IContainer container = modelFile.getParent();
		if (container instanceof IFolder folder) {
			// Create .rdfres file
			IFile rdfresFile = folder.getFile(rdfresFilename);
			try {
				saveConfiguration(monitor, config, rdfresFile);
			} catch (Exception ex) {
				return Status.error("Failed to save .rdfres", ex);
			}

			// Create empty .ttl file
			IFile ttlFile = folder.getFile(ttlFilename);
			try {
				ttlFile.create(new ByteArrayInputStream(new byte[0]), true, monitor);
			} catch (Exception ex) {
				return Status.error("Failed to generate empty .ttl", ex);
			}

			// Load source resource
			ResourceSet sourceRS = new ResourceSetImpl();
			Resource sourceR = sourceRS.getResource(
				URI.createURI(modelFile.getLocationURI().toString()), true);

			// Load target resource
			ResourceSet targetRS = new ResourceSetImpl();
			Resource targetR = targetRS.getResource(
				URI.createURI(rdfresFile.getLocationURI().toString()), true);

			targetR.getContents().addAll(sourceR.getContents());
			try {
				targetR.save(null);
			} catch (IOException e) {
				return Status.error("Failed to save target .rdfres", e);
			}
		} else {
			return Status.error("Container is not a folder");
		}

		return Status.OK_STATUS;
	}

	protected void saveConfiguration(IProgressMonitor monitor, RDFResourceConfiguration config, IFile rdfresFile) throws IOException, CoreException {
			CustomClassLoaderConstructor constructor = new CustomClassLoaderConstructor(
				this.getClass().getClassLoader(), new LoaderOptions());
			String dumpedYaml = new Yaml(constructor).dumpAsMap(config);
			try (
				ByteArrayInputStream bis = new ByteArrayInputStream(dumpedYaml.getBytes(StandardCharsets.UTF_8));
			) {
				rdfresFile.create(bis, true, monitor);
			}
	}

	protected String replaceFileExtension(String originalFilename, String newExtension) {
		int dotIndex = originalFilename.lastIndexOf('.');
		if (dotIndex != -1) {
			return originalFilename.substring(0, dotIndex) + '.' + newExtension;
		} else {
			return originalFilename + '.' + newExtension;
		}
	}
}
