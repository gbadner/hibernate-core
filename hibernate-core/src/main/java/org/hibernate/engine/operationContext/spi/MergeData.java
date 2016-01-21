/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

/**
 * Provides a merge entity and its associated entity copy.
 *
 * @see MergeOperationContext
 *
 * @author Gail Badner
 */
public interface MergeData {
	/**
	 * Gets the merge entity.
	 * @return the merge entity.
	 */
	Object getMergeEntity();

	/**
	 * Gets the entity copy.
	 * @return the entity copy.
	 */
	Object getEntityCopy();
}
