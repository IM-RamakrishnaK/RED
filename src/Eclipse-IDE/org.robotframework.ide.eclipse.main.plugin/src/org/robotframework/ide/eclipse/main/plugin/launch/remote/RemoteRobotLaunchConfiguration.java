/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.launch.remote;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.RedPreferences;
import org.robotframework.ide.eclipse.main.plugin.launch.IRemoteRobotLaunchConfiguration;
import org.robotframework.ide.eclipse.main.plugin.launch.LaunchConfigurationsWrappers;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;

import com.google.common.collect.Range;
import com.google.common.primitives.Ints;

public class RemoteRobotLaunchConfiguration implements IRemoteRobotLaunchConfiguration {

    public static final String TYPE_ID = "org.robotframework.ide.remoteRobotLaunchConfiguration";

    private final ILaunchConfiguration configuration;

    public RemoteRobotLaunchConfiguration(final ILaunchConfiguration config) {
        this.configuration = config;
    }

    @Override
    public void fillDefaults() throws CoreException {
        final RedPreferences preferences = RedPlugin.getDefault().getPreferences();
        setProjectName("");
        setProcessFactory(LaunchConfigurationsWrappers.FACTORY_ID);
        setRemoteDebugHostValue(preferences.getLaunchRemoteHost());
        setRemoteDebugPortValue(preferences.getLaunchRemotePort());
        setRemoteDebugTimeoutValue(preferences.getLaunchRemoteTimeout());
    }

    private void setProcessFactory(final String id) throws CoreException {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        launchCopy.setAttribute(DebugPlugin.ATTR_PROCESS_FACTORY_ID, id);
    }

    @Override
    public String getName() {
        return configuration.getName();
    }

    @Override
    public String getTypeName() {
        try {
            return configuration.getType().getName();
        } catch (final CoreException e) {
            return null;
        }
    }

    @Override
    public boolean isDefiningProjectDirectly() {
        return true;
    }

    @Override
    public String getProjectName() throws CoreException {
        return configuration.getAttribute(PROJECT_NAME_ATTRIBUTE, "");
    }

    @Override
    public RobotProject getRobotProject() throws CoreException {
        final IProject project = getProject();
        return RedPlugin.getModelManager().getModel().createRobotProject(project);
    }

    private IProject getProject() throws CoreException {
        final String projectName = getProjectName();
        if (projectName.isEmpty()) {
            return null;
        }
        final IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (!project.exists()) {
            throw newCoreException("Project '" + projectName + "' cannot be found in workspace");
        }
        return project;
    }

    @Override
    public List<IResource> getResourcesUnderDebug() throws CoreException {
        return newArrayList(getProject());
    }

    @Override
    public void setProjectName(final String projectName) throws CoreException {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        launchCopy.setAttribute(PROJECT_NAME_ATTRIBUTE, projectName);
    }

    @Override
    public String getRemoteDebugHost() throws CoreException {
        final String host = getRemoteDebugHostValue();
        if (host.isEmpty()) {
            throw newCoreException("Server IP cannot be empty");
        }
        return host;
    }

    @Override
    public int getRemoteDebugPort() throws CoreException {
        final String port = getRemoteDebugPortValue();
        final Integer portAsInt = Ints.tryParse(port);
        if (portAsInt == null || !Range.closed(1, 65_535).contains(portAsInt)) {
            throw newCoreException("Server port '" + port + "' should be a value from range [1, 65 535]");
        }
        return portAsInt;
    }

    @Override
    public int getRemoteDebugTimeout() throws CoreException {
        final String timeout = getRemoteDebugTimeoutValue();
        final Integer timeoutAsInt = Ints.tryParse(timeout);
        if (timeoutAsInt == null || !Range.atLeast(1).contains(timeoutAsInt)) {
            throw newCoreException("Connection timeout '" + timeout + "' should be a positive integer value");
        }
        return timeoutAsInt;
    }

    @Override
    public String getRemoteDebugHostValue() throws CoreException {
        return configuration.getAttribute(REMOTE_HOST_ATTRIBUTE, "");
    }

    @Override
    public String getRemoteDebugPortValue() throws CoreException {
        return configuration.getAttribute(REMOTE_PORT_ATTRIBUTE, "");
    }

    @Override
    public String getRemoteDebugTimeoutValue() throws CoreException {
        return configuration.getAttribute(REMOTE_TIMEOUT_ATTRIBUTE, "");
    }

    @Override
    public void setRemoteDebugHostValue(final String host) throws CoreException {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        launchCopy.setAttribute(REMOTE_HOST_ATTRIBUTE, host);
    }

    @Override
    public void setRemoteDebugPortValue(final String port) throws CoreException {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        launchCopy.setAttribute(REMOTE_PORT_ATTRIBUTE, port);
    }

    @Override
    public void setRemoteDebugTimeoutValue(final String timeout) throws CoreException {
        final ILaunchConfigurationWorkingCopy launchCopy = asWorkingCopy();
        launchCopy.setAttribute(REMOTE_TIMEOUT_ATTRIBUTE, timeout);
    }

    private ILaunchConfigurationWorkingCopy asWorkingCopy() throws CoreException {
        return configuration instanceof ILaunchConfigurationWorkingCopy
                ? (ILaunchConfigurationWorkingCopy) configuration : configuration.getWorkingCopy();
    }

    private static CoreException newCoreException(final String message) {
        return new CoreException(new Status(IStatus.ERROR, RedPlugin.PLUGIN_ID, message));
    }
}
