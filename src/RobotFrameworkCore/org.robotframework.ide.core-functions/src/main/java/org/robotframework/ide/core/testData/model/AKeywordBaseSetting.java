/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.core.testData.model;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken;


public abstract class AKeywordBaseSetting<T> extends AModelElement<T> {

    private final RobotToken declaration;
    private RobotToken keywordName;
    private final List<RobotToken> arguments = new ArrayList<>();
    private final List<RobotToken> comment = new ArrayList<>();


    protected AKeywordBaseSetting(final RobotToken declaration) {
        this.declaration = declaration;
    }


    @Override
    public boolean isPresent() {
        return (declaration != null);
    }


    public RobotToken getDeclaration() {
        return declaration;
    }


    public RobotToken getKeywordName() {
        return keywordName;
    }


    public void setKeywordName(RobotToken keywordName) {
        this.keywordName = keywordName;
    }


    public List<RobotToken> getArguments() {
        return Collections.unmodifiableList(arguments);
    }


    public void addArgument(final RobotToken argument) {
        arguments.add(argument);
    }


    public List<RobotToken> getComment() {
        return Collections.unmodifiableList(comment);
    }


    public void addCommentPart(final RobotToken rt) {
        this.comment.add(rt);
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
            tokens.addAll(getArguments());
            tokens.addAll(getComment());
        }

        return tokens;
    }
}
