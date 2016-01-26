/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.Set;

import org.hibernate.engine.spi.OperationContextType;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.internal.util.collections.IdentitySet;

/**
 * @author Gail Badner
 */
public class PersistOperationContext extends AbstractSaveOperationContext<PersistEvent> {
	private Set createCache = new IdentitySet(10);

	public PersistOperationContext() {
		super( PersistEvent.class );
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.PERSIST;
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
