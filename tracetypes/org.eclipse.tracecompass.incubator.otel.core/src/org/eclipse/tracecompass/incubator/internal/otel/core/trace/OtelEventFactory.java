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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.ctf.core.event.IEventDefinition;
import org.eclipse.tracecompass.internal.ctf.core.event.EventDefinition;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstEvent;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstEventFactory;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEvent;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventFactory;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventField;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;

import com.google.protobuf.InvalidProtocolBufferException;

import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;

/**
 * Factory for {@link OtelEvent}.
 *
 * @author Eya-Tom Augustin SANGAM
 */
public class OtelEventFactory extends CtfTmfEventFactory {

    private static final @NonNull OtelEventFactory INSTANCE = new OtelEventFactory();

    /**
     * Private constructor.
     */
    private OtelEventFactory() {
        super();
    }

    public static @NonNull OtelEventFactory instance() {
        return INSTANCE;
    }

    @Override
    public CtfTmfEvent createEvent(CtfTmfTrace trace, IEventDefinition eventDef, @Nullable String fileName) {
        CtfTmfEvent event = LttngUstEventFactory.instance().createEvent(trace, eventDef, fileName);
        if (!(event instanceof LttngUstEvent)) {
            return event;
        }
        LttngUstEvent ustEvent = (LttngUstEvent) event;
        ITmfTimestamp timestamp = ustEvent.getTimestamp();
        boolean changeEventDef = false;
        ResourceSpans resourceSpans = getResourceSpansFromEvent(ustEvent);
        if (resourceSpans != null) {
            for (InstrumentationLibrarySpans ilSpans : resourceSpans.getInstrumentationLibrarySpansList()) {
                for (Span span : ilSpans.getSpansList()) {
                    if (span.getStartTimeUnixNano() < timestamp.toNanos()) {
                        timestamp = TmfTimestamp.fromNanos(span.getStartTimeUnixNano());
                        changeEventDef = true;
                    }
                }
            }
        }
        ResourceMetrics resourceMetrics = getResourceMetricsFromEvent(ustEvent);
        IEventDefinition newEventDefinition = eventDef;
        if (changeEventDef) {
            newEventDefinition = new EventDefinition(eventDef.getDeclaration(), eventDef.getCPU(),
                    timestamp.toNanos(), eventDef.getEventHeader(), eventDef.getStreamContext(),
                    eventDef.getEventContext(), eventDef.getPacketHeader(), eventDef.getFields(), null);

        }
        return new OtelEvent(ustEvent.getTrace(), ustEvent.getRank(), timestamp,
                ustEvent.getChannel(), ustEvent.getCPU(), newEventDefinition.getDeclaration(),
                newEventDefinition, resourceSpans, resourceMetrics);
    }

    private static ResourceSpans getResourceSpansFromEvent(LttngUstEvent event) {
        byte[] bytes = getByteArray(event, Constants.RESOURCE_SPANS);
        if (bytes == null) {
            return null;
        }
        try {
            return ResourceSpans.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    private static ResourceMetrics getResourceMetricsFromEvent(LttngUstEvent event) {
        byte[] bytes = getByteArray(event, Constants.RESOURCE_METRICS);
        if (bytes == null) {
            return null;
        }
        try {
            return ResourceMetrics.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            return null;
        }
    }

    private static byte[] getByteArray(LttngUstEvent event, String field) {
        CtfTmfEventField ctfField = (CtfTmfEventField) event.getContent().getField(field);
        if (ctfField == null) {
            return null;
        }

        long[] longArr;
        try {
            longArr = (long[]) ctfField.getValue();
        } catch (ClassCastException e) {
            return null;
        }

        byte[] bytes = new byte[longArr.length];
        for (int i = 0; i < longArr.length; i++) {
            bytes[i] = (byte) longArr[i];
        }
        return bytes;
    }
}