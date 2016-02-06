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

	private final Map<OperationContextType, ManageableOperationContext> cachedOperationContextByType =
			new HashMap<OperationContextType, ManageableOperationContext>( 7 );

	public boolean isOperationInProgress(OperationContextType operationContextType) {
		if ( operationContextType == null ) {
			throw new IllegalArgumentException( "operationContextType must be non-null." );
		}
		ManageableOperationContext operationContext = cachedOperationContextByType.get( operationContextType );
		return operationContext != null && operationContext.isInProgress();
	}

	public <T extends OperationContext> T getOperationContextInProgress(OperationContextType<T> operationContextType) {
		ManageableOperationContext operationContext = cachedOperationContextByType.get( operationContextType );
		final boolean isInProgress = operationContext != null && operationContext.isInProgress();
		if ( !isInProgress  ) {
			throw new IllegalStateException(
					String.format(
							"Cannot get OperationContext [%s]; it is not in progress.",
							operationContextType.name()
					)
			);
		}
		if ( !operationContextType.getOperationContextInterface().isInstance( operationContext ) ) {
			throw new IllegalStateException(
					String.format(
							"Unexpected type of OperationContext type found in cache; expected [%s]; got [%s]",
							operationContextType.getOperationContextInterface(),
							operationContext.getClass().getName()
					)
			);
		}
		return operationContextType.getOperationContextInterface().cast( operationContext );
	}

	public void persist( OperationExecutor<PersistEvent> executor) {
		executeOperation(
				getOperationContextInternal(
						OperationContextType.PERSIST,
						PersistOperationContextImpl.class
				),
				executor
		);
	}

	public void saveOrUpdate(OperationExecutor<SaveOrUpdateEvent> executor) {
		executeOperation(
				getOperationContextInternal(
						OperationContextType.SAVE_OR_UPDATE,
						SaveOrUpdateOperationContextImpl.class
				),
				executor
		);
	}

	public void merge(OperationExecutor<MergeEvent> executor) {
		executeOperation(
				getOperationContextInternal(
						OperationContextType.MERGE,
						MergeOperationContextImpl.class
				),
				executor
		);
	}

	public void lock(OperationExecutor<LockEvent> executor) {
		executeOperation(
				getOperationContextInternal(
						OperationContextType.LOCK,
						LockOperationContextImpl.class
				),
				executor
		);
	}

	public void delete(OperationExecutor<DeleteEvent> executor) {
		executeOperation(
				getOperationContextInternal(
						OperationContextType.DELETE,
						DeleteOperationContextImpl.class
				),
				executor
		);
	}

	public void refresh(OperationExecutor<RefreshEvent> executor) {
		executeOperation(
				getOperationContextInternal(
						OperationContextType.REFRESH,
						RefreshOperationContextImpl.class
				),
				executor
		);
	}

	public void replicate(OperationExecutor<ReplicateEvent> executor) {
		executeOperation(
				getOperationContextInternal(
						OperationContextType.REPLICATE,
						ReplicateOperationContextImpl.class
				),
				executor
		);
	}

	private <T> void executeOperation(
			ManageableOperationContext<T> operationContext,
			OperationExecutor<T> operationExecutor) {
		final boolean isTopLevel = isTopLevel( operationContext );
		if ( isTopLevel ) {
			operationContext.beforeOperation( operationExecutor.getOperationContextData() );
		}
		boolean success = false;
		try {
			operationExecutor.execute();
			success = true;
		}
		finally {
			if ( isTopLevel ) {
				operationContext.afterOperation(
						operationExecutor.getOperationContextData(),
						success
				);
			}
		}
	}

	private boolean isTopLevel(ManageableOperationContext operationContext) {
		return !operationContext.isInProgress();
	}

	private <T extends OperationContext,U extends ManageableOperationContext> U getOperationContextInternal(
			OperationContextType<T> operationContextType,
			Class<U> expectedOperationContextClass) {
		ManageableOperationContext operationContext = cachedOperationContextByType.get( operationContextType );
		if ( operationContext == null ) {
			operationContext = createOperationContext( operationContextType);
			if ( cachedOperationContextByType.put( operationContext.getOperationContextType(), operationContext ) != null ) {
				throw new IllegalStateException(
						String.format( "OperationContext [%s] was already cached", operationContextType )
				);
			}
		}
		if ( !operationContextType.getOperationContextInterface().isInstance( operationContext ) ) {
			throw new IllegalStateException(
					String.format(
							"Unexpected type of OperationContext type found in cache; expected [%s]; got [%s]",
							operationContextType.getOperationContextInterface(),
							operationContext.getClass().getName()
					)
			);
		}
		return expectedOperationContextClass.cast( operationContext );
	}

	private static ManageableOperationContext createOperationContext(OperationContextType operationContextType) {
		final ManageableOperationContext operationContext;
		if ( operationContextType == OperationContextType.PERSIST ) {
			operationContext = new PersistOperationContextImpl();
		}
		else if ( operationContextType == OperationContextType.SAVE_OR_UPDATE ) {
			operationContext = new SaveOrUpdateOperationContextImpl();
		}
		else if ( operationContextType == OperationContextType.LOCK ) {
			operationContext = new LockOperationContextImpl();
		}
		else if ( operationContextType == OperationContextType.DELETE ) {
			operationContext = new DeleteOperationContextImpl();
		}
		else if ( operationContextType == OperationContextType.REFRESH ) {
			operationContext = new RefreshOperationContextImpl();
		}
		else if ( operationContextType == OperationContextType.MERGE ) {
			operationContext = new MergeOperationContextImpl();
		}
		else if ( operationContextType == OperationContextType.REPLICATE ) {
			operationContext = new ReplicateOperationContextImpl();
		}
		else {
			throw new HibernateException( "unexpected OperationContextType: " + operationContextType.name() );
		}
		return operationContext;
	}

	public void clear() {
		for ( ManageableOperationContext operationContext : cachedOperationContextByType.values() ) {
			operationContext.clear();
		}
		cachedOperationContextByType.clear();
	}
}
