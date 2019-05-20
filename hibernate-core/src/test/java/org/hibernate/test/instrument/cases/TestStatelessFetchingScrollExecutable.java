/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.instrument.cases;

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
import org.hibernate.test.instrument.domain.AEntity3;
import org.hibernate.test.instrument.domain.BEntity3;
import org.hibernate.test.instrument.domain.CEntity3;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
@TestForIssue( jiraKey = "HHH-11147" )
public class TestStatelessFetchingScrollExecutable extends AbstractExecutable {

	protected String[] getResources() {
		return new String[] {"org/hibernate/test/instrument/domain/ScrollEntities.hbm.xml"};
	}

	@Override
	public void execute() {
		Session session = getFactory().openSession();
		session.getTransaction().begin();
		{
					int idCounter = 1;
					for ( int i = 1; i <= 3; i++ ) {
						AEntity3 aEntity = new AEntity3( idCounter++ );
						for ( int j = 1; j <= i; j++ ) {
							BEntity3 bEntity = new BEntity3( idCounter++ );
							aEntity.bEntities.add( bEntity );
							bEntity.aEntity = aEntity;
						}
						session.persist( aEntity );
					}
		}
		session.getTransaction().commit();
		session.close();

		StatelessSession statelessSession = getFactory().openStatelessSession();
		statelessSession.getTransaction().begin();
		{
					final String qry = "select a from AEntity3 a join fetch a.bEntities order by a.id";
					final ScrollableResults results = statelessSession.createQuery( qry ).scroll();
					int idCounter = 1;
					for ( int i = 1; i <= 3; i++ ) {
						assertTrue( results.next() );
						final AEntity3 aEntity = (AEntity3) results.get( 0 );
						assertEquals( idCounter++, aEntity.id );
						assertTrue( Hibernate.isPropertyInitialized( aEntity, "bEntities" ) );
						assertTrue( Hibernate.isInitialized( aEntity.bEntities ) );
						assertEquals( i, aEntity.bEntities.size() );
						final Set<Integer> expectedIds = new HashSet<>();
						for ( int j = 1 ; j <= i ; j++ ) {
							expectedIds.add( idCounter++ );
						}
						for ( BEntity3 bEntity : aEntity.bEntities ) {
							assertTrue( expectedIds.contains( bEntity.id ) );
							//assertSame( aEntity, bEntity.aEntity );
						}
					}
					assertFalse( results.next() );
		}
		statelessSession.getTransaction().commit();
		statelessSession.close();

		session = getFactory().openSession();
		session.getTransaction().begin();
		{
			for ( Object aEntity : session.createQuery( "from AEntity3" ).list() ) {
				session.delete( aEntity );
			}
		}
		session.getTransaction().commit();
		session.close();

		session = getFactory().openSession();
		session.getTransaction().begin();
		{
					AEntity3 a = new AEntity3( 1 );
					BEntity3 b1 = new BEntity3( 2 );
					BEntity3 b2 = new BEntity3( 3 );
					CEntity3 c = new CEntity3( 4 );

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

		String hql = "Select distinct a from AEntity3 a left join fetch a.bEntities b";

		statelessSession = getFactory().openStatelessSession();
		statelessSession.getTransaction().begin();
		{
					final ScrollableResults rs = statelessSession.createQuery( hql ).scroll( ScrollMode.SCROLL_INSENSITIVE );
					while ( rs.next() ) {
						AEntity3 a = (AEntity3) rs.get(0);
					}
		}
		statelessSession.getTransaction().commit();
		statelessSession.close();

		hql = "Select distinct a from AEntity3 a left join fetch a.bEntities";

		statelessSession = getFactory().openStatelessSession();
		statelessSession.getTransaction().begin();
		{
					List result = statelessSession.createQuery( hql ).list();
					for (Object obj : result) {
						AEntity3 a = (AEntity3) obj;
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
}
