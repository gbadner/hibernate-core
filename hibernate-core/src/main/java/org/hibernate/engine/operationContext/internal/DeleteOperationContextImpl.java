/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import java.util.IdentityHashMap;
import java.util.Map;

import org.hibernate.engine.operationContext.spi.DeleteOperationContext;
import org.hibernate.engine.operationContext.spi.OperationContextType;
import org.hibernate.event.spi.DeleteEvent;

/**
 * Implementation of {@link DeleteOperationContext}.
 *
 * @author Gail Badner
 */
public class DeleteOperationContextImpl extends AbstractManageableOperationContextImpl<DeleteEvent>
		implements org.hibernate.engine.operationContext.spi.DeleteOperationContext {
	// A cache of already visited transient entities (to avoid infinite recursion)
	private Map<Object, Object> transientEntities = new IdentityHashMap<Object,Object>(10);

	DeleteOperationContextImpl() {
		super( DeleteEvent.class );
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.DELETE;
	}

	@Override
	public void clear() {
		transientEntities.clear();
		super.clear();
	}

	@Override
	public boolean addTransientEntity(Object transientEntity) {
		checkIsValid();
		return transientEntities.put( transientEntity, transientEntity ) == null;
	}
}
