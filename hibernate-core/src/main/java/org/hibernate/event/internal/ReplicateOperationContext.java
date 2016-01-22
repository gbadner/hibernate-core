/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.Set;

import org.hibernate.ReplicationMode;
import org.hibernate.engine.internal.EventSourceProvider;
import org.hibernate.engine.spi.OperationContext;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.util.collections.IdentitySet;

/**
 * @author Gail Badner
 */
public class ReplicateOperationContext extends AbstractEventOperationContext {
	private final ReplicationMode replicationMode;

	public ReplicateOperationContext(EventSourceProvider eventSourceProvider, ReplicationMode replicationMode) {
		super( eventSourceProvider, EventType.REPLICATE, 0 );
		this.replicationMode = replicationMode;
	}

	public ReplicationMode getReplicationMode() {
		return replicationMode;
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.REPLICATE;
	}
}
