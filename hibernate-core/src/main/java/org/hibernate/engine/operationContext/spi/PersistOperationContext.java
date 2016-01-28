/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

/**
 * An {@link OperationContext} of type {@link OperationContextType#PERSIST}
 * for entity persist operations.
 *
 * @author Gail Badner
 */
public interface PersistOperationContext extends OperationContext {
	/**
	 * Add an entity to a cache of entities that have
	 * have already been visited (to avoid infinite recursion).
	 * <p/>
	 * It is only valid to call this method if the persist operation is currently
	 * in progress (i.e., when {@link #isInProgress()} returns true).
	 *
	 * @param entity
	 * @return true, if the entity was added to the cache (because
	 * the cache did not already contain it); false, otherwise.
	 * @throws IllegalStateException if the persist operation is not currently
	 * in progress (i.e., when {@link #isInProgress()} returns false).
	 */
	boolean addEntity(Object entity);
}
