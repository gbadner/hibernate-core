/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import java.util.Set;

import org.hibernate.engine.operationContext.spi.OperationContextType;
import org.hibernate.engine.operationContext.spi.RefreshOperationContext;
import org.hibernate.event.spi.RefreshEvent;
import org.hibernate.internal.util.collections.IdentitySet;

/**
 * @author Gail Badner
 */
public class RefreshOperationContextImpl extends AbstractEventOperationContextImpl<RefreshEvent>
		implements RefreshOperationContext {
	private Set refreshedEntities = new IdentitySet(10);

	public RefreshOperationContextImpl() {
		super( RefreshEvent.class );
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.REFRESH;
	}

	@Override
	public void beforeOperation(RefreshEvent event) {
		super.beforeOperation( event );
	}

	@Override
	public void clear() {
		refreshedEntities.clear();
		super.clear();
	}

	@Override
	public boolean isRefreshed(Object entity) {
		checkValid();
		return refreshedEntities.contains( entity );
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public boolean addRefreshedEntity(Object refreshedEntity) {
		checkValid();
		return refreshedEntities.add( refreshedEntity );
	}
}
