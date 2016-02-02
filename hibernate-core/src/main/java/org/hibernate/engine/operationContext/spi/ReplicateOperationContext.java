/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

import org.hibernate.ReplicationMode;
import org.hibernate.event.spi.ReplicateEvent;

/**
 * ReplicateOperationContext is an {@link OperationContext} of type
 * {@link OperationContextType#REPLICATE} used to cache data for
 * replicating an entity and cascading the replicate operation.
 * The method in this interface is available only when a
 * replicate operation is in progress.
 * <p/>
 * To determine if a replicate operation is in progress use this method:
 * {@link org.hibernate.engine.spi.SessionImplementor#isOperationInProgress(OperationContextType)}.
 * {@code SessionImplementor#isOperationInProgress(OperationContextType.REPLICATE)}
 * will return true if a replicate operation is in progress.
 *
 * @author Gail Badner
 */
public interface ReplicateOperationContext extends OperationContext {
	/**
	 * Gets the replication mode for the replicate operation.
	 * <p/>
	 * It is only valid to call this method if the replicate operation is currently
	 * in progress (i.e., when {@link #isInProgress()} returns true).
	 *
	 * @return the replication mode.
	 * @throws IllegalStateException if the replicate operation is not currently
	 * in progress.
	 */
	ReplicationMode getReplicationMode();
}
