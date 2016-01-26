/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.OperationContext;
import org.hibernate.engine.spi.OperationContextType;
import org.hibernate.event.internal.AbstractEventOperationContext;
import org.hibernate.event.internal.DeleteOperationContext;
import org.hibernate.event.internal.LockOperationContext;
import org.hibernate.event.internal.MergeOperationContext;
import org.hibernate.event.internal.PersistOperationContext;
import org.hibernate.event.internal.RefreshOperationContext;
import org.hibernate.event.internal.ReplicateOperationContext;
import org.hibernate.event.internal.SaveOrUpdateOperationContext;
import org.hibernate.event.spi.AbstractEvent;

/**
 * @author Gail Badner
 */
public class OperationContextManager {

	private final Map<OperationContextType, AbstractEventOperationContext> cachedOperationContextByType =
			new HashMap<OperationContextType, AbstractEventOperationContext>( OperationContextType.values().length );

	public OperationContext getOperationContext(OperationContextType operationContextType) {
		return getValidEventOperationContext( operationContextType );
	}

	public boolean isOperationInProgress(OperationContextType operationContextType) {
		if ( operationContextType == null ) {
			throw new IllegalArgumentException( "operationContextType must be non-null." );
		}
		final AbstractEventOperationContext operationContext =
				cachedOperationContextByType.get( operationContextType );
		return operationContext != null &&
				operationContext.isValid();
	}

	public <T extends AbstractEvent> void beforeOperation(OperationContextType operationContextType, T event) {
		if ( operationContextType == null || event == null ) {
			throw new IllegalArgumentException( "operationContextType and event must be non-null" );
		}
		AbstractEventOperationContext<T> operationContext = getOrCreateInvalidEventOperationContext(
				operationContextType
		);
		operationContext.beforeOperation( operationContext.getEventClass().cast( event ) );
	}

	public <T extends AbstractEvent> void afterOperation(OperationContextType operationContextType, T event, boolean success) {
		if ( event == null ) {
			throw new IllegalArgumentException( "eventType and event must be non-null." );
		}

		final AbstractEventOperationContext<T>  operationContext = getValidEventOperationContext( operationContextType );
		try {
			if ( success ) {
				operationContext.afterOperation( operationContext.getEventClass().cast( event) );
			}
		}
		finally {
			operationContext.clear();
		}
	}

	@SuppressWarnings( value = {"unchecked"} )
	private <T extends AbstractEvent> AbstractEventOperationContext<T> getValidEventOperationContext(OperationContextType operationContextType) {
		AbstractEventOperationContext operationContext = cachedOperationContextByType.get( operationContextType );
		if ( operationContext == null || !operationContext.isValid() ) {
			throw new IllegalStateException(
					String.format(
							"Requested operation context [%s] is not in progress",
							operationContextType.name()
					)
			);
		}
		return operationContext;
	}

	@SuppressWarnings( value = {"unchecked"} )
	private <T extends AbstractEvent> AbstractEventOperationContext<T> getOrCreateInvalidEventOperationContext(OperationContextType operationContextType) {
		AbstractEventOperationContext operationContext = cachedOperationContextByType.get( operationContextType );
		if ( operationContext == null ) {
			operationContext = createOperationContext( operationContextType );
		}

		if ( operationContext.isValid() ) {
			throw new IllegalStateException(
					String.format(
							"Requested operation context [%s] is already in progress",
							operationContextType.name()
					)
			);
		}
		return operationContext;
	}

	private AbstractEventOperationContext createOperationContext(OperationContextType operationContextType) {
		final AbstractEventOperationContext operationContext;
		switch ( operationContextType ) {
			case PERSIST:
				operationContext = new PersistOperationContext();
				break;
			case SAVE_UPDATE:
				operationContext = new SaveOrUpdateOperationContext();
				break;
			case LOCK:
				operationContext = new LockOperationContext();
				break;
			case DELETE:
				operationContext = new DeleteOperationContext();
				break;
			case REFRESH:
				operationContext = new RefreshOperationContext();
				break;
			case MERGE:
				operationContext = new MergeOperationContext();
				break;
			case REPLICATE:
				operationContext = new ReplicateOperationContext();
				break;
			default:
				throw new HibernateException( "unexpected OperationContextType: " + operationContextType.name() );
		}
		if ( cachedOperationContextByType.put( operationContext.getOperationContextType(), operationContext ) != null ) {
			throw new IllegalStateException(
					String.format( "OperationContext [%s] was already cached", operationContextType )
			);
		}
		return operationContext;
	}

	public void clear() {
		for ( AbstractEventOperationContext operationContext : cachedOperationContextByType.values() ) {
			operationContext.clear();
		}
		cachedOperationContextByType.clear();
	}
}
