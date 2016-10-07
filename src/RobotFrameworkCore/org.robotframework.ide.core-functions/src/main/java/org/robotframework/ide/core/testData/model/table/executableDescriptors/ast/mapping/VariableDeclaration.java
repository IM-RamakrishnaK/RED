/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.core.testData.model.table.executableDescriptors.ast.mapping;

import java.util.List;
import java.util.regex.Pattern;

import org.robotframework.ide.core.testData.model.table.executableDescriptors.TextPosition;
import org.robotframework.ide.core.testData.model.table.variables.AVariable.VariableType;


public class VariableDeclaration extends AContainerOperation {

    private final static Pattern COMPUTATION_PATTERN = Pattern
            .compile("((?!\\s).)+(\\s)*([+]|[-]|[*]|[/]|[:]|[>]|[<])[=]*(\\s)*((?!\\s).)+");
    private final static Pattern NUMBER_PATTERN = Pattern
            .compile("^(//s)*([+]|[-])?(([0-9])+)([.]([0-9])+)?(//s)*$");
    private final static Pattern BINARY_NUMBER_PATTERN = Pattern
            .compile("^(//s)*0[b|B](0|1)+(//s)*$");
    private final static Pattern OCTAL_NUMBER_PATTERN = Pattern
            .compile("^(//s)*0[o|O][0-8]+(//s)*$");
    private final static Pattern HEX_NUMBER_PATTERN = Pattern
            .compile("^(//s)*0[x|X]([0-9]|[a-f]|[A-F])+(//s)*$");
    private final static Pattern EXPONENT_NUMBER_PATTERN = Pattern
            .compile("^(//s)*([+]|[-])?(([0-9])+)[e|E]([+]|[-])?([0-9])+(//s)*$");
    private final static Pattern PYTHON_METHOD_INVOKE_PATTERN = Pattern
            .compile("^(//s)*([a-z]|[A-Z]).*[.].+([(].*[)])$");
    private final static Pattern PYTHON_GET_INVOKE_PATTERN = Pattern
            .compile("^(//s)*([a-z]|[A-Z]).*[.].+$");

    private IElementDeclaration levelUpElement;
    private TextPosition escape;
    private TextPosition variableIdentificator;
    private final TextPosition variableStart;
    private final TextPosition variableEnd;


    public VariableDeclaration(final TextPosition variableStart,
            final TextPosition variableEnd) {
        this.variableStart = variableStart;
        this.variableEnd = variableEnd;
    }


    public boolean isEscaped() {
        return (escape != null);
    }


    public TextPosition getEscape() {
        return escape;
    }


    public void setEscape(final TextPosition escape) {
        this.escape = escape;
    }


    public TextPosition getTypeIdentficator() {
        return variableIdentificator;
    }


    public VariableType getRobotType() {
        char c = (char) -1;
        String text = getTypeIdentficator().getText();
        if (!text.isEmpty()) {
            c = text.charAt(0);
        }

        VariableType robotType = VariableType.getTypeByChar(c);

        return robotType;
    }


    public void setTypeIdentificator(final TextPosition variableIdentficator) {
        this.variableIdentificator = variableIdentficator;
    }


    @Override
    public TextPosition getStart() {
        return variableStart;
    }


    public TextPosition getVariableName() {
        TextPosition varName = null;
        if (!isDynamic()) {
            JoinedTextDeclarations nameJoined = new JoinedTextDeclarations();
            List<IElementDeclaration> elementsDeclarationInside = super
                    .getElementsDeclarationInside();
            for (IElementDeclaration elem : elementsDeclarationInside) {
                if (elem.isComplex()) {
                    break;
                } else {
                    nameJoined.addElementDeclarationInside(elem);
                }
            }

            varName = nameJoined.join();
        }

        return varName;
    }


    public TextPosition getObjectName() {
        TextPosition objectName = null;
        if (getVariableType() == GeneralVariableType.PYTHON_SPECIFIC_INVOKE_VALUE_GET) {
            TextPosition variableName = getVariableName();
            String variableText = variableName.getText();
            int dotCharPos = variableText.indexOf('.');
            if (dotCharPos >= 0) {
                objectName = new TextPosition(variableName.getFullText(),
                        variableName.getStart(), variableName.getStart()
                                + dotCharPos - 1);
            }
        }
        return objectName;
    }


    /**
     * check if variable depends on other variables
     * 
     * @return
     */
    public boolean isDynamic() {
        boolean result = false;
        List<IElementDeclaration> elementsDeclarationInside = super
                .getElementsDeclarationInside();
        for (IElementDeclaration iElementDeclaration : elementsDeclarationInside) {
            if (iElementDeclaration instanceof VariableDeclaration) {
                result = true;
                break;
            }
        }

        return result;
    }


    @Override
    public TextPosition getEnd() {
        return variableEnd;
    }


    public void setLevelUpElement(final IElementDeclaration levelUpElement) {
        this.levelUpElement = levelUpElement;
    }


    @Override
    public IElementDeclaration getLevelUpElement() {
        return levelUpElement;
    }


    @Override
    public boolean isComplex() {
        return true;
    }


    public IVariableType getVariableType() {
        IVariableType type = null;
        if (isDynamic()) {
            type = GeneralVariableType.DYNAMIC;
        } else {
            String variableNameText = getVariableName().getText();
            VariableType varType = getRobotType();
            if (varType == VariableType.SCALAR
                    || varType == VariableType.SCALAR_AS_LIST) {
                if (EXPONENT_NUMBER_PATTERN.matcher(variableNameText).find()) {
                    type = Number.EXPONENT_NUMBER;
                } else if (COMPUTATION_PATTERN.matcher(variableNameText).find()) {
                    type = GeneralVariableType.COMPUTATION;
                } else if (NUMBER_PATTERN.matcher(variableNameText).find()) {
                    type = Number.NORMAL_NUMBER;
                } else if (BINARY_NUMBER_PATTERN.matcher(variableNameText)
                        .find()) {
                    type = Number.BINARY_NUMBER;
                } else if (OCTAL_NUMBER_PATTERN.matcher(variableNameText)
                        .find()) {
                    type = Number.OCTAL_NUMBER;
                } else if (HEX_NUMBER_PATTERN.matcher(variableNameText).find()) {
                    type = Number.HEX_NUMBER;
                } else {
                    if (PYTHON_METHOD_INVOKE_PATTERN.matcher(variableNameText)
                            .find()) {
                        type = GeneralVariableType.PYTHON_SPECIFIC_INVOKE_METHOD;
                    } else if (PYTHON_GET_INVOKE_PATTERN.matcher(
                            variableNameText).find()) {
                        type = GeneralVariableType.PYTHON_SPECIFIC_INVOKE_VALUE_GET;
                    }
                }
            }

            if (type == null) {
                type = GeneralVariableType.NORMAL_TEXT;
            }
        }

        return type;
    }

    public interface IVariableType {

    }

    public enum GeneralVariableType implements IVariableType {
        DYNAMIC, NORMAL_TEXT, PYTHON_SPECIFIC_INVOKE_VALUE_GET, PYTHON_SPECIFIC_INVOKE_METHOD, COMPUTATION;
    }

    public enum Number implements IVariableType {
        NORMAL_NUMBER, BINARY_NUMBER, OCTAL_NUMBER, HEX_NUMBER, EXPONENT_NUMBER;
    }
}
