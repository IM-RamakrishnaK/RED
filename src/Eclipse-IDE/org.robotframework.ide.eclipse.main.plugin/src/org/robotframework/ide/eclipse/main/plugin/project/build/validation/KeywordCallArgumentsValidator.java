/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build.validation;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.robotframework.ide.eclipse.main.plugin.project.build.ProblemsReportingStrategy;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotArtifactsValidator.ModelUnitValidator;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.ArgumentProblem;
import org.robotframework.ide.eclipse.main.plugin.project.library.ArgumentsDescriptor;

import com.google.common.base.Optional;
import com.google.common.collect.Range;

/**
 * @author Michal Anglart
 *
 */
class KeywordCallArgumentsValidator implements ModelUnitValidator {

    private final IFile file;

    private final RobotToken definingToken;

    private final ProblemsReportingStrategy reporter;

    private final Optional<ArgumentsDescriptor> descriptor;

    private final List<RobotToken> arguments;


    KeywordCallArgumentsValidator(final IFile file, final RobotToken definingToken,
            final ProblemsReportingStrategy reporter,
            final Optional<ArgumentsDescriptor> descriptor, final List<RobotToken> arguments) {
        this.file = file;
        this.definingToken = definingToken;
        this.reporter = reporter;
        this.descriptor = descriptor;
        this.arguments = arguments;
    }

    @Override
    public void validate(final IProgressMonitor monitor) throws CoreException {
        final Range<Integer> possibleArgsNumber = descriptor.isPresent()
                ? descriptor.get().getPossibleNumberOfArguments()
                : Range.closed(0, 0);
        if (!possibleArgsNumber.contains(arguments.size())) {
            final String argumentsForm = descriptor.isPresent() ? descriptor.get().getDescription() : "[]";
            final String additionalMsg = String.format(" The '%s' accepts argument in form: %s",
                    definingToken.getText().toString(), argumentsForm);
            final RobotProblem problem = RobotProblem.causedBy(ArgumentProblem.INVALID_NUMBER_OF_PARAMETERS)
                    .formatMessageWith(additionalMsg);
            reporter.handleProblem(problem, file, definingToken);
        }
    }

}
