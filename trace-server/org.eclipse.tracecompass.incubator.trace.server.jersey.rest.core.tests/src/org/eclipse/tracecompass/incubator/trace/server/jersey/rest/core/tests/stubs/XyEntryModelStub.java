/**********************************************************************
 * Copyright (c) 2023 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License 2.0 which
 * accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
package org.eclipse.tracecompass.incubator.trace.server.jersey.rest.core.tests.stubs;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A Stub class for the entry model. It matches the trace server protocol's
 * <code>XYEntryModel</code> schema
 *
 * @author Bernd Hufmann
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class XyEntryModelStub implements Serializable {

    private static final long serialVersionUID = -5846598539343152768L;

    private final List<XyEntryStub> fEntries;
    private final List<EntryHeaderStub> fHeaders;

    /**
     * {@link JsonCreator} Constructor for final fields
     *
     * @param entries
     *            The set of entries for this model
     * @param headers
     *            The set of headers for this model
     */
    @JsonCreator
    public XyEntryModelStub(@JsonProperty("entries") List<XyEntryStub> entries,
            @JsonProperty("headers") List<EntryHeaderStub> headers) {
        fEntries = Objects.requireNonNull(entries, "The 'entries' json field was not set");
        fHeaders = headers == null ? Collections.emptyList() : headers;
    }

    /**
     * Get the entries described by this model
     *
     * @return The entries in this model
     */
    public List<XyEntryStub> getEntries() {
        return fEntries;
    }

    /**
     * Get the headers that describe this model
     *
     * @return The headers in this model
     */
    public List<EntryHeaderStub> getHeaders() {
        return fHeaders;
    }
}
