/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build.causes;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.robotframework.ide.eclipse.main.plugin.project.build.AdditionalMarkerAttributes;
import org.robotframework.ide.eclipse.main.plugin.project.build.fix.DefineLocalVariableFixer;
import org.robotframework.ide.eclipse.main.plugin.project.build.fix.RemoveVariableFixer;
import org.robotframework.ide.eclipse.main.plugin.project.build.fix.RemoveWhitespacesFromVariableNameFixer;


public enum VariablesProblem implements IProblemCause {
    DUPLICATED_VARIABLE {
        @Override
        public String getProblemDescription() {
            return "Duplicated variable definition '%s'";
        }

        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public boolean hasResolution() {
            return true;
        }

        @Override
        public List<? extends IMarkerResolution> createFixers(final IMarker marker) {
            return newArrayList(new RemoveVariableFixer(marker.getAttribute(AdditionalMarkerAttributes.NAME, null)));
        }
    },
    INVALID_TYPE {
        @Override
        public String getProblemDescription() {
            return "Invalid variable definition '%s'. Unable to recognize variable type";
        }

        @Override
        public boolean hasResolution() {
            return true;
        }

        @Override
        public List<? extends IMarkerResolution> createFixers(final IMarker marker) {
            return newArrayList(new RemoveVariableFixer(marker.getAttribute(AdditionalMarkerAttributes.NAME, null)));
        }
    },
    INVALID_NAME {
        @Override
        public String getProblemDescription() {
            return "Invalid variable name '%s'. Name can't contain whitespaces after the type identificator";
        }

        @Override
        public boolean hasResolution() {
            return true;
        }

        @Override
        public List<? extends IMarkerResolution> createFixers(final IMarker marker) {
            return newArrayList(new RemoveWhitespacesFromVariableNameFixer(
                    marker.getAttribute(AdditionalMarkerAttributes.NAME, null)));
        }
    },    
    DICTIONARY_NOT_AVAILABLE {
        @Override
        public String getProblemDescription() {
            return "Invalid variable definition '%s'. Dictionary type is available since Robot Framework 2.9.x version";
        }
    },
    SCALAR_WITH_MULTIPLE_VALUES_2_7 {
        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public String getProblemDescription() {
            return "Scalar variable '%s' is initialized with list value";
        }
    },
    SCALAR_WITH_MULTIPLE_VALUES_2_8_x {
        @Override
        public String getProblemDescription() {
            return "Invalid variable definition '%s'. Scalar variable cannot have multiple value in RobotFramework 2.8.x";
        }
    },
    UNDECLARED_VARIABLE_USE {
        @Override
        public String getProblemDescription() {
            return "Variable '%s' is used, but not defined";
        }

        @Override
        public boolean hasResolution() {
            return true;
        }

        @Override
        public List<? extends IMarkerResolution> createFixers(final IMarker marker) {
            return newArrayList(new DefineLocalVariableFixer(marker.getAttribute(AdditionalMarkerAttributes.NAME, "")));
        }
    };

    @Override
    public Severity getSeverity() {
        return Severity.ERROR;
    }

    @Override
    public boolean hasResolution() {
        return false;
    }

    @Override
    public List<? extends IMarkerResolution> createFixers(final IMarker marker) {
        return newArrayList();
    }

    @Override
    public ProblemCategory getProblemCategory() {
        return null;
    }

    @Override
    public String getEnumClassName() {
        return VariablesProblem.class.getName();
    }
}
