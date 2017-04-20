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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.Session;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ZonedDateTimeDSTTest extends BaseNonConfigCoreFunctionalTestCase {
	//private static final ZoneId zoneId = ZoneId.of( "Europe/Berlin" );
	//private static final ZoneId zoneId = ZoneId.of( "America/Vancouver" );
	private static final ZoneId zoneId = ZoneId.systemDefault();

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class
		};
	}

	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );

		//ssrb.applySetting( AvailableSettings.JDBC_TIME_ZONE, zoneId.getId() );
	}

	@Test
	public void test2HoursBeforeEndOfDST() {
		ZonedDateTime zdt = LocalDateTime.of( 2017, 11, 05, 0, 0, 0 ).atZone( zoneId );

		Session s = openSession();
		s.getTransaction().begin();

		AnEntity entity = new AnEntity();
		entity.setTs( zdt );
		entity.setId( 1 );
		s.persist( entity );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		assertEquals( zdt, s.get( AnEntity.class, 1 ).getTs() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void test1HourBeforeEndOfDST() {
		ZoneId zoneId = ZoneId.systemDefault();

		ZonedDateTime zdt = LocalDateTime.of( 2017, 11, 05, 0, 0, 0 ).atZone( zoneId );
		zdt = zdt.plusHours( 1 );

		Session s = openSession();
		s.getTransaction().begin();

		AnEntity entity = new AnEntity();
		entity.setTs( zdt );
		entity.setId( 1 );
		s.persist( entity );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		assertEquals( zdt, s.get( AnEntity.class, 1 ).getTs() );
		s.getTransaction().commit();
		s.close();
	}

	@Test
	public void testEndOfDST() {
		ZoneId zoneId = ZoneId.systemDefault();

		ZonedDateTime zdt = LocalDateTime.of( 2017, 11, 05, 0, 0, 0 ).atZone( zoneId );
		zdt = zdt.plusHours( 2 );

		Session s = openSession();
		s.getTransaction().begin();

		AnEntity entity = new AnEntity();
		entity.setTs( zdt );
		entity.setId( 1 );
		s.persist( entity );
		s.flush();
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.getTransaction().begin();
		assertEquals( zdt, s.get( AnEntity.class, 1 ).getTs() );
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
