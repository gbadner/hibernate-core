/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;


/**
 * An abstract implementation to be used as a base class for an
 * {@link ManageableOperationContext}.
 *
 * @author Gail Badner
 */
public abstract class AbstractManageableOperationContextImpl<T>
		implements ManageableOperationContext<T> {
	private T operationContextData;

	protected AbstractManageableOperationContextImpl(Class<T> operationContextDataClass) {
		if ( operationContextDataClass == null ) {
			throw new IllegalArgumentException( "operationContextDataClass must be non-null" );
		}
	}

	@Override
	public final void beforeOperation(T operationContextData) {
		if ( operationContextData == null ) {
			throw new IllegalArgumentException( "operationContextData must be non-null" );
		}
		if ( isInProgress() ) {
			throw new IllegalStateException(
					String.format(
							"OperationContext [%s] is already in progress.",
							getOperationContextType()
					)
			);
		}
		// this.operationContextData must be set before calling doBeforeOperationChecks()
		// so it is available when implementations call #getOperationContextData().
		this.operationContextData = operationContextData;
		doBeforeOperation();
	}

	/**
	 * Implementations can override this method to do operation-specific
	 * setup and/or to perform integrity checks before an operation is
	 * performed.
	 */
	protected void doBeforeOperation() {
	}

	@Override
	public final void afterOperation(T operationContextData, boolean success) {
		if ( operationContextData == null ) {
			throw new IllegalArgumentException( "operationContextData must be non-null" );
		}
		checkIsValid();
		if ( this.operationContextData != operationContextData ) {
			throw new IllegalStateException(
					String.format( "Inconsistent operationContextData for OperationContext [%s]", getOperationContextType() )
			);
		}
		try {
			if ( success ) {
				doAfterSuccessfulOperation();
			}
		}
		finally {
			clear();
		}
	}

	/**
	 * Implementations can override this method to do operation-specific
	 * work and/or to perform integrity checks after an operation is
	 * performed.
	 */
	protected void doAfterSuccessfulOperation() {
	}

	protected final T getOperationContextData() {
		checkIsValid();
		return operationContextData;
	}

	@Override
	public void clear() {
		operationContextData = null;
	}

	@Override
	public boolean isInProgress() {
		return operationContextData != null;
	}

	protected void checkIsValid() {
		if ( !isInProgress() ) {
			throw new IllegalStateException(
					String.format( "OperationContext [%s] is in an invalid state", getOperationContextType() )
			);
		}
	}
}
