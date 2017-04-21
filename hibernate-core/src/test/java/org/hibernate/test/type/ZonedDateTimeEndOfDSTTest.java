/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

public class ZonedDateTimeEndOfDSTTest extends BaseNonConfigCoreFunctionalTestCase {
	//private static final ZoneId zoneId = ZoneId.of( "Europe/Berlin" );
	private static final TimeZone tzSystemDefaultOriginal = TimeZone.getTimeZone( ZoneId.systemDefault() );
	private static final TimeZone tzUTC = TimeZone.getTimeZone( ZoneId.of( "UTC" ) );
	private static final TimeZone tzAmericaVancouver = TimeZone.getTimeZone( "America/Vancouver" );
	private static final TimeZone tzEuropeBerlin = TimeZone.getTimeZone( "Europe/Berlin" );

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class
		};
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );

		ssrb.applySetting( AvailableSettings.JDBC_TIME_ZONE, tzUTC );
	}

	@Test
	public void testAmericaVancouverZDTAndDefaultTZ() {
		// DST ends on 11/05/2017 at 2am
		doTestBatch(
				tzAmericaVancouver,
				LocalDateTime.of( 2017, 11, 05, 0, 0, 0 ).atZone( tzAmericaVancouver.toZoneId() ),
				5
		);
	}

	@Test
	public void testAmericaVancouverZDTUtdcDefaultTZ() {
		// DST ends on 11/05/2017 at 2am
		doTestBatch(
				tzUTC,
				LocalDateTime.of( 2017, 11, 05, 0, 0, 0 ).atZone( tzAmericaVancouver.toZoneId() ),
				5
		);
	}

	@Test
	public void testEuropeBerlinZDTAndDefaultTZ() {
		// DST ends on 10/29/2017 at 3am
		doTestBatch(
				tzEuropeBerlin,
				LocalDateTime.of( 2017, 10, 29, 0, 0, 0 ).atZone( tzEuropeBerlin.toZoneId() ),
				5
		);
	}

	@Test
	public void testEuropeBerlinZDTAndUtcDefaultTZ() {
		// DST ends on 10/29/2017 at 3am
		doTestBatch(
				tzUTC,
				LocalDateTime.of( 2017, 10, 29, 0, 0, 0 ).atZone( tzEuropeBerlin.toZoneId() ),
				5
		);
	}

	private void doTestBatch(TimeZone defaultTimeZone, ZonedDateTime start, int nTestsInBatch) {
		try {
			TimeZone.setDefault( defaultTimeZone );
			ZonedDateTime zdt = start;
			for ( int i = 0 ; i < nTestsInBatch ; i++ ) {
				doOneTest( defaultTimeZone, zdt );
				zdt = zdt.plusHours( 1 );
			}
		}
		finally {
			TimeZone.setDefault( tzSystemDefaultOriginal );
		}

	}

	private void doOneTest(TimeZone defaultTimeZone, ZonedDateTime zdt) {

			Session s = openSession();
			s.getTransaction().begin();

			AnEntity entity = new AnEntity();
			entity.setTs( zdt );
			s.persist( entity );
			s.getTransaction().commit();
			s.close();

			s = openSession();
			s.getTransaction().begin();
			assertEquals( zdt, s.get( AnEntity.class, entity.getId() ).getTs() );
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
		private ZonedDateTime ts;

		@Id
		@GeneratedValue
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		@Column(name = "TS")
		public ZonedDateTime getTs() {
			return ts;
		}

		public void setTs(ZonedDateTime ts) {
			this.ts = ts;
		}
	}
}
