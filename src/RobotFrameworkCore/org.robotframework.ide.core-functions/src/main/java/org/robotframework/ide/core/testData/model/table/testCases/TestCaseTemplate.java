/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.core.testData.model.table.testCases;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import org.robotframework.ide.core.testData.model.AModelElement;
import org.robotframework.ide.core.testData.model.FilePosition;
import org.robotframework.ide.core.testData.model.IDataDrivenSetting;
import org.robotframework.ide.core.testData.model.ModelType;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken;


public class TestCaseTemplate extends AModelElement<TestCase> implements
        IDataDrivenSetting {

    private final RobotToken declaration;
    private RobotToken keywordName;
    private final List<RobotToken> unexpectedTrashArguments = new ArrayList<>();

    private final List<RobotToken> comment = new ArrayList<>();


    public TestCaseTemplate(final RobotToken declaration) {
        this.declaration = declaration;
    }


    @Override
    public boolean isPresent() {
        return (declaration != null);
    }


    @Override
    public RobotToken getDeclaration() {
        return declaration;
    }


    @Override
    public RobotToken getKeywordName() {
        return keywordName;
    }


    public void setKeywordName(RobotToken keywordName) {
        this.keywordName = keywordName;
    }


    @Override
    public List<RobotToken> getUnexpectedTrashArguments() {
        return Collections.unmodifiableList(unexpectedTrashArguments);
    }


    public void addUnexpectedTrashArgument(final RobotToken trashArgument) {
        this.unexpectedTrashArguments.add(trashArgument);
    }


    @Override
    public List<RobotToken> getComment() {
        return Collections.unmodifiableList(comment);
    }


    public void addCommentPart(final RobotToken rt) {
        this.comment.add(rt);
    }


    @Override
    public ModelType getModelType() {
        return ModelType.TEST_CASE_TEMPLATE;
    }


    @Override
    public FilePosition getBeginPosition() {
        return getDeclaration().getFilePosition();
    }


    @Override
    public List<RobotToken> getElementTokens() {
        List<RobotToken> tokens = new ArrayList<>();
        if (isPresent()) {
            tokens.add(getDeclaration());
            if (getKeywordName() != null) {
                tokens.add(getKeywordName());
            }
            tokens.addAll(getUnexpectedTrashArguments());
            tokens.addAll(getComment());
        }

        return tokens;
    }
}
