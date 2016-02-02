/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import java.util.IdentityHashMap;
import java.util.Map;

import org.hibernate.engine.operationContext.spi.OperationContextType;
import org.hibernate.engine.operationContext.spi.RefreshOperationContext;
import org.hibernate.event.spi.RefreshEvent;

/**
 * @author Gail Badner
 */
public class RefreshOperationContextImpl extends AbstractOperationContextImpl<RefreshEvent>
		implements RefreshOperationContext {
	private Map<Object, Object> entities = new IdentityHashMap<Object,Object>(10);

	RefreshOperationContextImpl() {
		super( RefreshEvent.class );
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.REFRESH;
	}

	@Override
	public void clear() {
		entities.clear();
		super.clear();
	}

	@Override
	public boolean addEntity(Object entity) {
		checkIsValid();
		return entities.put( entity, entity ) == null;
	}
}
