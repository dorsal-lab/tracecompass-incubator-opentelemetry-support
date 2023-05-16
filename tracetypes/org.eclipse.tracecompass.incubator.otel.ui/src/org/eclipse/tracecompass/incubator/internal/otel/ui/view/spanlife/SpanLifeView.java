/*******************************************************************************
 * Copyright (c) 2023 Polytechnique Montreal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package org.eclipse.tracecompass.incubator.internal.otel.ui.view.spanlife;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGBA;
import org.eclipse.tracecompass.incubator.internal.otel.core.analysis.spanlife.SpanLifeAnalysis;
import org.eclipse.tracecompass.incubator.internal.otel.core.analysis.spanlife.SpanLifeDataProvider;
import org.eclipse.tracecompass.incubator.internal.otel.core.analysis.spanlife.SpanLifeEntryModel;
import org.eclipse.tracecompass.incubator.internal.otel.core.analysis.spanlife.SpanLifeEntryModel.LogEvent;
import org.eclipse.tracecompass.incubator.internal.otel.ui.Activator;
import org.eclipse.tracecompass.incubator.internal.otel.ui.Messages;
import org.eclipse.tracecompass.tmf.core.model.tree.ITmfTreeDataModel;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.experiment.TmfExperiment;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.BaseDataProviderTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.IMarkerEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * Simple gantt chart to see the life of the spans
 *
 * @author Eya-Tom Augustin SANGAM
 */
public class SpanLifeView extends BaseDataProviderTimeGraphView {

    /**
     * Span life view Id
     */
    public static final String ID = "org.eclipse.tracecompass.incubator.otel.ui.view.life.spanlife.view"; //$NON-NLS-1$

    private static final RGBA MARKER_COLOR = new RGBA(200, 0, 0, 150);

    private static final Image ERROR_IMAGE = Objects.requireNonNull(Activator.getDefault()).getImageFromPath("icons/delete_button.gif"); //$NON-NLS-1$

    private static class SpanTreeLabelProvider extends TreeLabelProvider {

        @Override
        public @Nullable Image getColumnImage(@Nullable Object element, int columnIndex) {
            if (columnIndex == 0 && element instanceof TimeGraphEntry) {
                TimeGraphEntry entry = (TimeGraphEntry) element;
                ITmfTreeDataModel entryModel = entry.getEntryModel();
                if ((entryModel instanceof SpanLifeEntryModel) && ((SpanLifeEntryModel) entryModel).getErrorTag()) {
                    return ERROR_IMAGE;
                }
            }
            return null;
        }
    }

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            Messages.SpanLifeView_stateTypeName
    };

    /**
     * Constructor
     */
    public SpanLifeView() {
        this(ID, new SpanLifePresentationProvider(), SpanLifeAnalysis.ID + SpanLifeDataProvider.SUFFIX);
        setTreeLabelProvider(new SpanTreeLabelProvider());
    }

    /**
     * Extendable constructor
     *
     * @param id
     *            the view ID
     * @param pres
     *            the presentation provider
     * @param dpID
     *            the dataprovider ID
     */
    public SpanLifeView(String id, TimeGraphPresentationProvider pres, String dpID) {
        super(id, pres, dpID);
        setFilterColumns(FILTER_COLUMN_NAMES);
        setFilterLabelProvider(new SpansFilterLabelProvider());
        setEntryComparator(new SpansEntryComparator());
    }

    private static class SpansEntryComparator implements Comparator<ITimeGraphEntry> {
        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            if (o1 instanceof TraceEntry && o2 instanceof TraceEntry) {
                /* sort trace entries alphabetically */
                return o1.getName().compareTo(o2.getName());
            }
            return 0;
        }
    }

    private static class SpansFilterLabelProvider extends TreeLabelProvider {
        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (columnIndex == 0 && element instanceof TimeGraphEntry) {
                return ((TimeGraphEntry) element).getName();
            }
            return ""; //$NON-NLS-1$
        }
    }

    @Override
    protected @NonNull List<IMarkerEvent> getViewMarkerList(Iterable<@NonNull TimeGraphEntry> entries, long startTime, long endTime,
            long resolution, @NonNull IProgressMonitor monitor) {
        ITimeGraphEntry[] expandedElements = getTimeGraphViewer().getExpandedElements();
        List<ITimeGraphEntry> queriedElements = new ArrayList<>();
        for (ITimeGraphEntry candidate : expandedElements) {
            if (StreamSupport.stream(entries.spliterator(), false).anyMatch(candidate::equals)) {
                queriedElements.add(candidate);
            }
        }
        if (queriedElements.isEmpty()) {
            // Fall-back to this method's previous implementation to not break
            // anything. Still possible to improve caller's efficiency later.
            queriedElements = Arrays.asList(expandedElements);
        }
        List<IMarkerEvent> markers = new ArrayList<>();
        for (ITimeGraphEntry element : queriedElements) {
            ITmfTreeDataModel entryModel = ((TimeGraphEntry) element).getEntryModel();
            if (entryModel instanceof SpanLifeEntryModel) {
                SpanLifeEntryModel model = (SpanLifeEntryModel) entryModel;
                for (LogEvent log : model.getLogs()) {
                    markers.add(new SpanMarkerEvent(element, log.getTime(), MARKER_COLOR));
                }
            }
        }
        return markers;
    }

    @Override
    protected void buildEntryList(@NonNull ITmfTrace trace, @NonNull ITmfTrace parentTrace, @NonNull IProgressMonitor monitor) {
        super.buildEntryList((parentTrace instanceof TmfExperiment) ? parentTrace : trace, parentTrace, monitor);
    }

}
