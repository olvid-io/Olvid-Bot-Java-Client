package io.olvid.daemon.client;

import io.grpc.stub.StreamObserver;

public abstract class NotificationObserver<V> implements StreamObserver<V> {
	@Override
	public void onError(Throwable throwable) {}

	@Override
	public void onCompleted() {}
}
