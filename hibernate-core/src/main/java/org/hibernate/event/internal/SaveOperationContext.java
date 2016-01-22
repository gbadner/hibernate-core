/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.Set;

import org.hibernate.engine.internal.EventSourceProvider;
import org.hibernate.engine.spi.OperationContextType;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.util.collections.IdentitySet;

/**
 * @author Gail Badner
 */
public class SaveOperationContext extends AbstractSaveOperationContext {
	private Set createCache = new IdentitySet(10);

	public SaveOperationContext(EventSourceProvider eventSourceProvider) {
		super( eventSourceProvider, 0 );
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.SAVE_UPDATE;
	}

	@Override
	public void clear() {
		createCache.clear();
		super.clear();
	}

	@SuppressWarnings({ "unchecked" })
	public boolean addEntity(Object entity) {
		return createCache.add( entity );
	}
}
