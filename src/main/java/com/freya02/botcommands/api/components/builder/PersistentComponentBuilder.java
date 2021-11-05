package com.freya02.botcommands.api.components.builder;

import java.util.concurrent.TimeUnit;

public interface PersistentComponentBuilder<T extends PersistentComponentBuilder<T>> {
	String getHandlerName();

	String[] getArgs();

	/**
	 * Makes this component expire after the specified timeout<br>
	 * Once the component expires it should be removed from the component manager
	 *
	 * @return This component builder for chaining purposes
	 */
	T timeout(long timeout, TimeUnit timeoutUnit);

	PersistentComponentTimeoutInfo getTimeout();
}
