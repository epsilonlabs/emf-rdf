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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.epsilon.rdf.emf.RDFGraphResourceImpl;
import org.eclipse.epsilon.rdf.emf.RDFGraphResourceNotificationAdapterChangeRDF;
import org.eclipse.epsilon.rdf.emf.RDFGraphResourceNotificationAdapterTrace;
import org.junit.Test;

/**
 * <p>
 * Checks that the RDF eAdapters propagate to children elements when they are
 * added to an RDF resource, and that they are automatically remove from
 * children when they are detached from the resource.
 * </p>
 */
public class AdapterPropagationTest {

	@Test
	public void changeAdapterPropagates() {
		var r = new RDFGraphResourceImpl();
		var adapter = new RDFGraphResourceNotificationAdapterChangeRDF(r);
		assertEAdaptersPropagate(r, adapter);
	}

	@Test
	public void traceAdapterPropagates() {
		var r = new RDFGraphResourceImpl();
		var adapter = new RDFGraphResourceNotificationAdapterTrace(r);
		assertEAdaptersPropagate(r, adapter);
	}

	private void assertEAdaptersPropagate(RDFGraphResourceImpl r, Adapter adapter) {
		r.eAdapters().add(adapter);

		var ePackage = EcorePackage.eINSTANCE.getEcoreFactory().createEPackage();
		r.getContents().add(ePackage);
		assertEquals("Adapter is added to EPackage when added to resource", 1, ePackage.eAdapters().size());
		assertSame("Adapter is the same instance as in the resource", adapter, ePackage.eAdapters().get(0));

		r.getContents().remove(ePackage);
		assertTrue("Adapter is removed from EPackage when removed from resource", ePackage.eAdapters().isEmpty());
	}

}
