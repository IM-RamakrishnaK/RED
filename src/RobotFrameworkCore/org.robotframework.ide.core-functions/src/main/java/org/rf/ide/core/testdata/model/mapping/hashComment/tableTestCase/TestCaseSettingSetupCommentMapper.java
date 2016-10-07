/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.mapping.hashComment.tableTestCase;

import java.util.List;

import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.mapping.IHashCommentMapper;
import org.rf.ide.core.testdata.model.table.testCases.TestCase;
import org.rf.ide.core.testdata.model.table.testCases.TestCaseSetup;
import org.rf.ide.core.testdata.text.read.ParsingState;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;


public class TestCaseSettingSetupCommentMapper implements IHashCommentMapper {

    @Override
    public boolean isApplicable(ParsingState state) {
        return (state == ParsingState.TEST_CASE_SETTING_SETUP
                || state == ParsingState.TEST_CASE_SETTING_SETUP_KEYWORD || state == ParsingState.TEST_CASE_SETTING_SETUP_KEYWORD_ARGUMENT);
    }


    @Override
    public void map(RobotToken rt, ParsingState currentState,
            RobotFile fileModel) {
        List<TestCase> testCases = fileModel.getTestCaseTable().getTestCases();
        TestCase testCase = testCases.get(testCases.size() - 1);

        List<TestCaseSetup> setups = testCase.getSetups();
        TestCaseSetup testCaseSetup = setups.get(setups.size() - 1);
        testCaseSetup.addCommentPart(rt);
    }

}
