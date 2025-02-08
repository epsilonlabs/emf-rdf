package org.eclipse.epsilon.emc.rdf.lens.dt;

import org.eclipse.epsilon.common.dt.util.DialogUtil;
import org.eclipse.epsilon.emc.emf.dt.EmfMetaModelConfigurationDialog;
import org.eclipse.epsilon.emc.rdf.lens.LensedRDFModel;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class LensedRDFModelConfigurationDialog extends EmfMetaModelConfigurationDialog {

	private Text modelNameText;

	@Override
	protected void createGroups(Composite control) {
		super.createGroups(control);
		createRDFModelNameGroup(control);
		createFilesGroup(control);
	}

	private Composite createRDFModelNameGroup(Composite parent) {
		final Composite groupContent = DialogUtil.createGroupContainer(parent, "RDF Model", 2);

		Label modelNameLabel = new Label(groupContent, SWT.NONE);
		modelNameLabel.setText("Name: ");
		modelNameText = new Text(groupContent, SWT.BORDER);
		modelNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		groupContent.layout();
		groupContent.pack();
		return groupContent;
	}

	@Override
	protected void loadProperties(){
		super.loadProperties();

		if (properties == null) return;
		modelNameText.setText(properties.getProperty(LensedRDFModel.PROPERTY_RDF_MODEL_NAME));
	}
	
	@Override
	protected void storeProperties(){
		super.storeProperties();
		properties.put(LensedRDFModel.PROPERTY_RDF_MODEL_NAME, modelNameText.getText());
	}

}
