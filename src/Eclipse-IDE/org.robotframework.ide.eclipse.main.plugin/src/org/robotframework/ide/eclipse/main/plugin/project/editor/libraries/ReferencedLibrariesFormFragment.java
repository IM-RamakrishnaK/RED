/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.editor.libraries;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.services.IDirtyProviderService;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.RowExposingTableViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerColumnsFactory;
import org.eclipse.jface.viewers.ViewersConfigurator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.rf.ide.core.executor.RobotRuntimeEnvironment;
import org.rf.ide.core.executor.SuiteExecutor;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModelEvents;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig.ReferencedLibrary;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfigEvents;
import org.robotframework.ide.eclipse.main.plugin.project.editor.Environments;
import org.robotframework.ide.eclipse.main.plugin.project.editor.RedProjectEditorInput;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.CellsActivationStrategy;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.CellsActivationStrategy.RowTabbingStrategy;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.ISectionFormFragment;
import org.robotframework.red.forms.RedFormToolkit;
import org.robotframework.red.viewers.Viewers;

class ReferencedLibrariesFormFragment implements ISectionFormFragment {

    private static final String CONTEXT_ID = "org.robotframework.ide.eclipse.redxmleditor.libraries.context";

    @Inject
    private IEditorSite site;

    @Inject
    private IEventBroker eventBroker;

    @Inject
    private RedFormToolkit toolkit;

    @Inject
    private IDirtyProviderService dirtyProviderService;

    @Inject
    private RedProjectEditorInput editorInput;
    
    private RowExposingTableViewer viewer;

    private Button addPythonLibButton;

    private Button addJavaLibButton;

    private Button addLibspecButton;

    private ControlDecoration decoration;

    private RobotRuntimeEnvironment environment;

    public TableViewer getViewer() {
        return viewer;
    }

    @Override
    public void initialize(final Composite parent) {
        final Section section = createSection(parent);

        final Composite internalComposite = toolkit.createComposite(section);
        section.setClient(internalComposite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(internalComposite);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(internalComposite);

        createViewer(internalComposite);
        createColumns();
        createContextMenu();

        createButtons(internalComposite);

        setInput();
    }

    private Section createSection(final Composite parent) {
        final Section section = toolkit.createSection(parent, ExpandableComposite.EXPANDED
                | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION | ExpandableComposite.TWISTIE);
        section.setText("Referenced libraries");
        section.setDescription("In this section referenced libraries can be specified. Those libraries will "
                + "be available for all suites within project.");
        GridDataFactory.fillDefaults().grab(true, true).applyTo(section);
        return section;
    }

    private void createViewer(final Composite parent) {
        viewer = new RowExposingTableViewer(parent,
                SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        CellsActivationStrategy.addActivationStrategy(viewer, RowTabbingStrategy.MOVE_TO_NEXT);

        GridDataFactory.fillDefaults().grab(true, true).span(1, 4).indent(0, 10).applyTo(viewer.getTable());
        viewer.setUseHashlookup(true);
        viewer.getTable().setEnabled(false);
        viewer.getTable().setLinesVisible(false);
        viewer.getTable().setHeaderVisible(false);

        viewer.setContentProvider(new ReferencedLibrariesContentProvider());

        ViewersConfigurator.enableDeselectionPossibility(viewer);
        ViewersConfigurator.disableContextMenuOnHeader(viewer);
        Viewers.boundViewerWithContext(viewer, site, CONTEXT_ID);
    }

    private void createColumns() {
        ViewerColumnsFactory.newColumn("").withWidth(100)
            .shouldGrabAllTheSpaceLeft(true).withMinWidth(100)
            .labelsProvidedBy(new ReferencedLibrariesLabelProvider())
            .createFor(viewer);
    }

    private void createContextMenu() {
        final String menuId = "org.robotframework.ide.eclipse.redxmleditor.reflibraries.contextMenu";

        final MenuManager manager = new MenuManager("Red.xml file editor referenced libraries context menu", menuId);
        final Table control = viewer.getTable();
        final Menu menu = manager.createContextMenu(control);
        control.setMenu(menu);
        site.registerContextMenu(menuId, manager, viewer, false);
    }

    private void createButtons(final Composite parent) {
        addPythonLibButton = toolkit.createButton(parent, "Add Python library", SWT.PUSH);
        addPythonLibButton.setEnabled(false);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).indent(0, 10).applyTo(addPythonLibButton);
        addPythonHandler();

        addJavaLibButton = toolkit.createButton(parent, "Add Java library", SWT.PUSH);
        addJavaLibButton.setEnabled(false);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(addJavaLibButton);
        addJavaHandler();

        addLibspecButton = toolkit.createButton(parent, "Add libspec file", SWT.PUSH);
        addLibspecButton.setEnabled(false);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(addLibspecButton);
        addLibspecHandler();
    }
    
    private void addPythonHandler() {
        addPythonLibButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
                
                final FileDialog dialog = createReferencedLibFileDialog();
                dialog.setFilterExtensions(new String[] { "*.py", "*.*" });

                final String chosenFilePath = dialog.open();
                if (chosenFilePath != null) {
                    final ReferencedLibraryImporter importer = new ReferencedLibraryImporter();
                    final ReferencedLibrary lib = importer.importPythonLib(viewer.getTable().getShell(), environment,
                            chosenFilePath);
                    if (lib != null) {
                        editorInput.getProjectConfiguration().addReferencedLibrary(lib);

                        eventBroker.send(RobotProjectConfigEvents.ROBOT_CONFIG_LIBRARIES_STRUCTURE_CHANGED,
                                newArrayList(lib));
                    }
                }
            }
        });
    }

    private void addJavaHandler() {
        addJavaLibButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                
                final FileDialog dialog = createReferencedLibFileDialog();
                dialog.setFilterExtensions(new String[] { "*.jar" });
                final String chosenFilePath = dialog.open();
                if (chosenFilePath != null) {
                    final ReferencedLibraryImporter importer = new ReferencedLibraryImporter();
                    final ReferencedLibrary lib = importer.importJavaLib(viewer.getTable().getShell(), chosenFilePath);
                    if (lib != null) {
                        editorInput.getProjectConfiguration().addReferencedLibrary(lib);
                        
                        eventBroker.send(RobotProjectConfigEvents.ROBOT_CONFIG_LIBRARIES_STRUCTURE_CHANGED,
                                newArrayList(lib));
                    }
                }
            }
        });
    }
    
    private void addLibspecHandler() {
        addLibspecButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final FileDialog dialog = createReferencedLibFileDialog();
                dialog.setFilterExtensions(new String[] { "*.xml", "*.*" });
                final String chosenFilePath = dialog.open();
                if (chosenFilePath != null) {
                    final ReferencedLibraryImporter importer = new ReferencedLibraryImporter();
                    final ReferencedLibrary lib = importer.importLibFromSpecFile(chosenFilePath);
                    if (lib != null) {
                        editorInput.getProjectConfiguration().addReferencedLibrary(lib);

                        eventBroker.send(RobotProjectConfigEvents.ROBOT_CONFIG_LIBRARIES_STRUCTURE_CHANGED,
                                newArrayList(lib));
                    }
                }
            }
        });
    }

    private FileDialog createReferencedLibFileDialog() {
        final String startingPath = editorInput.getRobotProject().getProject().getLocation().toOSString();
        final FileDialog dialog = new FileDialog(viewer.getTable().getShell(), SWT.OPEN);
        dialog.setFilterPath(startingPath);
        return dialog;
    }

    private void setInput() {
        final List<ReferencedLibrary> libspecs = editorInput.getProjectConfiguration().getLibraries();
        viewer.setInput(libspecs);
    }

    @Override
    public void setFocus() {
        viewer.getTable().setFocus();
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

        addPythonLibButton.setEnabled(false);
        addJavaLibButton.setEnabled(false);
        addLibspecButton.setEnabled(false);
        viewer.getTable().setEnabled(false);
    }

    @Inject
    @Optional
    private void whenEnvironmentsWereLoaded(
            @UIEventTopic(RobotProjectConfigEvents.ROBOT_CONFIG_ENV_LOADED) final Environments envs) {
        this.environment = envs.getActiveEnvironment();

        final boolean isEditable = editorInput.isEditable();
        final boolean projectIsInterpretedByJython = environment.getInterpreter() == SuiteExecutor.Jython;

        addPythonLibButton.setEnabled(isEditable);
        addJavaLibButton.setEnabled(isEditable && projectIsInterpretedByJython);
        addLibspecButton.setEnabled(isEditable);
        viewer.getTable().setEnabled(isEditable);

        if (!addJavaLibButton.isEnabled()) {
            decoration = new ControlDecoration(addJavaLibButton, SWT.RIGHT | SWT.TOP);
            decoration.setDescriptionText("Project is configured to use " + environment.getInterpreter().toString()
                    + " interpreter, but Jython is needed for Java libraries.");
            decoration.setImage(FieldDecorationRegistry.getDefault()
                    .getFieldDecoration(FieldDecorationRegistry.DEC_INFORMATION)
                    .getImage());
        } else if (decoration != null) {
            decoration.dispose();
            decoration = null;
        }
    }
    
    @Inject
    @Optional
    private void changeEvent(@UIEventTopic(RobotModelEvents.ROBOT_SETTING_LIBRARY_CHANGED_IN_SUITE) final String string) {
        editorInput.refreshProjectConfiguration();
        setInput();
        viewer.refresh();
    }

    @Inject
    @Optional
    private void whenLibrariesChanged(
            @UIEventTopic(RobotProjectConfigEvents.ROBOT_CONFIG_LIBRARIES_STRUCTURE_CHANGED) final String libName) {
        setDirty(true);
        viewer.refresh();
    }

    @Inject
    @Optional
    private void whenLibrariesChanged(
            @UIEventTopic(RobotProjectConfigEvents.ROBOT_CONFIG_LIBRARIES_STRUCTURE_CHANGED) final List<ReferencedLibrary> libs) {
        setDirty(true);
        viewer.refresh();
    }
}
