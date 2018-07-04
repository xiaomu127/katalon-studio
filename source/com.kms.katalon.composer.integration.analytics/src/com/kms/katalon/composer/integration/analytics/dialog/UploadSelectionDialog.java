package com.kms.katalon.composer.integration.analytics.dialog;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.kms.katalon.composer.components.impl.dialogs.MultiStatusErrorDialog;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.integration.analytics.constants.ComposerAnalyticsStringConstants;
import com.kms.katalon.composer.integration.analytics.constants.ComposerIntegrationAnalyticsMessageConstants;
import com.kms.katalon.composer.testcase.constants.StringConstants;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.integration.analytics.entity.AnalyticsProject;
import com.kms.katalon.integration.analytics.entity.AnalyticsTeam;
import com.kms.katalon.integration.analytics.entity.AnalyticsTokenInfo;
import com.kms.katalon.integration.analytics.exceptions.AnalyticsApiExeception;
import com.kms.katalon.integration.analytics.providers.AnalyticsApiProvider;
import com.kms.katalon.integration.analytics.setting.AnalyticsSettingStore;

public class UploadSelectionDialog extends Dialog {
    public static final int UPLOAD_ID = 2;
    public static final int CANCEL_ID = 3;

    private Button btnUpload;
    private Button btnCancel;
    private Button btnCreate;
    private Combo cbbProjects;
    private Combo cbbTeams;

    private List<AnalyticsTeam> teams;
    private List<AnalyticsProject> projects;
    private AnalyticsSettingStore analyticsSettingStore;

    public UploadSelectionDialog(Shell parentShell, List<AnalyticsTeam> teams) {
        super(parentShell);
        analyticsSettingStore = new AnalyticsSettingStore(
                ProjectController.getInstance().getCurrentProject().getFolderLocation());
        this.teams = teams;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite body = new Composite(parent, SWT.NONE);
        GridData bodyGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        bodyGridData.widthHint = 400;
        body.setLayoutData(bodyGridData);
        GridLayout bodyGridLayout = new GridLayout(1, false);
        bodyGridLayout.marginWidth = 10;
        bodyGridLayout.marginHeight = 10;
        body.setLayout(bodyGridLayout);

        CLabel lblInformation = new CLabel(body, SWT.WRAP);
        lblInformation.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, true, false, 1, 1));
        lblInformation.setBottomMargin(2);
        lblInformation.setText(StringConstants.LBL_SELECTION_INFORMATION);

        Label lblTeam = new Label(body, SWT.NONE);
        lblTeam.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_TEAM);

        cbbTeams = new Combo(body, SWT.READ_ONLY);
        cbbTeams.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1));

        Label lblProject = new Label(body, SWT.NONE);
        lblProject.setText(ComposerIntegrationAnalyticsMessageConstants.LBL_PROJECT);

        Composite projectComposite = new Composite(body, SWT.NONE);
        projectComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout projectGridLayout = new GridLayout(2, false);
        projectGridLayout.marginWidth = 0;
        projectGridLayout.marginHeight = 0;
        projectComposite.setLayout(projectGridLayout);
        cbbProjects = new Combo(projectComposite, SWT.READ_ONLY);
        cbbProjects.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        btnCreate = new Button(projectComposite, SWT.NONE);
        btnCreate.setText("New Project");
        if (teams.get(getDefaultTeamIndex()).getRole().equals("USER")){
            btnCreate.setEnabled(false);
        }

        fillData();

        return body;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        btnUpload = createButton(parent, UPLOAD_ID, StringConstants.BTN_UPLOAD, true);
        btnCancel = createButton(parent, CANCEL_ID, StringConstants.BTN_CANCEL, false);
        addControlListeners();
    }

    private void addControlListeners() {

        btnUpload.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                handleUpload();
            }
        });

        btnCreate.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                HashMap<String, String> results = getInfo();
                AnalyticsTeam team = null;

                if (teams != null && teams.size() > 0) {
                    team = teams.get(getDefaultTeamIndex());
                }
                
                NewProjectDialog dialog = new NewProjectDialog(btnCreate.getDisplay().getActiveShell(), results.get("serverUrl"),
                        results.get("email"), results.get("password"), team);
                if (dialog.open() == Dialog.OK) {
                    AnalyticsProject createdProject = dialog.getAnalyticsProject();
                    if (createdProject != null) {
                        try {
                            analyticsSettingStore.setProject(createdProject);
                            analyticsSettingStore.setTeam(team);
                            if (projects == null) {
                                return;
                            }
                            cbbProjects.setItems(getProjectNames(projects).toArray(new String[projects.size()]));
                            cbbProjects.select(getDefaultProjectIndex());
                        } catch (IOException ex) {
                            LoggerSingleton.logError(ex);
                            MultiStatusErrorDialog.showErrorDialog(ex, ComposerAnalyticsStringConstants.ERROR,
                                    ex.getMessage());
                        }
                    }
                }

                fillData();
            }
        });
        
        btnCancel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                cancelPressed();
            }
        });
        
        cbbTeams.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                cbbProjects.setItems();
                if (teams.get(cbbTeams.getSelectionIndex()).getRole().equals("USER")){
                    btnCreate.setEnabled(false);
                } else {
                    btnCreate.setEnabled(true);
                }
                try {
                    analyticsSettingStore.setTeam(teams.get(cbbTeams.getSelectionIndex()));
                    setProjectsBasedOnTeam(teams);
                } catch (IOException ex) {
                    LoggerSingleton.logError(ex);
                    MultiStatusErrorDialog.showErrorDialog(ex, ComposerAnalyticsStringConstants.ERROR, ex.getMessage());
                }
            }
        });
    }

    private List<AnalyticsProject> getProjects(final String serverUrl, final String email, final String password,
            final AnalyticsTeam team, final boolean isUpdateStatus) {
        final List<AnalyticsProject> projects = new ArrayList<>();
        try {
            new ProgressMonitorDialog(getShell()).run(true, false, new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                    try {
                        monitor.beginTask(ComposerIntegrationAnalyticsMessageConstants.MSG_DLG_PRG_RETRIEVING_PROJECTS,
                                2);
                        monitor.subTask(ComposerIntegrationAnalyticsMessageConstants.MSG_DLG_PRG_CONNECTING_TO_SERVER);
                        final AnalyticsTokenInfo tokenInfo = AnalyticsApiProvider.requestToken(serverUrl, email,
                                password);
                        monitor.worked(1);
                        monitor.subTask(ComposerIntegrationAnalyticsMessageConstants.MSG_DLG_PRG_GETTING_PROJECTS);
                        final List<AnalyticsProject> loaded = AnalyticsApiProvider.getProjects(serverUrl, team,
                                tokenInfo.getAccess_token());
                        if (loaded != null && !loaded.isEmpty()) {
                            projects.addAll(loaded);
                        }
                        monitor.worked(1);
                    } catch (Exception e) {
                        throw new InvocationTargetException(e);
                    } finally {
                        monitor.done();
                    }
                }
            });
            return projects;
        } catch (InvocationTargetException exception) {
            final Throwable cause = exception.getCause();
            if (cause instanceof AnalyticsApiExeception) {
                MessageDialog.openError(getShell(), ComposerAnalyticsStringConstants.ERROR, cause.getMessage());
            } else {
                LoggerSingleton.logError(cause);
            }
        } catch (InterruptedException e) {
            // Ignore this
        }
        return null;
    }

    private void setProjectsBasedOnTeam(List<AnalyticsTeam> teams) {
        AnalyticsTeam team = teams.get(getDefaultTeamIndex());
        HashMap<String, String> info = getInfo();
        projects = getProjects(info.get("serverUrl"), info.get("email"), info.get("password"), team, false);
        if (projects != null && !projects.isEmpty()) {
            cbbProjects.setItems(getProjectNames(projects).toArray(new String[projects.size()]));
            cbbProjects.select(getDefaultProjectIndex());
        }
    }

    private HashMap<String, String> getInfo() {
        HashMap<String, String> results = new HashMap<String, String>();
        try {
            results.put("serverUrl", analyticsSettingStore.getServerEndpoint(true));
            results.put("email", analyticsSettingStore.getEmail(true));
            results.put("password", analyticsSettingStore.getPassword(true));
        } catch (IOException | GeneralSecurityException e) {
            e.printStackTrace();
            return null;
        }
        return results;
    }

    private void fillData() {
        try {
            cbbTeams.setItems(getTeamNames(teams).toArray(new String[teams.size()]));
            cbbTeams.select(getDefaultTeamIndex());
            setProjectsBasedOnTeam(teams);
        } catch (Exception e) {
            LoggerSingleton.logError(e);
            MultiStatusErrorDialog.showErrorDialog(e, ComposerAnalyticsStringConstants.ERROR, e.getMessage());
        }
    }

    private List<String> getProjectNames(List<AnalyticsProject> projects) {
        List<String> names = new ArrayList<>();
        projects.forEach(p -> names.add(p.getName()));
        return names;
    }

    private List<String> getTeamNames(List<AnalyticsTeam> projects) {
        List<String> names = new ArrayList<>();
        projects.forEach(p -> names.add(p.getName()));
        return names;
    }

    private int getDefaultProjectIndex() {
        int selectionIndex = 0;

        try {
            AnalyticsProject storedProject = analyticsSettingStore.getProject();
            if (storedProject != null && storedProject.getId() != null) {
                for (int i = 0; i < projects.size(); i++) {
                    AnalyticsProject p = projects.get(i);
                    if (p.getId() == storedProject.getId()) {
                        selectionIndex = i;
                    }
                }
            }
        } catch (IOException e) {
            // do nothing
        }
        return selectionIndex;
    }

    private int getDefaultTeamIndex() {
        int selectionIndex = 0;

        try {
            AnalyticsTeam storedProject = analyticsSettingStore.getTeam();
            if (storedProject != null && storedProject.getId() != null && teams != null) {
                for (int i = 0; i < teams.size(); i++) {
                    AnalyticsTeam p = teams.get(i);
                    if (p.getId() == storedProject.getId()) {
                        selectionIndex = i;
                    }
                }
            }
        } catch (IOException e) {
            // do nothing
        }
        return selectionIndex;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(StringConstants.SELECTION_DIALOG_TITLE);
    }

    private void closeDialog() {
        this.close();
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    private void handleUpload() {
        try {
            AnalyticsTeam team = null;
            if (teams != null && teams.size() > 0) {
                team = teams.get(getDefaultTeamIndex());
            }
            analyticsSettingStore.setTeam(team);
            analyticsSettingStore.setProject(
                    cbbProjects.getSelectionIndex() != -1 ? projects.get(cbbProjects.getSelectionIndex()) : null);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        setReturnCode(UPLOAD_ID);
        closeDialog();
    }

}
