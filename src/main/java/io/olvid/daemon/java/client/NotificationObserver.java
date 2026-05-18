package io.olvid.daemon.java.client;

import io.grpc.stub.StreamObserver;

public abstract class NotificationObserver<V> implements StreamObserver<V> {
	@Override
	public void onError(Throwable throwable) {}

	@Override
	public void onCompleted() {}
}
