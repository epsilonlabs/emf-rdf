/********************************************************************************
 * Copyright (c) 2025 University of York
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
package org.eclipse.epsilon.examples.rdf.emf;

import java.io.File;
import java.util.Arrays;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.Resource.Factory.Registry;
import org.eclipse.emf.emfatic.core.EmfaticResourceFactory;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.eol.EolModule;
import org.eclipse.epsilon.rdf.emf.RDFGraphResourceFactory;

public class ResourceExample {

	public static void main(String[] args) throws Exception {
		// Register the .rdfres and .emf extensions and their factories
		final Registry factoryRegistry = Resource.Factory.Registry.INSTANCE;
		factoryRegistry.getExtensionToFactoryMap().put("rdfres", new RDFGraphResourceFactory());
		factoryRegistry.getExtensionToFactoryMap().put("emf", new EmfaticResourceFactory());

		EmfModel emfModel = new EmfModel();
		emfModel.setModelFile(args[0]);
		emfModel.setMetamodelFiles(Arrays.asList("models/book.emf", "models/fiction.emf"));
		emfModel.setName("Model");
		emfModel.setReadOnLoad(true);
		emfModel.setStoredOnDisposal(false);
		emfModel.load();

		EolModule eolModule = new EolModule();
		eolModule.parse(new File("epsilon", "example.eol"));
		eolModule.getContext().getModelRepository().addModel(emfModel);
		eolModule.execute();
	}

}
