/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.model;

import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Position;
import org.robotframework.ide.core.testData.model.FilePosition;
import org.robotframework.ide.core.testData.model.table.RobotExecutableRow;
import org.robotframework.ide.core.testData.model.table.userKeywords.KeywordArguments;
import org.robotframework.ide.core.testData.model.table.userKeywords.KeywordDocumentation;
import org.robotframework.ide.core.testData.model.table.userKeywords.KeywordReturn;
import org.robotframework.ide.core.testData.model.table.userKeywords.KeywordTags;
import org.robotframework.ide.core.testData.model.table.userKeywords.KeywordTeardown;
import org.robotframework.ide.core.testData.model.table.userKeywords.KeywordTimeout;
import org.robotframework.ide.core.testData.model.table.userKeywords.UserKeyword;
import org.robotframework.ide.core.testData.text.read.IRobotTokenType;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotTokenType;
import org.robotframework.ide.eclipse.main.plugin.RedImages;

import com.google.common.collect.Lists;

public class RobotKeywordDefinition extends RobotCodeHoldingElement {

    public static final String ARGUMENTS = "Arguments";
    public static final String DOCUMENTATION = "Documentation";
    public static final String TAGS = "Tags";
    public static final String TIMEOUT = "Timeout";
    public static final String TEARDOWN = "Teardown";
    public static final String RETURN = "Return";

    private UserKeyword keyword;
    
    RobotKeywordDefinition(final RobotKeywordsSection parent, final String name, final String comment) {
        super(parent, name, comment);
    }

    public void link(final UserKeyword keyword) {
        this.keyword = keyword;
        // body
        for (final RobotExecutableRow<UserKeyword> execRow : keyword.getKeywordExecutionRows()) {
            final String callName = execRow.getAction().getText().toString();
            final List<String> args = newArrayList(
                    Lists.transform(execRow.getArguments(), TokenFunctions.tokenToString()));
            final RobotKeywordCall call = new RobotKeywordCall(this, callName, args, "");
            getChildren().add(call);
            call.link(execRow);
        }
        // settings
        for (final KeywordArguments argument : keyword.getArguments()) {
            final String name = argument.getDeclaration().getText().toString();
            final List<String> args = newArrayList(
                    Lists.transform(argument.getArguments(), TokenFunctions.tokenToString()));
            final RobotDefinitionSetting setting = new RobotDefinitionSetting(this, omitSquareBrackets(name), args, "");
            setting.link(argument);
            getChildren().add(setting);
        }
        for (final KeywordDocumentation documentation : keyword.getDocumentation()) {
            final String name = documentation.getDeclaration().getText().toString();
            final List<String> args = newArrayList(
                    Lists.transform(documentation.getDocumentationText(), TokenFunctions.tokenToString()));
            final RobotDefinitionSetting setting = new RobotDefinitionSetting(this, omitSquareBrackets(name), args, "");
            setting.link(documentation);
            getChildren().add(setting);
        }
        for (final KeywordTags tags : keyword.getTags()) {
            final String name = tags.getDeclaration().getText().toString();
            final List<String> args = newArrayList(Lists.transform(tags.getTags(), TokenFunctions.tokenToString()));
            final RobotDefinitionSetting setting = new RobotDefinitionSetting(this, omitSquareBrackets(name), args, "");
            setting.link(tags);
            getChildren().add(setting);
        }
        for (final KeywordTimeout timeout : keyword.getTimeouts()) {
            final String name = timeout.getDeclaration().getText().toString();
            final List<String> args = timeout.getTimeout() == null ? new ArrayList<String>()
                    : newArrayList(timeout.getTimeout().getText().toString());
            args.addAll(Lists.transform(timeout.getMessage(), TokenFunctions.tokenToString()));
            final RobotDefinitionSetting setting = new RobotDefinitionSetting(this, omitSquareBrackets(name), args, "");
            setting.link(timeout);
            getChildren().add(setting);
        }
        for (final KeywordTeardown teardown : keyword.getTeardowns()) {
            final String name = teardown.getDeclaration().getText().toString();
            final List<String> args = teardown.getKeywordName() == null ? new ArrayList<String>()
                    : newArrayList(teardown.getKeywordName().getText().toString());
            args.addAll(Lists.transform(teardown.getArguments(), TokenFunctions.tokenToString()));
            final RobotDefinitionSetting setting = new RobotDefinitionSetting(this, omitSquareBrackets(name), args, "");
            setting.link(teardown);
            getChildren().add(setting);
        }
        for (final KeywordReturn returnSetting : keyword.getReturns()) {
            final String name = returnSetting.getDeclaration().getText().toString();
            final List<String> args = returnSetting.getReturnValues() == null ? new ArrayList<String>()
                    : Lists.transform(returnSetting.getReturnValues(), TokenFunctions.tokenToString());
            final RobotDefinitionSetting setting = new RobotDefinitionSetting(this, omitSquareBrackets(name), args, "");
            setting.link(returnSetting);
            getChildren().add(setting);
        }
    }

    private static String omitSquareBrackets(final String nameInBrackets) {
        return nameInBrackets.substring(1, nameInBrackets.length() - 1);
    }

    @Override
    public RobotKeywordsSection getParent() {
        return (RobotKeywordsSection) super.getParent();
    }

    @Override
    public ImageDescriptor getImage() {
        return RedImages.getUserKeywordImage();
    }

    public boolean hasArguments() {
        return getArgumentsSetting() != null;
    }

    public RobotDefinitionSetting getArgumentsSetting() {
        return findSetting(ARGUMENTS);
    }

    public boolean hasReturnValue() {
        return getReturnValueSetting() != null;
    }

    public RobotDefinitionSetting getReturnValueSetting() {
        return findSetting(RETURN);
    }

    public boolean hasDocumentation() {
        return getDocumentationSetting() != null;
    }

    public RobotDefinitionSetting getDocumentationSetting() {
        return findSetting(DOCUMENTATION);
    }

    public boolean hasTags() {
        return getTagsSetting() != null;
    }

    public RobotDefinitionSetting getTagsSetting() {
        return findSetting(TAGS);
    }

    public boolean hasTeardownValue() {
        return getTeardownSetting() != null;
    }

    public RobotDefinitionSetting getTeardownSetting() {
        return findSetting(TEARDOWN);
    }

    public boolean hasTimeoutValue() {
        return getTimeoutSetting() != null;
    }

    public RobotDefinitionSetting getTimeoutSetting() {
        return findSetting(TIMEOUT);
    }

    private RobotDefinitionSetting findSetting(final String name) {
        for (final RobotKeywordCall call : getChildren()) {
            if (call instanceof RobotDefinitionSetting && call.getName().equals(name)) {
                return (RobotDefinitionSetting) call;
            }
        }
        return null;
    }

    public String getDocumentation() {
        final RobotDefinitionSetting documentationSetting = getDocumentationSetting();
        if (documentationSetting != null) {
            final KeywordDocumentation documentation = (KeywordDocumentation) documentationSetting.getLinkedElement();
            final StringBuilder docBuilder = new StringBuilder();
            for (final RobotToken token : documentation.getDocumentationText()) {
                docBuilder.append(token.getText().toString());
            }
            return docBuilder.toString();
        }
        return "<not documented>";
    }

    public List<String> getArguments() {
        final RobotDefinitionSetting argumentsSetting = getArgumentsSetting();
        if (argumentsSetting != null) {
            final KeywordArguments arguments = (KeywordArguments) argumentsSetting.getLinkedElement();
            final List<String> args = newArrayList();
            for (final RobotToken token : arguments.getArguments()) {
                args.add(toPythonicNotation(token));
            }
            return args;
        }
        return newArrayList();
    }

    private String toPythonicNotation(final RobotToken token) {
        final List<IRobotTokenType> types = token.getTypes();
        final String text = token.getText().toString();
        if (types.contains(RobotTokenType.VARIABLES_DICTIONARY_DECLARATION)) {
            return "**" + text.substring(2, text.length() - 1);
        } else if (types.contains(RobotTokenType.VARIABLES_LIST_DECLARATION)) {
            return "*" + text.substring(2, text.length() - 1);
        } else if (types.contains(RobotTokenType.VARIABLES_SCALAR_DECLARATION)) {
            return text.substring(2, text.length() - 1);
        } else {
            return text;
        }
    }

    public boolean isDeprecated() {
        return Pattern.compile("^\\*deprecated[^\\n\\r]*\\*.*").matcher(getDocumentation().toLowerCase()).find();
    }

    @Override
    public Position getPosition() {
        final FilePosition begin = keyword.getBeginPosition();
        final FilePosition end = keyword.getEndPosition();

        return new Position(begin.getOffset(),
                end.getOffset() - begin.getOffset() + getSuiteFile().getLineDelimiterLength());
    }

    @Override
    public Position getDefinitionPosition() {
        final int begin = keyword.getKeywordName().getStartOffset();
        final int length = keyword.getKeywordName().getText().length();

        return new Position(begin, length);
    }
}
