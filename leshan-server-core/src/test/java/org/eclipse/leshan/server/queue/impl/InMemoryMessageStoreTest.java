/*******************************************************************************
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
 * <p/>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * <p/>
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.html.
 * <p/>
 * Contributors:
 * Balasubramanian Azhagappan (Bosch Software Innovations GmbH)
 * - initial API and implementation
 * Ingo Schaal (Bosch Software Innovations GmbH)
 *  -Tests
 *******************************************************************************/
package org.eclipse.leshan.server.queue.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.request.AbstractDownlinkRequest;
import org.eclipse.leshan.core.request.DownlinkRequest;
import org.eclipse.leshan.core.request.DownlinkRequestVisitor;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.server.queue.QueuedRequest;
import org.junit.Assert;
import org.junit.Test;

public class InMemoryMessageStoreTest {

    private static final String ENDPOINT = "myEndpoint";
    private final InMemoryMessageStore store = new InMemoryMessageStore();

    @Test
    public void add_one_entity_to_empty_queue() {
        QueuedRequest addNewQuedeRequest = getQueuedRequest(1);
        store.add(addNewQuedeRequest);

        Assert.assertEquals(store.getQueueSize(ENDPOINT), 1);
    }

    @Test
    public void add_one_entity_to_filled_queue() {
        fillQueue(5);
        QueuedRequest addNewQuedeRequest = getQueuedRequest(6);
        store.add(addNewQuedeRequest);

        Assert.assertEquals(store.getQueueSize(ENDPOINT), 6);
    }

    @Test
    public void retrieve_first_queued_request() {
        fillQueue(10);
        QueuedRequest result = store.retrieveFirst(ENDPOINT);

        Assert.assertEquals(result.getRequestTicket(), "1");
    }

    @Test
    public void retrieve_all_queued_request() {
        fillQueue(10);

        Assert.assertEquals(store.getQueueSize(ENDPOINT), 10);
    }

    @Test
    public void retrieve_all_Empty() {
        fillQueue(10);

        Assert.assertEquals(store.getQueueSize("unknown"), 0);
    }

    @Test
    public void test_if_queue_is_empty_true() {
        assertTrue(store.isEmpty(ENDPOINT));
    }

    @Test
    public void test_if_queue_is_empty_false() {
        fillQueue(10);

        assertFalse(store.isEmpty(ENDPOINT));
    }

    @Test
    public void remove_all_elements() {
        fillQueue(10);
        store.removeAll(ENDPOINT);

        Assert.assertEquals(store.getQueueSize(ENDPOINT), 0);
    }

    @Test
    public void delete_first_element_of_queue() {
        fillQueue(10);
        Assert.assertEquals(store.retrieveFirst(ENDPOINT).getRequestTicket(), "1");
        store.deleteFirst(ENDPOINT);
        Assert.assertNotEquals(store.retrieveFirst(ENDPOINT).getRequestTicket(), "1");
        Assert.assertEquals(store.retrieveFirst(ENDPOINT).getRequestTicket(), "2");
    }

    /**
     * Method creates a new QueuedRequest. With requestTicketID as requestTicket
     * 
     * @param requestTicketID
     * @return
     */
    private QueuedRequest getQueuedRequest(final int requestTicketID) {

        QueuedRequest queuedRequest = new QueuedRequest() {

            String requestTicket = "" + requestTicketID;

            @Override
            public String getRequestTicket() {
                return requestTicket;
            }

            @Override
            public String getEndpoint() {
                return ENDPOINT;
            }

            @Override
            public DownlinkRequest<LwM2mResponse> getDownlinkRequest() {
                return new AbstractDownlinkRequest<LwM2mResponse>(new LwM2mPath("3")) {

                    @Override
                    public void accept(DownlinkRequestVisitor visitor) {
                        // Noop.
                    }
                };

            }
        };

        return queuedRequest;
    }

    /**
     * Fills the Queue with given size.
     * 
     * @param size number of QueuedRequest to add to Queue.
     */
    private void fillQueue(int size) {
        for (int i = 1; i < size + 1; i++) {
            store.add(getQueuedRequest(i));
        }
    }

}
