/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.event.spi.AbstractEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;

/**
 * @author Gail Badner
 */
public abstract class AbstractSaveOperationContext extends AbstractEventOperationContext {

	AbstractSaveOperationContext() {
		super();
	}

	@Override
	public void beforeOperation(EventType eventType, AbstractEvent event) {
		final EventSource session = event.getSession();
		if ( session.getActionQueue().hasUnresolvedEntityInsertActions() ) {
			throw new IllegalStateException( "There are delayed insert actions when MergeContext is being initiated." );
		}
		super.beforeOperation( eventType, event );
	}

	@Override
	public void afterOperation() {
		getSession().getActionQueue().checkNoUnresolvedActionsAfterOperation();
	}
}
                                               ;