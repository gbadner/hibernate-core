/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

import java.util.Collections;
import java.util.Map;

/**
 * @author Gail Badner
 */
public interface MergeOperationContext extends OperationContext {

	/**
	 * Returns true if this MergeContext contains a cross-reference for the specified merge entity
	 * to a managed entity result.
	 *
	 * @param mergeEntity must be non-null
	 * @return true if this MergeContext contains a cross-reference for the specified merge entity
	 * @throws NullPointerException if mergeEntity is null
	 */
	boolean containsMergeEntity(Object mergeEntity);

	/**
	 * Returns true if this MergeContext contains a cross-reference from the specified managed entity
	 * to a merge entity.
	 * @param managedEntity must be non-null
	 * @return true if this MergeContext contains a cross-reference from the specified managed entity
	 * to a merge entity
	 * @throws NullPointerException if managedEntity is null
	 */
	boolean containsValue(Object managedEntity);

	/**
	 * Returns the managed entity associated with the specified merge Entity.
	 * @param mergeEntity the merge entity; must be non-null
	 * @return  the managed entity associated with the specified merge Entity
	 * @throws NullPointerException if mergeEntity is null
	 */
	Object get(Object mergeEntity);

	/**
	 * Associates the specified merge entity with the specified managed entity result in this MergeContext.
	 * If this MergeContext already contains a cross-reference for <code>mergeEntity</code> when this
	 * method is called, then <code>managedEntity</code> must be the same as what is already associated
	 * with <code>mergeEntity</code>.
	 * <p/>
	 * This method assumes that the merge process is not yet operating on <code>mergeEntity</code>.
	 * Later when <code>mergeEntity</code> enters the merge process, {@link #setOperatedOn(Object, boolean)}
	 * should be called.
	 * <p/>
	 * @param mergeEntity the merge entity; must be non-null
	 * @param managedEntity the managed entity result; must be non-null
	 * @return previous managed entity associated with specified merge entity, or null if
	 * there was no mapping for mergeEntity.
	 * @throws NullPointerException if mergeEntity or managedEntity is null
	 * @throws IllegalArgumentException if <code>managedEntity</code> is not the same as the previous
	 * managed entity associated with <code>merge entity</code>
	 * @throws IllegalStateException if internal cross-references are out of sync,
	 */
	Object put(Object mergeEntity, Object managedEntity);

	/**
	 * Associates the specified merge entity with the specified managed entity in this MergeContext.
	 * If this MergeContext already contains a cross-reference for <code>mergeEntity</code> when this
	 * method is called, then <code>managedEntity</code> must be the same as what is already associated
	 * with <code>mergeEntity</code>.
	 *
	 * @param mergeEntity the mergge entity; must be non-null
	 * @param managedEntity the managed entity; must be non-null
	 * @param isOperatedOn indicates if the merge operation is performed on the mergeEntity.
	 *
	 * @return previous managed entity associated with specified merge entity, or null if
	 * there was no mapping for mergeEntity.
	 * @throws NullPointerException if mergeEntity or managedEntity is null
	 * @throws IllegalArgumentException if <code>managedEntity</code> is not the same as the previous
	 * managed entity associated with <code>mergeEntity</code>
	 * @throws IllegalStateException if internal cross-references are out of sync,
	 */
	public Object put(Object mergeEntity, Object managedEntity, boolean isOperatedOn);

	/**
	 * Returns true if the listener is performing the merge operation on the specified merge entity.
	 * @param mergeEntity the merge entity; must be non-null
	 * @return true if the listener is performing the merge operation on the specified merge entity;
	 * false, if there is no mapping for mergeEntity.
	 * @throws NullPointerException if mergeEntity is null
	 */
	boolean isOperatedOn(Object mergeEntity);

	/**
	 * Set flag to indicate if the listener is performing the merge operation on the specified merge entity.
	 * @param mergeEntity must be non-null and this MergeContext must contain a cross-reference for mergeEntity
	 *                       to a managed entity
	 * @throws NullPointerException if mergeEntity is null
	 * @throws IllegalStateException if this MergeContext does not contain a a cross-reference for mergeEntity
	 */
	void setOperatedOn(Object mergeEntity, boolean isOperatedOn);

	/**
	 * Returns an unmodifiable map view of the managed-to-merge entity
	 * cross-references.
	 *
	 * The returned Map will contain a cross-reference from each managed entity
	 * to the most recently associated merge entity that was most recently put in the MergeContext.
	 *
	 * @return an unmodifiable map view of the managed-to-merge entity cross-references.
	 *
	 * @see {@link Collections#unmodifiableMap(java.util.Map)}
	 */
	Map invertMap();
}
