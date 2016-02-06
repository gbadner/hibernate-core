/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventSource;

/**
 * An base class for "save" operations that provides integrity checking
 * before and after saving an entity.
 *
 * @author Gail Badner
 */
public abstract class AbstractSaveOperationContextImpl<T extends AbstractEvent> extends AbstractManageableOperationContextImpl<T> {

	// TODO: Should code using UnresolvedEntityInsertActions be moved from ActionQueue to this class?

	protected AbstractSaveOperationContextImpl(Class<T> eventClass) {
		super( eventClass );
	}

	@Override
	protected void doBeforeOperation() {
		if ( getSession().getActionQueue().hasUnresolvedEntityInsertActions() ) {
			throw new IllegalStateException(
					String.format(
							"There are delayed insert actions when OperationContext [%s] is being initiated.",
							getOperationContextType()
					)
			);
		}
		super.doBeforeOperation();
	}

	@Override
	protected void doAfterSuccessfulOperation() {
		getSession().getActionQueue().checkNoUnresolvedActionsAfterOperation();
		super.doAfterSuccessfulOperation();
	}

	final protected EventSource getSession() {
		return getOperationContextData().getSession();
	}
}
