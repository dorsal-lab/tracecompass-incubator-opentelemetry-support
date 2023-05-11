/**********************************************************************

 * Copyright (c) 2023 Polytechnique de Montreal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

package org.eclipse.tracecompass.incubator.internal.otel.core.trace;

import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.otel.core.Activator;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;

/**
 * Experiment with otel traces
 *
 * @author Eya-Tom Augustin SANGAM
 */
public class OtelExperiment extends TmfExperiment {

    /**
     * Default Constructor
     */
    public OtelExperiment() {
        super();
    }

    /**
     * Constructor of an otel experiment
     *
     * @param type
     *            The event type
     * @param path
     *            The experiment path
     * @param traces
     *            The experiment set of traces
     * @param indexPageSize
     *            The experiment index page size. You can use
     *            {@link TmfExperiment#DEFAULT_INDEX_PAGE_SIZE} for a default
     *            value.
     * @param resource
     *            The resource associated to the experiment. You can use 'null'
     *            for no resources (tests, etc.)
     */
    public OtelExperiment(final Class<? extends ITmfEvent> type,
            final String path,
            final ITmfTrace[] traces,
            final int indexPageSize,
            final @Nullable IResource resource) {
        super(type, path, traces, indexPageSize, resource);
    }

    @Override
    public IStatus validateWithTraces(List<ITmfTrace> traces) {
        IStatus status = super.validateWithTraces(traces);
        if (!(status instanceof TraceValidationStatus)) {
            return status;
        }
        int confidence = ((TraceValidationStatus) status).getConfidence();
        confidence += traces.stream().filter(t -> t.getClass() == OtelTrace.class).count();
        return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
    }
}
