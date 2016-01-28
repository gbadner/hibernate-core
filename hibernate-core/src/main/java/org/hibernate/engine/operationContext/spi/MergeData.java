/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

/**
 * @author Gail Badner
 */
public class MergeData {
	private final Object mergeEntity;
	private final Object entityCopy;
	private boolean isInMergeProcess;

	/**
	 *
	 * @param mergeEntity the mergge entity; must be non-null
	 * @param entityCopy the managed entity; must be non-null
	 * @param isInMergeProcess indicates if the merge operation is performed on the mergeEntity.
	 */
	public MergeData(Object mergeEntity, Object entityCopy, boolean isInMergeProcess) {
		this.mergeEntity = mergeEntity;
		this.entityCopy = entityCopy;
		this.isInMergeProcess = isInMergeProcess;
	}

	public Object getMergeEntity() {
		return mergeEntity;
	}

	public Object getEntityCopy() {
		return entityCopy;
	}

	/**
	 * Returns true if the listener is performing the merge operation on the specified merge entity.
	 * @return true if the listener is performing the merge operation on the specified merge entity;
	 */
	public boolean isInMergeProcess() {
		return isInMergeProcess;
	}

	/**
	 * Set flag to indicate that the listener is performing the merge operation on this {@link MergeData}.
	 * @throws NullPointerException if mergeEntity is null
	 * @throws IllegalStateException if this MergeContext does not contain a a cross-reference for mergeEntity
	 */
	public void markInMergeProcess() {
		isInMergeProcess = true;
	}
}
