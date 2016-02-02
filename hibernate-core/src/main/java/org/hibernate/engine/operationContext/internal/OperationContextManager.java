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
import org.hibernate.event.spi.DeleteEvent;
import org.hibernate.event.spi.LockEvent;
import org.hibernate.event.spi.MergeEvent;
import org.hibernate.event.spi.PersistEvent;
import org.hibernate.event.spi.RefreshEvent;
import org.hibernate.event.spi.ReplicateEvent;
import org.hibernate.event.spi.SaveOrUpdateEvent;

/**
 * @author Gail Badner
 */
public class OperationContextManager {

	private final Map<OperationContextType, OperationContextImplementor> cachedOperationContextByType =
			new HashMap<OperationContextType, OperationContextImplementor>( OperationContextType.values().length );

	public OperationContext getOperationContextInProgress(OperationContextType operationContextType) {
		return getOperationContext( operationContextType, true );
	}

	public boolean isOperationInProgress(OperationContextType operationContextType) {
		if ( operationContextType == null ) {
			throw new IllegalArgumentException( "operationContextType must be non-null." );
		}
		OperationContextImplementor operationContext = cachedOperationContextByType.get( operationContextType );
		return operationContext != null && operationContext.isInProgress();
	}

	public void beforePersistOperation(PersistEvent event) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final PersistOperationContextImpl operationContext =
				getOperationContext( OperationContextType.PERSIST, false, PersistOperationContextImpl.class
				);
		operationContext.beforeOperation( event );
	}

	public void afterPersistOperation(PersistEvent event, boolean success) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final PersistOperationContextImpl operationContext =
				getOperationContext( OperationContextType.PERSIST, true, PersistOperationContextImpl.class
				);
		operationContext.afterOperation( event, success );
	}

	public void beforeSaveOrUpdateOperation(SaveOrUpdateEvent event) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final SaveOrUpdateOperationContextImpl operationContext =
				getOperationContext( OperationContextType.SAVE_UPDATE, false, SaveOrUpdateOperationContextImpl.class );
		operationContext.beforeOperation( event );
	}

	public void afterSaveOrUpdateOperation(SaveOrUpdateEvent event, boolean success) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final SaveOrUpdateOperationContextImpl operationContext =
				getOperationContext( OperationContextType.SAVE_UPDATE, true, SaveOrUpdateOperationContextImpl.class );
		operationContext.afterOperation( event, success );
	}

	public void beforeMergeOperation(MergeEvent event) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final MergeOperationContextImpl operationContext =
				getOperationContext( OperationContextType.MERGE, false, MergeOperationContextImpl.class );
		operationContext.beforeOperation( event );
	}

	public void afterMergeOperation(MergeEvent event, boolean success) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final MergeOperationContextImpl operationContext =
				getOperationContext( OperationContextType.MERGE, true, MergeOperationContextImpl.class );
		operationContext.afterOperation( event, success );
	}

	public void beforeLockOperation(LockEvent event) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final LockOperationContextImpl operationContext =
				getOperationContext( OperationContextType.LOCK, false, LockOperationContextImpl.class );
		operationContext.beforeOperation( event );
	}

	public void afterLockOperation(LockEvent event, boolean success) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final LockOperationContextImpl operationContext =
				getOperationContext( OperationContextType.LOCK, true, LockOperationContextImpl.class );
		operationContext.afterOperation( event, success );
	}

	public void beforeDeleteOperation(DeleteEvent event) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final DeleteOperationContextImpl operationContext =
				getOperationContext( OperationContextType.DELETE, false, DeleteOperationContextImpl.class );
		operationContext.beforeOperation( event );
	}

	public void afterDeleteOperation(DeleteEvent event, boolean success) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final DeleteOperationContextImpl operationContext =
				getOperationContext( OperationContextType.DELETE, true, DeleteOperationContextImpl.class );
		operationContext.afterOperation( event, success );
	}

	public void beforeRefreshOperation(RefreshEvent event) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final RefreshOperationContextImpl operationContext =
				getOperationContext( OperationContextType.REFRESH, false, RefreshOperationContextImpl.class );
		operationContext.beforeOperation( event );
	}

	public void afterRefreshOperation(RefreshEvent event, boolean success) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final RefreshOperationContextImpl operationContext =
				getOperationContext( OperationContextType.REFRESH, true, RefreshOperationContextImpl.class );
		operationContext.afterOperation( event, success );
	}

	public void beforeReplicateOperation(ReplicateEvent event) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final ReplicateOperationContextImpl operationContext =
				getOperationContext( OperationContextType.REPLICATE, false, ReplicateOperationContextImpl.class );
		operationContext.beforeOperation( event );
	}

	public void afterReplicateOperation(ReplicateEvent event, boolean success) {
		if ( event == null ) {
			throw new IllegalArgumentException( "event must be non-null." );
		}
		final ReplicateOperationContextImpl operationContext =
				getOperationContext( OperationContextType.REPLICATE, true, ReplicateOperationContextImpl.class );
		operationContext.afterOperation( event, success );
	}

	private <X> X getOperationContext(
			OperationContextType operationContextType,
			boolean expectedInProgress,
			Class<X> expectedOperationContextClass) {
		final OperationContextImplementor operationContext = getOperationContext(
				operationContextType,
				expectedInProgress
		);
		if ( !expectedOperationContextClass.isInstance( operationContext ) ) {
			throw new IllegalArgumentException(
					String.format(
							"Unexpected OperationContext class; expected [%s], got [%s].",
							expectedOperationContextClass.getName(),
							operationContext.getClass().getName()
					)
			);
		}
		return expectedOperationContextClass.cast( operationContext );
	}

	private OperationContextImplementor getOperationContext(
			OperationContextType operationContextType,
			boolean expectedInProgress) {
		OperationContextImplementor operationContext = cachedOperationContextByType.get( operationContextType );
		final boolean isInProgress = operationContext != null && operationContext.isInProgress();
		if ( isInProgress != expectedInProgress ) {
			if ( expectedInProgress ) {
				throw new IllegalStateException(
						"OperationContext is expected to be in progress, but it is not in progress."
				);
			}
			else {
				throw new IllegalStateException(
						"OperationContext is expected to not be in progress, but it is in progress."
				);
			}
		}
		if ( operationContext == null ) {
			operationContext = createOperationContext( operationContextType);
			if ( cachedOperationContextByType.put( operationContext.getOperationContextType(), operationContext ) != null ) {
				throw new IllegalStateException(
						String.format( "OperationContext [%s] was already cached", operationContextType )
				);
			}
		}
		return operationContext;
	}

	private static OperationContextImplementor createOperationContext(OperationContextType operationContextType) {
		final OperationContextImplementor operationContext;
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
		return operationContext;
	}

	public void clear() {
		for ( OperationContextImplementor operationContext : cachedOperationContextByType.values() ) {
			operationContext.clear();
		}
		cachedOperationContextByType.clear();
	}
}
