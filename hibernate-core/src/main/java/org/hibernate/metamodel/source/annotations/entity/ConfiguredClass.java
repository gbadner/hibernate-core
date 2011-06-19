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
package org.hibernate.metamodel.source.annotations.entity;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.AccessType;

import com.fasterxml.classmate.ResolvedTypeWithMembers;
import com.fasterxml.classmate.members.HierarchicType;
import com.fasterxml.classmate.members.ResolvedMember;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.metamodel.binding.InheritanceType;
import org.hibernate.metamodel.source.annotations.JPADotNames;
import org.hibernate.metamodel.source.annotations.util.JandexHelper;
import org.hibernate.metamodel.source.annotations.util.ReflectionHelper;

/**
 * Represents an entity, mapped superclass or component configured via annotations/xml.
 *
 * @author Hardy Ferentschik
 */
public class ConfiguredClass {
	/**
	 * The parent of this configured class or {@code null} in case this configured class is the root of a hierarchy.
	 */
	private final ConfiguredClass parent;

	/**
	 * The Jandex class info for this configured class. Provides access to the annotation defined on this configured class.
	 */
	private final ClassInfo classInfo;

	/**
	 * The actual java type.
	 */
	private final Class<?> clazz;

	private final boolean isRoot;
	private final AccessType classAccessType;
	private final AccessType hierarchyAccessType;

	private final InheritanceType inheritanceType;
	private final boolean hasOwnTable;
	private final String primaryTableName;

	private final ConfiguredClassType configuredClassType;
	private final IdType idType;

	private final Map<String, MappedAttribute> mappedAttributes;
	private final Set<String> transientFieldNames = new HashSet<String>();
	private final Set<String> transientMethodNames = new HashSet<String>();

	private final AnnotationBindingContext context;

	public ConfiguredClass(ClassInfo info,
						   ConfiguredClass parent,
						   AccessType hierarchyAccessType,
						   InheritanceType inheritanceType,
						   ResolvedTypeWithMembers resolvedType,
						   AnnotationBindingContext context) {
		this.context = context;
		this.classInfo = info;
		this.parent = parent;
		this.isRoot = parent == null;
		this.hierarchyAccessType = hierarchyAccessType;
		this.inheritanceType = inheritanceType;
		this.clazz = context.classLoaderService().classForName( info.toString() );

		this.configuredClassType = determineType();
		this.classAccessType = determineClassAccessType();
		this.idType = determineIdType();

		this.hasOwnTable = definesItsOwnTable();
		this.primaryTableName = determinePrimaryTableName();

		// find transient field and method names
		findTransientFieldAndMethodNames();

		List<MappedAttribute> simpleProps = collectAttributes( resolvedType );
		// make sure the properties are ordered by property name
		Collections.sort( simpleProps );
		Map<String, MappedAttribute> tmpMap = new LinkedHashMap<String, MappedAttribute>();
		for ( MappedAttribute property : simpleProps ) {
			tmpMap.put( property.getName(), property );
		}
		this.mappedAttributes = Collections.unmodifiableMap( tmpMap );
	}

	public String getName() {
		return clazz.getName();
	}

	public ClassInfo getClassInfo() {
		return classInfo;
	}

	public ConfiguredClass getParent() {
		return parent;
	}

	public boolean isRoot() {
		return isRoot;
	}

	public ConfiguredClassType getConfiguredClassType() {
		return configuredClassType;
	}

	public InheritanceType getInheritanceType() {
		return inheritanceType;
	}

	public IdType getIdType() {
		return idType;
	}

	public boolean hasOwnTable() {
		return hasOwnTable;
	}

	public String getPrimaryTableName() {
		return primaryTableName;
	}

	public Iterable<MappedAttribute> getMappedAttributes() {
		return mappedAttributes.values();
	}

	public MappedAttribute getMappedProperty(String propertyName) {
		return mappedAttributes.get( propertyName );
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append( "ConfiguredClass" );
		sb.append( "{clazz=" ).append( clazz.getSimpleName() );
		sb.append( ", type=" ).append( configuredClassType );
		sb.append( ", classAccessType=" ).append( classAccessType );
		sb.append( ", isRoot=" ).append( isRoot );
		sb.append( ", inheritanceType=" ).append( inheritanceType );
		sb.append( '}' );
		return sb.toString();
	}

	private ConfiguredClassType determineType() {
		AnnotationInstance entityAnnotation = JandexHelper.getSingleAnnotation(
				classInfo, JPADotNames.ENTITY
		);
		if ( entityAnnotation != null ) {
			return ConfiguredClassType.ENTITY;
		}

		AnnotationInstance mappedSuperClassAnnotation = JandexHelper.getSingleAnnotation(
				classInfo, JPADotNames.MAPPED_SUPERCLASS
		);
		if ( mappedSuperClassAnnotation != null ) {
			return ConfiguredClassType.MAPPED_SUPERCLASS;
		}

		AnnotationInstance embeddableAnnotation = JandexHelper.getSingleAnnotation(
				classInfo, JPADotNames.EMBEDDABLE
		);
		if ( embeddableAnnotation != null ) {
			return ConfiguredClassType.EMBEDDABLE;
		}
		return ConfiguredClassType.NON_ENTITY;
	}

	private AccessType determineClassAccessType() {
		// default to the hierarchy access type to start with
		AccessType accessType = hierarchyAccessType;

		AnnotationInstance accessAnnotation = JandexHelper.getSingleAnnotation( classInfo, JPADotNames.ACCESS );
		if ( accessAnnotation != null ) {
			accessType = JandexHelper.getValueAsEnum( accessAnnotation, "value", AccessType.class );
		}

		return accessType;
	}

	/**
	 * @param resolvedTypes the resolved types for the field/properties of this class
	 *
	 * @return A list of the persistent properties of this configured class
	 */
	private List<MappedAttribute> collectAttributes(ResolvedTypeWithMembers resolvedTypes) {
		// use the class mate library to generic types
		ResolvedTypeWithMembers resolvedType = null;
		for ( HierarchicType hierarchicType : resolvedTypes.allTypesAndOverrides() ) {
			if ( hierarchicType.getType().getErasedType().equals( clazz ) ) {
				resolvedType = ReflectionHelper.resolveMemberTypes( hierarchicType.getType() );
				break;
			}
		}

		if ( resolvedType == null ) {
			throw new AssertionFailure( "Unable to resolve types for " + clazz.getName() );
		}

		List<MappedAttribute> properties = new ArrayList<MappedAttribute>();
		Set<String> explicitlyConfiguredMemberNames = createExplicitlyConfiguredAccessProperties(
				properties, resolvedType
		);

		if ( AccessType.FIELD.equals( classAccessType ) ) {
			Field fields[] = clazz.getDeclaredFields();
			Field.setAccessible( fields, true );
			for ( Field field : fields ) {
				if ( isPersistentMember( transientFieldNames, explicitlyConfiguredMemberNames, field ) ) {
					properties.add( createMappedProperty( field, resolvedType ) );
				}
			}
		}
		else {
			Method[] methods = clazz.getDeclaredMethods();
			Method.setAccessible( methods, true );
			for ( Method method : methods ) {
				if ( isPersistentMember( transientMethodNames, explicitlyConfiguredMemberNames, method ) ) {
					properties.add( createMappedProperty( method, resolvedType ) );
				}
			}
		}
		return properties;
	}

	private boolean isPersistentMember(Set<String> transientNames, Set<String> explicitlyConfiguredMemberNames, Member member) {
		if ( !ReflectionHelper.isProperty( member ) ) {
			return false;
		}

		if ( transientNames.contains( member.getName() ) ) {
			return false;
		}

		if ( explicitlyConfiguredMemberNames.contains( member.getName() ) ) {
			return false;
		}

		return true;
	}

	/**
	 * Creates {@code MappedProperty} instances for the explicitly configured persistent properties
	 *
	 * @param mappedProperties list to which to add the explicitly configured mapped properties
	 * @param resolvedMembers the resolved type parameters for this class
	 *
	 * @return the property names of the explicitly configured class names in a set
	 */
	private Set<String> createExplicitlyConfiguredAccessProperties(List<MappedAttribute> mappedProperties, ResolvedTypeWithMembers resolvedMembers) {
		Set<String> explicitAccessMembers = new HashSet<String>();

		List<AnnotationInstance> accessAnnotations = classInfo.annotations().get( JPADotNames.ACCESS );
		if ( accessAnnotations == null ) {
			return explicitAccessMembers;
		}

		// iterate over all @Access annotations defined on the current class
		for ( AnnotationInstance accessAnnotation : accessAnnotations ) {
			// we are only interested at annotations defined on fields and methods
			AnnotationTarget annotationTarget = accessAnnotation.target();
			if ( !( annotationTarget.getClass().equals( MethodInfo.class ) || annotationTarget.getClass()
					.equals( FieldInfo.class ) ) ) {
				continue;
			}

			AccessType accessType = JandexHelper.getValueAsEnum( accessAnnotation, "value", AccessType.class );

			// when class access type is field
			// overriding access annotations must be placed on properties and have the access type PROPERTY
			if ( AccessType.FIELD.equals( classAccessType ) ) {
				if ( !( annotationTarget instanceof MethodInfo ) ) {
					// todo log warning !?
					continue;
				}

				if ( !AccessType.PROPERTY.equals( accessType ) ) {
					// todo log warning !?
					continue;
				}
			}

			// when class access type is property
			// overriding access annotations must be placed on fields and have the access type FIELD
			if ( AccessType.PROPERTY.equals( classAccessType ) ) {
				if ( !( annotationTarget instanceof FieldInfo ) ) {
					// todo log warning !?
					continue;
				}

				if ( !AccessType.FIELD.equals( accessType ) ) {
					// todo log warning !?
					continue;
				}
			}

			// the placement is correct, get the member
			Member member;
			if ( annotationTarget instanceof MethodInfo ) {
				Method m;
				try {
					m = clazz.getMethod( ( (MethodInfo) annotationTarget ).name() );
				}
				catch ( NoSuchMethodException e ) {
					throw new HibernateException(
							"Unable to load method "
									+ ( (MethodInfo) annotationTarget ).name()
									+ " of class " + clazz.getName()
					);
				}
				member = m;
			}
			else {
				Field f;
				try {
					f = clazz.getField( ( (FieldInfo) annotationTarget ).name() );
				}
				catch ( NoSuchFieldException e ) {
					throw new HibernateException(
							"Unable to load field "
									+ ( (FieldInfo) annotationTarget ).name()
									+ " of class " + clazz.getName()
					);
				}
				member = f;
			}
			if ( ReflectionHelper.isProperty( member ) ) {
				mappedProperties.add( createMappedProperty( member, resolvedMembers ) );
				explicitAccessMembers.add( member.getName() );
			}
		}
		return explicitAccessMembers;
	}

	private MappedAttribute createMappedProperty(Member member, ResolvedTypeWithMembers resolvedType) {
		final String name = ReflectionHelper.getPropertyName( member );
		ResolvedMember[] resolvedMembers;
		if ( member instanceof Field ) {
			resolvedMembers = resolvedType.getMemberFields();
		}
		else {
			resolvedMembers = resolvedType.getMemberMethods();
		}
		final Type type = findResolvedType( member.getName(), resolvedMembers );
		final Map<DotName, List<AnnotationInstance>> annotations = JandexHelper.getMemberAnnotations(
				classInfo, member.getName()
		);

		MappedAttribute attribute;
		AttributeType attributeType = determineAttributeType( annotations );
		switch ( attributeType ) {
			case BASIC: {
				attribute = SimpleAttribute.createSimpleAttribute( name, ( (Class) type ).getName(), annotations );
				break;
			}
			case EMBEDDED: {
				throw new HibernateException( "foo" );
			}
			// TODO handle the different association types
			default: {
				attribute = AssociationAttribute.createAssociationAttribute(
						name, ( (Class) type ).getName(), attributeType, annotations
				);
			}
		}

		return attribute;
	}

	/**
	 * Given the annotations defined on a persistent attribute this methods determines the attribute type.
	 *
	 * @param annotations the annotations defined on the persistent attribute
	 *
	 * @return an instance of the {@code AttributeType} enum
	 */
	private AttributeType determineAttributeType(Map<DotName, List<AnnotationInstance>> annotations) {
		EnumMap<AttributeType, AnnotationInstance> discoveredAttributeTypes =
				new EnumMap<AttributeType, AnnotationInstance>( AttributeType.class );

		AnnotationInstance oneToOne = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ONE_TO_ONE );
		if ( oneToOne != null ) {
			discoveredAttributeTypes.put( AttributeType.ONE_TO_ONE, oneToOne );
		}

		AnnotationInstance oneToMany = JandexHelper.getSingleAnnotation( annotations, JPADotNames.ONE_TO_MANY );
		if ( oneToMany != null ) {
			discoveredAttributeTypes.put( AttributeType.ONE_TO_MANY, oneToMany );
		}

		AnnotationInstance manyToOne = JandexHelper.getSingleAnnotation( annotations, JPADotNames.MANY_TO_ONE );
		if ( manyToOne != null ) {
			discoveredAttributeTypes.put( AttributeType.MANY_TO_ONE, manyToOne );
		}

		AnnotationInstance manyToMany = JandexHelper.getSingleAnnotation( annotations, JPADotNames.MANY_TO_MANY );
		if ( manyToMany != null ) {
			discoveredAttributeTypes.put( AttributeType.MANY_TO_MANY, manyToMany );
		}

		AnnotationInstance embedded = JandexHelper.getSingleAnnotation( annotations, JPADotNames.EMBEDDED );
		if ( embedded != null ) {
			discoveredAttributeTypes.put( AttributeType.EMBEDDED, embedded );
		}

		if ( discoveredAttributeTypes.size() == 0 ) {
			return AttributeType.BASIC;
		}
		else if ( discoveredAttributeTypes.size() == 1 ) {
			return discoveredAttributeTypes.keySet().iterator().next();
		}
		else {
			throw new AnnotationException( "More than one association type configured for property  " + getName() + " of class " + getName() );
		}
	}

	private Type findResolvedType(String name, ResolvedMember[] resolvedMembers) {
		for ( ResolvedMember resolvedMember : resolvedMembers ) {
			if ( resolvedMember.getName().equals( name ) ) {
				return resolvedMember.getType().getErasedType();
			}
		}
		// todo - what to do here
		return null;
	}

	/**
	 * Populates the sets of transient field and method names.
	 */
	private void findTransientFieldAndMethodNames() {
		List<AnnotationInstance> transientMembers = classInfo.annotations().get( JPADotNames.TRANSIENT );
		if ( transientMembers == null ) {
			return;
		}

		for ( AnnotationInstance transientMember : transientMembers ) {
			AnnotationTarget target = transientMember.target();
			if ( target instanceof FieldInfo ) {
				transientFieldNames.add( ( (FieldInfo) target ).name() );
			}
			else {
				transientMethodNames.add( ( (MethodInfo) target ).name() );
			}
		}
	}

	private boolean definesItsOwnTable() {
		// mapped super classes and embeddables don't have their own tables
		if ( ConfiguredClassType.MAPPED_SUPERCLASS.equals( getConfiguredClassType() ) || ConfiguredClassType.EMBEDDABLE
				.equals( getConfiguredClassType() ) ) {
			return false;
		}

		if ( InheritanceType.SINGLE_TABLE.equals( inheritanceType ) ) {
			return isRoot();
		}
		return true;
	}

	private String determinePrimaryTableName() {
		String tableName = null;
		if ( hasOwnTable() ) {
			tableName = clazz.getSimpleName();
			AnnotationInstance tableAnnotation = JandexHelper.getSingleAnnotation(
					classInfo, JPADotNames.TABLE
			);
			if ( tableAnnotation != null ) {
				AnnotationValue value = tableAnnotation.value( "name" );
				String tmp = value == null ? null : value.asString();
				if ( tmp != null && !tmp.isEmpty() ) {
					tableName = tmp;
				}
			}
		}
		else if ( parent != null
				&& !parent.getConfiguredClassType().equals( ConfiguredClassType.MAPPED_SUPERCLASS )
				&& !parent.getConfiguredClassType().equals( ConfiguredClassType.EMBEDDABLE ) ) {
			tableName = parent.getPrimaryTableName();
		}
		return tableName;
	}

	private IdType determineIdType() {
		List<AnnotationInstance> idAnnotations = getClassInfo().annotations().get( JPADotNames.ENTITY );
		List<AnnotationInstance> embeddedIdAnnotations = getClassInfo()
				.annotations()
				.get( JPADotNames.EMBEDDED_ID );

		if ( idAnnotations != null && embeddedIdAnnotations != null ) {
			throw new MappingException(
					"@EmbeddedId and @Id cannot be used together. Check the configuration for " + getName() + "."
			);
		}

		if ( embeddedIdAnnotations != null ) {
			if ( embeddedIdAnnotations.size() == 1 ) {
				return IdType.EMBEDDED;
			}
			else {
				throw new AnnotationException( "Multiple @EmbeddedId annotations are not allowed" );
			}
		}

		if ( idAnnotations != null ) {
			if ( idAnnotations.size() == 1 ) {
				return IdType.SIMPLE;
			}
			else {
				return IdType.COMPOSED;
			}
		}
		return IdType.NONE;
	}
}
