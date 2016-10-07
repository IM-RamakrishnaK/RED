/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IProject;
import org.junit.Test;
import org.rf.ide.core.executor.RobotRuntimeEnvironment;
import org.rf.ide.core.testdata.model.RobotVersion;
import org.robotframework.ide.eclipse.main.plugin.model.RobotModel;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;

public class ValidationContextTest {

    @Test
    public void construct_ValidationContext_with_RobotFramework30_installed() {
        // prepare
        RobotModel model = mock(RobotModel.class);
        IProject project = mock(IProject.class);
        RobotProject robotProject = mock(RobotProject.class);
        RobotRuntimeEnvironment robotRuntime = mock(RobotRuntimeEnvironment.class);

        when(model.createRobotProject(project)).thenReturn(robotProject);
        when(robotProject.getRuntimeEnvironment()).thenReturn(robotRuntime);
        when(robotProject.getVersion()).thenReturn("3.0");

        // execute
        ValidationContext valCtx = new ValidationContext(model, project);

        // verify
        assertThat(valCtx.getVersion().isEqualTo(new RobotVersion(3, 0))).isTrue();
    }

    @Test
    public void construct_ValidationContext_withNo_RobotFramework_installed() {
        // prepare
        RobotModel model = mock(RobotModel.class);
        IProject project = mock(IProject.class);
        RobotProject robotProject = mock(RobotProject.class);
        RobotRuntimeEnvironment robotRuntime = mock(RobotRuntimeEnvironment.class);

        when(model.createRobotProject(project)).thenReturn(robotProject);
        when(robotProject.getRuntimeEnvironment()).thenReturn(robotRuntime);
        when(robotProject.getVersion()).thenReturn(null);

        // execute
        ValidationContext valCtx = new ValidationContext(model, project);

        // verify
        assertThat(valCtx.getVersion()).isNull();
    }
}
