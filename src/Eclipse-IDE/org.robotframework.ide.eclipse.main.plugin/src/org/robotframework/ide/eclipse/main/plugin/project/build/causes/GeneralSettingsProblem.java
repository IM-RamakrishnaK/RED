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
import org.robotframework.ide.eclipse.main.plugin.project.build.fix.AddLibraryToRedXmlFixer;
import org.robotframework.ide.eclipse.main.plugin.project.build.fix.ChangeImportedPathFixer;
import org.robotframework.ide.eclipse.main.plugin.project.build.fix.DefineVariableFixer;

public enum GeneralSettingsProblem implements IProblemCause {
    UNKNOWN_SETTING {
        @Override
        public String getProblemDescription() {
            return "Unknown '%s' setting";
        }
    },
    UNSUPPORTED_SETTING {
        @Override
        public String getProblemDescription() {
            return "The setting '%s' is not supported inside %s file";
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
            return "The library name/path '%s' is parameterized. Some of used parameters cannot be resolved";
        }

        @Override
        public boolean hasResolution() {
            return true;
        }

        @Override
        public List<? extends IMarkerResolution> createFixers(final IMarker marker) {
            final String nameOrPath = marker.getAttribute("name", null);
            return DefineVariableFixer.createFixers(nameOrPath);
        }
    },
    ABSOLUTE_IMPORT_PATH {
        @Override
        public Severity getSeverity() {
            return Severity.WARNING;
        }

        @Override
        public String getProblemDescription() {
            return "Path '%s' is absolute. RED prefers relative paths";
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
            return "Unknown '%s' library";
        }

        @Override
        public boolean hasResolution() {
            return true;
        }

        @Override
        public List<? extends IMarkerResolution> createFixers(final IMarker marker) {
            final String nameOrPath = marker.getAttribute("name", null);
            final boolean isPath = marker.getAttribute("isPath", false);
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
            final IPath path = Path.fromPortableString(marker.getAttribute("path", null));
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
            return "Resource import '%s' is invalid: file does not exist";
        }
    },
    INVALID_VARIABLES_IMPORT {
        @Override
        public String getProblemDescription() {
            return "Variable import '%s' is invalid%s";
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
