package com.kms.katalon.composer.testcase.ast.dialogs;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;

import com.kms.katalon.composer.testcase.constants.StringConstants;
import com.kms.katalon.composer.testcase.groovy.ast.expressions.ExpressionWrapper;
import com.kms.katalon.composer.testcase.groovy.ast.expressions.MapEntryExpressionWrapper;
import com.kms.katalon.composer.testcase.groovy.ast.expressions.MapExpressionWrapper;
import com.kms.katalon.composer.testcase.model.InputValueType;
import com.kms.katalon.composer.testcase.providers.AstInputTypeLabelProvider;
import com.kms.katalon.composer.testcase.providers.AstInputValueLabelProvider;
import com.kms.katalon.composer.testcase.support.AstInputBuilderValueColumnSupport;
import com.kms.katalon.composer.testcase.support.AstInputBuilderValueTypeColumnSupport;

public class MapInputBuilderDialog extends AbstractAstBuilderWithTableDialog {
    private final InputValueType[] defaultInputValueTypes = { InputValueType.String, InputValueType.Number,
            InputValueType.Boolean, InputValueType.Null, InputValueType.Variable, InputValueType.GlobalVariable,
            InputValueType.TestDataValue, InputValueType.MethodCall, InputValueType.Property };

    private MapExpressionWrapper mapExpression;

    public MapInputBuilderDialog(Shell parentShell, MapExpressionWrapper mapExpression) {
        super(parentShell);
        if (mapExpression == null) {
            throw new IllegalArgumentException();
        }
        this.mapExpression = mapExpression.clone();
    }

    protected void createButtonsForButtonBar(Composite parent) {
        Button btnInsert = createButton(parent, 100, StringConstants.DIA_BTN_INSERT, true);
        btnInsert.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int selectionIndex = tableViewer.getTable().getSelectionIndex();
                MapEntryExpressionWrapper mapEntryExpression = new MapEntryExpressionWrapper(mapExpression);
                if (selectionIndex < 0 || selectionIndex >= mapExpression.getMapEntryExpressions().size()) {
                    mapExpression.getMapEntryExpressions().add(mapEntryExpression);
                } else {
                    mapExpression.getMapEntryExpressions().add(selectionIndex, mapEntryExpression);
                }
                tableViewer.refresh();
                tableViewer.getTable().setSelection(selectionIndex + 1);
            }
        });

        Button btnRemove = createButton(parent, 200, StringConstants.DIA_BTN_REMOVE, false);
        btnRemove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = tableViewer.getTable().getSelectionIndex();
                if (index >= 0 && index < mapExpression.getMapEntryExpressions().size()) {
                    mapExpression.getMapEntryExpressions().remove(index);
                    tableViewer.refresh();
                }
            }
        });

        super.createButtonsForButtonBar(parent);
    }

    @Override
    public MapExpressionWrapper getReturnValue() {
        return mapExpression;
    }

    @Override
    public void replaceObject(Object originalObject, Object newObject) {
        if (!(newObject instanceof ExpressionWrapper) || !(originalObject instanceof ExpressionWrapper)) {
            return;
        }
        ExpressionWrapper originalExpression = (ExpressionWrapper) originalObject;
        ExpressionWrapper newExpression = (ExpressionWrapper) newObject;
        for (MapEntryExpressionWrapper mapEntry : mapExpression.getMapEntryExpressions()) {
            if (mapEntry.getKeyExpression() == originalExpression) {
                mapEntry.setKeyExpression(newExpression);
                tableViewer.refresh();
                break;
            } else if (mapEntry.getValueExpression() == originalExpression) {
                mapEntry.setValueExpression(newExpression);
                tableViewer.refresh();
                break;
            }
        }
    }

    @Override
    public String getDialogTitle() {
        return StringConstants.DIA_TITLE_MAP_INPUT;
    }

    @Override
    protected void addTableColumns() {
        TableViewerColumn tableViewerColumnNo = new TableViewerColumn(tableViewer, SWT.NONE);
        TableColumn tblclmnColumnNo = tableViewerColumnNo.getColumn();
        tblclmnColumnNo.setText(StringConstants.DIA_COL_NO);
        tblclmnColumnNo.setWidth(40);
        tableViewerColumnNo.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof MapEntryExpressionWrapper) {
                    return String.valueOf(mapExpression.getMapEntryExpressions().indexOf(element) + 1);
                }
                return StringUtils.EMPTY;
            }
        });

        TableViewerColumn tableViewerColumnKeyType = new TableViewerColumn(tableViewer, SWT.NONE);
        tableViewerColumnKeyType.getColumn().setText(StringConstants.DIA_COL_KEY_TYPE);
        tableViewerColumnKeyType.getColumn().setWidth(100);
        tableViewerColumnKeyType.setLabelProvider(new AstInputTypeLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof MapEntryExpressionWrapper) {
                    return super.getText(((MapEntryExpressionWrapper) element).getKeyExpression());
                }
                return StringUtils.EMPTY;
            }
        });

        tableViewerColumnKeyType.setEditingSupport(new AstInputBuilderValueTypeColumnSupport(tableViewer,
                defaultInputValueTypes, this) {
            @Override
            protected void setValue(Object element, Object value) {
                super.setValue(((MapEntryExpressionWrapper) element).getKeyExpression(), value);
            }

            @Override
            protected Object getValue(Object element) {
                return super.getValue(((MapEntryExpressionWrapper) element).getKeyExpression());
            }

            @Override
            protected boolean canEdit(Object element) {
                if (element instanceof MapEntryExpressionWrapper) {
                    return super.canEdit(((MapEntryExpressionWrapper) element).getKeyExpression());
                }
                return false;
            }

            @Override
            protected CellEditor getCellEditor(Object element) {
                return super.getCellEditor(((MapEntryExpressionWrapper) element).getKeyExpression());
            }
        });

        TableViewerColumn tableViewerColumnKeyValue = new TableViewerColumn(tableViewer, SWT.NONE);
        TableColumn tblclmnNewColumnKeyValue = tableViewerColumnKeyValue.getColumn();
        tblclmnNewColumnKeyValue.setText(StringConstants.DIA_COL_KEY);
        tblclmnNewColumnKeyValue.setWidth(170);
        tableViewerColumnKeyValue.setLabelProvider(new AstInputValueLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof MapEntryExpressionWrapper) {
                    return super.getText(((MapEntryExpressionWrapper) element).getKeyExpression());
                }
                return StringUtils.EMPTY;
            }
        });

        tableViewerColumnKeyValue.setEditingSupport(new AstInputBuilderValueColumnSupport(tableViewer, this) {
            @Override
            protected boolean canEdit(Object element) {
                if (element instanceof MapEntryExpressionWrapper) {
                    return super.canEdit(((MapEntryExpressionWrapper) element).getKeyExpression());
                }
                return false;
            }

            @Override
            protected void setValue(Object element, Object value) {
                super.setValue(((MapEntryExpressionWrapper) element).getKeyExpression(), value);
            }

            @Override
            protected Object getValue(Object element) {
                return super.getValue(((MapEntryExpressionWrapper) element).getKeyExpression());
            }

            @Override
            protected CellEditor getCellEditor(Object element) {
                return super.getCellEditor(((MapEntryExpressionWrapper) element).getKeyExpression());
            }
        });

        TableViewerColumn tableViewerColumnValueType = new TableViewerColumn(tableViewer, SWT.NONE);
        tableViewerColumnValueType.getColumn().setText(StringConstants.DIA_COL_VALUE_TYPE);
        tableViewerColumnValueType.getColumn().setWidth(100);
        tableViewerColumnValueType.setLabelProvider(new AstInputTypeLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof MapEntryExpressionWrapper) {
                    return super.getText(((MapEntryExpressionWrapper) element).getValueExpression());
                }
                return StringUtils.EMPTY;
            }
        });

        tableViewerColumnValueType.setEditingSupport(new AstInputBuilderValueTypeColumnSupport(tableViewer,
                defaultInputValueTypes, this) {
            @Override
            protected void setValue(Object element, Object value) {
                super.setValue(((MapEntryExpressionWrapper) element).getValueExpression(), value);
            }

            @Override
            protected Object getValue(Object element) {
                return super.getValue(((MapEntryExpressionWrapper) element).getValueExpression());
            }

            @Override
            protected boolean canEdit(Object element) {
                if (element instanceof MapEntryExpressionWrapper) {
                    return super.canEdit(((MapEntryExpressionWrapper) element).getValueExpression());
                }
                return false;
            }

            @Override
            protected CellEditor getCellEditor(Object element) {
                return super.getCellEditor(((MapEntryExpressionWrapper) element).getValueExpression());
            }
        });

        TableViewerColumn tableViewerColumnValue = new TableViewerColumn(tableViewer, SWT.NONE);
        TableColumn tblclmnNewColumnValue = tableViewerColumnValue.getColumn();
        tblclmnNewColumnValue.setText(StringConstants.DIA_COL_VALUE);
        tblclmnNewColumnValue.setWidth(170);
        tableViewerColumnValue.setLabelProvider(new AstInputValueLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof MapEntryExpressionWrapper) {
                    return super.getText(((MapEntryExpressionWrapper) element).getValueExpression());
                }
                return StringUtils.EMPTY;
            }
        });

        tableViewerColumnValue.setEditingSupport(new AstInputBuilderValueColumnSupport(tableViewer, this) {
            @Override
            protected void setValue(Object element, Object value) {
                super.setValue(((MapEntryExpressionWrapper) element).getValueExpression(), value);
            }

            @Override
            protected Object getValue(Object element) {
                return super.getValue(((MapEntryExpressionWrapper) element).getValueExpression());
            }

            @Override
            protected boolean canEdit(Object element) {
                if (element instanceof MapEntryExpressionWrapper) {
                    return super.canEdit(((MapEntryExpressionWrapper) element).getValueExpression());
                }
                return false;
            }

            @Override
            protected CellEditor getCellEditor(Object element) {
                return super.getCellEditor(((MapEntryExpressionWrapper) element).getValueExpression());
            }
        });
    }

    @Override
    public void refresh() {
        tableViewer.setContentProvider(new ArrayContentProvider());
        tableViewer.setInput(mapExpression.getMapEntryExpressions());
        tableViewer.refresh();
    }
}
