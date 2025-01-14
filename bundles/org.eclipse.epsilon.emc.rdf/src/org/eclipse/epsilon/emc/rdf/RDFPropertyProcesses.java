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

import org.apache.jena.ontology.OntProperty;
import org.apache.jena.ontology.Restriction;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.util.iterator.ExtendedIterator;

public class RDFPropertyProcesses {

	public static ExtendedIterator<Statement> getPropertyStatementIterator (RDFQualifiedName propertyName, Resource resource) {
		ExtendedIterator<Statement> propertyStatementIt = null;
		// Filter all Property (Predicate) statements by prefix and local name
		if (propertyName.prefix == null) {
			propertyStatementIt = resource.listProperties()
				.filterKeep(stmt -> propertyName.localName.equals(stmt.getPredicate().getLocalName()));
		} else {
			String prefixIri = resource.getModel().getNsPrefixMap().get(propertyName.prefix);
			Property prop = new PropertyImpl(prefixIri, propertyName.localName);
			propertyStatementIt = resource.listProperties(prop);
		}
		return propertyStatementIt;
	}
	
	public static ExtendedIterator<Statement> filterPropertyStatementsIteratorWithLanguageTag (RDFQualifiedName propertyName, ExtendedIterator<Statement> propertyStatements) {
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
	
	public static int getPropertyStatementMaxCardinality(RDFQualifiedName propertyName, Resource resource) {
		// Gets all the propertyStatements and finds all the MaxCardinality restrictions, keeps the most restrictive (lowest maxCardinality)
		int maxCardinality = -1;

		ExtendedIterator<Statement> propertyStatementIt = getPropertyStatementIterator(propertyName, resource);

		while (propertyStatementIt.hasNext()) {
			Statement statement = propertyStatementIt.next();
			OntProperty predicateOntProperty = statement.getPredicate().as(OntProperty.class);
			ExtendedIterator<Restriction> restrictionMaxCardinalityIt = predicateOntProperty.listReferringRestrictions()
					.filterKeep(restriction -> restriction.isMaxCardinalityRestriction());
			
			while (restrictionMaxCardinalityIt.hasNext()) {
				Restriction restiction = restrictionMaxCardinalityIt.next();				
				int value = restiction.asMaxCardinalityRestriction().getMaxCardinality();
				if ((maxCardinality == -1) | (maxCardinality > value)) {
					maxCardinality = value;
				}
			}
		}
		if (maxCardinality != -1) System.out.println("returning maxCardinality " + maxCardinality);
		return maxCardinality;
	}
	
	// Probably delete this later...
	private static void checkPropertyStmtForRestrictionsOnPredicate(Statement propertyStmt) {
			OntProperty predicateOntProperty = propertyStmt.getPredicate().as(OntProperty.class);
			ExtendedIterator<Restriction> propertyRestrictionIt = predicateOntProperty.listReferringRestrictions();
			propertyRestrictionIt.forEach(refferingRestriction -> {
				System.out.println(
						"  property - " + propertyStmt + " predicate has restrictions -" + refferingRestriction);

				// Play guess who with the Cardinality restrictions
				
				if (refferingRestriction.isCardinalityRestriction()) {
					System.out.println("   " + refferingRestriction + " asCardinalityRestriction : "
							+ refferingRestriction.asCardinalityRestriction().getCardinality());
				}

				if (refferingRestriction.isMinCardinalityRestriction()) {
					System.out.println("   " + refferingRestriction + " asMinCardinalityRestriction : "
							+ refferingRestriction.asMinCardinalityRestriction().getMinCardinality());
				}

				if (refferingRestriction.isMaxCardinalityRestriction()) {
					System.out.println("   " + refferingRestriction + " asMaxCardinalityRestriction : "
							+ refferingRestriction.asMaxCardinalityRestriction().getMaxCardinality());
				}
			});
			// TODO Look up the Restrictions on a List of Restrictions stored on the model. 
	}
	
}
