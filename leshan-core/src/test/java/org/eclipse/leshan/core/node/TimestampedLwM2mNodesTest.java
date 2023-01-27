/*******************************************************************************
 * Copyright (c) 2021 Orange.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Orange - Send with multiple-timestamped values
 *******************************************************************************/
package org.eclipse.leshan.core.node;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class TimestampedLwM2mNodesTest {

    private final Instant timestamp_1E9_ms = Instant.ofEpochMilli(1_000_000_000);
    private final Instant timestamp_2E9_ms = Instant.ofEpochMilli(2_000_000_000);

    @Test
    public void should_getPathNodesMapForTimestamp_pick_specific_timestamp_nodes() {
        // given
        TimestampedLwM2mNodes tsNodes = getExampleTimestampedLwM2mNodes();

        // when
        Map<LwM2mPath, LwM2mNode> tsNodesMap = tsNodes.getNodesAt(timestamp_1E9_ms);

        // then
        assertNotNull(tsNodesMap);
        assertTrue(tsNodesMap.containsKey(new LwM2mPath("/0/0/1")));
        assertEquals(1L, tsNodesMap.get(new LwM2mPath("/0/0/1")).getId());
    }

    @Test
    public void should_getPathNodesMapForTimestamp_pick_null_timestamp_nodes() {
        // given
        TimestampedLwM2mNodes tsNodes = getExampleMixedTimestampLwM2mNodes();

        // when
        Map<LwM2mPath, LwM2mNode> tsNodesMap = tsNodes.getNodesAt(null);

        // then
        assertNotNull(tsNodesMap);
        assertTrue(tsNodesMap.containsKey(new LwM2mPath("/0/0/2")));
        assertEquals(2L, tsNodesMap.get(new LwM2mPath("/0/0/2")).getId());
    }

    @Test
    public void should_getPathNodesMapForTimestamp_returns_null_for_nonexistent_timestamp() {
        // given
        TimestampedLwM2mNodes tsNodes = getExampleTimestampedLwM2mNodes();

        // when
        Map<LwM2mPath, LwM2mNode> tsNodesMap = tsNodes.getNodesAt(Instant.EPOCH);

        // then
        assertNull(tsNodesMap);
    }

    @Test
    public void should_getNodes_returns_all_nodes_ignoring_timestamp() {
        // given
        TimestampedLwM2mNodes tsNodes = getExampleTimestampedLwM2mNodes();

        // when
        Map<LwM2mPath, LwM2mNode> tsNodesMap = tsNodes.getNodes();

        // then
        assertNotNull(tsNodesMap);
        assertTrue(tsNodesMap.containsKey(new LwM2mPath("/0/0/1")));
        assertTrue(tsNodesMap.containsKey(new LwM2mPath("/0/0/2")));
        assertEquals(1L, tsNodesMap.get(new LwM2mPath("/0/0/1")).getId());
        assertEquals(2L, tsNodesMap.get(new LwM2mPath("/0/0/2")).getId());
    }

    @Test
    public void should_getNodes_returns_latest_node_if_path_conflict() {
        // given
        TimestampedLwM2mNodes tsNodes = getSamePathTimestampedLwM2mNodes();

        // when
        Map<LwM2mPath, LwM2mNode> tsNodesMap = tsNodes.getNodes();

        // then
        assertNotNull(tsNodesMap);
        assertTrue(tsNodesMap.containsKey(new LwM2mPath("/0/0/1")));
        assertEquals(222L, ((LwM2mSingleResource) tsNodesMap.get(new LwM2mPath("/0/0/1"))).getValue());
    }

    @Test
    public void should_getTimestamps_returns_ascending_ordered_nodes() {
        // given
        TimestampedLwM2mNodes tsNodes = getExampleTimestampedLwM2mNodes();

        // when
        Set<Instant> timestamps = tsNodes.getTimestamps();

        // then
        assertNotNull(timestamps);
        Iterator<Instant> iterator = timestamps.iterator();
        assertEquals(timestamp_1E9_ms, iterator.next());
        assertEquals(timestamp_2E9_ms, iterator.next());
    }

    @Test
    public void should_null_timestamp_be_considered_as_latest_for_getTimestamps() {
        // given
        TimestampedLwM2mNodes tsNodes = getExampleMixedTimestampLwM2mNodes();

        // when
        Set<Instant> timestamps = tsNodes.getTimestamps();

        // then
        assertNotNull(timestamps);
        Iterator<Instant> iterator = timestamps.iterator();
        assertEquals(timestamp_1E9_ms, iterator.next());
        assertNull(iterator.next());
    }

    @Test
    public void should_getNodes_returns_empty_map_for_empty_TimestampedLwM2mNodes() {
        // given
        TimestampedLwM2mNodes tsNodes = TimestampedLwM2mNodes.builder().build();

        // when
        Map<LwM2mPath, LwM2mNode> tsNodesMap = tsNodes.getNodes();

        // then
        assertNotNull(tsNodesMap);
        assertTrue(tsNodesMap.isEmpty());
    }

    @Test
    public void should_getTimestamps_returns_all_timestamps() {
        // given
        TimestampedLwM2mNodes tsNodes = getExampleTimestampedLwM2mNodes();

        // when
        Set<Instant> timestamps = tsNodes.getTimestamps();

        // then
        assertNotNull(timestamps);
        assertEquals(new HashSet<>(Arrays.asList(timestamp_1E9_ms, timestamp_2E9_ms)), timestamps);
    }

    @Test
    public void should_raise_exception_for_duplicates() {
        // given
        TimestampedLwM2mNodes.Builder builder = TimestampedLwM2mNodes.builder();
        builder.put(timestamp_2E9_ms, new LwM2mPath("/0/0/2"), LwM2mSingleResource.newIntegerResource(2, 222L));
        builder.put(timestamp_1E9_ms, new LwM2mPath("/0/0/1"), LwM2mSingleResource.newIntegerResource(1, 111L));
        builder.put(timestamp_1E9_ms, new LwM2mPath("/0/0/1"), LwM2mSingleResource.newIntegerResource(1, 112L));

        // when
        assertThrowsExactly(IllegalArgumentException.class, () -> builder.build());
    }

    @Test
    public void should_raise_exception_id_path_does_not_match_node() {
        TimestampedLwM2mNodes.Builder builder = TimestampedLwM2mNodes.builder().put(timestamp_2E9_ms,
                new LwM2mPath("/0/0/2"), LwM2mResourceInstance.newIntegerInstance(0, 222L));
        assertThrowsExactly(IllegalArgumentException.class, () -> builder.build());
    }

    @Test
    public void should_raise_exception_path_does_not_match_id() {
        TimestampedLwM2mNodes.Builder builder = TimestampedLwM2mNodes.builder().put(timestamp_2E9_ms,
                new LwM2mPath("/0/0/2"), LwM2mSingleResource.newIntegerResource(1, 222L));
        assertThrowsExactly(IllegalArgumentException.class, () -> builder.build());
    }

    @Test
    public void should_not_raise_exception_for_duplicates() {
        // given
        TimestampedLwM2mNodes.Builder builder = TimestampedLwM2mNodes.builder().raiseExceptionOnDuplicate(false);
        builder.put(timestamp_2E9_ms, new LwM2mPath("/0/0/2"), LwM2mSingleResource.newIntegerResource(2, 222L));
        builder.put(timestamp_1E9_ms, new LwM2mPath("/0/0/1"), LwM2mSingleResource.newIntegerResource(1, 111L));
        builder.put(timestamp_1E9_ms, new LwM2mPath("/0/0/1"), LwM2mSingleResource.newIntegerResource(1, 112L));

        // when
        TimestampedLwM2mNodes nodes = builder.build();

        // then
        assertNotNull(nodes);
        TimestampedLwM2mNodes.Builder expected = TimestampedLwM2mNodes.builder();
        expected.put(timestamp_2E9_ms, new LwM2mPath("/0/0/2"), LwM2mSingleResource.newIntegerResource(2, 222L));
        expected.put(timestamp_1E9_ms, new LwM2mPath("/0/0/1"), LwM2mSingleResource.newIntegerResource(1, 112L));
        assertEquals(expected.build(), nodes);
    }

    private TimestampedLwM2mNodes getExampleTimestampedLwM2mNodes() {
        TimestampedLwM2mNodes.Builder tsNodes = TimestampedLwM2mNodes.builder();
        tsNodes.put(timestamp_2E9_ms, new LwM2mPath("/0/0/2"), LwM2mSingleResource.newIntegerResource(2, 222L));
        tsNodes.put(timestamp_1E9_ms, new LwM2mPath("/0/0/1"), LwM2mSingleResource.newIntegerResource(1, 111L));
        return tsNodes.build();
    }

    private TimestampedLwM2mNodes getSamePathTimestampedLwM2mNodes() {
        TimestampedLwM2mNodes.Builder tsNodes = TimestampedLwM2mNodes.builder();
        tsNodes.put(timestamp_2E9_ms, new LwM2mPath("/0/0/1"), LwM2mSingleResource.newIntegerResource(1, 222L));
        tsNodes.put(timestamp_1E9_ms, new LwM2mPath("/0/0/1"), LwM2mSingleResource.newIntegerResource(1, 111L));
        return tsNodes.build();
    }

    private TimestampedLwM2mNodes getExampleMixedTimestampLwM2mNodes() {
        TimestampedLwM2mNodes.Builder tsNodes = TimestampedLwM2mNodes.builder();
        tsNodes.put(new LwM2mPath("/0/0/2"), LwM2mSingleResource.newIntegerResource(2, 222L));
        tsNodes.put(timestamp_1E9_ms, new LwM2mPath("/0/0/1"), LwM2mSingleResource.newIntegerResource(1, 111L));
        return tsNodes.build();
    }
}
