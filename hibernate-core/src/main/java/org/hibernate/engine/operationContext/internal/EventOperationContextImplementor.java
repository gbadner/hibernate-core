/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import org.hibernate.engine.operationContext.spi.OperationContext;
import org.hibernate.event.spi.AbstractEvent;

/**
 * An interface for managing an OperationContext for an event.
 *
 * @author Gail Badner
 */
public interface EventOperationContextImplementor<T extends AbstractEvent> extends OperationContext {
	/**
	 * Called just before starting the operation. This method should not
	 * be called if the operation is already in progress (i.e., when
	 * {@link #isInProgress()} returns {@code true}).
	 * <p/>
	 * Implementations that do integrity checks should throw
	 * {@link IllegalStateException} on failure.
	 * <p/>
	 * After this method completes, {@link #isInProgress} should return {@code true}.
	 *
	 * @param event - the "top-level" event being processed; must be non-null.
	 *
	 * @throws IllegalStateException if the operation is already in progress
	 * (i.e., {@link #isInProgress()} returns {@code true}), or if an
	 * integrity check fails.
	 * @throws IllegalArgumentException if {@code event} is null.
	 */
	void beforeOperation(T event);

	/**
	 * Called just after the operation completes. This method should
	 * not be called if the operation failed.
	 * <p/>
	 * Implementations may do integrity checks if {@code success} is {@code true}.
	 * If an integrity check fails, {@link IllegalStateException} should be thrown.
	 * <p/>
	 * Resources held by implementations will be cleared by calling {@link #clear()}
	 * (even when {@code success} is {@code false}).
	 * <p/>
	 * After this method completes, {@link #isInProgress} should return {@code true}.
	 *
	 * @param event - the same event as used when {@link #beforeOperation(AbstractEvent)}
	 *                was called.
	 * @param success - {@code true}, if the operation was successful; {@code false}, otherwise.
	 *
	 * @throws IllegalStateException if {@code event} is not the same event as
	 * used when {@link #beforeOperation(AbstractEvent)} was called, or if an
	 * integrity check fails.
	 * @throws IllegalArgumentException if {@code event} is null.
	 */
	void afterOperation(T event, boolean success);

	/**
	 * The event class used for the operation. This method can be used to
	 * cast {@link AbstractEvent} to the expected class when calling
	 * {@link #beforeOperation(AbstractEvent)} and
	 * {@link #afterOperation(AbstractEvent, boolean)}.
	 *
	 * @return the event class.
	 */
	Class<T> getEventClass();

	/**
	 * Clears operation-specific data. After the method executes {@link #isInProgress()}
	 * will return {@code false}.
	 */
	void clear();
}
