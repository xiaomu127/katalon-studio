package com.kms.katalon.composer.project.handlers;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.MessageDialog;

import com.kms.katalon.composer.project.constants.StringConstants;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.execution.launcher.manager.LauncherManager;
import com.kms.katalon.feature.FeatureServiceConsumer;
import com.kms.katalon.feature.IFeatureService;
import com.kms.katalon.feature.KSEFeature;
import com.kms.katalon.groovy.util.GroovyUtil;

public class RebuildProjectHandler {
    
    private IFeatureService featureService = FeatureServiceConsumer.getServiceInstance();

    @CanExecute
    public boolean canExecute() {
        return (ProjectController.getInstance().getCurrentProject() != null)
                && !LauncherManager.getInstance().isAnyLauncherRunning();
    }

    @Execute
    public void execute() {
        try {
            Job job = new Job(StringConstants.HAND_REBUILD_PROJ) {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        monitor.beginTask(StringConstants.HAND_REBUILDING_PROJ, 10);
                        SubMonitor progress = SubMonitor.convert(monitor, 10);
                        ProjectController projectController = ProjectController.getInstance();
                        ProjectEntity currentProject = projectController.getCurrentProject();
                        boolean allowSourceAttachment = featureService.canUse(KSEFeature.SOURCE_CODE_FOR_DEBUGGING);
                        GroovyUtil.initGroovyProjectClassPath(currentProject,
                                projectController.getCustomKeywordPlugins(currentProject), false,
                                allowSourceAttachment,
                                progress.newChild(10));
                        return Status.OK_STATUS;
                    } catch (Exception e) {
                        return Status.CANCEL_STATUS;
                    } finally {
                        monitor.done();
                    }
                }
            };
            job.setUser(true);
            job.schedule();
        } catch (Exception e) {
            MessageDialog.openError(null, StringConstants.ERROR_TITLE,
                    StringConstants.HAND_ERROR_MSG_UNABLE_TO_REBUILD_PROJ);
        }
    }
}
