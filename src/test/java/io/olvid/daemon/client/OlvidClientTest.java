package io.olvid.daemon.client;

import io.olvid.daemon.command.v1.*;
import org.junit.jupiter.api.*;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OlvidClientTest {
    OlvidClient client;

    @BeforeAll
    void setUp() {
        client = OlvidClient.newBuilder().build();
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
    @DisplayName("AuthenticationTest: client key is valid")
    void authenticationTest() {
        client.stubs.toolCommand.authenticationTest(AuthenticationTestRequest.newBuilder().build());
    }

    @Test
    @DisplayName("DaemonVersion: returns a non-blank version string")
    void daemonVersion() {
        DaemonVersionResponse response = client.stubs.toolCommand.daemonVersion(DaemonVersionRequest.newBuilder().build());
        assertFalse(response.getVersion().isBlank());
    }

    @Test
    @DisplayName("IdentityGet: identity has a valid id and display name")
    void identityGet() {
        IdentityGetResponse response = client.stubs.identityCommand.identityGet(IdentityGetRequest.newBuilder().build());
        assertNotEquals(0, response.getIdentity().getId());
        assertFalse(response.getIdentity().getDisplayName().isBlank());
    }

    @Test
    @DisplayName("IdentityGetBytesIdentifier: returns non-empty identifier bytes")
    void identityGetBytesIdentifier() {
        IdentityGetBytesIdentifierResponse response = client.stubs.identityCommand.identityGetBytesIdentifier(IdentityGetBytesIdentifierRequest.newBuilder().build());
        assertFalse(response.getIdentifier().isEmpty());
    }

    @Test
    @DisplayName("IdentityGetInvitationLink: returns a non-blank invitation link")
    void identityGetInvitationLink() {
        IdentityGetInvitationLinkResponse response = client.stubs.identityCommand.identityGetInvitationLink(IdentityGetInvitationLinkRequest.newBuilder().build());
        assertFalse(response.getInvitationLink().isBlank());
    }

    @Test
    @DisplayName("ContactList: iterates without error")
    void contactList() {
        var iterator = client.stubs.contactCommand.contactList(ContactListRequest.newBuilder().build());
        while (iterator.hasNext()) {
            assertNotNull(iterator.next());
        }
    }

    @Test
    @DisplayName("DiscussionList: iterates without error")
    void discussionList() {
        var iterator = client.stubs.discussionCommand.discussionList(DiscussionListRequest.newBuilder().build());
        while (iterator.hasNext()) {
            assertNotNull(iterator.next());
        }
    }
}
