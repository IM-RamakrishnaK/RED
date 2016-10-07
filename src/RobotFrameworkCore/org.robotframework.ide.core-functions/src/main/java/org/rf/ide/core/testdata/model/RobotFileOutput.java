/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.rf.ide.core.testdata.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.rf.ide.core.testdata.importer.ResourceImportReference;
import org.rf.ide.core.testdata.importer.VariablesFileImportReference;
import org.rf.ide.core.testdata.text.read.IRobotLineElement;
import org.rf.ide.core.testdata.text.read.IRobotTokenType;
import org.rf.ide.core.testdata.text.read.RobotLine;


public class RobotFileOutput {

    public static final long FILE_NOT_EXIST_EPOCH = 0;
    private File processedFile;
    private final RobotFile fileModel;
    private long lastModificationEpoch = FILE_NOT_EXIST_EPOCH;
    private final List<ResourceImportReference> resourceReferences = new ArrayList<>();
    private final List<VariablesFileImportReference> variablesReferenced = new ArrayList<>();
    private final List<BuildMessage> buildingMessages = new ArrayList<>();
    private Status status = Status.FAILED;


    public RobotFileOutput() {
        this.fileModel = new RobotFile(this);
    }


    public String getFileLineSeparator() {
        String result = "";

        final List<RobotLine> fileContent = fileModel.getFileContent();
        if (!fileContent.isEmpty()) {
            final IRobotLineElement endOfLine = fileContent.get(0)
                    .getEndOfLine();
            final List<IRobotTokenType> types = endOfLine.getTypes();
            if (!types.isEmpty()) {
                final IRobotTokenType eolType = types.get(0);
                final List<String> representation = eolType.getRepresentation();
                if (!representation.isEmpty()) {
                    result = representation.get(0);
                }
            }
        }

        return result;
    }


    public File getProcessedFile() {
        return processedFile;
    }


    public void setProcessedFile(final File processedFile) {
        this.processedFile = processedFile;
        this.lastModificationEpoch = processedFile.lastModified();
    }


    public void setLastModificationEpochTime(final long lastModificationEpoch) {
        this.lastModificationEpoch = lastModificationEpoch;
    }


    public long getLastModificationEpochTime() {
        return lastModificationEpoch;
    }


    public RobotFile getFileModel() {
        return fileModel;
    }


    public List<BuildMessage> getBuildingMessages() {
        return Collections.unmodifiableList(buildingMessages);
    }


    public void addBuildMessage(final BuildMessage msg) {
        buildingMessages.add(msg);
    }


    public void addResourceReferences(
            final List<ResourceImportReference> references) {
        for (final ResourceImportReference resourceImportReference : references) {
            addResourceReference(resourceImportReference);
        }
    }


    public void addResourceReference(final ResourceImportReference ref) {
        final int positionToSet = findResourceReferencePositionToReplace(ref);

        if (positionToSet == -1) {
            resourceReferences.add(ref);
        } else {
            resourceReferences.set(positionToSet, ref);
        }
    }


    private int findResourceReferencePositionToReplace(
            final ResourceImportReference ref) {
        int positionToSet = -1;

        final int numberOfReferences = resourceReferences.size();
        for (int i = 0; i < numberOfReferences; i++) {
            final ResourceImportReference reference = resourceReferences.get(i);
            final File file = reference.getReference().getProcessedFile();
            final File thisFile = ref.getReference().getProcessedFile();
            boolean isSameFile = false;
            try {
                if (Files.isSameFile(file.toPath(), thisFile.toPath())) {
                    isSameFile = true;
                }
            } catch (final IOException e) {
                if (file.toPath().normalize().toAbsolutePath()
                        .equals(thisFile.toPath().normalize().toAbsolutePath())) {
                    isSameFile = true;
                }
            }

            if (isSameFile) {
                positionToSet = i;
                break;
            }
        }

        return positionToSet;
    }


    public List<ResourceImportReference> getResourceImportReferences() {
        return Collections.unmodifiableList(resourceReferences);
    }


    public void addVariablesReferenced(
            final List<VariablesFileImportReference> varsImported) {
        for (final VariablesFileImportReference variablesFileImportReference : varsImported) {
            addVariablesReference(variablesFileImportReference);
        }
    }


    public void addVariablesReference(
            final VariablesFileImportReference varImportRef) {
        variablesReferenced.add(varImportRef);
    }


    public List<VariablesFileImportReference> getVariablesImportReferences() {
        return Collections.unmodifiableList(variablesReferenced);
    }

    public static class BuildMessage {

        private final LogLevel type;
        private final String message;
        private String fileName;
        private FileRegion fileRegion;


        public BuildMessage(final LogLevel level, final String message,
                final String fileName) {
            this.type = level;
            this.message = message;
            this.fileName = fileName.intern();
        }


        public static BuildMessage createInfoMessage(final String message,
                final String fileName) {
            return new BuildMessage(LogLevel.INFO, message, fileName);
        }


        public static BuildMessage createWarnMessage(final String message,
                final String fileName) {
            return new BuildMessage(LogLevel.WARN, message, fileName);
        }


        public static BuildMessage createErrorMessage(final String message,
                final String fileName) {
            return new BuildMessage(LogLevel.ERROR, message, fileName);
        }


        public String getFileName() {
            return fileName;
        }


        public void setFileName(final String fileName) {
            this.fileName = fileName;
        }


        public FileRegion getFileRegion() {
            return fileRegion;
        }


        public void setFileRegion(final FileRegion fileRegion) {
            this.fileRegion = fileRegion;
        }


        public LogLevel getType() {
            return type;
        }


        public String getMessage() {
            return message;
        }

        public static enum LogLevel {
            INFO, WARN, ERROR;
        }


        @Override
        public String toString() {
            return String
                    .format("BuildMessage [type=%s, message=%s, fileName=%s, fileRegion=%s]",
                            type, message, fileName, fileRegion);
        }
    }


    public Status getStatus() {
        return status;
    }


    public void setStatus(final Status status) {
        this.status = status;
    }

    public static enum Status {
        FAILED, PASSED
    }


    public RobotFileType getType() {
        RobotFileType judgedType = RobotFileType.UNKNOWN;
        if (fileModel != null) {
            if (processedFile.isFile()) {
                final String name = processedFile.getName().toLowerCase();
                if (name.startsWith("__init__")) {
                    judgedType = RobotFileType.TEST_SUITE_INIT;
                } else {
                    if (fileModel.getTestCaseTable().isPresent()) {
                        judgedType = RobotFileType.TEST_SUITE;
                    } else if (fileModel.containsAnyRobotSection()) {
                        judgedType = RobotFileType.RESOURCE;
                    } else {
                        judgedType = RobotFileType.UNKNOWN;
                    }
                }
            } else {
                judgedType = RobotFileType.TEST_SUITE_DIR;
            }
        }

        return judgedType;
    }

    public enum RobotFileType {
        UNKNOWN, RESOURCE, TEST_SUITE, TEST_SUITE_DIR, TEST_SUITE_INIT;
    }
}
