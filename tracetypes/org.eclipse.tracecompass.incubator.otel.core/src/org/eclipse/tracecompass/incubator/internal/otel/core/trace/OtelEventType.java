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

/**
 * An enumeration with all different events from an {@link OtelTrace}
 *
 * @author Eya-Tom Augustin SANGAM
 */
public enum OtelEventType {
    /**
     * Event corresponding to a start of a span
     */
    START_SPAN,

    /**
     * Event corresponding to the end of a span
     */
    END_SPAN
}
