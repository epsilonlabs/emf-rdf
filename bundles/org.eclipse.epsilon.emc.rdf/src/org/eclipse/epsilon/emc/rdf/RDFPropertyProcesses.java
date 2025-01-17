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

import org.apache.jena.ontology.MaxCardinalityRestriction;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.util.iterator.ExtendedIterator;

public class RDFPropertyProcesses {

	private RDFPropertyProcesses() {
		// This class is not meant to be instantiated
	}

	public static ExtendedIterator<Statement> getPropertyStatementIterator(RDFQualifiedName propertyName, Resource resource) {
		ExtendedIterator<Statement> propertyStatementIt = null;
		// Filter all Property (Predicate) statements by prefix and local name
		if (propertyName.prefix == null) {
			propertyStatementIt = resource.listProperties()
				.filterKeep(stmt -> propertyName.localName.equals(stmt.getPredicate().getLocalName()));
		} else {
			Property prop = new PropertyImpl(propertyName.namespaceURI, propertyName.localName);
			propertyStatementIt = resource.listProperties(prop);
		}
		return propertyStatementIt;
	}

	public static ExtendedIterator<Statement> filterPropertyStatementsIteratorWithLanguageTag(RDFQualifiedName propertyName, ExtendedIterator<Statement> propertyStatements) {
		// If a language tag is used, only keep literals with that tag
		if (propertyName.languageTag != null) {
			propertyStatements = propertyStatements.filterKeep(stmt -> {
				if (stmt.getObject() instanceof Literal) {
					Literal l = (Literal) stmt.getObject();
					return propertyName.languageTag.equals(l.getLanguage());
				}
				return false;
			});
		}
		return propertyStatements;
	}

	public static MaxCardinalityRestriction getPropertyStatementMaxCardinalityRestriction(RDFQualifiedName propertyName, Resource resource) {
		// Gets all the propertyStatements and finds all the MaxCardinality restrictions, keeps the most restrictive (lowest maxCardinality)
		MaxCardinalityRestriction mostRestrictiveMaxCardinality = null;
		ExtendedIterator<Statement> propertyStatementIt = getPropertyStatementIterator(propertyName, resource);

		while (propertyStatementIt.hasNext()) {
			Statement statement = propertyStatementIt.next();
			OntProperty predicateOntProperty = statement.getPredicate().as(OntProperty.class);
			ExtendedIterator<Restriction> restrictionMaxCardinalityIt = predicateOntProperty.listReferringRestrictions()
					.filterKeep(restriction -> restriction.isMaxCardinalityRestriction());
			
			while (restrictionMaxCardinalityIt.hasNext()) {
				MaxCardinalityRestriction currentMaxCardinalityRestriction = restrictionMaxCardinalityIt.next().asMaxCardinalityRestriction();
				if (mostRestrictiveMaxCardinality == null) {
					mostRestrictiveMaxCardinality = currentMaxCardinalityRestriction;
				} else {
					if (mostRestrictiveMaxCardinality.getMaxCardinality() > currentMaxCardinalityRestriction.getMaxCardinality()) {
						mostRestrictiveMaxCardinality = currentMaxCardinalityRestriction;
					}
				}
			}
		}
		return mostRestrictiveMaxCardinality;
	}

}
