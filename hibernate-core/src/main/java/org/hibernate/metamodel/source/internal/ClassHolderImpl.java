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

import org.hibernate.MappingException;
import org.hibernate.metamodel.source.spi.ClassHolder;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.classloading.spi.ClassLoadingException;

/**
 * @author Gail Badner
 */

public class ClassHolderImpl implements ClassHolder {
	private final ClassLoaderService classLoaderService;
	private String className;
	private Class clazz;

	private ClassHolderImpl(Class clazz) {
		if ( clazz == null ) {
			throw new IllegalArgumentException( "Class must be non-null." );
		}
		this.className = clazz.getName();
		this.clazz = clazz;
		this.classLoaderService = null;
	}

	private ClassHolderImpl(String className, ClassLoaderService classLoaderService ) {
		if ( className == null ) {
			throw new IllegalArgumentException( "Class name must be non-null." );
		}
		if ( classLoaderService == null ) {
			throw new IllegalArgumentException( "ClassLoaderService must be non-null when class loading is deferred." );
		}
		this.className = className;
		this.classLoaderService = classLoaderService;
	}

    @Override
	public String getClassName() {
        return className;
    }

	/**
	 * Create a {@code ClassHolder}.for a loaded {@code class}.
     *
	 *
	 * @param clazz the loaded class
	 * @return the created {@code ClassHolder}
	 * @throws IllegalArgumentException if clazz is null
	 */
	public static ClassHolder createLoadedClassHolder(Class clazz) {
		return new ClassHolderImpl( clazz );
	}

	/**
	 * Create a {@code ClassHolder}.for a {@code class} that is (or may not have been) loaded yet..
     *
	 * @param className the name of the class
	 * @param classLoaderService the classloader service
	 * @return the created {@code ClassHolder}
	 * @throws IllegalArgumentException if className is null
	 */
	/* package-protected*/
	static ClassHolderImpl createDeferredClassHolder(String className, ClassLoaderService classLoaderService) {
		return new ClassHolderImpl( className, classLoaderService );
    }

	@Override
	public Class getLoadedClass() {
		if ( clazz == null ) {
			try {
				clazz = classLoaderService.classForName( className );
			}
			catch (ClassLoadingException ex) {
				throw new MappingException( "Could not find class: " + className );
			}
		}
		return clazz;
	}

	@Override
	public boolean hasLoadedClass() {
		return clazz != null;
	}
}
