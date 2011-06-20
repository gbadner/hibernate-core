/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.metamodel.source.internal;

import org.junit.Test;

import org.hibernate.MappingException;
import org.hibernate.metamodel.MetadataSources;
import org.hibernate.metamodel.source.spi.ClassHolder;
import org.hibernate.service.ServiceRegistryBuilder;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.testing.junit4.BaseUnitTestCase;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
public class ClassHolderImplTest extends BaseUnitTestCase {

	@Test
	public void testLoadedClass() {
		ClassHolder classHolder = ClassHolderImpl.createLoadedClassHolder( SimpleEntity.class );
		assertTrue( classHolder.hasLoadedClass() );
		assertEquals( SimpleEntity.class.getName(), classHolder.getClassName() );
		assertSame( SimpleEntity.class, classHolder.getLoadedClass() );
	}

	@Test
	public void testDeferredClass() {
		final String className = "org.hibernate.metamodel.source.internal.Foo";
		ClassHolder classHolder =
				ClassHolderImpl.createDeferredClassHolder(
						className,
						getMetadataImpl().getServiceRegistry().getService( ClassLoaderService.class )
				);
		assertFalse( classHolder.hasLoadedClass() );
		assertEquals( className, classHolder.getClassName() );
		Class fooClass = classHolder.getLoadedClass();
		assertNotNull( fooClass );
		assertSame( Foo.class, classHolder.getLoadedClass() );
	}

	@Test
	public void testDeferredClassLoadedExternally() {
		MetadataImpl metadata = getMetadataImpl();
		final String className = "org.hibernate.metamodel.source.internal.Foo";
		ClassHolder classHolder = ClassHolderImpl.createDeferredClassHolder(
				className,
				getMetadataImpl().getServiceRegistry().getService( ClassLoaderService.class )
		);
		assertEquals( className, classHolder.getClassName() );
		assertFalse( classHolder.hasLoadedClass() );

		ClassLoaderService classLoaderService = metadata.getServiceRegistry().getService( ClassLoaderService.class );
		Class fooClass = classLoaderService.classForName( className );

		// classHolder doesn't know that it was loaded externally
		assertFalse( classHolder.hasLoadedClass() );

		// ensure that the loaded class in classHolder is the same as fooClass
		assertSame( fooClass, classHolder.getLoadedClass() );
	}

	@Test
	public void testNonExistingDeferredClass() {
		final String className = "AFakeClass";
		ClassHolder classHolder = ClassHolderImpl.createDeferredClassHolder(
				className,
				getMetadataImpl().getServiceRegistry().getService( ClassLoaderService.class )
		);
		assertFalse( classHolder.hasLoadedClass() );
		assertEquals( className, classHolder.getClassName() );
		try {
			classHolder.getLoadedClass();
			fail( "Should have thrown MappingException." );
		}
		catch ( MappingException ex ) {
			// expected
		}
	}

	private MetadataImpl getMetadataImpl() {
		MetadataSources sources = new MetadataSources( new ServiceRegistryBuilder().buildServiceRegistry() );
		return ( MetadataImpl ) sources.buildMetadata();
	}
}
