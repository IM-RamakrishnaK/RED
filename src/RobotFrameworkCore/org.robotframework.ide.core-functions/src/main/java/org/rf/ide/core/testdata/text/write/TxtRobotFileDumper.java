/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.text.write;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.rf.ide.core.testdata.IRobotFileDumper;
import org.rf.ide.core.testdata.model.RobotFile;


public class TxtRobotFileDumper implements IRobotFileDumper {

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    @Override
    public void dump(final File destFile, final RobotFile model)
            throws Exception {
        Path tempFile = Files.createTempFile(
                destFile.getName() + System.currentTimeMillis(), "txt_temp");
        BufferedWriter writer = new BufferedWriter(new FileWriter(
                tempFile.toFile()));

        writer.write(dump(model));

        writer.flush();
        writer.close();

        Files.move(tempFile, destFile.toPath(), StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        Files.delete(tempFile);
    }


    public String dump(final RobotFile model) {
        StringBuilder str = new StringBuilder();

        return str.toString();
    }


    @Override
    public boolean canDumpFile(File file) {
        boolean result = false;

        if (file != null && file.isFile()) {
            String fileName = file.getName().toLowerCase();
            result = (fileName.endsWith(".txt") || fileName.endsWith(".robot"));
        }

        return result;
    }
}
