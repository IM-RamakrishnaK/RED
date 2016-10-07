/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;

public class FatalProblemsReportingStrategy extends ProblemsReportingStrategy {

    @Override
    public void handleProblem(final RobotProblem problem, final IFile file, final int line) {
        super.handleProblem(problem, file, line);
        throw new ReportingInterruptedException("Building and validation was interrupted by fatal problem");
    }

    @Override
    public void handleProblem(final RobotProblem problem, final IFile file, final int line,
            final Map<String, Object> additionalAttributes) throws ReportingInterruptedException {
        super.handleProblem(problem, file, line, additionalAttributes);
        throw new ReportingInterruptedException("Building and validation was interrupted by fatal problem");
    }

    @Override
    public void handleProblem(final RobotProblem problem, final IFile file, final RobotToken token) {
        super.handleProblem(problem, file, token);
        throw new ReportingInterruptedException("Building and validation was interrupted by fatal problem");
    }

    @Override
    public void handleProblem(final RobotProblem problem, final IFile file, final RobotToken token,
            final Map<String, Object> additionalAttributes) {
        super.handleProblem(problem, file, token, additionalAttributes);
        throw new ReportingInterruptedException("Building and validation was interrupted by fatal problem");
    }

    @Override
    public void handleProblem(final RobotProblem problem, final IFile file, final ProblemPosition filePosition) {
        super.handleProblem(problem, file, filePosition, new HashMap<String, Object>());
        throw new ReportingInterruptedException("Building and validation was interrupted by fatal problem");
    }

    @Override
    public void handleProblem(final RobotProblem problem, final IFile file, final ProblemPosition filePosition,
            final Map<String, Object> additionalAttributes) {
        super.handleProblem(problem, file, filePosition, additionalAttributes);
        throw new ReportingInterruptedException("Building and validation was interrupted by fatal problem");
    }
}
