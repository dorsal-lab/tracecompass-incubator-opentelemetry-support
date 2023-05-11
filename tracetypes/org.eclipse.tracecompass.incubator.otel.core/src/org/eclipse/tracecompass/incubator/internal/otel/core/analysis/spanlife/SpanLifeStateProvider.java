/*******************************************************************************
 * Copyright (c) 2018 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.otel.core.analysis.spanlife;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.otel.core.trace.Constants;
import org.eclipse.tracecompass.incubator.internal.otel.core.trace.OtelEvent;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.protobuf.ByteString;

import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Span.Event;
import io.opentelemetry.proto.trace.v1.Status;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

/**
 * Span life state provider
 *
 * @author Katherine Nadeau
 *
 */
public class SpanLifeStateProvider extends AbstractTmfStateProvider {

    /**
     * Quark name for open tracing spans
     */
    public static final String OTEL_SPANS_ATTRIBUTE = "openTracingSpans"; //$NON-NLS-1$

    /**
     * Quark name for ust spans
     */
    public static final String UST_ATTRIBUTE = "ustSpans"; //$NON-NLS-1$

    private final Map<String, Integer> fSpanMap;

    private final Map<String, BiConsumer<ITmfEvent, ITmfStateSystemBuilder>> fHandlers;

    /**
     * Constructor
     *
     * @param trace
     *            the trace to follow
     */
    public SpanLifeStateProvider(ITmfTrace trace) {
        super(trace, SpanLifeAnalysis.ID);
        fSpanMap = new HashMap<>();
        fHandlers = new HashMap<>();
        fHandlers.put(Constants.RESOURCE_SPANS_FIELD_FULL_NAME, this::handleEventSpan);
    }

    @Override
    public int getVersion() {
        return 3;
    }

    @Override
    public @NonNull ITmfStateProvider getNewInstance() {
        return new SpanLifeStateProvider(getTrace());
    }

    @Override
    protected void eventHandle(ITmfEvent event) {
        ITmfStateSystemBuilder ss = getStateSystemBuilder();
        if (ss == null) {
            return;
        }
        BiConsumer<ITmfEvent, ITmfStateSystemBuilder> handler = fHandlers.get(event.getType().getName());
        if (handler != null) {
            handler.accept(event, ss);
        }
    }

    private void handleEventSpan(ITmfEvent event, ITmfStateSystemBuilder ss) {
        OtelEvent otelEvent = (OtelEvent) event;

        ResourceSpans resourceSpans = otelEvent.getResourceSpans();
        if (resourceSpans == null) {
            return;
        }

        // Get the process name of the span creator
        Resource resource = resourceSpans.getResource();
        String processName = ""; //$NON-NLS-1$
        @SuppressWarnings("null")
        Optional<KeyValue> processNameSearch = resource.getAttributesList().stream()
                .filter(attr -> attr.getKey().equals(ResourceAttributes.PROCESS_RUNTIME_NAME.getKey()))
                .findFirst();
        if (processNameSearch.isPresent()) {
            processName = processNameSearch.get().getValue().getStringValue();
        }

        for (InstrumentationLibrarySpans ilSpans : resourceSpans.getInstrumentationLibrarySpansList()) {
            // InstrumentationLibrary instrumentationLibrary =
            // spans.getInstrumentationLibrary();
            for (Span span : ilSpans.getSpansList()) {
                long timestamp = span.getStartTimeUnixNano();

                @SuppressWarnings("null")
                String traceId = byteStringToHex(span.getTraceId());
                int traceQuark = ss.getQuarkAbsoluteAndAdd(traceId);

                int openTracingSpansQuark = ss.getQuarkRelativeAndAdd(traceQuark, OTEL_SPANS_ATTRIBUTE);

                // Check if the operation within the span was successful or not
                boolean errorTag = false;
                if (span.hasStatus()) {
                    Status status = span.getStatus();
                    // TODO: Handle deprecation
                    // Status.DeprecatedStatusCode deprecratedStatus =
                    // status.getDeprecatedCode();
                    // if (deprecratedStatus != null) {
                    // errorTag = errorTag ||
                    // !deprecratedStatus.equals(Status.DeprecatedStatusCode.DEPRECATED_STATUS_CODE_OK);
                    // }
                    Status.StatusCode statusCode = status.getCode();
                    if (statusCode != null) {
                        errorTag = errorTag || statusCode.equals(Status.StatusCode.STATUS_CODE_ERROR);
                    }
                }

                int spanQuark;
                String name = span.getName();
                @SuppressWarnings("null")
                String spanId = byteStringToHex(span.getSpanId());

                ByteString parentSpanIdBs = span.getParentSpanId();
                if (parentSpanIdBs == null) {
                    spanQuark = ss.getQuarkRelativeAndAdd(openTracingSpansQuark, name + '/' + spanId + '/' + errorTag + '/' + processName);
                } else {
                    String parentSpanId = byteStringToHex(parentSpanIdBs);
                    Integer parentQuark = fSpanMap.get(parentSpanId);
                    if (parentQuark == null) {
                        // We don't have the parent span, just start this span
                        // at root
                        parentQuark = openTracingSpansQuark;
                    }
                    spanQuark = ss.getQuarkRelativeAndAdd(parentQuark, name + '/' + spanId + '/' + errorTag + '/' + processName);
                }

                ss.modifyAttribute(timestamp, name, spanQuark);

                // Map<Long, Map<String, String>> logs =
                // eevent.getContent().getFieldValue(Map.class,
                // IOpenTracingConstants.LOGS);
                if (span.getEventsCount() > 0) {
                    // We put all the logs in the state system under the LOGS
                    // attribute
                    Integer logsQuark = ss.getQuarkRelativeAndAdd(traceQuark, IOpenTracingConstants.LOGS);
                    for (Event spanEvent : span.getEventsList()) {
                        // One attribute for each span where each state value is
                        // the
                        // logs at the timestamp corresponding to the start time
                        // of the
                        // state
                        Integer logQuark = ss.getQuarkRelativeAndAdd(logsQuark, spanId);
                        Long logTimestamp = spanEvent.getTimeUnixNano();
                        ss.modifyAttribute(logTimestamp, spanEvent.toString(), logQuark); // $NON-NLS-1$
                        ss.modifyAttribute(logTimestamp + 1, (Object) null, logQuark);
                    }
                }

                long endTimestamp = span.getEndTimeUnixNano();
                ss.modifyAttribute(endTimestamp, (Object) null, spanQuark);
                fSpanMap.put(spanId, spanQuark);
            }
        }
    }

    private static String byteStringToHex(ByteString byteString) {
        byte[] bytes = byteString.toByteArray();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b)); //$NON-NLS-1$
        }
        return sb.toString();
    }

}

class SpanComparator implements Comparator<Span> {

    @Override
    public int compare(Span x, Span y) {
        return Long.compare(x.getStartTimeUnixNano(), y.getStartTimeUnixNano());
    }
}
