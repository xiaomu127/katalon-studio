package com.kms.katalon.composer.execution.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogRecord;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.kms.katalon.composer.components.util.ColorUtil;
import com.kms.katalon.preferences.internal.ScopedPreferenceStore;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.constants.PreferenceConstants;
import com.kms.katalon.core.logging.LogLevel;
import com.kms.katalon.core.logging.XmlLogRecord;

public class LogTableViewer extends TableViewer {

    private static final int DEPTH_OF_MAIN_TEST_CASE = 0;

    private List<XmlLogRecord> records;
    private int logDepth;
    private IEventBroker eventBroker;
    private IPreferenceStore store;

    //Represents the latest test case's result record, used to update progress bar of table viewer 
    private LogRecord latestResultRecord;

    public LogTableViewer(Composite parent, int style, IEventBroker eventBroker) {
        super(parent, style);
        this.eventBroker = eventBroker;
        this.setContentProvider(new ArrayContentProvider());
        store = new ScopedPreferenceStore(InstanceScope.INSTANCE,
                PreferenceConstants.ExecutionPreferenceConstans.QUALIFIER);
        clearAll();
    }

    public void clearAll() {
        records = new ArrayList<XmlLogRecord>();
        super.setInput(records);
        logDepth = DEPTH_OF_MAIN_TEST_CASE;
        latestResultRecord = null;
    }

    @Override
    public void add(Object object) {
        if (object != null && object instanceof XmlLogRecord) {
            XmlLogRecord record = (XmlLogRecord) object;
            records.add(record);
            super.add(record);

            LogLevel logLevel = (LogLevel) record.getLevel();
            if (logLevel.equals(LogLevel.END)) {
                logDepth--;
                if (record.getSourceMethodName().equals(
                        com.kms.katalon.core.constants.StringConstants.LOG_END_TEST_METHOD)
                        && logDepth == DEPTH_OF_MAIN_TEST_CASE) {
                    eventBroker.send(EventConstants.CONSOLE_LOG_UPDATE_PROGRESS_BAR, latestResultRecord);
                }
            } else if (logLevel.equals(LogLevel.START)) {
                String startName = record.getSourceMethodName();
                if (!startName.equals(com.kms.katalon.core.constants.StringConstants.LOG_START_SUITE_METHOD)) {
                    logDepth++;
                }
            } else if (LogLevel.getResultLogs().contains(logLevel) && logDepth == DEPTH_OF_MAIN_TEST_CASE + 1) {
                latestResultRecord = record;
            }

            updateTableBackgroundColor();
        }
    }

    @Override
    public void refresh() {
        super.refresh();
        super.setInput(records);
        updateTableBackgroundColor();
    }

    private void updateTableBackgroundColor() {
        Table table = this.getTable();
        for (TableItem item : table.getItems()) {
            XmlLogRecord record = (XmlLogRecord) item.getData();
            if (record.getLevel().equals(LogLevel.PASSED)) {
                item.setBackground(ColorUtil.getPassedLogBackgroundColor());
                item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
            } else if (record.getLevel().equals(LogLevel.FAILED)) {
                item.setBackground(ColorUtil.getFailedLogBackgroundColor());
                item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
            } else if (record.getLevel().equals(LogLevel.ERROR) || record.getLevel().equals(LogLevel.WARNING)) {
                item.setBackground(ColorUtil.getWarningLogBackgroundColor());
                item.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
            }
        }

        if (isScrollLogEnable()) {
            int lastItemIndex = table.getItemCount() - 1;
            if (lastItemIndex >= 0) {
                table.showItem(table.getItem(lastItemIndex));
            }
        }
    }

    private boolean isScrollLogEnable() {
        return !store.getBoolean(PreferenceConstants.ExecutionPreferenceConstans.EXECUTION_PIN_LOG);
    }

    public List<XmlLogRecord> getRecords() {
        return records;
    }
}
