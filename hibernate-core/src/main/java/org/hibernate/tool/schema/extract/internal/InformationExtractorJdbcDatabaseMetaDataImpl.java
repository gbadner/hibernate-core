/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tool.schema.extract.internal;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import org.hibernate.JDBCException;
import org.hibernate.boot.model.TruthValue;
import org.hibernate.boot.model.naming.DatabaseIdentifier;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.QualifiedTableName;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.engine.config.spi.StandardConverters;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.hibernate.tool.schema.extract.spi.ExtractionContext;
import org.hibernate.tool.schema.extract.spi.ForeignKeyInformation;
import org.hibernate.tool.schema.extract.spi.IndexInformation;
import org.hibernate.tool.schema.extract.spi.InformationExtractor;
import org.hibernate.tool.schema.extract.spi.NameSpaceTablesInformation;
import org.hibernate.tool.schema.extract.spi.PrimaryKeyInformation;
import org.hibernate.tool.schema.extract.spi.SchemaExtractionException;
import org.hibernate.tool.schema.extract.spi.TableInformation;
import org.hibernate.tool.schema.spi.SchemaManagementException;

/**
 * Implementation of the SchemaMetaDataExtractor contract which uses the standard JDBC {@link java.sql.DatabaseMetaData}
 * API for extraction.
 *
 * @author Steve Ebersole
 */
public class InformationExtractorJdbcDatabaseMetaDataImpl extends AbstractInformationExtractorImpl {
	private static final CoreMessageLogger log = CoreLogging.messageLogger( InformationExtractorJdbcDatabaseMetaDataImpl.class );

	public InformationExtractorJdbcDatabaseMetaDataImpl(ExtractionContext extractionContext) {
		super(extractionContext );
	}

	@Override
	protected ResultSet getCatalogsResultSet() throws SQLException {
		return getExtractionContext().getJdbcDatabaseMetaData().getCatalogs();
	}

	@Override
	protected String getCatalogColumnName() {
		return "TABLE_CAT";
	}

	@Override
	protected ResultSet getSchemasResultSet(String catalogFilter, String schemaFilter) throws SQLException {
		return getExtractionContext().getJdbcDatabaseMetaData().getSchemas(
				catalogFilter,
				schemaFilter
		);
	}

	@Override
	protected ResultSet getTablesResultSet(
			String catalogFilter,
			String schemaFilter,
			String tableNameFilter,
			String[] tableTypes) throws SQLException {
		return getExtractionContext().getJdbcDatabaseMetaData().getTables(
				catalogFilter,
				schemaFilter,
				tableNameFilter,
				tableTypes
		);
	}

	@Override
	protected String getTablesResultSetTableNameColumn() {
		return "TABLE_NAME";
	}

	@Override
	protected String getTablesResultSetTableTypeColumn() {
		return "TABLE_TYPE";
	}

	@Override
	protected String getTablesResultSetRemarksColumn() {
		return "REMARKS";
	}

	@Override
	protected ResultSet getColumnsResultSet(
			String catalogFilter,
			String schemaFilter,
			String tableNamePattern,
			String columnNamePattern) throws SQLException {
		return getExtractionContext().getJdbcDatabaseMetaData().getColumns(
				catalogFilter,
				schemaFilter,
				tableNamePattern,
				columnNamePattern
		);
	}

	@Override
	protected String getColumnsResultSetTableNameColumn() {
		return "TABLE_NAME";
	}

	@Override
	protected String getColumnsResultSetColumnNameColumn() {
		return "COLUMN_NAME";
	}

	@Override
	protected String getColumnsResultSetDataTypeColumn() {
		return "DATA_TYPE";
	}

	@Override
	protected String getColumnsResultSetTypeNameColumn() {
		return "TYPE_NAME";
	}

	@Override
	protected String getColumnsResultSetColumnSizeColumn() {
		return "COLUMN_SIZE";
	}

	@Override
	protected String getColumnsResultSetDecimalDigitsColumn() {
		return "DECIMAL_DIGITS";
	}

	@Override
	protected String getColumnsResultSetIsNullableColumn() {
		return "IS_NULLABLE";
	}

	@Override
	protected String getTablesResultSetTableTypesTableConstant() {
		return "TABLE";
	}

	@Override
	protected ResultSet getPrimaryKeysResultSet(
			String catalogFilter,
			String schemaFilter,
			Identifier tableName) throws SQLException {
		return getExtractionContext().getJdbcDatabaseMetaData().getPrimaryKeys(
				catalogFilter,
				schemaFilter,
				tableName.getText()
		);
	}

	@Override
	protected String getPrimaryKeysResultSetPrimaryKeyNameColumn() {
		return "PK_NAME";
	}

	@Override
	protected String getPrimaryKeysResultSetColumnPositionColumn() {
		return "KEY_SEQ" ;
	}

	@Override
	protected String getPrimaryKeysResultSetColumnNameColumn() {
		return "COLUMN_NAME" ;
	}

	@Override
	protected ResultSet getIndexInfoResultSet(
			String catalogFilter,
			String schemaFilter,
			Identifier tableName,
			boolean unique,
			boolean approximate) throws SQLException {
		return getExtractionContext().getJdbcDatabaseMetaData().getIndexInfo(
				catalogFilter,
				schemaFilter,
				tableName.getText(),
				unique,
				approximate
		);
	}

}
