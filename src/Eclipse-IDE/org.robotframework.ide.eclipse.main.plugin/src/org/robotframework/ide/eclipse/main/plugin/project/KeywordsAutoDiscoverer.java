/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project;

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.rf.ide.core.dryrun.RobotDryRunKeywordSource;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedLibrary;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;

import com.google.common.io.Files;

/**
 * @author bembenek
 */
public class KeywordsAutoDiscoverer extends AbstractAutoDiscoverer {

    public KeywordsAutoDiscoverer(final RobotProject robotProject) {
        super(robotProject, Collections.<IResource> emptyList());
    }

    @Override
    public void start(final Shell parent) {
        if (lockDryRun()) {
            try {
                new ProgressMonitorDialog(parent).run(true, true, new IRunnableWithProgress() {

                    @Override
                    public void run(final IProgressMonitor monitor)
                            throws InvocationTargetException, InterruptedException {
                        try {
                            startDiscovering(monitor, new DryRunTargetsCollector());
                            if (monitor.isCanceled()) {
                                return;
                            }
                            startAddingKeywordsToProject(monitor, dryRunOutputParser.getKeywordSources());
                        } finally {
                            monitor.done();
                            unlockDryRun();
                        }
                    }
                });
            } catch (final InvocationTargetException e) {
                throw new AutoDiscovererException("Problems occurred during discovering keywords.", e);
            } catch (final InterruptedException e) {
                dryRunHandler.destroyDryRunProcess();
            }
        }
    }

    private void startAddingKeywordsToProject(final IProgressMonitor monitor,
            final List<RobotDryRunKeywordSource> dryRunKeywordSources) {
        if (!dryRunKeywordSources.isEmpty()) {
            final SubMonitor subMonitor = SubMonitor.convert(monitor);
            subMonitor.subTask("Adding keywords to project...");
            subMonitor.setWorkRemaining(dryRunKeywordSources.size());
            for (final RobotDryRunKeywordSource keywordSource : dryRunKeywordSources) {
                robotProject.addKeywordSource(keywordSource);
                subMonitor.worked(1);
            }
        }
    }

    private class DryRunTargetsCollector implements IDryRunTargetsCollector {

        private final List<String> suiteNames = newArrayList();

        private final List<String> additionalProjectsLocations = newArrayList();

        @Override
        public void collectSuiteNamesAndAdditionalProjectsLocations() {
            final List<String> libraryNames = collectLibraryNames();
            if (!libraryNames.isEmpty()) {
                final File tempSuiteFileWithLibraries = dryRunHandler.createTempSuiteFile(new ArrayList<String>(),
                        libraryNames);
                if (tempSuiteFileWithLibraries != null) {
                    suiteNames.add(Files.getNameWithoutExtension(tempSuiteFileWithLibraries.getPath()));
                    additionalProjectsLocations.add(tempSuiteFileWithLibraries.getParent());
                }
            }
        }

        private List<String> collectLibraryNames() {
            final List<String> libraryNames = newArrayList();
            libraryNames.addAll(robotProject.getStandardLibraries().keySet());
            for (final ReferencedLibrary referencedLibrary : robotProject.getReferencedLibraries().keySet()) {
                libraryNames.add(referencedLibrary.getName());
            }
            return libraryNames;
        }

        @Override
        public List<String> getSuiteNames() {
            return suiteNames;
        }

        @Override
        public List<String> getAdditionalProjectsLocations() {
            return additionalProjectsLocations;
        }

    }

}
