/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.Set;

import org.hibernate.engine.spi.OperationContext;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.util.collections.IdentitySet;

/**
 * @author Gail Badner
 */
public class DeleteOperationContext implements OperationContext {
	// A cache of already visited transient entities (to avoid infinite recursion)
	private Set transientEntities = new IdentitySet(10);
	private EventSource session;
	private final int initialCascadeLevel;

	public DeleteOperationContext(EventSource session) {
		initialCascadeLevel = session.getPersistenceContext().getCascadeLevel();
		this.session = session;
	}

	@Override
	public void afterOperation() {
		if ( session.getPersistenceContext().getCascadeLevel() != initialCascadeLevel ) {
			throw new IllegalStateException(
					String.format(
							"Cascade level is not %d after completing merge operation; it is %d",
							initialCascadeLevel,
							session.getPersistenceContext().getCascadeLevel()
					)
			);
		}
	}

	@Override
	public void cleanup() {
		session = null;
		transientEntities.clear();
	}

	@SuppressWarnings({ "unchecked" })
	public boolean addTransientEntity(Object transientEntity) {
		return transientEntities.add( transientEntity );
	}

	protected EventSource getSession() {
		return session;
	}
}
