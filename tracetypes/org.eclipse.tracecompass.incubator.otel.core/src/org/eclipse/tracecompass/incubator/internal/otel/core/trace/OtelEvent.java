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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.event.IEventDeclaration;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;

import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.trace.v1.ResourceSpans;

/**
 * Event type for use in OpenTelemetry traces.
 *
 * @author Eya-Tom Augustin SANGAM
 */
public class OtelEvent extends LttngUstEvent {

    private @Nullable ResourceSpans fResourceSpans;
    private @Nullable ResourceMetrics fResourceMetrics;

    /**
     * Default constructor. Only for use by extension points, should not be
     * called directly.
     */
    public OtelEvent() {
        super();
    }

    /**
     * Constructor
     *
     * @param trace
     *            The trace to which this event belongs
     * @param rank
     *            The rank of the event
     * @param timestamp
     *            The timestamp
     * @param channel
     *            The CTF channel of this event
     * @param cpu
     *            The event's CPU
     * @param declaration
     *            The event declaration
     * @param eventDefinition
     *            The event definition
     * @param resourceSpans
     *            The event {@link ResourceSpans}
     * @param resourceMetrics
     *            The event {@link ResourceMetrics}
     */
    protected OtelEvent(CtfTmfTrace trace, long rank, ITmfTimestamp timestamp,
            String channel, int cpu, IEventDeclaration declaration, IEventDefinition eventDefinition,
            ResourceSpans resourceSpans, ResourceMetrics resourceMetrics) {
        super(trace, rank, timestamp, channel, cpu, declaration, eventDefinition);
        fResourceSpans = resourceSpans;
        fResourceMetrics = resourceMetrics;
    }

    /**
     * Get the {@link ResourceSpans} associated to the event
     *
     * @return The {@link ResourceSpans} associated to the event
     */
    public ResourceSpans getResourceSpans() {
        return fResourceSpans;
    }

    /**
     * Get the {@link ResourceMetrics} associated to the event
     *
     * @return The {@link ResourceMetrics} associated to the event
     */
    public ResourceMetrics getResourceMetrics() {
        return fResourceMetrics;
    }

}
