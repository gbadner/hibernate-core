/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import org.hibernate.engine.operationContext.spi.OperationContext;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventSource;

/**
 * An abstract implementation for EventOperationContextImplementor to be
 * used as a base class for an {@link OperationContext} for an event.
 *
 * @author Gail Badner
 */
public abstract class AbstractEventOperationContextImpl<T extends AbstractEvent>
		implements EventOperationContextImplementor<T> {
	private T event;
	private final Class<T> eventClass;

	protected AbstractEventOperationContextImpl(Class<T> eventClass) {
		if ( eventClass == null ) {
			throw new IllegalArgumentException( "eventClass must be non-null" );
		}
		this.eventClass = eventClass;
	}

	@Override
	public final void beforeOperation(T event) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null" );
		}
		if ( isInProgress() ) {
			throw new IllegalStateException(
					String.format(
							"OperationContext [%s] is already in progress.",
							getOperationContextType()
					)
			);
		}
		// this.event must be set before calling doBeforeOperationChecks()
		// so it is available when implementations call #getEvent().
		this.event = event;
		doBeforeOperation();
	}

	/**
	 * Implementations can override this method to do operation-specific
	 * setup and/or to perform integrity checks before an operation is
	 * performed.
	 */
	protected void doBeforeOperation() {
	}

	@Override
	public final void afterOperation(T event, boolean success) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null" );
		}
		checkIsValid();
		if ( this.event != event ) {
			throw new IllegalStateException(
					String.format( "Inconsistent event for OperationContext [%s]", getOperationContextType() )
			);
		}
		try {
			if ( success ) {
				doAfterSuccessfulOperation();
			}
		}
		finally {
			clear();
		}
	}

	/**
	 * Implementations can override this method to do operation-specific
	 * work and/or to perform integrity checks after an operation is
	 * performed.
	 */
	protected void doAfterSuccessfulOperation() {
	}

	protected final T getEvent() {
		checkIsValid();
		return event;
	}

	@Override
	public final Class<T> getEventClass() {
		return eventClass;
	}

	@Override
	public void clear() {
		event = null;
	}

	@Override
	public boolean isInProgress() {
		return event != null;
	}

	protected void checkIsValid() {
		if ( !isInProgress() ) {
			throw new IllegalStateException(
					String.format( "OperationContext [%s] is in an invalid state", getOperationContextType() )
			);
		}
	}

	protected EventSource getSession() {
		checkIsValid();
		return event.getSession();
	}
}
