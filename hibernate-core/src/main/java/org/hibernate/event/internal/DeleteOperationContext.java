/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.Set;

import org.hibernate.engine.spi.OperationContextType;
import org.hibernate.internal.util.collections.IdentitySet;

/**
 * @author Gail Badner
 */
public class DeleteOperationContext extends AbstractEventOperationContext {
	// A cache of already visited transient entities (to avoid infinite recursion)
	private Set transientEntities = new IdentitySet(10);

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.DELETE;
	}

	@Override
	public void afterOperation() {
		// do nothing
	}

	@Override
	public void clear() {
		transientEntities.clear();
		super.clear();
	}

	@SuppressWarnings({ "unchecked" })
	public boolean addTransientEntity(Object transientEntity) {
		checkValid();
		return transientEntities.add( transientEntity );
	}
}
