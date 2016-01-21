/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import org.hibernate.LockOptions;
import org.hibernate.engine.operationContext.spi.LockOperationContext;
import org.hibernate.engine.operationContext.spi.OperationContextType;
import org.hibernate.event.spi.LockEvent;

/**
 * Implementation of {@link LockOperationContext}.
 *
 * @author Gail Badner
 */
public class LockOperationContextImpl extends AbstractManageableOperationContextImpl<LockEvent>
		implements org.hibernate.engine.operationContext.spi.LockOperationContext {

	LockOperationContextImpl() {
		super( LockEvent.class );
	}

	@Override
	public LockOptions getLockOptions() {
		checkIsValid();
		return getOperationContextData().getLockOptions();
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.LOCK;
	}

}
