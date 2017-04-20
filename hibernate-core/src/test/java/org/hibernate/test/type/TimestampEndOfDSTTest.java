/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.TimeZone;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimestampEndOfDSTTest extends BaseNonConfigCoreFunctionalTestCase {
	private static final TimeZone tzSystemDefaultOriginal = TimeZone.getTimeZone( ZoneId.systemDefault() );

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class
		};
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.JDBC_TIME_ZONE, TimeZone.getTimeZone( ZoneId.of( "UTC" ) ) );
	}

	@Test
	public void testAmericaVancouver() {
		try {
			// DST ends on 11/05/2017 at 2am in "America/Vancouver"
			TimeZone.setDefault( TimeZone.getTimeZone( "America/Vancouver" ) );
			doTestBatch(
					Timestamp.valueOf( "2017-11-05 00:00:00" ),
					5
			);
		}
		finally {
			TimeZone.setDefault( tzSystemDefaultOriginal );
		}
	}

	@Test
	public void testEuropeBerlin() {
		try {
			// DST ends on 10/29/2017 at 3am in "Europe/Berlin" time zone.
			TimeZone.setDefault( TimeZone.getTimeZone( "Europe/Berlin" ) );
			doTestBatch(
					Timestamp.valueOf( "2017-10-29 00:00:00" ),
					5
			);
		}
		finally {
			TimeZone.setDefault( tzSystemDefaultOriginal );
		}
	}

	private void doTestBatch(Timestamp start, int nTestsInBatch) {
		Timestamp timestamp = start;
		for ( int i = 0 ; i < nTestsInBatch ; i++ ) {
			doOneTest( timestamp );
			timestamp = Timestamp.from( timestamp.toInstant().plusSeconds( 3600 ) );
		}
	}

	private void doOneTest(Timestamp timestamp) {

		Session s = openSession();
		s.getTransaction().begin();

		AnEntity entity = new AnEntity();
		entity.setTs( timestamp );
		s.persist( entity );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		AnEntity anEntityRead = s.get( AnEntity.class, entity.getId() );
		assertEquals( timestamp, anEntityRead.getTs() );
		assertEquals( timestamp.toInstant(), anEntityRead.getTs().toInstant() );
		s.getTransaction().commit();
		s.close();
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Entity
	@Table(name = "Entity")
	public static class AnEntity {

		private int id;
		private Timestamp ts;

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@Column(name = "TS")
		public Timestamp getTs() {
			return ts;
		}

		public void setTs(Timestamp ts) {
			this.ts = ts;
		}
	}
}
