/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testData.model.mapping.hashComment.tableTestCase;

import java.util.List;

import org.rf.ide.core.testData.model.RobotFile;
import org.rf.ide.core.testData.model.mapping.IHashCommentMapper;
import org.rf.ide.core.testData.model.table.testCases.TestCase;
import org.rf.ide.core.testData.model.table.testCases.TestDocumentation;
import org.rf.ide.core.testData.text.read.ParsingState;
import org.rf.ide.core.testData.text.read.recognizer.RobotToken;


public class TestCaseSettingDocumentationCommentMapper implements
        IHashCommentMapper {

    @Override
    public boolean isApplicable(ParsingState state) {
        return (state == ParsingState.TEST_CASE_SETTING_DOCUMENTATION_DECLARATION || state == ParsingState.TEST_CASE_SETTING_DOCUMENTATION_TEXT);
    }


    @Override
    public void map(RobotToken rt, ParsingState currentState,
            RobotFile fileModel) {
        List<TestCase> testCases = fileModel.getTestCaseTable().getTestCases();
        TestCase testCase = testCases.get(testCases.size() - 1);

        List<TestDocumentation> documentation = testCase.getDocumentation();
        TestDocumentation testDocumentation = documentation.get(documentation
                .size() - 1);
        testDocumentation.addCommentPart(rt);
    }
}
