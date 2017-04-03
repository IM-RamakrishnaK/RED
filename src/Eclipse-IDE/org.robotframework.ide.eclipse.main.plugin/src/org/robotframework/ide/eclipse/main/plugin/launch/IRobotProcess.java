/*
 * Copyright 2017 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.launch;

import org.eclipse.debug.core.model.IProcess;

public interface IRobotProcess extends IProcess {

    RobotConsoleFacade provideConsoleFacade(String processLabel);

    void onTerminate(Runnable operation);

}
