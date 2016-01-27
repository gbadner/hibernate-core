/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.operationContext.internal;

import org.hibernate.engine.operationContext.spi.OperationContext;

/**
 * @author Gail Badner
 */
public abstract class AbstractOperationContext implements OperationContext {
	public abstract void clear();
	protected abstract boolean isValid();
}
