package org.eclipse.epsilon.emc.rdf.dt;
import org.eclipse.epsilon.common.dt.launching.dialogs.AbstractModelConfigurationDialog;
import org.eclipse.swt.widgets.Composite;

public class RDFModelConfigurationDialog extends AbstractModelConfigurationDialog {

	@Override
	protected String getModelName() {
		return "RDF Model";
	}

	@Override
	protected String getModelType() {
		return "RDF";
	}

	@Override
	protected void createGroups(Composite control) {
		createNameAliasGroup(control);
		createRDFUrisGroup(control);
		createRDFPrefixesGroup(control);
	}

	private void createRDFPrefixesGroup(Composite control) {
		// TODO Auto-generated method stub
		
	}

	private void createRDFUrisGroup(Composite control) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void loadProperties(){
		super.loadProperties();
		
		if (properties != null) {;
			// TODO populate URIs

			// TODO populate prefixes
		}
	}
	
	@Override
	protected void storeProperties(){
		super.storeProperties();

		// TODO populate URIs

		// TODO populate prefixes
	}

}
