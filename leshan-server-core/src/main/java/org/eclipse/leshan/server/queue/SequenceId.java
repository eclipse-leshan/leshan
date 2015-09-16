/*******************************************************************************
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Alexander Ellwein (Bosch Software Innovations GmbH)
 *                     - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.queue;

import org.eclipse.leshan.util.Validate;

/**
 * A sequence ID represents a unique identifier, which can be used in order to "group" queue requests in sequences.
 * Because it is allowed to move requests up and down the queue, there are sometimes situations that some requests
 * cannot be shifted and has to remain their sequential order in the queue. For this reason, requests can be assigned to
 * the same sequence ID and though can only be moved up and down the queue as a complete sequence.
 *
 */
public final class SequenceId implements Comparable<SequenceId> {

    /** Sequence ID, which stands for uninitialized sequence. */
    public static final SequenceId NONE = new SequenceId();

    private final long sequenceNumber;

    private SequenceId() {
        this.sequenceNumber = Long.MAX_VALUE;
    }

    /**
     * Creates a new sequence ID with the given number.
     *
     * @param sequenceNumber sequence number to use
     */
    public SequenceId(long sequenceNumber) {
        Validate.isTrue(sequenceNumber != Long.MAX_VALUE, "sequence number may not be the maximum long value",
                sequenceNumber);
        this.sequenceNumber = sequenceNumber;
    }

    /**
     * @return sequence number assigned to this sequence ID.
     */
    public long getSequenceNumber() {
        return sequenceNumber;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else {
            return ((SequenceId) obj).getSequenceNumber() == getSequenceNumber();
        }
    }

    @Override
    public int hashCode() {
        return (int) this.sequenceNumber;
    }

    @Override
    public int compareTo(SequenceId sequenceId) {
        if (sequenceId == null)
            return -1;
        return (int) (getSequenceNumber() - ((SequenceId) sequenceId).getSequenceNumber());
    }

    @Override
    public String toString() {
        return new String("SequenceId (" + sequenceNumber + ")");
    }

    /**
     * @return true, if the sequence ID has been set.
     */
    public boolean isSet() {
        return !SequenceId.NONE.equals(this);
    }
}
