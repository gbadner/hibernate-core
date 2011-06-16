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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.ResolvedField;

import org.hibernate.MappingException;
import org.hibernate.metamodel.source.annotations.util.ReflectionHelper;
import org.hibernate.metamodel.source.spi.ClassHolder;
import org.hibernate.metamodel.source.spi.FieldClassHolder;
import org.hibernate.service.classloading.spi.ClassLoadingException;

/**
 * @author Gail Badner
 */

public class ClassHolderImpl implements ClassHolder {
	private final Map<String,FieldClassHolderImpl> fieldHoldersByName = new HashMap<String,FieldClassHolderImpl>();
	private Map<String,ResolvedField> resolvedFieldByName;
	private final MetadataImpl metadata;
	private String className;
	private ResolvedType resolvedType;

	private ClassHolderImpl(Class clazz, MetadataImpl metadata) {
		if ( clazz == null ) {
			throw new IllegalArgumentException( "Class must be non-null." );
		}
		if ( metadata == null ) {
			throw new IllegalArgumentException( "metadata must be non-null." );
		}
		this.className = clazz.getName();
		this.resolvedType = ReflectionHelper.resolveType( clazz );
		this.metadata = metadata;
	}

	private ClassHolderImpl(ResolvedType resolvedType, MetadataImpl metadata) {
		if ( resolvedType == null ) {
			throw new IllegalArgumentException( "resolvedType must be non-null." );
		}
		if ( metadata == null ) {
			throw new IllegalArgumentException( "metadata must be non-null." );
		}
		this.className = resolvedType.getErasedType().getName();
		this.resolvedType = resolvedType;
		this.metadata = metadata;
	}

	private ClassHolderImpl(String className, MetadataImpl metadata ) {
		if ( className == null ) {
			throw new IllegalArgumentException( "Class name must be non-null." );
		}
		if ( metadata == null ) {
			throw new IllegalArgumentException( "metadata must be non-null." );
		}
		this.className = className;
		this.metadata = metadata;
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
	public static ClassHolder createLoadedClassHolder(Class clazz, MetadataImpl metadata) {
		return new ClassHolderImpl( clazz, metadata );
	}

	/**
	 * Create a {@code ClassHolder}.for a loaded {@code class}.
     *
	 *
	 * @param resolvedType the resolved type
	 * @return the created {@code ClassHolder}
	 * @throws IllegalArgumentException if clazz is null
	 */
	public static ClassHolder createLoadedClassHolder(ResolvedType resolvedType, MetadataImpl metadata) {
		return new ClassHolderImpl( resolvedType, metadata );
	}

	/**
	 * Create a {@code ClassHolder}.for a {@code class} that is (or may not have been) loaded yet..
     *
	 * @param className the name of the class
	 * @param metadata the metadata
	 * @return the created {@code ClassHolder}
	 * @throws IllegalArgumentException if className is null
	 */
	/* package-protected*/
	static ClassHolderImpl createDeferredClassHolder(String className, MetadataImpl metadata) {
		return new ClassHolderImpl( className, metadata );
    }

	public FieldClassHolder getFieldClassHolder(String fieldName) {
		if ( fieldName == null ) {
			throw new IllegalArgumentException( "fieldname must be non-null." );
		}
		FieldClassHolderImpl fieldClassHolder = fieldHoldersByName.get( fieldName );
		if ( fieldClassHolder == null ) {
			fieldClassHolder = new FieldClassHolderImpl( fieldName, this );
			fieldHoldersByName.put( fieldName, fieldClassHolder );
		}
		return fieldClassHolder;
	}

	@Override
	public Class getLoadedClass() {
		return getResolvedType().getErasedType();
	}

	@Override
	public boolean isClassResolved() {
		return resolvedType != null;
	}

	protected ResolvedType getResolvedType() {
		try {
			resolvedType = metadata.getResolvedType( className );
		}
		catch ( ClassLoadingException ex ) {
			throw new MappingException( "Could not find class: " + className );
		}
		return resolvedType;
	}

    /* package-protected */
	boolean isFieldResolved(String fieldName) {
		return resolvedFieldByName.containsKey( fieldName );
	}

	/* package-protected */
	ClassHolder getResolvedFieldClassHolder(String fieldName) {
		if ( resolvedFieldByName == null ) {
			ResolvedTypeWithMembers resolvedTypeWithMembers = ReflectionHelper.resolveMemberTypes( getResolvedType() );
			ResolvedField resolvedFields[] = resolvedTypeWithMembers.getMemberFields();
			resolvedFieldByName = new HashMap<String,ResolvedField>( resolvedFields.length );
			for ( ResolvedField resolvedField : resolvedFields ) {
				resolvedFieldByName.put( resolvedField.getName(), resolvedField );
			}
		}
		ResolvedField resolvedField = resolvedFieldByName.get( fieldName );
		if( resolvedField == null ) {
			throw new IllegalStateException( "Field not found: " + getClassName() + "." + fieldName );
		}
		return metadata.getLoadedClassHolder( resolvedField.getType()  );
	}

	public static class FieldClassHolderImpl implements FieldClassHolder {
		private final String fieldName;
		private final ClassHolderImpl declaringClassHolder;
		private ClassHolder classHolder;

		private FieldClassHolderImpl(String fieldName, ClassHolderImpl declaringClassHolder) {
			if ( fieldName == null ) {
				throw new IllegalArgumentException( "Field name must be non-null." );
			}
			if ( declaringClassHolder == null ) {
				throw new IllegalArgumentException( "Class holder must be non-null." );
			}
			this.declaringClassHolder = declaringClassHolder;
			this.fieldName = fieldName;
		}

		@Override
		public String getFieldName() {
			return fieldName;
		}

		@Override
		public String getClassName() {
			if ( ! isClassResolved() ) {
				throw new IllegalStateException( "Class name is not resolved yet." );
			}
			return classHolder.getClassName();
		}

		@Override
		public boolean isClassResolved() {
			if ( classHolder != null ) {
				return classHolder.isClassResolved();
			}
			else if ( declaringClassHolder.isFieldResolved( fieldName ) ) {
				classHolder = declaringClassHolder.getResolvedFieldClassHolder( fieldName );
				return true;
			}
			else {
				return false;
			}
		}

		@Override
		public Class getLoadedClass() {
			if ( classHolder == null ) {
				classHolder = declaringClassHolder.getResolvedFieldClassHolder( fieldName );
			}
			return classHolder.getLoadedClass();
		}
	}
}
