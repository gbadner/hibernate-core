/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;


/**
 * SaveOrUpdateOperationContext is an {@link OperationContext} of type
 * {@link OperationContextType#SAVE_OR_UPDATE} used to cache data for
 * entity save-or-update and cascading the save-or-update operation.
 * The method in this interface is available only when a
 * save-or-update operation is in progress.
 * <p/>
 * To determine if a save-or-update operation is in progress use this method:
 * {@link org.hibernate.engine.spi.SessionImplementor#isOperationInProgress(OperationContextType)}.
 * {@code SessionImplementor#isOperationInProgress(OperationContextType.SAVE_UPDATE)}
 * will return true if a save-or-update operation is in progress.
 *
 * @see org.hibernate.event.spi.EventSource#saveOrUpdate(Object)
 * @see org.hibernate.engine.spi.SessionImplementor#isOperationInProgress(OperationContextType)
 * @see org.hibernate.engine.spi.SessionImplementor#getOperationContext(OperationContextType)

 * @author Gail Badner
 */
public interface SaveOrUpdateOperationContext extends OperationContext {
	/**
	 * Add an entity to a cache of entities that have
	 * have already been visited (to avoid infinite recursion).
	 *
	 * @param entity
	 * @return true, if the entity was added to the cache (because
	 * the cache did not already contain it); false, otherwise.
	 * @throws IllegalStateException if the save-or-update operation is not
	 * currently in progress.
	 */
	boolean addEntity(Object entity);
}
