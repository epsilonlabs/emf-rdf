package org.eclipse.epsilon.emc.rdf;

public class RDFQualifiedName {
	final String prefix;
	final String localName;
	final String languageTag;

	public RDFQualifiedName(String prefix, String localName, String languageTag) {
		this.prefix = prefix;
		this.localName = localName;
		this.languageTag = languageTag;
	}

	/**
	 * Parses a property name, which may be in the form
	 * {@code (prefix:)?localName(@language)?}.
	 */
	public static RDFQualifiedName fromString(String property) {
		int colonIdx = property.indexOf(':');
		String prefix = null;
		if (colonIdx != -1) {
			prefix = property.substring(0, colonIdx);
			property = property.substring(colonIdx + 1);
		}

		int atIdx = property.indexOf('@');
		String languageTag = null;
		if (atIdx != -1) {
			languageTag = property.substring(atIdx + 1);
			property = property.substring(0, atIdx);
		}

		return new RDFQualifiedName(prefix, property, languageTag);
	}
}