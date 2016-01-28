/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.operationContext.spi.OperationContextType;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.internal.util.collections.IdentitySet;

/**
 * @author Gail Badner
 */
public class PersistOperationContextImpl extends AbstractSaveOperationContextImpl<PersistEvent>
		implements org.hibernate.engine.operationContext.spi.PersistOperationContext {
	private Map<Object, Object> entities = new IdentityHashMap<Object,Object>(10);

	PersistOperationContextImpl() {
		super( PersistEvent.class );
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.PERSIST;
	}

	@Override
	public void clear() {
		entities.clear();
		super.clear();
	}

	@Override
	public boolean addEntity(Object entity) {
		return entities.put( entity, entity ) == null;
	}
}
