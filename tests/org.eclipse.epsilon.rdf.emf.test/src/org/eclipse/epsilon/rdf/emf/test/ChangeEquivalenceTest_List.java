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
package org.eclipse.epsilon.rdf.emf.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
public class ChangeEquivalenceTest_List extends AbstractChangeEquivalenceTest {
	
	static final boolean CONSOLE_OUTPUT_ACTIVE = true;
	
	// Folders for processing as tests
	final static File RESOURCES_FOLDER = new File("resources");
	final static File TEST_FOLDER = new File(RESOURCES_FOLDER, "changeEquivalence_List");
	
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
	
	public ChangeEquivalenceTest_List(File eolTestFile) {
		super(eolTestFile);
	}
}
