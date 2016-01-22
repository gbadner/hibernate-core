/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.OperationContext;
import org.hibernate.engine.spi.OperationContextType;
import org.hibernate.event.internal.AbstractEventOperationContext;
import org.hibernate.event.internal.DeleteOperationContext;
import org.hibernate.event.internal.LockOperationContext;
import org.hibernate.event.internal.MergeOperationContext;
import org.hibernate.event.internal.RefreshOperationContext;
import org.hibernate.event.internal.ReplicateOperationContext;
import org.hibernate.event.internal.SaveOperationContext;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;

/**
 * @author Gail Badner
 */
public class OperationContextManager implements EventSourceProvider {

	private static Map<EventType, OperationContextType> OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE =
			new HashMap<EventType, OperationContextType>( EventType.values().size() );

	static {
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.SAVE, OperationContextType.SAVE_UPDATE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.SAVE_UPDATE, OperationContextType.SAVE_UPDATE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.UPDATE, OperationContextType.SAVE_UPDATE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.PERSIST, OperationContextType.SAVE_UPDATE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.PERSIST_ONFLUSH, OperationContextType.SAVE_UPDATE );
		//OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.FLUSH, OperationContextType.SAVE_UPDATE );
		//OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.AUTO_FLUSH, OperationContextType.SAVE_UPDATE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.LOCK, OperationContextType.LOCK );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.DELETE, OperationContextType.DELETE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.REFRESH, OperationContextType.REFRESH );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.MERGE, OperationContextType.MERGE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.REPLICATE, OperationContextType.REPLICATE );
	}

	private final Map<OperationContextType, AbstractEventOperationContext> cachedOperationContextByType =
			new HashMap<OperationContextType, AbstractEventOperationContext>( OperationContextType.values().length );

	private EventSource session;

	public OperationContextManager(EventSource session) {
		this.session = session;
	}

	@Override
	public EventSource getSession() {
		return session;
	}

	public OperationContext getOperationContext(OperationContextType operationContextType) {
		return getValidEventOperationContext( operationContextType );
	}

	public boolean isOperationInProgress(EventType eventType) {
		if ( eventType == null ) {
			throw new IllegalArgumentException( "eventType must be non-null." );
		}
		return isOperationInProgress( getOperationContextType( eventType ) );
	}

	private OperationContextType getOperationContextType(EventType eventType) {
		final OperationContextType operationContextType = OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.get( eventType );
		if ( operationContextType == null ) {
			throw new AssertionFailure( "No OperationContextType for EventType: " + eventType.eventName() );
		}
		return operationContextType;
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

	public void beforeOperation(EventType eventType, AbstractEvent event) {
		if ( eventType == null || event == null ) {
			throw new IllegalArgumentException( "eventType and event must be non-null" );
		}
		AbstractEventOperationContext operationContext = getOrCreateInvalidEventOperationContext(
				getOperationContextType(
						eventType
				)
		);
		operationContext.beforeOperation( eventType, event );
	}

	private AbstractEventOperationContext getValidEventOperationContext(OperationContextType operationContextType) {
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

	private AbstractEventOperationContext getOrCreateInvalidEventOperationContext(OperationContextType operationContextType) {
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
			case SAVE_UPDATE:
				operationContext = new SaveOperationContext( this );
				break;
			case LOCK:
				operationContext = new LockOperationContext( this );
				break;
			case DELETE:
				operationContext = new DeleteOperationContext( this );
				break;
			case REFRESH:
				operationContext = new RefreshOperationContext( this );
				break;
			case MERGE:
				operationContext = new MergeOperationContext( this );
				break;
			case REPLICATE:
				operationContext = new ReplicateOperationContext( this );
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

	public void afterOperation(EventType eventType, AbstractEvent event, boolean success) {
		if ( eventType == null || event == null ) {
			throw new IllegalArgumentException( "eventType and event must be non-null." );
		}

		final OperationContext operationContext = getValidEventOperationContext( getOperationContextType( eventType ) );
		if ( !AbstractEventOperationContext.class.isAssignableFrom( operationContext.getClass() ) ) {
			throw new IllegalStateException(
				String.format(
						"Unexpected OperationContext; expected %s; found %s",
						AbstractEventOperationContext.class.getName(),
						operationContext.getClass().getName()
				)
			);
		}

		final AbstractEventOperationContext eventOperationContext = (AbstractEventOperationContext) operationContext;
		if ( eventType != eventOperationContext.getEventType() ) {
			throw new IllegalStateException(
					String.format(
							"Inconsistent EventOperationContext; expected EventType [%s]; found [%s]",
							eventType,
							eventOperationContext.getEventType()
					)
			);
		}
		if ( event != eventOperationContext.getEvent() ) {
			throw new IllegalStateException( "Inconsistent event in EventOperationContext" );
		}
		try {
			if ( success ) {
				eventOperationContext.afterOperation();
			}
		}
		finally {
			eventOperationContext.clear();
		}
	}

	public void clear() {
		for ( AbstractEventOperationContext operationContext : cachedOperationContextByType.values() ) {
			operationContext.clear();
		}
		cachedOperationContextByType.clear();
	}
}
