package com.kms.katalon.composer.testcase.ast.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import com.kms.katalon.composer.testcase.constants.StringConstants;
import com.kms.katalon.composer.testcase.groovy.ast.expressions.BooleanExpressionWrapper;
import com.kms.katalon.composer.testcase.groovy.ast.expressions.ExpressionWrapper;
import com.kms.katalon.composer.testcase.model.InputValueType;
import com.kms.katalon.composer.testcase.providers.AstInputTypeLabelProvider;
import com.kms.katalon.composer.testcase.providers.AstInputValueLabelProvider;
import com.kms.katalon.composer.testcase.support.AstInputBuilderValueColumnSupport;
import com.kms.katalon.composer.testcase.support.AstInputBuilderValueTypeColumnSupport;

public class BooleanBuilderDialog extends AbstractAstBuilderWithTableDialog {
    private final InputValueType[] defaultInputValueTypes = { InputValueType.Boolean, InputValueType.Variable,
            InputValueType.GlobalVariable, InputValueType.TestDataValue, InputValueType.MethodCall,
            InputValueType.Condition, InputValueType.Binary, InputValueType.Property };

    private BooleanExpressionWrapper booleanExpression;
    private Button btnReverse;

    public BooleanBuilderDialog(Shell parentShell, BooleanExpressionWrapper booleanExpression) {
        super(parentShell);
        if (booleanExpression == null) {
            throw new IllegalArgumentException();
        }
        this.booleanExpression = booleanExpression.clone();
    }

    @Override
    protected TableViewer createTable(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        btnReverse = new Button(parent, SWT.CHECK);
        btnReverse.setText(StringConstants.DIA_BTN_REVERSE);
        btnReverse.setSelection(booleanExpression.isReverse());
        btnReverse.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                booleanExpression.setReverse(btnReverse.getSelection());
                refresh();
            }
        });

        TableViewer tableViewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
        Table table = tableViewer.getTable();
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        return tableViewer;
    }

    @Override
    public void refresh() {
        List<ExpressionWrapper> expressionList = new ArrayList<ExpressionWrapper>();
        expressionList.add(booleanExpression.getExpression());
        tableViewer.setContentProvider(new ArrayContentProvider());
        tableViewer.setInput(expressionList);
    }

    @Override
    public BooleanExpressionWrapper getReturnValue() {
        return booleanExpression;
    }

    @Override
    public void replaceObject(Object orginalObject, Object newObject) {
        if (!(newObject instanceof ExpressionWrapper) || orginalObject != booleanExpression.getExpression()) {
            return;
        }
        booleanExpression.setExpression((ExpressionWrapper) newObject);
        refresh();
    }

    @Override
    public String getDialogTitle() {
        return StringConstants.DIA_TITLE_CONDITION_INPUT;
    }

    @Override
    protected void addTableColumns() {
        TableViewerColumn tableViewerColumnValueType = new TableViewerColumn(tableViewer, SWT.NONE);
        tableViewerColumnValueType.getColumn().setText(StringConstants.DIA_COL_VALUE_TYPE);
        tableViewerColumnValueType.getColumn().setWidth(100);
        tableViewerColumnValueType.setLabelProvider(new AstInputTypeLabelProvider());
        tableViewerColumnValueType.setEditingSupport(new AstInputBuilderValueTypeColumnSupport(tableViewer,
                defaultInputValueTypes, this));

        TableViewerColumn tableViewerColumnValue = new TableViewerColumn(tableViewer, SWT.NONE);
        tableViewerColumnValue.getColumn().setText(StringConstants.DIA_COL_VALUE);
        tableViewerColumnValue.getColumn().setWidth(300);
        tableViewerColumnValue.setLabelProvider(new AstInputValueLabelProvider());
        tableViewerColumnValue.setEditingSupport(new AstInputBuilderValueColumnSupport(tableViewer, this));
    }
}
