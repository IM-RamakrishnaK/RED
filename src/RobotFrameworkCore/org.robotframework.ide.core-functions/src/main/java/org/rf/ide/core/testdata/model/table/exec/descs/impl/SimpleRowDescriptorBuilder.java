/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model.table.exec.descs.impl;

import java.util.List;

import org.rf.ide.core.testdata.model.AModelElement;
import org.rf.ide.core.testdata.model.RobotFile;
import org.rf.ide.core.testdata.model.table.ARobotSectionTable;
import org.rf.ide.core.testdata.model.table.RobotExecutableRow;
import org.rf.ide.core.testdata.model.table.exec.descs.IExecutableRowDescriptor;
import org.rf.ide.core.testdata.model.table.exec.descs.IRowDescriptorBuilder;
import org.rf.ide.core.testdata.model.table.exec.descs.RobotAction;
import org.rf.ide.core.testdata.model.table.exec.descs.VariableExtractor;
import org.rf.ide.core.testdata.model.table.exec.descs.ast.mapping.IElementDeclaration;
import org.rf.ide.core.testdata.model.table.exec.descs.ast.mapping.MappingResult;
import org.rf.ide.core.testdata.model.table.exec.descs.ast.mapping.VariableDeclaration;
import org.rf.ide.core.testdata.text.read.recognizer.RobotToken;
import org.rf.ide.core.testdata.text.read.recognizer.RobotTokenType;

public class SimpleRowDescriptorBuilder implements IRowDescriptorBuilder {

    @Override
    public <T> AcceptResult acceptable(final RobotExecutableRow<T> execRowLine) {
        return new AcceptResult(true);
    }

    @Override
    public <T> IExecutableRowDescriptor<T> buildDescription(final RobotExecutableRow<T> execRowLine,
            final AcceptResult acceptResult) {
        final SimpleRowDescriptor<T> simple = new SimpleRowDescriptor<>(execRowLine);
        final AModelElement<?> keywordOrTestcase = (AModelElement<?>) execRowLine.getParent();
        final ARobotSectionTable table = (ARobotSectionTable) keywordOrTestcase.getParent();
        final RobotFile robotFile = table.getParent();
        final String fileName = robotFile.getParent().getProcessedFile().getAbsolutePath();

        final VariableExtractor varExtractor = new VariableExtractor();
        final List<RobotToken> lineElements = execRowLine.getElementTokens();
        boolean isAfterFirstAction = false;
        for (final RobotToken elem : lineElements) {
            final MappingResult mappingResult = varExtractor.extract(elem, fileName);
            simple.addMessages(mappingResult.getMessages());

            // value is keyword if is on the first place and have in it nested
            // variables and when contains text on the beginning or end of field
            final List<VariableDeclaration> correctVariables = mappingResult.getCorrectVariables();
            final List<IElementDeclaration> mappedElements = mappingResult.getMappedElements();
            if (isAfterFirstAction) {
                simple.addUsedVariables(correctVariables);
                simple.addTextParameters(mappingResult.getTextElements());
            } else {
                if (correctVariables.size() == 1 && mappedElements.size() == 1) {
                    // definition variable
                    simple.addCreatedVariable(correctVariables.get(0));
                } else {
                    if (elem.getTypes().contains(RobotTokenType.START_HASH_COMMENT)) {
                        simple.addTextParameters(mappingResult.getTextElements());
                    } else {
                        simple.setAction(new RobotAction(elem, mappedElements));
                        simple.addUsedVariables(correctVariables);
                        isAfterFirstAction = true;
                    }
                }
            }
        }

        return simple;
    }
}
