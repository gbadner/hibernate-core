/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

import java.util.Collection;

import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.MergeEvent;

/**
 * MergeOperationContext is an {@link OperationContext} of type
 * {@link OperationContextType#MERGE} used to cache data for
 * merging an entity and cascading the merge operation.
 * The methods in this interface are available only when a
 * merge operation is in progress.
 * <p/>
 * To determine if a merge operation is in progress use this method:
 * {@link org.hibernate.engine.spi.SessionImplementor#isOperationInProgress(OperationContextType)}.
 * {@code SessionImplementor#isOperationInProgress(OperationContextType.MERGE)}
 * will return true if a merge operation is in progress.
 * <p/>
 * MergeOperationContext is intended to be used to track each entity being
 * merged (called a "merge entity") and its corresponding "entity copy".
 * It is this entity copy that ultimately becomes managed
 * by the{@link org.hibernate.engine.spi.PersistenceContext}.
 * In the process of merging a new or detached entity, the state of the
 * merge entity is copied onto the entity copy.
 * <p/>
 * When a merge entity is already managed, the entity copy must be the same as
 * the (managed) merge entity.
 * <p/>
 * A "mergeEntity" method parameter refers to the merge entity that is
 * (or will be) merged via {@link org.hibernate.event.spi.EventSource#merge(Object mergeEntity)}.
 * <p/>
 * An "entityCopy" method parameter refers to the entity copy that ultimately
 * becomes the merge result managed by the {@link org.hibernate.engine.spi.PersistenceContext}
 * <p/>
 * The following method should be called when a merge entity is actively being merged
 * (e.g., by {@link org.hibernate.event.spi.MergeEventListener#onMerge(MergeEvent)}:
 * {@link #addMergeData(EntityStatus mergeEntityStatus, Object mergeEntity, Object entityCopy)}}.
 * <p/>
 * After a merge entity and its entity copy is added via
 * {@link #addMergeData(EntityStatus mergeEntityStatus, Object mergeEntity, Object entityCopy)}},
 * a call to {@link #isInMergeProcess(Object mergeEntity)} will return
 * {@code true}.
 * <p/>
 * If this {@link MergeOperationContext} already contains a different merge
 * entity associated with the same entity copy, then multiple entity
 * representations for the same persistent entity are being merged.
 * If this happens, the following method will automatically be called:
 * {@link org.hibernate.event.spi.EntityCopyObserver#entityCopyDetected(
 * Object entityCopy, Object mergeEntity1, Object mergeEntity2, org.hibernate.event.spi.EventSource)}.
 * It is up to that method to determine the property course of action
 * for this situation. The implementation of {@link org.hibernate.event.spi.EntityCopyObserver
 * that will be used depends on the setting for hibernate.event.merge.entity_copy_observer
 * property. By default, merging multiple entity representations for the same
 * persistent entity is not allowed.
 * <p/>
 * In addition, if {@link #addMergeData(EntityStatus mergeEntityStatus, Object mergeEntity, Object entityCopy) }
 * is executed and this {@link MergeOperationContext} already contains
 * a cross-reference for <code>mergeEntity</code>, then <code>entityCopy</code>
 * must be the same as what is already associated with <code>mergeEntity</code>
 * in this {@link MergeOperationContext}.
 * <p/>
 * A transient entity (that will ultimately be merged) and its corresponding entity
 * copy may be added to this {@link MergeOperationContext} before the
 * merge operation cascades to that entity by using the method:
 * {@link #addTransientMergeDataBeforeInMergeProcess(Object mergeEntity, Object entityCopy)}.
 * The entity is not considered to be "in the merge process" yet and
 * a call to {@link #isInMergeProcess(Object entity)} will return
 * {@code false}. Later, when the merge operation cascades to the entity to
 * ({@link org.hibernate.event.spi.MergeEventListener#onMerge(MergeEvent)},
 * the following method must be called to indicate that
 * the transient merge entity and its entity copy is "in the merge process"
 * by calling: {@link #markTransientMergeDataInMergeProcess(Object mergeEntity)}
 * <p/>
 * The method {@link #getEntityCopyFromMergeEntity(Object mergeEntity)}} returns
 * the entity copy that corresponds with the provided merge entity.
 * <p/>
 * The method {@link #getMergeEntityFromEntityCopy(Object entityCopy)} returns
 * the merge entity that corresponds with the provided entity copy. If this
 * {@link MergeOperationContext} contains multiple merge entities that
 * correspond with the same entity copy, then the merge entity that
 * was most recently added will be returned.
 * <p/>
 * An unmodifiable collection of all associated merge entity / entity copy
 * pairs can be obtained by calling {@link #getAllMergeData()}.
 *
 * @see EventSource#merge(Object)
 * @author Gail Badner
 */
public interface MergeOperationContext extends OperationContext {

	/**
	 * Returns true if the {@link org.hibernate.event.spi.MergeEventListener} is actively
	 * performing or has already performed the merge operation on the specified merge entity.
	 * <p/>
	 * An entity is considered to be "in the merge process" if <code>mergeEntity</code>
	 * was added using {@link #addMergeData(EntityStatus mergeEntityStatus, Object mergeEntity, Object entityCopy)},
	 * or if added using {@link #addTransientMergeDataBeforeInMergeProcess(Object, Object)}
	 * followed by {@link #markTransientMergeDataInMergeProcess}.
	 *
	 * @param mergeEntity - the merge entity; must be non-null.
	 * returns true if <code>mergeEntity</code> was added using
	 * {@link #addMergeData(EntityStatus mergeEntityStatus, Object mergeEntity, Object entityCopy)},
	 * or if added using {@link #addTransientMergeDataBeforeInMergeProcess(Object, Object)}
	 * followed by {@link #markTransientMergeDataInMergeProcess}; false, otherwise.
	 * @throws IllegalArgumentException if <code>mergeEntity</code> is null.
	 * @throws IllegalStateException if a merge operation is not currently
	 * in progress.
	 */
	boolean isInMergeProcess(Object mergeEntity);

	/**
	 * Returns the entity copy associated with the specified merge Entity.
	 *
	 * @param mergeEntity the merge entity; must be non-null
	 * @return the entity copy associated with the specified merge entity,
	 * or null if this {@link MergeOperationContext} does not contain
	 * a cross-reference for <code>mergeEntity</code>.
	 *
	 * @throws IllegalArgumentException if mergeEntity is null
	 * @throws IllegalStateException if a merge operation is not currently
	 * in progress.
	 */
	Object getEntityCopyFromMergeEntity(Object mergeEntity);

	/**
	 * Returns the merge entity associated with the specified entity copy.
	 *
	 * @param entityCopy the entity copy; must be non-null
	 * @return the merge entity associated with the specified entity copy,
	 * or null if this {@link MergeOperationContext} does not contain
	 * a cross-reference for <code>entityCopy</code>.
	 *
	 * @throws IllegalArgumentException if mergeEntity is null
	 * @throws IllegalStateException if a merge operation is not currently
	 * in progress.
	 */
	Object getMergeEntityFromEntityCopy(Object entityCopy);

	/**
	 * Gets the status of the merge entity. The status of the entity copy
	 * may not be the same as for the merge entity (e.g., a transient
	 * mergeEntity may have a corresponding entity copy that is persistent).
	 *
	 * @param mergeEntity the merge entity; must be non-null.
	 * @return the status.
	 *
	 * @throws IllegalArgumentException if mergeEntity is null or
	 * this {@link MergeOperationContext} does not contain
	 * a cross-reference for <code>mergeEntity</code>.
	 * @throws IllegalStateException if a merge operation is not currently
	 * in progress.
	 */
	EntityStatus getMergeEntityStatus(Object mergeEntity);

	/**
	 * Associates the specified merge entity with the specified entity copy in this
	 * {@link MergeOperationContext}. This method should only be used when the merge
	 * entity is actively being merged (e.g., by
	 * {@link org.hibernate.event.spi.MergeEventListener#onMerge(MergeEvent)}.
	 * <p/>
	 * If this {@link MergeOperationContext} already
	 * contains a cross-reference for <code>mergeEntity</code> when this
	 * method is called, then <code>entityCopy</code> must be the same
	 * as what is already associated with <code>mergeEntity</code>.
	 *
	 * @param mergeEntityStatus the merge entity status; must be non-null.
	 * @param mergeEntity - the merge entity; must be non-null.
	 * @param entityCopy - the entity copy; must be non-null.
	 *
	 * @return true, if the merge entity and entity copy cross-reference was added
	 * to this {@link MergeOperationContext}; false, otherwise.
	 *
	 * @throws IllegalArgumentException if <code>mergeEntityStatus</code>,
	 * <code>mergeEntity</code> or <code>entityCopy</code> is null, or
	 * <code>entityCopy</code> is not the same as the previous entity copy
	 * associated with <code>mergeEntity</code>
	 * @throws IllegalStateException if a merge operation is not currently
	 * in progress.
	 */
	boolean addMergeData(EntityStatus mergeEntityStatus, Object mergeEntity, Object entityCopy);

	/**
	 * Associates the specified transient merge entity with the specified entity copy
	 * in this {@link MergeOperationContext}. This method should only be used in cases
	 * where the entity copy needs to be created before the merge operation cascades to
	 * merge entity.
	 * <p/>
	 * @param mergeEntity - the transient merge entity; must be non-null.
	 * @param entityCopy - the entity copy; must be non-null.
	 *
	 * @throws IllegalArgumentException if <code>mergeEntity</code> or <code>entityCopy</code> is null, or
	 * @throws IllegalStateException if {@link MergeOperationContext} already contains
	 * <code>mergeEntity</code> or <code>entityCopy</code>, or if a merge operation is
	 * not currently in progress.
	 */
	void addTransientMergeDataBeforeInMergeProcess(Object mergeEntity, Object entityCopy);

	/**
	 * Indicate that the listener is performing the merge operation on a transient
	 * merge entity that had previously been added before it was in the merge
	 * process.
	 *
	 * specified merge entity.
	 *
	 * @param mergeEntity; the merge entity; must be non-null.
	 * .
	 * @throws IllegalArgumentException if <code>mergeEntity</code> is null.
	 * @throws IllegalStateException if the previously added merge entity is
	 * already in the merge process, this MergeContext does not contain a
	 * cross-reference for <code>mergeEntity</code>, or if a merge operation is not currently
	 * in progress.
	 */
	void markTransientMergeDataInMergeProcess(Object mergeEntity);

	/**
	 * Gets an unmodifiable collection of all associated merge entity / entity copy pairs.
	 * If there more than 1 merge entity that is associated with the same entity copy, then
	 * the returned collection will contain a separate {@link MergeData} element for each merge
	 * entity.
	 * <p/>
	 * Teh collection will include {@link MergeData} for any merge entity / entity copy
	 * pairs addded using {@link #addTransientMergeDataBeforeInMergeProcess(Object, Object)}.
	 *
	 * @return an unmodifiable collection of all associated merge entity / entity copy pairs.
	 *
	 * @throws IllegalStateException if a merge operation is not currently
	 * in progress.
	 */
	Collection<MergeData> getAllMergeData();
}
