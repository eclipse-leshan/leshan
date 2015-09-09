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
package org.eclipse.leshan.server.queue.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.leshan.server.client.Client;
import org.eclipse.leshan.server.queue.QueueManagement;
import org.eclipse.leshan.server.queue.QueueReactor;
import org.eclipse.leshan.server.queue.QueueRequest;
import org.eclipse.leshan.server.queue.QueueRequestFactory;
import org.eclipse.leshan.server.queue.QueueTask;
import org.eclipse.leshan.server.queue.RequestQueue;
import org.eclipse.leshan.server.queue.RequestState;
import org.eclipse.leshan.server.queue.SequenceId;

/**
 * A request queue implementation, which keeps all the requests in memory.
 *
 * @see RequestQueue
 * @see QueueManagement
 */
public class RequestQueueImpl implements RequestQueue, QueueManagement {

    private final Map<SequenceId, List<QueueRequest>> requests = new HashMap<>();
    private final Map<String, List<SequenceId>> sequences = new HashMap<>();
    private long counter = 0L;

    private QueueRequestFactory requestFactory;
    private QueueReactor queueReactor;

    /**
     * Creates a new request queue.
     *
     * @param requestFactory request factory used here
     * @param queueReactor reactor which is used for scheduling queue management tasks
     */
    public RequestQueueImpl(QueueRequestFactory requestFactory, QueueReactor queueReactor) {
        this.requestFactory = requestFactory;
        this.queueReactor = queueReactor;
    }

    SequenceId getCreateSequenceId(QueueRequest queueRequest) {
        SequenceId id = queueRequest.getSequenceId();
        return id.isSet() ? id : createSequenceId();
    }

    List<SequenceId> getCreateClientSequences(Client client) {
        List<SequenceId> clientSequences = sequences.get(client.getEndpoint());
        return (clientSequences == null) ? new ArrayList<SequenceId>() : clientSequences;
    }

    List<QueueRequest> getCreateRequestsForSequenceId(SequenceId id) {
        List<QueueRequest> clientRequests = requests.get(id);
        return (clientRequests == null) ? new ArrayList<QueueRequest>() : clientRequests;
    }

    @Override
    public SequenceId enqueueRequest(QueueRequest queueRequest) {
        SequenceId id = getCreateSequenceId(queueRequest);
        enqueueRequest(queueRequest, id);
        return id;
    }

    @Override
    public void enqueueRequest(QueueRequest queueRequest, SequenceId existingId) {
        if (queueRequest.getSequenceId().isSet()) {
            throw new IllegalStateException("request is already enqueued");
        }
        Client client = queueRequest.getClient();
        List<SequenceId> clientSequences = getCreateClientSequences(client);
        List<QueueRequest> clientRequests = getCreateRequestsForSequenceId(existingId);

        QueueRequest newRequest = doRequestTransition(queueRequest, existingId, RequestState.ENQUEUED);

        if (!clientSequences.contains(existingId)) {
            clientSequences.add(existingId);
        }
        clientRequests.add(newRequest);
        sequences.put(client.getEndpoint(), clientSequences);
        requests.put(existingId, clientRequests);
    }

    QueueRequest doRequestTransition(QueueRequest request, SequenceId seqId, RequestState newState) {
        return requestFactory.transformRequest(request, seqId, newState);
    }

    SequenceId createSequenceId() {
        return new SequenceId(++counter);
    }

    @Override
    public void processingRequest(QueueRequest queueRequest) {
        doRequestTransition(queueRequest, queueRequest.getSequenceId(), RequestState.PROCESSING);
    }

    @Override
    public void deferRequest(QueueRequest queueRequest) {
        doRequestTransition(queueRequest, queueRequest.getSequenceId(), RequestState.DEFERRED);
    }

    @Override
    public void ttlElapsedRequest(QueueRequest queueRequest) {
        doRequestTransition(queueRequest, queueRequest.getSequenceId(), RequestState.TTL_ELAPSED);
    }

    @Override
    public void executedRequest(QueueRequest queueRequest) {
        doRequestTransition(queueRequest, queueRequest.getSequenceId(), RequestState.EXECUTED);
    }

    @Override
    public void unqueueRequest(QueueRequest queueRequest) {
        doRequestTransition(queueRequest, queueRequest.getSequenceId(), RequestState.UNKNOWN);
        dropRequest(queueRequest);
    }

    @Override
    public Collection<QueueRequest> getRequests(String endpoint) {
        List<QueueRequest> result = new ArrayList<>();
        List<SequenceId> clientSequences = sequences.get(endpoint);
        if (clientSequences == null) {
            return Collections.<QueueRequest> emptyList();
        }
        for (SequenceId seqId : clientSequences) {
            List<QueueRequest> clientRequests = requests.get(seqId);
            if (clientRequests != null) {
                result.addAll(clientRequests);
            }
        }
        return Collections.unmodifiableCollection(result);
    }

    @Override
    public void moveTop(final QueueRequest queueRequest) {
        queueReactor.addTask(new QueueTask() {
            @Override
            public void run() {
                Client client = queueRequest.getClient();
                List<SequenceId> clientSequences = sequences.get(client.getEndpoint());
                SequenceId sequenceId = queueRequest.getSequenceId();
                int indexOf = clientSequences.indexOf(sequenceId);
                if (indexOf > 0) {
                    clientSequences.remove(indexOf);
                    clientSequences.add(0, sequenceId);
                }
            }

            @Override
            public boolean wouldBlock() {
                return false;
            }
        });
    }

    @Override
    public void moveSequenceTop(final String endpoint, final SequenceId sequenceId) {
        queueReactor.addTask(new QueueTask() {
            @Override
            public void run() {
                List<SequenceId> clientSequences = sequences.get(endpoint);
                int indexOf = clientSequences.indexOf(sequenceId);
                if (indexOf > 0) {
                    clientSequences.remove(indexOf);
                    clientSequences.add(0, sequenceId);
                }
            }

            @Override
            public boolean wouldBlock() {
                return false;
            }
        });
    }

    @Override
    public void moveBottom(final QueueRequest queueRequest) {
        queueReactor.addTask(new QueueTask() {
            @Override
            public void run() {
                Client client = queueRequest.getClient();
                List<SequenceId> clientSequences = sequences.get(client.getEndpoint());
                SequenceId sequenceId = queueRequest.getSequenceId();
                int indexOf = clientSequences.indexOf(sequenceId);
                if (indexOf < clientSequences.size() - 1) {
                    clientSequences.remove(indexOf);
                    clientSequences.add(sequenceId);
                }
            }

            @Override
            public boolean wouldBlock() {
                return false;
            }
        });
    }

    @Override
    public void moveSequenceBottom(final String endpoint, final SequenceId sequenceId) {
        queueReactor.addTask(new QueueTask() {

            @Override
            public void run() {
                List<SequenceId> clientSequences = sequences.get(endpoint);
                int indexOf = clientSequences.indexOf(sequenceId);
                if (indexOf < clientSequences.size() - 1) {
                    clientSequences.remove(indexOf);
                    clientSequences.add(sequenceId);
                }
            }

            @Override
            public boolean wouldBlock() {
                return false;
            }
        });
    }

    @Override
    public void moveUp(final QueueRequest queueRequest) {
        queueReactor.addTask(new QueueTask() {

            @Override
            public void run() {
                Client client = queueRequest.getClient();
                List<SequenceId> clientSequences = sequences.get(client.getEndpoint());
                SequenceId sequenceId = queueRequest.getSequenceId();
                int indexOf = clientSequences.indexOf(sequenceId);
                if (indexOf > 0) {
                    Collections.swap(clientSequences, indexOf, indexOf - 1);
                }
            }

            @Override
            public boolean wouldBlock() {
                return false;
            }
        });
    }

    @Override
    public void moveSequenceUp(final String endpoint, final SequenceId sequenceId) {
        queueReactor.addTask(new QueueTask() {

            @Override
            public void run() {
                List<SequenceId> clientSequences = sequences.get(endpoint);
                int indexOf = clientSequences.indexOf(sequenceId);
                if (indexOf > 0) {
                    Collections.swap(clientSequences, indexOf, indexOf - 1);
                }
            }

            @Override
            public boolean wouldBlock() {
                return false;
            }
        });
    }

    @Override
    public void moveDown(final QueueRequest queueRequest) {
        queueReactor.addTask(new QueueTask() {
            @Override
            public void run() {
                Client client = queueRequest.getClient();
                List<SequenceId> clientSequences = sequences.get(client.getEndpoint());
                SequenceId sequenceId = queueRequest.getSequenceId();
                int indexOf = clientSequences.indexOf(sequenceId);
                if (indexOf < clientSequences.size() - 1) {
                    Collections.swap(clientSequences, indexOf, indexOf + 1);
                }
            }

            @Override
            public boolean wouldBlock() {
                return false;
            }
        });
    }

    @Override
    public void moveSequenceDown(final String endpoint, final SequenceId sequenceId) {
        queueReactor.addTask(new QueueTask() {
            @Override
            public void run() {
                List<SequenceId> clientSequences = sequences.get(endpoint);
                int indexOf = clientSequences.indexOf(sequenceId);
                if (indexOf < clientSequences.size() - 1) {
                    Collections.swap(clientSequences, indexOf, indexOf + 1);
                }
            }

            @Override
            public boolean wouldBlock() {
                return false;
            }
        });
    }

    @Override
    public void dropRequest(final QueueRequest queueRequest) {
        queueReactor.addTask(new QueueTask() {
            @Override
            public void run() {
                SequenceId sequenceId = queueRequest.getSequenceId();
                if (!sequenceId.isSet()) {
                    throw new IllegalStateException("unable to drop unqueued request");
                }
                Client client = queueRequest.getClient();
                String endpoint = client.getEndpoint();
                if (requests.containsKey(sequenceId)) {
                    List<QueueRequest> listRequests = requests.get(sequenceId);
                    listRequests.remove(queueRequest);
                    if (listRequests.isEmpty()) {
                        requests.remove(sequenceId);
                        List<SequenceId> listSequences = sequences.get(endpoint);
                        listSequences.remove(sequenceId);
                        listRequests = null;
                    }
                }
            }

            @Override
            public boolean wouldBlock() {
                return false;
            }
        });
    }

    @Override
    public void dropSequence(final String endpoint, final SequenceId sequenceId) {
        queueReactor.addTask(new QueueTask() {
            @Override
            public void run() {
                if (!sequenceId.isSet()) {
                    throw new IllegalStateException("unable to drop illegal sequence ID");
                }
                if (requests.containsKey(sequenceId)) {
                    requests.remove(sequenceId);
                    List<SequenceId> listSequences = sequences.get(endpoint);
                    listSequences.remove(sequenceId);
                }
            }
            @Override
            public boolean wouldBlock() {
                return false;
            }
        });
    }

    @Override
    public Set<String> getEndpoints() {
        return Collections.unmodifiableSet(sequences.keySet());
    }
}
