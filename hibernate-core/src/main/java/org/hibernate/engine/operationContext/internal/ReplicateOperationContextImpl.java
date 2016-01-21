/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import org.hibernate.ReplicationMode;
import org.hibernate.engine.operationContext.spi.OperationContextType;
import org.hibernate.event.spi.ReplicateEvent;

/**
 * @author Gail Badner
 */
public class ReplicateOperationContextImpl extends AbstractManageableOperationContextImpl<ReplicateEvent>
		implements org.hibernate.engine.operationContext.spi.ReplicateOperationContext {

	ReplicateOperationContextImpl() {
		super( ReplicateEvent.class );
	}

	@Override
	public ReplicationMode getReplicationMode() {
		checkIsValid();
		return getOperationContextData().getReplicationMode();
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.REPLICATE;
	}
}
