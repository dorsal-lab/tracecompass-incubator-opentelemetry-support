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

package org.eclipse.tracecompass.incubator.internal.otel.ui.view.spanlife;

import java.util.Objects;

import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.MarkerEvent;

/**
 * Span event markers
 *
 * @author Matthew Khouzam
 */
class SpanMarkerEvent extends MarkerEvent {

    public SpanMarkerEvent(ITimeGraphEntry entry, long time, RGBA color) {
        super(entry, time, 0, "logs", color, null, true); //$NON-NLS-1$
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode());
    }

}
