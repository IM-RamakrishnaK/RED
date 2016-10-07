/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build.validation;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Before;
import org.junit.Test;
import org.robotframework.ide.core.executor.SuiteExecutor;
import org.robotframework.ide.core.testData.model.table.variables.IVariableHolder;
import org.robotframework.ide.eclipse.main.plugin.mockmodel.RobotSuiteFileCreator;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModel;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.model.RobotVariablesSection;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig.ReferencedLibrary;
import org.robotframework.ide.eclipse.main.plugin.project.build.ProblemPosition;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotArtifactsValidator.ModelUnitValidator;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.IProblemCause;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.VariablesProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.validation.MockReporter.Problem;
import org.robotframework.ide.eclipse.main.plugin.project.build.validation.versiondependent.VersionDependentValidators;
import org.robotframework.ide.eclipse.main.plugin.project.library.LibrarySpecification;

import com.google.common.collect.Maps;
import com.google.common.collect.Range;

public class VariablesTableValidatorTest {

    private MockReporter reporter;

    @Before
    public void beforeTest() {
        reporter = new MockReporter();
    }

    @Test
    public void nothingIsReported_whenThereIsNoVariablesSection() throws CoreException {
        final RobotSuiteFile file = RobotSuiteFileCreator.createModel("");
        
        final FileValidationContext context = prepareContext(file);
        final VariablesTableValidator validator = new VariablesTableValidator(context,
                file.findSection(RobotVariablesSection.class), reporter, createVersionDependentValidators());
        validator.validate(null);

        assertThat(reporter.wasProblemReported()).isFalse();
    }

    @Test
    public void nothingIsReported_whenValidVariablesAreDefined() throws CoreException {
        final RobotSuiteFile file = RobotSuiteFileCreator.createModel(
                "*** Variables ***", 
                "${scalar}  1", 
                "@{list}  1",
                "&{dict}  1");

        final FileValidationContext context = prepareContext(file);
        final VariablesTableValidator validator = new VariablesTableValidator(context,
                file.findSection(RobotVariablesSection.class), reporter, createVersionDependentValidators());
        validator.validate(null);

        assertThat(reporter.wasProblemReported()).isFalse();
    }
    
    @Test
    public void customProblemsAreRaised_whenVersionDependentValidatorsAreUsed() throws CoreException {
        final RobotSuiteFile file = RobotSuiteFileCreator.createModel(
                "*** Variables ***", 
                "${scalar}  1");

        final IProblemCause mockedCause = mock(IProblemCause.class);
        final ModelUnitValidator alwaysFailingVersionDepValidator_1 = new ModelUnitValidator() {
            @Override
            public void validate(final IProgressMonitor monitor) throws CoreException {
                throw new ValidationProblemException(RobotProblem.causedBy(mockedCause), false);
            }
        };
        final ModelUnitValidator alwaysFailingVersionDepValidator_2 = new ModelUnitValidator() {
            @Override
            public void validate(final IProgressMonitor monitor) throws CoreException {
                throw new ValidationProblemException(RobotProblem.causedBy(mockedCause), true);
            }
        };
        final ModelUnitValidator alwaysPassingVersionDepValidator = new ModelUnitValidator() {
            @Override
            public void validate(final IProgressMonitor monitor) throws CoreException {
                // that's fine it passes
            }
        };
        final VersionDependentValidators versionValidators = createVersionDependentValidators(
                alwaysFailingVersionDepValidator_1, alwaysFailingVersionDepValidator_2,
                alwaysPassingVersionDepValidator);
        final FileValidationContext context = prepareContext(file);
        final VariablesTableValidator validator = new VariablesTableValidator(context,
                file.findSection(RobotVariablesSection.class), reporter, versionValidators);
        validator.validate(null);

        assertThat(reporter.getNumberOfReportedProblems()).isEqualTo(2);
        assertThat(reporter.getReportedProblems()).containsOnly(
                new Problem(mockedCause, new ProblemPosition(2, Range.closed(18, 27))),
                new Problem(mockedCause, new ProblemPosition(2, Range.closed(18, 30))));
    }

    @Test
    public void unrecognizedVariableIsReported() throws CoreException {
        final RobotSuiteFile file = RobotSuiteFileCreator.createModel(
                "*** Variables ***", 
                "var  1");

        final FileValidationContext context = prepareContext(file);
        final VariablesTableValidator validator = new VariablesTableValidator(context,
                file.findSection(RobotVariablesSection.class), reporter, createVersionDependentValidators());
        validator.validate(null);

        assertThat(reporter.getNumberOfReportedProblems()).isEqualTo(1);
        assertThat(reporter.getReportedProblems()).containsExactly(
                new Problem(VariablesProblem.INVALID_TYPE, new ProblemPosition(2, Range.closed(18, 21))));
    }

    @Test
    public void invalidVariableNameIsReported() throws CoreException {
        final RobotSuiteFile file = RobotSuiteFileCreator.createModel(
                "*** Variables ***", 
                "$ {var}  1");

        final FileValidationContext context = prepareContext(file);
        final VariablesTableValidator validator = new VariablesTableValidator(context,
                file.findSection(RobotVariablesSection.class), reporter, createVersionDependentValidators());
        validator.validate(null);

        assertThat(reporter.getNumberOfReportedProblems()).isEqualTo(1);
        assertThat(reporter.getReportedProblems()).containsExactly(
                new Problem(VariablesProblem.INVALID_NAME, new ProblemPosition(2, Range.closed(18, 25))));
    }

    @Test
    public void duplicatedVariablesAreReported() throws CoreException {
        final RobotSuiteFile file = RobotSuiteFileCreator.createModel(
                "*** Variables ***", 
                "${var}  1",
                "@{var}  2");

        final FileValidationContext context = prepareContext(file);
        final VariablesTableValidator validator = new VariablesTableValidator(context,
                file.findSection(RobotVariablesSection.class), reporter, createVersionDependentValidators());
        validator.validate(null);

        assertThat(reporter.getNumberOfReportedProblems()).isEqualTo(2);
        assertThat(reporter.getReportedProblems()).containsOnly(
                new Problem(VariablesProblem.DUPLICATED_VARIABLE, new ProblemPosition(2, Range.closed(18, 24))),
                new Problem(VariablesProblem.DUPLICATED_VARIABLE, new ProblemPosition(3, Range.closed(28, 34))));
    }
    
    @Test
    public void multipleProblemsAreReported() throws CoreException {
        final RobotSuiteFile file = RobotSuiteFileCreator.createModel(
                "*** Variables ***",
                "scalar  1",
                "$ {x}  1",
                "${var}  1",
                "@{var}  2");

        final FileValidationContext context = prepareContext(file);
        final VariablesTableValidator validator = new VariablesTableValidator(context,
                file.findSection(RobotVariablesSection.class), reporter, createVersionDependentValidators());
        validator.validate(null);

        assertThat(reporter.getNumberOfReportedProblems()).isEqualTo(4);
        assertThat(reporter.getReportedProblems()).containsOnly(
                new Problem(VariablesProblem.INVALID_TYPE, new ProblemPosition(2, Range.closed(18, 24))),
                new Problem(VariablesProblem.INVALID_NAME, new ProblemPosition(3, Range.closed(28, 33))),
                new Problem(VariablesProblem.DUPLICATED_VARIABLE, new ProblemPosition(4, Range.closed(37, 43))),
                new Problem(VariablesProblem.DUPLICATED_VARIABLE, new ProblemPosition(5, Range.closed(47, 53))));
        
    }

    private static VersionDependentValidators createVersionDependentValidators(final ModelUnitValidator... validators) {
        return new VersionDependentValidators() {
            @Override
            public List<? extends ModelUnitValidator> getVariableValidators(final IVariableHolder variable,
                    final RobotVersion version) {
                return newArrayList(validators);
            }
        };
    }

    private static FileValidationContext prepareContext(final RobotSuiteFile file) {
        final ValidationContext parentContext = new ValidationContext(new RobotModel(), RobotVersion.from("0.0"),
                SuiteExecutor.Python, Maps.<String, LibrarySpecification> newHashMap(),
                Maps.<ReferencedLibrary, LibrarySpecification> newHashMap());
        final FileValidationContext context = new FileValidationContext(parentContext, file.getFile());
        return context;
    }
}
