/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.callstack.ui.views.functiondensity;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.tracecompass.analysis.timing.core.segmentstore.ISegmentStoreProvider;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.table.AbstractSegmentStoreTableViewer;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;

/**
 * Displays the Call Stack data in a column table
 *
 * @author Sonia Farrah
 */
public class FunctionTableViewer extends AbstractSegmentStoreTableViewer {

    private final String fAnalysisId;

    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------

    /**
     * Constructor
     *
     * @param tableViewer
     *            The table viewer
     * @param analysisId The ID of the analysis for this view
     */
    public FunctionTableViewer(@NonNull TableViewer tableViewer, String analysisId) {
        super(tableViewer);
        fAnalysisId = analysisId;
    }

    // ------------------------------------------------------------------------
    // Operations
    // ------------------------------------------------------------------------

    @Override
    protected @Nullable ISegmentStoreProvider getSegmentStoreProvider(@NonNull ITmfTrace trace) {
        IAnalysisModule module = trace.getAnalysisModule(fAnalysisId);
        if (!(module instanceof ISegmentStoreProvider)) {
            Iterable<@NonNull ISegmentStoreProvider> modules = TmfTraceUtils.getAnalysisModulesOfClass(trace, ISegmentStoreProvider.class);
            Optional<@NonNull ISegmentStoreProvider> providers = StreamSupport.stream(modules.spliterator(), false)
                    .filter(mod -> (mod instanceof IAnalysisModule && Objects.equals(fAnalysisId, ((IAnalysisModule) mod).getId())))
                    .findFirst();
            if (providers.isPresent()) {
                return providers.get();
            }
            return null;
        }
        return (ISegmentStoreProvider) module;
    }
}