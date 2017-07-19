/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.list;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

/**
 * @author Gail Badner
 */
public class ListElementNullEmbeddableEmptyEnabledTest extends ListElementNullEmbeddableEmptyDisabledTest {
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( AvailableSettings.CREATE_EMPTY_COMPOSITES_ENABLED, "true" );
	}
}
