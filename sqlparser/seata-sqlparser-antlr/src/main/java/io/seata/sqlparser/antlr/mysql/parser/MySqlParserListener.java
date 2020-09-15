// Generated from E:/seata/seata/sqlparser/seata-sqlparser-antlr/src/main/java/io/seata/sqlparser/antlr/mysql/antlr\MySqlParser.g4 by ANTLR 4.8
package io.seata.sqlparser.antlr.mysql.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link MySqlParser}.
 */
public interface MySqlParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link MySqlParser#root}.
	 * @param ctx the parse tree
	 */
	void enterRoot(MySqlParser.RootContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#root}.
	 * @param ctx the parse tree
	 */
	void exitRoot(MySqlParser.RootContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#sqlStatements}.
	 * @param ctx the parse tree
	 */
	void enterSqlStatements(MySqlParser.SqlStatementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#sqlStatements}.
	 * @param ctx the parse tree
	 */
	void exitSqlStatements(MySqlParser.SqlStatementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#sqlStatement}.
	 * @param ctx the parse tree
	 */
	void enterSqlStatement(MySqlParser.SqlStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#sqlStatement}.
	 * @param ctx the parse tree
	 */
	void exitSqlStatement(MySqlParser.SqlStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void enterEmptyStatement(MySqlParser.EmptyStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#emptyStatement}.
	 * @param ctx the parse tree
	 */
	void exitEmptyStatement(MySqlParser.EmptyStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#ddlStatement}.
	 * @param ctx the parse tree
	 */
	void enterDdlStatement(MySqlParser.DdlStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#ddlStatement}.
	 * @param ctx the parse tree
	 */
	void exitDdlStatement(MySqlParser.DdlStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dmlStatement}.
	 * @param ctx the parse tree
	 */
	void enterDmlStatement(MySqlParser.DmlStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dmlStatement}.
	 * @param ctx the parse tree
	 */
	void exitDmlStatement(MySqlParser.DmlStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#transactionStatement}.
	 * @param ctx the parse tree
	 */
	void enterTransactionStatement(MySqlParser.TransactionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#transactionStatement}.
	 * @param ctx the parse tree
	 */
	void exitTransactionStatement(MySqlParser.TransactionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#replicationStatement}.
	 * @param ctx the parse tree
	 */
	void enterReplicationStatement(MySqlParser.ReplicationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#replicationStatement}.
	 * @param ctx the parse tree
	 */
	void exitReplicationStatement(MySqlParser.ReplicationStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#preparedStatement}.
	 * @param ctx the parse tree
	 */
	void enterPreparedStatement(MySqlParser.PreparedStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#preparedStatement}.
	 * @param ctx the parse tree
	 */
	void exitPreparedStatement(MySqlParser.PreparedStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#compoundStatement}.
	 * @param ctx the parse tree
	 */
	void enterCompoundStatement(MySqlParser.CompoundStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#compoundStatement}.
	 * @param ctx the parse tree
	 */
	void exitCompoundStatement(MySqlParser.CompoundStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#administrationStatement}.
	 * @param ctx the parse tree
	 */
	void enterAdministrationStatement(MySqlParser.AdministrationStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#administrationStatement}.
	 * @param ctx the parse tree
	 */
	void exitAdministrationStatement(MySqlParser.AdministrationStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#utilityStatement}.
	 * @param ctx the parse tree
	 */
	void enterUtilityStatement(MySqlParser.UtilityStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#utilityStatement}.
	 * @param ctx the parse tree
	 */
	void exitUtilityStatement(MySqlParser.UtilityStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createDatabase}.
	 * @param ctx the parse tree
	 */
	void enterCreateDatabase(MySqlParser.CreateDatabaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createDatabase}.
	 * @param ctx the parse tree
	 */
	void exitCreateDatabase(MySqlParser.CreateDatabaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createEvent}.
	 * @param ctx the parse tree
	 */
	void enterCreateEvent(MySqlParser.CreateEventContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createEvent}.
	 * @param ctx the parse tree
	 */
	void exitCreateEvent(MySqlParser.CreateEventContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createIndex}.
	 * @param ctx the parse tree
	 */
	void enterCreateIndex(MySqlParser.CreateIndexContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createIndex}.
	 * @param ctx the parse tree
	 */
	void exitCreateIndex(MySqlParser.CreateIndexContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createLogfileGroup}.
	 * @param ctx the parse tree
	 */
	void enterCreateLogfileGroup(MySqlParser.CreateLogfileGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createLogfileGroup}.
	 * @param ctx the parse tree
	 */
	void exitCreateLogfileGroup(MySqlParser.CreateLogfileGroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createProcedure}.
	 * @param ctx the parse tree
	 */
	void enterCreateProcedure(MySqlParser.CreateProcedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createProcedure}.
	 * @param ctx the parse tree
	 */
	void exitCreateProcedure(MySqlParser.CreateProcedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createFunction}.
	 * @param ctx the parse tree
	 */
	void enterCreateFunction(MySqlParser.CreateFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createFunction}.
	 * @param ctx the parse tree
	 */
	void exitCreateFunction(MySqlParser.CreateFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createServer}.
	 * @param ctx the parse tree
	 */
	void enterCreateServer(MySqlParser.CreateServerContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createServer}.
	 * @param ctx the parse tree
	 */
	void exitCreateServer(MySqlParser.CreateServerContext ctx);
	/**
	 * Enter a parse tree produced by the {@code copyCreateTable}
	 * labeled alternative in {@link MySqlParser#createTable}.
	 * @param ctx the parse tree
	 */
	void enterCopyCreateTable(MySqlParser.CopyCreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code copyCreateTable}
	 * labeled alternative in {@link MySqlParser#createTable}.
	 * @param ctx the parse tree
	 */
	void exitCopyCreateTable(MySqlParser.CopyCreateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code queryCreateTable}
	 * labeled alternative in {@link MySqlParser#createTable}.
	 * @param ctx the parse tree
	 */
	void enterQueryCreateTable(MySqlParser.QueryCreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code queryCreateTable}
	 * labeled alternative in {@link MySqlParser#createTable}.
	 * @param ctx the parse tree
	 */
	void exitQueryCreateTable(MySqlParser.QueryCreateTableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnCreateTable}
	 * labeled alternative in {@link MySqlParser#createTable}.
	 * @param ctx the parse tree
	 */
	void enterColumnCreateTable(MySqlParser.ColumnCreateTableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnCreateTable}
	 * labeled alternative in {@link MySqlParser#createTable}.
	 * @param ctx the parse tree
	 */
	void exitColumnCreateTable(MySqlParser.ColumnCreateTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createTablespaceInnodb}.
	 * @param ctx the parse tree
	 */
	void enterCreateTablespaceInnodb(MySqlParser.CreateTablespaceInnodbContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createTablespaceInnodb}.
	 * @param ctx the parse tree
	 */
	void exitCreateTablespaceInnodb(MySqlParser.CreateTablespaceInnodbContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createTablespaceNdb}.
	 * @param ctx the parse tree
	 */
	void enterCreateTablespaceNdb(MySqlParser.CreateTablespaceNdbContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createTablespaceNdb}.
	 * @param ctx the parse tree
	 */
	void exitCreateTablespaceNdb(MySqlParser.CreateTablespaceNdbContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createTrigger}.
	 * @param ctx the parse tree
	 */
	void enterCreateTrigger(MySqlParser.CreateTriggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createTrigger}.
	 * @param ctx the parse tree
	 */
	void exitCreateTrigger(MySqlParser.CreateTriggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createView}.
	 * @param ctx the parse tree
	 */
	void enterCreateView(MySqlParser.CreateViewContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createView}.
	 * @param ctx the parse tree
	 */
	void exitCreateView(MySqlParser.CreateViewContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createDatabaseOption}.
	 * @param ctx the parse tree
	 */
	void enterCreateDatabaseOption(MySqlParser.CreateDatabaseOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createDatabaseOption}.
	 * @param ctx the parse tree
	 */
	void exitCreateDatabaseOption(MySqlParser.CreateDatabaseOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#ownerStatement}.
	 * @param ctx the parse tree
	 */
	void enterOwnerStatement(MySqlParser.OwnerStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#ownerStatement}.
	 * @param ctx the parse tree
	 */
	void exitOwnerStatement(MySqlParser.OwnerStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code preciseSchedule}
	 * labeled alternative in {@link MySqlParser#scheduleExpression}.
	 * @param ctx the parse tree
	 */
	void enterPreciseSchedule(MySqlParser.PreciseScheduleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code preciseSchedule}
	 * labeled alternative in {@link MySqlParser#scheduleExpression}.
	 * @param ctx the parse tree
	 */
	void exitPreciseSchedule(MySqlParser.PreciseScheduleContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intervalSchedule}
	 * labeled alternative in {@link MySqlParser#scheduleExpression}.
	 * @param ctx the parse tree
	 */
	void enterIntervalSchedule(MySqlParser.IntervalScheduleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intervalSchedule}
	 * labeled alternative in {@link MySqlParser#scheduleExpression}.
	 * @param ctx the parse tree
	 */
	void exitIntervalSchedule(MySqlParser.IntervalScheduleContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#timestampValue}.
	 * @param ctx the parse tree
	 */
	void enterTimestampValue(MySqlParser.TimestampValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#timestampValue}.
	 * @param ctx the parse tree
	 */
	void exitTimestampValue(MySqlParser.TimestampValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#intervalExpr}.
	 * @param ctx the parse tree
	 */
	void enterIntervalExpr(MySqlParser.IntervalExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#intervalExpr}.
	 * @param ctx the parse tree
	 */
	void exitIntervalExpr(MySqlParser.IntervalExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#intervalType}.
	 * @param ctx the parse tree
	 */
	void enterIntervalType(MySqlParser.IntervalTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#intervalType}.
	 * @param ctx the parse tree
	 */
	void exitIntervalType(MySqlParser.IntervalTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#enableType}.
	 * @param ctx the parse tree
	 */
	void enterEnableType(MySqlParser.EnableTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#enableType}.
	 * @param ctx the parse tree
	 */
	void exitEnableType(MySqlParser.EnableTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#indexType}.
	 * @param ctx the parse tree
	 */
	void enterIndexType(MySqlParser.IndexTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#indexType}.
	 * @param ctx the parse tree
	 */
	void exitIndexType(MySqlParser.IndexTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#indexOption}.
	 * @param ctx the parse tree
	 */
	void enterIndexOption(MySqlParser.IndexOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#indexOption}.
	 * @param ctx the parse tree
	 */
	void exitIndexOption(MySqlParser.IndexOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#procedureParameter}.
	 * @param ctx the parse tree
	 */
	void enterProcedureParameter(MySqlParser.ProcedureParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#procedureParameter}.
	 * @param ctx the parse tree
	 */
	void exitProcedureParameter(MySqlParser.ProcedureParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#functionParameter}.
	 * @param ctx the parse tree
	 */
	void enterFunctionParameter(MySqlParser.FunctionParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#functionParameter}.
	 * @param ctx the parse tree
	 */
	void exitFunctionParameter(MySqlParser.FunctionParameterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code routineComment}
	 * labeled alternative in {@link MySqlParser#routineOption}.
	 * @param ctx the parse tree
	 */
	void enterRoutineComment(MySqlParser.RoutineCommentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code routineComment}
	 * labeled alternative in {@link MySqlParser#routineOption}.
	 * @param ctx the parse tree
	 */
	void exitRoutineComment(MySqlParser.RoutineCommentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code routineLanguage}
	 * labeled alternative in {@link MySqlParser#routineOption}.
	 * @param ctx the parse tree
	 */
	void enterRoutineLanguage(MySqlParser.RoutineLanguageContext ctx);
	/**
	 * Exit a parse tree produced by the {@code routineLanguage}
	 * labeled alternative in {@link MySqlParser#routineOption}.
	 * @param ctx the parse tree
	 */
	void exitRoutineLanguage(MySqlParser.RoutineLanguageContext ctx);
	/**
	 * Enter a parse tree produced by the {@code routineBehavior}
	 * labeled alternative in {@link MySqlParser#routineOption}.
	 * @param ctx the parse tree
	 */
	void enterRoutineBehavior(MySqlParser.RoutineBehaviorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code routineBehavior}
	 * labeled alternative in {@link MySqlParser#routineOption}.
	 * @param ctx the parse tree
	 */
	void exitRoutineBehavior(MySqlParser.RoutineBehaviorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code routineData}
	 * labeled alternative in {@link MySqlParser#routineOption}.
	 * @param ctx the parse tree
	 */
	void enterRoutineData(MySqlParser.RoutineDataContext ctx);
	/**
	 * Exit a parse tree produced by the {@code routineData}
	 * labeled alternative in {@link MySqlParser#routineOption}.
	 * @param ctx the parse tree
	 */
	void exitRoutineData(MySqlParser.RoutineDataContext ctx);
	/**
	 * Enter a parse tree produced by the {@code routineSecurity}
	 * labeled alternative in {@link MySqlParser#routineOption}.
	 * @param ctx the parse tree
	 */
	void enterRoutineSecurity(MySqlParser.RoutineSecurityContext ctx);
	/**
	 * Exit a parse tree produced by the {@code routineSecurity}
	 * labeled alternative in {@link MySqlParser#routineOption}.
	 * @param ctx the parse tree
	 */
	void exitRoutineSecurity(MySqlParser.RoutineSecurityContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#serverOption}.
	 * @param ctx the parse tree
	 */
	void enterServerOption(MySqlParser.ServerOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#serverOption}.
	 * @param ctx the parse tree
	 */
	void exitServerOption(MySqlParser.ServerOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createDefinitions}.
	 * @param ctx the parse tree
	 */
	void enterCreateDefinitions(MySqlParser.CreateDefinitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createDefinitions}.
	 * @param ctx the parse tree
	 */
	void exitCreateDefinitions(MySqlParser.CreateDefinitionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code columnDeclaration}
	 * labeled alternative in {@link MySqlParser#createDefinition}.
	 * @param ctx the parse tree
	 */
	void enterColumnDeclaration(MySqlParser.ColumnDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code columnDeclaration}
	 * labeled alternative in {@link MySqlParser#createDefinition}.
	 * @param ctx the parse tree
	 */
	void exitColumnDeclaration(MySqlParser.ColumnDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code constraintDeclaration}
	 * labeled alternative in {@link MySqlParser#createDefinition}.
	 * @param ctx the parse tree
	 */
	void enterConstraintDeclaration(MySqlParser.ConstraintDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code constraintDeclaration}
	 * labeled alternative in {@link MySqlParser#createDefinition}.
	 * @param ctx the parse tree
	 */
	void exitConstraintDeclaration(MySqlParser.ConstraintDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code indexDeclaration}
	 * labeled alternative in {@link MySqlParser#createDefinition}.
	 * @param ctx the parse tree
	 */
	void enterIndexDeclaration(MySqlParser.IndexDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code indexDeclaration}
	 * labeled alternative in {@link MySqlParser#createDefinition}.
	 * @param ctx the parse tree
	 */
	void exitIndexDeclaration(MySqlParser.IndexDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#columnDefinition}.
	 * @param ctx the parse tree
	 */
	void enterColumnDefinition(MySqlParser.ColumnDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#columnDefinition}.
	 * @param ctx the parse tree
	 */
	void exitColumnDefinition(MySqlParser.ColumnDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nullColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void enterNullColumnConstraint(MySqlParser.NullColumnConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nullColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void exitNullColumnConstraint(MySqlParser.NullColumnConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code defaultColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void enterDefaultColumnConstraint(MySqlParser.DefaultColumnConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code defaultColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void exitDefaultColumnConstraint(MySqlParser.DefaultColumnConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code autoIncrementColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void enterAutoIncrementColumnConstraint(MySqlParser.AutoIncrementColumnConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code autoIncrementColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void exitAutoIncrementColumnConstraint(MySqlParser.AutoIncrementColumnConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code primaryKeyColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryKeyColumnConstraint(MySqlParser.PrimaryKeyColumnConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code primaryKeyColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryKeyColumnConstraint(MySqlParser.PrimaryKeyColumnConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uniqueKeyColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void enterUniqueKeyColumnConstraint(MySqlParser.UniqueKeyColumnConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uniqueKeyColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void exitUniqueKeyColumnConstraint(MySqlParser.UniqueKeyColumnConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code commentColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void enterCommentColumnConstraint(MySqlParser.CommentColumnConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code commentColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void exitCommentColumnConstraint(MySqlParser.CommentColumnConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code formatColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void enterFormatColumnConstraint(MySqlParser.FormatColumnConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code formatColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void exitFormatColumnConstraint(MySqlParser.FormatColumnConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code storageColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void enterStorageColumnConstraint(MySqlParser.StorageColumnConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code storageColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void exitStorageColumnConstraint(MySqlParser.StorageColumnConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code referenceColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void enterReferenceColumnConstraint(MySqlParser.ReferenceColumnConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code referenceColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void exitReferenceColumnConstraint(MySqlParser.ReferenceColumnConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code collateColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void enterCollateColumnConstraint(MySqlParser.CollateColumnConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code collateColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void exitCollateColumnConstraint(MySqlParser.CollateColumnConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code generatedColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void enterGeneratedColumnConstraint(MySqlParser.GeneratedColumnConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code generatedColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void exitGeneratedColumnConstraint(MySqlParser.GeneratedColumnConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code serialDefaultColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void enterSerialDefaultColumnConstraint(MySqlParser.SerialDefaultColumnConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code serialDefaultColumnConstraint}
	 * labeled alternative in {@link MySqlParser#columnConstraint}.
	 * @param ctx the parse tree
	 */
	void exitSerialDefaultColumnConstraint(MySqlParser.SerialDefaultColumnConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code primaryKeyTableConstraint}
	 * labeled alternative in {@link MySqlParser#tableConstraint}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryKeyTableConstraint(MySqlParser.PrimaryKeyTableConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code primaryKeyTableConstraint}
	 * labeled alternative in {@link MySqlParser#tableConstraint}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryKeyTableConstraint(MySqlParser.PrimaryKeyTableConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code uniqueKeyTableConstraint}
	 * labeled alternative in {@link MySqlParser#tableConstraint}.
	 * @param ctx the parse tree
	 */
	void enterUniqueKeyTableConstraint(MySqlParser.UniqueKeyTableConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code uniqueKeyTableConstraint}
	 * labeled alternative in {@link MySqlParser#tableConstraint}.
	 * @param ctx the parse tree
	 */
	void exitUniqueKeyTableConstraint(MySqlParser.UniqueKeyTableConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code foreignKeyTableConstraint}
	 * labeled alternative in {@link MySqlParser#tableConstraint}.
	 * @param ctx the parse tree
	 */
	void enterForeignKeyTableConstraint(MySqlParser.ForeignKeyTableConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code foreignKeyTableConstraint}
	 * labeled alternative in {@link MySqlParser#tableConstraint}.
	 * @param ctx the parse tree
	 */
	void exitForeignKeyTableConstraint(MySqlParser.ForeignKeyTableConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code checkTableConstraint}
	 * labeled alternative in {@link MySqlParser#tableConstraint}.
	 * @param ctx the parse tree
	 */
	void enterCheckTableConstraint(MySqlParser.CheckTableConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code checkTableConstraint}
	 * labeled alternative in {@link MySqlParser#tableConstraint}.
	 * @param ctx the parse tree
	 */
	void exitCheckTableConstraint(MySqlParser.CheckTableConstraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#referenceDefinition}.
	 * @param ctx the parse tree
	 */
	void enterReferenceDefinition(MySqlParser.ReferenceDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#referenceDefinition}.
	 * @param ctx the parse tree
	 */
	void exitReferenceDefinition(MySqlParser.ReferenceDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#referenceAction}.
	 * @param ctx the parse tree
	 */
	void enterReferenceAction(MySqlParser.ReferenceActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#referenceAction}.
	 * @param ctx the parse tree
	 */
	void exitReferenceAction(MySqlParser.ReferenceActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#referenceControlType}.
	 * @param ctx the parse tree
	 */
	void enterReferenceControlType(MySqlParser.ReferenceControlTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#referenceControlType}.
	 * @param ctx the parse tree
	 */
	void exitReferenceControlType(MySqlParser.ReferenceControlTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleIndexDeclaration}
	 * labeled alternative in {@link MySqlParser#indexColumnDefinition}.
	 * @param ctx the parse tree
	 */
	void enterSimpleIndexDeclaration(MySqlParser.SimpleIndexDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleIndexDeclaration}
	 * labeled alternative in {@link MySqlParser#indexColumnDefinition}.
	 * @param ctx the parse tree
	 */
	void exitSimpleIndexDeclaration(MySqlParser.SimpleIndexDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code specialIndexDeclaration}
	 * labeled alternative in {@link MySqlParser#indexColumnDefinition}.
	 * @param ctx the parse tree
	 */
	void enterSpecialIndexDeclaration(MySqlParser.SpecialIndexDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code specialIndexDeclaration}
	 * labeled alternative in {@link MySqlParser#indexColumnDefinition}.
	 * @param ctx the parse tree
	 */
	void exitSpecialIndexDeclaration(MySqlParser.SpecialIndexDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionEngine}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionEngine(MySqlParser.TableOptionEngineContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionEngine}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionEngine(MySqlParser.TableOptionEngineContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionAutoIncrement}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionAutoIncrement(MySqlParser.TableOptionAutoIncrementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionAutoIncrement}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionAutoIncrement(MySqlParser.TableOptionAutoIncrementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionAverage}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionAverage(MySqlParser.TableOptionAverageContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionAverage}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionAverage(MySqlParser.TableOptionAverageContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionCharset}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionCharset(MySqlParser.TableOptionCharsetContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionCharset}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionCharset(MySqlParser.TableOptionCharsetContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionChecksum}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionChecksum(MySqlParser.TableOptionChecksumContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionChecksum}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionChecksum(MySqlParser.TableOptionChecksumContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionCollate}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionCollate(MySqlParser.TableOptionCollateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionCollate}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionCollate(MySqlParser.TableOptionCollateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionComment}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionComment(MySqlParser.TableOptionCommentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionComment}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionComment(MySqlParser.TableOptionCommentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionCompression}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionCompression(MySqlParser.TableOptionCompressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionCompression}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionCompression(MySqlParser.TableOptionCompressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionConnection}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionConnection(MySqlParser.TableOptionConnectionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionConnection}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionConnection(MySqlParser.TableOptionConnectionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionDataDirectory}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionDataDirectory(MySqlParser.TableOptionDataDirectoryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionDataDirectory}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionDataDirectory(MySqlParser.TableOptionDataDirectoryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionDelay}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionDelay(MySqlParser.TableOptionDelayContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionDelay}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionDelay(MySqlParser.TableOptionDelayContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionEncryption}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionEncryption(MySqlParser.TableOptionEncryptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionEncryption}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionEncryption(MySqlParser.TableOptionEncryptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionIndexDirectory}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionIndexDirectory(MySqlParser.TableOptionIndexDirectoryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionIndexDirectory}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionIndexDirectory(MySqlParser.TableOptionIndexDirectoryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionInsertMethod}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionInsertMethod(MySqlParser.TableOptionInsertMethodContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionInsertMethod}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionInsertMethod(MySqlParser.TableOptionInsertMethodContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionKeyBlockSize}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionKeyBlockSize(MySqlParser.TableOptionKeyBlockSizeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionKeyBlockSize}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionKeyBlockSize(MySqlParser.TableOptionKeyBlockSizeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionMaxRows}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionMaxRows(MySqlParser.TableOptionMaxRowsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionMaxRows}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionMaxRows(MySqlParser.TableOptionMaxRowsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionMinRows}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionMinRows(MySqlParser.TableOptionMinRowsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionMinRows}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionMinRows(MySqlParser.TableOptionMinRowsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionPackKeys}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionPackKeys(MySqlParser.TableOptionPackKeysContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionPackKeys}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionPackKeys(MySqlParser.TableOptionPackKeysContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionPassword}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionPassword(MySqlParser.TableOptionPasswordContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionPassword}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionPassword(MySqlParser.TableOptionPasswordContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionRowFormat}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionRowFormat(MySqlParser.TableOptionRowFormatContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionRowFormat}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionRowFormat(MySqlParser.TableOptionRowFormatContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionRecalculation}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionRecalculation(MySqlParser.TableOptionRecalculationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionRecalculation}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionRecalculation(MySqlParser.TableOptionRecalculationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionPersistent}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionPersistent(MySqlParser.TableOptionPersistentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionPersistent}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionPersistent(MySqlParser.TableOptionPersistentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionSamplePage}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionSamplePage(MySqlParser.TableOptionSamplePageContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionSamplePage}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionSamplePage(MySqlParser.TableOptionSamplePageContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionTablespace}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionTablespace(MySqlParser.TableOptionTablespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionTablespace}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionTablespace(MySqlParser.TableOptionTablespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableOptionUnion}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void enterTableOptionUnion(MySqlParser.TableOptionUnionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableOptionUnion}
	 * labeled alternative in {@link MySqlParser#tableOption}.
	 * @param ctx the parse tree
	 */
	void exitTableOptionUnion(MySqlParser.TableOptionUnionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#tablespaceStorage}.
	 * @param ctx the parse tree
	 */
	void enterTablespaceStorage(MySqlParser.TablespaceStorageContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#tablespaceStorage}.
	 * @param ctx the parse tree
	 */
	void exitTablespaceStorage(MySqlParser.TablespaceStorageContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#partitionDefinitions}.
	 * @param ctx the parse tree
	 */
	void enterPartitionDefinitions(MySqlParser.PartitionDefinitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#partitionDefinitions}.
	 * @param ctx the parse tree
	 */
	void exitPartitionDefinitions(MySqlParser.PartitionDefinitionsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionFunctionHash}
	 * labeled alternative in {@link MySqlParser#partitionFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterPartitionFunctionHash(MySqlParser.PartitionFunctionHashContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionFunctionHash}
	 * labeled alternative in {@link MySqlParser#partitionFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitPartitionFunctionHash(MySqlParser.PartitionFunctionHashContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionFunctionKey}
	 * labeled alternative in {@link MySqlParser#partitionFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterPartitionFunctionKey(MySqlParser.PartitionFunctionKeyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionFunctionKey}
	 * labeled alternative in {@link MySqlParser#partitionFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitPartitionFunctionKey(MySqlParser.PartitionFunctionKeyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionFunctionRange}
	 * labeled alternative in {@link MySqlParser#partitionFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterPartitionFunctionRange(MySqlParser.PartitionFunctionRangeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionFunctionRange}
	 * labeled alternative in {@link MySqlParser#partitionFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitPartitionFunctionRange(MySqlParser.PartitionFunctionRangeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionFunctionList}
	 * labeled alternative in {@link MySqlParser#partitionFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterPartitionFunctionList(MySqlParser.PartitionFunctionListContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionFunctionList}
	 * labeled alternative in {@link MySqlParser#partitionFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitPartitionFunctionList(MySqlParser.PartitionFunctionListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subPartitionFunctionHash}
	 * labeled alternative in {@link MySqlParser#subpartitionFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterSubPartitionFunctionHash(MySqlParser.SubPartitionFunctionHashContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subPartitionFunctionHash}
	 * labeled alternative in {@link MySqlParser#subpartitionFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitSubPartitionFunctionHash(MySqlParser.SubPartitionFunctionHashContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subPartitionFunctionKey}
	 * labeled alternative in {@link MySqlParser#subpartitionFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterSubPartitionFunctionKey(MySqlParser.SubPartitionFunctionKeyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subPartitionFunctionKey}
	 * labeled alternative in {@link MySqlParser#subpartitionFunctionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitSubPartitionFunctionKey(MySqlParser.SubPartitionFunctionKeyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionComparision}
	 * labeled alternative in {@link MySqlParser#partitionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterPartitionComparision(MySqlParser.PartitionComparisionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionComparision}
	 * labeled alternative in {@link MySqlParser#partitionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitPartitionComparision(MySqlParser.PartitionComparisionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionListAtom}
	 * labeled alternative in {@link MySqlParser#partitionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterPartitionListAtom(MySqlParser.PartitionListAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionListAtom}
	 * labeled alternative in {@link MySqlParser#partitionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitPartitionListAtom(MySqlParser.PartitionListAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionListVector}
	 * labeled alternative in {@link MySqlParser#partitionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterPartitionListVector(MySqlParser.PartitionListVectorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionListVector}
	 * labeled alternative in {@link MySqlParser#partitionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitPartitionListVector(MySqlParser.PartitionListVectorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionSimple}
	 * labeled alternative in {@link MySqlParser#partitionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterPartitionSimple(MySqlParser.PartitionSimpleContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionSimple}
	 * labeled alternative in {@link MySqlParser#partitionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitPartitionSimple(MySqlParser.PartitionSimpleContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#partitionDefinerAtom}.
	 * @param ctx the parse tree
	 */
	void enterPartitionDefinerAtom(MySqlParser.PartitionDefinerAtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#partitionDefinerAtom}.
	 * @param ctx the parse tree
	 */
	void exitPartitionDefinerAtom(MySqlParser.PartitionDefinerAtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#partitionDefinerVector}.
	 * @param ctx the parse tree
	 */
	void enterPartitionDefinerVector(MySqlParser.PartitionDefinerVectorContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#partitionDefinerVector}.
	 * @param ctx the parse tree
	 */
	void exitPartitionDefinerVector(MySqlParser.PartitionDefinerVectorContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#subpartitionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterSubpartitionDefinition(MySqlParser.SubpartitionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#subpartitionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitSubpartitionDefinition(MySqlParser.SubpartitionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionOptionEngine}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void enterPartitionOptionEngine(MySqlParser.PartitionOptionEngineContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionOptionEngine}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void exitPartitionOptionEngine(MySqlParser.PartitionOptionEngineContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionOptionComment}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void enterPartitionOptionComment(MySqlParser.PartitionOptionCommentContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionOptionComment}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void exitPartitionOptionComment(MySqlParser.PartitionOptionCommentContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionOptionDataDirectory}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void enterPartitionOptionDataDirectory(MySqlParser.PartitionOptionDataDirectoryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionOptionDataDirectory}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void exitPartitionOptionDataDirectory(MySqlParser.PartitionOptionDataDirectoryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionOptionIndexDirectory}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void enterPartitionOptionIndexDirectory(MySqlParser.PartitionOptionIndexDirectoryContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionOptionIndexDirectory}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void exitPartitionOptionIndexDirectory(MySqlParser.PartitionOptionIndexDirectoryContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionOptionMaxRows}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void enterPartitionOptionMaxRows(MySqlParser.PartitionOptionMaxRowsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionOptionMaxRows}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void exitPartitionOptionMaxRows(MySqlParser.PartitionOptionMaxRowsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionOptionMinRows}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void enterPartitionOptionMinRows(MySqlParser.PartitionOptionMinRowsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionOptionMinRows}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void exitPartitionOptionMinRows(MySqlParser.PartitionOptionMinRowsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionOptionTablespace}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void enterPartitionOptionTablespace(MySqlParser.PartitionOptionTablespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionOptionTablespace}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void exitPartitionOptionTablespace(MySqlParser.PartitionOptionTablespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code partitionOptionNodeGroup}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void enterPartitionOptionNodeGroup(MySqlParser.PartitionOptionNodeGroupContext ctx);
	/**
	 * Exit a parse tree produced by the {@code partitionOptionNodeGroup}
	 * labeled alternative in {@link MySqlParser#partitionOption}.
	 * @param ctx the parse tree
	 */
	void exitPartitionOptionNodeGroup(MySqlParser.PartitionOptionNodeGroupContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterSimpleDatabase}
	 * labeled alternative in {@link MySqlParser#alterDatabase}.
	 * @param ctx the parse tree
	 */
	void enterAlterSimpleDatabase(MySqlParser.AlterSimpleDatabaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterSimpleDatabase}
	 * labeled alternative in {@link MySqlParser#alterDatabase}.
	 * @param ctx the parse tree
	 */
	void exitAlterSimpleDatabase(MySqlParser.AlterSimpleDatabaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterUpgradeName}
	 * labeled alternative in {@link MySqlParser#alterDatabase}.
	 * @param ctx the parse tree
	 */
	void enterAlterUpgradeName(MySqlParser.AlterUpgradeNameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterUpgradeName}
	 * labeled alternative in {@link MySqlParser#alterDatabase}.
	 * @param ctx the parse tree
	 */
	void exitAlterUpgradeName(MySqlParser.AlterUpgradeNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#alterEvent}.
	 * @param ctx the parse tree
	 */
	void enterAlterEvent(MySqlParser.AlterEventContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#alterEvent}.
	 * @param ctx the parse tree
	 */
	void exitAlterEvent(MySqlParser.AlterEventContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#alterFunction}.
	 * @param ctx the parse tree
	 */
	void enterAlterFunction(MySqlParser.AlterFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#alterFunction}.
	 * @param ctx the parse tree
	 */
	void exitAlterFunction(MySqlParser.AlterFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#alterInstance}.
	 * @param ctx the parse tree
	 */
	void enterAlterInstance(MySqlParser.AlterInstanceContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#alterInstance}.
	 * @param ctx the parse tree
	 */
	void exitAlterInstance(MySqlParser.AlterInstanceContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#alterLogfileGroup}.
	 * @param ctx the parse tree
	 */
	void enterAlterLogfileGroup(MySqlParser.AlterLogfileGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#alterLogfileGroup}.
	 * @param ctx the parse tree
	 */
	void exitAlterLogfileGroup(MySqlParser.AlterLogfileGroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#alterProcedure}.
	 * @param ctx the parse tree
	 */
	void enterAlterProcedure(MySqlParser.AlterProcedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#alterProcedure}.
	 * @param ctx the parse tree
	 */
	void exitAlterProcedure(MySqlParser.AlterProcedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#alterServer}.
	 * @param ctx the parse tree
	 */
	void enterAlterServer(MySqlParser.AlterServerContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#alterServer}.
	 * @param ctx the parse tree
	 */
	void exitAlterServer(MySqlParser.AlterServerContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#alterTable}.
	 * @param ctx the parse tree
	 */
	void enterAlterTable(MySqlParser.AlterTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#alterTable}.
	 * @param ctx the parse tree
	 */
	void exitAlterTable(MySqlParser.AlterTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#alterTablespace}.
	 * @param ctx the parse tree
	 */
	void enterAlterTablespace(MySqlParser.AlterTablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#alterTablespace}.
	 * @param ctx the parse tree
	 */
	void exitAlterTablespace(MySqlParser.AlterTablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#alterView}.
	 * @param ctx the parse tree
	 */
	void enterAlterView(MySqlParser.AlterViewContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#alterView}.
	 * @param ctx the parse tree
	 */
	void exitAlterView(MySqlParser.AlterViewContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByTableOption}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByTableOption(MySqlParser.AlterByTableOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByTableOption}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByTableOption(MySqlParser.AlterByTableOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByAddColumn}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByAddColumn(MySqlParser.AlterByAddColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByAddColumn}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByAddColumn(MySqlParser.AlterByAddColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByAddColumns}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByAddColumns(MySqlParser.AlterByAddColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByAddColumns}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByAddColumns(MySqlParser.AlterByAddColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByAddIndex}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByAddIndex(MySqlParser.AlterByAddIndexContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByAddIndex}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByAddIndex(MySqlParser.AlterByAddIndexContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByAddPrimaryKey}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByAddPrimaryKey(MySqlParser.AlterByAddPrimaryKeyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByAddPrimaryKey}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByAddPrimaryKey(MySqlParser.AlterByAddPrimaryKeyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByAddUniqueKey}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByAddUniqueKey(MySqlParser.AlterByAddUniqueKeyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByAddUniqueKey}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByAddUniqueKey(MySqlParser.AlterByAddUniqueKeyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByAddSpecialIndex}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByAddSpecialIndex(MySqlParser.AlterByAddSpecialIndexContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByAddSpecialIndex}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByAddSpecialIndex(MySqlParser.AlterByAddSpecialIndexContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByAddForeignKey}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByAddForeignKey(MySqlParser.AlterByAddForeignKeyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByAddForeignKey}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByAddForeignKey(MySqlParser.AlterByAddForeignKeyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByAddCheckTableConstraint}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByAddCheckTableConstraint(MySqlParser.AlterByAddCheckTableConstraintContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByAddCheckTableConstraint}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByAddCheckTableConstraint(MySqlParser.AlterByAddCheckTableConstraintContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterBySetAlgorithm}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterBySetAlgorithm(MySqlParser.AlterBySetAlgorithmContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterBySetAlgorithm}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterBySetAlgorithm(MySqlParser.AlterBySetAlgorithmContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByChangeDefault}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByChangeDefault(MySqlParser.AlterByChangeDefaultContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByChangeDefault}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByChangeDefault(MySqlParser.AlterByChangeDefaultContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByChangeColumn}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByChangeColumn(MySqlParser.AlterByChangeColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByChangeColumn}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByChangeColumn(MySqlParser.AlterByChangeColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByRenameColumn}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByRenameColumn(MySqlParser.AlterByRenameColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByRenameColumn}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByRenameColumn(MySqlParser.AlterByRenameColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByLock}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByLock(MySqlParser.AlterByLockContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByLock}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByLock(MySqlParser.AlterByLockContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByModifyColumn}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByModifyColumn(MySqlParser.AlterByModifyColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByModifyColumn}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByModifyColumn(MySqlParser.AlterByModifyColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByDropColumn}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByDropColumn(MySqlParser.AlterByDropColumnContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByDropColumn}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByDropColumn(MySqlParser.AlterByDropColumnContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByDropPrimaryKey}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByDropPrimaryKey(MySqlParser.AlterByDropPrimaryKeyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByDropPrimaryKey}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByDropPrimaryKey(MySqlParser.AlterByDropPrimaryKeyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByRenameIndex}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByRenameIndex(MySqlParser.AlterByRenameIndexContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByRenameIndex}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByRenameIndex(MySqlParser.AlterByRenameIndexContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByDropIndex}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByDropIndex(MySqlParser.AlterByDropIndexContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByDropIndex}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByDropIndex(MySqlParser.AlterByDropIndexContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByDropForeignKey}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByDropForeignKey(MySqlParser.AlterByDropForeignKeyContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByDropForeignKey}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByDropForeignKey(MySqlParser.AlterByDropForeignKeyContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByDisableKeys}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByDisableKeys(MySqlParser.AlterByDisableKeysContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByDisableKeys}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByDisableKeys(MySqlParser.AlterByDisableKeysContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByEnableKeys}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByEnableKeys(MySqlParser.AlterByEnableKeysContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByEnableKeys}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByEnableKeys(MySqlParser.AlterByEnableKeysContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByRename}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByRename(MySqlParser.AlterByRenameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByRename}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByRename(MySqlParser.AlterByRenameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByOrder}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByOrder(MySqlParser.AlterByOrderContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByOrder}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByOrder(MySqlParser.AlterByOrderContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByConvertCharset}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByConvertCharset(MySqlParser.AlterByConvertCharsetContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByConvertCharset}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByConvertCharset(MySqlParser.AlterByConvertCharsetContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByDefaultCharset}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByDefaultCharset(MySqlParser.AlterByDefaultCharsetContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByDefaultCharset}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByDefaultCharset(MySqlParser.AlterByDefaultCharsetContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByDiscardTablespace}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByDiscardTablespace(MySqlParser.AlterByDiscardTablespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByDiscardTablespace}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByDiscardTablespace(MySqlParser.AlterByDiscardTablespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByImportTablespace}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByImportTablespace(MySqlParser.AlterByImportTablespaceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByImportTablespace}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByImportTablespace(MySqlParser.AlterByImportTablespaceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByForce}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByForce(MySqlParser.AlterByForceContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByForce}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByForce(MySqlParser.AlterByForceContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByValidate}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByValidate(MySqlParser.AlterByValidateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByValidate}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByValidate(MySqlParser.AlterByValidateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByAddPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByAddPartition(MySqlParser.AlterByAddPartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByAddPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByAddPartition(MySqlParser.AlterByAddPartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByDropPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByDropPartition(MySqlParser.AlterByDropPartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByDropPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByDropPartition(MySqlParser.AlterByDropPartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByDiscardPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByDiscardPartition(MySqlParser.AlterByDiscardPartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByDiscardPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByDiscardPartition(MySqlParser.AlterByDiscardPartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByImportPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByImportPartition(MySqlParser.AlterByImportPartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByImportPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByImportPartition(MySqlParser.AlterByImportPartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByTruncatePartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByTruncatePartition(MySqlParser.AlterByTruncatePartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByTruncatePartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByTruncatePartition(MySqlParser.AlterByTruncatePartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByCoalescePartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByCoalescePartition(MySqlParser.AlterByCoalescePartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByCoalescePartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByCoalescePartition(MySqlParser.AlterByCoalescePartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByReorganizePartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByReorganizePartition(MySqlParser.AlterByReorganizePartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByReorganizePartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByReorganizePartition(MySqlParser.AlterByReorganizePartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByExchangePartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByExchangePartition(MySqlParser.AlterByExchangePartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByExchangePartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByExchangePartition(MySqlParser.AlterByExchangePartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByAnalyzePartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByAnalyzePartition(MySqlParser.AlterByAnalyzePartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByAnalyzePartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByAnalyzePartition(MySqlParser.AlterByAnalyzePartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByCheckPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByCheckPartition(MySqlParser.AlterByCheckPartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByCheckPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByCheckPartition(MySqlParser.AlterByCheckPartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByOptimizePartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByOptimizePartition(MySqlParser.AlterByOptimizePartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByOptimizePartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByOptimizePartition(MySqlParser.AlterByOptimizePartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByRebuildPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByRebuildPartition(MySqlParser.AlterByRebuildPartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByRebuildPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByRebuildPartition(MySqlParser.AlterByRebuildPartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByRepairPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByRepairPartition(MySqlParser.AlterByRepairPartitionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByRepairPartition}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByRepairPartition(MySqlParser.AlterByRepairPartitionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByRemovePartitioning}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByRemovePartitioning(MySqlParser.AlterByRemovePartitioningContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByRemovePartitioning}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByRemovePartitioning(MySqlParser.AlterByRemovePartitioningContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterByUpgradePartitioning}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void enterAlterByUpgradePartitioning(MySqlParser.AlterByUpgradePartitioningContext ctx);
	/**
	 * Exit a parse tree produced by the {@code alterByUpgradePartitioning}
	 * labeled alternative in {@link MySqlParser#alterSpecification}.
	 * @param ctx the parse tree
	 */
	void exitAlterByUpgradePartitioning(MySqlParser.AlterByUpgradePartitioningContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dropDatabase}.
	 * @param ctx the parse tree
	 */
	void enterDropDatabase(MySqlParser.DropDatabaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dropDatabase}.
	 * @param ctx the parse tree
	 */
	void exitDropDatabase(MySqlParser.DropDatabaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dropEvent}.
	 * @param ctx the parse tree
	 */
	void enterDropEvent(MySqlParser.DropEventContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dropEvent}.
	 * @param ctx the parse tree
	 */
	void exitDropEvent(MySqlParser.DropEventContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dropIndex}.
	 * @param ctx the parse tree
	 */
	void enterDropIndex(MySqlParser.DropIndexContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dropIndex}.
	 * @param ctx the parse tree
	 */
	void exitDropIndex(MySqlParser.DropIndexContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dropLogfileGroup}.
	 * @param ctx the parse tree
	 */
	void enterDropLogfileGroup(MySqlParser.DropLogfileGroupContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dropLogfileGroup}.
	 * @param ctx the parse tree
	 */
	void exitDropLogfileGroup(MySqlParser.DropLogfileGroupContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dropProcedure}.
	 * @param ctx the parse tree
	 */
	void enterDropProcedure(MySqlParser.DropProcedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dropProcedure}.
	 * @param ctx the parse tree
	 */
	void exitDropProcedure(MySqlParser.DropProcedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dropFunction}.
	 * @param ctx the parse tree
	 */
	void enterDropFunction(MySqlParser.DropFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dropFunction}.
	 * @param ctx the parse tree
	 */
	void exitDropFunction(MySqlParser.DropFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dropServer}.
	 * @param ctx the parse tree
	 */
	void enterDropServer(MySqlParser.DropServerContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dropServer}.
	 * @param ctx the parse tree
	 */
	void exitDropServer(MySqlParser.DropServerContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dropTable}.
	 * @param ctx the parse tree
	 */
	void enterDropTable(MySqlParser.DropTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dropTable}.
	 * @param ctx the parse tree
	 */
	void exitDropTable(MySqlParser.DropTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dropTablespace}.
	 * @param ctx the parse tree
	 */
	void enterDropTablespace(MySqlParser.DropTablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dropTablespace}.
	 * @param ctx the parse tree
	 */
	void exitDropTablespace(MySqlParser.DropTablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dropTrigger}.
	 * @param ctx the parse tree
	 */
	void enterDropTrigger(MySqlParser.DropTriggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dropTrigger}.
	 * @param ctx the parse tree
	 */
	void exitDropTrigger(MySqlParser.DropTriggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dropView}.
	 * @param ctx the parse tree
	 */
	void enterDropView(MySqlParser.DropViewContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dropView}.
	 * @param ctx the parse tree
	 */
	void exitDropView(MySqlParser.DropViewContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#renameTable}.
	 * @param ctx the parse tree
	 */
	void enterRenameTable(MySqlParser.RenameTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#renameTable}.
	 * @param ctx the parse tree
	 */
	void exitRenameTable(MySqlParser.RenameTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#renameTableClause}.
	 * @param ctx the parse tree
	 */
	void enterRenameTableClause(MySqlParser.RenameTableClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#renameTableClause}.
	 * @param ctx the parse tree
	 */
	void exitRenameTableClause(MySqlParser.RenameTableClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#truncateTable}.
	 * @param ctx the parse tree
	 */
	void enterTruncateTable(MySqlParser.TruncateTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#truncateTable}.
	 * @param ctx the parse tree
	 */
	void exitTruncateTable(MySqlParser.TruncateTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#callStatement}.
	 * @param ctx the parse tree
	 */
	void enterCallStatement(MySqlParser.CallStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#callStatement}.
	 * @param ctx the parse tree
	 */
	void exitCallStatement(MySqlParser.CallStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#deleteStatement}.
	 * @param ctx the parse tree
	 */
	void enterDeleteStatement(MySqlParser.DeleteStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#deleteStatement}.
	 * @param ctx the parse tree
	 */
	void exitDeleteStatement(MySqlParser.DeleteStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#doStatement}.
	 * @param ctx the parse tree
	 */
	void enterDoStatement(MySqlParser.DoStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#doStatement}.
	 * @param ctx the parse tree
	 */
	void exitDoStatement(MySqlParser.DoStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#handlerStatement}.
	 * @param ctx the parse tree
	 */
	void enterHandlerStatement(MySqlParser.HandlerStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#handlerStatement}.
	 * @param ctx the parse tree
	 */
	void exitHandlerStatement(MySqlParser.HandlerStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#insertStatement}.
	 * @param ctx the parse tree
	 */
	void enterInsertStatement(MySqlParser.InsertStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#insertStatement}.
	 * @param ctx the parse tree
	 */
	void exitInsertStatement(MySqlParser.InsertStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#loadDataStatement}.
	 * @param ctx the parse tree
	 */
	void enterLoadDataStatement(MySqlParser.LoadDataStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#loadDataStatement}.
	 * @param ctx the parse tree
	 */
	void exitLoadDataStatement(MySqlParser.LoadDataStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#loadXmlStatement}.
	 * @param ctx the parse tree
	 */
	void enterLoadXmlStatement(MySqlParser.LoadXmlStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#loadXmlStatement}.
	 * @param ctx the parse tree
	 */
	void exitLoadXmlStatement(MySqlParser.LoadXmlStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#replaceStatement}.
	 * @param ctx the parse tree
	 */
	void enterReplaceStatement(MySqlParser.ReplaceStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#replaceStatement}.
	 * @param ctx the parse tree
	 */
	void exitReplaceStatement(MySqlParser.ReplaceStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleSelect}
	 * labeled alternative in {@link MySqlParser#selectStatement}.
	 * @param ctx the parse tree
	 */
	void enterSimpleSelect(MySqlParser.SimpleSelectContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleSelect}
	 * labeled alternative in {@link MySqlParser#selectStatement}.
	 * @param ctx the parse tree
	 */
	void exitSimpleSelect(MySqlParser.SimpleSelectContext ctx);
	/**
	 * Enter a parse tree produced by the {@code parenthesisSelect}
	 * labeled alternative in {@link MySqlParser#selectStatement}.
	 * @param ctx the parse tree
	 */
	void enterParenthesisSelect(MySqlParser.ParenthesisSelectContext ctx);
	/**
	 * Exit a parse tree produced by the {@code parenthesisSelect}
	 * labeled alternative in {@link MySqlParser#selectStatement}.
	 * @param ctx the parse tree
	 */
	void exitParenthesisSelect(MySqlParser.ParenthesisSelectContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unionSelect}
	 * labeled alternative in {@link MySqlParser#selectStatement}.
	 * @param ctx the parse tree
	 */
	void enterUnionSelect(MySqlParser.UnionSelectContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unionSelect}
	 * labeled alternative in {@link MySqlParser#selectStatement}.
	 * @param ctx the parse tree
	 */
	void exitUnionSelect(MySqlParser.UnionSelectContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unionParenthesisSelect}
	 * labeled alternative in {@link MySqlParser#selectStatement}.
	 * @param ctx the parse tree
	 */
	void enterUnionParenthesisSelect(MySqlParser.UnionParenthesisSelectContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unionParenthesisSelect}
	 * labeled alternative in {@link MySqlParser#selectStatement}.
	 * @param ctx the parse tree
	 */
	void exitUnionParenthesisSelect(MySqlParser.UnionParenthesisSelectContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#updateStatement}.
	 * @param ctx the parse tree
	 */
	void enterUpdateStatement(MySqlParser.UpdateStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#updateStatement}.
	 * @param ctx the parse tree
	 */
	void exitUpdateStatement(MySqlParser.UpdateStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#insertStatementValue}.
	 * @param ctx the parse tree
	 */
	void enterInsertStatementValue(MySqlParser.InsertStatementValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#insertStatementValue}.
	 * @param ctx the parse tree
	 */
	void exitInsertStatementValue(MySqlParser.InsertStatementValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#updatedElement}.
	 * @param ctx the parse tree
	 */
	void enterUpdatedElement(MySqlParser.UpdatedElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#updatedElement}.
	 * @param ctx the parse tree
	 */
	void exitUpdatedElement(MySqlParser.UpdatedElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#assignmentField}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentField(MySqlParser.AssignmentFieldContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#assignmentField}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentField(MySqlParser.AssignmentFieldContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#lockClause}.
	 * @param ctx the parse tree
	 */
	void enterLockClause(MySqlParser.LockClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#lockClause}.
	 * @param ctx the parse tree
	 */
	void exitLockClause(MySqlParser.LockClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#singleDeleteStatement}.
	 * @param ctx the parse tree
	 */
	void enterSingleDeleteStatement(MySqlParser.SingleDeleteStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#singleDeleteStatement}.
	 * @param ctx the parse tree
	 */
	void exitSingleDeleteStatement(MySqlParser.SingleDeleteStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#multipleDeleteStatement}.
	 * @param ctx the parse tree
	 */
	void enterMultipleDeleteStatement(MySqlParser.MultipleDeleteStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#multipleDeleteStatement}.
	 * @param ctx the parse tree
	 */
	void exitMultipleDeleteStatement(MySqlParser.MultipleDeleteStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#handlerOpenStatement}.
	 * @param ctx the parse tree
	 */
	void enterHandlerOpenStatement(MySqlParser.HandlerOpenStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#handlerOpenStatement}.
	 * @param ctx the parse tree
	 */
	void exitHandlerOpenStatement(MySqlParser.HandlerOpenStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#handlerReadIndexStatement}.
	 * @param ctx the parse tree
	 */
	void enterHandlerReadIndexStatement(MySqlParser.HandlerReadIndexStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#handlerReadIndexStatement}.
	 * @param ctx the parse tree
	 */
	void exitHandlerReadIndexStatement(MySqlParser.HandlerReadIndexStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#handlerReadStatement}.
	 * @param ctx the parse tree
	 */
	void enterHandlerReadStatement(MySqlParser.HandlerReadStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#handlerReadStatement}.
	 * @param ctx the parse tree
	 */
	void exitHandlerReadStatement(MySqlParser.HandlerReadStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#handlerCloseStatement}.
	 * @param ctx the parse tree
	 */
	void enterHandlerCloseStatement(MySqlParser.HandlerCloseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#handlerCloseStatement}.
	 * @param ctx the parse tree
	 */
	void exitHandlerCloseStatement(MySqlParser.HandlerCloseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#singleUpdateStatement}.
	 * @param ctx the parse tree
	 */
	void enterSingleUpdateStatement(MySqlParser.SingleUpdateStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#singleUpdateStatement}.
	 * @param ctx the parse tree
	 */
	void exitSingleUpdateStatement(MySqlParser.SingleUpdateStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#multipleUpdateStatement}.
	 * @param ctx the parse tree
	 */
	void enterMultipleUpdateStatement(MySqlParser.MultipleUpdateStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#multipleUpdateStatement}.
	 * @param ctx the parse tree
	 */
	void exitMultipleUpdateStatement(MySqlParser.MultipleUpdateStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#orderByClause}.
	 * @param ctx the parse tree
	 */
	void enterOrderByClause(MySqlParser.OrderByClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#orderByClause}.
	 * @param ctx the parse tree
	 */
	void exitOrderByClause(MySqlParser.OrderByClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#orderByExpression}.
	 * @param ctx the parse tree
	 */
	void enterOrderByExpression(MySqlParser.OrderByExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#orderByExpression}.
	 * @param ctx the parse tree
	 */
	void exitOrderByExpression(MySqlParser.OrderByExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#tableSources}.
	 * @param ctx the parse tree
	 */
	void enterTableSources(MySqlParser.TableSourcesContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#tableSources}.
	 * @param ctx the parse tree
	 */
	void exitTableSources(MySqlParser.TableSourcesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableSourceBase}
	 * labeled alternative in {@link MySqlParser#tableSource}.
	 * @param ctx the parse tree
	 */
	void enterTableSourceBase(MySqlParser.TableSourceBaseContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableSourceBase}
	 * labeled alternative in {@link MySqlParser#tableSource}.
	 * @param ctx the parse tree
	 */
	void exitTableSourceBase(MySqlParser.TableSourceBaseContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableSourceNested}
	 * labeled alternative in {@link MySqlParser#tableSource}.
	 * @param ctx the parse tree
	 */
	void enterTableSourceNested(MySqlParser.TableSourceNestedContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableSourceNested}
	 * labeled alternative in {@link MySqlParser#tableSource}.
	 * @param ctx the parse tree
	 */
	void exitTableSourceNested(MySqlParser.TableSourceNestedContext ctx);
	/**
	 * Enter a parse tree produced by the {@code atomTableItem}
	 * labeled alternative in {@link MySqlParser#tableSourceItem}.
	 * @param ctx the parse tree
	 */
	void enterAtomTableItem(MySqlParser.AtomTableItemContext ctx);
	/**
	 * Exit a parse tree produced by the {@code atomTableItem}
	 * labeled alternative in {@link MySqlParser#tableSourceItem}.
	 * @param ctx the parse tree
	 */
	void exitAtomTableItem(MySqlParser.AtomTableItemContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryTableItem}
	 * labeled alternative in {@link MySqlParser#tableSourceItem}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryTableItem(MySqlParser.SubqueryTableItemContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryTableItem}
	 * labeled alternative in {@link MySqlParser#tableSourceItem}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryTableItem(MySqlParser.SubqueryTableItemContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableSourcesItem}
	 * labeled alternative in {@link MySqlParser#tableSourceItem}.
	 * @param ctx the parse tree
	 */
	void enterTableSourcesItem(MySqlParser.TableSourcesItemContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableSourcesItem}
	 * labeled alternative in {@link MySqlParser#tableSourceItem}.
	 * @param ctx the parse tree
	 */
	void exitTableSourcesItem(MySqlParser.TableSourcesItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#indexHint}.
	 * @param ctx the parse tree
	 */
	void enterIndexHint(MySqlParser.IndexHintContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#indexHint}.
	 * @param ctx the parse tree
	 */
	void exitIndexHint(MySqlParser.IndexHintContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#indexHintType}.
	 * @param ctx the parse tree
	 */
	void enterIndexHintType(MySqlParser.IndexHintTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#indexHintType}.
	 * @param ctx the parse tree
	 */
	void exitIndexHintType(MySqlParser.IndexHintTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code innerJoin}
	 * labeled alternative in {@link MySqlParser#joinPart}.
	 * @param ctx the parse tree
	 */
	void enterInnerJoin(MySqlParser.InnerJoinContext ctx);
	/**
	 * Exit a parse tree produced by the {@code innerJoin}
	 * labeled alternative in {@link MySqlParser#joinPart}.
	 * @param ctx the parse tree
	 */
	void exitInnerJoin(MySqlParser.InnerJoinContext ctx);
	/**
	 * Enter a parse tree produced by the {@code straightJoin}
	 * labeled alternative in {@link MySqlParser#joinPart}.
	 * @param ctx the parse tree
	 */
	void enterStraightJoin(MySqlParser.StraightJoinContext ctx);
	/**
	 * Exit a parse tree produced by the {@code straightJoin}
	 * labeled alternative in {@link MySqlParser#joinPart}.
	 * @param ctx the parse tree
	 */
	void exitStraightJoin(MySqlParser.StraightJoinContext ctx);
	/**
	 * Enter a parse tree produced by the {@code outerJoin}
	 * labeled alternative in {@link MySqlParser#joinPart}.
	 * @param ctx the parse tree
	 */
	void enterOuterJoin(MySqlParser.OuterJoinContext ctx);
	/**
	 * Exit a parse tree produced by the {@code outerJoin}
	 * labeled alternative in {@link MySqlParser#joinPart}.
	 * @param ctx the parse tree
	 */
	void exitOuterJoin(MySqlParser.OuterJoinContext ctx);
	/**
	 * Enter a parse tree produced by the {@code naturalJoin}
	 * labeled alternative in {@link MySqlParser#joinPart}.
	 * @param ctx the parse tree
	 */
	void enterNaturalJoin(MySqlParser.NaturalJoinContext ctx);
	/**
	 * Exit a parse tree produced by the {@code naturalJoin}
	 * labeled alternative in {@link MySqlParser#joinPart}.
	 * @param ctx the parse tree
	 */
	void exitNaturalJoin(MySqlParser.NaturalJoinContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#queryExpression}.
	 * @param ctx the parse tree
	 */
	void enterQueryExpression(MySqlParser.QueryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#queryExpression}.
	 * @param ctx the parse tree
	 */
	void exitQueryExpression(MySqlParser.QueryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#queryExpressionNointo}.
	 * @param ctx the parse tree
	 */
	void enterQueryExpressionNointo(MySqlParser.QueryExpressionNointoContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#queryExpressionNointo}.
	 * @param ctx the parse tree
	 */
	void exitQueryExpressionNointo(MySqlParser.QueryExpressionNointoContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void enterQuerySpecification(MySqlParser.QuerySpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#querySpecification}.
	 * @param ctx the parse tree
	 */
	void exitQuerySpecification(MySqlParser.QuerySpecificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#querySpecificationNointo}.
	 * @param ctx the parse tree
	 */
	void enterQuerySpecificationNointo(MySqlParser.QuerySpecificationNointoContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#querySpecificationNointo}.
	 * @param ctx the parse tree
	 */
	void exitQuerySpecificationNointo(MySqlParser.QuerySpecificationNointoContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#unionParenthesis}.
	 * @param ctx the parse tree
	 */
	void enterUnionParenthesis(MySqlParser.UnionParenthesisContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#unionParenthesis}.
	 * @param ctx the parse tree
	 */
	void exitUnionParenthesis(MySqlParser.UnionParenthesisContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#unionStatement}.
	 * @param ctx the parse tree
	 */
	void enterUnionStatement(MySqlParser.UnionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#unionStatement}.
	 * @param ctx the parse tree
	 */
	void exitUnionStatement(MySqlParser.UnionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#selectSpec}.
	 * @param ctx the parse tree
	 */
	void enterSelectSpec(MySqlParser.SelectSpecContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#selectSpec}.
	 * @param ctx the parse tree
	 */
	void exitSelectSpec(MySqlParser.SelectSpecContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#selectElements}.
	 * @param ctx the parse tree
	 */
	void enterSelectElements(MySqlParser.SelectElementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#selectElements}.
	 * @param ctx the parse tree
	 */
	void exitSelectElements(MySqlParser.SelectElementsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code selectStarElement}
	 * labeled alternative in {@link MySqlParser#selectElement}.
	 * @param ctx the parse tree
	 */
	void enterSelectStarElement(MySqlParser.SelectStarElementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code selectStarElement}
	 * labeled alternative in {@link MySqlParser#selectElement}.
	 * @param ctx the parse tree
	 */
	void exitSelectStarElement(MySqlParser.SelectStarElementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code selectColumnElement}
	 * labeled alternative in {@link MySqlParser#selectElement}.
	 * @param ctx the parse tree
	 */
	void enterSelectColumnElement(MySqlParser.SelectColumnElementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code selectColumnElement}
	 * labeled alternative in {@link MySqlParser#selectElement}.
	 * @param ctx the parse tree
	 */
	void exitSelectColumnElement(MySqlParser.SelectColumnElementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code selectFunctionElement}
	 * labeled alternative in {@link MySqlParser#selectElement}.
	 * @param ctx the parse tree
	 */
	void enterSelectFunctionElement(MySqlParser.SelectFunctionElementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code selectFunctionElement}
	 * labeled alternative in {@link MySqlParser#selectElement}.
	 * @param ctx the parse tree
	 */
	void exitSelectFunctionElement(MySqlParser.SelectFunctionElementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code selectExpressionElement}
	 * labeled alternative in {@link MySqlParser#selectElement}.
	 * @param ctx the parse tree
	 */
	void enterSelectExpressionElement(MySqlParser.SelectExpressionElementContext ctx);
	/**
	 * Exit a parse tree produced by the {@code selectExpressionElement}
	 * labeled alternative in {@link MySqlParser#selectElement}.
	 * @param ctx the parse tree
	 */
	void exitSelectExpressionElement(MySqlParser.SelectExpressionElementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code selectIntoVariables}
	 * labeled alternative in {@link MySqlParser#selectIntoExpression}.
	 * @param ctx the parse tree
	 */
	void enterSelectIntoVariables(MySqlParser.SelectIntoVariablesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code selectIntoVariables}
	 * labeled alternative in {@link MySqlParser#selectIntoExpression}.
	 * @param ctx the parse tree
	 */
	void exitSelectIntoVariables(MySqlParser.SelectIntoVariablesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code selectIntoDumpFile}
	 * labeled alternative in {@link MySqlParser#selectIntoExpression}.
	 * @param ctx the parse tree
	 */
	void enterSelectIntoDumpFile(MySqlParser.SelectIntoDumpFileContext ctx);
	/**
	 * Exit a parse tree produced by the {@code selectIntoDumpFile}
	 * labeled alternative in {@link MySqlParser#selectIntoExpression}.
	 * @param ctx the parse tree
	 */
	void exitSelectIntoDumpFile(MySqlParser.SelectIntoDumpFileContext ctx);
	/**
	 * Enter a parse tree produced by the {@code selectIntoTextFile}
	 * labeled alternative in {@link MySqlParser#selectIntoExpression}.
	 * @param ctx the parse tree
	 */
	void enterSelectIntoTextFile(MySqlParser.SelectIntoTextFileContext ctx);
	/**
	 * Exit a parse tree produced by the {@code selectIntoTextFile}
	 * labeled alternative in {@link MySqlParser#selectIntoExpression}.
	 * @param ctx the parse tree
	 */
	void exitSelectIntoTextFile(MySqlParser.SelectIntoTextFileContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#selectFieldsInto}.
	 * @param ctx the parse tree
	 */
	void enterSelectFieldsInto(MySqlParser.SelectFieldsIntoContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#selectFieldsInto}.
	 * @param ctx the parse tree
	 */
	void exitSelectFieldsInto(MySqlParser.SelectFieldsIntoContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#selectLinesInto}.
	 * @param ctx the parse tree
	 */
	void enterSelectLinesInto(MySqlParser.SelectLinesIntoContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#selectLinesInto}.
	 * @param ctx the parse tree
	 */
	void exitSelectLinesInto(MySqlParser.SelectLinesIntoContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void enterFromClause(MySqlParser.FromClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#fromClause}.
	 * @param ctx the parse tree
	 */
	void exitFromClause(MySqlParser.FromClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#groupByItem}.
	 * @param ctx the parse tree
	 */
	void enterGroupByItem(MySqlParser.GroupByItemContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#groupByItem}.
	 * @param ctx the parse tree
	 */
	void exitGroupByItem(MySqlParser.GroupByItemContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#limitClause}.
	 * @param ctx the parse tree
	 */
	void enterLimitClause(MySqlParser.LimitClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#limitClause}.
	 * @param ctx the parse tree
	 */
	void exitLimitClause(MySqlParser.LimitClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#limitClauseAtom}.
	 * @param ctx the parse tree
	 */
	void enterLimitClauseAtom(MySqlParser.LimitClauseAtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#limitClauseAtom}.
	 * @param ctx the parse tree
	 */
	void exitLimitClauseAtom(MySqlParser.LimitClauseAtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#startTransaction}.
	 * @param ctx the parse tree
	 */
	void enterStartTransaction(MySqlParser.StartTransactionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#startTransaction}.
	 * @param ctx the parse tree
	 */
	void exitStartTransaction(MySqlParser.StartTransactionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#beginWork}.
	 * @param ctx the parse tree
	 */
	void enterBeginWork(MySqlParser.BeginWorkContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#beginWork}.
	 * @param ctx the parse tree
	 */
	void exitBeginWork(MySqlParser.BeginWorkContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#commitWork}.
	 * @param ctx the parse tree
	 */
	void enterCommitWork(MySqlParser.CommitWorkContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#commitWork}.
	 * @param ctx the parse tree
	 */
	void exitCommitWork(MySqlParser.CommitWorkContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#rollbackWork}.
	 * @param ctx the parse tree
	 */
	void enterRollbackWork(MySqlParser.RollbackWorkContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#rollbackWork}.
	 * @param ctx the parse tree
	 */
	void exitRollbackWork(MySqlParser.RollbackWorkContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#savepointStatement}.
	 * @param ctx the parse tree
	 */
	void enterSavepointStatement(MySqlParser.SavepointStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#savepointStatement}.
	 * @param ctx the parse tree
	 */
	void exitSavepointStatement(MySqlParser.SavepointStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#rollbackStatement}.
	 * @param ctx the parse tree
	 */
	void enterRollbackStatement(MySqlParser.RollbackStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#rollbackStatement}.
	 * @param ctx the parse tree
	 */
	void exitRollbackStatement(MySqlParser.RollbackStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#releaseStatement}.
	 * @param ctx the parse tree
	 */
	void enterReleaseStatement(MySqlParser.ReleaseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#releaseStatement}.
	 * @param ctx the parse tree
	 */
	void exitReleaseStatement(MySqlParser.ReleaseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#lockTables}.
	 * @param ctx the parse tree
	 */
	void enterLockTables(MySqlParser.LockTablesContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#lockTables}.
	 * @param ctx the parse tree
	 */
	void exitLockTables(MySqlParser.LockTablesContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#unlockTables}.
	 * @param ctx the parse tree
	 */
	void enterUnlockTables(MySqlParser.UnlockTablesContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#unlockTables}.
	 * @param ctx the parse tree
	 */
	void exitUnlockTables(MySqlParser.UnlockTablesContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#setAutocommitStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetAutocommitStatement(MySqlParser.SetAutocommitStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#setAutocommitStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetAutocommitStatement(MySqlParser.SetAutocommitStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#setTransactionStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetTransactionStatement(MySqlParser.SetTransactionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#setTransactionStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetTransactionStatement(MySqlParser.SetTransactionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#transactionMode}.
	 * @param ctx the parse tree
	 */
	void enterTransactionMode(MySqlParser.TransactionModeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#transactionMode}.
	 * @param ctx the parse tree
	 */
	void exitTransactionMode(MySqlParser.TransactionModeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#lockTableElement}.
	 * @param ctx the parse tree
	 */
	void enterLockTableElement(MySqlParser.LockTableElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#lockTableElement}.
	 * @param ctx the parse tree
	 */
	void exitLockTableElement(MySqlParser.LockTableElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#lockAction}.
	 * @param ctx the parse tree
	 */
	void enterLockAction(MySqlParser.LockActionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#lockAction}.
	 * @param ctx the parse tree
	 */
	void exitLockAction(MySqlParser.LockActionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#transactionOption}.
	 * @param ctx the parse tree
	 */
	void enterTransactionOption(MySqlParser.TransactionOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#transactionOption}.
	 * @param ctx the parse tree
	 */
	void exitTransactionOption(MySqlParser.TransactionOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#transactionLevel}.
	 * @param ctx the parse tree
	 */
	void enterTransactionLevel(MySqlParser.TransactionLevelContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#transactionLevel}.
	 * @param ctx the parse tree
	 */
	void exitTransactionLevel(MySqlParser.TransactionLevelContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#changeMaster}.
	 * @param ctx the parse tree
	 */
	void enterChangeMaster(MySqlParser.ChangeMasterContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#changeMaster}.
	 * @param ctx the parse tree
	 */
	void exitChangeMaster(MySqlParser.ChangeMasterContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#changeReplicationFilter}.
	 * @param ctx the parse tree
	 */
	void enterChangeReplicationFilter(MySqlParser.ChangeReplicationFilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#changeReplicationFilter}.
	 * @param ctx the parse tree
	 */
	void exitChangeReplicationFilter(MySqlParser.ChangeReplicationFilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#purgeBinaryLogs}.
	 * @param ctx the parse tree
	 */
	void enterPurgeBinaryLogs(MySqlParser.PurgeBinaryLogsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#purgeBinaryLogs}.
	 * @param ctx the parse tree
	 */
	void exitPurgeBinaryLogs(MySqlParser.PurgeBinaryLogsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#resetMaster}.
	 * @param ctx the parse tree
	 */
	void enterResetMaster(MySqlParser.ResetMasterContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#resetMaster}.
	 * @param ctx the parse tree
	 */
	void exitResetMaster(MySqlParser.ResetMasterContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#resetSlave}.
	 * @param ctx the parse tree
	 */
	void enterResetSlave(MySqlParser.ResetSlaveContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#resetSlave}.
	 * @param ctx the parse tree
	 */
	void exitResetSlave(MySqlParser.ResetSlaveContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#startSlave}.
	 * @param ctx the parse tree
	 */
	void enterStartSlave(MySqlParser.StartSlaveContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#startSlave}.
	 * @param ctx the parse tree
	 */
	void exitStartSlave(MySqlParser.StartSlaveContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#stopSlave}.
	 * @param ctx the parse tree
	 */
	void enterStopSlave(MySqlParser.StopSlaveContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#stopSlave}.
	 * @param ctx the parse tree
	 */
	void exitStopSlave(MySqlParser.StopSlaveContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#startGroupReplication}.
	 * @param ctx the parse tree
	 */
	void enterStartGroupReplication(MySqlParser.StartGroupReplicationContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#startGroupReplication}.
	 * @param ctx the parse tree
	 */
	void exitStartGroupReplication(MySqlParser.StartGroupReplicationContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#stopGroupReplication}.
	 * @param ctx the parse tree
	 */
	void enterStopGroupReplication(MySqlParser.StopGroupReplicationContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#stopGroupReplication}.
	 * @param ctx the parse tree
	 */
	void exitStopGroupReplication(MySqlParser.StopGroupReplicationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code masterStringOption}
	 * labeled alternative in {@link MySqlParser#masterOption}.
	 * @param ctx the parse tree
	 */
	void enterMasterStringOption(MySqlParser.MasterStringOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code masterStringOption}
	 * labeled alternative in {@link MySqlParser#masterOption}.
	 * @param ctx the parse tree
	 */
	void exitMasterStringOption(MySqlParser.MasterStringOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code masterDecimalOption}
	 * labeled alternative in {@link MySqlParser#masterOption}.
	 * @param ctx the parse tree
	 */
	void enterMasterDecimalOption(MySqlParser.MasterDecimalOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code masterDecimalOption}
	 * labeled alternative in {@link MySqlParser#masterOption}.
	 * @param ctx the parse tree
	 */
	void exitMasterDecimalOption(MySqlParser.MasterDecimalOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code masterBoolOption}
	 * labeled alternative in {@link MySqlParser#masterOption}.
	 * @param ctx the parse tree
	 */
	void enterMasterBoolOption(MySqlParser.MasterBoolOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code masterBoolOption}
	 * labeled alternative in {@link MySqlParser#masterOption}.
	 * @param ctx the parse tree
	 */
	void exitMasterBoolOption(MySqlParser.MasterBoolOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code masterRealOption}
	 * labeled alternative in {@link MySqlParser#masterOption}.
	 * @param ctx the parse tree
	 */
	void enterMasterRealOption(MySqlParser.MasterRealOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code masterRealOption}
	 * labeled alternative in {@link MySqlParser#masterOption}.
	 * @param ctx the parse tree
	 */
	void exitMasterRealOption(MySqlParser.MasterRealOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code masterUidListOption}
	 * labeled alternative in {@link MySqlParser#masterOption}.
	 * @param ctx the parse tree
	 */
	void enterMasterUidListOption(MySqlParser.MasterUidListOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code masterUidListOption}
	 * labeled alternative in {@link MySqlParser#masterOption}.
	 * @param ctx the parse tree
	 */
	void exitMasterUidListOption(MySqlParser.MasterUidListOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#stringMasterOption}.
	 * @param ctx the parse tree
	 */
	void enterStringMasterOption(MySqlParser.StringMasterOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#stringMasterOption}.
	 * @param ctx the parse tree
	 */
	void exitStringMasterOption(MySqlParser.StringMasterOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#decimalMasterOption}.
	 * @param ctx the parse tree
	 */
	void enterDecimalMasterOption(MySqlParser.DecimalMasterOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#decimalMasterOption}.
	 * @param ctx the parse tree
	 */
	void exitDecimalMasterOption(MySqlParser.DecimalMasterOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#boolMasterOption}.
	 * @param ctx the parse tree
	 */
	void enterBoolMasterOption(MySqlParser.BoolMasterOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#boolMasterOption}.
	 * @param ctx the parse tree
	 */
	void exitBoolMasterOption(MySqlParser.BoolMasterOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#channelOption}.
	 * @param ctx the parse tree
	 */
	void enterChannelOption(MySqlParser.ChannelOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#channelOption}.
	 * @param ctx the parse tree
	 */
	void exitChannelOption(MySqlParser.ChannelOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code doDbReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void enterDoDbReplication(MySqlParser.DoDbReplicationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code doDbReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void exitDoDbReplication(MySqlParser.DoDbReplicationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ignoreDbReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void enterIgnoreDbReplication(MySqlParser.IgnoreDbReplicationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ignoreDbReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void exitIgnoreDbReplication(MySqlParser.IgnoreDbReplicationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code doTableReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void enterDoTableReplication(MySqlParser.DoTableReplicationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code doTableReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void exitDoTableReplication(MySqlParser.DoTableReplicationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code ignoreTableReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void enterIgnoreTableReplication(MySqlParser.IgnoreTableReplicationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code ignoreTableReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void exitIgnoreTableReplication(MySqlParser.IgnoreTableReplicationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code wildDoTableReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void enterWildDoTableReplication(MySqlParser.WildDoTableReplicationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code wildDoTableReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void exitWildDoTableReplication(MySqlParser.WildDoTableReplicationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code wildIgnoreTableReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void enterWildIgnoreTableReplication(MySqlParser.WildIgnoreTableReplicationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code wildIgnoreTableReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void exitWildIgnoreTableReplication(MySqlParser.WildIgnoreTableReplicationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code rewriteDbReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void enterRewriteDbReplication(MySqlParser.RewriteDbReplicationContext ctx);
	/**
	 * Exit a parse tree produced by the {@code rewriteDbReplication}
	 * labeled alternative in {@link MySqlParser#replicationFilter}.
	 * @param ctx the parse tree
	 */
	void exitRewriteDbReplication(MySqlParser.RewriteDbReplicationContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#tablePair}.
	 * @param ctx the parse tree
	 */
	void enterTablePair(MySqlParser.TablePairContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#tablePair}.
	 * @param ctx the parse tree
	 */
	void exitTablePair(MySqlParser.TablePairContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#threadType}.
	 * @param ctx the parse tree
	 */
	void enterThreadType(MySqlParser.ThreadTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#threadType}.
	 * @param ctx the parse tree
	 */
	void exitThreadType(MySqlParser.ThreadTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code gtidsUntilOption}
	 * labeled alternative in {@link MySqlParser#untilOption}.
	 * @param ctx the parse tree
	 */
	void enterGtidsUntilOption(MySqlParser.GtidsUntilOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code gtidsUntilOption}
	 * labeled alternative in {@link MySqlParser#untilOption}.
	 * @param ctx the parse tree
	 */
	void exitGtidsUntilOption(MySqlParser.GtidsUntilOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code masterLogUntilOption}
	 * labeled alternative in {@link MySqlParser#untilOption}.
	 * @param ctx the parse tree
	 */
	void enterMasterLogUntilOption(MySqlParser.MasterLogUntilOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code masterLogUntilOption}
	 * labeled alternative in {@link MySqlParser#untilOption}.
	 * @param ctx the parse tree
	 */
	void exitMasterLogUntilOption(MySqlParser.MasterLogUntilOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code relayLogUntilOption}
	 * labeled alternative in {@link MySqlParser#untilOption}.
	 * @param ctx the parse tree
	 */
	void enterRelayLogUntilOption(MySqlParser.RelayLogUntilOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code relayLogUntilOption}
	 * labeled alternative in {@link MySqlParser#untilOption}.
	 * @param ctx the parse tree
	 */
	void exitRelayLogUntilOption(MySqlParser.RelayLogUntilOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code sqlGapsUntilOption}
	 * labeled alternative in {@link MySqlParser#untilOption}.
	 * @param ctx the parse tree
	 */
	void enterSqlGapsUntilOption(MySqlParser.SqlGapsUntilOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code sqlGapsUntilOption}
	 * labeled alternative in {@link MySqlParser#untilOption}.
	 * @param ctx the parse tree
	 */
	void exitSqlGapsUntilOption(MySqlParser.SqlGapsUntilOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code userConnectionOption}
	 * labeled alternative in {@link MySqlParser#connectionOption}.
	 * @param ctx the parse tree
	 */
	void enterUserConnectionOption(MySqlParser.UserConnectionOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code userConnectionOption}
	 * labeled alternative in {@link MySqlParser#connectionOption}.
	 * @param ctx the parse tree
	 */
	void exitUserConnectionOption(MySqlParser.UserConnectionOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code passwordConnectionOption}
	 * labeled alternative in {@link MySqlParser#connectionOption}.
	 * @param ctx the parse tree
	 */
	void enterPasswordConnectionOption(MySqlParser.PasswordConnectionOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code passwordConnectionOption}
	 * labeled alternative in {@link MySqlParser#connectionOption}.
	 * @param ctx the parse tree
	 */
	void exitPasswordConnectionOption(MySqlParser.PasswordConnectionOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code defaultAuthConnectionOption}
	 * labeled alternative in {@link MySqlParser#connectionOption}.
	 * @param ctx the parse tree
	 */
	void enterDefaultAuthConnectionOption(MySqlParser.DefaultAuthConnectionOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code defaultAuthConnectionOption}
	 * labeled alternative in {@link MySqlParser#connectionOption}.
	 * @param ctx the parse tree
	 */
	void exitDefaultAuthConnectionOption(MySqlParser.DefaultAuthConnectionOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code pluginDirConnectionOption}
	 * labeled alternative in {@link MySqlParser#connectionOption}.
	 * @param ctx the parse tree
	 */
	void enterPluginDirConnectionOption(MySqlParser.PluginDirConnectionOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code pluginDirConnectionOption}
	 * labeled alternative in {@link MySqlParser#connectionOption}.
	 * @param ctx the parse tree
	 */
	void exitPluginDirConnectionOption(MySqlParser.PluginDirConnectionOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#gtuidSet}.
	 * @param ctx the parse tree
	 */
	void enterGtuidSet(MySqlParser.GtuidSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#gtuidSet}.
	 * @param ctx the parse tree
	 */
	void exitGtuidSet(MySqlParser.GtuidSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#xaStartTransaction}.
	 * @param ctx the parse tree
	 */
	void enterXaStartTransaction(MySqlParser.XaStartTransactionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#xaStartTransaction}.
	 * @param ctx the parse tree
	 */
	void exitXaStartTransaction(MySqlParser.XaStartTransactionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#xaEndTransaction}.
	 * @param ctx the parse tree
	 */
	void enterXaEndTransaction(MySqlParser.XaEndTransactionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#xaEndTransaction}.
	 * @param ctx the parse tree
	 */
	void exitXaEndTransaction(MySqlParser.XaEndTransactionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#xaPrepareStatement}.
	 * @param ctx the parse tree
	 */
	void enterXaPrepareStatement(MySqlParser.XaPrepareStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#xaPrepareStatement}.
	 * @param ctx the parse tree
	 */
	void exitXaPrepareStatement(MySqlParser.XaPrepareStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#xaCommitWork}.
	 * @param ctx the parse tree
	 */
	void enterXaCommitWork(MySqlParser.XaCommitWorkContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#xaCommitWork}.
	 * @param ctx the parse tree
	 */
	void exitXaCommitWork(MySqlParser.XaCommitWorkContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#xaRollbackWork}.
	 * @param ctx the parse tree
	 */
	void enterXaRollbackWork(MySqlParser.XaRollbackWorkContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#xaRollbackWork}.
	 * @param ctx the parse tree
	 */
	void exitXaRollbackWork(MySqlParser.XaRollbackWorkContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#xaRecoverWork}.
	 * @param ctx the parse tree
	 */
	void enterXaRecoverWork(MySqlParser.XaRecoverWorkContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#xaRecoverWork}.
	 * @param ctx the parse tree
	 */
	void exitXaRecoverWork(MySqlParser.XaRecoverWorkContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#prepareStatement}.
	 * @param ctx the parse tree
	 */
	void enterPrepareStatement(MySqlParser.PrepareStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#prepareStatement}.
	 * @param ctx the parse tree
	 */
	void exitPrepareStatement(MySqlParser.PrepareStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#executeStatement}.
	 * @param ctx the parse tree
	 */
	void enterExecuteStatement(MySqlParser.ExecuteStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#executeStatement}.
	 * @param ctx the parse tree
	 */
	void exitExecuteStatement(MySqlParser.ExecuteStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#deallocatePrepare}.
	 * @param ctx the parse tree
	 */
	void enterDeallocatePrepare(MySqlParser.DeallocatePrepareContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#deallocatePrepare}.
	 * @param ctx the parse tree
	 */
	void exitDeallocatePrepare(MySqlParser.DeallocatePrepareContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#routineBody}.
	 * @param ctx the parse tree
	 */
	void enterRoutineBody(MySqlParser.RoutineBodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#routineBody}.
	 * @param ctx the parse tree
	 */
	void exitRoutineBody(MySqlParser.RoutineBodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void enterBlockStatement(MySqlParser.BlockStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#blockStatement}.
	 * @param ctx the parse tree
	 */
	void exitBlockStatement(MySqlParser.BlockStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#caseStatement}.
	 * @param ctx the parse tree
	 */
	void enterCaseStatement(MySqlParser.CaseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#caseStatement}.
	 * @param ctx the parse tree
	 */
	void exitCaseStatement(MySqlParser.CaseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(MySqlParser.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(MySqlParser.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#iterateStatement}.
	 * @param ctx the parse tree
	 */
	void enterIterateStatement(MySqlParser.IterateStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#iterateStatement}.
	 * @param ctx the parse tree
	 */
	void exitIterateStatement(MySqlParser.IterateStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#leaveStatement}.
	 * @param ctx the parse tree
	 */
	void enterLeaveStatement(MySqlParser.LeaveStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#leaveStatement}.
	 * @param ctx the parse tree
	 */
	void exitLeaveStatement(MySqlParser.LeaveStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#loopStatement}.
	 * @param ctx the parse tree
	 */
	void enterLoopStatement(MySqlParser.LoopStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#loopStatement}.
	 * @param ctx the parse tree
	 */
	void exitLoopStatement(MySqlParser.LoopStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#repeatStatement}.
	 * @param ctx the parse tree
	 */
	void enterRepeatStatement(MySqlParser.RepeatStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#repeatStatement}.
	 * @param ctx the parse tree
	 */
	void exitRepeatStatement(MySqlParser.RepeatStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStatement(MySqlParser.ReturnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStatement(MySqlParser.ReturnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileStatement(MySqlParser.WhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileStatement(MySqlParser.WhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by the {@code CloseCursor}
	 * labeled alternative in {@link MySqlParser#cursorStatement}.
	 * @param ctx the parse tree
	 */
	void enterCloseCursor(MySqlParser.CloseCursorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code CloseCursor}
	 * labeled alternative in {@link MySqlParser#cursorStatement}.
	 * @param ctx the parse tree
	 */
	void exitCloseCursor(MySqlParser.CloseCursorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code FetchCursor}
	 * labeled alternative in {@link MySqlParser#cursorStatement}.
	 * @param ctx the parse tree
	 */
	void enterFetchCursor(MySqlParser.FetchCursorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code FetchCursor}
	 * labeled alternative in {@link MySqlParser#cursorStatement}.
	 * @param ctx the parse tree
	 */
	void exitFetchCursor(MySqlParser.FetchCursorContext ctx);
	/**
	 * Enter a parse tree produced by the {@code OpenCursor}
	 * labeled alternative in {@link MySqlParser#cursorStatement}.
	 * @param ctx the parse tree
	 */
	void enterOpenCursor(MySqlParser.OpenCursorContext ctx);
	/**
	 * Exit a parse tree produced by the {@code OpenCursor}
	 * labeled alternative in {@link MySqlParser#cursorStatement}.
	 * @param ctx the parse tree
	 */
	void exitOpenCursor(MySqlParser.OpenCursorContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#declareVariable}.
	 * @param ctx the parse tree
	 */
	void enterDeclareVariable(MySqlParser.DeclareVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#declareVariable}.
	 * @param ctx the parse tree
	 */
	void exitDeclareVariable(MySqlParser.DeclareVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#declareCondition}.
	 * @param ctx the parse tree
	 */
	void enterDeclareCondition(MySqlParser.DeclareConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#declareCondition}.
	 * @param ctx the parse tree
	 */
	void exitDeclareCondition(MySqlParser.DeclareConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#declareCursor}.
	 * @param ctx the parse tree
	 */
	void enterDeclareCursor(MySqlParser.DeclareCursorContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#declareCursor}.
	 * @param ctx the parse tree
	 */
	void exitDeclareCursor(MySqlParser.DeclareCursorContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#declareHandler}.
	 * @param ctx the parse tree
	 */
	void enterDeclareHandler(MySqlParser.DeclareHandlerContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#declareHandler}.
	 * @param ctx the parse tree
	 */
	void exitDeclareHandler(MySqlParser.DeclareHandlerContext ctx);
	/**
	 * Enter a parse tree produced by the {@code handlerConditionCode}
	 * labeled alternative in {@link MySqlParser#handlerConditionValue}.
	 * @param ctx the parse tree
	 */
	void enterHandlerConditionCode(MySqlParser.HandlerConditionCodeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code handlerConditionCode}
	 * labeled alternative in {@link MySqlParser#handlerConditionValue}.
	 * @param ctx the parse tree
	 */
	void exitHandlerConditionCode(MySqlParser.HandlerConditionCodeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code handlerConditionState}
	 * labeled alternative in {@link MySqlParser#handlerConditionValue}.
	 * @param ctx the parse tree
	 */
	void enterHandlerConditionState(MySqlParser.HandlerConditionStateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code handlerConditionState}
	 * labeled alternative in {@link MySqlParser#handlerConditionValue}.
	 * @param ctx the parse tree
	 */
	void exitHandlerConditionState(MySqlParser.HandlerConditionStateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code handlerConditionName}
	 * labeled alternative in {@link MySqlParser#handlerConditionValue}.
	 * @param ctx the parse tree
	 */
	void enterHandlerConditionName(MySqlParser.HandlerConditionNameContext ctx);
	/**
	 * Exit a parse tree produced by the {@code handlerConditionName}
	 * labeled alternative in {@link MySqlParser#handlerConditionValue}.
	 * @param ctx the parse tree
	 */
	void exitHandlerConditionName(MySqlParser.HandlerConditionNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code handlerConditionWarning}
	 * labeled alternative in {@link MySqlParser#handlerConditionValue}.
	 * @param ctx the parse tree
	 */
	void enterHandlerConditionWarning(MySqlParser.HandlerConditionWarningContext ctx);
	/**
	 * Exit a parse tree produced by the {@code handlerConditionWarning}
	 * labeled alternative in {@link MySqlParser#handlerConditionValue}.
	 * @param ctx the parse tree
	 */
	void exitHandlerConditionWarning(MySqlParser.HandlerConditionWarningContext ctx);
	/**
	 * Enter a parse tree produced by the {@code handlerConditionNotfound}
	 * labeled alternative in {@link MySqlParser#handlerConditionValue}.
	 * @param ctx the parse tree
	 */
	void enterHandlerConditionNotfound(MySqlParser.HandlerConditionNotfoundContext ctx);
	/**
	 * Exit a parse tree produced by the {@code handlerConditionNotfound}
	 * labeled alternative in {@link MySqlParser#handlerConditionValue}.
	 * @param ctx the parse tree
	 */
	void exitHandlerConditionNotfound(MySqlParser.HandlerConditionNotfoundContext ctx);
	/**
	 * Enter a parse tree produced by the {@code handlerConditionException}
	 * labeled alternative in {@link MySqlParser#handlerConditionValue}.
	 * @param ctx the parse tree
	 */
	void enterHandlerConditionException(MySqlParser.HandlerConditionExceptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code handlerConditionException}
	 * labeled alternative in {@link MySqlParser#handlerConditionValue}.
	 * @param ctx the parse tree
	 */
	void exitHandlerConditionException(MySqlParser.HandlerConditionExceptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#procedureSqlStatement}.
	 * @param ctx the parse tree
	 */
	void enterProcedureSqlStatement(MySqlParser.ProcedureSqlStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#procedureSqlStatement}.
	 * @param ctx the parse tree
	 */
	void exitProcedureSqlStatement(MySqlParser.ProcedureSqlStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#caseAlternative}.
	 * @param ctx the parse tree
	 */
	void enterCaseAlternative(MySqlParser.CaseAlternativeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#caseAlternative}.
	 * @param ctx the parse tree
	 */
	void exitCaseAlternative(MySqlParser.CaseAlternativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#elifAlternative}.
	 * @param ctx the parse tree
	 */
	void enterElifAlternative(MySqlParser.ElifAlternativeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#elifAlternative}.
	 * @param ctx the parse tree
	 */
	void exitElifAlternative(MySqlParser.ElifAlternativeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code alterUserMysqlV56}
	 * labeled alternative in {@link MySqlParser#alterUser}.
	 * @param ctx the parse tree
	 */
	void enterAlterUserMysqlV56(MySqlParser.AlterUserMysqlV56Context ctx);
	/**
	 * Exit a parse tree produced by the {@code alterUserMysqlV56}
	 * labeled alternative in {@link MySqlParser#alterUser}.
	 * @param ctx the parse tree
	 */
	void exitAlterUserMysqlV56(MySqlParser.AlterUserMysqlV56Context ctx);
	/**
	 * Enter a parse tree produced by the {@code alterUserMysqlV57}
	 * labeled alternative in {@link MySqlParser#alterUser}.
	 * @param ctx the parse tree
	 */
	void enterAlterUserMysqlV57(MySqlParser.AlterUserMysqlV57Context ctx);
	/**
	 * Exit a parse tree produced by the {@code alterUserMysqlV57}
	 * labeled alternative in {@link MySqlParser#alterUser}.
	 * @param ctx the parse tree
	 */
	void exitAlterUserMysqlV57(MySqlParser.AlterUserMysqlV57Context ctx);
	/**
	 * Enter a parse tree produced by the {@code createUserMysqlV56}
	 * labeled alternative in {@link MySqlParser#createUser}.
	 * @param ctx the parse tree
	 */
	void enterCreateUserMysqlV56(MySqlParser.CreateUserMysqlV56Context ctx);
	/**
	 * Exit a parse tree produced by the {@code createUserMysqlV56}
	 * labeled alternative in {@link MySqlParser#createUser}.
	 * @param ctx the parse tree
	 */
	void exitCreateUserMysqlV56(MySqlParser.CreateUserMysqlV56Context ctx);
	/**
	 * Enter a parse tree produced by the {@code createUserMysqlV57}
	 * labeled alternative in {@link MySqlParser#createUser}.
	 * @param ctx the parse tree
	 */
	void enterCreateUserMysqlV57(MySqlParser.CreateUserMysqlV57Context ctx);
	/**
	 * Exit a parse tree produced by the {@code createUserMysqlV57}
	 * labeled alternative in {@link MySqlParser#createUser}.
	 * @param ctx the parse tree
	 */
	void exitCreateUserMysqlV57(MySqlParser.CreateUserMysqlV57Context ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dropUser}.
	 * @param ctx the parse tree
	 */
	void enterDropUser(MySqlParser.DropUserContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dropUser}.
	 * @param ctx the parse tree
	 */
	void exitDropUser(MySqlParser.DropUserContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#grantStatement}.
	 * @param ctx the parse tree
	 */
	void enterGrantStatement(MySqlParser.GrantStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#grantStatement}.
	 * @param ctx the parse tree
	 */
	void exitGrantStatement(MySqlParser.GrantStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#grantProxy}.
	 * @param ctx the parse tree
	 */
	void enterGrantProxy(MySqlParser.GrantProxyContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#grantProxy}.
	 * @param ctx the parse tree
	 */
	void exitGrantProxy(MySqlParser.GrantProxyContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#renameUser}.
	 * @param ctx the parse tree
	 */
	void enterRenameUser(MySqlParser.RenameUserContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#renameUser}.
	 * @param ctx the parse tree
	 */
	void exitRenameUser(MySqlParser.RenameUserContext ctx);
	/**
	 * Enter a parse tree produced by the {@code detailRevoke}
	 * labeled alternative in {@link MySqlParser#revokeStatement}.
	 * @param ctx the parse tree
	 */
	void enterDetailRevoke(MySqlParser.DetailRevokeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code detailRevoke}
	 * labeled alternative in {@link MySqlParser#revokeStatement}.
	 * @param ctx the parse tree
	 */
	void exitDetailRevoke(MySqlParser.DetailRevokeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code shortRevoke}
	 * labeled alternative in {@link MySqlParser#revokeStatement}.
	 * @param ctx the parse tree
	 */
	void enterShortRevoke(MySqlParser.ShortRevokeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code shortRevoke}
	 * labeled alternative in {@link MySqlParser#revokeStatement}.
	 * @param ctx the parse tree
	 */
	void exitShortRevoke(MySqlParser.ShortRevokeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#revokeProxy}.
	 * @param ctx the parse tree
	 */
	void enterRevokeProxy(MySqlParser.RevokeProxyContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#revokeProxy}.
	 * @param ctx the parse tree
	 */
	void exitRevokeProxy(MySqlParser.RevokeProxyContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#setPasswordStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetPasswordStatement(MySqlParser.SetPasswordStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#setPasswordStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetPasswordStatement(MySqlParser.SetPasswordStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#userSpecification}.
	 * @param ctx the parse tree
	 */
	void enterUserSpecification(MySqlParser.UserSpecificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#userSpecification}.
	 * @param ctx the parse tree
	 */
	void exitUserSpecification(MySqlParser.UserSpecificationContext ctx);
	/**
	 * Enter a parse tree produced by the {@code passwordAuthOption}
	 * labeled alternative in {@link MySqlParser#userAuthOption}.
	 * @param ctx the parse tree
	 */
	void enterPasswordAuthOption(MySqlParser.PasswordAuthOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code passwordAuthOption}
	 * labeled alternative in {@link MySqlParser#userAuthOption}.
	 * @param ctx the parse tree
	 */
	void exitPasswordAuthOption(MySqlParser.PasswordAuthOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringAuthOption}
	 * labeled alternative in {@link MySqlParser#userAuthOption}.
	 * @param ctx the parse tree
	 */
	void enterStringAuthOption(MySqlParser.StringAuthOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringAuthOption}
	 * labeled alternative in {@link MySqlParser#userAuthOption}.
	 * @param ctx the parse tree
	 */
	void exitStringAuthOption(MySqlParser.StringAuthOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code hashAuthOption}
	 * labeled alternative in {@link MySqlParser#userAuthOption}.
	 * @param ctx the parse tree
	 */
	void enterHashAuthOption(MySqlParser.HashAuthOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code hashAuthOption}
	 * labeled alternative in {@link MySqlParser#userAuthOption}.
	 * @param ctx the parse tree
	 */
	void exitHashAuthOption(MySqlParser.HashAuthOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleAuthOption}
	 * labeled alternative in {@link MySqlParser#userAuthOption}.
	 * @param ctx the parse tree
	 */
	void enterSimpleAuthOption(MySqlParser.SimpleAuthOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleAuthOption}
	 * labeled alternative in {@link MySqlParser#userAuthOption}.
	 * @param ctx the parse tree
	 */
	void exitSimpleAuthOption(MySqlParser.SimpleAuthOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#tlsOption}.
	 * @param ctx the parse tree
	 */
	void enterTlsOption(MySqlParser.TlsOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#tlsOption}.
	 * @param ctx the parse tree
	 */
	void exitTlsOption(MySqlParser.TlsOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#userResourceOption}.
	 * @param ctx the parse tree
	 */
	void enterUserResourceOption(MySqlParser.UserResourceOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#userResourceOption}.
	 * @param ctx the parse tree
	 */
	void exitUserResourceOption(MySqlParser.UserResourceOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#userPasswordOption}.
	 * @param ctx the parse tree
	 */
	void enterUserPasswordOption(MySqlParser.UserPasswordOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#userPasswordOption}.
	 * @param ctx the parse tree
	 */
	void exitUserPasswordOption(MySqlParser.UserPasswordOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#userLockOption}.
	 * @param ctx the parse tree
	 */
	void enterUserLockOption(MySqlParser.UserLockOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#userLockOption}.
	 * @param ctx the parse tree
	 */
	void exitUserLockOption(MySqlParser.UserLockOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#privelegeClause}.
	 * @param ctx the parse tree
	 */
	void enterPrivelegeClause(MySqlParser.PrivelegeClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#privelegeClause}.
	 * @param ctx the parse tree
	 */
	void exitPrivelegeClause(MySqlParser.PrivelegeClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#privilege}.
	 * @param ctx the parse tree
	 */
	void enterPrivilege(MySqlParser.PrivilegeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#privilege}.
	 * @param ctx the parse tree
	 */
	void exitPrivilege(MySqlParser.PrivilegeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code currentSchemaPriviLevel}
	 * labeled alternative in {@link MySqlParser#privilegeLevel}.
	 * @param ctx the parse tree
	 */
	void enterCurrentSchemaPriviLevel(MySqlParser.CurrentSchemaPriviLevelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code currentSchemaPriviLevel}
	 * labeled alternative in {@link MySqlParser#privilegeLevel}.
	 * @param ctx the parse tree
	 */
	void exitCurrentSchemaPriviLevel(MySqlParser.CurrentSchemaPriviLevelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code globalPrivLevel}
	 * labeled alternative in {@link MySqlParser#privilegeLevel}.
	 * @param ctx the parse tree
	 */
	void enterGlobalPrivLevel(MySqlParser.GlobalPrivLevelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code globalPrivLevel}
	 * labeled alternative in {@link MySqlParser#privilegeLevel}.
	 * @param ctx the parse tree
	 */
	void exitGlobalPrivLevel(MySqlParser.GlobalPrivLevelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code definiteSchemaPrivLevel}
	 * labeled alternative in {@link MySqlParser#privilegeLevel}.
	 * @param ctx the parse tree
	 */
	void enterDefiniteSchemaPrivLevel(MySqlParser.DefiniteSchemaPrivLevelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code definiteSchemaPrivLevel}
	 * labeled alternative in {@link MySqlParser#privilegeLevel}.
	 * @param ctx the parse tree
	 */
	void exitDefiniteSchemaPrivLevel(MySqlParser.DefiniteSchemaPrivLevelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code definiteFullTablePrivLevel}
	 * labeled alternative in {@link MySqlParser#privilegeLevel}.
	 * @param ctx the parse tree
	 */
	void enterDefiniteFullTablePrivLevel(MySqlParser.DefiniteFullTablePrivLevelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code definiteFullTablePrivLevel}
	 * labeled alternative in {@link MySqlParser#privilegeLevel}.
	 * @param ctx the parse tree
	 */
	void exitDefiniteFullTablePrivLevel(MySqlParser.DefiniteFullTablePrivLevelContext ctx);
	/**
	 * Enter a parse tree produced by the {@code definiteFullTablePrivLevel2}
	 * labeled alternative in {@link MySqlParser#privilegeLevel}.
	 * @param ctx the parse tree
	 */
	void enterDefiniteFullTablePrivLevel2(MySqlParser.DefiniteFullTablePrivLevel2Context ctx);
	/**
	 * Exit a parse tree produced by the {@code definiteFullTablePrivLevel2}
	 * labeled alternative in {@link MySqlParser#privilegeLevel}.
	 * @param ctx the parse tree
	 */
	void exitDefiniteFullTablePrivLevel2(MySqlParser.DefiniteFullTablePrivLevel2Context ctx);
	/**
	 * Enter a parse tree produced by the {@code definiteTablePrivLevel}
	 * labeled alternative in {@link MySqlParser#privilegeLevel}.
	 * @param ctx the parse tree
	 */
	void enterDefiniteTablePrivLevel(MySqlParser.DefiniteTablePrivLevelContext ctx);
	/**
	 * Exit a parse tree produced by the {@code definiteTablePrivLevel}
	 * labeled alternative in {@link MySqlParser#privilegeLevel}.
	 * @param ctx the parse tree
	 */
	void exitDefiniteTablePrivLevel(MySqlParser.DefiniteTablePrivLevelContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#renameUserClause}.
	 * @param ctx the parse tree
	 */
	void enterRenameUserClause(MySqlParser.RenameUserClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#renameUserClause}.
	 * @param ctx the parse tree
	 */
	void exitRenameUserClause(MySqlParser.RenameUserClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#analyzeTable}.
	 * @param ctx the parse tree
	 */
	void enterAnalyzeTable(MySqlParser.AnalyzeTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#analyzeTable}.
	 * @param ctx the parse tree
	 */
	void exitAnalyzeTable(MySqlParser.AnalyzeTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#checkTable}.
	 * @param ctx the parse tree
	 */
	void enterCheckTable(MySqlParser.CheckTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#checkTable}.
	 * @param ctx the parse tree
	 */
	void exitCheckTable(MySqlParser.CheckTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#checksumTable}.
	 * @param ctx the parse tree
	 */
	void enterChecksumTable(MySqlParser.ChecksumTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#checksumTable}.
	 * @param ctx the parse tree
	 */
	void exitChecksumTable(MySqlParser.ChecksumTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#optimizeTable}.
	 * @param ctx the parse tree
	 */
	void enterOptimizeTable(MySqlParser.OptimizeTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#optimizeTable}.
	 * @param ctx the parse tree
	 */
	void exitOptimizeTable(MySqlParser.OptimizeTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#repairTable}.
	 * @param ctx the parse tree
	 */
	void enterRepairTable(MySqlParser.RepairTableContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#repairTable}.
	 * @param ctx the parse tree
	 */
	void exitRepairTable(MySqlParser.RepairTableContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#checkTableOption}.
	 * @param ctx the parse tree
	 */
	void enterCheckTableOption(MySqlParser.CheckTableOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#checkTableOption}.
	 * @param ctx the parse tree
	 */
	void exitCheckTableOption(MySqlParser.CheckTableOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#createUdfunction}.
	 * @param ctx the parse tree
	 */
	void enterCreateUdfunction(MySqlParser.CreateUdfunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#createUdfunction}.
	 * @param ctx the parse tree
	 */
	void exitCreateUdfunction(MySqlParser.CreateUdfunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#installPlugin}.
	 * @param ctx the parse tree
	 */
	void enterInstallPlugin(MySqlParser.InstallPluginContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#installPlugin}.
	 * @param ctx the parse tree
	 */
	void exitInstallPlugin(MySqlParser.InstallPluginContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#uninstallPlugin}.
	 * @param ctx the parse tree
	 */
	void enterUninstallPlugin(MySqlParser.UninstallPluginContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#uninstallPlugin}.
	 * @param ctx the parse tree
	 */
	void exitUninstallPlugin(MySqlParser.UninstallPluginContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setVariable}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetVariable(MySqlParser.SetVariableContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setVariable}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetVariable(MySqlParser.SetVariableContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setCharset}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetCharset(MySqlParser.SetCharsetContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setCharset}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetCharset(MySqlParser.SetCharsetContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setNames}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetNames(MySqlParser.SetNamesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNames}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetNames(MySqlParser.SetNamesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setPassword}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetPassword(MySqlParser.SetPasswordContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setPassword}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetPassword(MySqlParser.SetPasswordContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setTransaction}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetTransaction(MySqlParser.SetTransactionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setTransaction}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetTransaction(MySqlParser.SetTransactionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setAutocommit}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetAutocommit(MySqlParser.SetAutocommitContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setAutocommit}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetAutocommit(MySqlParser.SetAutocommitContext ctx);
	/**
	 * Enter a parse tree produced by the {@code setNewValueInsideTrigger}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void enterSetNewValueInsideTrigger(MySqlParser.SetNewValueInsideTriggerContext ctx);
	/**
	 * Exit a parse tree produced by the {@code setNewValueInsideTrigger}
	 * labeled alternative in {@link MySqlParser#setStatement}.
	 * @param ctx the parse tree
	 */
	void exitSetNewValueInsideTrigger(MySqlParser.SetNewValueInsideTriggerContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showMasterLogs}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowMasterLogs(MySqlParser.ShowMasterLogsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showMasterLogs}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowMasterLogs(MySqlParser.ShowMasterLogsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showLogEvents}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowLogEvents(MySqlParser.ShowLogEventsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showLogEvents}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowLogEvents(MySqlParser.ShowLogEventsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showObjectFilter}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowObjectFilter(MySqlParser.ShowObjectFilterContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showObjectFilter}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowObjectFilter(MySqlParser.ShowObjectFilterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowColumns(MySqlParser.ShowColumnsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showColumns}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowColumns(MySqlParser.ShowColumnsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateDb}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateDb(MySqlParser.ShowCreateDbContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateDb}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateDb(MySqlParser.ShowCreateDbContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateFullIdObject}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateFullIdObject(MySqlParser.ShowCreateFullIdObjectContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateFullIdObject}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateFullIdObject(MySqlParser.ShowCreateFullIdObjectContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCreateUser}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowCreateUser(MySqlParser.ShowCreateUserContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCreateUser}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowCreateUser(MySqlParser.ShowCreateUserContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showEngine}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowEngine(MySqlParser.ShowEngineContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showEngine}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowEngine(MySqlParser.ShowEngineContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showGlobalInfo}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowGlobalInfo(MySqlParser.ShowGlobalInfoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showGlobalInfo}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowGlobalInfo(MySqlParser.ShowGlobalInfoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showErrors}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowErrors(MySqlParser.ShowErrorsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showErrors}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowErrors(MySqlParser.ShowErrorsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showCountErrors}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowCountErrors(MySqlParser.ShowCountErrorsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showCountErrors}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowCountErrors(MySqlParser.ShowCountErrorsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showSchemaFilter}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowSchemaFilter(MySqlParser.ShowSchemaFilterContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showSchemaFilter}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowSchemaFilter(MySqlParser.ShowSchemaFilterContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showRoutine}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowRoutine(MySqlParser.ShowRoutineContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showRoutine}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowRoutine(MySqlParser.ShowRoutineContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showGrants}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowGrants(MySqlParser.ShowGrantsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showGrants}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowGrants(MySqlParser.ShowGrantsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showIndexes}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowIndexes(MySqlParser.ShowIndexesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showIndexes}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowIndexes(MySqlParser.ShowIndexesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showOpenTables}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowOpenTables(MySqlParser.ShowOpenTablesContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showOpenTables}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowOpenTables(MySqlParser.ShowOpenTablesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showProfile}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowProfile(MySqlParser.ShowProfileContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showProfile}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowProfile(MySqlParser.ShowProfileContext ctx);
	/**
	 * Enter a parse tree produced by the {@code showSlaveStatus}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void enterShowSlaveStatus(MySqlParser.ShowSlaveStatusContext ctx);
	/**
	 * Exit a parse tree produced by the {@code showSlaveStatus}
	 * labeled alternative in {@link MySqlParser#showStatement}.
	 * @param ctx the parse tree
	 */
	void exitShowSlaveStatus(MySqlParser.ShowSlaveStatusContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#variableClause}.
	 * @param ctx the parse tree
	 */
	void enterVariableClause(MySqlParser.VariableClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#variableClause}.
	 * @param ctx the parse tree
	 */
	void exitVariableClause(MySqlParser.VariableClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#showCommonEntity}.
	 * @param ctx the parse tree
	 */
	void enterShowCommonEntity(MySqlParser.ShowCommonEntityContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#showCommonEntity}.
	 * @param ctx the parse tree
	 */
	void exitShowCommonEntity(MySqlParser.ShowCommonEntityContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#showFilter}.
	 * @param ctx the parse tree
	 */
	void enterShowFilter(MySqlParser.ShowFilterContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#showFilter}.
	 * @param ctx the parse tree
	 */
	void exitShowFilter(MySqlParser.ShowFilterContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#showGlobalInfoClause}.
	 * @param ctx the parse tree
	 */
	void enterShowGlobalInfoClause(MySqlParser.ShowGlobalInfoClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#showGlobalInfoClause}.
	 * @param ctx the parse tree
	 */
	void exitShowGlobalInfoClause(MySqlParser.ShowGlobalInfoClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#showSchemaEntity}.
	 * @param ctx the parse tree
	 */
	void enterShowSchemaEntity(MySqlParser.ShowSchemaEntityContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#showSchemaEntity}.
	 * @param ctx the parse tree
	 */
	void exitShowSchemaEntity(MySqlParser.ShowSchemaEntityContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#showProfileType}.
	 * @param ctx the parse tree
	 */
	void enterShowProfileType(MySqlParser.ShowProfileTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#showProfileType}.
	 * @param ctx the parse tree
	 */
	void exitShowProfileType(MySqlParser.ShowProfileTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#binlogStatement}.
	 * @param ctx the parse tree
	 */
	void enterBinlogStatement(MySqlParser.BinlogStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#binlogStatement}.
	 * @param ctx the parse tree
	 */
	void exitBinlogStatement(MySqlParser.BinlogStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#cacheIndexStatement}.
	 * @param ctx the parse tree
	 */
	void enterCacheIndexStatement(MySqlParser.CacheIndexStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#cacheIndexStatement}.
	 * @param ctx the parse tree
	 */
	void exitCacheIndexStatement(MySqlParser.CacheIndexStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#flushStatement}.
	 * @param ctx the parse tree
	 */
	void enterFlushStatement(MySqlParser.FlushStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#flushStatement}.
	 * @param ctx the parse tree
	 */
	void exitFlushStatement(MySqlParser.FlushStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#killStatement}.
	 * @param ctx the parse tree
	 */
	void enterKillStatement(MySqlParser.KillStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#killStatement}.
	 * @param ctx the parse tree
	 */
	void exitKillStatement(MySqlParser.KillStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#loadIndexIntoCache}.
	 * @param ctx the parse tree
	 */
	void enterLoadIndexIntoCache(MySqlParser.LoadIndexIntoCacheContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#loadIndexIntoCache}.
	 * @param ctx the parse tree
	 */
	void exitLoadIndexIntoCache(MySqlParser.LoadIndexIntoCacheContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#resetStatement}.
	 * @param ctx the parse tree
	 */
	void enterResetStatement(MySqlParser.ResetStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#resetStatement}.
	 * @param ctx the parse tree
	 */
	void exitResetStatement(MySqlParser.ResetStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#shutdownStatement}.
	 * @param ctx the parse tree
	 */
	void enterShutdownStatement(MySqlParser.ShutdownStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#shutdownStatement}.
	 * @param ctx the parse tree
	 */
	void exitShutdownStatement(MySqlParser.ShutdownStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#tableIndexes}.
	 * @param ctx the parse tree
	 */
	void enterTableIndexes(MySqlParser.TableIndexesContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#tableIndexes}.
	 * @param ctx the parse tree
	 */
	void exitTableIndexes(MySqlParser.TableIndexesContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleFlushOption}
	 * labeled alternative in {@link MySqlParser#flushOption}.
	 * @param ctx the parse tree
	 */
	void enterSimpleFlushOption(MySqlParser.SimpleFlushOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleFlushOption}
	 * labeled alternative in {@link MySqlParser#flushOption}.
	 * @param ctx the parse tree
	 */
	void exitSimpleFlushOption(MySqlParser.SimpleFlushOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code channelFlushOption}
	 * labeled alternative in {@link MySqlParser#flushOption}.
	 * @param ctx the parse tree
	 */
	void enterChannelFlushOption(MySqlParser.ChannelFlushOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code channelFlushOption}
	 * labeled alternative in {@link MySqlParser#flushOption}.
	 * @param ctx the parse tree
	 */
	void exitChannelFlushOption(MySqlParser.ChannelFlushOptionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code tableFlushOption}
	 * labeled alternative in {@link MySqlParser#flushOption}.
	 * @param ctx the parse tree
	 */
	void enterTableFlushOption(MySqlParser.TableFlushOptionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code tableFlushOption}
	 * labeled alternative in {@link MySqlParser#flushOption}.
	 * @param ctx the parse tree
	 */
	void exitTableFlushOption(MySqlParser.TableFlushOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#flushTableOption}.
	 * @param ctx the parse tree
	 */
	void enterFlushTableOption(MySqlParser.FlushTableOptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#flushTableOption}.
	 * @param ctx the parse tree
	 */
	void exitFlushTableOption(MySqlParser.FlushTableOptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#loadedTableIndexes}.
	 * @param ctx the parse tree
	 */
	void enterLoadedTableIndexes(MySqlParser.LoadedTableIndexesContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#loadedTableIndexes}.
	 * @param ctx the parse tree
	 */
	void exitLoadedTableIndexes(MySqlParser.LoadedTableIndexesContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#simpleDescribeStatement}.
	 * @param ctx the parse tree
	 */
	void enterSimpleDescribeStatement(MySqlParser.SimpleDescribeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#simpleDescribeStatement}.
	 * @param ctx the parse tree
	 */
	void exitSimpleDescribeStatement(MySqlParser.SimpleDescribeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#fullDescribeStatement}.
	 * @param ctx the parse tree
	 */
	void enterFullDescribeStatement(MySqlParser.FullDescribeStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#fullDescribeStatement}.
	 * @param ctx the parse tree
	 */
	void exitFullDescribeStatement(MySqlParser.FullDescribeStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#helpStatement}.
	 * @param ctx the parse tree
	 */
	void enterHelpStatement(MySqlParser.HelpStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#helpStatement}.
	 * @param ctx the parse tree
	 */
	void exitHelpStatement(MySqlParser.HelpStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#useStatement}.
	 * @param ctx the parse tree
	 */
	void enterUseStatement(MySqlParser.UseStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#useStatement}.
	 * @param ctx the parse tree
	 */
	void exitUseStatement(MySqlParser.UseStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#signalStatement}.
	 * @param ctx the parse tree
	 */
	void enterSignalStatement(MySqlParser.SignalStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#signalStatement}.
	 * @param ctx the parse tree
	 */
	void exitSignalStatement(MySqlParser.SignalStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#resignalStatement}.
	 * @param ctx the parse tree
	 */
	void enterResignalStatement(MySqlParser.ResignalStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#resignalStatement}.
	 * @param ctx the parse tree
	 */
	void exitResignalStatement(MySqlParser.ResignalStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#signalConditionInformation}.
	 * @param ctx the parse tree
	 */
	void enterSignalConditionInformation(MySqlParser.SignalConditionInformationContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#signalConditionInformation}.
	 * @param ctx the parse tree
	 */
	void exitSignalConditionInformation(MySqlParser.SignalConditionInformationContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#diagnosticsStatement}.
	 * @param ctx the parse tree
	 */
	void enterDiagnosticsStatement(MySqlParser.DiagnosticsStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#diagnosticsStatement}.
	 * @param ctx the parse tree
	 */
	void exitDiagnosticsStatement(MySqlParser.DiagnosticsStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#diagnosticsConditionInformationName}.
	 * @param ctx the parse tree
	 */
	void enterDiagnosticsConditionInformationName(MySqlParser.DiagnosticsConditionInformationNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#diagnosticsConditionInformationName}.
	 * @param ctx the parse tree
	 */
	void exitDiagnosticsConditionInformationName(MySqlParser.DiagnosticsConditionInformationNameContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeStatements}
	 * labeled alternative in {@link MySqlParser#describeObjectClause}.
	 * @param ctx the parse tree
	 */
	void enterDescribeStatements(MySqlParser.DescribeStatementsContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeStatements}
	 * labeled alternative in {@link MySqlParser#describeObjectClause}.
	 * @param ctx the parse tree
	 */
	void exitDescribeStatements(MySqlParser.DescribeStatementsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code describeConnection}
	 * labeled alternative in {@link MySqlParser#describeObjectClause}.
	 * @param ctx the parse tree
	 */
	void enterDescribeConnection(MySqlParser.DescribeConnectionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code describeConnection}
	 * labeled alternative in {@link MySqlParser#describeObjectClause}.
	 * @param ctx the parse tree
	 */
	void exitDescribeConnection(MySqlParser.DescribeConnectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#fullId}.
	 * @param ctx the parse tree
	 */
	void enterFullId(MySqlParser.FullIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#fullId}.
	 * @param ctx the parse tree
	 */
	void exitFullId(MySqlParser.FullIdContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#tableName}.
	 * @param ctx the parse tree
	 */
	void enterTableName(MySqlParser.TableNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#tableName}.
	 * @param ctx the parse tree
	 */
	void exitTableName(MySqlParser.TableNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#fullColumnName}.
	 * @param ctx the parse tree
	 */
	void enterFullColumnName(MySqlParser.FullColumnNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#fullColumnName}.
	 * @param ctx the parse tree
	 */
	void exitFullColumnName(MySqlParser.FullColumnNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#indexColumnName}.
	 * @param ctx the parse tree
	 */
	void enterIndexColumnName(MySqlParser.IndexColumnNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#indexColumnName}.
	 * @param ctx the parse tree
	 */
	void exitIndexColumnName(MySqlParser.IndexColumnNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#userName}.
	 * @param ctx the parse tree
	 */
	void enterUserName(MySqlParser.UserNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#userName}.
	 * @param ctx the parse tree
	 */
	void exitUserName(MySqlParser.UserNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#mysqlVariable}.
	 * @param ctx the parse tree
	 */
	void enterMysqlVariable(MySqlParser.MysqlVariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#mysqlVariable}.
	 * @param ctx the parse tree
	 */
	void exitMysqlVariable(MySqlParser.MysqlVariableContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#charsetName}.
	 * @param ctx the parse tree
	 */
	void enterCharsetName(MySqlParser.CharsetNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#charsetName}.
	 * @param ctx the parse tree
	 */
	void exitCharsetName(MySqlParser.CharsetNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#collationName}.
	 * @param ctx the parse tree
	 */
	void enterCollationName(MySqlParser.CollationNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#collationName}.
	 * @param ctx the parse tree
	 */
	void exitCollationName(MySqlParser.CollationNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#engineName}.
	 * @param ctx the parse tree
	 */
	void enterEngineName(MySqlParser.EngineNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#engineName}.
	 * @param ctx the parse tree
	 */
	void exitEngineName(MySqlParser.EngineNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#uuidSet}.
	 * @param ctx the parse tree
	 */
	void enterUuidSet(MySqlParser.UuidSetContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#uuidSet}.
	 * @param ctx the parse tree
	 */
	void exitUuidSet(MySqlParser.UuidSetContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#xid}.
	 * @param ctx the parse tree
	 */
	void enterXid(MySqlParser.XidContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#xid}.
	 * @param ctx the parse tree
	 */
	void exitXid(MySqlParser.XidContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#xuidStringId}.
	 * @param ctx the parse tree
	 */
	void enterXuidStringId(MySqlParser.XuidStringIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#xuidStringId}.
	 * @param ctx the parse tree
	 */
	void exitXuidStringId(MySqlParser.XuidStringIdContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#authPlugin}.
	 * @param ctx the parse tree
	 */
	void enterAuthPlugin(MySqlParser.AuthPluginContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#authPlugin}.
	 * @param ctx the parse tree
	 */
	void exitAuthPlugin(MySqlParser.AuthPluginContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#uid}.
	 * @param ctx the parse tree
	 */
	void enterUid(MySqlParser.UidContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#uid}.
	 * @param ctx the parse tree
	 */
	void exitUid(MySqlParser.UidContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#simpleId}.
	 * @param ctx the parse tree
	 */
	void enterSimpleId(MySqlParser.SimpleIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#simpleId}.
	 * @param ctx the parse tree
	 */
	void exitSimpleId(MySqlParser.SimpleIdContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dottedId}.
	 * @param ctx the parse tree
	 */
	void enterDottedId(MySqlParser.DottedIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dottedId}.
	 * @param ctx the parse tree
	 */
	void exitDottedId(MySqlParser.DottedIdContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#decimalLiteral}.
	 * @param ctx the parse tree
	 */
	void enterDecimalLiteral(MySqlParser.DecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#decimalLiteral}.
	 * @param ctx the parse tree
	 */
	void exitDecimalLiteral(MySqlParser.DecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#fileSizeLiteral}.
	 * @param ctx the parse tree
	 */
	void enterFileSizeLiteral(MySqlParser.FileSizeLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#fileSizeLiteral}.
	 * @param ctx the parse tree
	 */
	void exitFileSizeLiteral(MySqlParser.FileSizeLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void enterStringLiteral(MySqlParser.StringLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#stringLiteral}.
	 * @param ctx the parse tree
	 */
	void exitStringLiteral(MySqlParser.StringLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#booleanLiteral}.
	 * @param ctx the parse tree
	 */
	void enterBooleanLiteral(MySqlParser.BooleanLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#booleanLiteral}.
	 * @param ctx the parse tree
	 */
	void exitBooleanLiteral(MySqlParser.BooleanLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#hexadecimalLiteral}.
	 * @param ctx the parse tree
	 */
	void enterHexadecimalLiteral(MySqlParser.HexadecimalLiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#hexadecimalLiteral}.
	 * @param ctx the parse tree
	 */
	void exitHexadecimalLiteral(MySqlParser.HexadecimalLiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#nullNotnull}.
	 * @param ctx the parse tree
	 */
	void enterNullNotnull(MySqlParser.NullNotnullContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#nullNotnull}.
	 * @param ctx the parse tree
	 */
	void exitNullNotnull(MySqlParser.NullNotnullContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterConstant(MySqlParser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitConstant(MySqlParser.ConstantContext ctx);
	/**
	 * Enter a parse tree produced by the {@code stringDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterStringDataType(MySqlParser.StringDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code stringDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitStringDataType(MySqlParser.StringDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nationalStringDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterNationalStringDataType(MySqlParser.NationalStringDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nationalStringDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitNationalStringDataType(MySqlParser.NationalStringDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nationalVaryingStringDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterNationalVaryingStringDataType(MySqlParser.NationalVaryingStringDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nationalVaryingStringDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitNationalVaryingStringDataType(MySqlParser.NationalVaryingStringDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dimensionDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterDimensionDataType(MySqlParser.DimensionDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dimensionDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitDimensionDataType(MySqlParser.DimensionDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterSimpleDataType(MySqlParser.SimpleDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitSimpleDataType(MySqlParser.SimpleDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code collectionDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterCollectionDataType(MySqlParser.CollectionDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code collectionDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitCollectionDataType(MySqlParser.CollectionDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code spatialDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void enterSpatialDataType(MySqlParser.SpatialDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code spatialDataType}
	 * labeled alternative in {@link MySqlParser#dataType}.
	 * @param ctx the parse tree
	 */
	void exitSpatialDataType(MySqlParser.SpatialDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#collectionOptions}.
	 * @param ctx the parse tree
	 */
	void enterCollectionOptions(MySqlParser.CollectionOptionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#collectionOptions}.
	 * @param ctx the parse tree
	 */
	void exitCollectionOptions(MySqlParser.CollectionOptionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#convertedDataType}.
	 * @param ctx the parse tree
	 */
	void enterConvertedDataType(MySqlParser.ConvertedDataTypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#convertedDataType}.
	 * @param ctx the parse tree
	 */
	void exitConvertedDataType(MySqlParser.ConvertedDataTypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#lengthOneDimension}.
	 * @param ctx the parse tree
	 */
	void enterLengthOneDimension(MySqlParser.LengthOneDimensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#lengthOneDimension}.
	 * @param ctx the parse tree
	 */
	void exitLengthOneDimension(MySqlParser.LengthOneDimensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#lengthTwoDimension}.
	 * @param ctx the parse tree
	 */
	void enterLengthTwoDimension(MySqlParser.LengthTwoDimensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#lengthTwoDimension}.
	 * @param ctx the parse tree
	 */
	void exitLengthTwoDimension(MySqlParser.LengthTwoDimensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#lengthTwoOptionalDimension}.
	 * @param ctx the parse tree
	 */
	void enterLengthTwoOptionalDimension(MySqlParser.LengthTwoOptionalDimensionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#lengthTwoOptionalDimension}.
	 * @param ctx the parse tree
	 */
	void exitLengthTwoOptionalDimension(MySqlParser.LengthTwoOptionalDimensionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#uidList}.
	 * @param ctx the parse tree
	 */
	void enterUidList(MySqlParser.UidListContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#uidList}.
	 * @param ctx the parse tree
	 */
	void exitUidList(MySqlParser.UidListContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#tables}.
	 * @param ctx the parse tree
	 */
	void enterTables(MySqlParser.TablesContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#tables}.
	 * @param ctx the parse tree
	 */
	void exitTables(MySqlParser.TablesContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#indexColumnNames}.
	 * @param ctx the parse tree
	 */
	void enterIndexColumnNames(MySqlParser.IndexColumnNamesContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#indexColumnNames}.
	 * @param ctx the parse tree
	 */
	void exitIndexColumnNames(MySqlParser.IndexColumnNamesContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#expressions}.
	 * @param ctx the parse tree
	 */
	void enterExpressions(MySqlParser.ExpressionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#expressions}.
	 * @param ctx the parse tree
	 */
	void exitExpressions(MySqlParser.ExpressionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#expressionsForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterExpressionsForUpdate(MySqlParser.ExpressionsForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#expressionsForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitExpressionsForUpdate(MySqlParser.ExpressionsForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#expressionsWithDefaults}.
	 * @param ctx the parse tree
	 */
	void enterExpressionsWithDefaults(MySqlParser.ExpressionsWithDefaultsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#expressionsWithDefaults}.
	 * @param ctx the parse tree
	 */
	void exitExpressionsWithDefaults(MySqlParser.ExpressionsWithDefaultsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#constants}.
	 * @param ctx the parse tree
	 */
	void enterConstants(MySqlParser.ConstantsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#constants}.
	 * @param ctx the parse tree
	 */
	void exitConstants(MySqlParser.ConstantsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#simpleStrings}.
	 * @param ctx the parse tree
	 */
	void enterSimpleStrings(MySqlParser.SimpleStringsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#simpleStrings}.
	 * @param ctx the parse tree
	 */
	void exitSimpleStrings(MySqlParser.SimpleStringsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#userVariables}.
	 * @param ctx the parse tree
	 */
	void enterUserVariables(MySqlParser.UserVariablesContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#userVariables}.
	 * @param ctx the parse tree
	 */
	void exitUserVariables(MySqlParser.UserVariablesContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void enterDefaultValue(MySqlParser.DefaultValueContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#defaultValue}.
	 * @param ctx the parse tree
	 */
	void exitDefaultValue(MySqlParser.DefaultValueContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#currentTimestamp}.
	 * @param ctx the parse tree
	 */
	void enterCurrentTimestamp(MySqlParser.CurrentTimestampContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#currentTimestamp}.
	 * @param ctx the parse tree
	 */
	void exitCurrentTimestamp(MySqlParser.CurrentTimestampContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#expressionOrDefault}.
	 * @param ctx the parse tree
	 */
	void enterExpressionOrDefault(MySqlParser.ExpressionOrDefaultContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#expressionOrDefault}.
	 * @param ctx the parse tree
	 */
	void exitExpressionOrDefault(MySqlParser.ExpressionOrDefaultContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#ifExists}.
	 * @param ctx the parse tree
	 */
	void enterIfExists(MySqlParser.IfExistsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#ifExists}.
	 * @param ctx the parse tree
	 */
	void exitIfExists(MySqlParser.IfExistsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#ifNotExists}.
	 * @param ctx the parse tree
	 */
	void enterIfNotExists(MySqlParser.IfNotExistsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#ifNotExists}.
	 * @param ctx the parse tree
	 */
	void exitIfNotExists(MySqlParser.IfNotExistsContext ctx);
	/**
	 * Enter a parse tree produced by the {@code specificFunctionCall}
	 * labeled alternative in {@link MySqlParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterSpecificFunctionCall(MySqlParser.SpecificFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code specificFunctionCall}
	 * labeled alternative in {@link MySqlParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitSpecificFunctionCall(MySqlParser.SpecificFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code aggregateFunctionCall}
	 * labeled alternative in {@link MySqlParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterAggregateFunctionCall(MySqlParser.AggregateFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code aggregateFunctionCall}
	 * labeled alternative in {@link MySqlParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitAggregateFunctionCall(MySqlParser.AggregateFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code scalarFunctionCall}
	 * labeled alternative in {@link MySqlParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterScalarFunctionCall(MySqlParser.ScalarFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code scalarFunctionCall}
	 * labeled alternative in {@link MySqlParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitScalarFunctionCall(MySqlParser.ScalarFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code udfFunctionCall}
	 * labeled alternative in {@link MySqlParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterUdfFunctionCall(MySqlParser.UdfFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code udfFunctionCall}
	 * labeled alternative in {@link MySqlParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitUdfFunctionCall(MySqlParser.UdfFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code passwordFunctionCall}
	 * labeled alternative in {@link MySqlParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void enterPasswordFunctionCall(MySqlParser.PasswordFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code passwordFunctionCall}
	 * labeled alternative in {@link MySqlParser#functionCall}.
	 * @param ctx the parse tree
	 */
	void exitPasswordFunctionCall(MySqlParser.PasswordFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code simpleFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void enterSimpleFunctionCall(MySqlParser.SimpleFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code simpleFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void exitSimpleFunctionCall(MySqlParser.SimpleFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code dataTypeFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void enterDataTypeFunctionCall(MySqlParser.DataTypeFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code dataTypeFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void exitDataTypeFunctionCall(MySqlParser.DataTypeFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code valuesFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void enterValuesFunctionCall(MySqlParser.ValuesFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code valuesFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void exitValuesFunctionCall(MySqlParser.ValuesFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code caseFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void enterCaseFunctionCall(MySqlParser.CaseFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code caseFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void exitCaseFunctionCall(MySqlParser.CaseFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code charFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void enterCharFunctionCall(MySqlParser.CharFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code charFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void exitCharFunctionCall(MySqlParser.CharFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code positionFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void enterPositionFunctionCall(MySqlParser.PositionFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code positionFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void exitPositionFunctionCall(MySqlParser.PositionFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code substrFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void enterSubstrFunctionCall(MySqlParser.SubstrFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code substrFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void exitSubstrFunctionCall(MySqlParser.SubstrFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code trimFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void enterTrimFunctionCall(MySqlParser.TrimFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code trimFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void exitTrimFunctionCall(MySqlParser.TrimFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code weightFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void enterWeightFunctionCall(MySqlParser.WeightFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code weightFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void exitWeightFunctionCall(MySqlParser.WeightFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code extractFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void enterExtractFunctionCall(MySqlParser.ExtractFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code extractFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void exitExtractFunctionCall(MySqlParser.ExtractFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by the {@code getFormatFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void enterGetFormatFunctionCall(MySqlParser.GetFormatFunctionCallContext ctx);
	/**
	 * Exit a parse tree produced by the {@code getFormatFunctionCall}
	 * labeled alternative in {@link MySqlParser#specificFunction}.
	 * @param ctx the parse tree
	 */
	void exitGetFormatFunctionCall(MySqlParser.GetFormatFunctionCallContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#caseFuncAlternative}.
	 * @param ctx the parse tree
	 */
	void enterCaseFuncAlternative(MySqlParser.CaseFuncAlternativeContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#caseFuncAlternative}.
	 * @param ctx the parse tree
	 */
	void exitCaseFuncAlternative(MySqlParser.CaseFuncAlternativeContext ctx);
	/**
	 * Enter a parse tree produced by the {@code levelWeightList}
	 * labeled alternative in {@link MySqlParser#levelsInWeightString}.
	 * @param ctx the parse tree
	 */
	void enterLevelWeightList(MySqlParser.LevelWeightListContext ctx);
	/**
	 * Exit a parse tree produced by the {@code levelWeightList}
	 * labeled alternative in {@link MySqlParser#levelsInWeightString}.
	 * @param ctx the parse tree
	 */
	void exitLevelWeightList(MySqlParser.LevelWeightListContext ctx);
	/**
	 * Enter a parse tree produced by the {@code levelWeightRange}
	 * labeled alternative in {@link MySqlParser#levelsInWeightString}.
	 * @param ctx the parse tree
	 */
	void enterLevelWeightRange(MySqlParser.LevelWeightRangeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code levelWeightRange}
	 * labeled alternative in {@link MySqlParser#levelsInWeightString}.
	 * @param ctx the parse tree
	 */
	void exitLevelWeightRange(MySqlParser.LevelWeightRangeContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#levelInWeightListElement}.
	 * @param ctx the parse tree
	 */
	void enterLevelInWeightListElement(MySqlParser.LevelInWeightListElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#levelInWeightListElement}.
	 * @param ctx the parse tree
	 */
	void exitLevelInWeightListElement(MySqlParser.LevelInWeightListElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#aggregateWindowedFunction}.
	 * @param ctx the parse tree
	 */
	void enterAggregateWindowedFunction(MySqlParser.AggregateWindowedFunctionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#aggregateWindowedFunction}.
	 * @param ctx the parse tree
	 */
	void exitAggregateWindowedFunction(MySqlParser.AggregateWindowedFunctionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#scalarFunctionName}.
	 * @param ctx the parse tree
	 */
	void enterScalarFunctionName(MySqlParser.ScalarFunctionNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#scalarFunctionName}.
	 * @param ctx the parse tree
	 */
	void exitScalarFunctionName(MySqlParser.ScalarFunctionNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#passwordFunctionClause}.
	 * @param ctx the parse tree
	 */
	void enterPasswordFunctionClause(MySqlParser.PasswordFunctionClauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#passwordFunctionClause}.
	 * @param ctx the parse tree
	 */
	void exitPasswordFunctionClause(MySqlParser.PasswordFunctionClauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#functionArgs}.
	 * @param ctx the parse tree
	 */
	void enterFunctionArgs(MySqlParser.FunctionArgsContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#functionArgs}.
	 * @param ctx the parse tree
	 */
	void exitFunctionArgs(MySqlParser.FunctionArgsContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#functionArg}.
	 * @param ctx the parse tree
	 */
	void enterFunctionArg(MySqlParser.FunctionArgContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#functionArg}.
	 * @param ctx the parse tree
	 */
	void exitFunctionArg(MySqlParser.FunctionArgContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isExpression}
	 * labeled alternative in {@link MySqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterIsExpression(MySqlParser.IsExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isExpression}
	 * labeled alternative in {@link MySqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitIsExpression(MySqlParser.IsExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code notExpression}
	 * labeled alternative in {@link MySqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterNotExpression(MySqlParser.NotExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code notExpression}
	 * labeled alternative in {@link MySqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitNotExpression(MySqlParser.NotExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalExpression}
	 * labeled alternative in {@link MySqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalExpression(MySqlParser.LogicalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalExpression}
	 * labeled alternative in {@link MySqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalExpression(MySqlParser.LogicalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predicateExpression}
	 * labeled alternative in {@link MySqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterPredicateExpression(MySqlParser.PredicateExpressionContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predicateExpression}
	 * labeled alternative in {@link MySqlParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitPredicateExpression(MySqlParser.PredicateExpressionContext ctx);
	/**
	 * Enter a parse tree produced by the {@code soundsLikePredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterSoundsLikePredicate(MySqlParser.SoundsLikePredicateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code soundsLikePredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitSoundsLikePredicate(MySqlParser.SoundsLikePredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expressionAtomPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterExpressionAtomPredicate(MySqlParser.ExpressionAtomPredicateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expressionAtomPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitExpressionAtomPredicate(MySqlParser.ExpressionAtomPredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterInPredicate(MySqlParser.InPredicateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitInPredicate(MySqlParser.InPredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryComparasionPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryComparasionPredicate(MySqlParser.SubqueryComparasionPredicateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryComparasionPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryComparasionPredicate(MySqlParser.SubqueryComparasionPredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code betweenPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterBetweenPredicate(MySqlParser.BetweenPredicateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code betweenPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitBetweenPredicate(MySqlParser.BetweenPredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryComparasionPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterBinaryComparasionPredicate(MySqlParser.BinaryComparasionPredicateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryComparasionPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitBinaryComparasionPredicate(MySqlParser.BinaryComparasionPredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isNullPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterIsNullPredicate(MySqlParser.IsNullPredicateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isNullPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitIsNullPredicate(MySqlParser.IsNullPredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code likePredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterLikePredicate(MySqlParser.LikePredicateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code likePredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitLikePredicate(MySqlParser.LikePredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code regexpPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void enterRegexpPredicate(MySqlParser.RegexpPredicateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code regexpPredicate}
	 * labeled alternative in {@link MySqlParser#predicate}.
	 * @param ctx the parse tree
	 */
	void exitRegexpPredicate(MySqlParser.RegexpPredicateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code logicalExpressionForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterLogicalExpressionForUpdate(MySqlParser.LogicalExpressionForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code logicalExpressionForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitLogicalExpressionForUpdate(MySqlParser.LogicalExpressionForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code predicateExpressionForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterPredicateExpressionForUpdate(MySqlParser.PredicateExpressionForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code predicateExpressionForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitPredicateExpressionForUpdate(MySqlParser.PredicateExpressionForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isExpressionForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterIsExpressionForUpdate(MySqlParser.IsExpressionForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isExpressionForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitIsExpressionForUpdate(MySqlParser.IsExpressionForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code notExpressionForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterNotExpressionForUpdate(MySqlParser.NotExpressionForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code notExpressionForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitNotExpressionForUpdate(MySqlParser.NotExpressionForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryComparasionPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterBinaryComparasionPredicateForUpdate(MySqlParser.BinaryComparasionPredicateForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryComparasionPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitBinaryComparasionPredicateForUpdate(MySqlParser.BinaryComparasionPredicateForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code likePredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterLikePredicateForUpdate(MySqlParser.LikePredicateForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code likePredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitLikePredicateForUpdate(MySqlParser.LikePredicateForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code regexpPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterRegexpPredicateForUpdate(MySqlParser.RegexpPredicateForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code regexpPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitRegexpPredicateForUpdate(MySqlParser.RegexpPredicateForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code isNullPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterIsNullPredicateForUpdate(MySqlParser.IsNullPredicateForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code isNullPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitIsNullPredicateForUpdate(MySqlParser.IsNullPredicateForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code betweenPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterBetweenPredicateForUpdate(MySqlParser.BetweenPredicateForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code betweenPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitBetweenPredicateForUpdate(MySqlParser.BetweenPredicateForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code soundsLikePredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterSoundsLikePredicateForUpdate(MySqlParser.SoundsLikePredicateForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code soundsLikePredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitSoundsLikePredicateForUpdate(MySqlParser.SoundsLikePredicateForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code inPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterInPredicateForUpdate(MySqlParser.InPredicateForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code inPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitInPredicateForUpdate(MySqlParser.InPredicateForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryComparasionPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryComparasionPredicateForUpdate(MySqlParser.SubqueryComparasionPredicateForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryComparasionPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryComparasionPredicateForUpdate(MySqlParser.SubqueryComparasionPredicateForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code expressionAtomPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterExpressionAtomPredicateForUpdate(MySqlParser.ExpressionAtomPredicateForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code expressionAtomPredicateForUpdate}
	 * labeled alternative in {@link MySqlParser#predicateForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitExpressionAtomPredicateForUpdate(MySqlParser.ExpressionAtomPredicateForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intervalExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterIntervalExpressionAtomForUpdate(MySqlParser.IntervalExpressionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intervalExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitIntervalExpressionAtomForUpdate(MySqlParser.IntervalExpressionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code fullColumnNameExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterFullColumnNameExpressionAtomForUpdate(MySqlParser.FullColumnNameExpressionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code fullColumnNameExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitFullColumnNameExpressionAtomForUpdate(MySqlParser.FullColumnNameExpressionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mysqlVariableExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterMysqlVariableExpressionAtomForUpdate(MySqlParser.MysqlVariableExpressionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mysqlVariableExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitMysqlVariableExpressionAtomForUpdate(MySqlParser.MysqlVariableExpressionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unaryExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpressionAtomForUpdate(MySqlParser.UnaryExpressionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unaryExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpressionAtomForUpdate(MySqlParser.UnaryExpressionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nestedRowExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterNestedRowExpressionAtomForUpdate(MySqlParser.NestedRowExpressionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nestedRowExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitNestedRowExpressionAtomForUpdate(MySqlParser.NestedRowExpressionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryExpessionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryExpessionAtomForUpdate(MySqlParser.SubqueryExpessionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryExpessionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryExpessionAtomForUpdate(MySqlParser.SubqueryExpessionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bitExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterBitExpressionAtomForUpdate(MySqlParser.BitExpressionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bitExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitBitExpressionAtomForUpdate(MySqlParser.BitExpressionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mathExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterMathExpressionAtomForUpdate(MySqlParser.MathExpressionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mathExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitMathExpressionAtomForUpdate(MySqlParser.MathExpressionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code constantExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterConstantExpressionAtomForUpdate(MySqlParser.ConstantExpressionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code constantExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitConstantExpressionAtomForUpdate(MySqlParser.ConstantExpressionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code collateExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterCollateExpressionAtomForUpdate(MySqlParser.CollateExpressionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code collateExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitCollateExpressionAtomForUpdate(MySqlParser.CollateExpressionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterBinaryExpressionAtomForUpdate(MySqlParser.BinaryExpressionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitBinaryExpressionAtomForUpdate(MySqlParser.BinaryExpressionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nestedExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterNestedExpressionAtomForUpdate(MySqlParser.NestedExpressionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nestedExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitNestedExpressionAtomForUpdate(MySqlParser.NestedExpressionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code existsExpessionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterExistsExpessionAtomForUpdate(MySqlParser.ExistsExpessionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code existsExpessionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitExistsExpessionAtomForUpdate(MySqlParser.ExistsExpessionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCallExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCallExpressionAtomForUpdate(MySqlParser.FunctionCallExpressionAtomForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCallExpressionAtomForUpdate}
	 * labeled alternative in {@link MySqlParser#expressionAtomForUpdate}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCallExpressionAtomForUpdate(MySqlParser.FunctionCallExpressionAtomForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by the {@code unaryExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpressionAtom(MySqlParser.UnaryExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code unaryExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpressionAtom(MySqlParser.UnaryExpressionAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code collateExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterCollateExpressionAtom(MySqlParser.CollateExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code collateExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitCollateExpressionAtom(MySqlParser.CollateExpressionAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code subqueryExpessionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterSubqueryExpessionAtom(MySqlParser.SubqueryExpessionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code subqueryExpessionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitSubqueryExpessionAtom(MySqlParser.SubqueryExpessionAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mysqlVariableExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterMysqlVariableExpressionAtom(MySqlParser.MysqlVariableExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mysqlVariableExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitMysqlVariableExpressionAtom(MySqlParser.MysqlVariableExpressionAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nestedExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterNestedExpressionAtom(MySqlParser.NestedExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nestedExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitNestedExpressionAtom(MySqlParser.NestedExpressionAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code nestedRowExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterNestedRowExpressionAtom(MySqlParser.NestedRowExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code nestedRowExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitNestedRowExpressionAtom(MySqlParser.NestedRowExpressionAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code mathExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterMathExpressionAtom(MySqlParser.MathExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code mathExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitMathExpressionAtom(MySqlParser.MathExpressionAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code intervalExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterIntervalExpressionAtom(MySqlParser.IntervalExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code intervalExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitIntervalExpressionAtom(MySqlParser.IntervalExpressionAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code existsExpessionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterExistsExpessionAtom(MySqlParser.ExistsExpessionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code existsExpessionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitExistsExpessionAtom(MySqlParser.ExistsExpessionAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code constantExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterConstantExpressionAtom(MySqlParser.ConstantExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code constantExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitConstantExpressionAtom(MySqlParser.ConstantExpressionAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code functionCallExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterFunctionCallExpressionAtom(MySqlParser.FunctionCallExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code functionCallExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitFunctionCallExpressionAtom(MySqlParser.FunctionCallExpressionAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code binaryExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterBinaryExpressionAtom(MySqlParser.BinaryExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code binaryExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitBinaryExpressionAtom(MySqlParser.BinaryExpressionAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code fullColumnNameExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterFullColumnNameExpressionAtom(MySqlParser.FullColumnNameExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code fullColumnNameExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitFullColumnNameExpressionAtom(MySqlParser.FullColumnNameExpressionAtomContext ctx);
	/**
	 * Enter a parse tree produced by the {@code bitExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void enterBitExpressionAtom(MySqlParser.BitExpressionAtomContext ctx);
	/**
	 * Exit a parse tree produced by the {@code bitExpressionAtom}
	 * labeled alternative in {@link MySqlParser#expressionAtom}.
	 * @param ctx the parse tree
	 */
	void exitBitExpressionAtom(MySqlParser.BitExpressionAtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#unaryOperator}.
	 * @param ctx the parse tree
	 */
	void enterUnaryOperator(MySqlParser.UnaryOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#unaryOperator}.
	 * @param ctx the parse tree
	 */
	void exitUnaryOperator(MySqlParser.UnaryOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void enterComparisonOperator(MySqlParser.ComparisonOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#comparisonOperator}.
	 * @param ctx the parse tree
	 */
	void exitComparisonOperator(MySqlParser.ComparisonOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#logicalOperator}.
	 * @param ctx the parse tree
	 */
	void enterLogicalOperator(MySqlParser.LogicalOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#logicalOperator}.
	 * @param ctx the parse tree
	 */
	void exitLogicalOperator(MySqlParser.LogicalOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#bitOperator}.
	 * @param ctx the parse tree
	 */
	void enterBitOperator(MySqlParser.BitOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#bitOperator}.
	 * @param ctx the parse tree
	 */
	void exitBitOperator(MySqlParser.BitOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#mathOperator}.
	 * @param ctx the parse tree
	 */
	void enterMathOperator(MySqlParser.MathOperatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#mathOperator}.
	 * @param ctx the parse tree
	 */
	void exitMathOperator(MySqlParser.MathOperatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#charsetNameBase}.
	 * @param ctx the parse tree
	 */
	void enterCharsetNameBase(MySqlParser.CharsetNameBaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#charsetNameBase}.
	 * @param ctx the parse tree
	 */
	void exitCharsetNameBase(MySqlParser.CharsetNameBaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#transactionLevelBase}.
	 * @param ctx the parse tree
	 */
	void enterTransactionLevelBase(MySqlParser.TransactionLevelBaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#transactionLevelBase}.
	 * @param ctx the parse tree
	 */
	void exitTransactionLevelBase(MySqlParser.TransactionLevelBaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#privilegesBase}.
	 * @param ctx the parse tree
	 */
	void enterPrivilegesBase(MySqlParser.PrivilegesBaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#privilegesBase}.
	 * @param ctx the parse tree
	 */
	void exitPrivilegesBase(MySqlParser.PrivilegesBaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#intervalTypeBase}.
	 * @param ctx the parse tree
	 */
	void enterIntervalTypeBase(MySqlParser.IntervalTypeBaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#intervalTypeBase}.
	 * @param ctx the parse tree
	 */
	void exitIntervalTypeBase(MySqlParser.IntervalTypeBaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#dataTypeBase}.
	 * @param ctx the parse tree
	 */
	void enterDataTypeBase(MySqlParser.DataTypeBaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#dataTypeBase}.
	 * @param ctx the parse tree
	 */
	void exitDataTypeBase(MySqlParser.DataTypeBaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#keywordsCanBeId}.
	 * @param ctx the parse tree
	 */
	void enterKeywordsCanBeId(MySqlParser.KeywordsCanBeIdContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#keywordsCanBeId}.
	 * @param ctx the parse tree
	 */
	void exitKeywordsCanBeId(MySqlParser.KeywordsCanBeIdContext ctx);
	/**
	 * Enter a parse tree produced by {@link MySqlParser#functionNameBase}.
	 * @param ctx the parse tree
	 */
	void enterFunctionNameBase(MySqlParser.FunctionNameBaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link MySqlParser#functionNameBase}.
	 * @param ctx the parse tree
	 */
	void exitFunctionNameBase(MySqlParser.FunctionNameBaseContext ctx);
}