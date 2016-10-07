/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.core.testData.model.table.variables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken;


public class UnknownVariable extends AVariable {

    private final List<RobotToken> items = new ArrayList<>();


    public UnknownVariable(final String name, final RobotToken declaration,
            final VariableScope scope) {
        super(VariableType.INVALID, name, declaration, scope);
    }


    public void addItem(final RobotToken item) {
        items.add(item);
    }


    public List<RobotToken> getItems() {
        return Collections.unmodifiableList(items);
    }


    @Override
    public boolean isPresent() {
        return true;
    }


    @Override
    public List<RobotToken> getElementTokens() {
        final List<RobotToken> tokens = new ArrayList<>();
        if (isPresent()) {
            if (getDeclaration() != null) {
                tokens.add(getDeclaration());
            }

            tokens.addAll(getItems());
            tokens.addAll(getComment());
        }

        return tokens;
    }
}
