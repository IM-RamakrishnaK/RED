/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build.validation;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.Position;
import org.robotframework.ide.core.testData.model.table.RobotExecutableRow;
import org.robotframework.ide.core.testData.model.table.RobotExecutableRow.ExecutionLineDescriptor;
import org.robotframework.ide.core.testData.model.table.TestCaseTable;
import org.robotframework.ide.core.testData.model.table.testCases.TestCase;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken;
import org.robotframework.ide.eclipse.main.plugin.model.RobotCasesSection;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.project.build.ProblemPosition;
import org.robotframework.ide.eclipse.main.plugin.project.build.ProblemsReportingStrategy;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotArtifactsValidator.ModelUnitValidator;
import org.robotframework.ide.eclipse.main.plugin.project.build.RobotProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.IProblemCause;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.KeywordsProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.TestCasesProblem;
import org.robotframework.ide.eclipse.main.plugin.project.build.causes.VariablesProblem;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Range;

class TestCasesTableValidator implements ModelUnitValidator {

    private static final String VARIABLE_PATTERN = "[@$&%]\\{[^\\}]+\\}";

    private final FileValidationContext validationContext;

    private final Optional<RobotCasesSection> testCaseSection;

    private final ProblemsReportingStrategy reporter = new ProblemsReportingStrategy();

    TestCasesTableValidator(final FileValidationContext validationContext, final Optional<RobotCasesSection> section) {
        this.validationContext = validationContext;
        this.testCaseSection = section;
    }

    @Override
    public void validate(final IProgressMonitor monitor) throws CoreException {
        if (!testCaseSection.isPresent()) {
            return;
        }
        final RobotSuiteFile suiteModel = testCaseSection.get().getSuiteFile();
        final TestCaseTable casesTable = (TestCaseTable) testCaseSection.get().getLinkedElement();
        final List<TestCase> cases = casesTable.getTestCases();

        reportEmptyCases(suiteModel.getFile(), cases);
        reportDuplicatedCases(suiteModel.getFile(), cases);
        reportKeywordUsageProblems(suiteModel, validationContext, reporter, findExecutableRows(cases));
        reportUnknownVariables(suiteModel, cases);
    }

    private void reportEmptyCases(final IFile file, final List<TestCase> cases) {
        for (final TestCase testCase : cases) {
            final RobotToken caseName = testCase.getTestName();
            final IProblemCause cause = TestCasesProblem.EMPTY_CASE;
            reportEmptyExecutableRows(file, reporter, caseName, testCase.getTestExecutionRows(), cause);
        }
    }

    static void reportEmptyExecutableRows(final IFile file, final ProblemsReportingStrategy reporter,
            final RobotToken def, final List<? extends RobotExecutableRow<?>> executables,
            final IProblemCause causeToReport) {
        if (executables.isEmpty()) {
            final String name = def.getText().toString();
            final RobotProblem problem = RobotProblem.causedBy(causeToReport).formatMessageWith(name);
            final Map<String, Object> arguments = ImmutableMap.<String, Object> of("name", name);
            reporter.handleProblem(problem, file, def, arguments);
        }
    }

    private void reportDuplicatedCases(final IFile file, final List<TestCase> cases) {
        final Set<String> duplicatedNames = newHashSet();

        for (final TestCase case1 : cases) {
            for (final TestCase case2 : cases) {
                if (case1 != case2) {
                    final String case1Name = case1.getTestName().getText().toString();
                    final String case2Name = case2.getTestName().getText().toString();

                    if (case1Name.equalsIgnoreCase(case2Name)) {
                        duplicatedNames.add(case1Name.toLowerCase());
                    }
                }
            }
        }

        for (final TestCase testCase : cases) {
            final RobotToken caseName = testCase.getTestName();
            final String name = caseName.getText().toString();

            if (duplicatedNames.contains(name.toLowerCase())) {
                final RobotProblem problem = RobotProblem.causedBy(TestCasesProblem.DUPLICATED_CASE)
                        .formatMessageWith(name);
                final Map<String, Object> additionalArguments = ImmutableMap.<String, Object> of("name", name);
                reporter.handleProblem(problem, file, caseName, additionalArguments);
            }
        }

    }

    private static List<RobotExecutableRow<?>> findExecutableRows(final List<TestCase> cases) {
        final List<RobotExecutableRow<?>> executables = newArrayList();
        for (final TestCase testCase : cases) {
            executables.addAll(testCase.getTestExecutionRows());
        }
        return executables;
    }

    static void reportKeywordUsageProblems(final RobotSuiteFile robotSuiteFile, final FileValidationContext validationContext,
            final ProblemsReportingStrategy reporter, final List<RobotExecutableRow<?>> executables) {
        
        for (final RobotExecutableRow<?> executable : executables) {
            // FIXME : this disables for loops validating, enable asap
            if (executable.getAction().getText().toString().toLowerCase().replaceAll(" ", "").equals(":for")) {
                continue;
            } else if (executable.getAction().getText().toString().toLowerCase().replaceAll(" ", "").equals("\\")) {
                continue;
            }
            // remove this thing up from here

            if (!executable.isExecutable()) {
                continue;
            }
            final RobotToken keywordName = executable.buildLineDescription().getFirstAction();
            if (!keywordName.getFilePosition().isNotSet()) {
                final String name = keywordName.getText().toString();

                if (!validationContext.isKeywordAccessible(name)) {
                    final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.UNKNOWN_KEYWORD)
                            .formatMessageWith(name);
                    final Map<String, Object> additionalArguments = ImmutableMap.<String, Object> of("name", name);
                    reporter.handleProblem(problem, robotSuiteFile.getFile(), keywordName, additionalArguments);
                }
                if (validationContext.isKeywordDeprecated(name)) {
                    final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.DEPRECATED_KEYWORD)
                            .formatMessageWith(name);
                    reporter.handleProblem(problem, robotSuiteFile.getFile(), keywordName);
                }
                if (validationContext.isKeywordFromNestedLibrary(name)) {
                    final RobotProblem problem = RobotProblem.causedBy(KeywordsProblem.KEYWORD_FROM_NESTED_LIBRARY)
                            .formatMessageWith(name);
                    reporter.handleProblem(problem, robotSuiteFile.getFile(), keywordName);
                }
                
            } else {

            }
        }
    }

    private void reportUnknownVariables(final RobotSuiteFile suiteModel,
            final List<TestCase> cases) {        
        final Set<String> variables = validationContext.getAccessibleVariables();

        for (final TestCase testCase : cases) {
            reportUnknownVariables(suiteModel.getFile(), reporter, testCase.getTestExecutionRows(), variables);
        }
    }

    static void reportUnknownVariables(final IFile file, final ProblemsReportingStrategy reporter,
            final List<? extends RobotExecutableRow<?>> executables, final Set<String> variables) {
        final Set<String> definedVariables = newHashSet(variables);

        for (final RobotExecutableRow<?> row : executables) {
            // FIXME : this disables for loops validating, enable asap
            if (row.getAction().getText().toString().toLowerCase().replaceAll(" ", "").equals(":for")) {
                continue;
            } else if (row.getAction().getText().toString().toLowerCase().replaceAll(" ", "").equals("\\")) {
                continue;
            }
            // remove this thing up from here

            final ExecutionLineDescriptor lineDescription = row.buildLineDescription();

            final List<VariableInsideCell> usedParameters = extractVariables(lineDescription.getParameters());
            for (final VariableInsideCell usedParameter : usedParameters) {
                if (!definedVariables.contains(usedParameter.name.toLowerCase())) {
                    final RobotProblem problem = RobotProblem.causedBy(VariablesProblem.UNDECLARED_VARIABLE_USE)
                            .formatMessageWith(usedParameter.name);
                    final ProblemPosition position = new ProblemPosition(lineDescription.getFirstAction().getLineNumber(),
                            Range.closed(usedParameter.position.offset,
                                    usedParameter.position.offset + usedParameter.position.length));
                    final Map<String, Object> additionalArguments = ImmutableMap.<String, Object> of("name",
                            usedParameter.name);
                    reporter.handleProblem(problem, file, position, additionalArguments);
                }
            }
            definedVariables.addAll(extractVariableNames(lineDescription.getAssignments()));
        }
    }

    @VisibleForTesting
    static List<String> extractVariableNames(final List<RobotToken> assignments) {
        return newArrayList(
                Iterables.transform(extractVariables(assignments), new Function<VariableInsideCell, String>() {
                    @Override
                    public String apply(final VariableInsideCell var) {
                        return var.name.toLowerCase();
                    }
                }));
    }

    private static List<VariableInsideCell> extractVariables(final List<RobotToken> tokens) {
        final List<VariableInsideCell> vars = newArrayList();

        for (final RobotToken token : tokens) {
            vars.addAll(extractVariables(token.getText().toString(), token.getStartOffset()));
        }
        return vars;
    }

    private static List<VariableInsideCell> extractVariables(final String content, final int cellOffset) {
        final List<VariableInsideCell> variables = newArrayList();
        final Matcher matcher = Pattern.compile(VARIABLE_PATTERN).matcher(content);
        
        while (matcher.find()) {
            final String name = matcher.group(0);
            final Position position = new Position(cellOffset + matcher.start(), name.length());
            variables.add(new VariableInsideCell(name, position));
        }
        return variables;
    }

    private static class VariableInsideCell {

        private final String name;

        private final Position position;

        VariableInsideCell(final String name, final Position position) {
            this.name = name;
            this.position = position;
        }
    }
}
