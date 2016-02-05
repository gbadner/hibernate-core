/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import org.hibernate.engine.operationContext.spi.EntityStatus;
import org.hibernate.engine.operationContext.spi.MergeData;

/**
 * @author Gail Badner
 */
public class MergeDataImpl implements MergeData {
	private final EntityStatus mergeEntityStatus;
	private final Object mergeEntity;
	private final Object entityCopy;
	private boolean isInMergeProcess;

	/**
	 *
	 * @param mergeEntity the mergge entity; must be non-null
	 * @param entityCopy the managed entity; must be non-null
	 * @param isInMergeProcess indicates if the merge operation is performed on the mergeEntity.
	 */
	public MergeDataImpl(
			EntityStatus mergeEntityStatus,
			Object mergeEntity,
			Object entityCopy,
			boolean isInMergeProcess) {
		this.mergeEntityStatus = mergeEntityStatus;
		this.mergeEntity = mergeEntity;
		this.entityCopy = entityCopy;
		this.isInMergeProcess = isInMergeProcess;
	}

	public EntityStatus getMergeEntityStatus() {
		return mergeEntityStatus;
	}

	@Override
	public Object getMergeEntity() {
		return mergeEntity;
	}

	@Override
	public Object getEntityCopy() {
		return entityCopy;
	}

	boolean isInMergeProcess() {
		return isInMergeProcess;
	}

	void markInMergeProcess() {
		isInMergeProcess = true;
	}
}
