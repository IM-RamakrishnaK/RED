/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.editor;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;

import java.io.File;
import java.util.List;

import org.robotframework.ide.core.executor.RobotRuntimeEnvironment;
import org.robotframework.ide.core.executor.RobotRuntimeEnvironment.RobotEnvironmentException;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

class PythonLibStructureBuilder {
    
    private final RobotRuntimeEnvironment environment;

    public PythonLibStructureBuilder(final RobotRuntimeEnvironment environment) {
        this.environment = environment;
    }

    public List<PythonClass> provideEntriesFromFile(final String path) throws RobotEnvironmentException {
        
        final List<String> classes = environment.getClassesDefinedInModule(new File(path));
        return newArrayList(transform(classes, new Function<String, PythonClass>() {
            @Override
            public PythonClass apply(final String name) {
                return PythonClass.create(name);
            }
        }));
    }

    static class PythonClass {
        private final String qualifiedName;

        private PythonClass(final String qualifiedName) {
            this.qualifiedName = qualifiedName;
        }

        private static PythonClass create(final String name) {
            final List<String> splitted = newArrayList(Splitter.on('.').splitToList(name));
            if (splitted.size() > 1) {
                final String last = splitted.get(splitted.size() - 1);
                final String beforeLast = splitted.get(splitted.size() - 2);

                // ROBOT requires whole qualified name of class if it is defined with different name
                // than module
                // containing it in module
                if (last.equals(beforeLast)) {
                    splitted.remove(splitted.size() - 1);
                }
                return new PythonClass(Joiner.on('.').join(splitted));
            } else {
                return new PythonClass(name);
            }
        }

        public String getQualifiedName() {
            return qualifiedName;
        }
    }
}
