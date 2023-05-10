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
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;

import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.trace.v1.ResourceSpans;

/**
 * Event type for use in OpenTelemetry traces.
 *
 * @author Eya-Tom Augustin SANGAM
 */
public class OtelSortedEvent extends TmfEvent {

    private @Nullable ResourceSpans fResourceSpans;
    private @Nullable ResourceMetrics fResourceMetrics;

    /**
     * Default constructor. Only for use by extension points, should not be
     * called directly.
     */
    public OtelSortedEvent() {
        super();
    }

    /**
     * Full constructor
     *
     * @param trace
     *            the parent trace
     * @param rank
     *            the event rank (in the trace). You can use
     *            {@link ITmfContext#UNKNOWN_RANK} as default value
     * @param timestamp
     *            the event timestamp
     * @param type
     *            the event type
     * @param content
     *            the event content (payload)
     * @param resourceSpans
     *            the event {@link ResourceSpans}
     * @param resourceMetrics
     *            the event {@link ResourceMetrics}
     */
    protected OtelSortedEvent(
            OtelSortedTrace trace,
            long rank,
            ITmfTimestamp timestamp,
            ITmfEventType type,
            ITmfEventField content,
            ResourceSpans resourceSpans,
            ResourceMetrics resourceMetrics) {
        super(trace, rank, timestamp, type, content);
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
