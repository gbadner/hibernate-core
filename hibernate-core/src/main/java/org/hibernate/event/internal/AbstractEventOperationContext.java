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
import org.hibernate.event.spi.EventType;

/**
 * @author Gail Badner
 */
public abstract class AbstractEventOperationContext implements EventOperationContext {
	private EventType eventType;
	private AbstractEvent event;

	@Override
	public EventType getEventType() {
		return eventType;
	}

	public void beforeOperation(EventType eventType, AbstractEvent event) {
		if ( isValid() ) {
			throw new IllegalStateException(
					String.format(
							"OperationContext [%s] is already in progress; cannot initiate operation.",
							getOperationContextType()
					)
			);
		}
		if ( eventType == null || event == null ) {
			throw new IllegalArgumentException( "eventType and event must be non-null" );
		}
		this.eventType = eventType;
		this.event = event;
	}

	public AbstractEvent getEvent() {
		checkValid();
		return event;
	}

	public void clear() {
		eventType = null;
		event = null;
	}

	public boolean isValid() {
		return eventType != null && event != null;
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
