/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.editor.libraries;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.services.IDirtyProviderService;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.ViewerColumnsFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.rf.ide.core.executor.RobotRuntimeEnvironment;
import org.rf.ide.core.executor.SuiteExecutor;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModelEvents;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig.ReferencedLibrary;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfigEvents;
import org.robotframework.ide.eclipse.main.plugin.project.editor.Environments;
import org.robotframework.ide.eclipse.main.plugin.project.editor.RedProjectEditorInput;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.ISectionFormFragment;
import org.robotframework.red.forms.RedFormToolkit;
import org.robotframework.red.viewers.Selections;

class ReferencedLibrariesFormFragment implements ISectionFormFragment {

    @Inject
    private RedFormToolkit toolkit;

    @Inject
    private IDirtyProviderService dirtyProviderService;

    @Inject
    private RedProjectEditorInput editorInput;
    
    private RobotProject currentProject;

    private TableViewer viewer;

    private Button addPythonLibButton;

    private Button addJavaLibButton;

    private Button addLibspecButton;

    private Button removeButton;

    private ControlDecoration decoration;

    private RobotRuntimeEnvironment environment;

    public TableViewer getViewer() {
        return viewer;
    }

    @Override
    public void initialize(final Composite parent) {
        currentProject = editorInput.getRobotProject();
        
        final Section section = toolkit.createSection(parent, ExpandableComposite.EXPANDED
                | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION | ExpandableComposite.TWISTIE);
        section.setText("Referenced libraries");
        section.setDescription("In this section referenced libraries can be specified. Those libraries will "
                + "be available for all suites within project.");
        GridDataFactory.fillDefaults().grab(true, true).applyTo(section);

        final Composite internalComposite = toolkit.createComposite(section);
        section.setClient(internalComposite);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(internalComposite);
        GridLayoutFactory.fillDefaults().numColumns(2).applyTo(internalComposite);

        createViewer(internalComposite);
        createButtons(internalComposite);

        setInput();
    }

    private void createViewer(final Composite parent) {
        viewer = new TableViewer(parent);
        GridDataFactory.fillDefaults().grab(true, true).span(1, 4).applyTo(viewer.getTable());
        viewer.getTable().setEnabled(false);

        viewer.setContentProvider(new ReferencedLibrariesContentProvider());
        
        ViewerColumnsFactory.newColumn("").withWidth(100)
            .shouldGrabAllTheSpaceLeft(true).withMinWidth(100)
            .labelsProvidedBy(new ReferencedLibrariesLabelProvider())
            .createFor(viewer);

        final ISelectionChangedListener selectionChangedListener = new ISelectionChangedListener() {
            @Override
            public void selectionChanged(final SelectionChangedEvent event) {
                removeButton.setEnabled(!event.getSelection().isEmpty());
            }
        };
        viewer.addSelectionChangedListener(selectionChangedListener);
        viewer.getTable().addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(final DisposeEvent e) {
                viewer.removeSelectionChangedListener(selectionChangedListener);
            }
        });
    }

    private void createButtons(final Composite parent) {
        addPythonLibButton = toolkit.createButton(parent, "Add Python library", SWT.PUSH);
        addPythonLibButton.setEnabled(false);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(addPythonLibButton);
        addPythonHandler();

        addJavaLibButton = toolkit.createButton(parent, "Add Java library", SWT.PUSH);
        addJavaLibButton.setEnabled(false);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(addJavaLibButton);
        addJavaHandler();

        addLibspecButton = toolkit.createButton(parent, "Add libspec file", SWT.PUSH);
        addLibspecButton.setEnabled(false);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(addLibspecButton);
        addLibspecHandler();
        
        removeButton = toolkit.createButton(parent, "Remove", SWT.PUSH);
        removeButton.setEnabled(false);
        GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(removeButton);
        addRemoveHandler();
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
                        setDirty(true);
                        viewer.refresh();
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
                        setDirty(true);
                        viewer.refresh();
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
                    final ReferencedLibrary referencedLibrary = importer.importLibFromSpecFile(chosenFilePath);
                    if (referencedLibrary != null) {
                        editorInput.getProjectConfiguration().addReferencedLibrary(referencedLibrary);
                        setDirty(true);
                        viewer.refresh();
                    }
                }
            }
        });
    }

    private FileDialog createReferencedLibFileDialog() {
        final String startingPath = currentProject.getProject().getLocation().toOSString();
        final FileDialog dialog = new FileDialog(viewer.getTable().getShell(), SWT.OPEN);
        dialog.setFilterPath(startingPath);
        return dialog;
    }

    private void addRemoveHandler() {
        removeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
                final List<ReferencedLibrary> selectedLibs = Selections.getElements(
                        (IStructuredSelection) viewer.getSelection(), ReferencedLibrary.class);
                editorInput.getProjectConfiguration().removeLibraries(selectedLibs);
                setDirty(true);
                viewer.refresh();
            }
        });
    }

    @SuppressWarnings("restriction")
    private void setDirty(final boolean isDirty) {
        dirtyProviderService.setDirtyState(isDirty);
    }

    private void setInput() {
        final List<ReferencedLibrary> libspecs = editorInput.getProjectConfiguration().getLibraries();
        viewer.setInput(libspecs);
    }

    @Override
    public void setFocus() {
        viewer.getTable().setFocus();
    }

    @Override
    public MatchesCollection collectMatches(final String filter) {
        return null;
    }

    @Inject
    @Optional
    private void whenEnvironmentLoadingStarted(
            @UIEventTopic(RobotProjectConfigEvents.ROBOT_CONFIG_ENV_LOADING_STARTED) final RobotProjectConfig config) {
        addPythonLibButton.setEnabled(false);
        addJavaLibButton.setEnabled(false);
        addLibspecButton.setEnabled(false);
        removeButton.setEnabled(false);
        viewer.getTable().setEnabled(false);
        setInput();
        viewer.refresh();
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
        removeButton.setEnabled(false);
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
    private void whenMappingsChanged(
            @UIEventTopic(RobotProjectConfigEvents.ROBOT_CONFIG_LIBRARIES_STRUCTURE_CHANGED) final String libName) {
        setDirty(true);
        viewer.refresh();
    }
}
