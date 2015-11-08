package com.kms.katalon.composer.components.impl.util;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

public class ControlUtils {
    private ControlUtils() {
        //Disable default constructor.
    }
    
    public static final int DF_CONTROL_HEIGHT = 18;
    public static final int DF_VERTICAL_SPACING = 10;
    public static final int DF_HORIZONTAL_SPACING = 10;
    
    public static void recursiveSetEnabled(Control ctrl, boolean enabled) {
        if (ctrl instanceof Composite) {
            Composite comp = (Composite) ctrl;
            for (Control c : comp.getChildren()) {
                recursiveSetEnabled(c, enabled);
            }
        } else {
            ctrl.setEnabled(enabled);
        }
    }
    
    public static void setFontToBeBold(Control ctrl) {
        ctrl.setFont(JFaceResources.getFontRegistry().getBold(""));
    }
    
    public static void setFontSize(Control ctrl, int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("Font's size must be a positive number");
        }
        FontData[] fD = ctrl.getFont().getFontData();
        fD[0].setHeight(height);
        ctrl.setFont(new Font(ctrl.getDisplay(), fD));
    }
}
