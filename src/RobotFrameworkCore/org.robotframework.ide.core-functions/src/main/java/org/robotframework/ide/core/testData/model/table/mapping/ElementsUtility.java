/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.core.testData.model.table.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.robotframework.ide.core.testData.model.AKeywordBaseSetting;
import org.robotframework.ide.core.testData.model.FilePosition;
import org.robotframework.ide.core.testData.model.RobotFile;
import org.robotframework.ide.core.testData.model.RobotFileOutput;
import org.robotframework.ide.core.testData.model.mapping.PreviousLineHandler;
import org.robotframework.ide.core.testData.model.table.ARobotSectionTable;
import org.robotframework.ide.core.testData.model.table.ECompareResult;
import org.robotframework.ide.core.testData.model.table.TableHeader;
import org.robotframework.ide.core.testData.model.table.executableDescriptors.ForDescriptorInfo;
import org.robotframework.ide.core.testData.model.table.setting.AImported;
import org.robotframework.ide.core.testData.model.table.setting.LibraryImport;
import org.robotframework.ide.core.testData.text.read.IRobotLineElement;
import org.robotframework.ide.core.testData.text.read.IRobotTokenType;
import org.robotframework.ide.core.testData.text.read.LineReader.Constant;
import org.robotframework.ide.core.testData.text.read.ParsingState;
import org.robotframework.ide.core.testData.text.read.ParsingState.TableType;
import org.robotframework.ide.core.testData.text.read.RobotLine;
import org.robotframework.ide.core.testData.text.read.columnSeparators.ALineSeparator;
import org.robotframework.ide.core.testData.text.read.columnSeparators.Separator;
import org.robotframework.ide.core.testData.text.read.columnSeparators.Separator.SeparatorType;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotTokenType;


public class ElementsUtility {

    private final ParsingStateHelper parsingStateHelper;


    public ElementsUtility() {
        this.parsingStateHelper = new ParsingStateHelper();
    }


    public boolean isNewExecutableSection(final ALineSeparator separator,
            final RobotLine line) {
        boolean result = false;
        if (separator.getProducedType() == SeparatorType.PIPE) {
            final List<IRobotLineElement> lineElements = line.getLineElements();
            if (lineElements.size() == 1) {
                result = lineElements.get(0).getTypes()
                        .contains(SeparatorType.PIPE);
            }
        } else {
            result = line.getLineElements().isEmpty();
        }
        return result;
    }


    public LibraryImport findNearestLibraryImport(
            final RobotFileOutput robotFileOutput) {
        final AImported imported = getNearestImport(robotFileOutput);
        LibraryImport lib;
        if (imported instanceof LibraryImport) {
            lib = (LibraryImport) imported;
        } else {
            lib = null;

            // FIXME: sth wrong - declaration of library not inside setting
            // and
            // was not catch by previous library declaration logic
        }
        return lib;
    }


    public RobotToken computeCorrectRobotToken(final RobotLine currentLine,
            final Stack<ParsingState> processingState,
            final RobotFileOutput robotFileOutput, final FilePosition fp,
            final String text, final boolean isNewLine,
            final List<RobotToken> robotTokens, final String fileName) {
        final ParsingState state = parsingStateHelper
                .getCurrentStatus(processingState);

        RobotToken correct = null;
        if (robotTokens.size() > 1) {
            final List<RobotToken> tokensExactlyOnPosition = getTokensExactlyOnPosition(
                    robotTokens, fp);
            final TableType currentTable = state.getTable();
            if (tokensExactlyOnPosition.size() != 1
                    || currentTable == TableType.KEYWORD
                    || currentTable == TableType.TEST_CASE) {
                final List<RobotToken> headersPossible = findHeadersPossible(robotTokens);
                if (!headersPossible.isEmpty()) {
                    if (headersPossible.size() == 1) {
                        correct = headersPossible.get(0);
                    } else {
                        // FIXME: error
                    }
                } else {
                    final RobotToken comment = findCommentToken(robotTokens,
                            text);
                    if (comment != null) {
                        correct = comment;
                    } else {
                        for (final RobotToken rt : robotTokens) {
                            if (parsingStateHelper.isTypeForState(state, rt)) {
                                correct = rt;
                                break;
                            }
                        }
                    }

                    if (correct == null) {
                        if (ParsingState.getSettingsStates().contains(state)
                                || currentTable == TableType.VARIABLES
                                || currentTable == TableType.KEYWORD
                                || currentTable == TableType.TEST_CASE
                                || state == ParsingState.COMMENT) {
                            final RobotToken newRobotToken = new RobotToken();
                            newRobotToken.setLineNumber(fp.getLine());
                            newRobotToken.setStartColumn(fp.getColumn());
                            newRobotToken.setText(text);
                            newRobotToken.setRaw(text);
                            newRobotToken.setType(RobotTokenType.UNKNOWN);
                            correct = newRobotToken;
                        } else {
                            // FIXME: info that nothing was found so token will
                            // be
                            // treat as UNKNOWN
                            final RobotToken newRobotToken = new RobotToken();
                            newRobotToken.setLineNumber(fp.getLine());
                            newRobotToken.setStartColumn(fp.getColumn());
                            newRobotToken.setText(text);
                            newRobotToken.setRaw(text);
                            newRobotToken.setType(RobotTokenType.UNKNOWN);
                            final List<IRobotTokenType> types = newRobotToken
                                    .getTypes();
                            for (final RobotToken currentProposal : robotTokens) {
                                types.addAll(currentProposal.getTypes());
                            }
                            correct = newRobotToken;
                        }
                    }
                }
            } else {
                final RobotToken exactlyOnPosition = tokensExactlyOnPosition
                        .get(0);
                if (state.getPreviousState() != ParsingState.VARIABLE_TABLE_HEADER
                        && state.getPreviousState() != ParsingState.VARIABLE_TABLE_INSIDE
                        && state.getPreviousState() != ParsingState.SETTING_TABLE_HEADER
                        && state.getPreviousState() != ParsingState.SETTING_TABLE_INSIDE) {
                    final List<IRobotTokenType> types = exactlyOnPosition
                            .getTypes();
                    for (final RobotToken currentProposal : robotTokens) {
                        if (exactlyOnPosition != currentProposal) {
                            types.addAll(currentProposal.getTypes());
                        }
                    }
                }
                if (exactlyOnPosition.getRaw().toString().equals(text)) {
                    correct = exactlyOnPosition;
                } else {
                    final RobotToken newRobotToken = new RobotToken();
                    newRobotToken.setLineNumber(fp.getLine());
                    newRobotToken.setStartColumn(fp.getColumn());
                    newRobotToken.setText(text);
                    newRobotToken.setRaw(text);
                    newRobotToken.setType(RobotTokenType.UNKNOWN);
                    final List<IRobotTokenType> types = newRobotToken
                            .getTypes();
                    for (final RobotToken rt : robotTokens) {
                        types.addAll(rt.getTypes());
                    }
                    correct = newRobotToken;
                }
            }
        } else {
            final RobotToken token = robotTokens.get(0);
            if (!token.getTypes().contains(RobotTokenType.UNKNOWN)) {
                if (text.equals(token.getRaw().toString())) {
                    correct = token;
                } else {
                    final RobotToken newRobotToken = new RobotToken();
                    newRobotToken.setLineNumber(fp.getLine());
                    newRobotToken.setStartColumn(fp.getColumn());
                    newRobotToken.setText(text);
                    newRobotToken.setRaw(text);
                    newRobotToken.setType(RobotTokenType.UNKNOWN);
                    // FIXME: decide what to do
                    newRobotToken.getTypes().addAll(token.getTypes());
                    // or add warning about possible type
                    correct = newRobotToken;
                }
            } else {
                correct = token;
            }
        }

        boolean hasAnyProposalVariableInside = false;
        for (final RobotToken rt : robotTokens) {
            final List<IRobotTokenType> types = rt.getTypes();
            for (final IRobotTokenType type : types) {
                if (type == RobotTokenType.VARIABLES_DICTIONARY_DECLARATION
                        || type == RobotTokenType.VARIABLES_SCALAR_AS_LIST_DECLARATION
                        || type == RobotTokenType.VARIABLES_SCALAR_DECLARATION
                        || type == RobotTokenType.VARIABLES_LIST_DECLARATION) {
                    hasAnyProposalVariableInside = true;
                    break;
                }
            }
        }

        if (hasAnyProposalVariableInside
                && state != ParsingState.VARIABLE_TABLE_INSIDE) {
            correct.getTypes().add(RobotTokenType.VARIABLE_USAGE);
        }

        return correct;
    }


    private List<RobotToken> getTokensExactlyOnPosition(
            final List<RobotToken> robotTokens,
            final FilePosition currentPosition) {
        final List<RobotToken> tokens = new ArrayList<>();
        for (final RobotToken rt : robotTokens) {
            if (currentPosition.compare(rt.getFilePosition(), false) == ECompareResult.EQUAL_TO
                    .getValue()) {
                tokens.add(rt);
            }
        }

        return tokens;
    }


    public RobotToken findCommentToken(final List<RobotToken> robotTokens,
            final String text) {
        RobotToken comment = null;
        for (final RobotToken rt : robotTokens) {
            final List<IRobotTokenType> types = rt.getTypes();
            if (types.contains(RobotTokenType.START_HASH_COMMENT)
                    || types.contains(RobotTokenType.COMMENT_CONTINUE)) {
                if (text.equals(rt.getRaw().toString())) {
                    comment = rt;
                    break;
                }
            }
        }

        return comment;
    }


    public List<RobotToken> findHeadersPossible(final List<RobotToken> tokens) {
        final List<RobotToken> found = new ArrayList<>();
        for (final RobotToken t : tokens) {
            if (isTableHeader(t)) {
                found.add(t);
            }
        }

        return found;
    }


    public boolean isComment(final RobotLine line) {
        boolean result = false;
        for (final IRobotLineElement elem : line.getLineElements()) {
            final List<IRobotTokenType> types = elem.getTypes();
            if (types.isEmpty()) {
                result = false;
                break;
            } else {
                final IRobotTokenType tokenType = types.get(0);
                if (tokenType == SeparatorType.PIPE
                        || tokenType == SeparatorType.TABULATOR_OR_DOUBLE_SPACE) {
                    continue;
                } else if (tokenType == RobotTokenType.START_HASH_COMMENT) {
                    result = true;
                    break;
                } else {
                    result = false;
                    break;
                }
            }
        }

        return result;
    }


    public boolean isTableSection(final RobotLine line) {
        boolean result = false;
        for (final IRobotLineElement elem : line.getLineElements()) {
            if (isTableHeader(elem)) {
                result = true;
                break;
            }
        }

        return result;
    }


    public AImported getNearestImport(final RobotFileOutput robotFileOutput) {
        AImported result;
        final List<AImported> imports = robotFileOutput.getFileModel()
                .getSettingTable().getImports();
        if (!imports.isEmpty()) {
            result = imports.get(imports.size() - 1);
        } else {
            result = null;
        }

        return result;
    }


    public List<TableHeader<? extends ARobotSectionTable>> getKnownHeadersForTable(
            final RobotFileOutput robotFileOutput,
            final ParsingState tableHeaderState) {
        List<TableHeader<? extends ARobotSectionTable>> tableKnownHeaders = new ArrayList<>();
        final RobotFile fileModel = robotFileOutput.getFileModel();
        if (tableHeaderState == ParsingState.SETTING_TABLE_HEADER) {
            tableKnownHeaders = fileModel.getSettingTable().getHeaders();
        } else if (tableHeaderState == ParsingState.VARIABLE_TABLE_HEADER) {
            tableKnownHeaders = fileModel.getVariableTable().getHeaders();
        } else if (tableHeaderState == ParsingState.TEST_CASE_TABLE_HEADER) {
            tableKnownHeaders = fileModel.getTestCaseTable().getHeaders();
        } else if (tableHeaderState == ParsingState.KEYWORD_TABLE_HEADER) {
            tableKnownHeaders = fileModel.getKeywordTable().getHeaders();
        } else {
            // FIXME: error state not coherent
        }

        return tableKnownHeaders;
    }


    public boolean isTableHeader(final IRobotTokenType type) {
        return (type == RobotTokenType.SETTINGS_TABLE_HEADER
                || type == RobotTokenType.VARIABLES_TABLE_HEADER
                || type == RobotTokenType.TEST_CASES_TABLE_HEADER || type == RobotTokenType.KEYWORDS_TABLE_HEADER);
    }


    public boolean isTableHeader(final RobotToken t) {
        boolean result = false;
        final List<IRobotTokenType> declaredTypes = t.getTypes();
        for (final IRobotTokenType type : declaredTypes) {
            if (isTableHeader(type)) {
                result = true;
                break;
            }
        }

        if (!t.getRaw().trim().startsWith("*")) {
            result = false;
        }

        return result;
    }


    public boolean isTableHeader(final IRobotLineElement elem) {
        boolean result = false;
        if (elem instanceof RobotToken) {
            result = isTableHeader((RobotToken) elem);
        }

        return result;
    }


    public boolean isUserTableHeader(final RobotToken t) {
        final String raw = t.getRaw();
        if (raw != null && !raw.isEmpty()) {
            return raw.trim().startsWith("*");
        }
        return false;
    }


    public boolean checkIfHasAlreadyKeywordName(
            final List<? extends AKeywordBaseSetting<?>> keywordBases) {
        boolean result = false;
        for (final AKeywordBaseSetting<?> setting : keywordBases) {
            result = (setting.getKeywordName() != null);
            result = result || !setting.getArguments().isEmpty();
            if (result) {
                break;
            }
        }

        return result;
    }


    public boolean isNotOnlySeparatorOrEmptyLine(final RobotLine currentLine) {
        boolean anyValuableToken = false;
        final List<IRobotLineElement> lineElements = currentLine
                .getLineElements();
        for (final IRobotLineElement lineElem : lineElements) {
            if (lineElem instanceof RobotToken) {
                anyValuableToken = true;
                break;
            }
        }

        return anyValuableToken;
    }


    public boolean shouldGiveEmptyToProcess(
            final RobotFileOutput parsingOutput,
            final ALineSeparator separator, final Separator currentSeparator,
            final RobotLine line, final Stack<ParsingState> processingState) {
        boolean result = false;

        final ParsingState state = parsingStateHelper
                .getCurrentStatus(processingState);
        final TableType tableType = state.getTable();
        final List<IRobotLineElement> splittedLine = separator
                .getSplittedLine();

        if (separator.getProducedType() == SeparatorType.PIPE
                && currentSeparator.getStartColumn() == 0) {
            result = false;
        } else if (separator.getProducedType() == SeparatorType.PIPE) {
            final LineTokenInfo lineTokenInfo = LineTokenInfo
                    .build(splittedLine);
            if (!lineTokenInfo.getPositionsOfLineContinoue().isEmpty()
                    || !lineTokenInfo.getPositionsOfNotEmptyElements()
                            .isEmpty()) {
                // Logic: Grant valid to process empty elements in case:
                // SETTINGS or VARIABLES: always read empty the exclusion is
                // only that we have line continue and element is not the first
                // after header declaration
                // TEST CASES and KEYWORDS: read empty the exclusion is only
                // line continue in any case only first element is omitted
                // GRANT LOGIC main: always process start from beginning until
                // last not empty element
                final boolean isContinoue = lineTokenInfo
                        .isLineContinoueTheFirst();
                if (tableType == TableType.SETTINGS
                        || tableType == TableType.VARIABLES) {
                    if (isContinoue) {
                        final RobotFile model = parsingOutput.getFileModel();
                        final PreviousLineHandler prevLineHandler = new PreviousLineHandler();
                        if (prevLineHandler.isSomethingToContinue(model)) {
                            result = lineTokenInfo.getDataStartIndex() <= separator
                                    .getCurrentElementIndex();
                        } else {
                            result = true;
                        }
                    } else {
                        result = true;
                    }

                    result = result
                            && lineTokenInfo.getDataEndIndex() >= separator
                                    .getCurrentElementIndex();
                } else if (tableType == TableType.TEST_CASE
                        || tableType == TableType.KEYWORD) {
                    if (line.getLineElements().size() >= 2) {
                        if (isContinoue) {
                            result = lineTokenInfo.getDataStartIndex() <= separator
                                    .getCurrentElementIndex();
                        } else {
                            if (state == ParsingState.TEST_CASE_DECLARATION
                                    || state == ParsingState.KEYWORD_DECLARATION) {
                                /**
                                 * <pre>
                                 *  *** Test Cases *** 
                                 *  | x | | ... | Log | ... |
                                 *  
                                 *  is not inline:
                                 *  
                                 * ** Test Cases *** 
                                 * | | x | | ... | Log | ... |
                                 * </pre>
                                 */
                                if (shouldTreatAsInlineContinue(lineTokenInfo)) {
                                    result = separator.getCurrentElementIndex() > lineTokenInfo
                                            .getPositionsOfLineContinoue().get(
                                                    0)
                                            || separator
                                                    .getCurrentElementIndex() < lineTokenInfo
                                                    .getDataStartIndex();
                                } else {
                                    result = true;
                                }
                            } else if (state == ParsingState.TEST_CASE_INSIDE_ACTION
                                    || state == ParsingState.KEYWORD_INSIDE_ACTION) {
                                final ForDescriptorInfo forInfo = ForDescriptorInfo
                                        .build(splittedLine);
                                if (forInfo.getForStartIndex() > -1) {
                                    if (forInfo.getForLineContinueInlineIndex() > -1) {
                                        result = (separator
                                                .getCurrentElementIndex() > forInfo
                                                .getForLineContinueInlineIndex());
                                    } else {
                                        result = true;
                                    }
                                } else {
                                    result = true;
                                }
                            } else {
                                result = true;
                            }
                        }

                        result = result
                                && lineTokenInfo.getDataEndIndex() >= separator
                                        .getCurrentElementIndex();
                    }
                }
            }
        }

        return result;
    }


    private boolean shouldTreatAsInlineContinue(
            final LineTokenInfo lineTokenInfo) {
        boolean result = false;

        if (!lineTokenInfo.getPositionsOfLineContinoue().isEmpty()
                && !lineTokenInfo.getPositionsOfNotEmptyElements().isEmpty()) {
            final int theFirstToken = lineTokenInfo
                    .getPositionsOfNotEmptyElements().get(0);
            final int theFirstContinoue = lineTokenInfo
                    .getPositionsOfLineContinoue().get(0);
            if (lineTokenInfo.getPositionsOfNotEmptyElements().size() > 1) {
                final int theSecondToken = lineTokenInfo
                        .getPositionsOfNotEmptyElements().get(1);
                result = theFirstToken < theFirstContinoue
                        && theFirstContinoue < theSecondToken;
            } else {
                result = theFirstToken < theFirstContinoue;
            }
        }

        return result;
    }


    public ARobotSectionTable getTable(final RobotFile robotModel,
            final TableType type) {
        ARobotSectionTable table = null;
        if (type == TableType.SETTINGS) {
            table = robotModel.getSettingTable();
        } else if (type == TableType.VARIABLES) {
            table = robotModel.getVariableTable();
        } else if (type == TableType.KEYWORD) {
            table = robotModel.getKeywordTable();
        } else if (type == TableType.TEST_CASE) {
            table = robotModel.getTestCaseTable();
        }

        return table;
    }

    private static class LineTokenInfo {

        private final List<Integer> positionsOfNotEmptyElements = new ArrayList<>();
        private final List<Integer> positionsOfLineContinoue = new ArrayList<>();
        private boolean isLineContinoue;
        private int dataStartIndex = -1;
        private int dataEndIndex = -1;


        public static LineTokenInfo build(final List<IRobotLineElement> elements) {
            final LineTokenInfo lti = new LineTokenInfo();
            final int numberOfElements = elements.size();
            for (int elemIndex = 0; elemIndex < numberOfElements; elemIndex++) {
                final IRobotLineElement elem = elements.get(elemIndex);
                if (elem instanceof RobotToken) {
                    final RobotToken token = (RobotToken) elem;
                    final String tokenText = token.getRaw().toString();
                    if (RobotTokenType.PREVIOUS_LINE_CONTINUE
                            .getRepresentation().get(0).equals(tokenText)) {
                        lti.positionsOfLineContinoue.add(elemIndex);
                        if (lti.positionsOfNotEmptyElements.isEmpty()) {
                            lti.isLineContinoue = true;
                        }

                        if (lti.dataStartIndex == -1) {
                            lti.dataStartIndex = elemIndex;
                        }
                        lti.dataEndIndex = elemIndex;
                    } else if (tokenText != null
                            && !"".equals(tokenText.trim())) {
                        lti.positionsOfNotEmptyElements.add(elemIndex);

                        if (lti.dataStartIndex == -1) {
                            lti.dataStartIndex = elemIndex;
                        }
                        lti.dataEndIndex = elemIndex;
                    }
                }
            }

            return lti;
        }


        public List<Integer> getPositionsOfNotEmptyElements() {
            return positionsOfNotEmptyElements;
        }


        public List<Integer> getPositionsOfLineContinoue() {
            return positionsOfLineContinoue;
        }


        public boolean isLineContinoueTheFirst() {
            return isLineContinoue;
        }


        public int getDataStartIndex() {
            return dataStartIndex;
        }


        public int getDataEndIndex() {
            return dataEndIndex;
        }
    }


    public int getEndOfLineLength(final List<Constant> eols) {
        int size = 0;
        for (final Constant c : eols) {
            if (c != Constant.EOF) {
                size++;
            } else {
                break;
            }
        }

        return size;
    }


    public void fixNotSetPositions(final RobotToken token, final FilePosition fp) {
        if (token.getStartOffset() == IRobotLineElement.NOT_SET) {
            token.setStartOffset(fp.getOffset());
        }
        if (token.getLineNumber() == IRobotLineElement.NOT_SET) {
            token.setLineNumber(fp.getLine());
        }
        if (token.getStartColumn() == IRobotLineElement.NOT_SET) {
            token.setStartColumn(fp.getColumn());
        }
    }

}
