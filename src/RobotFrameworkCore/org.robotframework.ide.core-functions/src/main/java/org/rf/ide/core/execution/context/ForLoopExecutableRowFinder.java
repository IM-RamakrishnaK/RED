/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.execution.context;

import java.util.ArrayList;
import java.util.List;

import org.rf.ide.core.execution.context.RobotDebugExecutionContext.KeywordContext;
import org.rf.ide.core.execution.context.RobotDebugExecutionContext.TestCaseExecutionRowCounter;
import org.rf.ide.core.testdata.model.table.RobotExecutableRow;
import org.rf.ide.core.testdata.model.table.testcases.TestCase;


/**
 * @author mmarzec
 * 
 */
public class ForLoopExecutableRowFinder implements IRobotExecutableRowFinder {

    private TestCase currentTestCase;

    private TestCaseExecutionRowCounter testCaseExecutionRowCounter;
    
    private IRobotExecutableRowFinder userKeywordExecutableRowFinder;
    
    private IRobotExecutableRowFinder nestedForLoopExecutableRowFinder;

    private final List<RobotExecutableRow<?>> forLoopExecutableRows;

    private int forLoopExecutableRowsCounter = 0;

    private int currentKeywordsSizeAtLoopStart = 0;


    public ForLoopExecutableRowFinder(final TestCase currentTestCase,
            final TestCaseExecutionRowCounter testCaseExecutionRowCounter,
            final IRobotExecutableRowFinder userKeywordExecutableRowFinder) {
        this.currentTestCase = currentTestCase;
        this.testCaseExecutionRowCounter = testCaseExecutionRowCounter;
        this.userKeywordExecutableRowFinder = userKeywordExecutableRowFinder;
        forLoopExecutableRows = new ArrayList<>();
    }

    @Override
    public RobotExecutableRow<?> findExecutableRow(final List<KeywordContext> currentKeywords) {

        if (forLoopExecutableRows.isEmpty()) {
            currentKeywordsSizeAtLoopStart = currentKeywords.size();
            KeywordContext parentKeywordContext = null;
            final int forLoopParentKeywordContextPosition = getForLoopParentKeywordContextPosition(currentKeywords);
            if (forLoopParentKeywordContextPosition >= 0) {
                parentKeywordContext = currentKeywords.get(forLoopParentKeywordContextPosition);
            }
            fillForLoopExecutableRows(parentKeywordContext);
            return forLoopExecutableRows.get(0);
        }

        if (currentKeywords.size() > (currentKeywordsSizeAtLoopStart + 1)) { // step into the keyword placed inside a for loop
            return findExecutableRowForUserKeywordNestedInForLoop(currentKeywords);
        }
        nestedForLoopExecutableRowFinder = null;

        forLoopExecutableRowsCounter++;
        if (hasReachedNextForLoopIteration()) {
            forLoopExecutableRowsCounter = 0;
            return forLoopExecutableRows.get(0);
        }

        return forLoopExecutableRows.get(forLoopExecutableRowsCounter);
    }


    public void clear() {
        forLoopExecutableRows.clear();
        forLoopExecutableRowsCounter = 0;
        currentKeywordsSizeAtLoopStart = 0;
    }
    
    private int getForLoopParentKeywordContextPosition(
            final List<KeywordContext> currentKeywords) {
        return currentKeywords.size() - 3;
    }

    private void fillForLoopExecutableRows(final KeywordContext parentKeywordContext) {
        final List<RobotExecutableRow<?>> executableRows = extractAllExecutableRows(parentKeywordContext);
        final int forLoopStartPosition = extractForLoopStartPosition(parentKeywordContext);
        forLoopExecutableRows.add(executableRows.get(forLoopStartPosition));
        for (int i = forLoopStartPosition + 1; i < executableRows.size(); i++) {
            if (executableRows.get(i).isExecutable()) {
                if (isForLoopItem(executableRows.get(i))) {
                    forLoopExecutableRows.add(executableRows.get(i));
                } else {
                    break;
                }
            }
            incrementExecutionRowCounterInsideForLoop(parentKeywordContext);
        }
    }
    
    private List<RobotExecutableRow<?>> extractAllExecutableRows(final KeywordContext parentKeywordContext) {
        final List<RobotExecutableRow<?>> executableRows = new ArrayList<>();
        if (parentKeywordContext != null && parentKeywordContext.getUserKeyword() != null) {
            executableRows.addAll(parentKeywordContext.getUserKeyword().getKeywordExecutionRows());
        } else {
            executableRows.addAll(currentTestCase.getTestExecutionRows());
        }
        return executableRows;
    }
    
    private int extractForLoopStartPosition(final KeywordContext parentKeywordContext) {
        int counter;
        if (parentKeywordContext != null && parentKeywordContext.getUserKeyword() != null) {
            counter = parentKeywordContext.getKeywordExecutableRowCounter() - 1;
        } else {
            counter = testCaseExecutionRowCounter.getCounter() - 1;
        }
        return counter;
    }

    private void incrementExecutionRowCounterInsideForLoop(final KeywordContext parentKeywordContext) {
        if (parentKeywordContext != null && parentKeywordContext.getUserKeyword() != null) {
            parentKeywordContext.incrementKeywordExecutableRowCounter();
        } else {
            testCaseExecutionRowCounter.increment();
        }
    }
    
    private boolean isForLoopItem(final RobotExecutableRow<?> executableRow) {
        return executableRow.getAction().getText().toString().equals("\\");
    }

    private RobotExecutableRow<?> findExecutableRowForUserKeywordNestedInForLoop(final List<KeywordContext> currentKeywords) {
        RobotExecutableRow<?> executableRow = null;
        if (userKeywordExecutableRowFinder != null) {
            executableRow = userKeywordExecutableRowFinder.findExecutableRow(currentKeywords);
            if (executableRow == null) {
                // for loop nested in another for loop
                if (nestedForLoopExecutableRowFinder == null) {
                    nestedForLoopExecutableRowFinder = new ForLoopExecutableRowFinder(currentTestCase,
                            new TestCaseExecutionRowCounter(), userKeywordExecutableRowFinder);
                }
                executableRow = nestedForLoopExecutableRowFinder.findExecutableRow(currentKeywords);
            }
        }
        return executableRow;
    }
    
    private boolean hasReachedNextForLoopIteration() {
        return forLoopExecutableRowsCounter == forLoopExecutableRows.size();
    }


    public void setCurrentTestCase(final TestCase currentTestCase) {
        this.currentTestCase = currentTestCase;
    }


    public void setTestCaseExecutionRowCounter(final TestCaseExecutionRowCounter testCaseExecutionRowCounter) {
        this.testCaseExecutionRowCounter = testCaseExecutionRowCounter;
    }

}
