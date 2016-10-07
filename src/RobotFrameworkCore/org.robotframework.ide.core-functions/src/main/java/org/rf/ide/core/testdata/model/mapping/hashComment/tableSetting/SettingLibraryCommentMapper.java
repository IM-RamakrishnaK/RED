/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testData.model.mapping.hashComment.tableSetting;

import java.util.List;

import org.rf.ide.core.testData.model.RobotFile;
import org.rf.ide.core.testData.model.mapping.IHashCommentMapper;
import org.rf.ide.core.testData.model.table.setting.AImported;
import org.rf.ide.core.testData.model.table.setting.LibraryImport;
import org.rf.ide.core.testData.text.read.ParsingState;
import org.rf.ide.core.testData.text.read.recognizer.RobotToken;


public class SettingLibraryCommentMapper implements IHashCommentMapper {

    @Override
    public boolean isApplicable(ParsingState state) {
        return (state == ParsingState.SETTING_LIBRARY_IMPORT);
    }


    @Override
    public void map(RobotToken rt, ParsingState currentState,
            RobotFile fileModel) {
        List<AImported> imports = fileModel.getSettingTable().getImports();
        if (!imports.isEmpty()) {
            AImported aImported = imports.get(imports.size() - 1);
            if (aImported instanceof LibraryImport) {
                LibraryImport lib = (LibraryImport) aImported;
                lib.addCommentPart(rt);
            } else {
                // FIXME: error
            }
        } else {
            // FIXME: errors
        }
    }
}
