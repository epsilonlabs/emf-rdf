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
package org.eclipse.epsilon.emc.rdf;

import java.util.Map;
import java.util.function.Function;

/**
 * Represents a partially or fully-qualified name for an RDF resource, with an
 * optional namespace URI. Instances are created using {@link #from(String, Map)}.
 */
public class RDFQualifiedName {
	final String prefix;
	final String namespaceURI;
	final String localName;
	final String languageTag;

	public RDFQualifiedName(String prefix, String nsURI, String localName, String languageTag) {
		this.prefix = prefix;
		this.namespaceURI = nsURI;
		this.localName = localName;
		this.languageTag = languageTag;
	}

	/**
	 * Parses a property name, which may be in the form
	 * {@code (prefix:)?localName(@language)?}.
	 */
	public static RDFQualifiedName from(String property, Function<String, String> prefixToURIMapper) {
		int colonIdx = property.indexOf(':');
		String prefix = null, nsURI = null;
		if (colonIdx != -1) {
			prefix = property.substring(0, colonIdx);
			nsURI = prefixToURIMapper.apply(prefix);
			if (nsURI == null) {
				throw new IllegalArgumentException(String.format("Unknown prefix '%s'", prefix));
			}

			property = property.substring(colonIdx + 1);
		}

		int atIdx = property.indexOf('@');
		String languageTag = null;
		if (atIdx != -1) {
			languageTag = property.substring(atIdx + 1);
			property = property.substring(0, atIdx);
		}

		return new RDFQualifiedName(prefix, nsURI, property, languageTag);
	}
}