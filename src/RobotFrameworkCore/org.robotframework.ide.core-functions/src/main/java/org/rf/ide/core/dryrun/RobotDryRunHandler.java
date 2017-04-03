/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.dryrun;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

import org.rf.ide.core.executor.RobotRuntimeEnvironment;
import org.rf.ide.core.executor.RunCommandLineCallBuilder;
import org.rf.ide.core.executor.RunCommandLineCallBuilder.IRunCommandLineBuilder;
import org.rf.ide.core.executor.RunCommandLineCallBuilder.RunCommandLine;
import org.rf.ide.core.executor.TestRunnerAgentHandler;

/**
 * @author mmarzec
 */
public class RobotDryRunHandler {

    private Process dryRunProcess;

    public RunCommandLine buildDryRunCommand(final RobotRuntimeEnvironment environment, final int port,
            final File projectLocation, final Collection<String> suites, final Collection<String> pythonPathLocations,
            final Collection<String> classPathLocations, final Collection<String> additionalProjectsLocations)
            throws IOException {

        final IRunCommandLineBuilder builder = RunCommandLineCallBuilder.forEnvironment(environment, port);

        builder.withProject(projectLocation);
        builder.suitesToRun(suites);
        builder.addLocationsToPythonPath(pythonPathLocations);
        builder.addLocationsToClassPath(classPathLocations);
        builder.enableDryRun();
        builder.withAdditionalProjectsLocations(additionalProjectsLocations);

        return builder.build();
    }

    public Thread createDryRunHandlerThread(final int port, final List<IAgentMessageHandler> listeners) {
        final TestRunnerAgentHandler testRunnerAgentHandler = new TestRunnerAgentHandler(port);
        for (final IAgentMessageHandler listener : listeners) {
            testRunnerAgentHandler.addListener(listener);
        }
        return new Thread(testRunnerAgentHandler);
    }

    public void executeDryRunProcess(final RunCommandLine dryRunCommandLine, final File projectDir)
            throws InvocationTargetException {
        if (dryRunCommandLine != null) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(dryRunCommandLine.getCommandLine());
                if (projectDir != null && projectDir.exists()) {
                    processBuilder = processBuilder.directory(projectDir);
                }
                dryRunProcess = processBuilder.start();
                drainProcessOutputAndErrorStreams(dryRunProcess);
                if (dryRunProcess != null) {
                    dryRunProcess.waitFor();
                }
            } catch (InterruptedException | IOException e) {
                throw new InvocationTargetException(e);
            }
        }
    }

    public void destroyDryRunProcess() {
        if (dryRunProcess != null) {
            dryRunProcess.destroy();
        }
    }

    public File createTempSuiteFile(final List<String> resourcesPaths, final List<String> libraryNames) {
        File file = null;
        PrintWriter printWriter = null;
        try {
            file = RobotRuntimeEnvironment.createTemporaryFile("DryRunTempSuite.robot");
            printWriter = new PrintWriter(file);
            printWriter.println("*** Test Cases ***");
            printWriter.println("T1");
            printWriter.println("*** Settings ***");
            for (final String path : resourcesPaths) {
                printWriter.println("Resource  " + path);
            }
            for (final String name : libraryNames) {
                printWriter.println("Library  " + name);
            }
        } catch (final IOException e) {
            // nothing to do
        } finally {
            if (printWriter != null) {
                printWriter.close();
            }
        }
        return file;
    }

    private void drainProcessOutputAndErrorStreams(final Process process) {
        new Thread(createStreamDrainRunnable(process.getInputStream())).start();
        new Thread(createStreamDrainRunnable(process.getErrorStream())).start();
    }

    private Runnable createStreamDrainRunnable(final InputStream inputStream) {
        return new Runnable() {

            @Override
            public void run() {
                try (final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line = reader.readLine();
                    while (line != null) {
                        line = reader.readLine();
                    }
                } catch (final IOException e) {
                    // nothing to do
                }
            }
        };
    }
}
