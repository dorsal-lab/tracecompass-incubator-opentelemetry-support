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
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.BiConsumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.otel.core.analysis.spanlife.SpanLifeStateProvider;
import org.eclipse.tracecompass.incubator.internal.otel.core.trace.Constants;
import org.eclipse.tracecompass.incubator.internal.otel.core.trace.OtelSortedEvent;
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
    public static final String OPEN_TRACING_ATTRIBUTE = "openTracingSpans"; //$NON-NLS-1$

    /**
     * Quark name for ust spans
     */
    public static final String UST_ATTRIBUTE = "ustSpans"; //$NON-NLS-1$

    private final Map<String, Integer> fSpanMap;

    private final Map<String, PriorityQueue<Span>> fHangingSpans;

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
        fHangingSpans = new HashMap<>();
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
        // System.out.println(event);
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
        OtelSortedEvent otelEvent = (OtelSortedEvent) event;

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
                ByteString parentSpanIdBs = span.getParentSpanId();
                System.out.println(parentSpanIdBs);
                if (!parentSpanIdBs.isEmpty()) {
                    String parentSpanId = byteStringToHex(parentSpanIdBs);
                    Integer parentQuark = fSpanMap.get(parentSpanId);
                    if (parentQuark == null) {
                        // We don't have the parent span yet. We wait to have it
                        // at root
                        PriorityQueue<Span> childSpans = fHangingSpans.getOrDefault(
                                parentSpanId, new PriorityQueue<>(new SpanComparator()));
                        childSpans.add(span);
                        fHangingSpans.put(parentSpanId, childSpans);
                        continue;
                    }
                }
                handleSpan(ss, span, processName);
                handleSpanChilds(ss, span, processName);
            }
        }
    }

    private void handleSpan(ITmfStateSystemBuilder ss, Span span, String processName) {
        long timestamp = span.getStartTimeUnixNano();

        @SuppressWarnings("null")
        String traceId = byteStringToHex(span.getTraceId());
        int traceQuark = ss.getQuarkAbsoluteAndAdd(traceId);

        int openTracingSpansQuark = ss.getQuarkRelativeAndAdd(traceQuark, OPEN_TRACING_ATTRIBUTE);

        // Check if the operation within the span was successful or not
        boolean errorTag = false;
        if (span.hasStatus()) {
            Status status = span.getStatus();
            // TODO: Handle deprecation
            // Status.DeprecatedStatusCode deprecratedStatus = status.getDeprecatedCode();
            // if (deprecratedStatus != null) {
            //     errorTag = errorTag || !deprecratedStatus.equals(Status.DeprecatedStatusCode.DEPRECATED_STATUS_CODE_OK);
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
                // the logs at the
                // timestamp
                // corresponding to the start time of the state
                Integer logQuark = ss.getQuarkRelativeAndAdd(logsQuark, spanId);
                Long logTimestamp = spanEvent.getTimeUnixNano();
                ss.modifyAttribute(logTimestamp, spanEvent.toString(), logQuark); // $NON-NLS-1$
                ss.modifyAttribute(logTimestamp + 1, (Object) null, logQuark);
            }
        }

        long endTimestamp = span.getEndTimeUnixNano();
        ss.modifyAttribute(endTimestamp, (Object) null, spanQuark);
        System.out.println(endTimestamp - timestamp);
        fSpanMap.put(spanId, spanQuark);
    }

    private void handleSpanChilds(ITmfStateSystemBuilder ss, Span span, String processName) {
        Queue<Span> queue = new LinkedList<>();

        @SuppressWarnings("null")
        String spanId = byteStringToHex(span.getSpanId());
        PriorityQueue<Span> childSpans = fHangingSpans.remove(spanId);
        if (childSpans != null) {
            queue.addAll(childSpans);
        }

        while (!queue.isEmpty()) {
            Span currentSpan = queue.remove();
            handleSpan(ss, currentSpan, processName);
            @SuppressWarnings("null")
            String currentSpanId = byteStringToHex(currentSpan.getSpanId());
            PriorityQueue<Span> currentSpanChilds = fHangingSpans.remove(currentSpanId);
            if (currentSpanChilds != null) {
                queue.addAll(currentSpanChilds);
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

    // private void handleStart(ITmfEvent event, ITmfStateSystemBuilder ss) {
    // String traceId = event.getContent().getFieldValue(String.class,
    // "trace_id_low"); //$NON-NLS-1$
    // traceId = Long.toHexString(Long.decode(traceId));
    // int traceQuark = ss.getQuarkAbsoluteAndAdd(traceId);
    //
    // int ustSpansQuark = ss.getQuarkRelativeAndAdd(traceQuark, UST_ATTRIBUTE);
    //
    // String spanId = event.getContent().getFieldValue(String.class,
    // "span_id"); //$NON-NLS-1$
    // spanId = Long.toHexString(Long.decode(spanId));
    // int spanQuark = ss.getQuarkRelativeAndAdd(ustSpansQuark, spanId);
    //
    // long timestamp = event.getTimestamp().toNanos();
    // String name = event.getContent().getFieldValue(String.class, "op_name");
    // //$NON-NLS-1$
    // ss.modifyAttribute(timestamp, name, spanQuark);
    // }
    //
    // private void handleEnd(ITmfEvent event, ITmfStateSystemBuilder ss) {
    // String traceId = event.getContent().getFieldValue(String.class,
    // "trace_id_low"); //$NON-NLS-1$
    // traceId = Long.toHexString(Long.decode(traceId));
    // int traceQuark = ss.getQuarkAbsoluteAndAdd(traceId);
    //
    // int ustSpansQuark = ss.getQuarkRelativeAndAdd(traceQuark, UST_ATTRIBUTE);
    //
    // String spanId = event.getContent().getFieldValue(String.class,
    // "span_id"); //$NON-NLS-1$
    // spanId = Long.toHexString(Long.decode(spanId));
    // int spanQuark = ss.getQuarkRelativeAndAdd(ustSpansQuark, spanId);
    //
    // long timestamp = event.getTimestamp().toNanos();
    // ss.modifyAttribute(timestamp, (Object) null, spanQuark);
    // }
}

class SpanComparator implements Comparator<Span> {

    @Override
    public int compare(Span x, Span y) {
        return Long.compare(x.getStartTimeUnixNano(), y.getStartTimeUnixNano());
    }
}
