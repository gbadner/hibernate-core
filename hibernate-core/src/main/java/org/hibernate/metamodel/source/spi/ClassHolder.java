package org.hibernate.metamodel.source.spi;

/**
 * @author Gail Badner
 */
public interface ClassHolder {
	String getClassName();

	boolean isClassResolved();

	Class getLoadedClass();
}
