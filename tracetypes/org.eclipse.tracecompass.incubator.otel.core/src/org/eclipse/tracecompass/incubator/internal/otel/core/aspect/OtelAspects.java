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
import org.eclipse.tracecompass.incubator.internal.otel.core.trace.OtelTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.aspect.ITmfEventAspect;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;

import org.eclipse.tracecompass.tmf.ctf.core.event.CtfTmfEventField;
import io.opentelemetry.proto.metrics.v1.ResourceMetrics;
import io.opentelemetry.proto.trace.v1.ResourceSpans;

/**
 * Aspects that can be found in an {@link OtelTrace}.
 *
 * @author Eya-Tom Augustin SANGAM
 */
public final class OtelAspects {

    private static final @NonNull ITmfEventAspect<ResourceSpans> RESOURCE_SPANS_ASPECT = new ITmfEventAspect<ResourceSpans>() {

        private static final String RESOURCE_SPANS_FIELD_NAME = "resource_spans"; //$NON-NLS-1$

        @Override
        public String getName() {
            return Messages.getMessage(Messages.AspectName_ResourceSpans);
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.AspectHelpText_ResourceSpans);
        }

        @Override
        public @Nullable ResourceSpans resolve(ITmfEvent event) {
            CtfTmfEventField ctfField = (CtfTmfEventField) event.getContent().getField(RESOURCE_SPANS_FIELD_NAME);
            if (ctfField == null) {
                return null;
            }

            long[] longArr;
            try {
                longArr = (long[]) ctfField.getValue();
            } catch (ClassCastException e) {
                return null;
            }

            byte[] bytes = createByteArray(longArr);

            try {
                return ResourceSpans.parseFrom(bytes);
            } catch (InvalidProtocolBufferException e) {
                return null;
            }
        }
    };

    private static final @NonNull ITmfEventAspect<ResourceMetrics> RESOURCE_METRICS_ASPECT = new ITmfEventAspect<ResourceMetrics>() {

        private static final String RESOURCE_METRICS_FIELD_NAME = "resource_metrics"; //$NON-NLS-1$

        @Override
        public String getName() {
            return Messages.getMessage(Messages.AspectName_ResourceMetrics);
        }

        @Override
        public String getHelpText() {
            return Messages.getMessage(Messages.AspectHelpText_ResourceMetrics);
        }

        @Override
        public @Nullable ResourceMetrics resolve(ITmfEvent event) {
            CtfTmfEventField ctfField = (CtfTmfEventField) event.getContent().getField(RESOURCE_METRICS_FIELD_NAME);
            if (ctfField == null) {
                return null;
            }

            long[] longArr;
            try {
                longArr = (long[]) ctfField.getValue();
            } catch (ClassCastException e) {
                return null;
            }

            byte[] bytes = createByteArray(longArr);

            try {
                return ResourceMetrics.parseFrom(bytes);
            } catch (InvalidProtocolBufferException e) {
                return null;
            }
        }
    };

    private static final @NonNull Collection<ITmfEventAspect<?>> ASPECTS = ImmutableList.of(
            getResourceSpansAspect(),
            getResourceMetricsAspect());

    private OtelAspects() {

    }

    /**
     * Get the aspect for the event resource spans
     *
     * @return The resource spans aspect
     */
    public static @NonNull ITmfEventAspect<ResourceSpans> getResourceSpansAspect() {
        return RESOURCE_SPANS_ASPECT;
    }

    /**
     * Get the aspect for the event resource metrics
     *
     * @return The resource metrics aspect
     */
    public static @NonNull ITmfEventAspect<ResourceMetrics> getResourceMetricsAspect() {
        return RESOURCE_METRICS_ASPECT;
    }

    /**
     * Get the list of all common base aspects
     *
     * @return the list of base aspects
     */
    public static @NonNull Collection<ITmfEventAspect<?>> getAspects() {
        return ASPECTS;
    }

    private static byte[] createByteArray(long[] arr) {
        byte[] bytes = new byte[arr.length];
        for (int i = 0; i < arr.length; i++) {
            bytes[i] = (byte) arr[i];
        }
        return bytes;
    }

}
