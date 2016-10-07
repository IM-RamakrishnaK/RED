/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor.source.assist;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ContentAssistEvent;
import org.eclipse.jface.text.contentassist.ICompletionListener;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension;
import org.eclipse.jface.text.contentassist.ICompletionListenerExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.swt.widgets.Display;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;

/**
 * @author Michal Anglart
 *
 */
public class CycledContentAssistProcessor extends DefaultContentAssistProcessor implements IContentAssistProcessor,
        ICompletionListener, ICompletionListenerExtension, ICompletionListenerExtension2 {

    private final SuiteSourceAssistantContext assistContext;

    private final AssitantCallbacks assistant;

    private final List<RedContentAssistProcessor> processors;

    private int currentPage;

    private boolean canReopenAssitantProgramatically;

    public CycledContentAssistProcessor(final SuiteSourceAssistantContext assistContext,
            final AssitantCallbacks assistant) {
        this.assistContext = assistContext;
        this.assistant = assistant;
        this.processors = newArrayList();
        this.currentPage = 0;
        this.canReopenAssitantProgramatically = false;
    }

    public void addProcessor(final RedContentAssistProcessor processor) {
        processors.add(processor);
    }

    @Override
    public ICompletionProposal[] computeCompletionProposals(final ITextViewer viewer, final int offset) {
        assistant.setStatus(processors.size() == 1 ? "" : getTitle());

        final ICompletionProposal[] proposals = processors.get(currentPage).computeCompletionProposals(viewer, offset);

        currentPage = (currentPage + 1) % processors.size();
        return proposals;
    }

    private String getTitle() {
        final String title = processors.get((currentPage + 1) % processors.size()).getProposalsTitle();
        return String.format("Press Ctrl+Space to show %s proposals", title);
    }

    @Override
    public void assistSessionStarted(final ContentAssistEvent event) {
        if (event.processor == this) {
            System.err.println("started: " + toString());
            assistContext.refreshPreferences();
            canReopenAssitantProgramatically = true;
            currentPage = 0;
        }
    }

    @Override
    public void assistSessionRestarted(final ContentAssistEvent event) {
        if (event.processor == this) {
            System.err.println("restarted: " + toString());
            canReopenAssitantProgramatically = true;
            currentPage--;
            if (currentPage < 0) {
                currentPage = processors.size() - 1;
            }
        }

    }

    @Override
    public void assistSessionEnded(final ContentAssistEvent event) {
        if (event.processor == this) {
            System.err.println("ended: " + toString());
            currentPage = 0;
        }
    }

    @Override
    public void applied(final ICompletionProposal proposal) {
        // this method is called also for processors from which the proposal was not chosen
        // hence canReopenAssistantProgramatically is holding information which proccessor
        // is able to open proposals after accepting
        System.err.println("applied: " + toString() + " - " + canReopenAssitantProgramatically);
        if (canReopenAssitantProgramatically && proposal instanceof RedCompletionProposal
                && ((RedCompletionProposal) proposal).shouldActivateAssitantAfterAccepting()) {
            canReopenAssitantProgramatically = false;
            Display.getCurrent().asyncExec(new Runnable() {
                @Override
                public void run() {
                    assistant.openCompletionProposals();
                }
            });
        }
    }

    @Override
    public String toString() {
        final String string = super.toString();
        return string.substring(string.lastIndexOf('@'));
    }

    @Override
    public void selectionChanged(final ICompletionProposal proposal, final boolean smartToggle) {
        // nothing to do here
    }

    @Override
    public char[] getCompletionProposalAutoActivationCharacters() {
        return RedPlugin.getDefault().getPreferences().getAssistantAutoActivationChars();
    }

    public interface AssitantCallbacks {

        void setStatus(String title);

        void openCompletionProposals();
    }
}
