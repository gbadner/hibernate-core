/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.cid;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.NamedNativeQuery;

import org.hibernate.HibernateException;
import org.hibernate.annotations.Loader;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLUpdate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StringType;
import org.hibernate.usertype.UserType;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class IdClassUserTypeNullableColumnTest extends BaseNonConfigCoreFunctionalTestCase {

	@Test
	public void testNullableProperty() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					session.persist( new CompositeKeyEntity( "abc", LocaleType.DEFAULT ) );
				}
		);

		// Make sure id2 is null
		doInHibernate(
				this::sessionFactory,
				session -> {
					Object[] result = (Object[]) session.createNativeQuery( "select id1, id2 from CKE" ).getSingleResult();
					assertEquals( "abc", result[0] );
					assertNull( result[1]);
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					CompositeKeyEntity compositeKeyEntity  = session.get(
							CompositeKeyEntity.class,
							new CompositeKey( "abc", LocaleType.DEFAULT )
					);
					assertNotNull( compositeKeyEntity );
					assertEquals( "abc", compositeKeyEntity.getId1() );
					assertEquals( LocaleType.DEFAULT, compositeKeyEntity.getId2() );
					compositeKeyEntity.setName( "aName" );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					CompositeKeyEntity compositeKeyEntity  = session.createQuery(
							"from CKE",
							CompositeKeyEntity.class
					).uniqueResult();
					assertNotNull( compositeKeyEntity );
					assertEquals( "abc", compositeKeyEntity.getId1() );
					assertEquals( LocaleType.DEFAULT, compositeKeyEntity.getId2() );
					assertEquals( "aName", compositeKeyEntity.getName() );
				}
		);

		//The following will not work because the generated SQL will be:
		// select embeddable0_.id1 as id1_0_ embeddable0_.id2 as id2_0_, embeddable0_.name as name3_0_
		// from CKE embeddable0_
		// where embeddable0_.id1=? and embeddable0_.id2=?
		// An entity with id2 == null will never be loaded.
		//doInHibernate(
		//		this::sessionFactory,
		//		session -> {
		//			CompositeKeyEntity compositeKeyEntity  = session.createQuery(
		//					"from CKE where compositeKey = ?1",
		//					CompositeKeyEntity.class
		//			).setParameter( 1, new CompositeKey( "abc", LocaleType.DEFAULT ) ).uniqueResult();
		//			assertNotNull( compositeKeyEntity );
		//			assertEquals( "abc", compositeKeyEntity.getId1() );
		//			assertEquals( LocaleType.DEFAULT, compositeKeyEntity.getId2() );
		//		}
		//);

		// WORKAROUND:
		doInHibernate(
				this::sessionFactory,
				session -> {
					CompositeKeyEntity compositeKeyEntity  = session.createQuery(
							"from CKE where id1 = ?1 and (id2 = ?2 or id2 is null )" ,
							CompositeKeyEntity.class
					).setParameter( 1, "abc" ).setParameter( 2, LocaleType.DEFAULT ).uniqueResult();
					assertNotNull( compositeKeyEntity );
					assertEquals( "abc", compositeKeyEntity.getId1() );
					assertEquals( LocaleType.DEFAULT, compositeKeyEntity.getId2() );
				}
		);

		doInHibernate(
				this::sessionFactory,
				session -> {
					session.delete(
							session.get(
									CompositeKeyEntity.class, new CompositeKey(
											"abc",
											LocaleType.DEFAULT
									)
							)
					);
				}
		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { CompositeKeyEntity.class, CompositeKey.class };
	}

	@Before
	public void recreateTableWithUniqueConstraint() {
		doInHibernate(
				this::sessionFactory,
				session -> {
					session.createNativeQuery( "drop table CKE" ).executeUpdate();
					session.createNativeQuery(
							"create table CKE (id1 varchar(255) not null, id2 varchar(255), name varchar(255), unique key id1_and_id2( id1, id2))"
					).executeUpdate();
				}
		);
	}


	@Entity(name = "CKE")
	@Loader(namedQuery = "loader")
	@SQLUpdate(sql = "update CKE set name = ? where id1 = ? and ( id2=? or id2 is null )")
	@SQLDelete( sql = "delete from CKE where id1 = ? and ( id2=? or id2 is null )" )
	@NamedNativeQuery(
		name = "loader",
			query = "select id1, id2, name from CKE where id1=? and ( id2=? or id2 is null )",

		resultClass = IdClassUserTypeNullableColumnTest.CompositeKeyEntity.class
	)
	@IdClass( CompositeKey.class )
	@TypeDef(name = "LocaleType", typeClass = LocaleType.class)
	public static class CompositeKeyEntity {

		@Id
		private String id1;

		@Id
		@Type(type = "LocaleType")
		private String id2;

		private String name;

		public CompositeKeyEntity() {

		}

		public CompositeKeyEntity(String id1, String id2) {
			this.id1 = id1;
			this.id2 = id2;
		}

		public String getId1() {
			return id1;
		}

		public void setId1(String id1) {
			this.id1 = id1;
		}

		public String getId2() {
			return id2;
		}

		public void setId2(String id2) {
			this.id2 = id2;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	public static class CompositeKey implements Serializable {

		private String id1;

		private String id2;


		public CompositeKey() {
		}

		public CompositeKey(String id1, String id2) {
			this.id1 = id1;
			this.id2 = id2;
		}

		public String getId1() {
			return id1;
		}

		public void setId1(String id1) {
			this.id1 = id1;
		}

		public String getId2() {
			return id2;
		}

		public void setId2(String id2) {
			this.id2 = id2;
		}
	}

	public static class LocaleType implements UserType {
		private static String DEFAULT = "";

		public int[] sqlTypes() {
			return new int[] { StringType.INSTANCE.sqlType() };
		}

		public Class returnedClass() {
			return String.class;
		}

		public boolean equals(Object x, Object y) throws HibernateException {
			return StringType.INSTANCE.isEqual( x, y );
		}

		public int hashCode(Object x) throws HibernateException {
			return StringType.INSTANCE.getHashCode( x );
		}

		public Object nullSafeGet(
				ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
				throws HibernateException, SQLException {
			final Object value = StringType.INSTANCE.nullSafeGet( rs, names, session, owner );
			return value == null ? DEFAULT : value;
		}

		public void nullSafeSet(
				PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
				throws HibernateException, SQLException {
			StringType.INSTANCE.nullSafeSet(
					st,
					( DEFAULT.equals( value ) ? null : value ),
					index,
					session
			);
		}

		public Object deepCopy(Object value) throws HibernateException {
			return value;
		}

		public boolean isMutable() {
			return StringType.INSTANCE.isMutable();
		}

		public Serializable disassemble(Object value) throws HibernateException {
			return (Serializable) value;
		}

		public Object assemble(Serializable cached, Object owner) throws HibernateException {
			return cached;
		}

		public Object replace(Object original, Object target, Object owner) throws HibernateException {
			return original;
		}
	}
}