/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import java.util.Set;

import org.hibernate.engine.operationContext.spi.OperationContextType;
import org.hibernate.engine.operationContext.spi.SaveOrUpdateOperationContext;
import org.hibernate.event.spi.SaveOrUpdateEvent;
import org.hibernate.internal.util.collections.IdentitySet;

/**
 * @author Gail Badner
 */
public class SaveOrUpdateOperationContextImpl extends AbstractSaveOperationContextImpl<SaveOrUpdateEvent>
		implements SaveOrUpdateOperationContext {
	private Set createCache = new IdentitySet(10);

	public SaveOrUpdateOperationContextImpl() {
		super( SaveOrUpdateEvent.class );
	}

	@Override
	public OperationContextType getOperationContextType() {
		return OperationContextType.SAVE_UPDATE;
	}

	@Override
	public void clear() {
		createCache.clear();
		super.clear();
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	public boolean addEntity(Object entity) {
		return createCache.add( entity );
	}
}
