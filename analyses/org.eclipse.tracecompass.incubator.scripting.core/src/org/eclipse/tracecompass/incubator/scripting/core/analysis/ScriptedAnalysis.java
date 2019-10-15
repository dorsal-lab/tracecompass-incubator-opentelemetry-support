/*******************************************************************************
 * Copyright (c) 2019 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.scripting.core.analysis;

import java.util.Iterator;

import org.eclipse.ease.modules.ScriptParameter;
import org.eclipse.ease.modules.WrapToScript;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.scripting.core.analysis.TmfScriptAnalysis;
import org.eclipse.tracecompass.incubator.internal.scripting.core.trace.ScriptEventRequest;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Analysis class that can be used with the scripts. It provides an event
 * iterator, as well as backends to store data. Scripts can thus parse events
 * and fill the backend appropriately.
 *
 * @author Geneviève Bastien
 */
public class ScriptedAnalysis {

    private final ITmfTrace fTrace;
    private final String fName;

    /**
     * Constructor
     *
     * package-private because it is only expected to be constructed by the
     * module.
     *
     * @param activeTrace
     *            The trace to associate with this analysis
     * @param name
     *            The name of the analysis
     */
    ScriptedAnalysis(ITmfTrace activeTrace, String name) {
        fTrace = activeTrace;
        fName = name;
    }

    /**
     * Get a state system to go with this analysis. If an analysis of the same
     * name already exists, it can be re-used instead of re-created.
     *
     * @param useExisting
     *            if <code>true</code>, any state system with the same name for
     *            the trace will be reused, otherwise, a new state system will
     *            be created
     * @return A state system builder
     */
    @WrapToScript
    public @Nullable ITmfStateSystemBuilder getStateSystem(@ScriptParameter(defaultValue = "false") boolean useExisting) {

        TmfScriptAnalysis analysisModule = TmfTraceUtils.getAnalysisModuleOfClass(fTrace, TmfScriptAnalysis.class, TmfScriptAnalysis.ID);
        if (analysisModule == null) {
            return null;
        }

        return (ITmfStateSystemBuilder) analysisModule.getStateSystem(fName, useExisting);

    }

    /**
     * Get an iterator to iterate chronologically through the events of the
     * trace.
     *
     * Thus, to iterate through a trace in a scripted analysis, one can just do
     * the following snippet (javascript)
     *
     * <pre>
     * var iter = analysis.getEventIterator();
     *
     * var event = null;
     * while (iter.hasNext()) {
     *
     *     event = iter.next();
     *
     *     // Do something with the event
     * }
     * </pre>
     *
     * @return The event iterator, starting from the first event
     */
    @WrapToScript
    public Iterator<ITmfEvent> getEventIterator() {
        ScriptEventRequest scriptEventRequest = new ScriptEventRequest();
        fTrace.sendRequest(scriptEventRequest);
        return scriptEventRequest.getEventIterator();
    }

    /**
     * Get the trace, not to be used by scripts.
     *
     * @return The trace
     */
    public ITmfTrace getTrace() {
        return fTrace;
    }

    /**
     * Get the name of this analysis, not to be used by scripts
     *
     * @return The name of the analysis
     */
    public String getName() {
        return fName;
    }
}