/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.engine.internal.EventSourceProvider;
import org.hibernate.engine.spi.AbstractOperationContext;
import org.hibernate.event.spi.EventOperationContext;
import org.hibernate.event.spi.EventType;

/**
 * @author Gail Badner
 */
public abstract class AbstractEventOperationContext extends AbstractOperationContext implements EventOperationContext {
	private final EventType eventType;
	private final int initialCascadeLevel;

	@Override
	public EventType getEventType() {
		return eventType;
	}

	AbstractEventOperationContext(
			EventSourceProvider eventSourceProvider,
			EventType eventType,
			int requiredCascadeLevel) {
		super( eventSourceProvider );
		this.eventType = eventType;
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

	@Override
	public void afterOperation() {
		if ( getCascadeLevel( getSession() ) != initialCascadeLevel ) {
			throw new IllegalStateException(
					String.format(
							"Cascade level is not %d after completing merge operation; it is %d",
							initialCascadeLevel,
							getSession().getPersistenceContext().getCascadeLevel()
					)
			);
		}
	}
}
