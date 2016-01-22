package org.hibernate.engine.spi;

import org.hibernate.engine.internal.EventSourceProvider;
import org.hibernate.event.spi.EventSource;

/**
 * @author Gail Badner
 */
public abstract class AbstractOperationContext implements OperationContext {
	private final EventSourceProvider eventSourceProvider;

	protected AbstractOperationContext(EventSourceProvider eventSourceProvider) {
		this.eventSourceProvider = eventSourceProvider;
	}

	@Override
	public void afterOperation() {
	}

	@Override
	public void clear() {
	}

	protected EventSource getSession() {
		return eventSourceProvider.getSession();
	}

	protected static int getCascadeLevel(EventSource session) {
		return session.getPersistenceContext().getCascadeLevel();
	}
}
