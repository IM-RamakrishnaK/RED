/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.core.testData.model.table.userKeywords.mapping;

import java.util.List;
import java.util.Stack;

import org.robotframework.ide.core.testData.model.FilePosition;
import org.robotframework.ide.core.testData.model.RobotFileOutput;
import org.robotframework.ide.core.testData.model.table.KeywordTable;
import org.robotframework.ide.core.testData.model.table.mapping.ElementPositionResolver;
import org.robotframework.ide.core.testData.model.table.mapping.ElementPositionResolver.PositionExpected;
import org.robotframework.ide.core.testData.model.table.mapping.IParsingMapper;
import org.robotframework.ide.core.testData.model.table.userKeywords.UserKeyword;
import org.robotframework.ide.core.testData.text.read.IRobotTokenType;
import org.robotframework.ide.core.testData.text.read.ParsingState;
import org.robotframework.ide.core.testData.text.read.RobotLine;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotTokenType;

import com.google.common.annotations.VisibleForTesting;


public class UserKeywordNameMapper implements IParsingMapper {

    private final ElementPositionResolver positionResolver;


    public UserKeywordNameMapper() {
        this.positionResolver = new ElementPositionResolver();
    }


    @Override
    public RobotToken map(RobotLine currentLine,
            Stack<ParsingState> processingState,
            RobotFileOutput robotFileOutput, RobotToken rt, FilePosition fp,
            String text) {
        List<IRobotTokenType> types = rt.getTypes();
        types.remove(RobotTokenType.UNKNOWN);
        types.add(0, RobotTokenType.KEYWORD_NAME);
        rt.setText(new StringBuilder(text));
        rt.setRaw(new StringBuilder(text));

        KeywordTable keywordTable = robotFileOutput.getFileModel()
                .getKeywordTable();
        UserKeyword keyword = new UserKeyword(rt);
        keywordTable.addKeyword(keyword);

        processingState.push(ParsingState.KEYWORD_DECLARATION);

        return rt;
    }


    @Override
    public boolean checkIfCanBeMapped(RobotFileOutput robotFileOutput,
            RobotLine currentLine, RobotToken rt, String text,
            Stack<ParsingState> processingState) {
        boolean result = false;
        if (positionResolver.isCorrectPosition(
                PositionExpected.USER_KEYWORD_NAME,
                robotFileOutput.getFileModel(), currentLine, rt)) {
            if (isIncludedInKeywordTable(currentLine, processingState)) {
                boolean wasUpdated = false;
                String keywordName = rt.getRaw().toString();
                if (keywordName != null) {
                    result = !keywordName.trim().startsWith(
                            RobotTokenType.START_HASH_COMMENT
                                    .getRepresentation().get(0));
                    wasUpdated = true;
                }

                if (!wasUpdated) {
                    result = true;
                }
            } else {
                // FIXME: it is in wrong place means no keyword table
                // declaration
            }
        } else {
            // FIXME: wrong place | | Library or | Library | Library X |
            // case.
        }

        return result;
    }


    @VisibleForTesting
    protected boolean isIncludedInKeywordTable(final RobotLine line,
            final Stack<ParsingState> processingState) {

        return processingState.contains(ParsingState.KEYWORD_TABLE_INSIDE);
    }
}
