/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.OperationContextType;
import org.hibernate.event.spi.LockEvent;

/**
 * @author Gail Badner
 */
public class LockOperationContext extends AbstractEventOperationContext {

	public LockOptions getLockOptions() {
		checkValid();
		return ( (LockEvent) getEvent() ).getLockOptions();
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.LOCK;
	}

	@Override
	public void afterOperation() {
		// do nothing
	}
}
