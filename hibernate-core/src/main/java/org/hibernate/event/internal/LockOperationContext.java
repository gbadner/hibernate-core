/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.LockOptions;
import org.hibernate.engine.internal.EventSourceProvider;
import org.hibernate.engine.spi.OperationContext;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LockEvent;

/**
 * @author Gail Badner
 */
public class LockOperationContext extends AbstractEventOperationContext {

	public LockOperationContext(EventSourceProvider eventSourceProvider, LockEvent event) {
		super( eventSourceProvider, EventType.LOCK, event, 0 );
	}

	public LockOptions getLockOptions() {
		return ( (LockEvent) getEvent() ).getLockOptions();
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.LOCK;
	}

	@Override
	public void clear() {
	}
}
