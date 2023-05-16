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

    /**
     * All constants related to start_span event
     */
    public class SpanEvents {

        /**
         * start_span event EventType id
         */
        public static final String START_SPAN_EVENT_TYPE_ID = "start_span"; //$NON-NLS-1$

        /**
         * end_span event EventType id
         */
        public static final String END_SPAN_EVENT_TYPE_ID = "end_span"; //$NON-NLS-1$

        /**
         * All start_span event fields constants
         */
        public class Fields {

            /**
             * resource field
             */
            public static final String RESOURCE = "resource"; //$NON-NLS-1$
            /**
             * resource_schema_url field
             */
            public static final String RESOURCE_SCHEMA_URL = "resource_schema_url"; //$NON-NLS-1$
            /**
             * instrumentation_library field
             */
            public static final String INSTRUMENTATION_LIBRARY = "instrumentation_library"; //$NON-NLS-1$
            /**
             * Resource instrumentation_library_schema_url
             */
            public static final String INSTRUMENTATION_LIBRARY_SCHEMA_URL = "instrumentation_library_schema_url"; //$NON-NLS-1$
            /**
             * Resource span
             */
            public static final String SPAN = "span"; //$NON-NLS-1$

        }

    }

}
