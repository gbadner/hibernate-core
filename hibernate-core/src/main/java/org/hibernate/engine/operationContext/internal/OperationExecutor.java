/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

/**
 * An interface for executing an operation.
 *
 * @author Gail Badner
 */
public interface OperationExecutor<T> {
	/**
	 * Gets data required by the {@link OperationContext}.
	 *
	 * @return data required by the {@link OperationContext}
	 */
	T getOperationContextData();

	/**
	 * Executes the operation.
	 */
	void execute();
}
