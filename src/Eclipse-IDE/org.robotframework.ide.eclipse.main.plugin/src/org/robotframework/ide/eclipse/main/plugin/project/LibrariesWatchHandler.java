/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.rf.ide.core.fileWatcher.IWatchEventHandler;
import org.rf.ide.core.fileWatcher.RedFileWatcher;
import org.robotframework.ide.eclipse.main.plugin.PathsConverter;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModelEvents;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig.LibraryType;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig.ReferencedLibrary;
import org.robotframework.ide.eclipse.main.plugin.project.build.BuildLogger;
import org.robotframework.ide.eclipse.main.plugin.project.build.libs.LibrariesBuilder;
import org.robotframework.ide.eclipse.main.plugin.project.library.LibrarySpecification;
import org.robotframework.red.swt.SwtThread;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * @author mmarzec
 */
public class LibrariesWatchHandler implements IWatchEventHandler {

    private final RobotProject robotProject;

    private IEventBroker eventBroker = null;

    private ListMultimap<LibrarySpecification, String> librarySpecifications = Multimaps
            .synchronizedListMultimap(ArrayListMultimap.<LibrarySpecification, String> create());

    private Set<LibrarySpecification> dirtySpecs = Collections.synchronizedSet(new HashSet<LibrarySpecification>());
    
    private Map<ReferencedLibrary, String> registeredRefLibraries = Collections.synchronizedMap(new HashMap<ReferencedLibrary, String>());

    private ConcurrentLinkedQueue<RebuildTask> rebuildTasksQueue = new ConcurrentLinkedQueue<>();

    public LibrariesWatchHandler(final RobotProject robotProject) {
        this.robotProject = robotProject;
    }

    public void registerLibrary(ReferencedLibrary library, final LibrarySpecification spec) {

        final String absolutePathToLibraryFile = findLibraryFileAbsolutePath(library);

        if (absolutePathToLibraryFile != null && spec != null && !librarySpecifications.containsKey(spec)) {
            final File libFile = new File(absolutePathToLibraryFile);
            final File libDir = libFile.getParentFile();
            final String libFileName = libFile.getName();
            if (libDir != null && libDir.exists() && libDir.isDirectory()) {
                if (isPythonModule(absolutePathToLibraryFile)) {
                    String[] moduleFilesList = extractPythonModuleFiles(libDir);
                    if (moduleFilesList != null) {
                        for (int i = 0; i < moduleFilesList.length; i++) {
                            addLibraryToWatch(moduleFilesList[i], libDir.toPath(), spec);
                        }
                    }
                } else {
                    addLibraryToWatch(libFileName, libDir.toPath(), spec);
                }
            }
        }
    }

    public void unregisterLibraries(final List<ReferencedLibrary> libraries) {
        if (libraries != null) {
            for (ReferencedLibrary referencedLibrary : libraries) {
                final String path = registeredRefLibraries.get(referencedLibrary);
                if (path != null) {
                    final File libFile = new File(path);
                    final File libDir = libFile.getParentFile();
                    if (isPythonModule(path) && libDir != null && libDir.exists()) {
                        String[] moduleFilesList = extractPythonModuleFiles(libDir);
                        if (moduleFilesList != null) {
                            for (int i = 0; i < moduleFilesList.length; i++) {
                                removeLibraryToWatch(moduleFilesList[i]);
                            }
                        }
                    } else {
                        removeLibraryToWatch(libFile.getName());
                    }
                    registeredRefLibraries.remove(referencedLibrary);
                }
            }
        }
    }

    public boolean isLibSpecDirty(final LibrarySpecification spec) {
        return dirtySpecs.contains(spec);
    }

    public void removeDirtySpecs(final Collection<LibrarySpecification> reloadedSpecs) {
        dirtySpecs.removeAll(reloadedSpecs);
    }

    private boolean isPythonModule(final String absolutePathToLibraryFile) {
        return absolutePathToLibraryFile.endsWith("__init__.py");
    }

    private String[] extractPythonModuleFiles(final File fileDir) {
        return fileDir.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".py")) {
                    return true;
                }
                return false;
            }
        });
    }

    private void addLibraryToWatch(final String fileName, final Path dir, final LibrarySpecification spec) {
        final List<LibrarySpecification> specsToReplace = new ArrayList<>();
        synchronized (librarySpecifications) {
            for (Entry<LibrarySpecification, String> entry : librarySpecifications.entries()) {
                if (entry.getValue().equals(fileName) && entry.getKey().equalsIgnoreKeywords(spec)) {
                    specsToReplace.add(entry.getKey());
                }
            }
            for (final LibrarySpecification specToReplace : specsToReplace) {
                librarySpecifications.removeAll(specToReplace);
            }
            librarySpecifications.put(spec, fileName);
        }
        registerPath(dir, fileName, this);
    }

    private void removeLibraryToWatch(final String fileName) {
        removeLibrarySpecification(fileName);
        unregisterFile(fileName, this);
    }

    private void removeLibrarySpecification(final String fileName) {
        final List<LibrarySpecification> specsToRemove = new ArrayList<>();
        synchronized (librarySpecifications) {
            for (Entry<LibrarySpecification, String> entry : librarySpecifications.entries()) {
                if (entry.getValue().equals(fileName)) {
                    specsToRemove.add(entry.getKey());
                }
            }
            for (final LibrarySpecification spec : specsToRemove) {
                librarySpecifications.removeAll(spec);
            }
        }
    }

    @Override
    public void registerPath(final Path dir, final String fileName, final IWatchEventHandler handler) {
        RedFileWatcher.getInstance().registerPath(dir, fileName, this);
    }

    @Override
    public void unregisterFile(final String fileName, final IWatchEventHandler handler) {
        RedFileWatcher.getInstance().unregisterFile(fileName, this);
    }

    @Override
    public void handleModifyEvent(final String modifiedFileName) {
        if (librarySpecifications.containsValue(modifiedFileName)) {

            final IProject project = robotProject.getProject();
            if (!project.exists()) {
                removeLibrarySpecification(modifiedFileName);
                unregisterFile(modifiedFileName, this);
                return;
            }

            SwtThread.asyncExec(new Runnable() {

                @Override
                public void run() {

                    if (robotProject.getRobotProjectConfig().isReferencedLibrariesAutoReloadEnabled()) {
                        final List<LibrarySpecification> specsToRebuild = new ArrayList<>();
                        synchronized (librarySpecifications) {
                            for (Entry<LibrarySpecification, String> entry : librarySpecifications.entries()) {
                                if (entry.getValue().equals(modifiedFileName)) {
                                    specsToRebuild.add(entry.getKey());
                                }
                            }
                        }
                        rebuildLibraries(project, specsToRebuild);
                    } else {
                        changeLibSpecModifyFlag(modifiedFileName);
                    }
                    refreshNavigator(project);
                }
            });
        }
    }

    private void rebuildLibraries(final IProject project, final List<LibrarySpecification> specs) {

        final RebuildTask newRebuildTask = new RebuildTask(project, specs);
        
        if (rebuildTasksQueue.isEmpty()) {
            rebuildTasksQueue.add(newRebuildTask);
            final Shell shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
            try {
                new ProgressMonitorDialog(shell).run(true, true, new IRunnableWithProgress() {

                    @Override
                    public void run(final IProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException {
                        handleRebuildTask(monitor, newRebuildTask);
                    }
                });
            } catch (InvocationTargetException | InterruptedException e) {
                MessageDialog.openError(shell, "Regenerating library specification",
                        "Problems occured during library specification generation " + e.getCause().getMessage());
            }
        } else {
            rebuildTasksQueue.add(newRebuildTask);
        }
    }

    private void handleRebuildTask(final IProgressMonitor monitor, final RebuildTask rebuildTask) {

        final Multimap<IProject, LibrarySpecification> groupedSpecifications = LinkedHashMultimap.create();
        groupedSpecifications.putAll(rebuildTask.getProject(), rebuildTask.getSpecsToRebuild());
        new LibrariesBuilder(new BuildLogger()).forceLibrariesRebuild(groupedSpecifications,
                SubMonitor.convert(monitor));
        robotProject.clearConfiguration();

        rebuildTasksQueue.poll();

        final RebuildTask nextRebuildTask = rebuildTasksQueue.peek();
        if (nextRebuildTask != null) {
            removePossibleDuplicatedRebuildTasks(nextRebuildTask);
            handleRebuildTask(monitor, nextRebuildTask);
        }
    }

    private void removePossibleDuplicatedRebuildTasks(final RebuildTask nextRebuildTask) {
        final Iterator<RebuildTask> tasksIterator = rebuildTasksQueue.iterator();
        if(tasksIterator.hasNext()) {tasksIterator.next();} //skip queue head
        while (tasksIterator.hasNext()) {
            if (tasksIterator.next().equals(nextRebuildTask)) {
                tasksIterator.remove();
            }
        }
    }

    private void changeLibSpecModifyFlag(final String modifiedFileName) {

        final List<LibrarySpecification> specsToChange = new ArrayList<>();
        synchronized (librarySpecifications) {
            for (Entry<LibrarySpecification, String> entry : librarySpecifications.entries()) {
                if (entry.getValue().equals(modifiedFileName)) {
                    specsToChange.add(entry.getKey());
                    dirtySpecs.add(entry.getKey());
                }
            }

            final Map<ReferencedLibrary, LibrarySpecification> referencedLibraries = robotProject
                    .getReferencedLibraries();
            for (final ReferencedLibrary refLib : referencedLibraries.keySet()) {
                final LibrarySpecification librarySpecification = referencedLibraries.get(refLib);
                if (specsToChange.contains(librarySpecification)) {
                    librarySpecification.setIsModified(true);
                }
            }
        }
    }

    private void refreshNavigator(final IProject project) {
        if (eventBroker == null) {
            eventBroker = (IEventBroker) PlatformUI.getWorkbench().getService(IEventBroker.class);
        }
        if (eventBroker != null) {
            eventBroker.post(RobotModelEvents.ROBOT_LIBRARY_SPECIFICATION_CHANGE, project);
        }
    }
    
    private String findLibraryFileAbsolutePath(final ReferencedLibrary library) {

        String absolutePath = registeredRefLibraries.get(library);
        if (absolutePath != null) {
            return absolutePath;
        }
        if (library.provideType() == LibraryType.VIRTUAL) {
            return null;
        }

        IPath libraryPath = new org.eclipse.core.runtime.Path(library.getPath());
        if (!libraryPath.isAbsolute()) {
            libraryPath = PathsConverter.toAbsoluteFromWorkspaceRelativeIfPossible(libraryPath);
        }
        final File libraryFile = libraryPath.toFile();
        if (libraryFile.exists()) {
            if (!libraryFile.isDirectory()) {
                absolutePath = libraryPath.toPortableString();
            } else {
                String libraryName = library.getName();
                if (libraryName.contains(".")) {
                    libraryName = libraryName.split("\\.")[0];
                }
                if (library.provideType() == LibraryType.PYTHON) {
                    IPath libFilePath = libraryPath.append(libraryName + ".py");
                    if (libFilePath.toFile().exists()) {
                        absolutePath = libFilePath.toPortableString();
                    }
                } else if (library.provideType() == LibraryType.JAVA) {
                    IPath libFilePath = libraryPath.append(libraryName + ".java");
                    if (libFilePath.toFile().exists()) {
                        absolutePath = libFilePath.toPortableString();
                    }
                }
                if (absolutePath == null) {
                    IPath libFilePath = libraryPath.append(libraryName).append("__init__.py");
                    if (libFilePath.toFile().exists()) {
                        absolutePath = libFilePath.toPortableString();
                    }
                }
            }
        }
        if (absolutePath != null) {
            registeredRefLibraries.put(library, absolutePath);
        }
        return absolutePath;
    }

    /**
     * for testing purposes only
     */
    protected Map<ReferencedLibrary, String> getRegisteredRefLibraries() {
        return registeredRefLibraries;
    }
    
    /**
     * for testing purposes only
     */
    protected ListMultimap<LibrarySpecification, String> getLibrarySpecifications() {
        return librarySpecifications;
    }

    private class RebuildTask {

        private IProject project;

        private List<LibrarySpecification> specsToRebuild;

        public RebuildTask(IProject project, List<LibrarySpecification> specsToRebuild) {
            this.project = project;
            this.specsToRebuild = specsToRebuild;
        }

        public IProject getProject() {
            return project;
        }

        public List<LibrarySpecification> getSpecsToRebuild() {
            return specsToRebuild;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            } else if (obj.getClass() == getClass()) {
                final RebuildTask other = (RebuildTask) obj;
                return Objects.equals(project, other.project) && Objects.equals(specsToRebuild, other.specsToRebuild);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(project, specsToRebuild);
        }
    }
}
