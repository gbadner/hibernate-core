/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.engine.operationContext.spi.OperationContext;
import org.hibernate.engine.operationContext.spi.OperationContextType;
import org.hibernate.event.spi.AbstractEvent;

/**
 * @author Gail Badner
 */
public class OperationContextManager {

	private final Map<OperationContextType, EventOperationContextImplementor> cachedOperationContextByType =
			new HashMap<OperationContextType, EventOperationContextImplementor>( OperationContextType.values().length );

	public OperationContext getOperationContext(OperationContextType operationContextType) {
		return getValidEventOperationContext( operationContextType );
	}

	public boolean isOperationInProgress(OperationContextType operationContextType) {
		if ( operationContextType == null ) {
			throw new IllegalArgumentException( "operationContextType must be non-null." );
		}
		final EventOperationContextImplementor operationContext =
				cachedOperationContextByType.get( operationContextType );
		return operationContext != null &&
				operationContext.isInProgress();
	}

	public <T extends AbstractEvent> void beforeOperation(OperationContextType operationContextType, T event) {
		if ( operationContextType == null || event == null ) {
			throw new IllegalArgumentException( "operationContextType and event must be non-null" );
		}
		EventOperationContextImplementor<T> operationContext = getOrCreateInvalidEventOperationContext(
				operationContextType
		);
		operationContext.beforeOperation( operationContext.getEventClass().cast( event ) );
	}

	public <T extends AbstractEvent> void afterOperation(OperationContextType operationContextType, T event, boolean success) {
		if ( event == null ) {
			throw new IllegalArgumentException( "eventType and event must be non-null." );
		}

		final EventOperationContextImplementor<T> operationContext = getValidEventOperationContext( operationContextType );
		operationContext.afterOperation( operationContext.getEventClass().cast( event ), success );
	}

	@SuppressWarnings( value = {"unchecked"} )
	private <T extends AbstractEvent> EventOperationContextImplementor<T> getValidEventOperationContext(OperationContextType operationContextType) {
		EventOperationContextImplementor operationContext = cachedOperationContextByType.get( operationContextType );
		if ( operationContext == null || !operationContext.isInProgress() ) {
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
	private <T extends AbstractEvent> EventOperationContextImplementor<T> getOrCreateInvalidEventOperationContext(OperationContextType operationContextType) {
		EventOperationContextImplementor operationContext = cachedOperationContextByType.get( operationContextType );
		if ( operationContext == null ) {
			operationContext = createOperationContext( operationContextType );
		}

		if ( operationContext.isInProgress() ) {
			throw new IllegalStateException(
					String.format(
							"Requested operation context [%s] is already in progress",
							operationContextType.name()
					)
			);
		}
		return operationContext;
	}

	private AbstractEventOperationContextImpl createOperationContext(OperationContextType operationContextType) {
		final AbstractEventOperationContextImpl operationContext;
		switch ( operationContextType ) {
			case PERSIST:
				operationContext = new PersistOperationContextImpl();
				break;
			case SAVE_UPDATE:
				operationContext = new SaveOrUpdateOperationContextImpl();
				break;
			case LOCK:
				operationContext = new LockOperationContextImpl();
				break;
			case DELETE:
				operationContext = new DeleteOperationContextImpl();
				break;
			case REFRESH:
				operationContext = new RefreshOperationContextImpl();
				break;
			case MERGE:
				operationContext = new MergeOperationContextImpl();
				break;
			case REPLICATE:
				operationContext = new ReplicateOperationContextImpl();
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
		for ( EventOperationContextImplementor operationContext : cachedOperationContextByType.values() ) {
			operationContext.clear();
		}
		cachedOperationContextByType.clear();
	}
}
