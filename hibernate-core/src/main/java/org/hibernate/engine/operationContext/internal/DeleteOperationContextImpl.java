/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import java.util.Set;

import org.hibernate.engine.operationContext.spi.OperationContextType;
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.internal.util.collections.IdentitySet;

/**
 * @author Gail Badner
 */
public class DeleteOperationContextImpl extends AbstractEventOperationContextImpl<DeleteEvent>
		implements org.hibernate.engine.operationContext.spi.DeleteOperationContext {
	// A cache of already visited transient entities (to avoid infinite recursion)
	private Set transientEntities = new IdentitySet(10);

	public DeleteOperationContextImpl() {
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
	@SuppressWarnings({ "unchecked" })
	public boolean addTransientEntity(Object transientEntity) {
		checkValid();
		return transientEntities.add( transientEntity );
	}
}
