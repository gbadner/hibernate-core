/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.engine.internal.EventSourceProvider;
import org.hibernate.engine.spi.AbstractOperationContext;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventOperationContext;
import org.hibernate.event.spi.EventType;

/**
 * @author Gail Badner
 */
public abstract class AbstractEventOperationContext extends AbstractOperationContext implements EventOperationContext {
	private final int initialCascadeLevel;
	private EventType eventType;
	private AbstractEvent event;

	@Override
	public EventType getEventType() {
		return eventType;
	}

	AbstractEventOperationContext(EventSourceProvider eventSourceProvider, int requiredCascadeLevel) {
		super( eventSourceProvider );
		this.initialCascadeLevel = getCascadeLevel( eventSourceProvider.getSession() );
//		if ( requiredCascadeLevel >= 0 && initialCascadeLevel != requiredCascadeLevel ) {
//			throw new HibernateException(
//					String.format(
//							"Cannot initiate operation [%s] while cascading; cascade level is [%d]; cascade level must be 0",
//							eventType,
//							initialCascadeLevel
//					)
//			);
//		}
	}

	public void beforeOperation(EventType eventType, AbstractEvent event) {
		this.eventType = eventType;
		this.event = event;
	}

	public AbstractEvent getEvent() {
		return event;
	}

	@Override
	public void afterOperation() {
//		if ( getCascadeLevel( getSession() ) != initialCascadeLevel ) {
//			throw new IllegalStateException(
//					String.format(
//							"Cascade level is not %d after completing [%s] operation; it is %d",
//							initialCascadeLevel,
//							getEventType().eventName(),
//							getSession().getPersistenceContext().getCascadeLevel()
//					)
//			);
//		}
	}

	public void clear() {
		eventType = null;
		event = null;
	}

	public boolean isValid() {
		return eventType != null && event != null;
	}
}
