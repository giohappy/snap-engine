/*
 * $Id: NavigationToolView.java,v 1.2 2007/04/23 13:49:34 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.nav;

import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.swing.LayerCanvasModel;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.AdjustableView;
import com.bc.ceres.grender.Viewport;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ApplicationPage;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import static java.lang.Math.*;

/**
 * A window which displays product spectra.
 */
public class NavigationToolView extends AbstractToolView {

    public static final String ID = NavigationToolView.class.getName();
    private static final int MIN_SLIDER_VALUE = -100;
    private static final int MAX_SLIDER_VALUE = +100;

    private LayerCanvasModelChangeHandler layerCanvasModelChangeChangeHandler;
    private ProductNodeListener productNodeChangeHandler;

    private ProductSceneView currentView;

    private NavigationCanvas canvas;
    private AbstractButton zoomInButton;
    private AbstractButton zoomZeroButton;
    private AbstractButton zoomOutButton;
    private AbstractButton zoomAllButton;
    private AbstractButton syncViewsButton;
    private JTextField zoomFactorField;
    private JSlider zoomSlider;
    private boolean inUpdateMode;

    private boolean debug = true;

    public NavigationToolView() {
    }

    @Override
    public JComponent createControl() {
        layerCanvasModelChangeChangeHandler = new LayerCanvasModelChangeHandler();
        productNodeChangeHandler = createProductNodeListener();

        zoomInButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomIn24.gif"), false);
        zoomInButton.setToolTipText("Zoom in."); /*I18N*/
        zoomInButton.setName("zoomInButton");
        zoomInButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                zoom(getCurrentView().getZoomFactor() * 1.2);
            }
        });

        zoomZeroButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomZero24.gif"), false);
        zoomZeroButton.setToolTipText("Actual Pixels."); /*I18N*/
        zoomZeroButton.setName("zoomZeroButton");
        zoomZeroButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                zoomToPixelResolution();
            }
        });

        zoomOutButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomOut24.gif"), false);
        zoomOutButton.setName("zoomOutButton");
        zoomOutButton.setToolTipText("Zoom out."); /*I18N*/
        zoomOutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                zoom(getCurrentView().getZoomFactor() / 1.2);
            }
        });

        zoomAllButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomAll24.gif"), false);
        zoomAllButton.setName("zoomAllButton");
        zoomAllButton.setToolTipText("Zoom all."); /*I18N*/
        zoomAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                zoomAll();
            }
        });

        syncViewsButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Chain24.gif"), true);
        syncViewsButton.setToolTipText("Synchronize compatible product views."); /*I18N*/
        syncViewsButton.setName("syncViewsButton");
        syncViewsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                maybeSynchronizeCompatibleProductViews();
            }
        });

        AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help24.gif"), false);
        helpButton.setToolTipText("Help."); /*I18N*/
        helpButton.setName("helpButton");


        final JPanel eastPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;

        gbc.gridy = 0;
        eastPane.add(zoomInButton, gbc);

        gbc.gridy++;
        eastPane.add(zoomZeroButton, gbc);

        gbc.gridy++;
        eastPane.add(zoomOutButton, gbc);

        gbc.gridy++;
        eastPane.add(zoomAllButton, gbc);

        gbc.gridy++;
        eastPane.add(syncViewsButton, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.VERTICAL;
        eastPane.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;

        gbc.gridy++;
        eastPane.add(helpButton, gbc);

        zoomFactorField = new JTextField();
        zoomFactorField.setColumns(5);
        zoomFactorField.setHorizontalAlignment(JTextField.RIGHT);
        zoomFactorField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                applyZoomFactorFieldValue();
            }
        });
        zoomFactorField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(final FocusEvent e) {
                applyZoomFactorFieldValue();
            }
        });

        zoomSlider = new JSlider(JSlider.HORIZONTAL);
        zoomSlider.setValue(0);
        zoomSlider.setMinimum(MIN_SLIDER_VALUE);
        zoomSlider.setMaximum(MAX_SLIDER_VALUE);
        zoomSlider.setPaintTicks(false);
        zoomSlider.setPaintLabels(false);
        zoomSlider.setSnapToTicks(false);
        zoomSlider.setPaintTrack(true);
        zoomSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                if (!inUpdateMode) {
                    zoom(sliderValueToZoomFactor(zoomSlider.getValue()));
                }
            }
        });

        final JPanel zoomFactorPane = new JPanel(new BorderLayout());
        zoomFactorPane.add(zoomFactorField, BorderLayout.WEST);

        final JPanel sliderPane = new JPanel(new BorderLayout(2, 2));
        sliderPane.add(zoomFactorPane, BorderLayout.WEST);
        sliderPane.add(zoomSlider, BorderLayout.CENTER);

        canvas = createNavigationCanvas();
//        canvas = new NavigationCanvas2(this);
        canvas.setBackground(new Color(138, 133, 128)); // image background
        canvas.setForeground(new Color(153, 153, 204)); // slider overlay

        final JPanel centerPane = new JPanel(new BorderLayout(4, 4));
        centerPane.add(BorderLayout.CENTER, canvas);
        centerPane.add(BorderLayout.SOUTH, sliderPane);

        final JPanel mainPane = new JPanel(new BorderLayout(8, 8));
        mainPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPane.add(centerPane, BorderLayout.CENTER);
        mainPane.add(eastPane, BorderLayout.EAST);

        mainPane.setPreferredSize(new Dimension(320, 320));

        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
            HelpSys.enableHelpKey(mainPane, getDescriptor().getHelpId());
        }

        setCurrentView(VisatApp.getApp().getSelectedProductSceneView());

        updateState();

        // Add an internal frame listener to VISAT so that we can update our
        // navigation window with the information of the currently activated
        // product scene view.
        //
        VisatApp.getApp().addInternalFrameListener(new NavigationIFL());

        return mainPane;
    }

    public ProductSceneView getCurrentView() {
        return currentView;
    }

    public void setCurrentView(final ProductSceneView newView) {
        final ProductSceneView oldView = currentView;
        if (oldView != newView) {
            if (oldView != null) {
                currentView.getProduct().removeProductNodeListener(productNodeChangeHandler);
                if (oldView.getLayerCanvas() != null) {
                    oldView.getLayerCanvas().getModel().removeChangeListener(layerCanvasModelChangeChangeHandler);
                }
            }
            currentView = newView;
            if (currentView != null) {
                currentView.getProduct().addProductNodeListener(productNodeChangeHandler);
                if (currentView.getLayerCanvas() != null) {
                    currentView.getLayerCanvas().getModel().addChangeListener(layerCanvasModelChangeChangeHandler);
                }
            }
            canvas.handleViewChanged(oldView, newView);
            updateState();
        }
    }


    NavigationCanvas createNavigationCanvas() {
        return new NavigationCanvas(this);
    }

    private void applyZoomFactorFieldValue() {
        Integer value = getZoomFactorFieldValue();
        if (value != null) {
            int adjustedValue = max(MIN_SLIDER_VALUE, min(MAX_SLIDER_VALUE, value));
            if (value != adjustedValue) {
                zoomFactorField.setText(String.valueOf(adjustedValue));
            }
            zoom(sliderValueToZoomFactor(adjustedValue));
        }
    }

    private Integer getZoomFactorFieldValue() {
        final String text = zoomFactorField.getText();
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setModelOffset(final double modelOffsetX, final double modelOffsetY) {
        final ProductSceneView view = getCurrentView();
        if (view != null) {
            view.getLayerCanvas().getViewport().move(modelOffsetX, modelOffsetY);
            maybeSynchronizeCompatibleProductViews();
        }
    }

    private void zoomToPixelResolution() {
        final ProductSceneView view = getCurrentView();
        if (view != null) {
            final LayerCanvas layerCanvas = view.getLayerCanvas();
            layerCanvas.getViewport().setZoomFactor(layerCanvas.getDefaultZoomFactor());
            maybeSynchronizeCompatibleProductViews();
        }
    }

    public void zoom(final double zoomFactor) {
        final ProductSceneView view = getCurrentView();
        if (view != null) {
            view.getLayerCanvas().getViewport().setZoomFactor(zoomFactor);
            maybeSynchronizeCompatibleProductViews();
        }
    }

    public void zoomAll() {
        final ProductSceneView view = getCurrentView();
        if (view != null) {
            view.getLayerCanvas().zoomAll();
            maybeSynchronizeCompatibleProductViews();
        }
    }

    private void maybeSynchronizeCompatibleProductViews() {
        if (syncViewsButton.isSelected()) {
            synchronizeCompatibleProductViews();
        }
    }

    private void synchronizeCompatibleProductViews() {
        final ProductSceneView currentView = getCurrentView();
        if (currentView == null) {
            return;
        }
        final JInternalFrame[] internalFrames = VisatApp.getApp().getAllInternalFrames();
        for (final JInternalFrame internalFrame : internalFrames) {
            if (internalFrame.getContentPane() instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) internalFrame.getContentPane();
                if (view != currentView) {
                    currentView.synchronizeViewport(view);
                }
            }
        }
    }

    /**
     * @param sv a value between MIN_SLIDER_VALUE and MAX_SLIDER_VALUE
     * @return a value between min and max zoom factor of the AdjustableView
     */
    private double sliderValueToZoomFactor(final int sv) {
        AdjustableView adjustableView = getCurrentView().getLayerCanvas();
        double f1 = floor(log10(adjustableView.getMinZoomFactor()));
        double f2 = floor(log10(adjustableView.getMaxZoomFactor())) + 1.0;
        double v1 = (double) (sv - zoomSlider.getMinimum()) / (double) (zoomSlider.getMaximum() - zoomSlider.getMinimum());
        double v2 = f1 + v1 * (f2 - f1);
        double zf = pow(10.0, v2);

        if (debug) {
            System.out.println("NavigationToolView.sliderValueToZoomFactor:");
            System.out.println("  sv = " + sv);
            System.out.println("  f1 = " + f1);
            System.out.println("  f2 = " + f2);
            System.out.println("  v1 = " + v1);
            System.out.println("  v2 = " + v2);
            System.out.println("  zf = " + zf);
        }

        return zf;
    }

    /**
     * @param zf a value between min and max zoom factor of the AdjustableView
     * @return a value between MIN_SLIDER_VALUE and MAX_SLIDER_VALUE
     */
    private int zoomFactorToSliderValue(final double zf) {
        AdjustableView adjustableView = getCurrentView().getLayerCanvas();
        double f1 = floor(log10(adjustableView.getMinZoomFactor()));
        double f2 = floor(log10(adjustableView.getMaxZoomFactor())) + 1.0;
        double v2 = log10(zf);
        double v1 = max(0.0, min(1.0, (v2 - f1) / (f2 - f1)));
        int sv = (int) (zoomSlider.getMinimum() + v1 * (zoomSlider.getMaximum() - zoomSlider.getMinimum()));

        if (debug) {
            System.out.println("NavigationToolView.zoomFactorToSliderValue:");
            System.out.println("  zf = " + zf);
            System.out.println("  f1 = " + f1);
            System.out.println("  f2 = " + f2);
            System.out.println("  v2 = " + v2);
            System.out.println("  v1 = " + v1);
            System.out.println("  sv = " + sv);
        }

        return sv;
    }

    private void updateState() {
        final boolean canNavigate = getCurrentView() != null;
        zoomInButton.setEnabled(canNavigate);
        zoomZeroButton.setEnabled(canNavigate);
        zoomOutButton.setEnabled(canNavigate);
        zoomAllButton.setEnabled(canNavigate);
        zoomSlider.setEnabled(canNavigate);
        syncViewsButton.setEnabled(canNavigate);
        zoomFactorField.setEnabled(canNavigate);
        updateTitle();
        updateValues();
    }

    private void updateTitle() {
// todo - activate when we can use ToolView.setTitle()  
/*
        if (currentView != null) {
            if (currentView.isRGB()) {
                setTitle(getDescriptor().getTitle() + " - " + currentView.getProduct().getProductRefString() + " RGB");
            } else {
                setTitle(getDescriptor().getTitle() + " - " + currentView.getRaster().getDisplayName());
            }
        } else {
            setTitle(getDescriptor().getTitle());
        }
*/
    }

    private void updateValues() {
        final ProductSceneView view = getCurrentView();
        if (view != null) {
            boolean oldState = inUpdateMode;
            inUpdateMode = true;
            final int sliderValue = zoomFactorToSliderValue(view.getZoomFactor());
            zoomSlider.setValue(sliderValue);
            zoomFactorField.setText(String.valueOf(sliderValue));
            inUpdateMode = oldState;
        }
    }

    private ProductNodeListener createProductNodeListener() {
        return new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(final ProductNodeEvent event) {
                if (event.getPropertyName().equalsIgnoreCase(Product.PROPERTY_NAME_NAME)) {
                    final ProductNode sourceNode = event.getSourceNode();
                    if ((currentView.isRGB() && sourceNode == currentView.getProduct())
                            || sourceNode == currentView.getRaster()) {
                        updateTitle();
                    }
                }
            }
        };
    }


    private class NavigationIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                PropertyMap preferences = VisatApp.getApp().getPreferences();
                final boolean showWindow = preferences.getPropertyBool(VisatApp.PROPERTY_KEY_AUTO_SHOW_NAVIGATION, true);
                if (showWindow) {
                    ApplicationPage page = VisatApp.getApp().getPage();
                    ToolView toolView = page.getToolView(NavigationToolView.ID);
                    if (toolView != null) {
                        page.showToolView(NavigationToolView.ID);
                    }
                }
            }
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            if (isControlCreated()) {
                final Container contentPane = e.getInternalFrame().getContentPane();
                if (contentPane instanceof ProductSceneView) {
                    final ProductSceneView view = (ProductSceneView) contentPane;
                    setCurrentView(view);
                } else {
                    setCurrentView(null);
                }
            }
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            if (isControlCreated()) {
                final Container contentPane = e.getInternalFrame().getContentPane();
                if (contentPane instanceof ProductSceneView) {
                    ProductSceneView view = (ProductSceneView) contentPane;
                    if (getCurrentView() == view) {
                        setCurrentView(null);
                    }
                }
            }
        }

    }

    private class LayerCanvasModelChangeHandler implements LayerCanvasModel.ChangeListener {
        @Override
        public void handleLayerPropertyChanged(Layer layer, PropertyChangeEvent event) {
        }

        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
        }

        @Override
        public void handleLayersAdded(Layer parentLayer, Layer[] childLayers) {
        }

        @Override
        public void handleLayersRemoved(Layer parentLayer, Layer[] childLayers) {
        }

        @Override
        public void handleViewportChanged(Viewport viewport, boolean orientationChanged) {
            updateValues();
            maybeSynchronizeCompatibleProductViews();
        }
    }
}
