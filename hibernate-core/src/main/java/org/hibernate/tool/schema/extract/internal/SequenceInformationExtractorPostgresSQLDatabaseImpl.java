package org.hibernate.tool.schema.extract.internal;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SequenceInformationExtractorPostgresSQLDatabaseImpl  extends SequenceInformationExtractorLegacyImpl {
	//Singleton access
	public static final SequenceInformationExtractorPostgresSQLDatabaseImpl INSTANCE = new SequenceInformationExtractorPostgresSQLDatabaseImpl();

	protected Long resultSetStartValueSize(ResultSet resultSet) throws SQLException {
		// column value is of type character_data so get it as a String
		final String stringValue = resultSet.getString( sequenceStartValueColumn() );
		return stringValue != null ? Long.valueOf( stringValue ) : null;
	}

	protected Long resultSetMinValue(ResultSet resultSet) throws SQLException {
		// column value is of type character_data so get it as a String
		final String stringValue = resultSet.getString( sequenceMinValueColumn() );
		return stringValue != null ? Long.valueOf( stringValue ) : null;
	}

	protected Long resultSetMaxValue(ResultSet resultSet) throws SQLException {
		// column value is of type character_data so get it as a String
		final String stringValue = resultSet.getString( sequenceMaxValueColumn() );
		return stringValue != null ? Long.valueOf( stringValue ) : null;
	}

	protected Long resultSetIncrementValue(ResultSet resultSet) throws SQLException {
		// column value is of type character_data so get it as a String
		final String stringValue = resultSet.getString( sequenceIncrementColumn() );
		return stringValue != null ? Long.valueOf( stringValue ) : null;
	}
}
