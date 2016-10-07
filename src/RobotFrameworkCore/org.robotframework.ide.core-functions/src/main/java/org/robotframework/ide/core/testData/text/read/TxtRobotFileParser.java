/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.core.testData.text.read;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.robotframework.ide.core.testData.IRobotFileParser;
import org.robotframework.ide.core.testData.model.FilePosition;
import org.robotframework.ide.core.testData.model.RobotFile;
import org.robotframework.ide.core.testData.model.RobotFileOutput;
import org.robotframework.ide.core.testData.model.RobotFileOutput.BuildMessage;
import org.robotframework.ide.core.testData.model.RobotFileOutput.Status;
import org.robotframework.ide.core.testData.model.mapping.PreviousLineHandler;
import org.robotframework.ide.core.testData.model.mapping.PreviousLineHandler.LineContinueType;
import org.robotframework.ide.core.testData.model.table.ARobotSectionTable;
import org.robotframework.ide.core.testData.model.table.TableHeader;
import org.robotframework.ide.core.testData.model.table.mapping.ElementPositionResolver;
import org.robotframework.ide.core.testData.model.table.mapping.ElementPositionResolver.PositionExpected;
import org.robotframework.ide.core.testData.model.table.mapping.ElementsUtility;
import org.robotframework.ide.core.testData.model.table.mapping.IParsingMapper;
import org.robotframework.ide.core.testData.model.table.mapping.ParsingStateHelper;
import org.robotframework.ide.core.testData.model.table.mapping.PrettyAlignSpaceUtility;
import org.robotframework.ide.core.testData.model.table.mapping.SettingsMapperProvider;
import org.robotframework.ide.core.testData.model.table.mapping.TestCaseMapperProvider;
import org.robotframework.ide.core.testData.model.table.mapping.UserKeywordMapperProvider;
import org.robotframework.ide.core.testData.model.table.mapping.VariablesDeclarationMapperProvider;
import org.robotframework.ide.core.testData.model.table.setting.mapping.UnknownSettingArgumentMapper;
import org.robotframework.ide.core.testData.model.table.setting.mapping.UnknownSettingMapper;
import org.robotframework.ide.core.testData.model.table.setting.mapping.library.LibraryAliasFixer;
import org.robotframework.ide.core.testData.model.table.testCases.mapping.TestCaseExecutableRowActionMapper;
import org.robotframework.ide.core.testData.model.table.testCases.mapping.TestCaseExecutableRowArgumentMapper;
import org.robotframework.ide.core.testData.model.table.userKeywords.mapping.KeywordExecutableRowActionMapper;
import org.robotframework.ide.core.testData.model.table.userKeywords.mapping.KeywordExecutableRowArgumentMapper;
import org.robotframework.ide.core.testData.model.table.variables.mapping.CommonVariableHelper;
import org.robotframework.ide.core.testData.model.table.variables.mapping.UnknownVariableMapper;
import org.robotframework.ide.core.testData.model.table.variables.mapping.UnknownVariableValueMapper;
import org.robotframework.ide.core.testData.text.read.LineReader.Constant;
import org.robotframework.ide.core.testData.text.read.columnSeparators.ALineSeparator;
import org.robotframework.ide.core.testData.text.read.columnSeparators.Separator;
import org.robotframework.ide.core.testData.text.read.columnSeparators.TokenSeparatorBuilder;
import org.robotframework.ide.core.testData.text.read.recognizer.ATokenRecognizer;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotToken;
import org.robotframework.ide.core.testData.text.read.recognizer.RobotTokenType;
import org.robotframework.ide.core.testData.text.read.recognizer.SettingsRecognizersProvider;
import org.robotframework.ide.core.testData.text.read.recognizer.TestCaseRecognizersProvider;
import org.robotframework.ide.core.testData.text.read.recognizer.UserKeywordRecognizersProvider;
import org.robotframework.ide.core.testData.text.read.recognizer.VariablesDeclarationRecognizersProvider;

import com.google.common.annotations.VisibleForTesting;


public class TxtRobotFileParser implements IRobotFileParser {

    private final TokenSeparatorBuilder tokenSeparatorBuilder;
    private final List<ATokenRecognizer> recognized = new ArrayList<>();
    private final List<IParsingMapper> mappers = new ArrayList<>();
    private final List<IParsingMapper> unknownTableElementsMapper = new ArrayList<>();
    private final ElementsUtility utility;
    private final PrettyAlignSpaceUtility alignUtility;
    private final ParsingStateHelper parsingStateHelper;
    private final LibraryAliasFixer libraryFixer;
    private final PreviousLineHandler previousLineHandler;
    private final CommonVariableHelper variableHelper;
    private final ElementPositionResolver positionResolvers;


    public TxtRobotFileParser() {
        this.utility = new ElementsUtility();
        this.alignUtility = new PrettyAlignSpaceUtility();
        this.variableHelper = new CommonVariableHelper();
        this.parsingStateHelper = new ParsingStateHelper();
        this.tokenSeparatorBuilder = new TokenSeparatorBuilder();
        this.libraryFixer = new LibraryAliasFixer(utility, parsingStateHelper);
        this.previousLineHandler = new PreviousLineHandler();
        this.positionResolvers = new ElementPositionResolver();

        recognized.addAll(new SettingsRecognizersProvider().getRecognizers());
        recognized.addAll(new VariablesDeclarationRecognizersProvider()
                .getRecognizers());
        recognized.addAll(new TestCaseRecognizersProvider().getRecognizers());
        recognized
                .addAll(new UserKeywordRecognizersProvider().getRecognizers());

        mappers.addAll(new SettingsMapperProvider().getMappers());
        mappers.addAll(new VariablesDeclarationMapperProvider().getMappers());
        mappers.addAll(new TestCaseMapperProvider().getMappers());
        mappers.addAll(new UserKeywordMapperProvider().getMappers());

        unknownTableElementsMapper.add(new UnknownSettingMapper());
        unknownTableElementsMapper.add(new UnknownSettingArgumentMapper());
        unknownTableElementsMapper.add(new UnknownVariableMapper());
        unknownTableElementsMapper.add(new UnknownVariableValueMapper());
        unknownTableElementsMapper.add(new TestCaseExecutableRowActionMapper());
        unknownTableElementsMapper
                .add(new TestCaseExecutableRowArgumentMapper());
        unknownTableElementsMapper.add(new KeywordExecutableRowActionMapper());
        unknownTableElementsMapper
                .add(new KeywordExecutableRowArgumentMapper());
    }


    @Override
    public boolean canParseFile(final File file,
            final boolean isFromStringContent) {
        boolean result = false;

        if (file != null && (file.isFile() || isFromStringContent)) {
            final String fileName = file.getName().toLowerCase();
            result = (fileName.endsWith(".txt") || fileName.endsWith(".robot"));
        }

        return result;
    }


    @Override
    public IRobotFileParser newInstance() {
        return new TxtRobotFileParser();
    }


    @Override
    public void parse(final RobotFileOutput parsingOutput,
            final InputStream inputStream, final File robotFile) {
        boolean wasProcessingError = false;
        try {
            parse(parsingOutput, robotFile, new InputStreamReader(inputStream,
                    Charset.forName("UTF-8")));
        } catch (final Exception e) {
            parsingOutput.addBuildMessage(BuildMessage.createErrorMessage(
                    "Unknown problem during reading file " + robotFile
                            + ".\nStack:" + e, "File " + robotFile));
            // FIXME: stack trace adding
            e.printStackTrace();
            wasProcessingError = true;
        }

        if (wasProcessingError || parsingOutput.getStatus() == Status.FAILED) {
            parsingOutput.setStatus(Status.FAILED);
        } else {
            parsingOutput.setStatus(Status.PASSED);
        }

        parsingOutput.setProcessedFile(robotFile);
    }


    @Override
    public void parse(final RobotFileOutput parsingOutput, final File robotFile) {
        boolean wasProcessingError = false;
        try {
            final FileInputStream fis = new FileInputStream(robotFile);
            parse(parsingOutput, fis, robotFile);
        } catch (final FileNotFoundException e) {
            parsingOutput.addBuildMessage(BuildMessage.createErrorMessage(
                    "File " + robotFile + " was not found.\nStack:" + e,
                    "File " + robotFile));
            wasProcessingError = true;
            // FIXME: position should be more descriptive
        } catch (final Exception e) {
            parsingOutput.addBuildMessage(BuildMessage.createErrorMessage(
                    "Unknown problem during reading file " + robotFile
                            + ".\nStack:" + e, "File " + robotFile));
            // FIXME: stack trace adding
            e.printStackTrace();
            wasProcessingError = true;
        }

        if (wasProcessingError || parsingOutput.getStatus() == Status.FAILED) {
            parsingOutput.setStatus(Status.FAILED);
        } else {
            parsingOutput.setStatus(Status.PASSED);
        }

        parsingOutput.setProcessedFile(robotFile);
    }


    private RobotFileOutput parse(final RobotFileOutput parsingOutput,
            final File robotFile, final Reader reader) {
        boolean wasProcessingError = false;
        previousLineHandler.clear();

        final LineReader lineHolder = new LineReader(reader);
        final BufferedReader lineReader = new BufferedReader(lineHolder);
        int lineNumber = 1;
        int currentOffset = 0;
        String currentLineText = null;
        final Stack<ParsingState> processingState = new Stack<>();
        boolean isNewLine = false;
        try {
            while((currentLineText = lineReader.readLine()) != null) {
                final RobotLine line = new RobotLine(lineNumber,
                        parsingOutput.getFileModel());
                final StringBuilder text = new StringBuilder(currentLineText);
                int lastColumnProcessed = 0;
                // get separator for this line
                final ALineSeparator separator = tokenSeparatorBuilder
                        .createSeparator(lineNumber, currentLineText);
                line.setSeparatorType(separator.getProducedType());
                RobotToken rt = null;

                final int textLength = currentLineText.length();
                // check if is any data to process
                if (textLength > 0) {
                    // consume all data
                    while(lastColumnProcessed < textLength) {
                        if (separator.hasNext()) {
                            // iterate column-by-column in robot file
                            final Separator currentSeparator = separator.next();
                            final int startColumn = currentSeparator
                                    .getStartColumn();
                            final int remainingData = startColumn
                                    - lastColumnProcessed;
                            // {$a} | {$b} in this case we check if {$a} was
                            // before
                            // '|' pipe separator
                            if (remainingData > 0
                                    || utility.shouldGiveEmptyToProcess(
                                            parsingOutput, separator,
                                            currentSeparator, line,
                                            processingState)) {
                                final String rawText = text.substring(
                                        lastColumnProcessed, startColumn);

                                rt = processLineElement(line, processingState,
                                        parsingOutput, new FilePosition(
                                                lineNumber,
                                                lastColumnProcessed,
                                                currentOffset), rawText,
                                        robotFile.getName(), isNewLine);

                                rt.setStartOffset(currentOffset);
                                currentOffset += rt.getRaw().length();
                                line.addLineElement(rt);

                                alignUtility.extractPrettyAlignWhitespaces(
                                        line, rt, rawText);

                                isNewLine = false;
                            }

                            currentSeparator.setStartOffset(currentOffset);
                            currentOffset += currentSeparator.getRaw().length();
                            line.addLineElement(currentSeparator);
                            lastColumnProcessed = currentSeparator
                                    .getEndColumn();
                        } else {
                            // last element in line
                            if (utility.isNewExecutableSection(separator, line)) {
                                processingState
                                        .remove(ParsingState.TEST_CASE_DECLARATION);
                                processingState
                                        .remove(ParsingState.KEYWORD_DECLARATION);
                            }

                            final String rawText = text
                                    .substring(lastColumnProcessed);

                            rt = processLineElement(
                                    line,
                                    processingState,
                                    parsingOutput,
                                    new FilePosition(lineNumber,
                                            lastColumnProcessed, currentOffset),
                                    rawText, robotFile.getName(), isNewLine);
                            rt.setStartOffset(currentOffset);
                            currentOffset += rt.getRaw().length();
                            line.addLineElement(rt);

                            alignUtility.extractPrettyAlignWhitespaces(line,
                                    rt, rawText);

                            lastColumnProcessed = textLength;
                            isNewLine = false;
                        }
                    }
                }

                alignUtility.fixOnlyPrettyAlignLinesInSettings(line,
                        processingState);
                alignUtility.fixOnlyPrettyAlignLinesInVariables(line,
                        processingState);

                final List<IRobotLineElement> lineElements = line
                        .getLineElements();
                if (!lineElements.isEmpty()) {
                    final IRobotLineElement lineElem = lineElements
                            .get(lineElements.size() - 1);
                    currentOffset = lineElem.getStartOffset()
                            + lineElem.getText().length();
                }

                final List<Constant> endOfLine = lineHolder
                        .getLineEnd(currentOffset);
                line.setEndOfLine(endOfLine, currentOffset, lastColumnProcessed);
                currentOffset += utility.getEndOfLineLength(endOfLine);
                lineNumber++;
                lastColumnProcessed = 0;
                libraryFixer.checkAndFixLine(parsingOutput, processingState);
                /**
                 * special for case
                 * 
                 * <pre>
                 * *** Settings
                 * Suite Setup      Keyword
                 * 
                 * ...              argument_x
                 * </pre>
                 */
                if (utility.isNotOnlySeparatorOrEmptyLine(line)) {
                    variableHelper.extractVariableAssignmentPart(line,
                            processingState);
                    previousLineHandler.flushNew(processingState);
                }
                parsingOutput.getFileModel().addNewLine(line);

                parsingStateHelper.updateStatusesForNewLine(processingState);
                isNewLine = true;
            }
        } catch (final FileNotFoundException e) {
            parsingOutput.addBuildMessage(BuildMessage.createErrorMessage(
                    "File " + robotFile + " was not found.\nStack:" + e,
                    "File " + robotFile));
            wasProcessingError = true;
            // FIXME: position should be more descriptive
        } catch (final IOException e) {
            parsingOutput.addBuildMessage(BuildMessage.createErrorMessage(
                    "Problem during file " + robotFile + " reading.\nStack:"
                            + e.getLocalizedMessage(), "File " + robotFile));
            wasProcessingError = true;
        } catch (final Exception e) {
            parsingOutput.addBuildMessage(BuildMessage.createErrorMessage(
                    "Unknown problem during reading file " + robotFile
                            + ".\nStack:" + e, "File " + robotFile));
            // FIXME: stack trace adding
            e.printStackTrace();
            wasProcessingError = true;
        } finally {
            try {
                lineReader.close();
            } catch (final IOException e) {
                parsingOutput.addBuildMessage(BuildMessage.createErrorMessage(
                        "Error occured, when file was closing. Stack trace\n\t"
                                + e, robotFile.getAbsolutePath()));
            }
        }

        if (wasProcessingError) {
            parsingOutput.setStatus(Status.FAILED);
        } else {
            parsingOutput.setStatus(Status.PASSED);
        }

        return parsingOutput;
    }


    @VisibleForTesting
    protected RobotToken processLineElement(final RobotLine currentLine,
            final Stack<ParsingState> processingState,
            final RobotFileOutput robotFileOutput, final FilePosition fp,
            final String text, final String fileName, final boolean isNewLine) {
        final List<RobotToken> robotTokens = recognize(fp, text);
        RobotToken robotToken = utility.computeCorrectRobotToken(currentLine,
                processingState, robotFileOutput, fp, text, isNewLine,
                robotTokens, fileName);

        final LineContinueType lineContinueType = previousLineHandler
                .computeLineContinue(processingState, isNewLine,
                        robotFileOutput.getFileModel(), currentLine, robotToken);

        boolean processThisElement = true;
        if (lineContinueType == LineContinueType.LINE_CONTINUE_INLINED) {
            processThisElement = false;
        } else if (previousLineHandler.isSomethingToDo(lineContinueType)) {
            previousLineHandler.restorePreviousStack(lineContinueType,
                    processingState, currentLine, robotToken);

            processThisElement = (processingState.size() > 1)
                    && !robotToken.getTypes().contains(
                            RobotTokenType.PREVIOUS_LINE_CONTINUE);
        }

        if (processThisElement) {
            ParsingState newStatus = parsingStateHelper.getStatus(robotToken);
            boolean wasRecognizedCorrectly = true;
            if (robotToken != null) {
                if (!text.trim().equals(robotToken.getText().toString().trim())) {
                    wasRecognizedCorrectly = false;
                    // FIXME: add information that type is incorrect missing
                    // separator
                    final RobotToken newRobotToken = new RobotToken();
                    newRobotToken.setLineNumber(fp.getLine());
                    newRobotToken.setStartColumn(fp.getColumn());
                    newRobotToken.setText(text);
                    newRobotToken.setRaw(text);
                    newRobotToken.setType(RobotTokenType.UNKNOWN);
                    newRobotToken.getTypes().addAll(robotToken.getTypes());
                    robotToken = newRobotToken;
                    newStatus = ParsingState.UNKNOWN;
                }
            } else {
                robotToken = new RobotToken();
                robotToken.setLineNumber(fp.getLine());
                robotToken.setStartColumn(fp.getColumn());
                robotToken.setText(text);
                robotToken.setRaw(text);
                robotToken.setType(RobotTokenType.UNKNOWN);

                newStatus = ParsingState.UNKNOWN;
            }

            boolean useMapper = true;
            final RobotFile fileModel = robotFileOutput.getFileModel();
            if (utility.isTableHeader(robotToken)) {
                if (positionResolvers.isCorrectPosition(
                        PositionExpected.TABLE_HEADER, fileModel, currentLine,
                        robotToken)) {
                    if (wasRecognizedCorrectly) {
                        @SuppressWarnings("rawtypes")
                        final TableHeader<?> header = new TableHeader(
                                robotToken);
                        ARobotSectionTable table = null;
                        if (newStatus == ParsingState.SETTING_TABLE_HEADER) {
                            table = fileModel.getSettingTable();
                        } else if (newStatus == ParsingState.VARIABLE_TABLE_HEADER) {
                            table = fileModel.getVariableTable();
                        } else if (newStatus == ParsingState.TEST_CASE_TABLE_HEADER) {
                            table = fileModel.getTestCaseTable();
                        } else if (newStatus == ParsingState.KEYWORD_TABLE_HEADER) {
                            table = fileModel.getKeywordTable();
                        }

                        table.addHeader(header);
                        processingState.clear();
                        processingState.push(newStatus);

                        useMapper = false;
                    } else {
                        // FIXME: add warning about incorrect table
                    }
                } else {
                    // FIXME: add warning about wrong place
                }
            }

            if (!wasRecognizedCorrectly
                    && !text.equals(robotToken.getRaw().toString())
                    && utility.isUserTableHeader(robotToken)
                    && positionResolvers.isCorrectPosition(
                            PositionExpected.TABLE_HEADER, fileModel,
                            currentLine, robotToken)) {
                robotToken.getTypes().add(0,
                        RobotTokenType.USER_OWN_TABLE_HEADER);
                // FIXME: add warning about user trash table
                processingState.clear();
                processingState.push(ParsingState.TRASH);

                useMapper = false;
            }

            robotToken = alignUtility.applyPrettyAlignTokenIfIsValid(
                    currentLine, processingState, robotFileOutput, fp, text,
                    fileName, robotToken);

            useMapper = useMapper
                    & !robotToken.getTypes().contains(
                            RobotTokenType.PRETTY_ALIGN_SPACE);

            if (useMapper) {
                robotToken = mapToCorrectTokenAndPutInCorrectPlaceInModel(
                        currentLine, processingState, robotFileOutput, fp,
                        text, fileName, robotToken);
            }
        }

        utility.fixNotSetPositions(robotToken, fp);

        return robotToken;
    }


    private RobotToken mapToCorrectTokenAndPutInCorrectPlaceInModel(
            final RobotLine currentLine,
            final Stack<ParsingState> processingState,
            final RobotFileOutput robotFileOutput, final FilePosition fp,
            final String text, final String fileName, RobotToken robotToken) {
        final List<IParsingMapper> matchedMappers = new ArrayList<>();
        for (final IParsingMapper mapper : mappers) {
            if (mapper.checkIfCanBeMapped(robotFileOutput, currentLine,
                    robotToken, text, processingState)) {
                matchedMappers.add(mapper);
            }
        }

        // check for unknown setting
        int size = matchedMappers.size();
        if (size == 0) {
            for (final IParsingMapper mapper : unknownTableElementsMapper) {
                if (mapper.checkIfCanBeMapped(robotFileOutput, currentLine,
                        robotToken, text, processingState)) {
                    matchedMappers.add(mapper);
                }
            }
        }

        size = matchedMappers.size();
        if (size == 1) {
            robotToken = matchedMappers.get(0).map(currentLine,
                    processingState, robotFileOutput, robotToken, fp, text);
        } else {
            robotFileOutput.addBuildMessage(BuildMessage.createErrorMessage(
                    "Unknown data \'" + text + "\' appears in " + fp
                            + ", during state: " + processingState, fileName));
        }
        return robotToken;
    }


    @VisibleForTesting
    protected List<RobotToken> recognize(final FilePosition fp,
            final String text) {
        final List<RobotToken> possibleRobotTokens = new ArrayList<>();
        final StringBuilder sb = new StringBuilder(text);
        for (final ATokenRecognizer rec : recognized) {
            if (rec.hasNext(sb, fp.getLine())) {
                final RobotToken t = rec.next();
                t.setStartColumn(t.getStartColumn() + fp.getColumn());
                possibleRobotTokens.add(t);
            }
        }

        if (possibleRobotTokens.isEmpty()) {
            final RobotToken rt = new RobotToken();
            rt.setLineNumber(fp.getLine());
            rt.setText(text);
            rt.setRaw(text);
            rt.setStartColumn(fp.getColumn());

            possibleRobotTokens.add(rt);
        }

        return possibleRobotTokens;
    }
}
