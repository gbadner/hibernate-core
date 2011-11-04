/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.action.internal;

import java.io.Serializable;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.internal.Nullability;
import org.hibernate.engine.internal.Versioning;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.EntityKey;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;

/**
 * @author Gail Badner
 */
public abstract class AbstractEntityInsertAction extends EntityAction {
	private transient Object[] state;
	private final boolean isVersionIncrementDisabled;
	private transient Set<Object> nonNullableTransientEntities;
	private boolean isPrepared;

	protected AbstractEntityInsertAction(
			SessionImplementor session,
			Serializable id,
			Object[] state,
			Object instance,
			EntityPersister persister,
			boolean isVersionIncrementDisabled) {
		super( session, id, instance, persister );
		this.state = state;
		this.isVersionIncrementDisabled = isVersionIncrementDisabled;
		this.isPrepared = false;
		initializeNonNullableTransientEntities();
	}

	private void initializeNonNullableTransientEntities() {
		this.nonNullableTransientEntities =
			new ForeignKeys.Nullifier( getInstance(), false, useIdentityColumn(), getSession() )
				.findNonNullableTransientEntities(
						getState(), getPersister().getPropertyTypes(), getPersister().getPropertyNullability()
				);
	}

	public boolean hasNonNullableTransientEntities() {
		return ! nonNullableTransientEntities.isEmpty();
	}

	public Iterable<Object> getNonNullableTransientEntities() {
		return nonNullableTransientEntities;
	}

	public void resolveNonNullableTransientEntity(Object entity) {
		if ( nonNullableTransientEntities.isEmpty() ) {
			throw new IllegalStateException(
					"Attempt to resolve a non-nullable transient entity, but there are none left to resolve; this action may have already been scheduled for execution." );
		}
		if ( ! nonNullableTransientEntities.remove( entity ) ) {
			// TODO: should not happen; log this...
		}
	}

	public Object[] getState() {
		return state;
	}

	protected abstract boolean useIdentityColumn();

	protected abstract EntityKey getPreparedEntityKey();

	public void prepare() {
		if ( isPrepared ) {
			throw new IllegalStateException( "Attempt to prepare an entity insert that has already been prepared." );
		}
		new ForeignKeys.Nullifier( getInstance(), false, useIdentityColumn(), getSession() )
				.nullifyTransientReferences( getState(), getPersister().getPropertyTypes() );
		new Nullability( getSession() ).checkNullability( getState(), getPersister(), false );
		Object version = Versioning.getVersion( getState(), getPersister() );
		getSession().getPersistenceContext().addEntity(
				getInstance(),
				( getPersister().isMutable() ? Status.MANAGED : Status.READ_ONLY ),
				getState(),
				getPreparedEntityKey(),
				version,
				LockMode.WRITE,
				useIdentityColumn(),
				getPersister(),
				isVersionIncrementDisabled,
				false
		);
		isPrepared = true;
	}

	protected void checkPrepared() {
		if ( ! isPrepared ) {
			throw new IllegalStateException( "An entity insert action was expected to be prepared, but it was not prepared." );
		}
	}

	@Override
    public void afterDeserialize(SessionImplementor session) {
		super.afterDeserialize( session );
		// IMPL NOTE: non-flushed changes code calls this method with session == null...
		// guard against NullPointerException
		if ( session != null ) {
			EntityEntry entityEntry = session.getPersistenceContext().getEntry( getInstance() );
			this.state = entityEntry.getLoadedState();
			initializeNonNullableTransientEntities();
		}
	}
}
