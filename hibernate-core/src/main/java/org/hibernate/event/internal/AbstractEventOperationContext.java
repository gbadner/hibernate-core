/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventOperationContext;
import org.hibernate.event.spi.EventSource;

/**
 * @author Gail Badner
 */
public abstract class AbstractEventOperationContext<T extends AbstractEvent> implements EventOperationContext {
	private T event;
	private Class<T> eventClass;

	protected AbstractEventOperationContext(Class<T> eventClass) {
		this.eventClass = eventClass;
	}

	public void beforeOperation( T event) {
		if ( isValid() ) {
			throw new IllegalStateException(
					String.format(
							"OperationContext [%s] is already in progress; cannot initiate operation.",
							getOperationContextType()
					)
			);
		}
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null" );
		}
		this.event = event;
	}

	public void afterOperation( T event) {
		if ( this.event != event ) {
			throw new IllegalStateException(
					String.format( "Inconsistent event for OperationContext [%s]", getOperationContextType() )
			);
		}

		if ( !isValid() ) {
			throw new IllegalStateException(
					String.format(
							"OperationContext [%s] is not valid.",
							getOperationContextType()
					)
			);
		}
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null" );
		}
		this.event = event;
	}


	public final T getEvent() {
		checkValid();
		return event;
	}

	public final Class<T> getEventClass() {
		return eventClass;
	}

	public void clear() {
		event = null;
	}

	public boolean isValid() {
		return event != null;
	}

	protected void checkValid() {
		if ( !isValid() ) {
			throw new IllegalStateException(
					String.format( "OperationContext [%s] is in an invalid state", getOperationContextType() )
			);
		}
	}

	protected EventSource getSession() {
		checkValid();
		return event.getSession();
	}
}
