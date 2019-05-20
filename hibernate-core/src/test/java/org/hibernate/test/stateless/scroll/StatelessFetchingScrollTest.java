/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.stateless.scroll;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-11147" )
public class StatelessFetchingScrollTest extends BaseCoreFunctionalTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-11147" )
	public void testScrollInStatelessSession() {
		Session session = openSession();
		session.getTransaction().begin();
		{
					int idCounter = 1;
					for ( int i = 1; i <= 3; i++ ) {
						AEntity aEntity = new AEntity( idCounter++ );
						for ( int j = 1; j <= i; j++ ) {
							BEntity bEntity = new BEntity( idCounter++ );
							aEntity.bEntities.add( bEntity );
							bEntity.aEntity = aEntity;
						}
						session.persist( aEntity );
					}
		}
		session.getTransaction().commit();
		session.close();

		StatelessSession statelessSession = sessionFactory().openStatelessSession();
		statelessSession.getTransaction().begin();
		{
					final String qry = "select a from AEntity a join fetch a.bEntities order by a.id";
					final ScrollableResults results = statelessSession.createQuery( qry ).scroll();
					int idCounter = 1;
					for ( int i = 1; i <= 3; i++ ) {
						assertTrue( results.next() );
						final AEntity aEntity = (AEntity) results.get( 0 );
						assertEquals( idCounter++, aEntity.id );
						assertTrue( Hibernate.isPropertyInitialized( aEntity, "bEntities" ) );
						assertTrue( Hibernate.isInitialized( aEntity.bEntities ) );
						assertEquals( i, aEntity.bEntities.size() );
						final Set<Integer> expectedIds = new HashSet<>();
						for ( int j = 1 ; j <= i ; j++ ) {
							expectedIds.add( idCounter++ );
						}
						for ( BEntity bEntity : aEntity.bEntities ) {
							assertTrue( expectedIds.contains( bEntity.id ) );
							//assertSame( aEntity, bEntity.aEntity );
						}
					}
					assertFalse( results.next() );
		}
		statelessSession.getTransaction().commit();
		statelessSession.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11147" )
	public void testDistinctScrollInStatelessSession() {
		Session session = openSession();
		session.getTransaction().begin();
		{
					AEntity a = new AEntity( 1 );
					BEntity b1 = new BEntity( 2 );
					BEntity b2 = new BEntity( 3 );
					CEntity c = new CEntity( 4 );

					b1.aEntity = a;
					b2.aEntity = a;
					b1.cEntity = c;
					b2.cEntity = c;

					session.persist( a );
					session.persist( b1 );
					session.persist( b2 );
					session.persist( c );
		}
		session.getTransaction().commit();
		session.close();

		final String hql = "Select distinct a from AEntity a left join fetch a.bEntities b";

		StatelessSession statelessSession = sessionFactory().openStatelessSession();
		statelessSession.getTransaction().begin();
		{
					final ScrollableResults rs = statelessSession.createQuery( hql ).scroll( ScrollMode.SCROLL_INSENSITIVE );
					while ( rs.next() ) {
						AEntity a = (AEntity) rs.get(0);
					}
		}
		statelessSession.getTransaction().commit();
		statelessSession.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11147" )
	public void testDistinctListInStatelessSession() {
		Session session = openSession();
		session.getTransaction().begin();
		{
					AEntity a = new AEntity( 1 );
					BEntity b1 = new BEntity( 2 );
					BEntity b2 = new BEntity( 3 );
					CEntity c = new CEntity( 4 );

					b1.aEntity = a;
					b2.aEntity = a;
					b1.cEntity = c;
					b2.cEntity = c;

					session.persist( a );
					session.persist( b1 );
					session.persist( b2 );
					session.persist( c );
		}
		session.getTransaction().commit();
		session.close();

		final String hql = "Select distinct a from AEntity a left join fetch a.bEntities";

		StatelessSession statelessSession = sessionFactory().openStatelessSession();
		statelessSession.getTransaction().begin();
		{
					List result = statelessSession.createQuery( hql ).list();
					for (Object obj : result) {
						AEntity a = (AEntity) obj;
					}
		}
		statelessSession.getTransaction().commit();
		statelessSession.close();
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( AvailableSettings.FORMAT_SQL, "false" );
		cfg.setProperty( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { AEntity.class, BEntity.class, CEntity.class };
	}

	@After
	public void cleanUpTestData() {
		Session session = openSession();
		session.getTransaction().begin();
		{
					for ( Object aEntity : session.createQuery( "from AEntity" ).list() ) {
						session.delete( aEntity );
					}
		}
		session.getTransaction().commit();
		session.close();
	}

	@Entity(name="AEntity")
	public static class AEntity {

		@Id
		private int id;

		@OneToMany(mappedBy="aEntity", fetch= FetchType.LAZY, cascade = CascadeType.ALL)
		private Set<BEntity> bEntities = new HashSet<>();

		public AEntity() {
		}

		public AEntity(int id) {
			this.id = id;
		}

	}

	@Entity(name="BEntity")
	public static class BEntity {
		@Id
		private int id;

		@ManyToOne(fetch=FetchType.LAZY)
		@LazyToOne(LazyToOneOption.NO_PROXY )
		private AEntity aEntity = null;

		@ManyToOne(fetch=FetchType.LAZY, cascade = CascadeType.ALL)
		@LazyToOne(LazyToOneOption.NO_PROXY)
		protected CEntity cEntity = null;

		public BEntity() {
		}

		public BEntity(int id) {
			this.id = id;
		}
	}

	@Entity(name="CEntity")
	public static class CEntity {

		@Id
		private int id;

		public CEntity(int id) {
			this();
			this.id = id;
			bEntities = new LinkedHashSet();
		}

		protected CEntity() {
			// this form used by Hibernate
		}

		public Set<BEntity> getBEntities() {
			return bEntities;
		}

		@OneToMany(mappedBy="cEntity", fetch=FetchType.LAZY)
		protected Set<BEntity> bEntities = null;
	}
}
