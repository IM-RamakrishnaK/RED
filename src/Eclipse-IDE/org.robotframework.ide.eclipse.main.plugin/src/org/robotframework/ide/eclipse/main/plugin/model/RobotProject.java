/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.model;

import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.newLinkedHashMap;
import static com.google.common.collect.Sets.newHashSet;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.rf.ide.core.executor.RobotRuntimeEnvironment;
import org.rf.ide.core.testdata.RobotParser;
import org.rf.ide.core.testdata.model.RobotProjectHolder;
import org.robotframework.ide.eclipse.main.plugin.PathsConverter;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.RobotExpressions;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig.LibraryType;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig.ReferencedLibrary;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig.ReferencedVariableFile;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig.RemoteLocation;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig.VariableMapping;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfigReader;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfigReader.CannotReadProjectConfigurationException;
import org.robotframework.ide.eclipse.main.plugin.project.library.LibrarySpecification;
import org.robotframework.ide.eclipse.main.plugin.project.library.LibrarySpecificationReader;
import org.robotframework.ide.eclipse.main.plugin.project.library.LibrarySpecificationReader.CannotReadlibrarySpecificationException;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicates;

public class RobotProject extends RobotContainer {

    private RobotProjectHolder projectHolder;
    
    private Map<String, LibrarySpecification> stdLibsSpecs;
    private Map<ReferencedLibrary, LibrarySpecification> refLibsSpecs;
    private List<ReferencedVariableFile> referencedVariableFiles;
    
    private RobotProjectConfig configuration;

    private List<File> modulesSearchPath;

    RobotProject(final IProject project) {
        super(null, project);
    }
    
    public synchronized RobotProjectHolder getRobotProjectHolder() {
        if (projectHolder == null) {
            projectHolder = new RobotProjectHolder(getRuntimeEnvironment());
        }
        return projectHolder;
    }

    public RobotParser getEagerRobotParser() {
        return RobotParser.createEager(getRobotProjectHolder());
    }
    
    public RobotParser getRobotParser() {
        return RobotParser.create(getRobotProjectHolder());
    }

    public IProject getProject() {
        return (IProject) container;
    }

    public String getVersion() {
        readProjectConfigurationIfNeeded();
        final RobotRuntimeEnvironment env = getRuntimeEnvironment();
        return env == null ? "???" : env.getVersion();
    }

    public Collection<LibrarySpecification> getLibrariesSpecifications() {
        final List<LibrarySpecification> specifications = newArrayList();
        specifications.addAll(getStandardLibraries().values());
        specifications.addAll(getReferencedLibraries().values());
        return newArrayList(filter(specifications, Predicates.notNull()));
    }

    public synchronized boolean hasStandardLibraries() {
        readProjectConfigurationIfNeeded();
        if (stdLibsSpecs != null && !stdLibsSpecs.isEmpty()) {
            return true;
        }
        return configuration != null;
    }

    public synchronized Map<String, LibrarySpecification> getStandardLibraries() {
        if (stdLibsSpecs != null) {
            return stdLibsSpecs;
        }
        readProjectConfigurationIfNeeded();
        final RobotRuntimeEnvironment env = getRuntimeEnvironment();
        if (env == null || configuration == null) {
            return newLinkedHashMap();
        }
        stdLibsSpecs = newLinkedHashMap();
        for (final String stdLib : env.getStandardLibrariesNames()) {
            stdLibsSpecs.put(stdLib, stdLibToSpec(getProject()).apply(stdLib));
        }
        for (final RemoteLocation location : configuration.getRemoteLocations()) {
            stdLibsSpecs.put("Remote " + location.getUri(), remoteLibToSpec(getProject()).apply(location));
        }
        return stdLibsSpecs;
    }

    public synchronized boolean hasReferencedLibraries() {
        readProjectConfigurationIfNeeded();
        if (refLibsSpecs != null && !refLibsSpecs.isEmpty()) {
            return true;
        }
        return configuration != null && configuration.hasReferencedLibraries();
    }

    public synchronized Map<ReferencedLibrary, LibrarySpecification> getReferencedLibraries() {
        if (refLibsSpecs != null) {
            return refLibsSpecs;
        }
        readProjectConfigurationIfNeeded();
        if (configuration == null) {
            return newLinkedHashMap();
        }
        refLibsSpecs = newLinkedHashMap();
        for (final ReferencedLibrary library : configuration.getLibraries()) {
            refLibsSpecs.put(library, libToSpec(getProject()).apply(library));
        }
        return refLibsSpecs;
    }

    private static Function<String, LibrarySpecification> stdLibToSpec(final IProject project) {
        return new Function<String, LibrarySpecification>() {

            @Override
            public LibrarySpecification apply(final String libraryName) {
                try {
                    final IFile file = LibspecsFolder.get(project).getSpecFile(libraryName);
                    return LibrarySpecificationReader.readSpecification(file);
                } catch (final CannotReadlibrarySpecificationException e) {
                    return null;
                }
            }
        };
    }

    private static Function<RemoteLocation, LibrarySpecification> remoteLibToSpec(final IProject project) {
        return new Function<RemoteLocation, LibrarySpecification>() {

            @Override
            public LibrarySpecification apply(final RemoteLocation remoteLocation) {
                try {
                    final IFile file = LibspecsFolder.get(project).getSpecFile(remoteLocation.createLibspecFileName());
                    return LibrarySpecificationReader.readRemoteSpecification(file, remoteLocation);
                } catch (final CannotReadlibrarySpecificationException e) {
                    return null;
                }
            }
        };
    }

    private static Function<ReferencedLibrary, LibrarySpecification> libToSpec(final IProject project) {
        return new Function<ReferencedLibrary, LibrarySpecification>() {

            @Override
            public LibrarySpecification apply(final ReferencedLibrary lib) {
                try {
                    if (lib.provideType() == LibraryType.VIRTUAL) {
                        final IPath path = Path.fromPortableString(lib.getPath());
                        final IResource libspec = project.getParent().findMember(path);
                        IFile fileToRead;

                        if (libspec != null && libspec.getType() == IResource.FILE) {
                            fileToRead = (IFile) libspec;
                            return LibrarySpecificationReader.readSpecification((IFile) libspec);
                        } else if (libspec == null) {
                            fileToRead = LibspecsFolder.get(project).getSpecFile(lib.getName());
                        } else {
                            fileToRead = null;
                        }
                        return fileToRead == null ? null : LibrarySpecificationReader.readSpecification(fileToRead);
                    } else if (lib.provideType() == LibraryType.JAVA || lib.provideType() == LibraryType.PYTHON) {
                        final IFile file = LibspecsFolder.get(project).getSpecFile(lib.getName());
                        return LibrarySpecificationReader.readReferencedSpecification(file, lib.getPath());
                    } else {
                        return null;
                    }
                } catch (final CannotReadlibrarySpecificationException e) {
                    return null;
                }
            }
        };
    }

    private synchronized RobotProjectConfig readProjectConfigurationIfNeeded() {
        if (configuration == null) {
            try {
                configuration = new RobotProjectConfigReader().readConfiguration(getProject());
            } catch (final CannotReadProjectConfigurationException e) {
                // oh well...
            }
        }
        return configuration;
    }
    
    public synchronized RobotProjectConfig getRobotProjectConfig() {
        readProjectConfigurationIfNeeded();
        return configuration;
    }

    public void clearCachedData() {
        projectHolder.clearModelFiles();
    }

    /**
     * Clearing should be done when user changed his/hers execution environment (python+robot)
     */
    public synchronized void clearAll() {
        projectHolder = null;
        clearConfiguration();
    }

    public synchronized void clearConfiguration() {
        configuration = null;
        referencedVariableFiles = null;
        stdLibsSpecs = null;
        refLibsSpecs = null;
    }

    public synchronized RobotRuntimeEnvironment getRuntimeEnvironment() {
        readProjectConfigurationIfNeeded();
        if (configuration == null || configuration.usesPreferences()) {
            return RedPlugin.getDefault().getActiveRobotInstallation();
        }
        return RedPlugin.getDefault().getRobotInstallation(configuration.providePythonLocation());
    }

    public IFile getConfigurationFile() {
        return getProject().getFile(RobotProjectConfig.FILENAME);
    }

    public IFile getFile(final String filename) {
        return getProject().getFile(filename);
    }

    public synchronized List<File> getModuleSearchPaths() {
        if (modulesSearchPath != null) {
            return modulesSearchPath;
        }
        modulesSearchPath = getRuntimeEnvironment().getModuleSearchPaths();
        return modulesSearchPath;
    }
    
    public synchronized List<String> getPythonpath() {
        readProjectConfigurationIfNeeded();
        if (configuration != null) {
            final Set<String> pp = newHashSet();
            for (final ReferencedLibrary lib : configuration.getLibraries()) {
                if (lib.provideType() == LibraryType.PYTHON) {
                    final String path = PathsConverter.toAbsoluteFromWorkspaceRelativeIfPossible(
                            new Path(lib.getPath())).toPortableString();
                    pp.add(path);
                }
            }
            return newArrayList(pp);
        }
        return newArrayList();
    }
    
    public synchronized List<String> getClasspath() {
        readProjectConfigurationIfNeeded();
        if (configuration != null) {
            final Set<String> cp = newHashSet(".");
            for (final ReferencedLibrary lib : configuration.getLibraries()) {
                if (lib.provideType() == LibraryType.JAVA) {
                    cp.add(lib.getPath());
                }
            }
            return newArrayList(cp);
        }
        return newArrayList(".");
    }
    
    public synchronized boolean isStandardLibrary(final LibrarySpecification spec) {
        final Map<String, LibrarySpecification> stdLibs = getStandardLibraries();
        return isLibraryFrom(spec, stdLibs == null ? null : stdLibs.values());
    }
    
    public synchronized boolean isReferencedLibrary(final LibrarySpecification spec) {
        final Map<ReferencedLibrary, LibrarySpecification> refLibs = getReferencedLibraries();
        return isLibraryFrom(spec, refLibs == null ? null : refLibs.values());
    }

    private boolean isLibraryFrom(final LibrarySpecification spec, final Collection<LibrarySpecification> libs) {
        if (libs == null) {
            return false;
        }
        for (final LibrarySpecification librarySpecification : libs) {
            if (librarySpecification == spec) {
                return true;
            }
        }
        return false;
    }
    
    public synchronized String getPythonLibraryPath(final String libName) {
        readProjectConfigurationIfNeeded();
        if (configuration != null) {
            for (final ReferencedLibrary lib : configuration.getLibraries()) {
                if (lib.provideType() == LibraryType.PYTHON && lib.getName().equals(libName)) {
                    return PathsConverter.toAbsoluteFromWorkspaceRelativeIfPossible(
                            new Path(lib.getPath()).append(lib.getName() + ".py")).toPortableString();
                }
            }
        }
        return "";
    }
    
    public List<String> getVariableFilePaths() {
        readProjectConfigurationIfNeeded();
        if (configuration != null) {
            final List<String> list = newArrayList();
            for (final ReferencedVariableFile variableFile : configuration.getReferencedVariableFiles()) {
                final String path = PathsConverter
                        .toAbsoluteFromWorkspaceRelativeIfPossible(new Path(variableFile.getPath())).toPortableString();
                final List<String> args = variableFile.getArguments();
                final String arguments = args == null || args.isEmpty() ? "" : ":" + Joiner.on(":").join(args);
                list.add(path + arguments);
            }
            return list;
        }
        return newArrayList();
    }
    
    public synchronized List<ReferencedVariableFile> getVariablesFromReferencedFiles() {
        if(referencedVariableFiles != null) {
            return referencedVariableFiles;
        }
        readProjectConfigurationIfNeeded();
        if (configuration != null) {
            referencedVariableFiles = newArrayList();
            for (final ReferencedVariableFile variableFile : configuration.getReferencedVariableFiles()) {
                IPath path = new Path(variableFile.getPath());
                if (!path.isAbsolute()) {
                    final IResource targetFile = getProject().getWorkspace().getRoot().findMember(path);
                    if (targetFile != null && targetFile.exists()) {
                        path = targetFile.getLocation();
                    }
                }

                final Map<String, Object> varsMap = getRuntimeEnvironment()
                        .getVariablesFromFile(path.toPortableString(), variableFile.getArguments());
                if (varsMap != null && !varsMap.isEmpty()) {
                    variableFile.setVariables(varsMap);
                    referencedVariableFiles.add(variableFile);
                }
            }
            return referencedVariableFiles;
        }
        return newArrayList();
    }

    public String resolve(final String expression) {
        readProjectConfigurationIfNeeded();
        final Map<String, String> knownVariables = newHashMap();
        knownVariables.put("${/}", File.separator);
        if (configuration != null) {
            for (final VariableMapping mapping : configuration.getVariableMappings()) {
                knownVariables.put(mapping.getName(), mapping.getValue());
            }
        }
        return RobotExpressions.resolve(knownVariables, expression);
    }
}
