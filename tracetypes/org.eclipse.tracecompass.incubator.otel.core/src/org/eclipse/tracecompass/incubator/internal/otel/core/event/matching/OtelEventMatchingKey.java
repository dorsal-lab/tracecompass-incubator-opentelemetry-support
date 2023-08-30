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

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.incubator.internal.otel.core.trace.OtelEventType;
import org.eclipse.tracecompass.tmf.core.event.matching.IEventMatchingKey;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;

class OtelEventMatchingKey implements IEventMatchingKey {

    private static final HashFunction HF = Preconditions.checkNotNull(Hashing.goodFastHash(32));
    private final OtelEventType fOtelEventType;
    private final String fOtelTraceId;
    private final String fOtelSpanId;

    public OtelEventMatchingKey(
            OtelEventType otelEventType,
            ByteString otelTraceId,
            ByteString otelSpanId) {
        fOtelEventType = otelEventType;
        fOtelTraceId = byteArrayToHexString(otelTraceId);
        fOtelSpanId = byteArrayToHexString(otelSpanId);
    }

    @Override
    public int hashCode() {
        return HF.newHasher()
                .putInt(fOtelEventType.hashCode())
                .putInt(fOtelTraceId.hashCode())
                .putInt(fOtelSpanId.hashCode())
                .hash().asInt();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof OtelEventMatchingKey) {
            OtelEventMatchingKey key = (OtelEventMatchingKey) o;
            /**
             * When we have a Child event, we get identify the parent event
             * using the trace ID and the parent
             */
            return (fOtelEventType.equals(key.fOtelEventType)
                    && fOtelTraceId.equals(key.fOtelTraceId)
                    && fOtelSpanId.equals(key.fOtelSpanId));
        }
        return false;
    }

    public static String byteArrayToHexString(ByteString byteString) {
        StringBuilder sb = new StringBuilder();
        for (byte b : byteString.toByteArray()) {
            sb.append(String.format("%02X", b)); //$NON-NLS-1$
        }
        return sb.toString();
    }

}
