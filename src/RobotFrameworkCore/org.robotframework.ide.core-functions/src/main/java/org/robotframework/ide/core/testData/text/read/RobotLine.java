/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.core.testData.text.read;

import java.util.ArrayList;
import java.util.List;

import org.robotframework.ide.core.testData.model.IChildElement;
import org.robotframework.ide.core.testData.model.RobotFile;
import org.robotframework.ide.core.testData.text.read.LineReader.Constant;
import org.robotframework.ide.core.testData.text.read.columnSeparators.Separator.SeparatorType;

import com.google.common.base.Optional;


public class RobotLine implements IChildElement<RobotFile> {

    private final RobotFile parent;
    private int lineNumber = -1;
    private List<IRobotLineElement> lineElements = new ArrayList<>(0);
    private Optional<SeparatorType> separatorForLine = Optional.absent();

    private IRobotLineElement eol = EndOfLineBuilder.newInstance()
            .setEndOfLines(null).setLineNumber(IRobotLineElement.NOT_SET)
            .setStartColumn(IRobotLineElement.NOT_SET)
            .setStartOffset(IRobotLineElement.NOT_SET).buildEOL();


    public RobotLine(final int lineNumber, final RobotFile parent) {
        this.lineNumber = lineNumber;
        this.parent = parent;
    }


    public void setSeparatorType(final SeparatorType separatorForLine) {
        if (separatorForLine == null) {
            this.separatorForLine = Optional.absent();
        } else {
            this.separatorForLine = Optional.of(separatorForLine);
        }
    }


    public Optional<SeparatorType> getSeparatorForLine() {
        return separatorForLine;
    }


    @Override
    public RobotFile getParent() {
        return parent;
    }


    public List<IRobotLineElement> getLineElements() {
        return lineElements;
    }


    public void setLineElements(final List<IRobotLineElement> lineElements) {
        this.lineElements = lineElements;
    }


    public void addLineElement(final IRobotLineElement lineElement) {
        this.lineElements.add(lineElement);
    }


    public void addLineElementAt(final int position, final IRobotLineElement lineElement) {
        this.lineElements.add(position, lineElement);
    }


    public int getLineNumber() {
        return lineNumber;
    }


    public IRobotLineElement getEndOfLine() {
        return this.eol;
    }


    public void setEndOfLine(final List<Constant> endOfLine, final int currentOffset,
            final int currentColumn) {
        this.eol = EndOfLineBuilder.newInstance().setEndOfLines(endOfLine)
                .setStartColumn(currentColumn).setStartOffset(currentOffset)
                .setLineNumber(lineNumber).buildEOL();
    }


    @Override
    public String toString() {
        return String.format(
                "RobotLine [lineNumber=%s, lineElements=%s, endOfLine=%s]",
                lineNumber, lineElements, eol);
    }

}
