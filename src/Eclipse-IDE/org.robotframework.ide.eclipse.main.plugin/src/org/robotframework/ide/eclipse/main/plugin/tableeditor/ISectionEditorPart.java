/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.tableeditor;

import java.util.List;
import java.util.Optional;

import org.robotframework.ide.eclipse.main.plugin.hyperlink.detectors.ITableHyperlinksDetector;
import org.robotframework.ide.eclipse.main.plugin.model.RobotElement;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFileSection;

public interface ISectionEditorPart {

    String SECTION_FILTERING_TOPIC = "red/suite_editor/section/filter/changed";

    String getId();

    void updateOnActivation();

    void aboutToChangeToOtherPage();

    Optional<? extends RobotSuiteFileSection> provideSection(RobotSuiteFile suiteModel);

    boolean isPartFor(RobotSuiteFileSection section);

    void revealElement(RobotElement robotElement);

    void revealElementAndFocus(RobotElement robotElement);

    void setFocus();

    SelectionLayerAccessor getSelectionLayerAccessor();
    
    List<ITableHyperlinksDetector> getDetectors();

    Optional<TreeLayerAccessor> getTreeLayerAccessor();

    void waitForPendingJobs();
}
