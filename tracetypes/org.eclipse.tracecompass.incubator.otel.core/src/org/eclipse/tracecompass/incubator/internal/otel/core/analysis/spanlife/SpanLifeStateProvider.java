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
import java.util.function.BiConsumer;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.otel.core.trace.Constants;
import org.eclipse.tracecompass.statesystem.core.ITmfStateSystemBuilder;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.statesystem.AbstractTmfStateProvider;
import org.eclipse.tracecompass.tmf.core.statesystem.ITmfStateProvider;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.protobuf.ByteString;

import io.opentelemetry.proto.common.v1.KeyValue;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.Span;
import io.opentelemetry.proto.trace.v1.Span.Event;
import io.opentelemetry.proto.trace.v1.Status;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;

/**
 * Span life state provider
 *
 * @author Eya-Tom Augustin SANGAM
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

    // TODO : Replace the cache with one with an expiration policy so it does
    // not grow too big
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
        fHandlers.put(Constants.SpanEvents.START_SPAN_EVENT_TYPE_ID, this::handleStartSpanEvent);
        fHandlers.put(Constants.SpanEvents.END_SPAN_EVENT_TYPE_ID, this::handleEndSpanEvent);
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

    private void handleStartSpanEvent(ITmfEvent event, ITmfStateSystemBuilder ss) {
        // Get the process name and service name of the span creator
        Resource resource = getResource(event);
        String serviceName = getServiceName(resource);
        String processName = getProcessName(resource);

        // Get the timestamp. We use the event timestamp instead of the span
        // timestamp.
        // Because we will not change span data even after a trace
        // synchronization
        Span span = getSpan(event);
        long startTimestamp = event.getTimestamp().toNanos();

        // Check if the operation within the span was successful or not
        boolean errorTag = getErrorTag(span);

        // Get the trace id, span id and name
        String traceId = byteStringToHex(span.getTraceId());
        String spanId = byteStringToHex(span.getSpanId());
        String name = span.getName();

        // Create the trace and span identifier. We add lot of details to make
        // filtering
        // easier
        String traceIdentifier = getTraceIdentifier(traceId);
        @SuppressWarnings("null")
        String spanIdentifier = getSpanIdentifier(name, spanId, serviceName, processName, errorTag);

        // Update the State System
        int traceQuark = ss.getQuarkAbsoluteAndAdd(traceIdentifier);
        int openTracingSpansQuark = ss.getQuarkRelativeAndAdd(traceQuark, OTEL_SPANS_ATTRIBUTE);
        int spanQuark;

        ByteString parentSpanIdBs = span.getParentSpanId();
        if (parentSpanIdBs == null) {
            spanQuark = ss.getQuarkRelativeAndAdd(openTracingSpansQuark, spanIdentifier);
        } else {
            String parentSpanId = byteStringToHex(parentSpanIdBs);
            Integer parentQuark = fSpanMap.get(parentSpanId);
            if (parentQuark == null) {
                // We don't have the parent span, just start this span at root
                parentQuark = openTracingSpansQuark;
            }
            spanQuark = ss.getQuarkRelativeAndAdd(parentQuark, spanIdentifier);
        }

        ss.modifyAttribute(startTimestamp, name, spanQuark);

        fSpanMap.put(spanId, spanQuark);
    }

    private void handleEndSpanEvent(ITmfEvent event, ITmfStateSystemBuilder ss) {
        // Get the process name and service name of the span creator
        Resource resource = getResource(event);
        String serviceName = getServiceName(resource);
        String processName = getProcessName(resource);

        // Get the timestamp. We use the event timestamp instead of the span
        // timestamp.
        // Because we will not change span data even after a trace
        // synchronization
        Span span = getSpan(event);
        long endTimestamp = event.getTimestamp().toNanos();

        // Check if the operation within the span was successful or not
        boolean errorTag = getErrorTag(span);

        // Get the trace id, span id and name
        @SuppressWarnings("null")
        String traceId = byteStringToHex(span.getTraceId());
        @SuppressWarnings("null")
        String spanId = byteStringToHex(span.getSpanId());
        String name = span.getName();

        // Create the trace and span identifier. We add lot of details to make
        // filtering
        // easier
        String traceIdentifier = getTraceIdentifier(traceId);
        @SuppressWarnings("null")
        String spanIdentifier = getSpanIdentifier(name, spanId, serviceName, processName, errorTag);

        // Update the State System
        int traceQuark = ss.getQuarkAbsoluteAndAdd(traceIdentifier);
        Integer spanQuark = fSpanMap.get(spanId);
        if (spanQuark == null) {
            spanQuark = ss.getQuarkRelativeAndAdd(traceQuark, spanIdentifier);
        }

        if (span.getEventsCount() > 0) {
            // We put all the logs in the state system under the LOGS attribute
            Integer logsQuark = ss.getQuarkRelativeAndAdd(traceQuark, IOpenTracingConstants.LOGS);
            for (Event spanEvent : span.getEventsList()) {
                // One attribute for each span where each state value is the
                // logs at the timestamp corresponding to the start time of the
                // state
                Integer logQuark = ss.getQuarkRelativeAndAdd(logsQuark, spanId);
                // TODO: Adjust event timestamp
                Long logTimestamp = spanEvent.getTimeUnixNano();
                ss.modifyAttribute(logTimestamp, spanEvent, logQuark);
                ss.modifyAttribute(logTimestamp + 1, (Object) null, logQuark);
            }
        }

        ss.modifyAttribute(endTimestamp, (Object) null, spanQuark);

        if (resource.getAttributesCount() > 0) {
            // We put all the resources in the state system under the RESOURCES
            // attribute
            Integer resourcesQuark = ss.getQuarkRelativeAndAdd(traceQuark, IOpenTracingConstants.RESOURCES);
            Integer resourceQuark = ss.getQuarkRelativeAndAdd(resourcesQuark, spanId);
            long startTimestamp = ss.queryOngoingState(spanQuark).unboxLong();
            ss.modifyAttribute(startTimestamp, resource, resourceQuark);
            ss.modifyAttribute(endTimestamp, (Object) null, resourceQuark);
        }

    }

    private static Span getSpan(ITmfEvent event) {
        return (Span) event.getContent().getField(Constants.SpanEvents.Fields.SPAN).getValue();
    }

    private static String getServiceName(Resource resource) {
        return getResourceKey(resource, ResourceAttributes.SERVICE_NAME.getKey());
    }

    private static String getProcessName(Resource resource) {
        return getResourceKey(resource, ResourceAttributes.PROCESS_RUNTIME_NAME.getKey());
    }

    private static String getResourceKey(Resource resource, String resourcekey) {
        for (KeyValue attr : resource.getAttributesList()) {
            if (attr.getKey().equals(resourcekey)) {
                return attr.getValue().getStringValue();
            }
        }
        return ""; //$NON-NLS-1$
    }

    private static Resource getResource(ITmfEvent event) {
        return (Resource) event.getContent().getField(Constants.SpanEvents.Fields.RESOURCE).getValue();
    }

    private static boolean getErrorTag(Span span) {
        boolean errorTag = false;
        if (span.hasStatus()) {
            Status.StatusCode statusCode = span.getStatus().getCode();
            if (statusCode != null) {
                errorTag = errorTag || statusCode.equals(Status.StatusCode.STATUS_CODE_ERROR);
            }
        }
        return errorTag;
    }

    private static String byteStringToHex(ByteString byteString) {
        byte[] bytes = byteString.toByteArray();
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b)); //$NON-NLS-1$
        }
        return sb.toString();
    }

    private static String getSpanIdentifier(String name, String spanId, String serviceName, String processName, boolean hasError) {
        return String.format(
                "name=%s/span_id=%s/service_name=%s/process_name=%s/has_error=%s", //$NON-NLS-1$
                name,
                spanId,
                serviceName,
                processName,
                String.valueOf(hasError));
    }

    private static String getTraceIdentifier(String traceId) {
        return String.format("trace=%s", traceId); //$NON-NLS-1$
    }

}

class SpanComparator implements Comparator<Span> {

    @Override
    public int compare(Span x, Span y) {
        return Long.compare(x.getStartTimeUnixNano(), y.getStartTimeUnixNano());
    }
}
