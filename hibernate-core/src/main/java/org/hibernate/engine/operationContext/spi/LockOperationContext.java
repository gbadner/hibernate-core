/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

import org.hibernate.LockOptions;

/**
 * An {@link OperationContext} of type {@link OperationContextType#LOCK}
 * for entity lock operations.
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
	 * in progress (i.e., when {@link #isInProgress()} returns false).
	 */
	LockOptions getLockOptions();
}
