/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor;

import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PlatformUI;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCase;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCodeHoldingElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotElementChange;
import org.robotframework.ide.eclipse.main.plugin.model.RobotElementChange.Kind;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordCall;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordDefinition;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModelEvents;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSetting;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSetting.SettingsGroup;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSettingsSection;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.model.RobotVariable;
import org.robotframework.ide.eclipse.main.plugin.navigator.ArtificialGroupingRobotElement;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

public class RobotOutlineContentProvider implements ITreeContentProvider {

    private TreeViewer viewer;

    @Inject
    @Optional
    @Named(ISources.ACTIVE_SITE_NAME)
    private IViewSite site;

    public RobotOutlineContentProvider() {
        final IEclipseContext activeContext = getContext().getActiveLeaf();
        ContextInjectionFactory.inject(this, activeContext);
    }

	@Override
	public void dispose() {
        final IEclipseContext activeContext = getContext().getActiveLeaf();
        ContextInjectionFactory.uninject(this, activeContext);
	}

    private IEclipseContext getContext() {
        return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getService(IEclipseContext.class);
    }

	@Override
	public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {
        this.viewer = (TreeViewer) viewer;
	}

	@Override
	public Object[] getElements(final Object inputElement) {
        return new Object[] { ((Object[]) inputElement)[0] };
	}

	@Override
    public Object[] getChildren(final Object parentElement) {
        if (parentElement instanceof RobotSettingsSection) {
            final List<? extends RobotElement> children = ((RobotElement) parentElement).getChildren();
            return groupedChildren(children).toArray();
        } else if (parentElement instanceof RobotCodeHoldingElement) {
            final List<? extends RobotElement> children = ((RobotElement) parentElement).getChildren();
            return newArrayList(Iterables.filter(children, new Predicate<RobotElement>() {

                @Override
                public boolean apply(final RobotElement element) {
                    return element.getClass() == RobotKeywordCall.class;
                }
            })).toArray();
        } else if (parentElement instanceof RobotElement) {
            return ((RobotElement) parentElement).getChildren().toArray();
        }
        return new Object[0];
    }

    private List<RobotElement> groupedChildren(final List<? extends RobotElement> children) {
        final List<RobotElement> grouped = new ArrayList<>(children);
        final Multimap<SettingsGroup, RobotElement> removedElements = LinkedHashMultimap.create();

        for (final RobotElement element : children) {
            if (element instanceof RobotSetting) {
                final SettingsGroup group = ((RobotSetting) element).getGroup();
                if (group != SettingsGroup.NO_GROUP) {
                    grouped.remove(element);
                    removedElements.put(group, element);
                }
            }
        }
        for (final SettingsGroup key : removedElements.keySet()) {
            grouped.add(new ArtificialGroupingRobotElement(key, new ArrayList<>(removedElements.get(key))));
        }
        return grouped;
    }

    @Override
	public Object getParent(final Object element) {
        if (element instanceof RobotElement) {
            return ((RobotElement) element).getParent();
        }
		return null;
	}

	@Override
	public boolean hasChildren(final Object element) {
        if (element instanceof RobotElement) {
            return !((RobotElement) element).getChildren().isEmpty();
        }
        return true;
	}

    @Inject
    @Optional
    private void whenStructuralChangeWasMade(
            @UIEventTopic(RobotModelEvents.ROBOT_SUITE_FILE_ALL) final RobotElement affectedElement) {
        if (viewer != null && !viewer.getTree().isDisposed()) {
            viewer.refresh(affectedElement);
        }
    }

    @Inject
    @Optional
    private void whenFileChangesExternally(
            @UIEventTopic(RobotModelEvents.EXTERNAL_MODEL_CHANGE) final RobotElementChange change) {
        if (change.getElement() instanceof RobotSuiteFile && change.getKind() == Kind.CHANGED && viewer != null
                && !viewer.getTree().isDisposed()) {
            viewer.refresh();
        }
    }

    @Inject
    @Optional
    private void whenReconcilationWasDone(
            @UIEventTopic(RobotModelEvents.RECONCILATION_DONE) final RobotSuiteFile fileModel) {
        if (viewer != null && !viewer.getTree().isDisposed()) {
            viewer.refresh();
        }
    }

    @Inject
    @Optional
    private void whenCaseNameChanges(@UIEventTopic(RobotModelEvents.ROBOT_CASE_NAME_CHANGE) final RobotCase testCase) {
        if (viewer != null && !viewer.getTree().isDisposed()) {
            viewer.update(testCase, null);
        }
    }

    @Inject
    @Optional
    private void whenKeywordDefinitionNameChanges(
            @UIEventTopic(RobotModelEvents.ROBOT_KEYWORD_DEFINITION_NAME_CHANGE) final RobotKeywordDefinition keywordDef) {
        if (viewer != null && !viewer.getTree().isDisposed()) {
            viewer.update(keywordDef, null);
        }
    }

    @Inject
    @Optional
    private void whenKeywordCallNameChanges(
            @UIEventTopic(RobotModelEvents.ROBOT_KEYWORD_CALL_NAME_CHANGE) final RobotKeywordCall keywordCall) {
        if (viewer != null && !viewer.getTree().isDisposed()) {
            viewer.update(keywordCall, null);
        }
    }

    @Inject
    @Optional
    private void whenVariableTypeChanges(
            @UIEventTopic(RobotModelEvents.ROBOT_VARIABLE_TYPE_CHANGE) final RobotVariable variable) {
        if (viewer != null && !viewer.getTree().isDisposed()) {
            viewer.update(variable, null);
        }
    }

    @Inject
    @Optional
    private void whenVariableNameChanges(
            @UIEventTopic(RobotModelEvents.ROBOT_VARIABLE_NAME_CHANGE) final RobotVariable variable) {
        if (viewer != null && !viewer.getTree().isDisposed()) {
            viewer.update(variable, null);
        }
    }

    @Inject
    @Optional
    private void whenSettingArgumentChanges(
            @UIEventTopic(RobotModelEvents.ROBOT_KEYWORD_CALL_ARGUMENT_CHANGE) final RobotSetting setting) {
        // in case of Library/Resource/etc. import or metadata, we are also
        // interested in changes of first argument to keyword call, since it
        // used as label
        if (viewer != null && !viewer.getTree().isDisposed()) {
            viewer.update(setting, null);
        }
    }
}
