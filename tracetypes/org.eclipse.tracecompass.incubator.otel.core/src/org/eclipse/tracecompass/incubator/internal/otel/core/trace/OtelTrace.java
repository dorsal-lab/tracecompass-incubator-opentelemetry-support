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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.otel.core.Activator;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstEvent;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventField;
import org.eclipse.tracecompass.tmf.core.event.ITmfEventType;
import org.eclipse.tracecompass.tmf.core.event.TmfEventField;
import org.eclipse.tracecompass.tmf.core.event.TmfEventType;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfTraceException;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfContext;
import org.eclipse.tracecompass.tmf.core.trace.TmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.core.trace.location.ITmfLocation;
import org.eclipse.tracecompass.tmf.core.trace.location.TmfLongLocation;
import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventField;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTmfTrace;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.protobuf.InvalidProtocolBufferException;

import io.opentelemetry.proto.common.v1.InstrumentationLibrary;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.resource.v1.Resource;
import io.opentelemetry.proto.trace.v1.InstrumentationLibrarySpans;
import io.opentelemetry.proto.trace.v1.ResourceSpans;
import io.opentelemetry.proto.trace.v1.Span;

/**
 * Class which contains OpenTelemetry traces.
 *
 * @author Eya-Tom Augustin SANGAM
 */
public class OtelTrace extends TmfTrace {

    private static final int BASE_CONFIDENCE = 100;

    private static final @NonNull Collection<ITmfEventAspect<?>> OTEL_ASPECTS;

    static {
        ImmutableSet.Builder<ITmfEventAspect<?>> builder = ImmutableSet.builder();
        builder.add(TmfBaseAspects.getTimestampAspect());
        builder.add(TmfBaseAspects.getEventTypeAspect());
        builder.add(TmfBaseAspects.getContentsAspect());
        OTEL_ASPECTS = builder.build();
    }

    private @NonNull Collection<ITmfEventAspect<?>> fOtelTraceAspects = ImmutableSet.copyOf(OTEL_ASPECTS);

    private List<OtelEvent> fEvents = new ArrayList<>();
    private long fNbEvents = 0;

    TmfLongLocation fCurrent;

    /**
     * {@inheritDoc}
     * <p>
     * This implementation sets the confidence to 100 plus the number of
     * opentelemetry:* events
     */
    @Override
    public IStatus validate(final IProject project, final String path) {
        IStatus ctfStatus = new CtfTmfTrace().validate(project, path);
        if (!(ctfStatus instanceof CtfTraceValidationStatus)) {
            return ctfStatus;
        }
        Collection<String> eventNames = ((CtfTraceValidationStatus) ctfStatus).getEventNames();
        long nOtelEvents = eventNames.stream().filter(event -> event.startsWith(Constants.PROVIDER_NAME)).count();
        if (nOtelEvents == 0) {
            return new Status(IStatus.ERROR, Activator.PLUGIN_ID, "The trace is not an OpenTelemetry trace"); //$NON-NLS-1$
        }
        // Return Integer.MAX_VALUE when the number of Otel traces is greater
        // than it
        int confidence = (int) Math.min(BASE_CONFIDENCE + nOtelEvents, Integer.MAX_VALUE);
        return new TraceValidationStatus(confidence, Activator.PLUGIN_ID);
    }

    @Override
    public void initTrace(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        super.initTrace(resource, path, type);
        List<Timestamped<? extends RawEvent>> events = getUnsortedEvents(resource, path, type);
        fEvents = sortEvents(events, this);
        fNbEvents = fEvents.size();
    }

    @Override
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        return fOtelTraceAspects;
    }

    @Override
    public ITmfLocation getCurrentLocation() {
        return fCurrent;
    }

    @Override
    public double getLocationRatio(ITmfLocation location) {
        return ((TmfLongLocation) location).getLocationInfo().doubleValue()
                / fNbEvents;
    }

    @Override
    public ITmfContext seekEvent(ITmfLocation location) {
        TmfLongLocation nl = (TmfLongLocation) location;
        if (location == null) {
            nl = new TmfLongLocation(0L);
        }
        fCurrent = nl;
        return new TmfContext(nl, nl.getLocationInfo());
    }

    @Override
    public ITmfContext seekEvent(double ratio) {
        long rank = (long) (ratio * fNbEvents);
        fCurrent = new TmfLongLocation(rank);
        return new TmfContext(fCurrent, rank);
    }

    @Override
    public ITmfEvent parseEvent(ITmfContext context) {
        long rank = context.getRank();
        if (rank >= fNbEvents) {
            return null;
        }
        return fEvents.get((int) context.getRank());
    }

    @Override
    public synchronized long getNbEvents() {
        return fNbEvents;
    }

    private List<Timestamped<? extends RawEvent>> getUnsortedEvents(IResource resource, String path, Class<? extends ITmfEvent> type) throws TmfTraceException {
        List<Timestamped<? extends RawEvent>> events = new ArrayList<>();

        fCurrent = new TmfLongLocation(0L);
        LttngUstTrace lttngUstTrace = new LttngUstTrace();

        try {
            lttngUstTrace.initTrace(resource, path, type);
            ITmfContext context = lttngUstTrace.seekEvent(0.0);

            while (true) {
                LttngUstEvent lttngEvent = (LttngUstEvent) lttngUstTrace.getNext(context);
                if (lttngEvent == null) {
                    break;
                }
                ResourceSpans resourceSpans = getResourceSpansFromEvent(lttngEvent);
                ResourceMetrics resourceMetrics = getResourceMetricsFromEvent(lttngEvent);
                if (resourceSpans == null && resourceMetrics == null) {
                    continue;
                }
                if (resourceSpans != null) {
                    Resource traceResource = resourceSpans.getResource();
                    String resourceSchemaUrl = resourceSpans.getSchemaUrl();
                    for (InstrumentationLibrarySpans ilSpans : resourceSpans.getInstrumentationLibrarySpansList()) {
                        InstrumentationLibrary instrumentationLibrary = ilSpans.getInstrumentationLibrary();
                        String instrumentationLibrarySchemaUrl = ilSpans.getSchemaUrl();
                        for (Span span : ilSpans.getSpansList()) {
                            SpanStartEvent spanStartData = new SpanStartEvent(traceResource, resourceSchemaUrl, instrumentationLibrary, instrumentationLibrarySchemaUrl, span);
                            events.add(new Timestamped<>(span.getStartTimeUnixNano(), spanStartData));
                            SpanEndEvent spanEndData = new SpanEndEvent(traceResource, resourceSchemaUrl, instrumentationLibrary, instrumentationLibrarySchemaUrl, span);
                            events.add(new Timestamped<>(span.getEndTimeUnixNano(), spanEndData));
                        }
                    }
                }
                if (resourceMetrics != null) {
                    // TODO : Handle metrics events
                }
            }

        } finally {
            lttngUstTrace.dispose();
        }

        return events;
    }

    @SuppressWarnings("null")
    private static List<OtelEvent> sortEvents(List<Timestamped<? extends RawEvent>> events, @NonNull OtelTrace trace) {
        Collections.sort(events, Comparator.comparing(Timestamped<? extends RawEvent>::getTimestamp));
        return Streams.mapWithIndex(events.stream(), (event, rank) -> event.getData().createEvent(trace, rank)).collect(Collectors.toList());
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

interface RawEvent {
    OtelEvent createEvent(@NonNull OtelTrace trace, long rank);
}

class Timestamped<T> {
    private long fTimestamp;
    private T fData;

    public Timestamped(long timestamp, T data) {
        fTimestamp = timestamp;
        fData = data;
    }

    long getTimestamp() {
        return fTimestamp;
    }

    T getData() {
        return fData;
    }
}

abstract class SpanEvent implements RawEvent {
    private Resource fResource;
    private String fResourceSchemaUrl;
    private InstrumentationLibrary fInstrumentationLibrary;
    private String fInstrumentationLibrarySchemaUrl;
    private Span fSpan;

    public SpanEvent(
            Resource resource,
            String resourceSchemaUrl,
            InstrumentationLibrary instrumentationLibrary,
            String instrumentationLibrarySchemaUrl,
            Span span) {
        super();
        fResource = resource;
        fResourceSchemaUrl = resourceSchemaUrl;
        fInstrumentationLibrary = instrumentationLibrary;
        fInstrumentationLibrarySchemaUrl = instrumentationLibrarySchemaUrl;
        fSpan = span;
    }

    public Resource getResource() {
        return fResource;
    }

    public String getResourceSchemaUrl() {
        return fResourceSchemaUrl;
    }

    public InstrumentationLibrary getInstrumentationLibrary() {
        return fInstrumentationLibrary;
    }

    public String getInstrumentationLibrarySchemaUrl() {
        return fInstrumentationLibrarySchemaUrl;
    }

    public Span getSpan() {
        return fSpan;
    }

    public OtelEvent createEvent(OtelTrace trace, long rank, @NonNull ITmfTimestamp timestamp, @NonNull String eventId) {
        ITmfEventField eventContent = new TmfEventField(
                ITmfEventField.ROOT_FIELD_ID,
                null,
                new TmfEventField[] {
                        new TmfEventField(Constants.SpanEvents.Fields.RESOURCE, fResource, null),
                        new TmfEventField(Constants.SpanEvents.Fields.RESOURCE_SCHEMA_URL, fResourceSchemaUrl, null),
                        new TmfEventField(Constants.SpanEvents.Fields.INSTRUMENTATION_LIBRARY, fInstrumentationLibrary, null),
                        new TmfEventField(Constants.SpanEvents.Fields.INSTRUMENTATION_LIBRARY_SCHEMA_URL, fInstrumentationLibrarySchemaUrl, null),
                        new TmfEventField(Constants.SpanEvents.Fields.SPAN, fSpan, null),
                });
        ITmfEventType eventType = new TmfEventType(eventId, eventContent);
        return new OtelEvent(trace, rank, timestamp, eventType, eventContent);
    }

}

class SpanStartEvent extends SpanEvent {

    public SpanStartEvent(Resource resource, String resourceSchemaUrl, InstrumentationLibrary instrumentationLibrary, String instrumentationLibrarySchemaUrl, Span span) {
        super(resource, resourceSchemaUrl, instrumentationLibrary, instrumentationLibrarySchemaUrl, span);
    }

    @Override
    public OtelEvent createEvent(@NonNull OtelTrace trace, long rank) {
        return super.createEvent(trace, rank, TmfTimestamp.fromNanos(getSpan().getStartTimeUnixNano()), Constants.SpanEvents.START_SPAN_EVENT_TYPE_ID);
    }

}

class SpanEndEvent extends SpanEvent {

    public SpanEndEvent(Resource resource, String resourceSchemaUrl, InstrumentationLibrary instrumentationLibrary, String instrumentationLibrarySchemaUrl, Span span) {
        super(resource, resourceSchemaUrl, instrumentationLibrary, instrumentationLibrarySchemaUrl, span);
    }

    @Override
    public OtelEvent createEvent(@NonNull OtelTrace trace, long rank) {
        return super.createEvent(trace, rank, TmfTimestamp.fromNanos(getSpan().getEndTimeUnixNano()), Constants.SpanEvents.END_SPAN_EVENT_TYPE_ID);
    }

}
