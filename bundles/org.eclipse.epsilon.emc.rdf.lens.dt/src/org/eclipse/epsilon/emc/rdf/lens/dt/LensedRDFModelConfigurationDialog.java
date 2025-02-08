package org.eclipse.epsilon.emc.rdf.lens.dt;

import org.eclipse.epsilon.common.dt.util.DialogUtil;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.rdf.dt.RDFModelConfigurationDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class LensedRDFModelConfigurationDialog extends RDFModelConfigurationDialog {

	private Text ePackageURIText;

	@Override
	protected void createGroups(Composite control) {
		super.createGroups(control);
		createEPackageURIGroup(control);
	}

	private Composite createEPackageURIGroup(Composite parent) {
		final Composite groupContent = DialogUtil.createGroupContainer(parent, "Ecore EPackage", 2);

		Label modelNameLabel = new Label(groupContent, SWT.NONE);
		modelNameLabel.setText("URI: ");
		ePackageURIText = new Text(groupContent, SWT.BORDER);
		ePackageURIText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		groupContent.layout();
		groupContent.pack();
		return groupContent;
	}

	@Override
	protected void loadProperties(){
		super.loadProperties();

		if (properties == null) return;
		ePackageURIText.setText(properties.getProperty(EmfModel.PROPERTY_METAMODEL_URI));
	}
	
	@Override
	protected void storeProperties(){
		super.storeProperties();
		properties.put(EmfModel.PROPERTY_METAMODEL_URI, ePackageURIText.getText());
	}

	@Override
	protected String getModelName() {
		return "Ecore Lens over RDF Model";
	}

	@Override
	protected String getModelType() {
		return "EcoreLensRDFModel";
	}

	
}
