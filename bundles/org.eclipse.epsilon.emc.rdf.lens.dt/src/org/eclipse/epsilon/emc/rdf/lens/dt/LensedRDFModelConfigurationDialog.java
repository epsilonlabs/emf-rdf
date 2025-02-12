package org.eclipse.epsilon.emc.rdf.lens.dt;

import org.eclipse.epsilon.common.dt.util.DialogUtil;
import org.eclipse.epsilon.emc.emf.EmfModel;
import org.eclipse.epsilon.emc.rdf.dt.RDFModelConfigurationDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

public class LensedRDFModelConfigurationDialog extends RDFModelConfigurationDialog {

	private Text ePackageURIText;

	@Override
	protected void createGroups(Composite parent) {
		parent.setLayout(new FillLayout());

		// Create a tab folder within the parent composite
		TabFolder tabFolder = new TabFolder(parent, SWT.NONE);

		// Add the General tab
		TabItem generalTab = new TabItem(tabFolder, SWT.NONE);
		generalTab.setText("General");
		Composite generalComposite = createGeneralTab(tabFolder);

		// Add the RDF Options tab
		TabItem rdfOptionsTab = new TabItem(tabFolder, SWT.NONE);
		rdfOptionsTab.setText("RDF Options");
		Composite rdfOptionsComposite = this.createRdfOptionsTab(tabFolder);

		// Add the Lensing tab
		TabItem lensingTab = new TabItem(tabFolder, SWT.NONE);
		lensingTab.setText("Lensing");
		Composite lensingComposite = this.createLensingTab(tabFolder);

		// Associate each composite with its respective tab item
		generalTab.setControl(generalComposite);
		rdfOptionsTab.setControl(rdfOptionsComposite);
		lensingTab.setControl(lensingComposite);
	}

	private Composite createGeneralTab(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		this.createNameAliasGroup(composite);
		this.createDataModelRDFUrlsGroup(composite);
		this.createSchemaModelRDFUrlsGroup(composite);

		composite.pack();
		composite.layout();
		return composite;
	}

	private Composite createRdfOptionsTab(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		this.createNamespaceMappingGroup(composite);
		this.createLanguagePreferenceGroup(composite);

		composite.pack();
		composite.layout();
		return composite;
	}

	private Composite createLensingTab(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());

		this.createEPackageURIGroup(composite);

		composite.pack();
		composite.layout();
		return composite;
	}

	private Composite createEPackageURIGroup(Composite parent) {
		final Composite groupContent = DialogUtil.createGroupContainer(parent, "Ecore EPackage", 2);

		Label modelNameLabel = new Label(groupContent, SWT.NONE);
		modelNameLabel.setText("URI: ");
		ePackageURIText = new Text(groupContent, SWT.BORDER);
		ePackageURIText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return groupContent;
	}

	@Override
	protected void loadProperties() {
		super.loadProperties();

		if (properties == null)
			return;
		ePackageURIText.setText(properties.getProperty(EmfModel.PROPERTY_METAMODEL_URI));
	}

	@Override
	protected void storeProperties() {
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
