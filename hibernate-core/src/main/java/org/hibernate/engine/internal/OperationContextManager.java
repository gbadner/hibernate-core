/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.OperationContext;
import org.hibernate.event.internal.DeleteOperationContext;
import org.hibernate.event.internal.LockOperationContext;
import org.hibernate.event.internal.MergeOperationContext;
import org.hibernate.event.internal.RefreshOperationContext;
import org.hibernate.event.internal.ReplicateOperationContext;
import org.hibernate.event.internal.SaveOperationContext;
import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.LockEvent;
import org.hibernate.event.spi.ReplicateEvent;

import static org.hibernate.engine.spi.OperationContext.*;
/**
 * @author Gail Badner
 */
public class OperationContextManager implements EventSourceProvider {

	private static Map<EventType,OperationContextType> OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE =
			new HashMap<EventType,OperationContextType>( EventType.values().size() );
	static {
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.SAVE, OperationContextType.SAVE_UPDATE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.SAVE_UPDATE, OperationContextType.SAVE_UPDATE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.UPDATE, OperationContextType.SAVE_UPDATE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.PERSIST, OperationContextType.SAVE_UPDATE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.PERSIST_ONFLUSH, OperationContextType.SAVE_UPDATE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.FLUSH, OperationContextType.SAVE_UPDATE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.AUTO_FLUSH, OperationContextType.SAVE_UPDATE );
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.LOCK, OperationContextType.LOCK);
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.DELETE, OperationContextType.DELETE);
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.REFRESH, OperationContextType.REFRESH);
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.MERGE, OperationContextType.MERGE);
		OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.put( EventType.REPLICATE, OperationContextType.REPLICATE);
	}

	private EventSource session;
	private final Deque<OperationContext> operationContextStack = new ArrayDeque<OperationContext>(5);

	public OperationContextManager(EventSource session) {
		this.session = session;
	}

	@Override
	public EventSource getSession() {
		return session;
	}

	public OperationContext getCurrentOperationContext() {
		return operationContextStack.peekLast();
	}

	public boolean isOperationInProgress(EventType eventType) {
		if ( eventType == null ) {
			throw new IllegalArgumentException( "eventType must be non-null." );
		}
		final OperationContextType operationContextType =
				OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.get( eventType );
		if ( operationContextType == null ) {
			throw new AssertionFailure( "No OperationContextType for EventType: " + eventType.eventName() );
		}
		return isOperationInProgress( operationContextType );
	}

	public boolean isOperationInProgress(OperationContextType operationContextType) {
		if ( operationContextType == null ) {
			throw new IllegalArgumentException( "operationContextType must be non-null." );
		}

		final OperationContext currentOperationContext = getCurrentOperationContext();
		return currentOperationContext != null &&
				currentOperationContext.getOperationContextType().equals( operationContextType );
	}

	public OperationContext pushEventOperationContext(EventType eventType, AbstractEvent event) {
		if ( eventType == null || event == null ) {
			throw new IllegalArgumentException( "eventType and event must be non-null" );
		}
		final OperationContext operationContext = createOperationContext( eventType, event );
		operationContextStack.addLast( operationContext );
		return operationContext;
	}

	private OperationContext createOperationContext(EventType eventType, AbstractEvent event) {
		final OperationContextType operationContextType = OPERATION_CONTEXT_TYPE_BY_EVENT_TYPE.get( eventType );
		switch ( operationContextType ) {
			case SAVE_UPDATE:
				return new SaveOperationContext( this, eventType );
			case LOCK:
				return new LockOperationContext( this, ( (LockEvent) event ).getLockOptions() );
			case DELETE:
				return new DeleteOperationContext( this );
			case REFRESH:
				return new RefreshOperationContext( this );
			case MERGE:
				return new MergeOperationContext( this );
			case REPLICATE:
				return new ReplicateOperationContext( this, ( (ReplicateEvent) event ).getReplicationMode() );
			default:
				throw new HibernateException( "unexpected OperationContextType: " + operationContextType.name() );
		}
	}

	public void popAndClearOperationContext(OperationContext operationContext) {
		if ( operationContext == null ) {
			throw new IllegalArgumentException( "operationContext must be non-null." );
		}
		if ( operationContextStack.isEmpty() ) {
			throw new IllegalStateException( "Cannot remove OperationContext because there is no OperationContext in process." );
		}
		if ( operationContext != operationContextStack.peekLast() ) {
			throw new IllegalStateException( "Provided operationContext is not at top of stack." );
		}
		operationContextStack.removeLast().clear();
	}

	public void clear() {
		while ( !operationContextStack.isEmpty() ) {
			OperationContext operationContext = operationContextStack.removeLast();
			operationContext.clear();
		}
	}
}
