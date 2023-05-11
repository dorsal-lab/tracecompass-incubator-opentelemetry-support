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
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.incubator.internal.otel.core.Activator;
import org.eclipse.tracecompass.incubator.internal.otel.core.aspect.OtelAspects;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstEvent;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.ITmfLostEvent;
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
import com.google.protobuf.InvalidProtocolBufferException;

import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.trace.v1.ResourceSpans;

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
        builder.addAll(OtelAspects.getAspects());
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

        fCurrent = new TmfLongLocation(0L);
        LttngUstTrace lttngUstTrace = new LttngUstTrace();

        try {
            lttngUstTrace.initTrace(resource, path, type);
            ITmfContext context = lttngUstTrace.seekEvent(0.0);

            for (long rank = 0;; rank++) {
                LttngUstEvent lttngEvent = (LttngUstEvent) lttngUstTrace.getNext(context);
                if (lttngEvent == null) {
                    break;
                }
                ResourceSpans resourceSpans = getResourceSpansFromEvent(lttngEvent);
                ResourceMetrics resourceMetrics = getResourceMetricsFromEvent(lttngEvent);
                Pair<@NonNull ITmfTimestamp, @NonNull ITmfTimestamp> tsRange = OtelEvent.getStartAndEndTimeStamp(lttngEvent.getTimestamp(), resourceSpans, resourceMetrics);
                OtelEvent event = new OtelEvent(
                        this,
                        rank,
                        tsRange.getLeft(),
                        tsRange.getRight(),
                        lttngEvent.getType(),
                        lttngEvent.getContent(),
                        resourceSpans,
                        resourceMetrics);
                fEvents.add(event);
            }

            fEvents.sort((evt1, evt2) -> evt1.getTimestamp().compareTo(evt2.getTimestamp()));
            fNbEvents = fEvents.size();

        } finally {
            lttngUstTrace.dispose();
        }

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

    @Override
    protected synchronized void updateAttributes(final ITmfContext context, final @NonNull ITmfEvent event) {
        ITmfTimestamp timestamp = event.getTimestamp();
        ITmfTimestamp endTime = ((OtelEvent) event).getEndTimestamp();
        if (event instanceof ITmfLostEvent) {
            endTime = ((ITmfLostEvent) event).getTimeRange().getEndTime();
        }
        if (getStartTime().equals(TmfTimestamp.BIG_BANG) || (getStartTime().compareTo(timestamp) > 0)) {
            setStartTime(timestamp);
        }
        if (getEndTime().equals(TmfTimestamp.BIG_CRUNCH) || (getEndTime().compareTo(endTime) < 0)) {
            setEndTime(endTime);
        }
        if (context.hasValidRank()) {
            long rank = context.getRank();
            if (fNbEvents <= rank) {
                fNbEvents = rank + 1;
            }
            if (getIndexer() != null) {
                getIndexer().updateIndex(context, timestamp);
            }
        }
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
