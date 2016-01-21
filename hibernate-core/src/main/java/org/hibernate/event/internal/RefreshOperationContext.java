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
public class RefreshOperationContext implements OperationContext {
	private Set refreshedEntities = new IdentitySet(10);
	private EventSource session;

	public RefreshOperationContext(EventSource session) {
		if ( session.getPersistenceContext().getCascadeLevel() != 0 ) {
			throw new IllegalStateException(
					"Initiating operation with cascade level " +
							session.getPersistenceContext().getCascadeLevel()
			);
		}
		this.session = session;
	}

	@Override
	public void afterOperation() {
		if ( session.getPersistenceContext().getCascadeLevel() != 0 ) {
			throw new IllegalStateException(
					"Cascade level is not 0 after completing operation; it is " +
							session.getPersistenceContext().getCascadeLevel()
			);
		}
	}

	@Override
	public void cleanup() {
		session = null;
		refreshedEntities.clear();
	}

	@SuppressWarnings({ "unchecked" })
	public boolean addRefreshedEntity(Object refreshedEntity) {
		return refreshedEntities.add( refreshedEntity );
	}

	protected EventSource getSession() {
		return session;
	}
}
