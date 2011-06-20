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
package org.hibernate.metamodel.source.spi;

/**
 * This class provides a placeholder for Class objects that may not been loaded yet.
 * @author Gail Badner
 */
public interface ClassHolder {
	/**
	 * Get the name of the class held (or to be held) by this object.
	 * @return the name of the class held by this object
	 */
	String getClassName();

	/**
	 * Indicates if this object holds a loaded {@code Class}.
     *
	 * @return true, if this object holds a loaded {@code Class};
	 *         false, otherwise.
	 */
	boolean hasLoadedClass();

	/**
	 * Returns the loaded class held by this object. If the class
	 * has not been loaded yet, this method forces the class to be
	 * loaded.
	 *
	 * @return the loaded class held by this object
	 */
	Class getLoadedClass();
}
