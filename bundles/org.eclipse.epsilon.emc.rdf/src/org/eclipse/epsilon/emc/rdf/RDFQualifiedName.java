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
import java.util.regex.Pattern;

/**
 * <p>
 * Represents a partially or fully-qualified name for an RDF resource, with an
 * optional namespace URI. Instances are created using
 * {@link #from(String, Map)}.
 * </p>
 *
 * <p>
 * There is a class invariant that:
 * </p>
 *
 * <code>this.namespaceURI != null if this.prefix != null</code>.
 */
public class RDFQualifiedName {

	protected static final Pattern COLON_SPLITTER = Pattern.compile(":{1,2}");

	final String prefix;
	final String namespaceURI;
	final String localName;
	final String languageTag;

	// NOTE: this constructor is intentionally private. See class comment.
	private RDFQualifiedName(String prefix, String nsURI, String localName, String languageTag) {
		this.prefix = prefix;
		this.namespaceURI = nsURI;
		this.localName = localName;
		this.languageTag = languageTag;
	}

	/**
	 * Parses a property name, which matches
	 * {@code (prefix:)?localName(@language)?}.
	 */
	public static RDFQualifiedName from(String property, Function<String, String> prefixToURIMapper) {
		String[] parts = COLON_SPLITTER.split(property);
		if (parts.length > 1) {
			String nsURI = prefixToURIMapper.apply(parts[0]);
			if (nsURI == null) {
				throw new IllegalArgumentException(String.format("Unknown prefix '%s'", parts[0]));
			}
			return from(parts[0], nsURI, parts[1]);
		}

		return from(null, null, property);
	}

	/**
	 * Parses a property where the prefix (if any) has already been resolved to
	 * a namespace URI, and the local name matches {@code localName(@language)?}.
	 */
	public static RDFQualifiedName from(String prefix, String nsURI, String localNameWithOptionalTag) {
		if (prefix != null && nsURI == null) {
			throw new IllegalArgumentException("Namespace URI cannot be null if prefix is not null");
		}
		
		int atIdx = localNameWithOptionalTag.indexOf('@');
		String localName = null, languageTag = null;
		if (atIdx == -1) {
			localName = localNameWithOptionalTag;
		} else {
			languageTag = localNameWithOptionalTag.substring(atIdx + 1);
			localName = localNameWithOptionalTag.substring(0, atIdx);
		}

		return new RDFQualifiedName(prefix, nsURI, localName, languageTag);
	}

	public RDFQualifiedName withLocalName(String newLocalName) {
		return new RDFQualifiedName(prefix, namespaceURI, newLocalName, languageTag);
	}
	
	public RDFQualifiedName withLanguageTag(String newLanguageTag) {
		return new RDFQualifiedName(prefix, namespaceURI, localName, newLanguageTag);
	}

	@Override
	public String toString() {
		return "RDFQualifiedName [prefix=" + prefix + ", namespaceURI=" + namespaceURI + ", localName=" + localName
				+ ", languageTag=" + languageTag + "]";
	}

}