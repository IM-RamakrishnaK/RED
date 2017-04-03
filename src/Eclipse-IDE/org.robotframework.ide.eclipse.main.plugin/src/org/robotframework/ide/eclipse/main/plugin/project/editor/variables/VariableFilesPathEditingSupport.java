/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.editor.variables;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.viewers.AlwaysDeactivatingCellEditor;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedVariableFile;
import org.robotframework.ide.eclipse.main.plugin.RedWorkspace;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfigEvents;
import org.robotframework.ide.eclipse.main.plugin.project.editor.RedProjectEditorInput;
import org.robotframework.red.viewers.ElementsAddingEditingSupport;


/**
 * @author Michal Anglart
 *
 */
class VariableFilesPathEditingSupport extends ElementsAddingEditingSupport {

    VariableFilesPathEditingSupport(final ColumnViewer viewer,
            final Supplier<ReferencedVariableFile> elementsCreator) {
        super(viewer, 0, elementsCreator);
    }

    @Override
    protected int getColumnShift() {
        return 1;
    }

    @Override
    protected CellEditor getCellEditor(final Object element) {
        return new AlwaysDeactivatingCellEditor((Composite) getViewer().getControl());
    }

    @Override
    protected Object getValue(final Object element) {
        return null;
    }

    @Override
    protected void setValue(final Object element, final Object value) {
        if (element instanceof ReferencedVariableFile) {
            final VariableFileCreator variableFileCreator = (VariableFileCreator) elementsCreator;
            scheduleViewerRefreshAndEditorActivation(
                    variableFileCreator.modifyExisting((ReferencedVariableFile) element));
        } else {
            super.setValue(element, value);
        }
    }

    private static IEventBroker getEventBroker() {
        return PlatformUI.getWorkbench().getService(IEventBroker.class);
    }

    static class VariableFileCreator implements Supplier<ReferencedVariableFile> {

        private final Shell shell;

        private final RedProjectEditorInput editorInput;

        VariableFileCreator(final Shell shell, final RedProjectEditorInput editorInput) {
            this.shell = shell;
            this.editorInput = editorInput;
        }

        @Override
        public ReferencedVariableFile get() {
            final FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
            dialog.setFilterExtensions(new String[] { "*.py", "*.*" });
            dialog.setFilterPath(editorInput.getRobotProject().getProject().getLocation().toPortableString());

            ReferencedVariableFile firstFile = null;

            if (dialog.open() != null) {
                final List<ReferencedVariableFile> variableFiles  = new ArrayList<>();
                final String[] chosenFiles = dialog.getFileNames();
                for (final String file : chosenFiles) {
                    final IPath path = RedWorkspace.Paths
                            .toWorkspaceRelativeIfPossible(new Path(dialog.getFilterPath()))
                            .addTrailingSeparator() //add separator when filterPath is e.g. 'D:'
                            .append(file);

                    variableFiles.add(ReferencedVariableFile.create(path.toPortableString()));
                }
                for (final ReferencedVariableFile variableFile : variableFiles) {
                    if (firstFile == null) {
                        firstFile = variableFile;
                    }
                    editorInput.getProjectConfiguration().addReferencedVariableFile(variableFile);
                }
                getEventBroker().send(RobotProjectConfigEvents.ROBOT_CONFIG_VAR_FILE_STRUCTURE_CHANGED, variableFiles);
            }
            return firstFile;
        }

        ReferencedVariableFile modifyExisting(final ReferencedVariableFile varFile) {
            final FileDialog dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
            dialog.setFilterExtensions(new String[] { "*.py", "*.*" });
            final IPath startingPath = RedWorkspace.Paths
                    .toAbsoluteFromWorkspaceRelativeIfPossible(new Path(varFile.getPath())).removeLastSegments(1);
            dialog.setFilterPath(startingPath.toPortableString());

            final String chosenFile = dialog.open();
            if (chosenFile != null) {
                final IPath path = RedWorkspace.Paths.toWorkspaceRelativeIfPossible(new Path(chosenFile));
                varFile.setPath(path.toPortableString());

                getEventBroker().send(RobotProjectConfigEvents.ROBOT_CONFIG_VAR_FILE_PATH_CHANGED, varFile);
            }
            return varFile;
        }
    }
}
