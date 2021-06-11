/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.spi;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.Incubating;
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.service.Service;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.internal.exec.GenerationTarget;

/**
 * Contract for schema management tool integration.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface SchemaManagementTool extends Service {
	SchemaCreator getSchemaCreator(Map options);
	SchemaDropper getSchemaDropper(Map options);
	SchemaMigrator getSchemaMigrator(Map options);
	SchemaValidator getSchemaValidator(Map options);

	/**
	 * This allows to set an alternative implementation for the Database
	 * generation target.
	 * Used by Hibernate Reactive so that it can use the reactive database
	 * access rather than needing a JDBC connection.
	 * @param generationTarget the custom instance to use.
	 */
	void setCustomDatabaseGenerationTarget(GenerationTarget generationTarget);

	void setCustomExtractionContextFunction(BiFunction<Namespace.Name, ExtractionContext.DatabaseObjectAccess, ExtractionContext> extractionContextFunction);
	void setCustomInformationExtractorFunction(Function<ExtractionContext, InformationExtractor> informationExtractorFunction);

}
