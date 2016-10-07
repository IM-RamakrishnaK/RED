/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.assist;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.robotframework.ide.eclipse.main.plugin.RedImages;
import org.robotframework.ide.eclipse.main.plugin.model.RobotKeywordDefinition;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.project.library.KeywordSpecification;
import org.robotframework.ide.eclipse.main.plugin.project.library.LibrarySpecification;

import com.google.common.base.Joiner;

public class RedKeywordProposal {

    private final String sourceName;
    private final KeywordType type;
    private final String name;
    private final String decoration;
    private final boolean hasDescription;
    private final String documentation;

    private final LazyProvider<List<String>> argumentsProvider;
    private final LazyProvider<String> htmlDocumentationProvider;

    private RedKeywordProposal(final String sourceName, final KeywordType type, final String name,
            final String decoration, final boolean hasDescription, final LazyProvider<List<String>> argumentsProvider,
            final LazyProvider<String> htmlDocumentationProvider, final String documentation) {
        this.sourceName = sourceName;
        this.type = type;
        this.name = name;
        this.decoration = decoration;
        this.hasDescription = hasDescription;
        this.documentation = documentation;

        this.htmlDocumentationProvider = htmlDocumentationProvider;
        this.argumentsProvider = argumentsProvider;
    }

    static RedKeywordProposal create(final LibrarySpecification spec, final KeywordSpecification keyword) {
        final LazyProvider<String> htmlDocuProvider = new LazyProvider<String>() {
            @Override
            public String provide() {
                return keyword.getDocumentationAsHtml();
            }
        };
        final LazyProvider<List<String>> argsProvider = new LazyProvider<List<String>>() {
            @Override
            public List<String> provide() {
                return keyword.getArguments() == null ? new ArrayList<String>() : keyword.getArguments();
            }
        };
        return new RedKeywordProposal(spec.getName(), KeywordType.LIBRARY, keyword.getName(), "- " + spec.getName(),
                true, argsProvider, htmlDocuProvider, keyword.getDocumentation());
    }

    static RedKeywordProposal create(final RobotSuiteFile file, final RobotKeywordDefinition userKeyword) {
        final LazyProvider<String> htmlDocuProvider = new LazyProvider<String>() {
            @Override
            public String provide() {
                return "<p>to be implemented</p>";
            }
        };
        final LazyProvider<List<String>> argsProvider = new LazyProvider<List<String>>() {
            @Override
            public List<String> provide() {
                return userKeyword.getArguments();
            }
        };
        return new RedKeywordProposal("User Defined (" + file.getName() + ")", KeywordType.USER_DEFINED,
                userKeyword.getName(), "- " + file.getName(), true, argsProvider, htmlDocuProvider,
                userKeyword.getDocumentation());
    }

    public String getSourceName() {
        return sourceName;
    }

    public KeywordType getType() {
        return type;
    }

    public String getLabel() {
        return name;
    }

    public String getLabelDecoration() {
        return decoration;
    }

    public String getContent() {
        return name;
    }

    public boolean hasDescription() {
        return hasDescription;
    }

    public ImageDescriptor getImage() {
        return type.getImage();
    }

    public String getHtmlDocumentation() {
        return htmlDocumentationProvider.provide();
    }

    public String getArgumentsLabel() {
        return "[" + Joiner.on(" | ").join(argumentsProvider.provide()) + "]";
    }

    private static interface LazyProvider<T> {
        T provide();
    }

    public String getDocumentation() {
        return String.format("Name: %s\nSource: %s\nArguments: %s\n\n%s", name, sourceName, getArgumentsLabel(),
                documentation);
    }

    public enum KeywordType {
        LIBRARY(RedImages.getKeywordImage()),
        USER_DEFINED(RedImages.getUserKeywordImage());

        private ImageDescriptor image;

        private KeywordType(final ImageDescriptor image) {
            this.image = image;
        }

        private ImageDescriptor getImage() {
            return image;
        }
    }
}
