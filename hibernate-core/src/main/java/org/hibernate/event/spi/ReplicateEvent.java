/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.spi;

import org.hibernate.ReplicationMode;
import org.hibernate.event.internal.ReplicateOperationContext;

/**
 *  Defines an event class for the replication of an entity.
 *
 * @author Steve Ebersole
 */
public class ReplicateEvent extends AbstractEvent {
	private Object object;
	private String entityName;

	/**
	 * @deprecated Use {@link #ReplicateEvent(Object, EventSource)} instead.
	 */
	@Deprecated
	public ReplicateEvent(Object object, ReplicationMode replicationMode, EventSource source) {
		this(null, object, replicationMode, source);
	}

	public ReplicateEvent(Object object, EventSource source) {
		this(null, object, source);
	}

	/**
	 * @deprecated Use {@link #ReplicateEvent(String, Object, EventSource)} instead.
	 */
	@Deprecated
	public ReplicateEvent(String entityName, Object object, ReplicationMode replicationMode, EventSource source) {
		this( entityName, object, source );
	}

	public ReplicateEvent(String entityName, Object object, EventSource source) {
		super(source);
		this.entityName = entityName;

		if ( object == null ) {
			throw new IllegalArgumentException(
					"attempt to create replication strategy with null entity"
			);
		}

		this.object = object;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	/**.
	 * @return the replication mode
	 * @deprecated Use {@link ReplicateOperationContext#getReplicationMode()} instead.
	 */
	@Deprecated
	public ReplicationMode getReplicationMode() {
		return ( (ReplicateOperationContext) getSession().getOperationContext() ).getReplicationMode();
	}

	/**.
	 * Set the replication mode
	 * @deprecated Use {@link ReplicateOperationContext#ReplicateOperationContext(ReplicationMode)} instead.
	 */
	@Deprecated
	public void setReplicationMode(ReplicationMode replicationMode) {
		if ( !replicationMode.equals( getReplicationMode() ) ) {
			throw new IllegalStateException(
					String.format(
							"Attempt to set replicationMode to [%s]; it is already set in ReplicateContext as [%s]",
							replicationMode.name(),
							getReplicationMode().name()
					)
			);
		}
	}

	public String getEntityName() {
		return entityName;
	}
	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}
}
