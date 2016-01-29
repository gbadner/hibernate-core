/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

import java.util.Collection;

/**
 * An {@link OperationContext} of type {@link OperationContextType#MERGE}
 * for entity merge operations.
 *
 * @author Gail Badner
 */
public interface MergeOperationContext extends OperationContext {

	/**
	 * Returns the managed entity associated with the specified merge Entity.
	 * @param mergeEntity the merge entity; must be non-null
	 * @return  the managed entity associated with the specified merge Entity
	 * @throws NullPointerException if mergeEntity is null
	 */
	Object getEntityCopyFromMergeEntity(Object mergeEntity);

	Object getMergeEntityFromEntityCopy(Object entityCopy);

	/**
	 * Associates the specified merge entity with the specified managed entity in this MergeContext.
	 * If this MergeContext already contains a cross-reference for <code>mergeEntity</code> when this
	 * method is called, then <code>managedEntity</code> must be the same as what is already associated
	 * with <code>mergeEntity</code>.
	 *
	 *
	 * @return previous managed entity associated with specified merge entity, or null if
	 * there was no mapping for mergeEntity.
	 *
	 * @throws NullPointerException if mergeEntity or managedEntity is null
	 * @throws IllegalArgumentException if <code>managedEntity</code> is not the same as the previous
	 * managed entity associated with <code>mergeEntity</code>
	 * @throws IllegalStateException if internal cross-references are out of sync,
	 * @param mergeEntity
	 * @param entityCopy
	 */
	boolean addMergeData(Object mergeEntity, Object entityCopy);

	/**
	 * Associates the specified merge entity with the specified managed entity in this MergeContext.
	 * If this MergeContext already contains a cross-reference for <code>mergeEntity</code> when this
	 * method is called, then <code>managedEntity</code> must be the same as what is already associated
	 * with <code>mergeEntity</code>.
	 *
	 *
	 * @return previous managed entity associated with specified merge entity, or null if
	 * there was no mapping for mergeEntity.
	 * @throws NullPointerException if mergeEntity or managedEntity is null
	 * @throws IllegalArgumentException if <code>managedEntity</code> is not the same as the previous
	 * managed entity associated with <code>mergeEntity</code>
	 * @throws IllegalStateException if internal cross-references are out of sync,
	 * @param mergeEntity
	 * @param entityCopy
	 */
	void addTransientMergeDataPlaceholder(Object mergeEntity, Object entityCopy);

	/**
	 * Returns true if the listener is performing the merge operation on the specified merge entity.
	 */
	boolean isInMergeProcess(Object mergeEntity);

	/**
	 * Set flag to indicate that the listener is performing the merge operation on this {@link MergeData}.
	 * @throws NullPointerException if mergeEntity is null
	 * @throws IllegalStateException if this MergeContext does not contain a a cross-reference for mergeEntity
	 */
	void markTransientMergeDataInProcess(Object mergeEntity, Object entityCopy);

	Collection<MergeData> getAllMergeData();
}
