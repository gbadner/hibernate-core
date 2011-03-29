package org.hibernate.metamodel.source.annotations;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;

import org.hibernate.AssertionFailure;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.binding.EntityBinding;
import org.hibernate.metamodel.domain.Entity;
import org.hibernate.metamodel.source.Metadata;

/**
 * @author Hardy Ferentschik
 */
public class EntityBinder {
	private final ClassInfo classToBind;


	public EntityBinder(Metadata metadata, ClassInfo classInfo, AnnotationInstance jpaEntityAnnotation, AnnotationInstance hibernateEntityAnnotation) {
		this.classToBind = classInfo;
		EntityBinding entityBinding = new EntityBinding();
		bindJpaAnnotation( jpaEntityAnnotation, entityBinding );
		bindHibernateAnnotation( hibernateEntityAnnotation, entityBinding );
		metadata.addEntity( entityBinding );
	}

	private void bindHibernateAnnotation(AnnotationInstance annotation, EntityBinding entityBinding) {
//		if ( hibAnn != null ) {
//			dynamicInsert = hibAnn.dynamicInsert();
//			dynamicUpdate = hibAnn.dynamicUpdate();
//			optimisticLockType = hibAnn.optimisticLock();
//			selectBeforeUpdate = hibAnn.selectBeforeUpdate();
//			polymorphismType = hibAnn.polymorphism();
//			explicitHibernateEntityAnnotation = true;
//			//persister handled in bind
//		}
//		else {
//			//default values when the annotation is not there
//			dynamicInsert = false;
//			dynamicUpdate = false;
//			optimisticLockType = OptimisticLockType.VERSION;
//			polymorphismType = PolymorphismType.IMPLICIT;
//			selectBeforeUpdate = false;
//		}
	}

	private void bindJpaAnnotation(AnnotationInstance annotation, EntityBinding entityBinding) {
		if ( annotation == null ) {
			throw new AssertionFailure( "@Entity cannot be not null when binding an entity" );
		}
		String name;
		if ( annotation.value( "name" ) == null ) {
			name = StringHelper.unqualify( classToBind.name().toString() );
		}
		else {
			name = annotation.value( "name" ).asString();
		}
		entityBinding.setEntity( new Entity( name, null ) );
	}
}



