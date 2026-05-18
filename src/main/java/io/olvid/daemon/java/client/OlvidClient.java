package io.olvid.daemon.java.client;

import io.grpc.*;
import io.olvid.daemon.services.v1.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

public class OlvidClient {
	private static final Logger LOGGER = Logger.getLogger(OlvidClient.class.getName());

	public final String clientKey;
	public final String daemonUrl;
	public final ChannelCredentials channelCredentials;
	public final ManagedChannel channel;
	public final StubHolder stubs;

	@SuppressWarnings("unused")
	public static class Builder {
		private String daemonUrl = null;
		private String clientKey = null;
		private ChannelCredentials channelCredentials = null;

		public Builder setDaemonUrl(String daemonUrl) {
			this.daemonUrl = daemonUrl;
			return this;
		}

		public Builder setClientKey(String clientKey) {
			this.clientKey = clientKey;
			return this;
		}

		public Builder setChannelCredentials(ChannelCredentials channelCredentials) {
			this.channelCredentials = channelCredentials;
			return this;
		}

		public OlvidClient build() {
			return new OlvidClient(daemonUrl, clientKey, channelCredentials);
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	private OlvidClient(String daemonUrl, String clientKey, ChannelCredentials channelCredentials) {
        // load .env file if it exists
		var dotEnv = new Properties();
		try {
			var envFile = Paths.get(".env");
			try (var inputStream = Files.newInputStream(envFile)) {
				dotEnv.load(inputStream);
			}
		} catch (IOException ignored) {}

		// determine the client key to use (argument > env > file)
		if (clientKey != null) {
			this.clientKey = clientKey;
			LOGGER.info("Used argument client key");
		} else if (System.getenv("OLVID_CLIENT_KEY") != null && !System.getenv("OLVID_CLIENT_KEY").isEmpty()) {
			this.clientKey = System.getenv("OLVID_CLIENT_KEY");
			LOGGER.info("Used environment client key");
		}
		else if (dotEnv.containsKey("OLVID_CLIENT_KEY")) {
			this.clientKey = dotEnv.getProperty("OLVID_CLIENT_KEY");
			LOGGER.info("Used .env client key");
		} else {
			throw new RuntimeException("No client key provided");
		}

		// determine daemonUrl
		if (daemonUrl != null) {
			LOGGER.info("Used argument daemon url");
		} else if (System.getenv("OLVID_DAEMON_URl") != null && !System.getenv("OLVID_DAEMON_URl").isEmpty()) {
			daemonUrl = System.getenv().get("OLVID_DAEMON_URl");
			LOGGER.info("Used environment daemon url");
		} else if (dotEnv.containsKey("OLVID_DAEMON_URl")) {
			daemonUrl =  dotEnv.getProperty("OLVID_DAEMON_URl");
			LOGGER.info("Used .env daemon url");
		} else {
			daemonUrl = "127.0.0.1:50051";
			LOGGER.info("Used default daemon url");
		}

		// handle grpc channel credentials
		if (channelCredentials != null) {
			this.channelCredentials = channelCredentials;
			LOGGER.info("Used argument credentials");
		}
		// create default credentials
		else {
			if (daemonUrl.startsWith("https://")) {
				this.channelCredentials = TlsChannelCredentials.create();
				LOGGER.info("Used default TLS credentials");
			} else {
				this.channelCredentials = InsecureChannelCredentials.create();
				LOGGER.info("Used default insecure credentials");
			}
		}

		// remove http:// or https:// prefix in daemonUrl
		this.daemonUrl = daemonUrl.replaceFirst("^https://", "").replaceFirst("^http://", "");

		this.channel = Grpc.newChannelBuilder(this.daemonUrl, this.channelCredentials)
				.intercept(new AuthenticationInterceptor())
				.build();
		this.stubs = new StubHolder(this.channel);
	}

	public static class StubHolder {
		public final IdentityCommandServiceGrpc.IdentityCommandServiceBlockingStub identityCommand;
		public final InvitationCommandServiceGrpc.InvitationCommandServiceBlockingStub invitationCommand;
		public final ContactCommandServiceGrpc.ContactCommandServiceBlockingStub contactCommand;
		public final KeycloakCommandServiceGrpc.KeycloakCommandServiceBlockingStub keycloakCommand;
		public final GroupCommandServiceGrpc.GroupCommandServiceBlockingStub groupCommand;
		public final DiscussionCommandServiceGrpc.DiscussionCommandServiceBlockingStub discussionCommand;
		public final MessageCommandServiceGrpc.MessageCommandServiceBlockingStub messageCommand;
		public final AttachmentCommandServiceGrpc.AttachmentCommandServiceBlockingStub attachmentCommand;
		public final StorageCommandServiceGrpc.StorageCommandServiceBlockingStub storageCommand;
		public final DiscussionStorageCommandServiceGrpc.DiscussionStorageCommandServiceBlockingStub discussionStorageCommand;
		public final ToolCommandServiceGrpc.ToolCommandServiceBlockingStub toolCommand;

		public final InvitationNotificationServiceGrpc.InvitationNotificationServiceStub invitationNotification;
		public final ContactNotificationServiceGrpc.ContactNotificationServiceStub contactNotification;
		public final GroupNotificationServiceGrpc.GroupNotificationServiceStub groupNotification;
		public final DiscussionNotificationServiceGrpc.DiscussionNotificationServiceStub discussionNotification;
		public final MessageNotificationServiceGrpc.MessageNotificationServiceStub messageNotification;
		public final AttachmentNotificationServiceGrpc.AttachmentNotificationServiceStub attachmentNotification;

		StubHolder(ManagedChannel channel) {
			identityCommand = IdentityCommandServiceGrpc.newBlockingStub(channel);
			invitationCommand = InvitationCommandServiceGrpc.newBlockingStub(channel);
			contactCommand = ContactCommandServiceGrpc.newBlockingStub(channel);
			keycloakCommand = KeycloakCommandServiceGrpc.newBlockingStub(channel);
			groupCommand = GroupCommandServiceGrpc.newBlockingStub(channel);
			discussionCommand = DiscussionCommandServiceGrpc.newBlockingStub(channel);
			messageCommand = MessageCommandServiceGrpc.newBlockingStub(channel);
			attachmentCommand = AttachmentCommandServiceGrpc.newBlockingStub(channel);
			storageCommand = StorageCommandServiceGrpc.newBlockingStub(channel);
			discussionStorageCommand = DiscussionStorageCommandServiceGrpc.newBlockingStub(channel);
			toolCommand = ToolCommandServiceGrpc.newBlockingStub(channel);

			invitationNotification = InvitationNotificationServiceGrpc.newStub(channel);
			contactNotification = ContactNotificationServiceGrpc.newStub(channel);
			groupNotification = GroupNotificationServiceGrpc.newStub(channel);
			discussionNotification = DiscussionNotificationServiceGrpc.newStub(channel);
			messageNotification = MessageNotificationServiceGrpc.newStub(channel);
			attachmentNotification = AttachmentNotificationServiceGrpc.newStub(channel);
		}
	}

	class AuthenticationInterceptor implements io.grpc.ClientInterceptor {
		static final Metadata.Key<String> CLIENT_KEY_METADATA_KEY = Metadata.Key.of("daemon-client-key", Metadata.ASCII_STRING_MARSHALLER);

		@Override
		public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel channel) {
			return new ForwardingClientCall.SimpleForwardingClientCall<>(channel.newCall(method, callOptions)) {
				@Override
				public void start(Listener<RespT> responseListener, Metadata headers) {
					headers.put(CLIENT_KEY_METADATA_KEY, clientKey);
					super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {}, headers);
				}
			};
		}
	}
}
