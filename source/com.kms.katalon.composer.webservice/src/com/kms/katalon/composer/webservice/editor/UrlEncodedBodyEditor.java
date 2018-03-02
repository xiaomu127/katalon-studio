package com.kms.katalon.composer.webservice.editor;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;

import com.google.gson.reflect.TypeToken;
import com.kms.katalon.core.util.internal.JsonUtil;
import com.kms.katalon.entity.webservice.BodyContent;
import com.kms.katalon.entity.webservice.UrlEncodedBodyParameter;

public class UrlEncodedBodyEditor extends AbstractFormBodyEditor<UrlEncodedBodyParameter> {
    
    private static final String TABLE_COLUMN_KEY = "Key";
    
    private static final String TABLE_COLUMN_VALUE = "Value";
    
    private static final String DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded";
    
    private static final String DEFAULT_CHARSET = "UTF-8";
    
    private TableColumn cKey, cValue;
    
    public UrlEncodedBodyEditor(Composite parent, int style) {
        super(parent, style);
    }

    @Override
    protected ParameterTable doCreateParameterTable(Composite parent) {
        TableColumnLayout tableColumnLayout = new TableColumnLayout();
        Composite tableComposite = new Composite(parent, SWT.NONE);
        GridData gdData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        gdData.heightHint = 150;
        tableComposite.setLayoutData(gdData);
        tableComposite.setLayout(tableColumnLayout);
        
        ParameterTable tvParams = new ParameterTable(tableComposite,
                SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        tvParams.setContentProvider(ArrayContentProvider.getInstance());
        Table tParams = tvParams.getTable();
        tParams.setHeaderVisible(true);
        tParams.setLinesVisible(true);
        
        TableViewerColumn cvKey = new TableViewerColumn(tvParams, SWT.LEFT);
        cKey = cvKey.getColumn();
        cKey.setText(TABLE_COLUMN_KEY);
        cvKey.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((UrlEncodedBodyParameter) element).getName();
            }
        });
        cvKey.setEditingSupport(new EditingSupport(cvKey.getViewer()) {
            @Override
            protected void setValue(Object element, Object value) {
                ((UrlEncodedBodyParameter) element).setName(String.valueOf(value));
                tvParams.update(element, null);
                fireModifyEvent();
            }

            @Override
            protected CellEditor getCellEditor(Object element) {
                return new TextCellEditor(tParams);
            }

            @Override
            protected boolean canEdit(Object element) {
                return true;
            }

            @Override
            protected Object getValue(Object element) {
                return ((UrlEncodedBodyParameter) element).getName();
            }
        });
        
        TableViewerColumn cvValue = new TableViewerColumn(tvParams, SWT.LEFT);
        cValue = cvValue.getColumn();
        cValue.setText(TABLE_COLUMN_VALUE);
        cvValue.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((UrlEncodedBodyParameter) element).getValue();
            }
        });
        cvValue.setEditingSupport(new EditingSupport(cvValue.getViewer()) {
            @Override
            protected void setValue(Object element, Object value) {
                ((UrlEncodedBodyParameter) element).setValue(String.valueOf(value));
                tvParams.update(element, null);
                fireModifyEvent();
            }

            @Override
            protected CellEditor getCellEditor(Object element) {
                return new TextCellEditor(tParams);
            }

            @Override
            protected boolean canEdit(Object element) {
                return true;
            }

            @Override
            protected Object getValue(Object element) {
                return ((UrlEncodedBodyParameter) element).getValue();
            }
        });
        
        tableColumnLayout.setColumnData(cKey, new ColumnWeightData(50, 100));
        tableColumnLayout.setColumnData(cValue, new ColumnWeightData(50, 100));
        
        return tvParams;
    }

    @Override
    protected UrlEncodedBodyParameter createEmptyParameter() {
        UrlEncodedBodyParameter param = new UrlEncodedBodyParameter();
        param.setName(StringUtils.EMPTY);
        param.setValue(StringUtils.EMPTY);
        return param;
    }

    @Override
    protected boolean checkEmptyParameter(UrlEncodedBodyParameter parameter) {
        return StringUtils.isBlank(parameter.getName()) 
                && StringUtils.isBlank(parameter.getValue());
    }

    @Override
    public void setInput(String httpBodyContent) {
        if (StringUtils.isEmpty(httpBodyContent)) {
            bodyContent = new BodyContent<UrlEncodedBodyParameter>();
            bodyContent.setContentType(DEFAULT_CONTENT_TYPE);
            bodyContent.setCharset(DEFAULT_CHARSET);
        } else {
            bodyContent = JsonUtil.fromJson(httpBodyContent, 
                    new TypeToken<BodyContent<UrlEncodedBodyParameter>>(){}.getType());
        }
        
        tvParams.setInput(bodyContent.getParameters());
        if (!bodyContent.getParameters().isEmpty()) {
            btnRemove.setEnabled(true);
        }
    }
}
