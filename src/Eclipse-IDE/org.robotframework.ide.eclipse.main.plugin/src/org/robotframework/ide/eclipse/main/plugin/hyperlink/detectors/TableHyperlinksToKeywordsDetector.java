/*
 * Copyright 2016 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.hyperlink.detectors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.nebula.widgets.nattable.data.IRowDataProvider;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.hyperlink.SuiteFileTableElementHyperlink;
import org.robotframework.ide.eclipse.main.plugin.model.RobotFileInternalElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModel;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;

import com.google.common.annotations.VisibleForTesting;

public class TableHyperlinksToKeywordsDetector extends HyperlinksToKeywordsDetector implements ITableHyperlinksDetector {

    private final IRowDataProvider<? extends Object> dataProvider;

    public TableHyperlinksToKeywordsDetector(final IRowDataProvider<? extends Object> dataProvider) {
        this(RedPlugin.getModelManager().getModel(), dataProvider);
    }

    @VisibleForTesting
    TableHyperlinksToKeywordsDetector(final RobotModel model, final IRowDataProvider<? extends Object> dataProvider) {
        super(model);
        this.dataProvider = dataProvider;
    }

    @Override
    public List<IHyperlink> detectHyperlinks(final int row, final int column, final String label, final int indexInLabel) {
        final Object rowObject = dataProvider.getRowObject(row);
        if (rowObject instanceof RobotFileInternalElement) {
            final RobotFileInternalElement element = (RobotFileInternalElement) rowObject;
            final RobotSuiteFile suiteFile = element.getSuiteFile();

            return detectHyperlinks(suiteFile, new Region(0, label.length()), label);
        }
        return new ArrayList<>();
    }

    @Override
    protected IHyperlink createLocalKeywordHyperlink(final KeywordHyperlinkEntity keywordEntity, final IRegion from,
            final String additionalInfo) {
        return new SuiteFileTableElementHyperlink(from, keywordEntity.exposingResource, keywordEntity.userKeyword,
                null);
    }

    @Override
    protected IHyperlink createResourceKeywordHyperlink(final KeywordHyperlinkEntity keywordEntity, final IRegion from,
            final String additionalInfo) {
        return new SuiteFileTableElementHyperlink(from, keywordEntity.exposingResource, keywordEntity.userKeyword,
                null);
    }
}
