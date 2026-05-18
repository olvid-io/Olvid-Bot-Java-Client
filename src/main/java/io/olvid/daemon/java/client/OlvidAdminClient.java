package io.olvid.daemon.java.client;

import io.grpc.*;
import io.olvid.daemon.services.v1.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

public class OlvidAdminClient {
	private static final Logger LOGGER = Logger.getLogger(OlvidAdminClient.class.getName());

	public final String clientKey;
	public final String daemonUrl;
	public final long identityId;
	public final ChannelCredentials channelCredentials;
	public final ManagedChannel channel;
	public final AdminStubHolder stubs;

	@SuppressWarnings("unused")
	public static class Builder {
		private String daemonUrl = null;
		private String clientKey = null;
		private Long identityId = null;
		private ChannelCredentials channelCredentials;

		public Builder setDaemonUrl(String daemonUrl) {
			this.daemonUrl = daemonUrl;
			return this;
		}

		public Builder setClientKey(String clientKey) {
			this.clientKey = clientKey;
			return this;
		}

		public Builder setIdentityId(long identityId) {
			this.identityId = identityId;
			return this;
		}

		public Builder setChannelCredentials(ChannelCredentials channelCredentials) {
			this.channelCredentials = channelCredentials;
			return this;
		}

		public OlvidAdminClient build() {
			return new OlvidAdminClient(daemonUrl, clientKey, identityId, channelCredentials);
		}
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	private OlvidAdminClient(String daemonUrl, String clientKey, Long identityId, ChannelCredentials channelCredentials) {
		// load .env file if it exists
		var dotEnv = new Properties();
		try {
			var envFile = Paths.get(".env");
			try (var inputStream = Files.newInputStream(envFile)) {
				dotEnv.load(inputStream);
			}
		} catch (IOException ignored) {}

		this.identityId = Objects.requireNonNullElse(identityId, 0L);

		// determine the client key to use (argument > env > file)
		if (clientKey != null) {
			this.clientKey = clientKey;
			LOGGER.info("Used argument client key");
		} else if (System.getenv("OLVID_ADMIN_CLIENT_KEY") != null && !System.getenv("OLVID_ADMIN_CLIENT_KEY").isEmpty()) {
			this.clientKey = System.getenv("OLVID_ADMIN_CLIENT_KEY");
			LOGGER.info("Used environment client key");
		}
		else if (dotEnv.containsKey("OLVID_ADMIN_CLIENT_KEY")) {
			this.clientKey = dotEnv.getProperty("OLVID_ADMIN_CLIENT_KEY");
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
				.intercept(new AdminAuthenticationInterceptor())
				.build();
		this.stubs = new AdminStubHolder(this.channel);
	}

	public static class AdminStubHolder extends OlvidClient.StubHolder {
		public final IdentityAdminServiceGrpc.IdentityAdminServiceBlockingStub adminIdentityCommand;
		public final ClientKeyAdminServiceGrpc.ClientKeyAdminServiceBlockingStub adminClientKeyCommand;
		public final BackupAdminServiceGrpc.BackupAdminServiceBlockingStub adminBackupCommand;

		AdminStubHolder(ManagedChannel channel) {
			super(channel);

			// add admin stubs to classic stubs
			adminIdentityCommand = IdentityAdminServiceGrpc.newBlockingStub(channel);
			adminClientKeyCommand = ClientKeyAdminServiceGrpc.newBlockingStub(channel);
			adminBackupCommand = BackupAdminServiceGrpc.newBlockingStub(channel);
		}
	}

	public class AdminAuthenticationInterceptor implements io.grpc.ClientInterceptor {
		static final Metadata.Key<String> CLIENT_KEY_METADATA_KEY = Metadata.Key.of("daemon-client-key", Metadata.ASCII_STRING_MARSHALLER);
		static final Metadata.Key<String> IDENTITY_ID_METADATA_KEY = Metadata.Key.of("daemon-identity-id", Metadata.ASCII_STRING_MARSHALLER);

		@Override
		public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel channel) {
			return new ForwardingClientCall.SimpleForwardingClientCall<>(channel.newCall(method, callOptions)) {

				@Override
				public void start(Listener<RespT> responseListener, Metadata headers) {
					headers.put(CLIENT_KEY_METADATA_KEY, clientKey);
					headers.put(IDENTITY_ID_METADATA_KEY, Long.toString(identityId));
					super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {}, headers);
				}
			};
		}
	}
}
