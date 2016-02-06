/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

import org.hibernate.event.spi.PersistEvent;

/**
 * PersistOperationContext is an {@link OperationContext} of type
 * {@link OperationContextType#PERSIST} used to cache data for
 * persisting an entity and cascading the persist operation.
 * The method in this interface is available only when a
 * persist operation is in progress.
 * <p/>
 * To determine if a persist operation is in progress use this method:
 * {@link org.hibernate.engine.spi.SessionImplementor#isOperationInProgress(OperationContextType)}.
 * {@code SessionImplementor#isOperationInProgress(OperationContextType.PERSIST)}
 * will return true if a persist operation is in progress.
 *
 * @see org.hibernate.event.spi.EventSource#persist(Object)
 * @see org.hibernate.engine.spi.SessionImplementor#isOperationInProgress(OperationContextType)
 * @see org.hibernate.engine.spi.SessionImplementor#getOperationContext(OperationContextType)
 *
 * @author Gail Badner
 */
public interface PersistOperationContext extends OperationContext {
	/**
	 * Add an entity to a cache of entities that have
	 * have already been visited (to avoid infinite recursion).
	 *
	 * @param entity
	 * @return true, if the entity was added to the cache (because
	 * the cache did not already contain it); false, otherwise.
	 * @throws IllegalStateException if the persist operation is not currently
	 * in progress.
	 */
	boolean addEntity(Object entity);
}
