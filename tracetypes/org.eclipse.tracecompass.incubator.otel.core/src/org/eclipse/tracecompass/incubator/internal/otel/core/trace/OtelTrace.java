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

import java.util.Collection;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.lttng2.ust.core.trace.LttngUstTrace;
import org.eclipse.tracecompass.incubator.internal.otel.core.Activator;
import org.eclipse.tracecompass.incubator.internal.otel.core.aspect.OtelAspects;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import org.eclipse.tracecompass.tmf.core.event.aspect.TmfBaseAspects;
import org.eclipse.tracecompass.tmf.core.trace.TraceValidationStatus;
import org.eclipse.tracecompass.tmf.ctf.core.trace.CtfTraceValidationStatus;

import com.google.common.collect.ImmutableSet;

/**
 * Class which contains OpenTelemetry traces.
 *
 * @author Eya-Tom Augustin SANGAM
 */
public class OtelTrace extends LttngUstTrace {

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

    /**
     * Default constructor
     */
    public OtelTrace() {
        super(OtelEventFactory.instance());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation sets the confidence to 100 plus the number of
     * opentelemetry:* events
     */
    @Override
    public IStatus validate(final IProject project, final String path) {
        IStatus status = super.validate(project, path);
        if (!(status instanceof CtfTraceValidationStatus)) {
            return status;
        }
        Collection<String> eventNames = ((CtfTraceValidationStatus) status).getEventNames();
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
    public Iterable<ITmfEventAspect<?>> getEventAspects() {
        return fOtelTraceAspects;
    }

}
