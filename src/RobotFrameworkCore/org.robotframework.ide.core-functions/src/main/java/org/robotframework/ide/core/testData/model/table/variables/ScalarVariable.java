/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.core.testData.model.table.variables;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken;


public class ScalarVariable extends AVariable {

    private final List<RobotToken> values = new ArrayList<>();


    public ScalarVariable(String name, RobotToken declaration,
            VariableScope scope) {
        super(VariableType.SCALAR, name, declaration, scope);
    }


    public void addValue(final RobotToken value) {
        values.add(value);
    }


    public List<RobotToken> getValues() {
        return Collections.unmodifiableList(values);
    }


    @Override
    public boolean isPresent() {
        return (getDeclaration() != null);
    }


    @Override
    public VariableType getType() {
        if (values.size() >= 2) {
            this.type = VariableType.SCALAR_AS_LIST;
        } else {
            this.type = VariableType.SCALAR;
        }

        return type;
    }


    @Override
    public List<RobotToken> getElementTokens() {
        List<RobotToken> tokens = new ArrayList<>();
        if (isPresent()) {
            tokens.add(getDeclaration());
            tokens.addAll(getValues());
            tokens.addAll(getComment());
        }

        return tokens;
    }
}
