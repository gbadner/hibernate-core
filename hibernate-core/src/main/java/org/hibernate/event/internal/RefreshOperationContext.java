/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.Set;

import org.hibernate.engine.spi.OperationContextType;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.RefreshEvent;
import org.hibernate.internal.util.collections.IdentitySet;

/**
 * @author Gail Badner
 */
public class RefreshOperationContext extends AbstractEventOperationContext<RefreshEvent> {
	private Set refreshedEntities = new IdentitySet(10);

	public RefreshOperationContext() {
		super( RefreshEvent.class );
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.REFRESH;
	}

	@Override
	public void beforeOperation(RefreshEvent event) {
		super.beforeOperation( event );
	}

	@Override
	public void clear() {
		refreshedEntities.clear();
		super.clear();
	}

	public boolean isRefreshed(Object entity) {
		checkValid();
		return refreshedEntities.contains( entity );
	}

	@SuppressWarnings({ "unchecked" })
	public boolean addRefreshedEntity(Object refreshedEntity) {
		checkValid();
		return refreshedEntities.add( refreshedEntity );
	}
}
