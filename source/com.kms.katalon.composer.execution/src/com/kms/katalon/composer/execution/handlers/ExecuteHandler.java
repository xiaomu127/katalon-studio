package com.kms.katalon.composer.execution.handlers;

import java.io.IOException;
import java.text.MessageFormat;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.contributions.IContributionFactory;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.menu.MDynamicMenuContribution;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledToolItem;
import org.eclipse.e4.ui.model.application.ui.menu.MMenu;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.event.Event;

import com.kms.katalon.composer.components.impl.event.EventServiceAdapter;
import com.kms.katalon.composer.components.log.LoggerSingleton;
import com.kms.katalon.composer.execution.constants.StringConstants;
import com.kms.katalon.composer.execution.menu.AbstractExecutionMenuContribution;
import com.kms.katalon.composer.execution.menu.ExecutionHandledMenuItem;
import com.kms.katalon.composer.execution.menu.ExistingExecutionHandledMenuItem;
import com.kms.katalon.constants.EventConstants;
import com.kms.katalon.constants.IdConstants;
import com.kms.katalon.controller.ProjectController;
import com.kms.katalon.core.event.EventBusSingleton;
import com.kms.katalon.entity.project.ProjectEntity;
import com.kms.katalon.entity.project.ProjectType;
import com.kms.katalon.execution.configuration.IRunConfiguration;
import com.kms.katalon.execution.configuration.contributor.IRunConfigurationContributor;
import com.kms.katalon.execution.event.ExecutionEvent;
import com.kms.katalon.execution.launcher.model.LaunchMode;
import com.kms.katalon.execution.util.ExecutionUtil;

@SuppressWarnings("restriction")
public class ExecuteHandler extends AbstractExecutionHandler {
    private static final String TEMP_ID = "tempId";

    @Inject
    private IContributionFactory contributionFactory;
    
    @Override
    protected IRunConfiguration getRunConfigurationForExecution(String projectDir) throws IOException {
        return null;
    }
    
    @Execute
    public void execute(MHandledToolItem toolItem) {
        try {
            IRunConfigurationContributor defaultRunContributor = ExecutionUtil.getDefaultExecutionConfiguration();
            if (defaultRunContributor == null) {
                return;
            }
            ExecutionHandledMenuItem defaultMenuItem = findDefaultMenuItem(toolItem.getMenu());
            if (defaultMenuItem == null) {
                return;
            }
            handlerService.executeHandler(defaultMenuItem.getParameterizedCommandFromMenuItem(commandService));
        } catch (Exception e) {
            MessageDialog.openError(Display.getCurrent().getActiveShell(), StringConstants.ERROR, MessageFormat
                    .format(StringConstants.HAND_ERROR_MSG_UNABLE_TO_EXECUTE_TEST_SCRIPT_ROOT_CAUSE, e.getMessage()));
            LoggerSingleton.logError(e);
        }
    }

    private ExecutionHandledMenuItem findDefaultMenuItem(MMenu parentMenu) {
        for (MMenuElement menuItem : parentMenu.getChildren()) {
            if (menuItem instanceof ExecutionHandledMenuItem
                    && !(menuItem instanceof ExistingExecutionHandledMenuItem)) {
                ExecutionHandledMenuItem handledMenuItem = (ExecutionHandledMenuItem) menuItem;
                if (handledMenuItem.isDefault() && handledMenuItem.getCommand() != null) {
                    return handledMenuItem;
                }
            }
            if (menuItem instanceof MMenu) {
                ExecutionHandledMenuItem result = findDefaultMenuItem((MMenu) menuItem);
                if (result != null) {
                    return result;
                }
            }
            if (menuItem instanceof MDynamicMenuContribution) {
                MDynamicMenuContribution dynamicMenuContribution = (MDynamicMenuContribution) menuItem;
                Object contribution = dynamicMenuContribution.getObject();
                if (contribution == null) {
                    IEclipseContext context = modelService.getContainingContext(parentMenu);
                    contribution = contributionFactory.create(dynamicMenuContribution.getContributionURI(), context);
                }
                if (contribution instanceof AbstractExecutionMenuContribution) {
                    ExecutionHandledMenuItem contributionDefaultMenuItem = ((AbstractExecutionMenuContribution) contribution)
                            .createDefaultMenuItem();
                    if (contributionDefaultMenuItem.isDefault()) {
                        contributionDefaultMenuItem.setElementId(TEMP_ID + System.currentTimeMillis());
                        return contributionDefaultMenuItem;
                    }
                }

            }
        }
        return null;
    }
}
