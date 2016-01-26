/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.ReplicationMode;
import org.hibernate.engine.spi.OperationContextType;
import org.hibernate.event.spi.ReplicateEvent;

/**
 * @author Gail Badner
 */
public class ReplicateOperationContext extends AbstractEventOperationContext<ReplicateEvent> {

	public ReplicateOperationContext() {
		super( ReplicateEvent.class );
	}

	public ReplicationMode getReplicationMode() {
		checkValid();
		return getEvent().getReplicationMode();
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.REPLICATE;
	}
}
