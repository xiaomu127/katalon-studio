package com.kms.katalon.composer.components.impl.util;

import java.util.concurrent.Callable;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ScrollBar;

import com.kms.katalon.composer.components.impl.constants.ComposerComponentsImplMessageConstants;
import com.kms.katalon.composer.components.impl.control.CMenu;
import com.kms.katalon.entity.file.FileEntity;

public class ControlUtils {
    private static final String UPDATING_LAYOUT = "updatingLayout";

    public static final int DF_CONTROL_HEIGHT = 18;

    public static final int DF_VERTICAL_SPACING = 10;

    public static final int DF_HORIZONTAL_SPACING = 10;

    private static final int DELAY_IN_MILLIS = 50;

    public static final int MENU_OPEN_ID = 100;

    private ControlUtils() {
        // Disable default constructor.
    }

    public static void recursiveSetEnabled(Control ctrl, boolean enabled) {
        if (ctrl instanceof Composite) {
            Composite comp = (Composite) ctrl;
            for (Control c : comp.getChildren()) {
                recursiveSetEnabled(c, enabled);
                c.setEnabled(enabled);
            }
        } else {
            ctrl.setEnabled(enabled);
        }
    }

    public static void recursivelyAddMouseListener(Control ctrl, MouseAdapter mouseAdapter) {
        if (ctrl instanceof Composite) {
            Composite comp = (Composite) ctrl;
            for (Control c : comp.getChildren()) {
                recursivelyAddMouseListener(c, mouseAdapter);
            }
        }
        ctrl.addMouseListener(mouseAdapter);
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

    public static Listener getAutoHideStyledTextScrollbarListener = new Listener() {
        @Override
        public void handleEvent(final Event event) {
            final StyledText t = (StyledText) event.widget;
            final Rectangle r1 = t.getClientArea();
            final Rectangle r2 = t.computeTrim(r1.x, r1.y, r1.width, r1.height);
            final Point p = t.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
            t.getDisplay().timerExec(DELAY_IN_MILLIS, new Runnable() {
                @Override
                public void run() {
                    if (t.isDisposed() || (Boolean.TRUE.equals(t.getData(UPDATING_LAYOUT)))) {
                        return;
                    }
                    t.setRedraw(false);
                    t.setData(UPDATING_LAYOUT, true);

                    try {
                        ScrollBar horizontalBar = t.getHorizontalBar();
                        if (horizontalBar != null) {
                            horizontalBar.setVisible(!t.getWordWrap() && r2.width < p.x);
                        }

                        ScrollBar verticalBar = t.getVerticalBar();
                        if (verticalBar != null) {
                            verticalBar.setVisible(r2.height < p.y);
                        }

                        if (event.type == SWT.Modify) {
                            updateParentLayout(t);
                            t.showSelection();
                        }
                    } finally {
                        t.setData(UPDATING_LAYOUT, false);
                        t.setRedraw(true);
                    }
                }
            });
        }
    };

    private static void updateParentLayout(Control ctrl) {
        Composite parentComposite = ctrl.getParent();
        if (parentComposite != null) {
            parentComposite.layout(true);
        }
    }

    public static void removeOldOpenMenuItem(Menu menu) {
        for (MenuItem item : menu.getItems()) {
            if (item.getID() == MENU_OPEN_ID) {
                item.dispose();
                return;
            }
        }
    }

    public static void createOpenMenuWhenSelectOnlyOne(CMenu menu, FileEntity entity,
            Callable<Boolean> enableWhenItemSelected, SelectionAdapter adapter) {
        MenuItem openMenuItem = menu.createMenuItemWithoutSelectionListener(
                ComposerComponentsImplMessageConstants.MENU_OPEN, null, enableWhenItemSelected, SWT.PUSH);
        openMenuItem.setID(ControlUtils.MENU_OPEN_ID);
        openMenuItem.setText(getFileEntityMenuItemLabel(entity));
        openMenuItem.setData(entity);
        openMenuItem.addSelectionListener(adapter);
    }

    public static void createOpenMenuWhenSelectOnlyOne(final Menu menu, FileEntity entity, final TableViewer viewer,
            SelectionAdapter adapter) {
        MenuItem openMenuItem = new MenuItem(menu, SWT.PUSH);
        openMenuItem.setText(getFileEntityMenuItemLabel(entity));
        openMenuItem.setID(ControlUtils.MENU_OPEN_ID);
        openMenuItem.setData(entity);
        viewer.getTable().addMenuDetectListener(new MenuDetectListener() {

            @Override
            public void menuDetected(MenuDetectEvent e) {
                menu.setEnabled(!viewer.getSelection().isEmpty());

            }
        });
        openMenuItem.addSelectionListener(adapter);
    }

    public static void createSubMenuOpen(Menu subMenu, FileEntity fileEntity, SelectionAdapter selectionAdapter,
            String name) {
        MenuItem menuItem = new MenuItem(subMenu, SWT.PUSH);
        menuItem.setText(name);
        menuItem.setData(fileEntity);
        menuItem.addSelectionListener(selectionAdapter);
    }
    
    private static String getFileEntityMenuItemLabel(FileEntity entity) {
        return ComposerComponentsImplMessageConstants.MENU_OPEN + " " + entity.getName();
    }


}
