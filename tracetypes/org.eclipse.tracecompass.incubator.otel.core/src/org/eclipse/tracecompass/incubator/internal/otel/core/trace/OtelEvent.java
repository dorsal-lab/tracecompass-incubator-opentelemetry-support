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

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEvent;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;

import com.google.protobuf.ByteString;

import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;

/**
 * Event type for use in OpenTelemetry traces.
 *
 * @author Eya-Tom Augustin SANGAM
 */
public class OtelEvent extends TmfEvent {

    private final OtelEventType fOtelEventType;

    /**
     * Default constructor. Only for use by extension points, should not be
     * called directly.
     */
    public OtelEvent() {
        super();
        fOtelEventType = null;
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
     *            the timestamp of the event
     * @param type
     *            the event type
     * @param content
     *            the event content (payload)
     * @param otelEventType
     *            the Otel event type
     */
    protected OtelEvent(
            OtelTrace trace,
            long rank,
            @NonNull ITmfTimestamp timestamp,
            ITmfEventType type,
            ITmfEventField content,
            OtelEventType otelEventType) {
        super(trace, rank, timestamp, type, content);
        fOtelEventType = otelEventType;
    }

    /**
     * @param ctfEventTimestamp
     *            The timestamp of the CTF event
     * @param resourceSpans
     *            The resource span in the CTF event
     * @param resourceMetrics
     *            The resource metrics in the CTF event
     * @return A pair containing start and end time. If resourceSpans and
     *         resourceMetrics mare null, the range will be [ctfEventTimestamp,
     *         ctfEventTimestamp]. Other wise it will be [lowest start timestamp
     *         of all span/metric, highest end timestamp of all last
     *         span/metric]
     */
    public static Pair<@NonNull ITmfTimestamp, @NonNull ITmfTimestamp> getStartAndEndTimeStamp(@NonNull ITmfTimestamp ctfEventTimestamp, ResourceSpans resourceSpans,
            ResourceMetrics resourceMetrics) {
        long start = ctfEventTimestamp.toNanos();
        long end = Long.MIN_VALUE;
        if (resourceSpans != null) {
            for (InstrumentationLibrarySpans ilSpans : resourceSpans.getInstrumentationLibrarySpansList()) {
                for (Span span : ilSpans.getSpansList()) {
                    start = Math.min(start, span.getStartTimeUnixNano());
                    end = Math.max(end, span.getEndTimeUnixNano());
                }
            }
        }
        if (resourceMetrics != null) {
            // TODO : Read metrics timestamp
        }
        if (end == Long.MIN_VALUE) {
            end = ctfEventTimestamp.toNanos();
        }
        return Pair.of(TmfTimestamp.fromNanos(start), TmfTimestamp.fromNanos(end));
    }

    /**
     * @return The OtelEventType
     */
    public OtelEventType getOtelEventType() {
        return fOtelEventType;
    }

    /**
     * @return The Otel trace id of the span
     */
    public ByteString getOtelTraceId() {
        ITmfEventField field = getContent().getField(Constants.SpanEvents.Fields.SPAN);
        if (field != null) {
            Span span = (Span) field.getValue();
            return span.getTraceId();
        }
        return null;
    }

    /**
     * @return The Otel span id of the span
     */
    public ByteString getOtelSpanId() {
        ITmfEventField field = getContent().getField(Constants.SpanEvents.Fields.SPAN);
        if (field != null) {
            Span span = (Span) field.getValue();
            return span.getSpanId();
        }
        return null;
    }

    /**
     * @return The Otel parent span id of the span
     */
    public ByteString getOtelParentSpanId() {
        ITmfEventField field = getContent().getField(Constants.SpanEvents.Fields.SPAN);
        if (field != null) {
            Span span = (Span) field.getValue();
            return span.getParentSpanId();
        }
        return null;
    }

}
