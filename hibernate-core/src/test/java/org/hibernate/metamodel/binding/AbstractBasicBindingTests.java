/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.metamodel.binding;

import java.util.Iterator;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.relational.Column;
import org.hibernate.metamodel.source.internal.MetadataImpl;
import org.hibernate.metamodel.source.spi.ClassHolder;
import org.hibernate.metamodel.source.spi.MetadataImplementor;
import org.hibernate.service.BasicServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.internal.BasicServiceRegistryImpl;
import org.hibernate.testing.junit4.BaseUnitTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Basic tests of {@code hbm.xml} and annotation binding code
 *
 * @author Steve Ebersole
 */
public abstract class AbstractBasicBindingTests extends BaseUnitTestCase {

	private BasicServiceRegistryImpl serviceRegistry;
	private MetadataSources sources;

	@Before
	public void setUp() {
		serviceRegistry = (BasicServiceRegistryImpl) new ServiceRegistryBuilder().buildServiceRegistry();
		sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
	}

	@After
	public void tearDown() {
		serviceRegistry.destroy();
	}

	protected BasicServiceRegistry basicServiceRegistry() {
		return serviceRegistry;
	}

	@Test
	public void testSimpleEntityMapping() {
		MetadataImpl metadata = addSourcesForSimpleEntityBinding( sources );
		EntityBinding entityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertEntityClassHolder( entityBinding.getEntity().getPojoEntitySpecifics().getClassHolder() );
		assertRoot( metadata, entityBinding );
		assertIdAndSimpleProperty( entityBinding );

		assertNull( entityBinding.getVersioningValueBinding() );
	}

	@Test
	public void testSimpleVersionedEntityMapping() {
		MetadataImpl metadata = addSourcesForSimpleVersionedEntityBinding( sources );
		EntityBinding entityBinding = metadata.getEntityBinding( SimpleVersionedEntity.class.getName() );
		assertIdAndSimpleProperty( entityBinding );

		assertNotNull( entityBinding.getVersioningValueBinding() );
		assertNotNull( entityBinding.getVersioningValueBinding().getAttribute() );
	}

	@Test
	public void testEntityWithManyToOneMapping() {
		MetadataImpl metadata = addSourcesForManyToOne( sources );

		EntityBinding simpleEntityBinding = metadata.getEntityBinding( SimpleEntity.class.getName() );
		assertIdAndSimpleProperty( simpleEntityBinding );

		Set<EntityReferencingAttributeBinding> referenceBindings = simpleEntityBinding.getAttributeBinding( "id" )
				.getEntityReferencingAttributeBindings();
		assertEquals( "There should be only one reference binding", 1, referenceBindings.size() );

		EntityReferencingAttributeBinding referenceBinding = referenceBindings.iterator().next();
		EntityBinding referencedEntityBinding = referenceBinding.getReferencedEntityBinding();
		// TODO - Is this assertion correct (HF)?
		assertEquals( "Should be the same entity binding", referencedEntityBinding, simpleEntityBinding );

		EntityBinding entityWithManyToOneBinding = metadata.getEntityBinding( ManyToOneEntity.class.getName() );
		Iterator<EntityReferencingAttributeBinding> it = entityWithManyToOneBinding.getEntityReferencingAttributeBindings()
				.iterator();
		assertTrue( it.hasNext() );
		assertSame( entityWithManyToOneBinding.getAttributeBinding( "simpleEntity" ), it.next() );
		assertFalse( it.hasNext() );
	}

	public abstract MetadataImpl addSourcesForSimpleVersionedEntityBinding(MetadataSources sources);

	public abstract MetadataImpl addSourcesForSimpleEntityBinding(MetadataSources sources);

	public abstract MetadataImpl addSourcesForManyToOne(MetadataSources sources);

	public abstract boolean isEntityClassLoaded();

	protected void assertIdAndSimpleProperty(EntityBinding entityBinding) {
		assertNotNull( entityBinding );
		assertNotNull( entityBinding.getEntityIdentifier() );
		assertNotNull( entityBinding.getEntityIdentifier().getValueBinding() );

		AttributeBinding idAttributeBinding = entityBinding.getAttributeBinding( "id" );
		assertNotNull( idAttributeBinding );
		assertSame( idAttributeBinding, entityBinding.getEntityIdentifier().getValueBinding() );
		assertNotNull( idAttributeBinding.getAttribute() );
		assertNotNull( idAttributeBinding.getValue() );
		assertTrue( idAttributeBinding.getValue() instanceof Column );

		AttributeBinding nameBinding = entityBinding.getAttributeBinding( "name" );
		assertNotNull( nameBinding );
		assertNotNull( nameBinding.getAttribute() );
		assertNotNull( nameBinding.getValue() );
	}

	protected void assertRoot(MetadataImplementor metadata, EntityBinding entityBinding) {
		assertTrue( entityBinding.isRoot() );
		assertSame( entityBinding, metadata.getRootEntityBinding( entityBinding.getEntity().getName() ) );
	}

	protected void assertEntityClassHolder( ClassHolder classHolder ) {
		if ( isEntityClassLoaded() ) {
			assertTrue( classHolder.isClassResolved() );
		}
		else {
			assertFalse( classHolder.isClassResolved() );
		}
	}
}
