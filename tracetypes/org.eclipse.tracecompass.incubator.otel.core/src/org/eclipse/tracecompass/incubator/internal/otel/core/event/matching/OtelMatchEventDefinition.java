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
package org.eclipse.tracecompass.incubator.internal.otel.core.event.matching;

import org.eclipse.tracecompass.incubator.internal.otel.core.trace.OtelEvent;
import org.eclipse.tracecompass.incubator.internal.otel.core.trace.OtelEventType;
import org.eclipse.tracecompass.incubator.internal.otel.core.trace.OtelTrace;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.event.matching.ITmfTreeMatchEventDefinition;
import org.eclipse.tracecompass.tmf.core.event.matching.TmfEventMatching.Direction;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;

import com.google.protobuf.ByteString;

/**
 * Class to match {@link OtelEvent}
 *
 * @author Eya-Tom Augustin SANGAM
 */
public class OtelMatchEventDefinition implements ITmfTreeMatchEventDefinition {

    @Override
    public OtelEventMatchingKey getEventKey(ITmfEvent event) {
        if (event instanceof OtelEvent) {
            OtelEvent otelEvent = (OtelEvent) event;
            return new OtelEventMatchingKey(
                    otelEvent.getOtelEventType(),
                    otelEvent.getOtelTraceId(),
                    otelEvent.getOtelSpanId());
        }
        return null;
    }

    @Override
    public OtelEventMatchingKey getParentEventKey(ITmfEvent event) {
        if (event instanceof OtelEvent) {
            OtelEvent otelEvent = (OtelEvent) event;
            ByteString parentSpanId = otelEvent.getOtelParentSpanId();
            if (!parentSpanId.isEmpty()) {
                return new OtelEventMatchingKey(
                        otelEvent.getOtelEventType(),
                        otelEvent.getOtelTraceId(),
                        parentSpanId);
            }
        }
        return null;
    }

    @Override
    public boolean canMatchTrace(ITmfTrace trace) {
        return trace instanceof OtelTrace;
    }

    @Override
    public Direction getDirection(ITmfEvent event) {
        if (event instanceof OtelEvent) {
            OtelEventType otelEventType = ((OtelEvent) event).getOtelEventType();
            switch (otelEventType) {
            case START_SPAN:
                return Direction.EFFECT;
            case END_SPAN:
                return Direction.CAUSE;
            default:
                return null;
            }
        }
        return null;
    }

}
