/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.robotframework.ide.eclipse.main.plugin.RedImages;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.DocumentUtilities;
import org.robotframework.ide.eclipse.main.plugin.tableeditor.source.SuiteSourcePartitionScanner;
import org.robotframework.red.graphics.ImagesManager;

import com.google.common.base.Optional;

/**
 * @author Michal Anglart
 *
 */
public class VariablesDefinitionsProcessor extends RedContentAssistProcessor {

    private static final Collection<VarDef> VARIABLE_DEFS = EnumSet.allOf(VarDef.class);

    private final SuiteSourceAssistantContext assist;

    public VariablesDefinitionsProcessor(final SuiteSourceAssistantContext assist) {
        this.assist = assist;
    }

    @Override
    protected List<String> getValidContentTypes() {
        return newArrayList(SuiteSourcePartitionScanner.VARIABLES_SECTION);
    }

    @Override
    protected String getProposalsTitle() {
        return "Variable definitions";
    }

    @Override
    public List<? extends ICompletionProposal> computeProposals(final ITextViewer viewer, final int offset) {
        final IDocument document = viewer.getDocument();
        try {
            final IRegion lineInformation = document.getLineInformationOfOffset(offset);
            final boolean shouldShowProposal = shouldShowProposals(offset, document, lineInformation);

            if (shouldShowProposal) {
                final String prefix = DocumentUtilities.getPrefix(document, Optional.of(lineInformation), offset);
                final Optional<IRegion> cellRegion = DocumentUtilities.findCellRegion(document, offset);
                final String content = cellRegion.isPresent()
                        ? document.get(cellRegion.get().getOffset(), cellRegion.get().getLength()) : "";
                final String separator = assist.getSeparatorToFollow();

                final List<ICompletionProposal> proposals = newArrayList();
                for (final VarDef varDef : VARIABLE_DEFS) {
                    if (varDef.content.toLowerCase().startsWith(prefix.toLowerCase())) {
                        final String textToInsert = varDef.content + separator;

                        final RedCompletionProposal proposal = RedCompletionBuilder.newProposal()
                                .will(assist.getAcceptanceMode())
                                .theText(textToInsert)
                                .atOffset(lineInformation.getOffset())
                                .givenThatCurrentPrefixIs(prefix)
                                .andWholeContentIs(content)
                                .secondaryPopupShouldBeDisplayed(varDef.info)
                                .thenCursorWillStopAt(2, varDef.content.length() - 3)
                                .displayedLabelShouldBe(varDef.label)
                                .proposalsShouldHaveIcon(varDef.getImage())
                                .create();
                        proposals.add(proposal);
                    }
                }
                return proposals;

            }
            return null;
        } catch (final BadLocationException e) {
            return null;
        }
    }

    private boolean shouldShowProposals(final int offset, final IDocument document, final IRegion lineInformation)
            throws BadLocationException {
        if (isInProperContentType(document, offset)) {
            // we only want to show those proposals in first cell of the line
            if (offset != lineInformation.getOffset()) {
                final Optional<IRegion> cellRegion = DocumentUtilities.findLiveCellRegion(document, offset);
                return cellRegion.isPresent() && lineInformation.getOffset() == cellRegion.get().getOffset();
            } else {
                return true;
            }
        }
        return false;
    }

    private enum VarDef {
        SCALAR("${newScalar}", "Fresh scalar", 
                RedImages.getRobotScalarVariableImage(), "Creates fresh scalar variable"),
        LIST("@{newList}", "Fresh list", 
                RedImages.getRobotListVariableImage(), "Creates fresh list variable"),
        DICT("&{newDict}", "Fresh dictionary", 
                RedImages.getRobotDictionaryVariableImage(), "Creates fresh dictionary variable");

        private String content;

        private String label;

        private ImageDescriptor image;

        private String info;

        private VarDef(final String content, final String label, final ImageDescriptor image, final String info) {
            this.content = content;
            this.label = label;
            this.image = image;
            this.info = info;
        }

        Image getImage() {
            return ImagesManager.getImage(image);
        }
    }
}
