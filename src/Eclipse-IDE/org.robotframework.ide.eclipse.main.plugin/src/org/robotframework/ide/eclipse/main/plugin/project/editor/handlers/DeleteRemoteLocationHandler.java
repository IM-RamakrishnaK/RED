/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.editor.handlers;

import java.util.List;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig.RemoteLocation;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfigEvents;
import org.robotframework.ide.eclipse.main.plugin.project.editor.RedProjectEditorInput;
import org.robotframework.ide.eclipse.main.plugin.project.editor.handlers.DeleteRemoteLocationHandler.E4DeleteRemoteLocationHandler;
import org.robotframework.red.viewers.Selections;


/**
 * @author Michal Anglart
 *
 */
public class DeleteRemoteLocationHandler extends DIHandler<E4DeleteRemoteLocationHandler> {

    public DeleteRemoteLocationHandler() {
        super(E4DeleteRemoteLocationHandler.class);
    }

    public static class E4DeleteRemoteLocationHandler {

        @Execute
        public Object deleteRemoteLocation(@Named(Selections.SELECTION) final IStructuredSelection selection,
                final RedProjectEditorInput input, final IEventBroker eventBroker) {
            final List<RemoteLocation> locations = Selections.getElements(selection, RemoteLocation.class);
            input.getProjectConfiguration().removeRemoteLocations(locations);

            eventBroker.send(RobotProjectConfigEvents.ROBOT_CONFIG_REMOTE_STRUCTURE_CHANGED,
                    input.getProjectConfiguration().getRemoteLocations());
            
            return null;
        }
    }
}
