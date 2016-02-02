/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

import org.hibernate.LockOptions;
import org.hibernate.event.spi.LockEvent;

/**
 * LockOperationContext is an {@link OperationContext} of type
 * {@link OperationContextType#LOCK} used to cache data for
 * the locking an entity and cascading the lock operation.
 * The method in this interface is available only when a
 * lock operation is in progress.
 * <p/>
 * To determine if a merge operation is in progress use this method:
 * {@link org.hibernate.engine.spi.SessionImplementor#isOperationInProgress(OperationContextType)}.
 * {@code SessionImplementor#isOperationInProgress(OperationContextType.LOCK)}
 * will return true if a lock operation is in progress.
 *
 * @author Gail Badner
 */
public interface LockOperationContext extends OperationContext {
	/**
	 * Returns the {@link LockOptions} for an entity lock operation
	 * that is in progress.
	 * <p/>
	 * It is only valid to call this method if the lock operation is currently
	 * in progress (i.e., when {@link #isInProgress()} returns true).
	 *
	 * @return the lock options.
	 *
	 * @throws IllegalStateException if the lock operation is not currently
	 * in progress.
	 */
	LockOptions getLockOptions();
}
