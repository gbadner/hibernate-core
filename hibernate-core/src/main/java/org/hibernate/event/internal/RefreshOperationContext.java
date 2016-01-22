/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.engine.internal.EventSourceProvider;
import org.hibernate.engine.spi.OperationContext;
import org.hibernate.engine.spi.OperationContextType;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.RefreshEvent;
import org.hibernate.internal.util.collections.IdentitySet;

/**
 * @author Gail Badner
 */
public class RefreshOperationContext extends AbstractEventOperationContext {
	private Set refreshedEntities = new IdentitySet(10);

	public RefreshOperationContext(EventSourceProvider eventSourceProvider) {
		super( eventSourceProvider, 0 );
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.REFRESH;
	}

	@Override
	public void beforeOperation(EventType eventType, AbstractEvent event) {
		super.beforeOperation( eventType, event );
	}

	@Override
	public void clear() {
		refreshedEntities.clear();
		super.clear();
	}

	@SuppressWarnings({ "unchecked" })
	public boolean addRefreshedEntity(Object refreshedEntity) {
		return refreshedEntities.add( refreshedEntity );
	}
}
