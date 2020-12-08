package org.eclipse.leshan.client.observer;

/**
 * An extended interface of {@link LwM2mClientObserver} with handler for unexpected error.
 * <p>
 * Future plan: Since version 2.0, this interface is going to merge into {@link LwM2mClientObserver}.
 *
 * @since 1.3
 */
public interface LwM2mClientObserver2 extends LwM2mClientObserver {
	// ============== Unexpected Error Handling =================

	void onUnexpectedError(Throwable unexpectedError);
}
