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
 * Class which all string constants related to an OpenTelemetry trace.
 *
 * @author Eya-Tom Augustin SANGAM
 */
public class Constants {

    /**
     * LTTng UST event provider name
     */
    public static final String PROVIDER_NAME = "opentelemetry:"; //$NON-NLS-1$

    /**
     * Name of LTTng UST event field for resource spans
     */
    public static final String RESOURCE_SPANS = "resource_spans"; //$NON-NLS-1$

    /**
     * Name of LTTng UST event field for resource metrics
     */
    public static final String RESOURCE_METRICS = "resource_metrics"; //$NON-NLS-1$

    /**
     * Name of LTTng UST event field for resource spans with provider name
     */
    public static final String RESOURCE_SPANS_FIELD_FULL_NAME = PROVIDER_NAME + RESOURCE_SPANS;

    /**
     * Name of LTTng UST event field for resource metrics with provider name
     */
    public static final String RESOURCE_METRICS_FIELD_FULL_NAME = PROVIDER_NAME + RESOURCE_METRICS;
}
