/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

import org.hibernate.engine.operationContext.spi.OperationContext;

/**
 * RefreshOperationContext is an {@link OperationContext} of type
 * {@link OperationContextType#REFRESH} used to cache data for
 * refreshing an entity and cascading the refresh operation.
 * The method in this interface is available only when a
 * refresh operation is in progress.
 * <p/>
 * To determine if a refresh operation is in progress use this method:
 * {@link org.hibernate.engine.spi.SessionImplementor#isOperationInProgress(OperationContextType)}.
 * {@code SessionImplementor#isOperationInProgress(OperationContextType.REFRESH)}
 * will return true if a refresh operation is in progress.
 *
 * @author Gail Badner
 */
public interface RefreshOperationContext extends OperationContext {
	/**
	 * Add an entity to a cache of entities that have
	 * have already been visited (to avoid infinite recursion).
	 * <p/>
	 * It is only valid to call this method if the refresh operation is currently
	 * in progress (i.e., when {@link #isInProgress()} returns true).
	 *
	 * @param entity
	 * @return true, if the refreshed entity was added to the cache (because
	 * the cache did not already contain it); false, otherwise.
	 * @throws IllegalStateException if the refresh operation is not currently
	 * in progress.
	 */
	boolean addEntity(Object entity);
}
