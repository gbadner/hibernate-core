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
import org.hibernate.event.spi.EventType;

/**
 * @author Gail Badner
 */
public class LockOperationContext extends AbstractEventOperationContext {
	private final LockOptions lockOptions;

	public LockOperationContext(EventSourceProvider eventSourceProvider, LockOptions lockOptions) {
		super( eventSourceProvider, EventType.LOCK, 0 );
		this.lockOptions = lockOptions;;
	}

	public LockOptions getLockOptions() {
		return lockOptions;
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.LOCK;
	}
}
