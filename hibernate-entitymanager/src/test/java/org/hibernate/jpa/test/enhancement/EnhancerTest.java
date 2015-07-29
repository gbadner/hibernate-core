/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.enhancement;

import org.hibernate.jpa.test.enhancement.lazy.LazyPropertyOnPreUpdateListenerTestTask;
import org.hibernate.testing.FailureExpected;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.junit.Test;

/**
 * @author Gail Badner
 */
public class EnhancerTest extends BaseUnitTestCase {

	@Test
	public void testBasic() {
		EnhancerTestUtils.runEnhancerTestTask( LazyPropertyOnPreUpdateListenerTestTask.class );
	}
}
