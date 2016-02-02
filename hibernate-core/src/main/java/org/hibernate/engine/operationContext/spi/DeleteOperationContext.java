/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

import org.hibernate.event.spi.DeleteEvent;

/**
 * DeleteOperationContext is an {@link OperationContext} of type
 * {@link OperationContextType#DELETE} used to cache data while deleting
 * and entity and cascading the delete operation. The methods in this
 * interface are available only when a delete operation is in progress.
 * <p/>
 * To determine if a delete operation is in progress use this method:
 * {@link org.hibernate.engine.spi.SessionImplementor#isOperationInProgress(OperationContextType)}.
 * {@code SessionImplementor#isOperationInProgress(OperationContextType.DELETE)}
 * will return true if a delete operation is in progress.
 * <p/>
 *
 * @see org.hibernate.event.spi.EventSource#delete(Object)
 *
 * @author Gail Badner
 */
public interface DeleteOperationContext extends OperationContext {
	/**
	 * Add a transient entity to a cache of transient entities that have
	 * have already been visited (to avoid infinite recursion).
	 * <p/>
	 * It is only valid to call this method if the delete operation is currently
	 * in progress (i.e., when {@link #isInProgress()} returns true).
	 *
	 * @param transientEntity
	 * @return true, if the transient entity was added to the cache (because
	 * the cache did not already contain it); false, otherwise.
	 * @throws IllegalStateException if a delete operation is not currently
	 * in progress.
	 */
	boolean addTransientEntity(Object transientEntity);
}
