/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.debug.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.robotframework.ide.eclipse.main.plugin.debug.RobotDebugEventDispatcher;
import org.robotframework.ide.eclipse.main.plugin.debug.RobotDebugEventDispatcher.ExecutionEvent;
import org.robotframework.ide.eclipse.main.plugin.debug.utils.DebugSocketManager;
import org.robotframework.ide.eclipse.main.plugin.debug.utils.KeywordContext;
import org.robotframework.ide.eclipse.main.plugin.debug.utils.RobotDebugStackFrameManager;
import org.robotframework.ide.eclipse.main.plugin.debug.utils.RobotDebugValueManager;
import org.robotframework.ide.eclipse.main.plugin.debug.utils.RobotDebugVariablesManager;
import org.robotframework.ide.eclipse.main.plugin.launch.RobotEventBroker;

/**
 * @author mmarzec
 */
@SuppressWarnings({ "PMD.TooManyFields", "PMD.TooManyMethods", "PMD.GodClass" })
public class RobotDebugTarget extends RobotDebugElement implements IDebugTarget {

    // associated system process (Robot)
    private final IProcess process;

    // containing launch object
    private final ILaunch launch;

    // program name
    private String name;
    
    private ServerSocket serverSocket;

    // socket to communicate with Agent
    private Socket eventSocket;

    private PrintWriter eventWriter;

    private BufferedReader eventReader;

    // suspend state
    private boolean isSuspended = false;

    // threads
    private final RobotThread thread;

    private final IThread[] threads;

    private final Map<String, KeywordContext> currentKeywordDebugContextMap;

    private int currentStepOverLevel = 0;
    
    private int currentStepReturnLevel = 0;

    private final RobotDebugVariablesManager robotVariablesManager;
    
    private final RobotDebugValueManager robotDebugValueManager;
    
    private final RobotDebugStackFrameManager robotDebugStackFrameManager;
    
    private IOConsoleOutputStream remoteDebugConsole;
    
    public RobotDebugTarget(final ILaunch launch, final IProcess process, final List<IResource> suiteResources,
            final RobotEventBroker robotEventBroker, final DebugSocketManager socketManager,
            final IOConsoleOutputStream remoteDebugConsole) throws CoreException {
        super(null);
        target = this;
        this.launch = launch;
        this.process = process;
        this.remoteDebugConsole = remoteDebugConsole;
        currentKeywordDebugContextMap = new LinkedHashMap<>();
        robotVariablesManager = new RobotDebugVariablesManager(this);
        robotDebugValueManager = new RobotDebugValueManager();
        
        int retryCounter = 0;
        try {
            while (socketManager.getEventSocket() == null ? true : (eventReader = new BufferedReader(
                    new InputStreamReader(socketManager.getEventSocket().getInputStream()))).readLine() == null) {
                try {
                    Thread.sleep(500);  //wait 30s for TestRunnerAgent
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
                retryCounter++;
                if(retryCounter > 60 || process.isTerminated()) {
                    if (remoteDebugConsole != null) {
                        remoteDebugConsole.write("A timeout was reached while waiting for a remote connection.\n");
                    }
                    process.terminate();
                    throw new CoreException(Status.CANCEL_STATUS);
                }
            }
            serverSocket = socketManager.getServerSocket();
        	eventSocket = socketManager.getEventSocket();
        	if (remoteDebugConsole != null && eventSocket != null) {
                remoteDebugConsole.write("Remote connection from " + eventSocket.getInetAddress().getHostAddress()
                        + ":" + eventSocket.getLocalPort() + " established.\n");
            }
        	eventWriter = new PrintWriter(eventSocket.getOutputStream(), true);
        } catch (final IOException e) {
            e.printStackTrace();
        }

        thread = new RobotThread(this);
        threads = new IThread[] { thread };
        
        robotDebugStackFrameManager = new RobotDebugStackFrameManager(thread);

        final RobotDebugEventDispatcher eventDispatcher = new RobotDebugEventDispatcher(this, suiteResources,
                robotEventBroker, remoteDebugConsole);
        eventDispatcher.schedule();

        DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
    }

    @Override
    public IProcess getProcess() {
        return process;
    }

    @Override
    public IThread[] getThreads() throws DebugException {
        return threads;
    }

    @Override
    public boolean hasThreads() throws DebugException {
        return true;
    }

    @Override
    public String getName() throws DebugException {
        if (name == null) {
            if (remoteDebugConsole == null) {
                name = "Robot Test";
            } else {
                name = "Robot Remote Test";
            }
            name += " at " + eventSocket.getInetAddress().getHostAddress() + ":" + eventSocket.getLocalPort();
        }
        return name;
    }

    @Override
    public IDebugTarget getDebugTarget() {
        return this;
    }

    @Override
    public ILaunch getLaunch() {
        return launch;
    }

    @Override
    public boolean canDisconnect() {
        return false;
    }

    @Override
    public boolean isDisconnected() {
        return false;
    }

    @Override
    public void disconnect() throws DebugException {

    }

    @Override
    public boolean canTerminate() {
        return getProcess().canTerminate();
    }

    @Override
    public boolean isTerminated() {
        return getProcess().isTerminated();
    }

    @Override
    public void terminate() throws DebugException {
        if (eventSocket != null) {
            sendExecutionEventToAgent(RobotDebugEventDispatcher.ExecutionEvent.INTERRUPT_EXECUTION);
        }
        terminated();
    }

    @Override
    public boolean canSuspend() {
        return !isTerminated() && !isSuspended();
    }

    @Override
    public boolean isSuspended() {
        return isSuspended;
    }

    @Override
    public void suspend() throws DebugException {

    }

    @Override
    public boolean canResume() {
        return !isTerminated() && isSuspended();
    }

    @Override
    public void resume() throws DebugException {
        thread.setStepping(false);
        sendExecutionEventToAgent(RobotDebugEventDispatcher.ExecutionEvent.RESUME_EXECUTION);
        resumed(DebugEvent.CLIENT_REQUEST);
    }

    protected void step() {
        thread.setStepping(true);
        sendExecutionEventToAgent(RobotDebugEventDispatcher.ExecutionEvent.RESUME_EXECUTION);
        resumed(DebugEvent.CLIENT_REQUEST);
    }

    protected void stepOver() {
        currentStepOverLevel = currentKeywordDebugContextMap.size();
        step();
    }
    
    protected void stepReturn() {
        currentStepReturnLevel = currentKeywordDebugContextMap.size();
        step();
    }

    /**
     * Notification the target has resumed for the given reason
     * 
     * @param detail
     *            reason for the resume
     */
    public void resumed(final int detail) {
        isSuspended = false;
        thread.fireResumeEvent(detail);
    }

    /**
     * Notification the target has suspended for the given reason
     * 
     * @param detail
     *            reason for the suspend
     */
    public void suspended(final int detail) {
        isSuspended = true;
        thread.fireSuspendEvent(detail);
    }

    /**
     * Notification we have connected to the Agent and it has started.
     */
    public void started() {
        fireCreationEvent();
    }

    /**
     * Called when this debug target terminates.
     */
    public void terminated() {
        isSuspended = false;
        DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
        fireTerminateEvent();
       
        try {
            if (eventWriter != null) {
                eventWriter.close();
            }
            if (eventReader != null) {
                eventReader.close();
            }
            if (eventSocket != null) {
                eventSocket.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
            if(remoteDebugConsole != null) {
                remoteDebugConsole.write("Remote connection closed.");
                remoteDebugConsole = null;
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }

        if(getProcess() != null) {
            try {
                getProcess().terminate();
            } catch (DebugException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void breakpointAdded(final IBreakpoint breakpoint) {

    }

    @Override
    public void breakpointRemoved(final IBreakpoint breakpoint, final IMarkerDelta delta) {
        if (supportsBreakpoint(breakpoint)) {

        }
    }

    @Override
    public void breakpointChanged(final IBreakpoint breakpoint, final IMarkerDelta delta) {
        if (supportsBreakpoint(breakpoint)) {
            try {
                if (breakpoint.isEnabled()) {
                    breakpointAdded(breakpoint);
                } else {
                    breakpointRemoved(breakpoint, null);
                }
            } catch (final CoreException e) {
            }
        }
    }

    @Override
    public boolean supportsBreakpoint(final IBreakpoint breakpoint) {
        if (breakpoint.getModelIdentifier().equals(RobotDebugElement.DEBUG_MODEL_ID)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean supportsStorageRetrieval() {
        return false;
    }

    @Override
    public IMemoryBlock getMemoryBlock(final long startAddress, final long length) throws DebugException {
        return null;
    }

    /**
     * Returns the current stack frames in the target.
     * 
     * @return the current stack frames in the target
     */
    protected IStackFrame[] getStackFrames() {

        return robotDebugStackFrameManager.getStackFrames(currentKeywordDebugContextMap);
    }

    public void setHasStackFramesCreated(final boolean hasStackFramesCreated) {
        robotDebugStackFrameManager.setHasStackFramesCreated(hasStackFramesCreated);
    }
    
    public void clearStackFrames() {
        robotDebugStackFrameManager.setStackFrames(null);
    }

    /**
     * Set the thread's breakpoint
     * 
     * @param breakpoint
     */
    public void breakpointHit(final IBreakpoint breakpoint) {

        if (breakpoint instanceof ILineBreakpoint) {
            thread.setBreakpoints(new IBreakpoint[] { breakpoint });
        }
    }

    /**
     * Sends a message to the TestRunnerAgent
     * 
     * @param event
     */
    public void sendEventToAgent(final String event) {

        synchronized (eventSocket) {
            eventWriter.print(event);
            eventWriter.flush();
        }
    }
    
    public void sendExecutionEventToAgent(final ExecutionEvent event) {
        sendEventToAgent(event.getMessage());
    }

    /**
     * Sends a message with change variable request to the TestRunnerAgent
     * 
     * @param variable
     * @param value
     */
    public void sendChangeVariableRequest(final String variable, final String value) {

        sendEventToAgent("{\"" + variable + "\":[\"" + value + "\"]}");
    }
    
    /**
     * Sends a message with change variable request to the TestRunnerAgent
     * 
     * @param variable
     * @param childList
     * @param value
     */
    public void sendChangeCollectionRequest(final String variable, final List<String> childList, final String value) {
        
        final StringBuilder requestJson = new StringBuilder();
        requestJson.append("{\"" + variable + "\":[");
        for (int i = 0; i < childList.size(); i++) {
            requestJson.append("\"" + childList.get(i) + "\",");
        }
        requestJson.append("\"" + value + "\"]}");
        sendEventToAgent(requestJson.toString());
    }
    
    public void sendChangeRequest(final String expression, final String variableName, final RobotDebugVariable parent) {
        
        if (parent != null) {
            final LinkedList<String> childNameList = new LinkedList<String>();
            final String root = robotVariablesManager.extractVariableRootAndChilds(parent, childNameList, variableName);
            sendChangeCollectionRequest(root, childNameList, expression);
        } else {
            sendChangeVariableRequest(variableName, expression);
        }
    }

    public boolean hasStepOver() {

        if (thread.isSteppingOver() && currentStepOverLevel <= currentKeywordDebugContextMap.size()) {
            return true;
        }

        return false;
    }
    
    public boolean hasStepReturn() {

        if (thread.isSteppingReturn() && currentStepReturnLevel <= currentKeywordDebugContextMap.size()+1) {
            return true;
        }

        return false;
    }

    public BufferedReader getEventReader() {
        return eventReader;
    }

    public RobotThread getRobotThread() {
        return thread;
    }

    public Map<String, KeywordContext> getCurrentKeywordsContextMap() {
        return currentKeywordDebugContextMap;
    }

    public KeywordContext getLastKeywordFromCurrentContextMap() {
        if(currentKeywordDebugContextMap.size() > 0) {
            return (KeywordContext) currentKeywordDebugContextMap.values().toArray()[currentKeywordDebugContextMap.size() - 1];
        }
        return new KeywordContext();
    }
    
    public RobotDebugVariablesManager getRobotVariablesManager() {
        return robotVariablesManager;
    }

    public RobotDebugValueManager getRobotDebugValueManager() {
        return robotDebugValueManager;
    }
}
