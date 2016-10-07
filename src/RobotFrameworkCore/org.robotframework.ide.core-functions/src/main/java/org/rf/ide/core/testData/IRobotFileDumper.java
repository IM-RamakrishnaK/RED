/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testData;

import java.io.File;

import org.rf.ide.core.testData.model.RobotFile;


public interface IRobotFileDumper {

    boolean canDumpFile(final File file);


    void dump(final File robotFile, final RobotFile model) throws Exception;


    String dump(final RobotFile model);
}
