package org.eclipse.epsilon.rdf.validation;

import org.eclipse.epsilon.rdf.validation.RDFValidation.ValidationMode.RDFModelValidationReport;


public class RDFValidationException extends RuntimeException {

	private static final long serialVersionUID = -542046827985505503L;
	
	private final RDFModelValidationReport report;
	
	public RDFModelValidationReport getReport() {
		return report;
	}

	public RDFValidationException(String errorMessage) {
		super(errorMessage);
		this.report = null;
	}

	public RDFValidationException(String errorMessage, RDFModelValidationReport report) {
		super(errorMessage);
		this.report = report;
	}
}
