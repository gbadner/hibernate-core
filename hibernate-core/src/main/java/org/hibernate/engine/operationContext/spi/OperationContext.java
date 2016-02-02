/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

import org.hibernate.event.spi.AbstractEvent;

/**
 * An interface that represents the "context" for an operation.
 * Implementations provide a means to cache and retrieve data
 * important to the operation.
 *
 * @see {@link org.hibernate.engine.spi.SessionImplementor#getOperationContext(OperationContextType)}.
 *
 * @author Gail Badner
 */
public interface OperationContext {
	/**
	 * Gets the operation context type.
	 *
	 * @return the operation context type.
	 *
	 * @see {@link OperationContextType}
	 */
	OperationContextType getOperationContextType();

	/**
	 * Indicates if an operation is currently in progress.
	 *
	 * @return {@code true}, if the operation is in progress; {@code false}, otherwise.
	 */
	boolean isInProgress();
}
