package io.olvid.daemon.java.client;

import io.olvid.daemon.admin.v1.*;
import io.olvid.daemon.command.v1.*;
import io.olvid.daemon.datatypes.v1.Identity;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OlvidAdminClientTest {
    OlvidAdminClient client;

    @BeforeAll
    void setUp() {
        client = OlvidAdminClient.newBuilder().build();
    }

    @AfterAll
    void tearDown() throws InterruptedException {
        client.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("Ping: daemon is reachable")
    void testConnection() {
        client.stubs.toolCommand.ping(PingRequest.newBuilder().build());
    }

    @Test
    @DisplayName("AuthenticationAdminTest: admin client key is valid")
    void authenticationTest() {
        client.stubs.toolCommand.authenticationAdminTest(AuthenticationAdminTestRequest.newBuilder().build());
    }

    @Test
    @DisplayName("IdentityList: returns at least one identity")
    void identityList() {
        List<Identity> identities = new ArrayList<>();
        var iterator = client.stubs.adminIdentityCommand.identityList(IdentityListRequest.newBuilder().build());
        while (iterator.hasNext()) {
            identities.addAll(iterator.next().getIdentitiesList());
        }
        assertFalse(identities.isEmpty());
    }

    @Test
    @DisplayName("IdentityAdminGet: fetched identity matches queried id")
    void identityAdminGet() {
        var iterator = client.stubs.adminIdentityCommand.identityList(IdentityListRequest.newBuilder().build());
        assertTrue(iterator.hasNext(), "At least one identity must exist");
        List<Identity> first = iterator.next().getIdentitiesList();
        assertFalse(first.isEmpty(), "First response must contain at least one identity");
        long identityId = first.get(0).getId();

        IdentityAdminGetResponse response = client.stubs.adminIdentityCommand.identityAdminGet(
            IdentityAdminGetRequest.newBuilder().setIdentityId(identityId).build()
        );
        assertEquals(identityId, response.getIdentity().getId());
    }

    @Test
    @DisplayName("ClientKeyList: at least the current admin key is listed")
    void clientKeyList() {
        int count = 0;
        var iterator = client.stubs.adminClientKeyCommand.clientKeyList(ClientKeyListRequest.newBuilder().build());
        while (iterator.hasNext()) {
            count += iterator.next().getClientKeysCount();
        }
        assertTrue(count > 0);
    }
}
