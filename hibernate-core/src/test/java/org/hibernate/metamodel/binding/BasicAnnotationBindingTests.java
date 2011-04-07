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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.Indexer;
import org.junit.Test;

import org.hibernate.metamodel.source.Metadata;
import org.hibernate.metamodel.source.annotations.AnnotationIndex;
import org.hibernate.testing.FailureExpected;

import static org.junit.Assert.fail;

/**
 * Basic tests of annotation based binding code
 *
 * @author Hardy Ferentschik
 */
public class BasicAnnotationBindingTests extends AbstractBasicBindingTests {

	@FailureExpected(jiraKey = "HHH-5672", message = "Work in progress")
	@Test
	public void testSimpleEntityMapping() {
		super.testSimpleEntityMapping();
	}

	@FailureExpected(jiraKey = "HHH-5672", message = "Work in progress")
	@Test
	public void testSimpleVersionedEntityMapping() {
		super.testSimpleVersionedEntityMapping();
	}

	public EntityBinding buildSimpleEntityBinding() {
		AnnotationIndex index = indexForClass( SimpleEntity.class );
		Metadata metadata = new Metadata();
		metadata.getAnnotationBinder().bindClass(
				index.getClassByName( DotName.createSimple( SimpleEntity.class.getName() ) ), index
		);

		return metadata.getEntityBinding( SimpleEntity.class.getSimpleName() );
	}

	public EntityBinding buildSimpleVersionedEntityBinding() {
		AnnotationIndex index = indexForClass( SimpleEntity.class );
		Metadata metadata = new Metadata();
		metadata.getAnnotationBinder().bindClass(
				index.getClassByName( DotName.createSimple( SimpleVersionedEntity.class.getName() ) ), index
		);

		return metadata.getEntityBinding( SimpleVersionedEntity.class.getSimpleName() );
	}

	private AnnotationIndex indexForClass(Class<?>... classes) {
		Indexer indexer = new Indexer();
		for ( Class<?> clazz : classes ) {
			InputStream stream = getClass().getClassLoader().getResourceAsStream(
					clazz.getName().replace( '.', '/' ) + ".class"
			);
			try {
				indexer.index( stream );
			}
			catch ( IOException e ) {
				fail( "Unable to index" );
			}
		}

		List<Index> indexList = new ArrayList<Index>();
		indexList.add( indexer.complete() );
		return new AnnotationIndex( indexList );
	}
}
