/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.cases.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IServiceLocator;
import org.robotframework.ide.eclipse.main.plugin.model.IRobotCodeHoldingElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCase;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordCall;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.RobotFormEditor;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.handler.RunTestDynamicMenuItem;

public class RunTestFromTableDynamicMenuItem extends RunTestDynamicMenuItem {

    private static final String RUN_TEST_COMMAND_ID = "org.robotframework.red.runSelectedTestsFromTable";

    static final String RUN_TEST_COMMAND_MODE_PARAMETER = RUN_TEST_COMMAND_ID + ".mode";

    private final String id;

    public RunTestFromTableDynamicMenuItem() {
        this("org.robotframework.red.menu.dynamic.table.run");
    }

    public RunTestFromTableDynamicMenuItem(final String id) {
        this.id = id;
    }
    
    @Override
    protected IContributionItem[] getContributionItems() {
        final IWorkbenchWindow activeWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (!(activeWindow.getActivePage().getActiveEditor() instanceof RobotFormEditor)) {
            return new IContributionItem[0];
        }
        final List<IContributionItem> contributedItems = new ArrayList<>();
        final ISelection selection = activeWindow.getSelectionService().getSelection();
        if (selection instanceof StructuredSelection && !selection.isEmpty()) {
            final StructuredSelection structuredSelection = (StructuredSelection) selection;

            final Set<RobotCase> firstCases = findFirstCases(structuredSelection);
            if (firstCases.size() == 1) {
                contributeBefore(contributedItems);
                contributedItems.add(createCurrentCaseItem(activeWindow, firstCases.toArray(new RobotCase[1])[0]));
            } else if (firstCases.size() > 1) {
                contributeBefore(contributedItems);
                contributedItems.add(createCurrentCaseItem(activeWindow, null));
            }
        }
        return contributedItems.toArray(new IContributionItem[0]);
    }

    private Set<RobotCase> findFirstCases(final IStructuredSelection selection) {
        final Set<RobotCase> firstCases = new HashSet<RobotCase>();
        for (final Object o : selection.toList()) {
            RobotCase testCase = null;
            if (o instanceof RobotKeywordCall) {
                // workaround for possible RobotSettingsSection type, should be changed
                final IRobotCodeHoldingElement parent = ((RobotKeywordCall) o).getParent();
                if (parent instanceof RobotCase) {
                    testCase = (RobotCase) parent;
                }
            } else if (o instanceof RobotCase) {
                testCase = (RobotCase) o;
            }
            if (testCase != null) {
                firstCases.add(testCase);
                if (firstCases.size() > 1) {
                    // This method should never return set bigger than 2 elements
                    break;
                }
            }
        }
        return firstCases;
    }

    @Override
    protected IContributionItem createCurrentCaseItem(final IServiceLocator serviceLocator, final RobotCase testCase) {
        final CommandContributionItemParameter contributionParameters = new CommandContributionItemParameter(
                serviceLocator, id, RUN_TEST_COMMAND_ID, SWT.PUSH);
        contributionParameters.label = getModeName()
                + (testCase == null ? " selected tests" : " test: '" + testCase.getName() + "'");
        contributionParameters.icon = getImageDescriptor();
        final HashMap<String, String> parameters = new HashMap<>();
        parameters.put(RUN_TEST_COMMAND_MODE_PARAMETER, getModeName().toUpperCase());
        contributionParameters.parameters = parameters;
        return new CommandContributionItem(contributionParameters);
    }

}