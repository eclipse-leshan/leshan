/*******************************************************************************
 * Copyright (c) 2026 Sierra Wireless and others.
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
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.registration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.leshan.core.LwM2m.LwM2mVersion;
import org.eclipse.leshan.core.LwM2m.Version;
import org.eclipse.leshan.core.endpoint.EndpointUri;
import org.eclipse.leshan.core.node.LwM2mPath;
import org.eclipse.leshan.core.observation.ObservationIdentifier;
import org.eclipse.leshan.core.observation.SingleObservation;
import org.eclipse.leshan.core.peer.IpPeer;
import org.eclipse.leshan.core.peer.LwM2mPeer;
import org.eclipse.leshan.core.request.ContentFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests covering the LwM2M Gateway support in {@link InMemoryRegistrationStore}.
 * <p>
 * The "classic" registration flow is covered by {@link InMemoryRegistrationStoreTest}; this class focuses on
 * gateway/end-device specific behavior introduced for OMA LwM2M Gateway v1.1.1 (Object 25).
 */
class InMemoryRegistrationStoreGatewayTest {

    private InMemoryRegistrationStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryRegistrationStore();
    }

    // *********************************************************************
    // addRegistration
    // *********************************************************************

    @Test
    @DisplayName("Adding a gateway with no children indexes it like a classic registration")
    void add_without_children() {
        DeviceRegistration gw = newGateway("gw-regid", "gw-ep", 5683);

        Deregistration prev = store.addRegistration(gw);

        assertNull(prev);
        assertSame(gw, store.getRegistration("gw-regid"));
        assertSame(gw, store.getRegistrationByEndpoint("gw-ep"));
        assertSame(gw, store.getRegistrationByAdress(gw.getSocketAddress()));
        assertSame(gw, store.getRegistrationByIdentity(gw.getClientTransportData().getIdentity()));
    }

    @Test
    @DisplayName("addRegistration must reject EndDeviceRegistration directly")
    void add_end_device_directly_throws() {
        DeviceRegistration gw = newGateway("gw-regid", "gw-ep", 5683);
        store.addRegistration(gw);
        EndDeviceRegistration ed = newEndDevice(gw, "ed-regid", "d01", "ed-ep");

        assertThrows(IllegalStateException.class, () -> store.addRegistration(ed));
    }

    @Test
    @DisplayName("Re-registering a gateway under the same endpoint deregisters its previous children")
    void re_register_gateway_clears_old_children() {
        DeviceRegistration gw1 = newGateway("gw-regid-1", "gw-ep", 5683);
        store.addRegistration(gw1);
        store.replaceEndDeviceRegistrations(gw1, Arrays.asList(newEndDevice(gw1, "ed-regid", "d01", "ed-ep")));

        // Same endpoint, new registration id (different DTLS session typically -> different identity/port)
        DeviceRegistration gw2 = newGateway("gw-regid-2", "gw-ep", 5684);
        Deregistration removed = store.addRegistration(gw2);

        assertNotNull(removed);
        assertEquals("gw-regid-1", removed.getRegistration().getId());
        assertEquals("ed-regid", removed.getChildrenDeRegistration().get(0).getRegistration().getId());

        // The previous gateway's child must be gone from all indexes.
        assertNull(store.getRegistration("ed-regid"));
        assertNull(store.getRegistrationByEndpoint("ed-ep"));

        // The new gateway is in place with no children.
        Registration newGw = store.getRegistration("gw-regid-2");
        assertNotNull(newGw);
        assertTrue(newGw.getChildEndDevices().isEmpty());
    }

    // *********************************************************************
    // replaceEndDeviceRegistrations - happy path
    // *********************************************************************

    @Test
    @DisplayName("replaceEndDeviceRegistrations adds new children and updates the gateway's child map")
    void replace_adds_children() {
        DeviceRegistration gw = newGateway("gw-regid", "gw-ep", 5683);
        store.addRegistration(gw);

        DeviceRegistration current = currentGateway("gw-regid");
        EndDeviceRegistration a = newEndDevice(current, "ed-regid-1", "d01", "ed-ep-1");
        EndDeviceRegistration b = newEndDevice(current, "ed-regid-2", "d02", "ed-ep-2");

        List<RegistrationModification> mods = store.replaceEndDeviceRegistrations(current, Arrays.asList(a, b));

        assertEquals(2, mods.size());
        assertTrue(mods.stream().allMatch(RegistrationAddition.class::isInstance),
                "All modifications should be additions");

        // Children reachable by endpoint and reg-id
        assertSame(a, store.getRegistrationByEndpoint("ed-ep-1"));
        assertSame(b, store.getRegistrationByEndpoint("ed-ep-2"));
        assertSame(a, store.getRegistration("ed-regid-1"));
        assertSame(b, store.getRegistration("ed-regid-2"));

        // Gateway now lists both children
        Registration updatedGw = store.getRegistration("gw-regid");
        Map<String, String> children = updatedGw.getChildEndDevices();
        assertEquals(2, children.size());
        assertEquals("ed-ep-1", children.get("d01"));
        assertEquals("ed-ep-2", children.get("d02"));

        // End devices are NOT indexed by addr or identity (they share the gateway's transport)
        // Looking up by the gateway's address should still return the gateway, not an end device.
        assertSame(updatedGw, store.getRegistrationByAdress(gw.getSocketAddress()));
        assertSame(updatedGw, store.getRegistrationByIdentity(gw.getClientTransportData().getIdentity()));
    }

    @Test
    @DisplayName("getAllRegistrations enumerates gateways and their end devices")
    void get_all_returns_gateways_and_children() {
        DeviceRegistration gw = newGateway("gw-regid", "gw-ep", 5683);
        store.addRegistration(gw);
        DeviceRegistration current = currentGateway("gw-regid");
        store.replaceEndDeviceRegistrations(current,
                Arrays.asList(newEndDevice(current, "ed-regid-1", "d01", "ed-ep-1"),
                        newEndDevice(current, "ed-regid-2", "d02", "ed-ep-2")));

        List<Registration> all = new ArrayList<>();
        Iterator<Registration> it = store.getAllRegistrations();
        while (it.hasNext()) {
            all.add(it.next());
        }

        assertEquals(3, all.size());
        assertTrue(all.stream().anyMatch(r -> r.getId().equals("gw-regid")));
        assertTrue(all.stream().anyMatch(r -> r.getId().equals("ed-regid-1")));
        assertTrue(all.stream().anyMatch(r -> r.getId().equals("ed-regid-2")));
    }

    // *********************************************************************
    // replaceEndDeviceRegistrations - update / remove
    // *********************************************************************

    @Test
    @DisplayName("Updating an end device with the same registration id preserves its observations")
    void update_end_device_preserves_observations() {
        DeviceRegistration gw = newGateway("gw-regid", "gw-ep", 5683);
        store.addRegistration(gw);
        DeviceRegistration current = currentGateway("gw-regid");
        EndDeviceRegistration a = newEndDevice(current, "ed-regid", "d01", "ed-ep");
        store.replaceEndDeviceRegistrations(current, Arrays.asList(a));

        // attach an observation to the end device
        SingleObservation obs = singleObservation("ed-regid", "/3303/0");
        store.addObservation("ed-regid", obs, false);
        assertEquals(1, store.getObservations("ed-regid").size());

        // re-register same end device (same id, same endpoint, same prefix) -> update
        DeviceRegistration current2 = currentGateway("gw-regid");
        EndDeviceRegistration aPrime = newEndDevice(current2, "ed-regid", "d01", "ed-ep");
        List<RegistrationModification> mods = store.replaceEndDeviceRegistrations(current2, Arrays.asList(aPrime));

        assertEquals(1, mods.size());
        assertInstanceOf(UpdatedRegistration.class, mods.get(0));

        // observations should survive the update -- this is the key regression check
        assertEquals(1, store.getObservations("ed-regid").size());
    }

    @Test
    @DisplayName("End devices not present in the new list are removed and their observations cleared")
    void replace_removes_orphans() {
        DeviceRegistration gw = newGateway("gw-regid", "gw-ep", 5683);
        store.addRegistration(gw);
        DeviceRegistration current = currentGateway("gw-regid");
        EndDeviceRegistration a = newEndDevice(current, "ed-regid-1", "d01", "ed-ep-1");
        EndDeviceRegistration b = newEndDevice(current, "ed-regid-2", "d02", "ed-ep-2");
        store.replaceEndDeviceRegistrations(current, Arrays.asList(a, b));
        store.addObservation("ed-regid-1", singleObservation("ed-regid-1", "/3/0"), false);

        // now replace with only "b" — "a" should be deregistered
        DeviceRegistration current2 = currentGateway("gw-regid");
        EndDeviceRegistration bRebuilt = newEndDevice(current2, "ed-regid-2", "d02", "ed-ep-2");
        List<RegistrationModification> mods = store.replaceEndDeviceRegistrations(current2, Arrays.asList(bRebuilt));

        // we expect at least one Deregistration referring to ed-regid-1
        boolean foundARemoval = mods.stream().filter(Deregistration.class::isInstance).map(m -> (Deregistration) m)
                .anyMatch(d -> "ed-regid-1".equals(d.getRegistration().getId()));
        assertTrue(foundARemoval, "expected a Deregistration for ed-regid-1, got: " + mods);

        assertNull(store.getRegistration("ed-regid-1"));
        assertNull(store.getRegistrationByEndpoint("ed-ep-1"));
        assertTrue(store.getObservations("ed-regid-1").isEmpty());

        // Gateway's child map updated
        assertEquals(1, store.getRegistration("gw-regid").getChildEndDevices().size());
        assertEquals("ed-ep-2", store.getRegistration("gw-regid").getChildEndDevices().get("d02"));
    }

    @Test
    @DisplayName("Replacing with an empty list detaches all end devices and updates the gateway's child map")
    void replace_with_empty_detaches_all() {
        DeviceRegistration gw = newGateway("gw-regid", "gw-ep", 5683);
        store.addRegistration(gw);
        DeviceRegistration current = currentGateway("gw-regid");
        store.replaceEndDeviceRegistrations(current,
                Arrays.asList(newEndDevice(current, "ed-regid-1", "d01", "ed-ep-1"),
                        newEndDevice(current, "ed-regid-2", "d02", "ed-ep-2")));

        store.replaceEndDeviceRegistrations(currentGateway("gw-regid"), Collections.emptyList());

        assertNull(store.getRegistration("ed-regid-1"));
        assertNull(store.getRegistration("ed-regid-2"));
        assertNull(store.getRegistrationByEndpoint("ed-ep-1"));
        assertNull(store.getRegistrationByEndpoint("ed-ep-2"));
        assertTrue(store.getRegistration("gw-regid").getChildEndDevices().isEmpty(),
                "gateway's child map must be cleared even when replacing with an empty list");
    }

    // *********************************************************************
    // replaceEndDeviceRegistrations - validation
    // *********************************************************************

    @Test
    @DisplayName("Two children with the same prefix in one call are rejected")
    void duplicate_prefixes_rejected() {
        DeviceRegistration gw = newGateway("gw-regid", "gw-ep", 5683);
        store.addRegistration(gw);
        DeviceRegistration current = currentGateway("gw-regid");

        EndDeviceRegistration a = newEndDevice(current, "ed-regid-1", "d01", "ed-ep-1");
        EndDeviceRegistration b = newEndDevice(current, "ed-regid-2", "d01", "ed-ep-2"); // duplicate prefix

        final List<EndDeviceRegistration> childrenList = Arrays.asList(a, b);
        assertThrows(IllegalStateException.class, () -> store.replaceEndDeviceRegistrations(current, childrenList));

        // store unchanged
        assertNull(store.getRegistration("ed-regid-1"));
        assertNull(store.getRegistration("ed-regid-2"));
        assertTrue(store.getRegistration("gw-regid").getChildEndDevices().isEmpty());
    }

    @Test
    @DisplayName("Reusing a registration id from a different gateway is rejected")
    void reg_id_collision_with_other_gateway_rejected() {
        DeviceRegistration gw1 = newGateway("gw-regid-1", "gw-ep-1", 5683);
        DeviceRegistration gw2 = newGateway("gw-regid-2", "gw-ep-2", 5684);
        store.addRegistration(gw1);
        store.addRegistration(gw2);

        // child of gw1 with a given regId
        DeviceRegistration current1 = currentGateway("gw-regid-1");
        EndDeviceRegistration a = newEndDevice(current1, "ed-regid-shared", "d01", "ed-ep-1");
        store.replaceEndDeviceRegistrations(current1, Arrays.asList(a));

        // try to attach a child to gw2 reusing the same regId -> reject
        DeviceRegistration current2 = currentGateway("gw-regid-2");
        EndDeviceRegistration aPrime = newEndDevice(current2, "ed-regid-shared", "d01", "ed-ep-2");

        final List<EndDeviceRegistration> childrenList = Arrays.asList(aPrime);
        assertThrows(IllegalStateException.class, () -> store.replaceEndDeviceRegistrations(current2, childrenList));

        // gw1's child unchanged
        assertSame(a, store.getRegistration("ed-regid-shared"));
    }

    @Test
    @DisplayName("Replacing children of a non-gateway registration is rejected")
    void replace_on_non_gateway_rejected() {
        DeviceRegistration regular = newRegularDevice("dev-regid", "dev-ep", 5683);
        store.addRegistration(regular);

        final List<EndDeviceRegistration> emptyList = Collections.emptyList();
        assertThrows(IllegalStateException.class, () -> store.replaceEndDeviceRegistrations(regular, emptyList));
    }

    @Test
    @DisplayName("Replacing children of an unknown gateway is rejected")
    void replace_on_unknown_gateway_rejected() {
        DeviceRegistration ghost = newGateway("gw-regid-ghost", "gw-ep-ghost", 5683);
        // not added to the store

        final List<EndDeviceRegistration> emptyList = Collections.emptyList();
        List<RegistrationModification> modifications = store.replaceEndDeviceRegistrations(ghost, emptyList);
        assertNull(modifications);
    }

    // *********************************************************************
    // removeRegistration
    // *********************************************************************

    @Test
    @DisplayName("Removing a gateway removes its end devices and observations")
    void remove_gateway_cascades() {
        DeviceRegistration gw = newGateway("gw-regid", "gw-ep", 5683);
        store.addRegistration(gw);
        DeviceRegistration current = currentGateway("gw-regid");
        store.replaceEndDeviceRegistrations(current, Arrays.asList(newEndDevice(current, "ed-regid", "d01", "ed-ep")));
        store.addObservation("ed-regid", singleObservation("ed-regid", "/3/0"), false);

        Deregistration dereg = store.removeRegistration("gw-regid");

        assertNotNull(dereg);
        assertEquals("gw-regid", dereg.getRegistration().getId());
        // child deregistrations are reported
        List<Deregistration> children = dereg.getChildrenDeRegistration();
        assertNotNull(children);
        assertEquals(1, children.size());
        assertEquals("ed-regid", children.get(0).getRegistration().getId());

        // store empty
        assertNull(store.getRegistration("gw-regid"));
        assertNull(store.getRegistration("ed-regid"));
        assertNull(store.getRegistrationByEndpoint("ed-ep"));
        assertNull(store.getRegistrationByEndpoint("gw-ep"));
        assertTrue(store.getObservations("ed-regid").isEmpty());
        assertTrue(store.getObservations("gw-regid").isEmpty());
    }

    @Test
    @DisplayName("removeRegistration on an end device throws — must go through the gateway")
    void remove_end_device_directly_throws() {
        DeviceRegistration gw = newGateway("gw-regid", "gw-ep", 5683);
        store.addRegistration(gw);
        DeviceRegistration current = currentGateway("gw-regid");
        store.replaceEndDeviceRegistrations(current, Arrays.asList(newEndDevice(current, "ed-regid", "d01", "ed-ep")));

        assertThrows(IllegalStateException.class, () -> store.removeRegistration("ed-regid"));

        // and the end device is still there
        assertNotNull(store.getRegistration("ed-regid"));
    }

    // *********************************************************************
    // updateRegistration
    // *********************************************************************

    @Test
    @DisplayName("updateRegistration on an end device throws — must go through the gateway")
    void update_end_device_directly_throws() {
        DeviceRegistration gw = newGateway("gw-regid", "gw-ep", 5683);
        store.addRegistration(gw);
        DeviceRegistration current = currentGateway("gw-regid");
        store.replaceEndDeviceRegistrations(current, Arrays.asList(newEndDevice(current, "ed-regid", "d01", "ed-ep")));

        RegistrationUpdate update = new RegistrationUpdate("ed-regid",
                new IpPeer(new InetSocketAddress("localhost", 5683)), null, null, null, null, null, null, null, null,
                null, null);
        assertThrows(IllegalStateException.class, () -> store.updateRegistration(update));
    }

    // *********************************************************************
    // Helpers
    // *********************************************************************

    /** Read the gateway back from the store so we use the up-to-date snapshot (with current child map). */
    private DeviceRegistration currentGateway(String regId) {
        Registration r = store.getRegistration(regId);
        assertInstanceOf(DeviceRegistration.class, r);
        return (DeviceRegistration) r;
    }

    private DeviceRegistration newGateway(String regId, String endpoint, int port) {
        return newDeviceRegistration(regId, endpoint, port, /* isGateway */ true);
    }

    private DeviceRegistration newRegularDevice(String regId, String endpoint, int port) {
        return newDeviceRegistration(regId, endpoint, port, /* isGateway */ false);
    }

    private DeviceRegistration newDeviceRegistration(String regId, String endpoint, int port, boolean isGateway) {
        // The TestPeerBuilder/TestPeer pattern is what Leshan tests typically use; here we keep it minimal.
        InetSocketAddress addr = new InetSocketAddress("127.0.0.1", port);
        LwM2mPeer peer = new IpPeer(addr); // adjust if IpPeer requires an Identity in your tree
        EndpointUri uri = new EndpointUri("coap", "localhost", 5683);

        Map<Integer, Version> supported = new HashMap<>();
        supported.put(1, new Version("1.1"));
        supported.put(3, new Version("1.1"));
        if (isGateway) {
            supported.put(25, new Version("2.0")); // makes isGateway() return true
        }

        return new DeviceRegistration.Builder(regId, endpoint, peer, uri) //
                .lwM2mVersion(LwM2mVersion.V1_1) //
                .supportedObjects(supported) //
                .supportedContentFormats(ContentFormat.SENML_CBOR) //
                .build();
    }

    private EndDeviceRegistration newEndDevice(Registration parentGateway, String regId, String prefix,
            String endpoint) {
        return new EndDeviceRegistration.Builder(parentGateway, regId, prefix, endpoint) //
                .supportedContentFormats(ContentFormat.SENML_CBOR) //
                .build();
    }

    private SingleObservation singleObservation(String registrationId, String path) {
        // Adjust constructor to your SingleObservation signature; this is the typical (id, regId, path, format,
        // attributes, context) shape but exact arguments vary across Leshan versions.
        return new SingleObservation( //
                new ObservationIdentifier(new EndpointUri("coap", "localhost", 5683),
                        ("token-" + registrationId + "-" + path).getBytes()), //
                registrationId, //
                new LwM2mPath(path), //
                ContentFormat.SENML_CBOR, //
                null, //
                Collections.emptyMap());
    }
}
