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
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.ReplicateEvent;
import org.hibernate.internal.util.collections.IdentitySet;

/**
 * @author Gail Badner
 */
public class ReplicateOperationContext extends AbstractEventOperationContext {

	public ReplicateOperationContext(
			EventSourceProvider eventSourceProvider,
			ReplicateEvent event) {
		super( eventSourceProvider, EventType.REPLICATE, event, 0 );
	}

	public ReplicationMode getReplicationMode() {
		return ( (ReplicateEvent) getEvent() ).getReplicationMode();
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.REPLICATE;
	}

	@Override
	public void clear() {
	}
}
