/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.editor.validation;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.services.IDirtyProviderService;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.RowExposingTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerColumnsFactory;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewersConfigurator;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.robotframework.ide.eclipse.main.plugin.model.LibspecsFolder;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig.ExcludedFolderPath;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfigEvents;
import org.robotframework.ide.eclipse.main.plugin.project.editor.Environments;
import org.robotframework.ide.eclipse.main.plugin.project.editor.RedProjectEditorInput;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.CellsActivationStrategy;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.CellsActivationStrategy.RowTabbingStrategy;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.ISectionFormFragment;
import org.robotframework.red.forms.RedFormToolkit;
import org.robotframework.red.swt.SwtThread;
import org.robotframework.red.viewers.Viewers;

import com.google.common.base.Predicate;

/**
 * @author Michal Anglart
 *
 */
public class ProjectValidationFormFragment implements ISectionFormFragment {

    private static final String CONTEXT_ID = "org.robotframework.ide.eclipse.redxmleditor.validation.context";

    @Inject
    private IEditorSite site;

    @Inject
    private RedFormToolkit toolkit;

    @Inject
    private IDirtyProviderService dirtyProviderService;

    @Inject
    private RedProjectEditorInput editorInput;

    private RowExposingTreeViewer viewer;

    ISelectionProvider getViewer() {
        return viewer;
    }

    @Override
    public void initialize(final Composite parent) {
        final Section section = createSection(parent);

        final Composite internalComposite = toolkit.createComposite(section);
        section.setClient(internalComposite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(internalComposite);
        GridLayoutFactory.fillDefaults().applyTo(internalComposite);

        createViewer(internalComposite);
        createColumns();
        createContextMenu();

        setInput();
        installResourceChangeListener();
    }

    private Section createSection(final Composite parent) {
        final Section section = toolkit.createSection(parent,
                ExpandableComposite.EXPANDED | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION);
        section.setText("Excluded project parts");
        section.setDescription("Specify parts of the project which shouldn't be validated.");
        GridDataFactory.fillDefaults().grab(true, true).applyTo(section);
        return section;
    }

    private void createViewer(final Composite parent) {
        viewer = new RowExposingTreeViewer(parent,
                SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        CellsActivationStrategy.addActivationStrategy(viewer, RowTabbingStrategy.MOVE_TO_NEXT);
        ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);

        GridDataFactory.fillDefaults().grab(true, true).indent(0, 10).applyTo(viewer.getTree());
        viewer.setUseHashlookup(true);
        viewer.getTree().setEnabled(false);
        viewer.setComparator(new ViewerSorter());

        viewer.setContentProvider(new WorkbenchContentProvider());

        ViewersConfigurator.enableDeselectionPossibility(viewer);
        ViewersConfigurator.disableContextMenuOnHeader(viewer);
        Viewers.boundViewerWithContext(viewer, site, CONTEXT_ID);
    }
    
    private void createColumns() {
        ViewerColumnsFactory.newColumn("").withWidth(300)
            .shouldGrabAllTheSpaceLeft(true).withMinWidth(100)
            .labelsProvidedBy(new ProjectValidationPathsLabelProvider(editorInput))
            .createFor(viewer);
    }

    private void createContextMenu() {
        final String menuId = "org.robotframework.ide.eclipse.redxmleditor.validation.contextMenu";

        final MenuManager manager = new MenuManager("Red.xml file editor validation context menu", menuId);
        final Tree control = viewer.getTree();
        final Menu menu = manager.createContextMenu(control);
        control.setMenu(menu);
        site.registerContextMenu(menuId, manager, viewer, false);
    }

    private void setInput() {
        try {
            viewer.getTree().setRedraw(false);

            final TreeItem topTreeItem = viewer.getTree().getTopItem();
            final Object topItem = topTreeItem == null ? null : topTreeItem.getData();

            final IProject project = editorInput.getRobotProject().getProject();
            final IWorkspaceRoot wsRoot = project.getWorkspace().getRoot();
            final ProjectTreeElement wrappedRoot = new ProjectTreeElement(wsRoot, false);

            buildTreeFor(wrappedRoot, project);
            addMissingEntriesToTree(wrappedRoot, wrappedRoot.getAll());

            viewer.setInput(wrappedRoot);
            viewer.setExpandedElements(getElementsToExpand(wrappedRoot.getAll()));
            if (topItem != null) {
                viewer.setTopItem(topItem);
            }
        } catch (final CoreException e) {
            throw new IllegalStateException("Unable to read project structure");
        } finally {
            viewer.getTree().setRedraw(true);
        }
    }

    private void buildTreeFor(final ProjectTreeElement parent, final IResource resource) throws CoreException {
        if (resource.getName().startsWith(".") || LibspecsFolder.get(resource.getProject()).getResource().equals(resource)) {
            return;
        }

        final boolean isExcluded = editorInput.getProjectConfiguration()
                .isExcludedFromValidation(resource.getProjectRelativePath());
        
        final ProjectTreeElement wrappedChild = new ProjectTreeElement(resource, isExcluded);
        parent.addChild(wrappedChild);

        if (!isExcluded && resource instanceof IContainer) {
            final IContainer childContainer = (IContainer) resource;
            for (final IResource child : childContainer.members()) {
                buildTreeFor(wrappedChild, child);
            }
        }
    }

    private void addMissingEntriesToTree(final ProjectTreeElement wrappedRoot,
            final Collection<ProjectTreeElement> allElements) {
        final Set<ProjectTreeElement> excludedShownInTree = getExcludedElementsInTheTree(allElements);
        final List<ExcludedFolderPath> allExcluded = editorInput.getProjectConfiguration().getExcludedPath();
        final List<ExcludedFolderPath> excludedNotShownInTree = getExcludedNotShownInTheTree(allExcluded,
                excludedShownInTree);

        for (final ExcludedFolderPath excludedNotShown : excludedNotShownInTree) {
            wrappedRoot.createVirtualNodeFor(excludedNotShown.asPath());
        }
    }

    private Set<ProjectTreeElement> getExcludedElementsInTheTree(final Collection<ProjectTreeElement> allElements) {
        return newHashSet(filter(allElements, new Predicate<ProjectTreeElement>() {

            @Override
            public boolean apply(final ProjectTreeElement elem) {
                return elem.isExcluded();
            }
        }));
    }

    private List<ExcludedFolderPath> getExcludedNotShownInTheTree(final List<ExcludedFolderPath> allExcluded,
            final Set<ProjectTreeElement> excludedShownInTree) {
        final List<ExcludedFolderPath> paths = newArrayList();

        for (final ExcludedFolderPath excludedPath : allExcluded) {
            boolean isInTree = false;
            for (final ProjectTreeElement element : excludedShownInTree) {
                if (element.getPath().equals(excludedPath.asPath())) {
                    isInTree = true;
                    break;
                }
            }
            if (!isInTree) {
                paths.add(excludedPath);
            }
        }
        return paths;
    }

    private ProjectTreeElement[] getElementsToExpand(final Collection<ProjectTreeElement> allElements) {
        return toArray(filter(allElements, new Predicate<ProjectTreeElement>() {

            @Override
            public boolean apply(final ProjectTreeElement element) {
                return element.containsOtherFolders();
            }
        }), ProjectTreeElement.class);
    }

    private void installResourceChangeListener() {
        final IResourceChangeListener resourceListener = new IResourceChangeListener() {
            @Override
            public void resourceChanged(final IResourceChangeEvent event) {
                final AtomicBoolean shouldRefresh = new AtomicBoolean(false);
                
                if (event.getType() != IResourceChangeEvent.POST_CHANGE || event.getDelta() == null) {
                    return;
                }

                try {
                    event.getDelta().accept(new IResourceDeltaVisitor() {

                        @Override
                        public boolean visit(final IResourceDelta delta) throws CoreException {
                            if (editorInput.getRobotProject().getProject().equals(delta.getResource().getProject())) {
                                shouldRefresh.set(true);
                                return false;
                            }
                            return true;
                        }
                    });
                } catch (final CoreException e) {
                    // nothing to do
                }
                if (shouldRefresh.get()) {
                    SwtThread.syncExec(viewer.getTree().getDisplay(), new Runnable() {
                        @Override
                        public void run() {
                            setInput();
                        }
                    });
                }
            }
        };
        ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);
        viewer.getTree().addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
            }
        });
    }

    @Override
    public void setFocus() {
        viewer.getTree().setFocus();
    }

    private void setDirty(final boolean isDirty) {
        dirtyProviderService.setDirtyState(isDirty);
    }

    @Override
    public MatchesCollection collectMatches(final String filter) {
        return null;
    }

    @Inject
    @Optional
    private void whenEnvironmentLoadingStarted(
            @UIEventTopic(RobotProjectConfigEvents.ROBOT_CONFIG_ENV_LOADING_STARTED) final RobotProjectConfig config) {
        setInput();

        viewer.getTree().setEnabled(false);
    }

    @Inject
    @Optional
    private void whenEnvironmentsWereLoaded(
            @UIEventTopic(RobotProjectConfigEvents.ROBOT_CONFIG_ENV_LOADED) final Environments envs) {
        viewer.getTree().setEnabled(editorInput.isEditable());
    }

    @Inject
    @Optional
    private void whenExclusionListChanged(
            @UIEventTopic(RobotProjectConfigEvents.ROBOT_CONFIG_VALIDATION_EXCLUSIONS_STRUCTURE_CHANGED) final List<ProjectTreeElement> elements) {
        setDirty(true);
        setInput();
    }

    private static final class ViewerSorter extends ViewerComparator {

        @Override
        public int category(final Object element) {
            return ((ProjectTreeElement) element).isInternalFolder() ? 0 : 1;
        }

        @Override
        public int compare(final Viewer viewer, final Object e1, final Object e2) {
            final int cat1 = category(e1);
            final int cat2 = category(e2);

            if (cat1 != cat2) {
                return cat1 - cat2;
            }
            final ProjectTreeElement elem1 = (ProjectTreeElement) e1;
            final ProjectTreeElement elem2 = (ProjectTreeElement) e2;

            final String name1 = elem1.getPath().removeFileExtension().lastSegment();
            final String name2 = elem2.getPath().removeFileExtension().lastSegment();

            return name1.compareToIgnoreCase(name2);
        }
    }
}
