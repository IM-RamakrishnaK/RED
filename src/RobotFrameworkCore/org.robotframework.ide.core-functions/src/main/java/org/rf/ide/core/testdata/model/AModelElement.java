/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model;

import java.util.List;

import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;

public abstract class AModelElement<T> implements IOptional, IChildElement<T> {

    private T parent;

    public abstract ModelType getModelType();

    public abstract FilePosition getBeginPosition();

    public abstract List<RobotToken> getElementTokens();

    public abstract RobotToken getDeclaration();

    public void setParent(T parent) {
        this.parent = parent;
    }

    public T getParent() {
        return parent;
    }

    public FilePosition getEndPosition() {
        FilePosition pos = FilePosition.createNotSet();
        if (isPresent()) {
            List<RobotToken> elementTokens = getElementTokens();

            int size = elementTokens.size();
            for (int i = size - 1; i >= 0; i--) {
                RobotToken robotToken = elementTokens.get(i);
                if (robotToken.getStartOffset() >= 0) {
                    int endColumn = robotToken.getEndColumn();
                    int length = endColumn - robotToken.getStartColumn();
                    FilePosition fp = robotToken.getFilePosition();
                    pos = new FilePosition(fp.getLine(), robotToken.getEndColumn(), fp.getOffset() + length);
                    break;
                }
            }
        }

        return pos;
    }
}
