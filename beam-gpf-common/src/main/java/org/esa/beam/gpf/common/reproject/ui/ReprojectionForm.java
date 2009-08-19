package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;
import org.esa.beam.framework.ui.application.SelectionChangeListener;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.text.MessageFormat;

/**
 * User: Marco
 * Date: 16.08.2009
 */
public class ReprojectionForm extends JTabbedPane {

    private final AppContext appContext;
    private final ValueContainer valueContainer;

    private ProjectedCrsSelectionFormModel crsSelectionModel;
    private SourceProductSelector sourceProductSelector;
    private TargetProductSelector targetProductSelector;
    private SourceProductSelector collocateProductSelector;
    private ProjectedCrsSelectionForm crsSelectionForm;
    private JRadioButton collocateRadioButton;

    public ReprojectionForm(final ReprojectionFormModel model,
                            TargetProductSelector targetProductSelector,
                            AppContext appContext) throws FactoryException {
        this.appContext = appContext;
        valueContainer = ValueContainer.createObjectBacked(model);
        this.targetProductSelector = targetProductSelector;
        sourceProductSelector = new SourceProductSelector(appContext, "Source Product:");

        final List<CrsInfo> crsList = CrsInfo.generateCRSList();
        CrsInfoListModel crsInfoListModel = new CrsInfoListModel(crsList);
        CoordinateReferenceSystem selectedCrs = crsInfoListModel.getElementAt(0).getCrs();
        crsSelectionModel = new ProjectedCrsSelectionFormModel(crsInfoListModel, selectedCrs);

        createUI();
        bindUI();
        updateUIState();
    }

    private void createUI() {
        addTab("I/O Parameter", createIOTab());
        addTab("Projection Parameter", createProjectionTab());
    }

    private JPanel createProjectionTab() {

        final JPanel projPanel = new JPanel();
        projPanel.setLayout(new BoxLayout(projPanel, BoxLayout.Y_AXIS));
        projPanel.add(createProjectionPanel());
//        projPanel.add(createGridDifinitionForm());
        return projPanel;
    }

    // currently not displaying this form
//    private GridDefinitionForm createGridDifinitionForm() throws FactoryException {
//        final Rectangle sourceDimension = new Rectangle(100, 200);
//        final CoordinateReferenceSystem sourceCrs = DefaultGeographicCRS.WGS84;
//        final CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:32632");
//        final String unit = targetCrs.getCoordinateSystem().getAxis(0).getUnit().toString();
//        GridDefinitionFormModel formModel = new GridDefinitionFormModel(sourceDimension, sourceCrs, targetCrs, unit);
//        final GridDefinitionForm gridDefinitionForm = new GridDefinitionForm(formModel);
//        gridDefinitionForm.setBorder(BorderFactory.createTitledBorder("Target Grid"));
//        return gridDefinitionForm;
//    }

    private JPanel createIOTab() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTablePadding(3,3);

        final JPanel ioPanel = new JPanel(tableLayout);
        ioPanel.add(createSourceProductPanel());
        ioPanel.add(targetProductSelector.createDefaultPanel());
        ioPanel.add(tableLayout.createVerticalSpacer());
        return ioPanel;
    }

    private JPanel createProjectionPanel() {
        collocateProductSelector = new SourceProductSelector(appContext, "Product:");
        collocateProductSelector.setProductFilter(new CollocateProductFilter());
        collocateProductSelector.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product selectedProduct = collocateProductSelector.getSelectedProduct();
                if (selectedProduct != null) {
                    updateTargetCrs(selectedProduct.getGeoCoding().getModelCRS());
                }
            }
        });
        crsSelectionForm = createCrsSelectionForm();
        final ButtonGroup group = new ButtonGroup();
        collocateRadioButton = new JRadioButton("Collocate with Product");
        JRadioButton projectionRadioButton = new JRadioButton("Define Projection", true);

        collocateRadioButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final Product selectedProduct = collocateProductSelector.getSelectedProduct();
                if (selectedProduct != null) {
                    updateTargetCrs(selectedProduct.getGeoCoding().getModelCRS());
                }
                updateUIState();
            }
        });
        group.add(collocateRadioButton);
        group.add(projectionRadioButton);
        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(3, 3);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setRowWeightY(0, 0.0);
        tableLayout.setCellColspan(0, 0, 2);
        tableLayout.setCellColspan(1, 0, 2);
        tableLayout.setRowWeightY(1, 1.0);
        tableLayout.setCellPadding(1, 0, new Insets(3, 15, 3, 3));
        tableLayout.setRowPadding(1, new Insets(3, 3, 15, 3));
        tableLayout.setRowWeightY(2, 0.0);
        tableLayout.setCellColspan(2, 0, 2);
        tableLayout.setRowWeightY(3, 0.0);
        tableLayout.setCellPadding(3, 0, new Insets(3, 15, 3, 3));
        tableLayout.setCellWeightX(3, 1, 0.0);

        final JPanel panel = new JPanel(tableLayout);
        panel.setBorder(BorderFactory.createTitledBorder("Projection"));
        // row 0
        panel.add(projectionRadioButton);                                       // col 0
        // row 1
        panel.add(crsSelectionForm);
        // row 2
        panel.add(collocateRadioButton);                                        // col 0
        // row 3
        panel.add(collocateProductSelector.getProductNameComboBox());           // col 0
        panel.add(collocateProductSelector.getProductFileChooserButton());      // col 1
        return panel;
    }

    private void updateUIState() {
        final boolean collocate = collocateRadioButton.isSelected();
        collocateProductSelector.getProductNameComboBox().setEnabled(collocate);
        collocateProductSelector.getProductFileChooserButton().setEnabled(collocate);
        crsSelectionForm.setFormEnabled(!collocate);
    }

    private ProjectedCrsSelectionForm createCrsSelectionForm() {
        final ProjectedCrsSelectionForm crsForm = new ProjectedCrsSelectionForm(crsSelectionModel);
        crsSelectionModel.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateTargetCrs((CoordinateReferenceSystem) evt.getNewValue());
            }
        });
        return crsForm;
    }

    private void updateTargetCrs(CoordinateReferenceSystem value) {
        try {
            valueContainer.setValue("targetCrs", value);
        } catch (ValidationException e) {
            e.printStackTrace();
        }
    }

    private void bindUI() {
        final BindingContext context = new BindingContext(valueContainer);
        context.bind("sourceProduct", sourceProductSelector.getProductNameComboBox());
    }

    public void prepareShow() {
        sourceProductSelector.initProducts();
        if (sourceProductSelector.getProductCount() > 0) {
            sourceProductSelector.setSelectedIndex(0);
        }
        collocateProductSelector.initProducts();
        updateUIState();
    }

    public void prepareHide() {
        sourceProductSelector.releaseProducts();
    }

    private JPanel createSourceProductPanel() {
        final JPanel panel = sourceProductSelector.createDefaultPanel();
        sourceProductSelector.getProductNameLabel().setText("Name:");
        sourceProductSelector.getProductNameComboBox().setPrototypeDisplayValue(
                "MER_RR__1PPBCM20030730_071000_000003972018_00321_07389_0000.N1");
        sourceProductSelector.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                updateTargetProductName(sourceProductSelector.getSelectedProduct());
                updateUIState();
            }
        });
        return panel;
    }

    private void updateTargetProductName(Product selectedProduct) {
        final TargetProductSelectorModel selectorModel = targetProductSelector.getModel();
        if (selectedProduct != null) {
            final String productName = MessageFormat.format("{0}_reprojected", selectedProduct.getName());
            selectorModel.setProductName(productName);
        } else if (selectorModel.getProductName() == null) {
            selectorModel.setProductName("reprojected");
        }
    }

    private class CollocateProductFilter implements ProductFilter {

        @Override
            public boolean accept(Product product) {
            return sourceProductSelector.getSelectedProduct() != product;
        }
    }
}
