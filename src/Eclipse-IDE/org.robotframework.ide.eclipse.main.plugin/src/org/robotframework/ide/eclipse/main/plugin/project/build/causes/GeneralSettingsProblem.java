/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build.causes;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.IMarkerResolution;
import org.robotframework.ide.eclipse.main.plugin.project.build.AdditionalMarkerAttributes;
import org.robotframework.ide.eclipse.main.plugin.project.build.fix.AddLibraryToRedXmlFixer;
import org.robotframework.ide.eclipse.main.plugin.project.build.fix.ChangeImportedPathFixer;
import org.robotframework.ide.eclipse.main.plugin.project.build.fix.DefineGlobalVariableInConfigFixer;

public enum GeneralSettingsProblem implements IProblemCause {
    UNKNOWN_SETTING {

        @Override
        public String getProblemDescription() {
            return "Unknown '%s' setting";
        }
    },
    EMPTY_SETTING {

        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public String getProblemDescription() {
            return "Empty setting '%s'";
        }
    },
    UNSUPPORTED_SETTING {

        @Override
        public String getProblemDescription() {
            return "The setting '%s' is not supported inside %s file";
        }
    },
    DUPLICATED_SETTING {

        @Override
        public String getProblemDescription() {
            return "The setting '%s' is duplicated";
        }
    },
    MISSING_LIBRARY_NAME {

        @Override
        public String getProblemDescription() {
            return "Empty setting '%s'. Specify name or path of library to import";
        }
    },
    MISSING_RESOURCE_NAME {

        @Override
        public String getProblemDescription() {
            return "Empty setting '%s'. Specify path of resource file to import";
        }
    },
    MISSING_VARIABLES_NAME {

        @Override
        public String getProblemDescription() {
            return "Empty setting '%s'. Specify path of variable file to import";
        }
    },
    PARAMETERIZED_IMPORT_PATH {

        @Override
        public String getProblemDescription() {
            return "The library name/path '%s' is parameterized. Some of used parameters cannot be resolved. Use Variable mappings in red.xml for parameter resolution";
        }

        @Override
        public boolean hasResolution() {
            return true;
        }

        @Override
        public List<? extends IMarkerResolution> createFixers(final IMarker marker) {
            final String nameOrPath = marker.getAttribute(AdditionalMarkerAttributes.NAME, null);
            return DefineGlobalVariableInConfigFixer.createFixers(nameOrPath);
        }
    },
    ABSOLUTE_IMPORT_PATH {

        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public String getProblemDescription() {
            return "Path '%s' is absolute. It is preferred to use relative paths";
        }
    },
    IMPORT_PATH_OUTSIDE_WORKSPACE {

        @Override
        public String getProblemDescription() {
            return "Path '%s' points to location outside your workspace";
        }
    },
    UNKNOWN_LIBRARY {

        @Override
        public String getProblemDescription() {
            return "Unknown '%s' library. Try to use Quick Fix (Ctrl+1) or add library to red.xml for proper validation";
        }

        @Override
        public boolean hasResolution() {
            return true;
        }

        @Override
        public List<? extends IMarkerResolution> createFixers(final IMarker marker) {
            final String nameOrPath = marker.getAttribute(AdditionalMarkerAttributes.NAME, null);
            final boolean isPath = marker.getAttribute(AdditionalMarkerAttributes.IS_PATH, false);
            return newArrayList(new AddLibraryToRedXmlFixer(nameOrPath, isPath));
        }
    },
    SETTING_ARGUMENTS_NOT_APPLICABLE {

        @Override
        public String getProblemDescription() {
            return "Setting '%s' is not applicable for arguments: %s. %s";
        }
    },
    NON_EXISTING_RESOURCE_IMPORT {

        @Override
        public String getProblemDescription() {
            return "Resource import '%s' is invalid: file does not exist";
        }

        @Override
        public boolean hasResolution() {
            return true;
        }

        @Override
        public List<? extends IMarkerResolution> createFixers(final IMarker marker) {
            final IPath path = Path.fromPortableString(marker.getAttribute(AdditionalMarkerAttributes.PATH, null));
            return ChangeImportedPathFixer.createFixersForSameFile((IFile) marker.getResource(), path);
        }
    },
    INVALID_RESOURCE_IMPORT {

        @Override
        public String getProblemDescription() {
            return "Resource import '%s' is invalid%s";
        }
    },
    NON_EXISTING_VARIABLES_IMPORT {

        @Override
        public String getProblemDescription() {
            return "Resource import '%s' is invalid: file does not exist. Check file name and path.";
        }
    },
    INVALID_VARIABLES_IMPORT {

        @Override
        public String getProblemDescription() {
            return "Variable import '%s' is invalid%s";
        }
    },
    DUPLICATED_TEMPLATE_28 {

        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public String getProblemDescription() {
            return "Setting '%s' is duplicated. Robot will try to use '%s' keyword as template";
        }
    },
    DUPLICATED_SUITE_SETUP_28 {

        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public String getProblemDescription() {
            return "Setting '%s' is duplicated. Robot will try to use '%s' keyword as suite setup";
        }
    },
    DUPLICATED_SUITE_TEARDOWN_28 {

        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public String getProblemDescription() {
            return "Setting '%s' is duplicated. Robot will try to use '%s' keyword as suite teardown";
        }
    },
    DUPLICATED_TEST_SETUP_28 {

        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public String getProblemDescription() {
            return "Setting '%s' is duplicated. Robot will try to use '%s' keyword as test setup";
        }
    },
    DUPLICATED_TEST_TEARDOWN_28 {

        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public String getProblemDescription() {
            return "Setting '%s' is duplicated. Robot will try to use '%s' keyword as test teardown";
        }
    },
    DUPLICATED_TEST_TIMEOUT_28 {

        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public String getProblemDescription() {
            return "Setting '%s' is duplicated. Robot will try to use '%s' as test timeout";
        }
    },
    DUPLICATED_FORCE_TAGS_28 {

        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public String getProblemDescription() {
            return "Setting '%s' is duplicated. Robot will try to use '%s' merged elements as force tags";
        }
    },
    DUPLICATED_DEFAULT_TAGS_28 {

        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public String getProblemDescription() {
            return "Setting '%s' is duplicated. Robot will try to use '%s' merged elements as default tags";
        }
    },
    DUPLICATED_DOCUMENTATION_28 {

        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public String getProblemDescription() {
            return "Setting '%s' is duplicated. Robot will try to use merged elements as documentation";
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
        return GeneralSettingsProblem.class.getName();
    }
}
