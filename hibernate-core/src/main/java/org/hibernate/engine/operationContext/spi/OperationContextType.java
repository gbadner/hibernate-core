/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.spi;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.event.spi.PersistEvent;

/**
 * @author Gail Badner
 */
public final class OperationContextType<X extends OperationContext> {

	public static final OperationContextType<PersistOperationContext> PERSIST =
			new OperationContextType<PersistOperationContext>(
					"persist",
					PersistOperationContext.class
			);
	public static final OperationContextType<SaveOrUpdateOperationContext> SAVE_OR_UPDATE =
			new OperationContextType<SaveOrUpdateOperationContext>(
					"save_or_update",
					SaveOrUpdateOperationContext.class
			);
	public static final OperationContextType<MergeOperationContext> MERGE =
			new OperationContextType<MergeOperationContext>(
					"merge",
					MergeOperationContext.class
			);
	public static final OperationContextType<LockOperationContext> LOCK =
			new OperationContextType<LockOperationContext>(
					"lock",
					LockOperationContext.class
			);
	public static final OperationContextType<DeleteOperationContext> DELETE =
			new OperationContextType<DeleteOperationContext>(
					"delete",
					DeleteOperationContext.class
			);
	public static final OperationContextType<RefreshOperationContext> REFRESH =
			new OperationContextType<RefreshOperationContext>(
					"refresh",
					RefreshOperationContext.class
			);
	public static final OperationContextType<ReplicateOperationContext> REPLICATE =
			new OperationContextType<ReplicateOperationContext>(
					"replicate",
					ReplicateOperationContext.class
			);

	private final String name;
	private final Class<X> operationContextInterface;

	private OperationContextType(String name, Class<X> operationContextInterface) {
		this.name = name;
		this.operationContextInterface = operationContextInterface;
	}

	public String name() {
		return name;
	}
	public Class<X> getOperationContextInterface() {
		return operationContextInterface;
	}
}
