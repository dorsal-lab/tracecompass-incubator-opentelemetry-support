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

package org.eclipse.tracecompass.incubator.internal.otel.core.aspect;

import java.util.Collection;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.otel.core.trace.OtelEvent;
import org.eclipse.tracecompass.incubator.internal.otel.core.trace.OtelTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import com.google.common.collect.ImmutableList;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.trace.v1.ResourceSpans;

/**
 * Aspects that can be found in an {@link OtelTrace}.
 *
 * @author Eya-Tom Augustin SANGAM
 */
public final class OtelAspects {

    private static final @NonNull Collection<IOtelAspect<?>> ASPECTS = ImmutableList.of(
            getResourceSpansAspect(),
            getResourceMetricsAspect());

    /**
     * Get the list of all common base aspects
     *
     * @return the list of base aspects
     */
    public static @NonNull Collection<IOtelAspect<?>> getAspects() {
        return ASPECTS;
    }

    /**
     * Get the aspect for the event resource spans
     *
     * @return The resource spans aspect
     */
    private static @NonNull IOtelAspect<ResourceSpans> getResourceSpansAspect() {
        return new IOtelAspect<ResourceSpans>() {

            @Override
            public String getName() {
                return Messages.getMessage(Messages.AspectName_ResourceSpans);
            }

            @Override
            public String getHelpText() {
                return Messages.getMessage(Messages.AspectHelpText_ResourceSpans);
            }

            @Override
            public @Nullable ResourceSpans resolveOtelEvent(@NonNull OtelEvent event) {
                return event.getResourceSpans();
            }
        };
    }

    /**
     * Get the aspect for the event resource metrics
     *
     * @return The resource metrics aspect
     */
    private static @NonNull IOtelAspect<ResourceMetrics> getResourceMetricsAspect() {
        return new IOtelAspect<ResourceMetrics>() {

            @Override
            public String getName() {
                return Messages.getMessage(Messages.AspectName_ResourceMetrics);
            }

            @Override
            public String getHelpText() {
                return Messages.getMessage(Messages.AspectHelpText_ResourceMetrics);
            }

            @Override
            public @Nullable ResourceMetrics resolveOtelEvent(@NonNull OtelEvent event) {
                return event.getResourceMetrics();
            }
        };
    }

    private interface IOtelAspect<T> extends ITmfEventAspect<T> {

        @Override
        default @Nullable T resolve(@NonNull ITmfEvent event) {
            if (event instanceof OtelEvent) {
                return resolveOtelEvent((OtelEvent) event);
            }
            return null;
        }

        /**
         * {@link IOtelAspect#resolve(ITmfEvent)} equivalent for
         * {@link OtelEvent}
         *
         * @param event
         *            The event
         * @return Corresponding resource
         */
        public T resolveOtelEvent(@NonNull OtelEvent event);

    }

}
