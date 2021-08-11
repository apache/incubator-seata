// Generated from /Users/apple/Desktop/程序用文件夹/seata/sqlparser/seata-sqlparser-antlr/src/main/java/io/seata/sqlparser/antlr/oracle/antlr/PlSqlParser.g4 by ANTLR 4.9.1
package io.seata.sqlparser.antlr.oracle.antlr;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link PlSqlParserParser}.
 */
public interface PlSqlParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#sql_script}.
	 * @param ctx the parse tree
	 */
	void enterSql_script(PlSqlParserParser.Sql_scriptContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#sql_script}.
	 * @param ctx the parse tree
	 */
	void exitSql_script(PlSqlParserParser.Sql_scriptContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#unit_statement}.
	 * @param ctx the parse tree
	 */
	void enterUnit_statement(PlSqlParserParser.Unit_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#unit_statement}.
	 * @param ctx the parse tree
	 */
	void exitUnit_statement(PlSqlParserParser.Unit_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_function}.
	 * @param ctx the parse tree
	 */
	void enterDrop_function(PlSqlParserParser.Drop_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_function}.
	 * @param ctx the parse tree
	 */
	void exitDrop_function(PlSqlParserParser.Drop_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_function}.
	 * @param ctx the parse tree
	 */
	void enterAlter_function(PlSqlParserParser.Alter_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_function}.
	 * @param ctx the parse tree
	 */
	void exitAlter_function(PlSqlParserParser.Alter_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_function_body}.
	 * @param ctx the parse tree
	 */
	void enterCreate_function_body(PlSqlParserParser.Create_function_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_function_body}.
	 * @param ctx the parse tree
	 */
	void exitCreate_function_body(PlSqlParserParser.Create_function_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#parallel_enable_clause}.
	 * @param ctx the parse tree
	 */
	void enterParallel_enable_clause(PlSqlParserParser.Parallel_enable_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#parallel_enable_clause}.
	 * @param ctx the parse tree
	 */
	void exitParallel_enable_clause(PlSqlParserParser.Parallel_enable_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#partition_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterPartition_by_clause(PlSqlParserParser.Partition_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#partition_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitPartition_by_clause(PlSqlParserParser.Partition_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#result_cache_clause}.
	 * @param ctx the parse tree
	 */
	void enterResult_cache_clause(PlSqlParserParser.Result_cache_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#result_cache_clause}.
	 * @param ctx the parse tree
	 */
	void exitResult_cache_clause(PlSqlParserParser.Result_cache_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#relies_on_part}.
	 * @param ctx the parse tree
	 */
	void enterRelies_on_part(PlSqlParserParser.Relies_on_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#relies_on_part}.
	 * @param ctx the parse tree
	 */
	void exitRelies_on_part(PlSqlParserParser.Relies_on_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#streaming_clause}.
	 * @param ctx the parse tree
	 */
	void enterStreaming_clause(PlSqlParserParser.Streaming_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#streaming_clause}.
	 * @param ctx the parse tree
	 */
	void exitStreaming_clause(PlSqlParserParser.Streaming_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_package}.
	 * @param ctx the parse tree
	 */
	void enterDrop_package(PlSqlParserParser.Drop_packageContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_package}.
	 * @param ctx the parse tree
	 */
	void exitDrop_package(PlSqlParserParser.Drop_packageContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_package}.
	 * @param ctx the parse tree
	 */
	void enterAlter_package(PlSqlParserParser.Alter_packageContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_package}.
	 * @param ctx the parse tree
	 */
	void exitAlter_package(PlSqlParserParser.Alter_packageContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_package}.
	 * @param ctx the parse tree
	 */
	void enterCreate_package(PlSqlParserParser.Create_packageContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_package}.
	 * @param ctx the parse tree
	 */
	void exitCreate_package(PlSqlParserParser.Create_packageContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_package_body}.
	 * @param ctx the parse tree
	 */
	void enterCreate_package_body(PlSqlParserParser.Create_package_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_package_body}.
	 * @param ctx the parse tree
	 */
	void exitCreate_package_body(PlSqlParserParser.Create_package_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#package_obj_spec}.
	 * @param ctx the parse tree
	 */
	void enterPackage_obj_spec(PlSqlParserParser.Package_obj_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#package_obj_spec}.
	 * @param ctx the parse tree
	 */
	void exitPackage_obj_spec(PlSqlParserParser.Package_obj_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#procedure_spec}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_spec(PlSqlParserParser.Procedure_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#procedure_spec}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_spec(PlSqlParserParser.Procedure_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#function_spec}.
	 * @param ctx the parse tree
	 */
	void enterFunction_spec(PlSqlParserParser.Function_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#function_spec}.
	 * @param ctx the parse tree
	 */
	void exitFunction_spec(PlSqlParserParser.Function_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#package_obj_body}.
	 * @param ctx the parse tree
	 */
	void enterPackage_obj_body(PlSqlParserParser.Package_obj_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#package_obj_body}.
	 * @param ctx the parse tree
	 */
	void exitPackage_obj_body(PlSqlParserParser.Package_obj_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_procedure}.
	 * @param ctx the parse tree
	 */
	void enterDrop_procedure(PlSqlParserParser.Drop_procedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_procedure}.
	 * @param ctx the parse tree
	 */
	void exitDrop_procedure(PlSqlParserParser.Drop_procedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_procedure}.
	 * @param ctx the parse tree
	 */
	void enterAlter_procedure(PlSqlParserParser.Alter_procedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_procedure}.
	 * @param ctx the parse tree
	 */
	void exitAlter_procedure(PlSqlParserParser.Alter_procedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#function_body}.
	 * @param ctx the parse tree
	 */
	void enterFunction_body(PlSqlParserParser.Function_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#function_body}.
	 * @param ctx the parse tree
	 */
	void exitFunction_body(PlSqlParserParser.Function_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#procedure_body}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_body(PlSqlParserParser.Procedure_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#procedure_body}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_body(PlSqlParserParser.Procedure_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_procedure_body}.
	 * @param ctx the parse tree
	 */
	void enterCreate_procedure_body(PlSqlParserParser.Create_procedure_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_procedure_body}.
	 * @param ctx the parse tree
	 */
	void exitCreate_procedure_body(PlSqlParserParser.Create_procedure_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_trigger}.
	 * @param ctx the parse tree
	 */
	void enterDrop_trigger(PlSqlParserParser.Drop_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_trigger}.
	 * @param ctx the parse tree
	 */
	void exitDrop_trigger(PlSqlParserParser.Drop_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_trigger}.
	 * @param ctx the parse tree
	 */
	void enterAlter_trigger(PlSqlParserParser.Alter_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_trigger}.
	 * @param ctx the parse tree
	 */
	void exitAlter_trigger(PlSqlParserParser.Alter_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_trigger}.
	 * @param ctx the parse tree
	 */
	void enterCreate_trigger(PlSqlParserParser.Create_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_trigger}.
	 * @param ctx the parse tree
	 */
	void exitCreate_trigger(PlSqlParserParser.Create_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#trigger_follows_clause}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_follows_clause(PlSqlParserParser.Trigger_follows_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#trigger_follows_clause}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_follows_clause(PlSqlParserParser.Trigger_follows_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#trigger_when_clause}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_when_clause(PlSqlParserParser.Trigger_when_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#trigger_when_clause}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_when_clause(PlSqlParserParser.Trigger_when_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#simple_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void enterSimple_dml_trigger(PlSqlParserParser.Simple_dml_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#simple_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void exitSimple_dml_trigger(PlSqlParserParser.Simple_dml_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#for_each_row}.
	 * @param ctx the parse tree
	 */
	void enterFor_each_row(PlSqlParserParser.For_each_rowContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#for_each_row}.
	 * @param ctx the parse tree
	 */
	void exitFor_each_row(PlSqlParserParser.For_each_rowContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#compound_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void enterCompound_dml_trigger(PlSqlParserParser.Compound_dml_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#compound_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void exitCompound_dml_trigger(PlSqlParserParser.Compound_dml_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#non_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void enterNon_dml_trigger(PlSqlParserParser.Non_dml_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#non_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void exitNon_dml_trigger(PlSqlParserParser.Non_dml_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#trigger_body}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_body(PlSqlParserParser.Trigger_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#trigger_body}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_body(PlSqlParserParser.Trigger_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#routine_clause}.
	 * @param ctx the parse tree
	 */
	void enterRoutine_clause(PlSqlParserParser.Routine_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#routine_clause}.
	 * @param ctx the parse tree
	 */
	void exitRoutine_clause(PlSqlParserParser.Routine_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#compound_trigger_block}.
	 * @param ctx the parse tree
	 */
	void enterCompound_trigger_block(PlSqlParserParser.Compound_trigger_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#compound_trigger_block}.
	 * @param ctx the parse tree
	 */
	void exitCompound_trigger_block(PlSqlParserParser.Compound_trigger_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#timing_point_section}.
	 * @param ctx the parse tree
	 */
	void enterTiming_point_section(PlSqlParserParser.Timing_point_sectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#timing_point_section}.
	 * @param ctx the parse tree
	 */
	void exitTiming_point_section(PlSqlParserParser.Timing_point_sectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#non_dml_event}.
	 * @param ctx the parse tree
	 */
	void enterNon_dml_event(PlSqlParserParser.Non_dml_eventContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#non_dml_event}.
	 * @param ctx the parse tree
	 */
	void exitNon_dml_event(PlSqlParserParser.Non_dml_eventContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#dml_event_clause}.
	 * @param ctx the parse tree
	 */
	void enterDml_event_clause(PlSqlParserParser.Dml_event_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#dml_event_clause}.
	 * @param ctx the parse tree
	 */
	void exitDml_event_clause(PlSqlParserParser.Dml_event_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#dml_event_element}.
	 * @param ctx the parse tree
	 */
	void enterDml_event_element(PlSqlParserParser.Dml_event_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#dml_event_element}.
	 * @param ctx the parse tree
	 */
	void exitDml_event_element(PlSqlParserParser.Dml_event_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#dml_event_nested_clause}.
	 * @param ctx the parse tree
	 */
	void enterDml_event_nested_clause(PlSqlParserParser.Dml_event_nested_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#dml_event_nested_clause}.
	 * @param ctx the parse tree
	 */
	void exitDml_event_nested_clause(PlSqlParserParser.Dml_event_nested_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#referencing_clause}.
	 * @param ctx the parse tree
	 */
	void enterReferencing_clause(PlSqlParserParser.Referencing_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#referencing_clause}.
	 * @param ctx the parse tree
	 */
	void exitReferencing_clause(PlSqlParserParser.Referencing_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#referencing_element}.
	 * @param ctx the parse tree
	 */
	void enterReferencing_element(PlSqlParserParser.Referencing_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#referencing_element}.
	 * @param ctx the parse tree
	 */
	void exitReferencing_element(PlSqlParserParser.Referencing_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_type}.
	 * @param ctx the parse tree
	 */
	void enterDrop_type(PlSqlParserParser.Drop_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_type}.
	 * @param ctx the parse tree
	 */
	void exitDrop_type(PlSqlParserParser.Drop_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_type}.
	 * @param ctx the parse tree
	 */
	void enterAlter_type(PlSqlParserParser.Alter_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_type}.
	 * @param ctx the parse tree
	 */
	void exitAlter_type(PlSqlParserParser.Alter_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#compile_type_clause}.
	 * @param ctx the parse tree
	 */
	void enterCompile_type_clause(PlSqlParserParser.Compile_type_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#compile_type_clause}.
	 * @param ctx the parse tree
	 */
	void exitCompile_type_clause(PlSqlParserParser.Compile_type_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#replace_type_clause}.
	 * @param ctx the parse tree
	 */
	void enterReplace_type_clause(PlSqlParserParser.Replace_type_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#replace_type_clause}.
	 * @param ctx the parse tree
	 */
	void exitReplace_type_clause(PlSqlParserParser.Replace_type_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_method_spec}.
	 * @param ctx the parse tree
	 */
	void enterAlter_method_spec(PlSqlParserParser.Alter_method_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_method_spec}.
	 * @param ctx the parse tree
	 */
	void exitAlter_method_spec(PlSqlParserParser.Alter_method_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_method_element}.
	 * @param ctx the parse tree
	 */
	void enterAlter_method_element(PlSqlParserParser.Alter_method_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_method_element}.
	 * @param ctx the parse tree
	 */
	void exitAlter_method_element(PlSqlParserParser.Alter_method_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_attribute_definition}.
	 * @param ctx the parse tree
	 */
	void enterAlter_attribute_definition(PlSqlParserParser.Alter_attribute_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_attribute_definition}.
	 * @param ctx the parse tree
	 */
	void exitAlter_attribute_definition(PlSqlParserParser.Alter_attribute_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#attribute_definition}.
	 * @param ctx the parse tree
	 */
	void enterAttribute_definition(PlSqlParserParser.Attribute_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#attribute_definition}.
	 * @param ctx the parse tree
	 */
	void exitAttribute_definition(PlSqlParserParser.Attribute_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_collection_clauses}.
	 * @param ctx the parse tree
	 */
	void enterAlter_collection_clauses(PlSqlParserParser.Alter_collection_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_collection_clauses}.
	 * @param ctx the parse tree
	 */
	void exitAlter_collection_clauses(PlSqlParserParser.Alter_collection_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#dependent_handling_clause}.
	 * @param ctx the parse tree
	 */
	void enterDependent_handling_clause(PlSqlParserParser.Dependent_handling_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#dependent_handling_clause}.
	 * @param ctx the parse tree
	 */
	void exitDependent_handling_clause(PlSqlParserParser.Dependent_handling_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#dependent_exceptions_part}.
	 * @param ctx the parse tree
	 */
	void enterDependent_exceptions_part(PlSqlParserParser.Dependent_exceptions_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#dependent_exceptions_part}.
	 * @param ctx the parse tree
	 */
	void exitDependent_exceptions_part(PlSqlParserParser.Dependent_exceptions_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_type}.
	 * @param ctx the parse tree
	 */
	void enterCreate_type(PlSqlParserParser.Create_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_type}.
	 * @param ctx the parse tree
	 */
	void exitCreate_type(PlSqlParserParser.Create_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#type_definition}.
	 * @param ctx the parse tree
	 */
	void enterType_definition(PlSqlParserParser.Type_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#type_definition}.
	 * @param ctx the parse tree
	 */
	void exitType_definition(PlSqlParserParser.Type_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#object_type_def}.
	 * @param ctx the parse tree
	 */
	void enterObject_type_def(PlSqlParserParser.Object_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#object_type_def}.
	 * @param ctx the parse tree
	 */
	void exitObject_type_def(PlSqlParserParser.Object_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#object_as_part}.
	 * @param ctx the parse tree
	 */
	void enterObject_as_part(PlSqlParserParser.Object_as_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#object_as_part}.
	 * @param ctx the parse tree
	 */
	void exitObject_as_part(PlSqlParserParser.Object_as_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#object_under_part}.
	 * @param ctx the parse tree
	 */
	void enterObject_under_part(PlSqlParserParser.Object_under_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#object_under_part}.
	 * @param ctx the parse tree
	 */
	void exitObject_under_part(PlSqlParserParser.Object_under_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#nested_table_type_def}.
	 * @param ctx the parse tree
	 */
	void enterNested_table_type_def(PlSqlParserParser.Nested_table_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#nested_table_type_def}.
	 * @param ctx the parse tree
	 */
	void exitNested_table_type_def(PlSqlParserParser.Nested_table_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#sqlj_object_type}.
	 * @param ctx the parse tree
	 */
	void enterSqlj_object_type(PlSqlParserParser.Sqlj_object_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#sqlj_object_type}.
	 * @param ctx the parse tree
	 */
	void exitSqlj_object_type(PlSqlParserParser.Sqlj_object_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#type_body}.
	 * @param ctx the parse tree
	 */
	void enterType_body(PlSqlParserParser.Type_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#type_body}.
	 * @param ctx the parse tree
	 */
	void exitType_body(PlSqlParserParser.Type_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#type_body_elements}.
	 * @param ctx the parse tree
	 */
	void enterType_body_elements(PlSqlParserParser.Type_body_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#type_body_elements}.
	 * @param ctx the parse tree
	 */
	void exitType_body_elements(PlSqlParserParser.Type_body_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#map_order_func_declaration}.
	 * @param ctx the parse tree
	 */
	void enterMap_order_func_declaration(PlSqlParserParser.Map_order_func_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#map_order_func_declaration}.
	 * @param ctx the parse tree
	 */
	void exitMap_order_func_declaration(PlSqlParserParser.Map_order_func_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subprog_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void enterSubprog_decl_in_type(PlSqlParserParser.Subprog_decl_in_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subprog_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void exitSubprog_decl_in_type(PlSqlParserParser.Subprog_decl_in_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#proc_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void enterProc_decl_in_type(PlSqlParserParser.Proc_decl_in_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#proc_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void exitProc_decl_in_type(PlSqlParserParser.Proc_decl_in_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#func_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void enterFunc_decl_in_type(PlSqlParserParser.Func_decl_in_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#func_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void exitFunc_decl_in_type(PlSqlParserParser.Func_decl_in_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#constructor_declaration}.
	 * @param ctx the parse tree
	 */
	void enterConstructor_declaration(PlSqlParserParser.Constructor_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#constructor_declaration}.
	 * @param ctx the parse tree
	 */
	void exitConstructor_declaration(PlSqlParserParser.Constructor_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#modifier_clause}.
	 * @param ctx the parse tree
	 */
	void enterModifier_clause(PlSqlParserParser.Modifier_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#modifier_clause}.
	 * @param ctx the parse tree
	 */
	void exitModifier_clause(PlSqlParserParser.Modifier_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#object_member_spec}.
	 * @param ctx the parse tree
	 */
	void enterObject_member_spec(PlSqlParserParser.Object_member_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#object_member_spec}.
	 * @param ctx the parse tree
	 */
	void exitObject_member_spec(PlSqlParserParser.Object_member_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#sqlj_object_type_attr}.
	 * @param ctx the parse tree
	 */
	void enterSqlj_object_type_attr(PlSqlParserParser.Sqlj_object_type_attrContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#sqlj_object_type_attr}.
	 * @param ctx the parse tree
	 */
	void exitSqlj_object_type_attr(PlSqlParserParser.Sqlj_object_type_attrContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#element_spec}.
	 * @param ctx the parse tree
	 */
	void enterElement_spec(PlSqlParserParser.Element_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#element_spec}.
	 * @param ctx the parse tree
	 */
	void exitElement_spec(PlSqlParserParser.Element_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#element_spec_options}.
	 * @param ctx the parse tree
	 */
	void enterElement_spec_options(PlSqlParserParser.Element_spec_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#element_spec_options}.
	 * @param ctx the parse tree
	 */
	void exitElement_spec_options(PlSqlParserParser.Element_spec_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subprogram_spec}.
	 * @param ctx the parse tree
	 */
	void enterSubprogram_spec(PlSqlParserParser.Subprogram_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subprogram_spec}.
	 * @param ctx the parse tree
	 */
	void exitSubprogram_spec(PlSqlParserParser.Subprogram_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#overriding_subprogram_spec}.
	 * @param ctx the parse tree
	 */
	void enterOverriding_subprogram_spec(PlSqlParserParser.Overriding_subprogram_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#overriding_subprogram_spec}.
	 * @param ctx the parse tree
	 */
	void exitOverriding_subprogram_spec(PlSqlParserParser.Overriding_subprogram_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#overriding_function_spec}.
	 * @param ctx the parse tree
	 */
	void enterOverriding_function_spec(PlSqlParserParser.Overriding_function_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#overriding_function_spec}.
	 * @param ctx the parse tree
	 */
	void exitOverriding_function_spec(PlSqlParserParser.Overriding_function_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#type_procedure_spec}.
	 * @param ctx the parse tree
	 */
	void enterType_procedure_spec(PlSqlParserParser.Type_procedure_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#type_procedure_spec}.
	 * @param ctx the parse tree
	 */
	void exitType_procedure_spec(PlSqlParserParser.Type_procedure_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#type_function_spec}.
	 * @param ctx the parse tree
	 */
	void enterType_function_spec(PlSqlParserParser.Type_function_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#type_function_spec}.
	 * @param ctx the parse tree
	 */
	void exitType_function_spec(PlSqlParserParser.Type_function_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#constructor_spec}.
	 * @param ctx the parse tree
	 */
	void enterConstructor_spec(PlSqlParserParser.Constructor_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#constructor_spec}.
	 * @param ctx the parse tree
	 */
	void exitConstructor_spec(PlSqlParserParser.Constructor_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#map_order_function_spec}.
	 * @param ctx the parse tree
	 */
	void enterMap_order_function_spec(PlSqlParserParser.Map_order_function_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#map_order_function_spec}.
	 * @param ctx the parse tree
	 */
	void exitMap_order_function_spec(PlSqlParserParser.Map_order_function_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#pragma_clause}.
	 * @param ctx the parse tree
	 */
	void enterPragma_clause(PlSqlParserParser.Pragma_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#pragma_clause}.
	 * @param ctx the parse tree
	 */
	void exitPragma_clause(PlSqlParserParser.Pragma_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#pragma_elements}.
	 * @param ctx the parse tree
	 */
	void enterPragma_elements(PlSqlParserParser.Pragma_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#pragma_elements}.
	 * @param ctx the parse tree
	 */
	void exitPragma_elements(PlSqlParserParser.Pragma_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#type_elements_parameter}.
	 * @param ctx the parse tree
	 */
	void enterType_elements_parameter(PlSqlParserParser.Type_elements_parameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#type_elements_parameter}.
	 * @param ctx the parse tree
	 */
	void exitType_elements_parameter(PlSqlParserParser.Type_elements_parameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_sequence}.
	 * @param ctx the parse tree
	 */
	void enterDrop_sequence(PlSqlParserParser.Drop_sequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_sequence}.
	 * @param ctx the parse tree
	 */
	void exitDrop_sequence(PlSqlParserParser.Drop_sequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_sequence}.
	 * @param ctx the parse tree
	 */
	void enterAlter_sequence(PlSqlParserParser.Alter_sequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_sequence}.
	 * @param ctx the parse tree
	 */
	void exitAlter_sequence(PlSqlParserParser.Alter_sequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_session}.
	 * @param ctx the parse tree
	 */
	void enterAlter_session(PlSqlParserParser.Alter_sessionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_session}.
	 * @param ctx the parse tree
	 */
	void exitAlter_session(PlSqlParserParser.Alter_sessionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_session_set_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_session_set_clause(PlSqlParserParser.Alter_session_set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_session_set_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_session_set_clause(PlSqlParserParser.Alter_session_set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_sequence}.
	 * @param ctx the parse tree
	 */
	void enterCreate_sequence(PlSqlParserParser.Create_sequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_sequence}.
	 * @param ctx the parse tree
	 */
	void exitCreate_sequence(PlSqlParserParser.Create_sequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#sequence_spec}.
	 * @param ctx the parse tree
	 */
	void enterSequence_spec(PlSqlParserParser.Sequence_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#sequence_spec}.
	 * @param ctx the parse tree
	 */
	void exitSequence_spec(PlSqlParserParser.Sequence_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#sequence_start_clause}.
	 * @param ctx the parse tree
	 */
	void enterSequence_start_clause(PlSqlParserParser.Sequence_start_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#sequence_start_clause}.
	 * @param ctx the parse tree
	 */
	void exitSequence_start_clause(PlSqlParserParser.Sequence_start_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_index}.
	 * @param ctx the parse tree
	 */
	void enterCreate_index(PlSqlParserParser.Create_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_index}.
	 * @param ctx the parse tree
	 */
	void exitCreate_index(PlSqlParserParser.Create_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cluster_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterCluster_index_clause(PlSqlParserParser.Cluster_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cluster_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitCluster_index_clause(PlSqlParserParser.Cluster_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cluster_name}.
	 * @param ctx the parse tree
	 */
	void enterCluster_name(PlSqlParserParser.Cluster_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cluster_name}.
	 * @param ctx the parse tree
	 */
	void exitCluster_name(PlSqlParserParser.Cluster_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#table_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterTable_index_clause(PlSqlParserParser.Table_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#table_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitTable_index_clause(PlSqlParserParser.Table_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#bitmap_join_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterBitmap_join_index_clause(PlSqlParserParser.Bitmap_join_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#bitmap_join_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitBitmap_join_index_clause(PlSqlParserParser.Bitmap_join_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#index_expr}.
	 * @param ctx the parse tree
	 */
	void enterIndex_expr(PlSqlParserParser.Index_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#index_expr}.
	 * @param ctx the parse tree
	 */
	void exitIndex_expr(PlSqlParserParser.Index_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#index_properties}.
	 * @param ctx the parse tree
	 */
	void enterIndex_properties(PlSqlParserParser.Index_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#index_properties}.
	 * @param ctx the parse tree
	 */
	void exitIndex_properties(PlSqlParserParser.Index_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#domain_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterDomain_index_clause(PlSqlParserParser.Domain_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#domain_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitDomain_index_clause(PlSqlParserParser.Domain_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#local_domain_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterLocal_domain_index_clause(PlSqlParserParser.Local_domain_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#local_domain_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitLocal_domain_index_clause(PlSqlParserParser.Local_domain_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xmlindex_clause}.
	 * @param ctx the parse tree
	 */
	void enterXmlindex_clause(PlSqlParserParser.Xmlindex_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xmlindex_clause}.
	 * @param ctx the parse tree
	 */
	void exitXmlindex_clause(PlSqlParserParser.Xmlindex_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#local_xmlindex_clause}.
	 * @param ctx the parse tree
	 */
	void enterLocal_xmlindex_clause(PlSqlParserParser.Local_xmlindex_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#local_xmlindex_clause}.
	 * @param ctx the parse tree
	 */
	void exitLocal_xmlindex_clause(PlSqlParserParser.Local_xmlindex_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#global_partitioned_index}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_partitioned_index(PlSqlParserParser.Global_partitioned_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#global_partitioned_index}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_partitioned_index(PlSqlParserParser.Global_partitioned_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#index_partitioning_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_partitioning_clause(PlSqlParserParser.Index_partitioning_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#index_partitioning_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_partitioning_clause(PlSqlParserParser.Index_partitioning_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#local_partitioned_index}.
	 * @param ctx the parse tree
	 */
	void enterLocal_partitioned_index(PlSqlParserParser.Local_partitioned_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#local_partitioned_index}.
	 * @param ctx the parse tree
	 */
	void exitLocal_partitioned_index(PlSqlParserParser.Local_partitioned_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#on_range_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterOn_range_partitioned_table(PlSqlParserParser.On_range_partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#on_range_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitOn_range_partitioned_table(PlSqlParserParser.On_range_partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#on_list_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterOn_list_partitioned_table(PlSqlParserParser.On_list_partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#on_list_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitOn_list_partitioned_table(PlSqlParserParser.On_list_partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterPartitioned_table(PlSqlParserParser.Partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitPartitioned_table(PlSqlParserParser.Partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#on_hash_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterOn_hash_partitioned_table(PlSqlParserParser.On_hash_partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#on_hash_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitOn_hash_partitioned_table(PlSqlParserParser.On_hash_partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#on_hash_partitioned_clause}.
	 * @param ctx the parse tree
	 */
	void enterOn_hash_partitioned_clause(PlSqlParserParser.On_hash_partitioned_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#on_hash_partitioned_clause}.
	 * @param ctx the parse tree
	 */
	void exitOn_hash_partitioned_clause(PlSqlParserParser.On_hash_partitioned_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#on_comp_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterOn_comp_partitioned_table(PlSqlParserParser.On_comp_partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#on_comp_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitOn_comp_partitioned_table(PlSqlParserParser.On_comp_partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#on_comp_partitioned_clause}.
	 * @param ctx the parse tree
	 */
	void enterOn_comp_partitioned_clause(PlSqlParserParser.On_comp_partitioned_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#on_comp_partitioned_clause}.
	 * @param ctx the parse tree
	 */
	void exitOn_comp_partitioned_clause(PlSqlParserParser.On_comp_partitioned_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#index_subpartition_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_subpartition_clause(PlSqlParserParser.Index_subpartition_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#index_subpartition_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_subpartition_clause(PlSqlParserParser.Index_subpartition_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#index_subpartition_subclause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_subpartition_subclause(PlSqlParserParser.Index_subpartition_subclauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#index_subpartition_subclause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_subpartition_subclause(PlSqlParserParser.Index_subpartition_subclauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#odci_parameters}.
	 * @param ctx the parse tree
	 */
	void enterOdci_parameters(PlSqlParserParser.Odci_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#odci_parameters}.
	 * @param ctx the parse tree
	 */
	void exitOdci_parameters(PlSqlParserParser.Odci_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#indextype}.
	 * @param ctx the parse tree
	 */
	void enterIndextype(PlSqlParserParser.IndextypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#indextype}.
	 * @param ctx the parse tree
	 */
	void exitIndextype(PlSqlParserParser.IndextypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_index}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index(PlSqlParserParser.Alter_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_index}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index(PlSqlParserParser.Alter_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_index_ops_set1}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_ops_set1(PlSqlParserParser.Alter_index_ops_set1Context ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_index_ops_set1}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_ops_set1(PlSqlParserParser.Alter_index_ops_set1Context ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_index_ops_set2}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_ops_set2(PlSqlParserParser.Alter_index_ops_set2Context ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_index_ops_set2}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_ops_set2(PlSqlParserParser.Alter_index_ops_set2Context ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#visible_or_invisible}.
	 * @param ctx the parse tree
	 */
	void enterVisible_or_invisible(PlSqlParserParser.Visible_or_invisibleContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#visible_or_invisible}.
	 * @param ctx the parse tree
	 */
	void exitVisible_or_invisible(PlSqlParserParser.Visible_or_invisibleContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#monitoring_nomonitoring}.
	 * @param ctx the parse tree
	 */
	void enterMonitoring_nomonitoring(PlSqlParserParser.Monitoring_nomonitoringContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#monitoring_nomonitoring}.
	 * @param ctx the parse tree
	 */
	void exitMonitoring_nomonitoring(PlSqlParserParser.Monitoring_nomonitoringContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#rebuild_clause}.
	 * @param ctx the parse tree
	 */
	void enterRebuild_clause(PlSqlParserParser.Rebuild_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#rebuild_clause}.
	 * @param ctx the parse tree
	 */
	void exitRebuild_clause(PlSqlParserParser.Rebuild_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_index_partitioning}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_partitioning(PlSqlParserParser.Alter_index_partitioningContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_index_partitioning}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_partitioning(PlSqlParserParser.Alter_index_partitioningContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#modify_index_default_attrs}.
	 * @param ctx the parse tree
	 */
	void enterModify_index_default_attrs(PlSqlParserParser.Modify_index_default_attrsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#modify_index_default_attrs}.
	 * @param ctx the parse tree
	 */
	void exitModify_index_default_attrs(PlSqlParserParser.Modify_index_default_attrsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#add_hash_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterAdd_hash_index_partition(PlSqlParserParser.Add_hash_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#add_hash_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitAdd_hash_index_partition(PlSqlParserParser.Add_hash_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#coalesce_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterCoalesce_index_partition(PlSqlParserParser.Coalesce_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#coalesce_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitCoalesce_index_partition(PlSqlParserParser.Coalesce_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#modify_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterModify_index_partition(PlSqlParserParser.Modify_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#modify_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitModify_index_partition(PlSqlParserParser.Modify_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#modify_index_partitions_ops}.
	 * @param ctx the parse tree
	 */
	void enterModify_index_partitions_ops(PlSqlParserParser.Modify_index_partitions_opsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#modify_index_partitions_ops}.
	 * @param ctx the parse tree
	 */
	void exitModify_index_partitions_ops(PlSqlParserParser.Modify_index_partitions_opsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#rename_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterRename_index_partition(PlSqlParserParser.Rename_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#rename_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitRename_index_partition(PlSqlParserParser.Rename_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterDrop_index_partition(PlSqlParserParser.Drop_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitDrop_index_partition(PlSqlParserParser.Drop_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#split_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterSplit_index_partition(PlSqlParserParser.Split_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#split_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitSplit_index_partition(PlSqlParserParser.Split_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#index_partition_description}.
	 * @param ctx the parse tree
	 */
	void enterIndex_partition_description(PlSqlParserParser.Index_partition_descriptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#index_partition_description}.
	 * @param ctx the parse tree
	 */
	void exitIndex_partition_description(PlSqlParserParser.Index_partition_descriptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#modify_index_subpartition}.
	 * @param ctx the parse tree
	 */
	void enterModify_index_subpartition(PlSqlParserParser.Modify_index_subpartitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#modify_index_subpartition}.
	 * @param ctx the parse tree
	 */
	void exitModify_index_subpartition(PlSqlParserParser.Modify_index_subpartitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#partition_name_old}.
	 * @param ctx the parse tree
	 */
	void enterPartition_name_old(PlSqlParserParser.Partition_name_oldContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#partition_name_old}.
	 * @param ctx the parse tree
	 */
	void exitPartition_name_old(PlSqlParserParser.Partition_name_oldContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#new_partition_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_partition_name(PlSqlParserParser.New_partition_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#new_partition_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_partition_name(PlSqlParserParser.New_partition_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#new_index_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_index_name(PlSqlParserParser.New_index_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#new_index_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_index_name(PlSqlParserParser.New_index_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_user}.
	 * @param ctx the parse tree
	 */
	void enterCreate_user(PlSqlParserParser.Create_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_user}.
	 * @param ctx the parse tree
	 */
	void exitCreate_user(PlSqlParserParser.Create_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_user}.
	 * @param ctx the parse tree
	 */
	void enterAlter_user(PlSqlParserParser.Alter_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_user}.
	 * @param ctx the parse tree
	 */
	void exitAlter_user(PlSqlParserParser.Alter_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_identified_by}.
	 * @param ctx the parse tree
	 */
	void enterAlter_identified_by(PlSqlParserParser.Alter_identified_byContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_identified_by}.
	 * @param ctx the parse tree
	 */
	void exitAlter_identified_by(PlSqlParserParser.Alter_identified_byContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#identified_by}.
	 * @param ctx the parse tree
	 */
	void enterIdentified_by(PlSqlParserParser.Identified_byContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#identified_by}.
	 * @param ctx the parse tree
	 */
	void exitIdentified_by(PlSqlParserParser.Identified_byContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#identified_other_clause}.
	 * @param ctx the parse tree
	 */
	void enterIdentified_other_clause(PlSqlParserParser.Identified_other_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#identified_other_clause}.
	 * @param ctx the parse tree
	 */
	void exitIdentified_other_clause(PlSqlParserParser.Identified_other_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#user_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void enterUser_tablespace_clause(PlSqlParserParser.User_tablespace_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#user_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void exitUser_tablespace_clause(PlSqlParserParser.User_tablespace_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#quota_clause}.
	 * @param ctx the parse tree
	 */
	void enterQuota_clause(PlSqlParserParser.Quota_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#quota_clause}.
	 * @param ctx the parse tree
	 */
	void exitQuota_clause(PlSqlParserParser.Quota_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#profile_clause}.
	 * @param ctx the parse tree
	 */
	void enterProfile_clause(PlSqlParserParser.Profile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#profile_clause}.
	 * @param ctx the parse tree
	 */
	void exitProfile_clause(PlSqlParserParser.Profile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#role_clause}.
	 * @param ctx the parse tree
	 */
	void enterRole_clause(PlSqlParserParser.Role_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#role_clause}.
	 * @param ctx the parse tree
	 */
	void exitRole_clause(PlSqlParserParser.Role_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#user_default_role_clause}.
	 * @param ctx the parse tree
	 */
	void enterUser_default_role_clause(PlSqlParserParser.User_default_role_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#user_default_role_clause}.
	 * @param ctx the parse tree
	 */
	void exitUser_default_role_clause(PlSqlParserParser.User_default_role_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#password_expire_clause}.
	 * @param ctx the parse tree
	 */
	void enterPassword_expire_clause(PlSqlParserParser.Password_expire_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#password_expire_clause}.
	 * @param ctx the parse tree
	 */
	void exitPassword_expire_clause(PlSqlParserParser.Password_expire_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#user_lock_clause}.
	 * @param ctx the parse tree
	 */
	void enterUser_lock_clause(PlSqlParserParser.User_lock_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#user_lock_clause}.
	 * @param ctx the parse tree
	 */
	void exitUser_lock_clause(PlSqlParserParser.User_lock_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#user_editions_clause}.
	 * @param ctx the parse tree
	 */
	void enterUser_editions_clause(PlSqlParserParser.User_editions_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#user_editions_clause}.
	 * @param ctx the parse tree
	 */
	void exitUser_editions_clause(PlSqlParserParser.User_editions_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_user_editions_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_user_editions_clause(PlSqlParserParser.Alter_user_editions_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_user_editions_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_user_editions_clause(PlSqlParserParser.Alter_user_editions_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#proxy_clause}.
	 * @param ctx the parse tree
	 */
	void enterProxy_clause(PlSqlParserParser.Proxy_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#proxy_clause}.
	 * @param ctx the parse tree
	 */
	void exitProxy_clause(PlSqlParserParser.Proxy_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#container_names}.
	 * @param ctx the parse tree
	 */
	void enterContainer_names(PlSqlParserParser.Container_namesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#container_names}.
	 * @param ctx the parse tree
	 */
	void exitContainer_names(PlSqlParserParser.Container_namesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#set_container_data}.
	 * @param ctx the parse tree
	 */
	void enterSet_container_data(PlSqlParserParser.Set_container_dataContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#set_container_data}.
	 * @param ctx the parse tree
	 */
	void exitSet_container_data(PlSqlParserParser.Set_container_dataContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#add_rem_container_data}.
	 * @param ctx the parse tree
	 */
	void enterAdd_rem_container_data(PlSqlParserParser.Add_rem_container_dataContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#add_rem_container_data}.
	 * @param ctx the parse tree
	 */
	void exitAdd_rem_container_data(PlSqlParserParser.Add_rem_container_dataContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#container_data_clause}.
	 * @param ctx the parse tree
	 */
	void enterContainer_data_clause(PlSqlParserParser.Container_data_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#container_data_clause}.
	 * @param ctx the parse tree
	 */
	void exitContainer_data_clause(PlSqlParserParser.Container_data_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#analyze}.
	 * @param ctx the parse tree
	 */
	void enterAnalyze(PlSqlParserParser.AnalyzeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#analyze}.
	 * @param ctx the parse tree
	 */
	void exitAnalyze(PlSqlParserParser.AnalyzeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#partition_extention_clause}.
	 * @param ctx the parse tree
	 */
	void enterPartition_extention_clause(PlSqlParserParser.Partition_extention_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#partition_extention_clause}.
	 * @param ctx the parse tree
	 */
	void exitPartition_extention_clause(PlSqlParserParser.Partition_extention_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#validation_clauses}.
	 * @param ctx the parse tree
	 */
	void enterValidation_clauses(PlSqlParserParser.Validation_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#validation_clauses}.
	 * @param ctx the parse tree
	 */
	void exitValidation_clauses(PlSqlParserParser.Validation_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#online_or_offline}.
	 * @param ctx the parse tree
	 */
	void enterOnline_or_offline(PlSqlParserParser.Online_or_offlineContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#online_or_offline}.
	 * @param ctx the parse tree
	 */
	void exitOnline_or_offline(PlSqlParserParser.Online_or_offlineContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#into_clause1}.
	 * @param ctx the parse tree
	 */
	void enterInto_clause1(PlSqlParserParser.Into_clause1Context ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#into_clause1}.
	 * @param ctx the parse tree
	 */
	void exitInto_clause1(PlSqlParserParser.Into_clause1Context ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#partition_key_value}.
	 * @param ctx the parse tree
	 */
	void enterPartition_key_value(PlSqlParserParser.Partition_key_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#partition_key_value}.
	 * @param ctx the parse tree
	 */
	void exitPartition_key_value(PlSqlParserParser.Partition_key_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subpartition_key_value}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_key_value(PlSqlParserParser.Subpartition_key_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subpartition_key_value}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_key_value(PlSqlParserParser.Subpartition_key_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#associate_statistics}.
	 * @param ctx the parse tree
	 */
	void enterAssociate_statistics(PlSqlParserParser.Associate_statisticsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#associate_statistics}.
	 * @param ctx the parse tree
	 */
	void exitAssociate_statistics(PlSqlParserParser.Associate_statisticsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#column_association}.
	 * @param ctx the parse tree
	 */
	void enterColumn_association(PlSqlParserParser.Column_associationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#column_association}.
	 * @param ctx the parse tree
	 */
	void exitColumn_association(PlSqlParserParser.Column_associationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#function_association}.
	 * @param ctx the parse tree
	 */
	void enterFunction_association(PlSqlParserParser.Function_associationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#function_association}.
	 * @param ctx the parse tree
	 */
	void exitFunction_association(PlSqlParserParser.Function_associationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#indextype_name}.
	 * @param ctx the parse tree
	 */
	void enterIndextype_name(PlSqlParserParser.Indextype_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#indextype_name}.
	 * @param ctx the parse tree
	 */
	void exitIndextype_name(PlSqlParserParser.Indextype_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#using_statistics_type}.
	 * @param ctx the parse tree
	 */
	void enterUsing_statistics_type(PlSqlParserParser.Using_statistics_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#using_statistics_type}.
	 * @param ctx the parse tree
	 */
	void exitUsing_statistics_type(PlSqlParserParser.Using_statistics_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#statistics_type_name}.
	 * @param ctx the parse tree
	 */
	void enterStatistics_type_name(PlSqlParserParser.Statistics_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#statistics_type_name}.
	 * @param ctx the parse tree
	 */
	void exitStatistics_type_name(PlSqlParserParser.Statistics_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#default_cost_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_cost_clause(PlSqlParserParser.Default_cost_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#default_cost_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_cost_clause(PlSqlParserParser.Default_cost_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cpu_cost}.
	 * @param ctx the parse tree
	 */
	void enterCpu_cost(PlSqlParserParser.Cpu_costContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cpu_cost}.
	 * @param ctx the parse tree
	 */
	void exitCpu_cost(PlSqlParserParser.Cpu_costContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#io_cost}.
	 * @param ctx the parse tree
	 */
	void enterIo_cost(PlSqlParserParser.Io_costContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#io_cost}.
	 * @param ctx the parse tree
	 */
	void exitIo_cost(PlSqlParserParser.Io_costContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#network_cost}.
	 * @param ctx the parse tree
	 */
	void enterNetwork_cost(PlSqlParserParser.Network_costContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#network_cost}.
	 * @param ctx the parse tree
	 */
	void exitNetwork_cost(PlSqlParserParser.Network_costContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#default_selectivity_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_selectivity_clause(PlSqlParserParser.Default_selectivity_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#default_selectivity_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_selectivity_clause(PlSqlParserParser.Default_selectivity_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#default_selectivity}.
	 * @param ctx the parse tree
	 */
	void enterDefault_selectivity(PlSqlParserParser.Default_selectivityContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#default_selectivity}.
	 * @param ctx the parse tree
	 */
	void exitDefault_selectivity(PlSqlParserParser.Default_selectivityContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#storage_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterStorage_table_clause(PlSqlParserParser.Storage_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#storage_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitStorage_table_clause(PlSqlParserParser.Storage_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#unified_auditing}.
	 * @param ctx the parse tree
	 */
	void enterUnified_auditing(PlSqlParserParser.Unified_auditingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#unified_auditing}.
	 * @param ctx the parse tree
	 */
	void exitUnified_auditing(PlSqlParserParser.Unified_auditingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#policy_name}.
	 * @param ctx the parse tree
	 */
	void enterPolicy_name(PlSqlParserParser.Policy_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#policy_name}.
	 * @param ctx the parse tree
	 */
	void exitPolicy_name(PlSqlParserParser.Policy_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#audit_traditional}.
	 * @param ctx the parse tree
	 */
	void enterAudit_traditional(PlSqlParserParser.Audit_traditionalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#audit_traditional}.
	 * @param ctx the parse tree
	 */
	void exitAudit_traditional(PlSqlParserParser.Audit_traditionalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#audit_direct_path}.
	 * @param ctx the parse tree
	 */
	void enterAudit_direct_path(PlSqlParserParser.Audit_direct_pathContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#audit_direct_path}.
	 * @param ctx the parse tree
	 */
	void exitAudit_direct_path(PlSqlParserParser.Audit_direct_pathContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#audit_container_clause}.
	 * @param ctx the parse tree
	 */
	void enterAudit_container_clause(PlSqlParserParser.Audit_container_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#audit_container_clause}.
	 * @param ctx the parse tree
	 */
	void exitAudit_container_clause(PlSqlParserParser.Audit_container_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#audit_operation_clause}.
	 * @param ctx the parse tree
	 */
	void enterAudit_operation_clause(PlSqlParserParser.Audit_operation_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#audit_operation_clause}.
	 * @param ctx the parse tree
	 */
	void exitAudit_operation_clause(PlSqlParserParser.Audit_operation_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#auditing_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterAuditing_by_clause(PlSqlParserParser.Auditing_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#auditing_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitAuditing_by_clause(PlSqlParserParser.Auditing_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#audit_user}.
	 * @param ctx the parse tree
	 */
	void enterAudit_user(PlSqlParserParser.Audit_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#audit_user}.
	 * @param ctx the parse tree
	 */
	void exitAudit_user(PlSqlParserParser.Audit_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#audit_schema_object_clause}.
	 * @param ctx the parse tree
	 */
	void enterAudit_schema_object_clause(PlSqlParserParser.Audit_schema_object_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#audit_schema_object_clause}.
	 * @param ctx the parse tree
	 */
	void exitAudit_schema_object_clause(PlSqlParserParser.Audit_schema_object_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#sql_operation}.
	 * @param ctx the parse tree
	 */
	void enterSql_operation(PlSqlParserParser.Sql_operationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#sql_operation}.
	 * @param ctx the parse tree
	 */
	void exitSql_operation(PlSqlParserParser.Sql_operationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#auditing_on_clause}.
	 * @param ctx the parse tree
	 */
	void enterAuditing_on_clause(PlSqlParserParser.Auditing_on_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#auditing_on_clause}.
	 * @param ctx the parse tree
	 */
	void exitAuditing_on_clause(PlSqlParserParser.Auditing_on_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#model_name}.
	 * @param ctx the parse tree
	 */
	void enterModel_name(PlSqlParserParser.Model_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#model_name}.
	 * @param ctx the parse tree
	 */
	void exitModel_name(PlSqlParserParser.Model_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#object_name}.
	 * @param ctx the parse tree
	 */
	void enterObject_name(PlSqlParserParser.Object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#object_name}.
	 * @param ctx the parse tree
	 */
	void exitObject_name(PlSqlParserParser.Object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#profile_name}.
	 * @param ctx the parse tree
	 */
	void enterProfile_name(PlSqlParserParser.Profile_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#profile_name}.
	 * @param ctx the parse tree
	 */
	void exitProfile_name(PlSqlParserParser.Profile_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#sql_statement_shortcut}.
	 * @param ctx the parse tree
	 */
	void enterSql_statement_shortcut(PlSqlParserParser.Sql_statement_shortcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#sql_statement_shortcut}.
	 * @param ctx the parse tree
	 */
	void exitSql_statement_shortcut(PlSqlParserParser.Sql_statement_shortcutContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_index}.
	 * @param ctx the parse tree
	 */
	void enterDrop_index(PlSqlParserParser.Drop_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_index}.
	 * @param ctx the parse tree
	 */
	void exitDrop_index(PlSqlParserParser.Drop_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#rename_object}.
	 * @param ctx the parse tree
	 */
	void enterRename_object(PlSqlParserParser.Rename_objectContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#rename_object}.
	 * @param ctx the parse tree
	 */
	void exitRename_object(PlSqlParserParser.Rename_objectContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#grant_statement}.
	 * @param ctx the parse tree
	 */
	void enterGrant_statement(PlSqlParserParser.Grant_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#grant_statement}.
	 * @param ctx the parse tree
	 */
	void exitGrant_statement(PlSqlParserParser.Grant_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#container_clause}.
	 * @param ctx the parse tree
	 */
	void enterContainer_clause(PlSqlParserParser.Container_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#container_clause}.
	 * @param ctx the parse tree
	 */
	void exitContainer_clause(PlSqlParserParser.Container_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_directory}.
	 * @param ctx the parse tree
	 */
	void enterCreate_directory(PlSqlParserParser.Create_directoryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_directory}.
	 * @param ctx the parse tree
	 */
	void exitCreate_directory(PlSqlParserParser.Create_directoryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#directory_name}.
	 * @param ctx the parse tree
	 */
	void enterDirectory_name(PlSqlParserParser.Directory_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#directory_name}.
	 * @param ctx the parse tree
	 */
	void exitDirectory_name(PlSqlParserParser.Directory_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#directory_path}.
	 * @param ctx the parse tree
	 */
	void enterDirectory_path(PlSqlParserParser.Directory_pathContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#directory_path}.
	 * @param ctx the parse tree
	 */
	void exitDirectory_path(PlSqlParserParser.Directory_pathContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_library}.
	 * @param ctx the parse tree
	 */
	void enterAlter_library(PlSqlParserParser.Alter_libraryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_library}.
	 * @param ctx the parse tree
	 */
	void exitAlter_library(PlSqlParserParser.Alter_libraryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#library_editionable}.
	 * @param ctx the parse tree
	 */
	void enterLibrary_editionable(PlSqlParserParser.Library_editionableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#library_editionable}.
	 * @param ctx the parse tree
	 */
	void exitLibrary_editionable(PlSqlParserParser.Library_editionableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#library_debug}.
	 * @param ctx the parse tree
	 */
	void enterLibrary_debug(PlSqlParserParser.Library_debugContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#library_debug}.
	 * @param ctx the parse tree
	 */
	void exitLibrary_debug(PlSqlParserParser.Library_debugContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#compiler_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void enterCompiler_parameters_clause(PlSqlParserParser.Compiler_parameters_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#compiler_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void exitCompiler_parameters_clause(PlSqlParserParser.Compiler_parameters_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#parameter_value}.
	 * @param ctx the parse tree
	 */
	void enterParameter_value(PlSqlParserParser.Parameter_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#parameter_value}.
	 * @param ctx the parse tree
	 */
	void exitParameter_value(PlSqlParserParser.Parameter_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#library_name}.
	 * @param ctx the parse tree
	 */
	void enterLibrary_name(PlSqlParserParser.Library_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#library_name}.
	 * @param ctx the parse tree
	 */
	void exitLibrary_name(PlSqlParserParser.Library_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_view}.
	 * @param ctx the parse tree
	 */
	void enterAlter_view(PlSqlParserParser.Alter_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_view}.
	 * @param ctx the parse tree
	 */
	void exitAlter_view(PlSqlParserParser.Alter_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_view_editionable}.
	 * @param ctx the parse tree
	 */
	void enterAlter_view_editionable(PlSqlParserParser.Alter_view_editionableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_view_editionable}.
	 * @param ctx the parse tree
	 */
	void exitAlter_view_editionable(PlSqlParserParser.Alter_view_editionableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_view}.
	 * @param ctx the parse tree
	 */
	void enterCreate_view(PlSqlParserParser.Create_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_view}.
	 * @param ctx the parse tree
	 */
	void exitCreate_view(PlSqlParserParser.Create_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#view_options}.
	 * @param ctx the parse tree
	 */
	void enterView_options(PlSqlParserParser.View_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#view_options}.
	 * @param ctx the parse tree
	 */
	void exitView_options(PlSqlParserParser.View_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#view_alias_constraint}.
	 * @param ctx the parse tree
	 */
	void enterView_alias_constraint(PlSqlParserParser.View_alias_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#view_alias_constraint}.
	 * @param ctx the parse tree
	 */
	void exitView_alias_constraint(PlSqlParserParser.View_alias_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#object_view_clause}.
	 * @param ctx the parse tree
	 */
	void enterObject_view_clause(PlSqlParserParser.Object_view_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#object_view_clause}.
	 * @param ctx the parse tree
	 */
	void exitObject_view_clause(PlSqlParserParser.Object_view_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#inline_constraint}.
	 * @param ctx the parse tree
	 */
	void enterInline_constraint(PlSqlParserParser.Inline_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#inline_constraint}.
	 * @param ctx the parse tree
	 */
	void exitInline_constraint(PlSqlParserParser.Inline_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#inline_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void enterInline_ref_constraint(PlSqlParserParser.Inline_ref_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#inline_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void exitInline_ref_constraint(PlSqlParserParser.Inline_ref_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#out_of_line_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_ref_constraint(PlSqlParserParser.Out_of_line_ref_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#out_of_line_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_ref_constraint(PlSqlParserParser.Out_of_line_ref_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#out_of_line_constraint}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_constraint(PlSqlParserParser.Out_of_line_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#out_of_line_constraint}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_constraint(PlSqlParserParser.Out_of_line_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#constraint_state}.
	 * @param ctx the parse tree
	 */
	void enterConstraint_state(PlSqlParserParser.Constraint_stateContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#constraint_state}.
	 * @param ctx the parse tree
	 */
	void exitConstraint_state(PlSqlParserParser.Constraint_stateContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_tablespace}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablespace(PlSqlParserParser.Alter_tablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_tablespace}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablespace(PlSqlParserParser.Alter_tablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#datafile_tempfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDatafile_tempfile_clauses(PlSqlParserParser.Datafile_tempfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#datafile_tempfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDatafile_tempfile_clauses(PlSqlParserParser.Datafile_tempfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#tablespace_logging_clauses}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_logging_clauses(PlSqlParserParser.Tablespace_logging_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#tablespace_logging_clauses}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_logging_clauses(PlSqlParserParser.Tablespace_logging_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#tablespace_group_clause}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_group_clause(PlSqlParserParser.Tablespace_group_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#tablespace_group_clause}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_group_clause(PlSqlParserParser.Tablespace_group_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#tablespace_group_name}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_group_name(PlSqlParserParser.Tablespace_group_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#tablespace_group_name}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_group_name(PlSqlParserParser.Tablespace_group_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#tablespace_state_clauses}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_state_clauses(PlSqlParserParser.Tablespace_state_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#tablespace_state_clauses}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_state_clauses(PlSqlParserParser.Tablespace_state_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#flashback_mode_clause}.
	 * @param ctx the parse tree
	 */
	void enterFlashback_mode_clause(PlSqlParserParser.Flashback_mode_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#flashback_mode_clause}.
	 * @param ctx the parse tree
	 */
	void exitFlashback_mode_clause(PlSqlParserParser.Flashback_mode_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#new_tablespace_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_tablespace_name(PlSqlParserParser.New_tablespace_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#new_tablespace_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_tablespace_name(PlSqlParserParser.New_tablespace_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_tablespace}.
	 * @param ctx the parse tree
	 */
	void enterCreate_tablespace(PlSqlParserParser.Create_tablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_tablespace}.
	 * @param ctx the parse tree
	 */
	void exitCreate_tablespace(PlSqlParserParser.Create_tablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#permanent_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void enterPermanent_tablespace_clause(PlSqlParserParser.Permanent_tablespace_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#permanent_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void exitPermanent_tablespace_clause(PlSqlParserParser.Permanent_tablespace_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#tablespace_encryption_spec}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_encryption_spec(PlSqlParserParser.Tablespace_encryption_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#tablespace_encryption_spec}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_encryption_spec(PlSqlParserParser.Tablespace_encryption_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#logging_clause}.
	 * @param ctx the parse tree
	 */
	void enterLogging_clause(PlSqlParserParser.Logging_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#logging_clause}.
	 * @param ctx the parse tree
	 */
	void exitLogging_clause(PlSqlParserParser.Logging_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#extent_management_clause}.
	 * @param ctx the parse tree
	 */
	void enterExtent_management_clause(PlSqlParserParser.Extent_management_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#extent_management_clause}.
	 * @param ctx the parse tree
	 */
	void exitExtent_management_clause(PlSqlParserParser.Extent_management_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#segment_management_clause}.
	 * @param ctx the parse tree
	 */
	void enterSegment_management_clause(PlSqlParserParser.Segment_management_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#segment_management_clause}.
	 * @param ctx the parse tree
	 */
	void exitSegment_management_clause(PlSqlParserParser.Segment_management_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#temporary_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void enterTemporary_tablespace_clause(PlSqlParserParser.Temporary_tablespace_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#temporary_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void exitTemporary_tablespace_clause(PlSqlParserParser.Temporary_tablespace_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#undo_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void enterUndo_tablespace_clause(PlSqlParserParser.Undo_tablespace_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#undo_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void exitUndo_tablespace_clause(PlSqlParserParser.Undo_tablespace_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#tablespace_retention_clause}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_retention_clause(PlSqlParserParser.Tablespace_retention_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#tablespace_retention_clause}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_retention_clause(PlSqlParserParser.Tablespace_retention_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#datafile_specification}.
	 * @param ctx the parse tree
	 */
	void enterDatafile_specification(PlSqlParserParser.Datafile_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#datafile_specification}.
	 * @param ctx the parse tree
	 */
	void exitDatafile_specification(PlSqlParserParser.Datafile_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#tempfile_specification}.
	 * @param ctx the parse tree
	 */
	void enterTempfile_specification(PlSqlParserParser.Tempfile_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#tempfile_specification}.
	 * @param ctx the parse tree
	 */
	void exitTempfile_specification(PlSqlParserParser.Tempfile_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#datafile_tempfile_spec}.
	 * @param ctx the parse tree
	 */
	void enterDatafile_tempfile_spec(PlSqlParserParser.Datafile_tempfile_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#datafile_tempfile_spec}.
	 * @param ctx the parse tree
	 */
	void exitDatafile_tempfile_spec(PlSqlParserParser.Datafile_tempfile_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#redo_log_file_spec}.
	 * @param ctx the parse tree
	 */
	void enterRedo_log_file_spec(PlSqlParserParser.Redo_log_file_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#redo_log_file_spec}.
	 * @param ctx the parse tree
	 */
	void exitRedo_log_file_spec(PlSqlParserParser.Redo_log_file_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#autoextend_clause}.
	 * @param ctx the parse tree
	 */
	void enterAutoextend_clause(PlSqlParserParser.Autoextend_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#autoextend_clause}.
	 * @param ctx the parse tree
	 */
	void exitAutoextend_clause(PlSqlParserParser.Autoextend_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#maxsize_clause}.
	 * @param ctx the parse tree
	 */
	void enterMaxsize_clause(PlSqlParserParser.Maxsize_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#maxsize_clause}.
	 * @param ctx the parse tree
	 */
	void exitMaxsize_clause(PlSqlParserParser.Maxsize_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#build_clause}.
	 * @param ctx the parse tree
	 */
	void enterBuild_clause(PlSqlParserParser.Build_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#build_clause}.
	 * @param ctx the parse tree
	 */
	void exitBuild_clause(PlSqlParserParser.Build_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#parallel_clause}.
	 * @param ctx the parse tree
	 */
	void enterParallel_clause(PlSqlParserParser.Parallel_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#parallel_clause}.
	 * @param ctx the parse tree
	 */
	void exitParallel_clause(PlSqlParserParser.Parallel_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_materialized_view}.
	 * @param ctx the parse tree
	 */
	void enterAlter_materialized_view(PlSqlParserParser.Alter_materialized_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_materialized_view}.
	 * @param ctx the parse tree
	 */
	void exitAlter_materialized_view(PlSqlParserParser.Alter_materialized_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_mv_option1}.
	 * @param ctx the parse tree
	 */
	void enterAlter_mv_option1(PlSqlParserParser.Alter_mv_option1Context ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_mv_option1}.
	 * @param ctx the parse tree
	 */
	void exitAlter_mv_option1(PlSqlParserParser.Alter_mv_option1Context ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_mv_refresh}.
	 * @param ctx the parse tree
	 */
	void enterAlter_mv_refresh(PlSqlParserParser.Alter_mv_refreshContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_mv_refresh}.
	 * @param ctx the parse tree
	 */
	void exitAlter_mv_refresh(PlSqlParserParser.Alter_mv_refreshContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#rollback_segment}.
	 * @param ctx the parse tree
	 */
	void enterRollback_segment(PlSqlParserParser.Rollback_segmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#rollback_segment}.
	 * @param ctx the parse tree
	 */
	void exitRollback_segment(PlSqlParserParser.Rollback_segmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#modify_mv_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterModify_mv_column_clause(PlSqlParserParser.Modify_mv_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#modify_mv_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitModify_mv_column_clause(PlSqlParserParser.Modify_mv_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_materialized_view_log}.
	 * @param ctx the parse tree
	 */
	void enterAlter_materialized_view_log(PlSqlParserParser.Alter_materialized_view_logContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_materialized_view_log}.
	 * @param ctx the parse tree
	 */
	void exitAlter_materialized_view_log(PlSqlParserParser.Alter_materialized_view_logContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#add_mv_log_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_mv_log_column_clause(PlSqlParserParser.Add_mv_log_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#add_mv_log_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_mv_log_column_clause(PlSqlParserParser.Add_mv_log_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#move_mv_log_clause}.
	 * @param ctx the parse tree
	 */
	void enterMove_mv_log_clause(PlSqlParserParser.Move_mv_log_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#move_mv_log_clause}.
	 * @param ctx the parse tree
	 */
	void exitMove_mv_log_clause(PlSqlParserParser.Move_mv_log_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#mv_log_augmentation}.
	 * @param ctx the parse tree
	 */
	void enterMv_log_augmentation(PlSqlParserParser.Mv_log_augmentationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#mv_log_augmentation}.
	 * @param ctx the parse tree
	 */
	void exitMv_log_augmentation(PlSqlParserParser.Mv_log_augmentationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#datetime_expr}.
	 * @param ctx the parse tree
	 */
	void enterDatetime_expr(PlSqlParserParser.Datetime_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#datetime_expr}.
	 * @param ctx the parse tree
	 */
	void exitDatetime_expr(PlSqlParserParser.Datetime_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#interval_expr}.
	 * @param ctx the parse tree
	 */
	void enterInterval_expr(PlSqlParserParser.Interval_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#interval_expr}.
	 * @param ctx the parse tree
	 */
	void exitInterval_expr(PlSqlParserParser.Interval_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#synchronous_or_asynchronous}.
	 * @param ctx the parse tree
	 */
	void enterSynchronous_or_asynchronous(PlSqlParserParser.Synchronous_or_asynchronousContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#synchronous_or_asynchronous}.
	 * @param ctx the parse tree
	 */
	void exitSynchronous_or_asynchronous(PlSqlParserParser.Synchronous_or_asynchronousContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#including_or_excluding}.
	 * @param ctx the parse tree
	 */
	void enterIncluding_or_excluding(PlSqlParserParser.Including_or_excludingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#including_or_excluding}.
	 * @param ctx the parse tree
	 */
	void exitIncluding_or_excluding(PlSqlParserParser.Including_or_excludingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_materialized_view_log}.
	 * @param ctx the parse tree
	 */
	void enterCreate_materialized_view_log(PlSqlParserParser.Create_materialized_view_logContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_materialized_view_log}.
	 * @param ctx the parse tree
	 */
	void exitCreate_materialized_view_log(PlSqlParserParser.Create_materialized_view_logContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#new_values_clause}.
	 * @param ctx the parse tree
	 */
	void enterNew_values_clause(PlSqlParserParser.New_values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#new_values_clause}.
	 * @param ctx the parse tree
	 */
	void exitNew_values_clause(PlSqlParserParser.New_values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#mv_log_purge_clause}.
	 * @param ctx the parse tree
	 */
	void enterMv_log_purge_clause(PlSqlParserParser.Mv_log_purge_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#mv_log_purge_clause}.
	 * @param ctx the parse tree
	 */
	void exitMv_log_purge_clause(PlSqlParserParser.Mv_log_purge_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_materialized_view}.
	 * @param ctx the parse tree
	 */
	void enterCreate_materialized_view(PlSqlParserParser.Create_materialized_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_materialized_view}.
	 * @param ctx the parse tree
	 */
	void exitCreate_materialized_view(PlSqlParserParser.Create_materialized_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_mv_refresh}.
	 * @param ctx the parse tree
	 */
	void enterCreate_mv_refresh(PlSqlParserParser.Create_mv_refreshContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_mv_refresh}.
	 * @param ctx the parse tree
	 */
	void exitCreate_mv_refresh(PlSqlParserParser.Create_mv_refreshContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_context}.
	 * @param ctx the parse tree
	 */
	void enterCreate_context(PlSqlParserParser.Create_contextContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_context}.
	 * @param ctx the parse tree
	 */
	void exitCreate_context(PlSqlParserParser.Create_contextContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#oracle_namespace}.
	 * @param ctx the parse tree
	 */
	void enterOracle_namespace(PlSqlParserParser.Oracle_namespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#oracle_namespace}.
	 * @param ctx the parse tree
	 */
	void exitOracle_namespace(PlSqlParserParser.Oracle_namespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_cluster}.
	 * @param ctx the parse tree
	 */
	void enterCreate_cluster(PlSqlParserParser.Create_clusterContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_cluster}.
	 * @param ctx the parse tree
	 */
	void exitCreate_cluster(PlSqlParserParser.Create_clusterContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_table}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table(PlSqlParserParser.Create_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_table}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table(PlSqlParserParser.Create_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xmltype_table}.
	 * @param ctx the parse tree
	 */
	void enterXmltype_table(PlSqlParserParser.Xmltype_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xmltype_table}.
	 * @param ctx the parse tree
	 */
	void exitXmltype_table(PlSqlParserParser.Xmltype_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xmltype_virtual_columns}.
	 * @param ctx the parse tree
	 */
	void enterXmltype_virtual_columns(PlSqlParserParser.Xmltype_virtual_columnsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xmltype_virtual_columns}.
	 * @param ctx the parse tree
	 */
	void exitXmltype_virtual_columns(PlSqlParserParser.Xmltype_virtual_columnsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xmltype_column_properties}.
	 * @param ctx the parse tree
	 */
	void enterXmltype_column_properties(PlSqlParserParser.Xmltype_column_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xmltype_column_properties}.
	 * @param ctx the parse tree
	 */
	void exitXmltype_column_properties(PlSqlParserParser.Xmltype_column_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xmltype_storage}.
	 * @param ctx the parse tree
	 */
	void enterXmltype_storage(PlSqlParserParser.Xmltype_storageContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xmltype_storage}.
	 * @param ctx the parse tree
	 */
	void exitXmltype_storage(PlSqlParserParser.Xmltype_storageContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xmlschema_spec}.
	 * @param ctx the parse tree
	 */
	void enterXmlschema_spec(PlSqlParserParser.Xmlschema_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xmlschema_spec}.
	 * @param ctx the parse tree
	 */
	void exitXmlschema_spec(PlSqlParserParser.Xmlschema_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#object_table}.
	 * @param ctx the parse tree
	 */
	void enterObject_table(PlSqlParserParser.Object_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#object_table}.
	 * @param ctx the parse tree
	 */
	void exitObject_table(PlSqlParserParser.Object_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#oid_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterOid_index_clause(PlSqlParserParser.Oid_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#oid_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitOid_index_clause(PlSqlParserParser.Oid_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#oid_clause}.
	 * @param ctx the parse tree
	 */
	void enterOid_clause(PlSqlParserParser.Oid_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#oid_clause}.
	 * @param ctx the parse tree
	 */
	void exitOid_clause(PlSqlParserParser.Oid_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#object_properties}.
	 * @param ctx the parse tree
	 */
	void enterObject_properties(PlSqlParserParser.Object_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#object_properties}.
	 * @param ctx the parse tree
	 */
	void exitObject_properties(PlSqlParserParser.Object_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#object_table_substitution}.
	 * @param ctx the parse tree
	 */
	void enterObject_table_substitution(PlSqlParserParser.Object_table_substitutionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#object_table_substitution}.
	 * @param ctx the parse tree
	 */
	void exitObject_table_substitution(PlSqlParserParser.Object_table_substitutionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#relational_table}.
	 * @param ctx the parse tree
	 */
	void enterRelational_table(PlSqlParserParser.Relational_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#relational_table}.
	 * @param ctx the parse tree
	 */
	void exitRelational_table(PlSqlParserParser.Relational_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#relational_property}.
	 * @param ctx the parse tree
	 */
	void enterRelational_property(PlSqlParserParser.Relational_propertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#relational_property}.
	 * @param ctx the parse tree
	 */
	void exitRelational_property(PlSqlParserParser.Relational_propertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#table_partitioning_clauses}.
	 * @param ctx the parse tree
	 */
	void enterTable_partitioning_clauses(PlSqlParserParser.Table_partitioning_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#table_partitioning_clauses}.
	 * @param ctx the parse tree
	 */
	void exitTable_partitioning_clauses(PlSqlParserParser.Table_partitioning_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#range_partitions}.
	 * @param ctx the parse tree
	 */
	void enterRange_partitions(PlSqlParserParser.Range_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#range_partitions}.
	 * @param ctx the parse tree
	 */
	void exitRange_partitions(PlSqlParserParser.Range_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#list_partitions}.
	 * @param ctx the parse tree
	 */
	void enterList_partitions(PlSqlParserParser.List_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#list_partitions}.
	 * @param ctx the parse tree
	 */
	void exitList_partitions(PlSqlParserParser.List_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#hash_partitions}.
	 * @param ctx the parse tree
	 */
	void enterHash_partitions(PlSqlParserParser.Hash_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#hash_partitions}.
	 * @param ctx the parse tree
	 */
	void exitHash_partitions(PlSqlParserParser.Hash_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#individual_hash_partitions}.
	 * @param ctx the parse tree
	 */
	void enterIndividual_hash_partitions(PlSqlParserParser.Individual_hash_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#individual_hash_partitions}.
	 * @param ctx the parse tree
	 */
	void exitIndividual_hash_partitions(PlSqlParserParser.Individual_hash_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#hash_partitions_by_quantity}.
	 * @param ctx the parse tree
	 */
	void enterHash_partitions_by_quantity(PlSqlParserParser.Hash_partitions_by_quantityContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#hash_partitions_by_quantity}.
	 * @param ctx the parse tree
	 */
	void exitHash_partitions_by_quantity(PlSqlParserParser.Hash_partitions_by_quantityContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#hash_partition_quantity}.
	 * @param ctx the parse tree
	 */
	void enterHash_partition_quantity(PlSqlParserParser.Hash_partition_quantityContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#hash_partition_quantity}.
	 * @param ctx the parse tree
	 */
	void exitHash_partition_quantity(PlSqlParserParser.Hash_partition_quantityContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#composite_range_partitions}.
	 * @param ctx the parse tree
	 */
	void enterComposite_range_partitions(PlSqlParserParser.Composite_range_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#composite_range_partitions}.
	 * @param ctx the parse tree
	 */
	void exitComposite_range_partitions(PlSqlParserParser.Composite_range_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#composite_list_partitions}.
	 * @param ctx the parse tree
	 */
	void enterComposite_list_partitions(PlSqlParserParser.Composite_list_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#composite_list_partitions}.
	 * @param ctx the parse tree
	 */
	void exitComposite_list_partitions(PlSqlParserParser.Composite_list_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#composite_hash_partitions}.
	 * @param ctx the parse tree
	 */
	void enterComposite_hash_partitions(PlSqlParserParser.Composite_hash_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#composite_hash_partitions}.
	 * @param ctx the parse tree
	 */
	void exitComposite_hash_partitions(PlSqlParserParser.Composite_hash_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#reference_partitioning}.
	 * @param ctx the parse tree
	 */
	void enterReference_partitioning(PlSqlParserParser.Reference_partitioningContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#reference_partitioning}.
	 * @param ctx the parse tree
	 */
	void exitReference_partitioning(PlSqlParserParser.Reference_partitioningContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#reference_partition_desc}.
	 * @param ctx the parse tree
	 */
	void enterReference_partition_desc(PlSqlParserParser.Reference_partition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#reference_partition_desc}.
	 * @param ctx the parse tree
	 */
	void exitReference_partition_desc(PlSqlParserParser.Reference_partition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#system_partitioning}.
	 * @param ctx the parse tree
	 */
	void enterSystem_partitioning(PlSqlParserParser.System_partitioningContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#system_partitioning}.
	 * @param ctx the parse tree
	 */
	void exitSystem_partitioning(PlSqlParserParser.System_partitioningContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#range_partition_desc}.
	 * @param ctx the parse tree
	 */
	void enterRange_partition_desc(PlSqlParserParser.Range_partition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#range_partition_desc}.
	 * @param ctx the parse tree
	 */
	void exitRange_partition_desc(PlSqlParserParser.Range_partition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#list_partition_desc}.
	 * @param ctx the parse tree
	 */
	void enterList_partition_desc(PlSqlParserParser.List_partition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#list_partition_desc}.
	 * @param ctx the parse tree
	 */
	void exitList_partition_desc(PlSqlParserParser.List_partition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subpartition_template}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_template(PlSqlParserParser.Subpartition_templateContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subpartition_template}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_template(PlSqlParserParser.Subpartition_templateContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#hash_subpartition_quantity}.
	 * @param ctx the parse tree
	 */
	void enterHash_subpartition_quantity(PlSqlParserParser.Hash_subpartition_quantityContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#hash_subpartition_quantity}.
	 * @param ctx the parse tree
	 */
	void exitHash_subpartition_quantity(PlSqlParserParser.Hash_subpartition_quantityContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subpartition_by_range}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_by_range(PlSqlParserParser.Subpartition_by_rangeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subpartition_by_range}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_by_range(PlSqlParserParser.Subpartition_by_rangeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subpartition_by_list}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_by_list(PlSqlParserParser.Subpartition_by_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subpartition_by_list}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_by_list(PlSqlParserParser.Subpartition_by_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subpartition_by_hash}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_by_hash(PlSqlParserParser.Subpartition_by_hashContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subpartition_by_hash}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_by_hash(PlSqlParserParser.Subpartition_by_hashContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subpartition_name}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_name(PlSqlParserParser.Subpartition_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subpartition_name}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_name(PlSqlParserParser.Subpartition_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#range_subpartition_desc}.
	 * @param ctx the parse tree
	 */
	void enterRange_subpartition_desc(PlSqlParserParser.Range_subpartition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#range_subpartition_desc}.
	 * @param ctx the parse tree
	 */
	void exitRange_subpartition_desc(PlSqlParserParser.Range_subpartition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#list_subpartition_desc}.
	 * @param ctx the parse tree
	 */
	void enterList_subpartition_desc(PlSqlParserParser.List_subpartition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#list_subpartition_desc}.
	 * @param ctx the parse tree
	 */
	void exitList_subpartition_desc(PlSqlParserParser.List_subpartition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#individual_hash_subparts}.
	 * @param ctx the parse tree
	 */
	void enterIndividual_hash_subparts(PlSqlParserParser.Individual_hash_subpartsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#individual_hash_subparts}.
	 * @param ctx the parse tree
	 */
	void exitIndividual_hash_subparts(PlSqlParserParser.Individual_hash_subpartsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#hash_subparts_by_quantity}.
	 * @param ctx the parse tree
	 */
	void enterHash_subparts_by_quantity(PlSqlParserParser.Hash_subparts_by_quantityContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#hash_subparts_by_quantity}.
	 * @param ctx the parse tree
	 */
	void exitHash_subparts_by_quantity(PlSqlParserParser.Hash_subparts_by_quantityContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#range_values_clause}.
	 * @param ctx the parse tree
	 */
	void enterRange_values_clause(PlSqlParserParser.Range_values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#range_values_clause}.
	 * @param ctx the parse tree
	 */
	void exitRange_values_clause(PlSqlParserParser.Range_values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#list_values_clause}.
	 * @param ctx the parse tree
	 */
	void enterList_values_clause(PlSqlParserParser.List_values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#list_values_clause}.
	 * @param ctx the parse tree
	 */
	void exitList_values_clause(PlSqlParserParser.List_values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#table_partition_description}.
	 * @param ctx the parse tree
	 */
	void enterTable_partition_description(PlSqlParserParser.Table_partition_descriptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#table_partition_description}.
	 * @param ctx the parse tree
	 */
	void exitTable_partition_description(PlSqlParserParser.Table_partition_descriptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#partitioning_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterPartitioning_storage_clause(PlSqlParserParser.Partitioning_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#partitioning_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitPartitioning_storage_clause(PlSqlParserParser.Partitioning_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#lob_partitioning_storage}.
	 * @param ctx the parse tree
	 */
	void enterLob_partitioning_storage(PlSqlParserParser.Lob_partitioning_storageContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#lob_partitioning_storage}.
	 * @param ctx the parse tree
	 */
	void exitLob_partitioning_storage(PlSqlParserParser.Lob_partitioning_storageContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#datatype_null_enable}.
	 * @param ctx the parse tree
	 */
	void enterDatatype_null_enable(PlSqlParserParser.Datatype_null_enableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#datatype_null_enable}.
	 * @param ctx the parse tree
	 */
	void exitDatatype_null_enable(PlSqlParserParser.Datatype_null_enableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#size_clause}.
	 * @param ctx the parse tree
	 */
	void enterSize_clause(PlSqlParserParser.Size_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#size_clause}.
	 * @param ctx the parse tree
	 */
	void exitSize_clause(PlSqlParserParser.Size_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#table_compression}.
	 * @param ctx the parse tree
	 */
	void enterTable_compression(PlSqlParserParser.Table_compressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#table_compression}.
	 * @param ctx the parse tree
	 */
	void exitTable_compression(PlSqlParserParser.Table_compressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#physical_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void enterPhysical_attributes_clause(PlSqlParserParser.Physical_attributes_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#physical_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void exitPhysical_attributes_clause(PlSqlParserParser.Physical_attributes_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterStorage_clause(PlSqlParserParser.Storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitStorage_clause(PlSqlParserParser.Storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#deferred_segment_creation}.
	 * @param ctx the parse tree
	 */
	void enterDeferred_segment_creation(PlSqlParserParser.Deferred_segment_creationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#deferred_segment_creation}.
	 * @param ctx the parse tree
	 */
	void exitDeferred_segment_creation(PlSqlParserParser.Deferred_segment_creationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#segment_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void enterSegment_attributes_clause(PlSqlParserParser.Segment_attributes_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#segment_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void exitSegment_attributes_clause(PlSqlParserParser.Segment_attributes_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#physical_properties}.
	 * @param ctx the parse tree
	 */
	void enterPhysical_properties(PlSqlParserParser.Physical_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#physical_properties}.
	 * @param ctx the parse tree
	 */
	void exitPhysical_properties(PlSqlParserParser.Physical_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#row_movement_clause}.
	 * @param ctx the parse tree
	 */
	void enterRow_movement_clause(PlSqlParserParser.Row_movement_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#row_movement_clause}.
	 * @param ctx the parse tree
	 */
	void exitRow_movement_clause(PlSqlParserParser.Row_movement_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#flashback_archive_clause}.
	 * @param ctx the parse tree
	 */
	void enterFlashback_archive_clause(PlSqlParserParser.Flashback_archive_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#flashback_archive_clause}.
	 * @param ctx the parse tree
	 */
	void exitFlashback_archive_clause(PlSqlParserParser.Flashback_archive_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#log_grp}.
	 * @param ctx the parse tree
	 */
	void enterLog_grp(PlSqlParserParser.Log_grpContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#log_grp}.
	 * @param ctx the parse tree
	 */
	void exitLog_grp(PlSqlParserParser.Log_grpContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#supplemental_table_logging}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_table_logging(PlSqlParserParser.Supplemental_table_loggingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#supplemental_table_logging}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_table_logging(PlSqlParserParser.Supplemental_table_loggingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#supplemental_log_grp_clause}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_log_grp_clause(PlSqlParserParser.Supplemental_log_grp_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#supplemental_log_grp_clause}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_log_grp_clause(PlSqlParserParser.Supplemental_log_grp_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#supplemental_id_key_clause}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_id_key_clause(PlSqlParserParser.Supplemental_id_key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#supplemental_id_key_clause}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_id_key_clause(PlSqlParserParser.Supplemental_id_key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#allocate_extent_clause}.
	 * @param ctx the parse tree
	 */
	void enterAllocate_extent_clause(PlSqlParserParser.Allocate_extent_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#allocate_extent_clause}.
	 * @param ctx the parse tree
	 */
	void exitAllocate_extent_clause(PlSqlParserParser.Allocate_extent_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#deallocate_unused_clause}.
	 * @param ctx the parse tree
	 */
	void enterDeallocate_unused_clause(PlSqlParserParser.Deallocate_unused_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#deallocate_unused_clause}.
	 * @param ctx the parse tree
	 */
	void exitDeallocate_unused_clause(PlSqlParserParser.Deallocate_unused_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#shrink_clause}.
	 * @param ctx the parse tree
	 */
	void enterShrink_clause(PlSqlParserParser.Shrink_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#shrink_clause}.
	 * @param ctx the parse tree
	 */
	void exitShrink_clause(PlSqlParserParser.Shrink_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#records_per_block_clause}.
	 * @param ctx the parse tree
	 */
	void enterRecords_per_block_clause(PlSqlParserParser.Records_per_block_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#records_per_block_clause}.
	 * @param ctx the parse tree
	 */
	void exitRecords_per_block_clause(PlSqlParserParser.Records_per_block_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#upgrade_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterUpgrade_table_clause(PlSqlParserParser.Upgrade_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#upgrade_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitUpgrade_table_clause(PlSqlParserParser.Upgrade_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#truncate_table}.
	 * @param ctx the parse tree
	 */
	void enterTruncate_table(PlSqlParserParser.Truncate_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#truncate_table}.
	 * @param ctx the parse tree
	 */
	void exitTruncate_table(PlSqlParserParser.Truncate_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_table}.
	 * @param ctx the parse tree
	 */
	void enterDrop_table(PlSqlParserParser.Drop_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_table}.
	 * @param ctx the parse tree
	 */
	void exitDrop_table(PlSqlParserParser.Drop_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_view}.
	 * @param ctx the parse tree
	 */
	void enterDrop_view(PlSqlParserParser.Drop_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_view}.
	 * @param ctx the parse tree
	 */
	void exitDrop_view(PlSqlParserParser.Drop_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#comment_on_column}.
	 * @param ctx the parse tree
	 */
	void enterComment_on_column(PlSqlParserParser.Comment_on_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#comment_on_column}.
	 * @param ctx the parse tree
	 */
	void exitComment_on_column(PlSqlParserParser.Comment_on_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#enable_or_disable}.
	 * @param ctx the parse tree
	 */
	void enterEnable_or_disable(PlSqlParserParser.Enable_or_disableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#enable_or_disable}.
	 * @param ctx the parse tree
	 */
	void exitEnable_or_disable(PlSqlParserParser.Enable_or_disableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#allow_or_disallow}.
	 * @param ctx the parse tree
	 */
	void enterAllow_or_disallow(PlSqlParserParser.Allow_or_disallowContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#allow_or_disallow}.
	 * @param ctx the parse tree
	 */
	void exitAllow_or_disallow(PlSqlParserParser.Allow_or_disallowContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_synonym}.
	 * @param ctx the parse tree
	 */
	void enterCreate_synonym(PlSqlParserParser.Create_synonymContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_synonym}.
	 * @param ctx the parse tree
	 */
	void exitCreate_synonym(PlSqlParserParser.Create_synonymContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#comment_on_table}.
	 * @param ctx the parse tree
	 */
	void enterComment_on_table(PlSqlParserParser.Comment_on_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#comment_on_table}.
	 * @param ctx the parse tree
	 */
	void exitComment_on_table(PlSqlParserParser.Comment_on_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_cluster}.
	 * @param ctx the parse tree
	 */
	void enterAlter_cluster(PlSqlParserParser.Alter_clusterContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_cluster}.
	 * @param ctx the parse tree
	 */
	void exitAlter_cluster(PlSqlParserParser.Alter_clusterContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cache_or_nocache}.
	 * @param ctx the parse tree
	 */
	void enterCache_or_nocache(PlSqlParserParser.Cache_or_nocacheContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cache_or_nocache}.
	 * @param ctx the parse tree
	 */
	void exitCache_or_nocache(PlSqlParserParser.Cache_or_nocacheContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#database_name}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_name(PlSqlParserParser.Database_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#database_name}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_name(PlSqlParserParser.Database_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_database}.
	 * @param ctx the parse tree
	 */
	void enterAlter_database(PlSqlParserParser.Alter_databaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_database}.
	 * @param ctx the parse tree
	 */
	void exitAlter_database(PlSqlParserParser.Alter_databaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#startup_clauses}.
	 * @param ctx the parse tree
	 */
	void enterStartup_clauses(PlSqlParserParser.Startup_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#startup_clauses}.
	 * @param ctx the parse tree
	 */
	void exitStartup_clauses(PlSqlParserParser.Startup_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#resetlogs_or_noresetlogs}.
	 * @param ctx the parse tree
	 */
	void enterResetlogs_or_noresetlogs(PlSqlParserParser.Resetlogs_or_noresetlogsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#resetlogs_or_noresetlogs}.
	 * @param ctx the parse tree
	 */
	void exitResetlogs_or_noresetlogs(PlSqlParserParser.Resetlogs_or_noresetlogsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#upgrade_or_downgrade}.
	 * @param ctx the parse tree
	 */
	void enterUpgrade_or_downgrade(PlSqlParserParser.Upgrade_or_downgradeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#upgrade_or_downgrade}.
	 * @param ctx the parse tree
	 */
	void exitUpgrade_or_downgrade(PlSqlParserParser.Upgrade_or_downgradeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#recovery_clauses}.
	 * @param ctx the parse tree
	 */
	void enterRecovery_clauses(PlSqlParserParser.Recovery_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#recovery_clauses}.
	 * @param ctx the parse tree
	 */
	void exitRecovery_clauses(PlSqlParserParser.Recovery_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#begin_or_end}.
	 * @param ctx the parse tree
	 */
	void enterBegin_or_end(PlSqlParserParser.Begin_or_endContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#begin_or_end}.
	 * @param ctx the parse tree
	 */
	void exitBegin_or_end(PlSqlParserParser.Begin_or_endContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#general_recovery}.
	 * @param ctx the parse tree
	 */
	void enterGeneral_recovery(PlSqlParserParser.General_recoveryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#general_recovery}.
	 * @param ctx the parse tree
	 */
	void exitGeneral_recovery(PlSqlParserParser.General_recoveryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#full_database_recovery}.
	 * @param ctx the parse tree
	 */
	void enterFull_database_recovery(PlSqlParserParser.Full_database_recoveryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#full_database_recovery}.
	 * @param ctx the parse tree
	 */
	void exitFull_database_recovery(PlSqlParserParser.Full_database_recoveryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#partial_database_recovery}.
	 * @param ctx the parse tree
	 */
	void enterPartial_database_recovery(PlSqlParserParser.Partial_database_recoveryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#partial_database_recovery}.
	 * @param ctx the parse tree
	 */
	void exitPartial_database_recovery(PlSqlParserParser.Partial_database_recoveryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#partial_database_recovery_10g}.
	 * @param ctx the parse tree
	 */
	void enterPartial_database_recovery_10g(PlSqlParserParser.Partial_database_recovery_10gContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#partial_database_recovery_10g}.
	 * @param ctx the parse tree
	 */
	void exitPartial_database_recovery_10g(PlSqlParserParser.Partial_database_recovery_10gContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#managed_standby_recovery}.
	 * @param ctx the parse tree
	 */
	void enterManaged_standby_recovery(PlSqlParserParser.Managed_standby_recoveryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#managed_standby_recovery}.
	 * @param ctx the parse tree
	 */
	void exitManaged_standby_recovery(PlSqlParserParser.Managed_standby_recoveryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#db_name}.
	 * @param ctx the parse tree
	 */
	void enterDb_name(PlSqlParserParser.Db_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#db_name}.
	 * @param ctx the parse tree
	 */
	void exitDb_name(PlSqlParserParser.Db_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#database_file_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_file_clauses(PlSqlParserParser.Database_file_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#database_file_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_file_clauses(PlSqlParserParser.Database_file_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#create_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void enterCreate_datafile_clause(PlSqlParserParser.Create_datafile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#create_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void exitCreate_datafile_clause(PlSqlParserParser.Create_datafile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_datafile_clause(PlSqlParserParser.Alter_datafile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_datafile_clause(PlSqlParserParser.Alter_datafile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_tempfile_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tempfile_clause(PlSqlParserParser.Alter_tempfile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_tempfile_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tempfile_clause(PlSqlParserParser.Alter_tempfile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterLogfile_clauses(PlSqlParserParser.Logfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitLogfile_clauses(PlSqlParserParser.Logfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#add_logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterAdd_logfile_clauses(PlSqlParserParser.Add_logfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#add_logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitAdd_logfile_clauses(PlSqlParserParser.Add_logfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#log_file_group}.
	 * @param ctx the parse tree
	 */
	void enterLog_file_group(PlSqlParserParser.Log_file_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#log_file_group}.
	 * @param ctx the parse tree
	 */
	void exitLog_file_group(PlSqlParserParser.Log_file_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDrop_logfile_clauses(PlSqlParserParser.Drop_logfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDrop_logfile_clauses(PlSqlParserParser.Drop_logfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#switch_logfile_clause}.
	 * @param ctx the parse tree
	 */
	void enterSwitch_logfile_clause(PlSqlParserParser.Switch_logfile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#switch_logfile_clause}.
	 * @param ctx the parse tree
	 */
	void exitSwitch_logfile_clause(PlSqlParserParser.Switch_logfile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#supplemental_db_logging}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_db_logging(PlSqlParserParser.Supplemental_db_loggingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#supplemental_db_logging}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_db_logging(PlSqlParserParser.Supplemental_db_loggingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#add_or_drop}.
	 * @param ctx the parse tree
	 */
	void enterAdd_or_drop(PlSqlParserParser.Add_or_dropContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#add_or_drop}.
	 * @param ctx the parse tree
	 */
	void exitAdd_or_drop(PlSqlParserParser.Add_or_dropContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#supplemental_plsql_clause}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_plsql_clause(PlSqlParserParser.Supplemental_plsql_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#supplemental_plsql_clause}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_plsql_clause(PlSqlParserParser.Supplemental_plsql_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#logfile_descriptor}.
	 * @param ctx the parse tree
	 */
	void enterLogfile_descriptor(PlSqlParserParser.Logfile_descriptorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#logfile_descriptor}.
	 * @param ctx the parse tree
	 */
	void exitLogfile_descriptor(PlSqlParserParser.Logfile_descriptorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#controlfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterControlfile_clauses(PlSqlParserParser.Controlfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#controlfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitControlfile_clauses(PlSqlParserParser.Controlfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#trace_file_clause}.
	 * @param ctx the parse tree
	 */
	void enterTrace_file_clause(PlSqlParserParser.Trace_file_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#trace_file_clause}.
	 * @param ctx the parse tree
	 */
	void exitTrace_file_clause(PlSqlParserParser.Trace_file_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#standby_database_clauses}.
	 * @param ctx the parse tree
	 */
	void enterStandby_database_clauses(PlSqlParserParser.Standby_database_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#standby_database_clauses}.
	 * @param ctx the parse tree
	 */
	void exitStandby_database_clauses(PlSqlParserParser.Standby_database_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#activate_standby_db_clause}.
	 * @param ctx the parse tree
	 */
	void enterActivate_standby_db_clause(PlSqlParserParser.Activate_standby_db_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#activate_standby_db_clause}.
	 * @param ctx the parse tree
	 */
	void exitActivate_standby_db_clause(PlSqlParserParser.Activate_standby_db_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#maximize_standby_db_clause}.
	 * @param ctx the parse tree
	 */
	void enterMaximize_standby_db_clause(PlSqlParserParser.Maximize_standby_db_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#maximize_standby_db_clause}.
	 * @param ctx the parse tree
	 */
	void exitMaximize_standby_db_clause(PlSqlParserParser.Maximize_standby_db_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#register_logfile_clause}.
	 * @param ctx the parse tree
	 */
	void enterRegister_logfile_clause(PlSqlParserParser.Register_logfile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#register_logfile_clause}.
	 * @param ctx the parse tree
	 */
	void exitRegister_logfile_clause(PlSqlParserParser.Register_logfile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#commit_switchover_clause}.
	 * @param ctx the parse tree
	 */
	void enterCommit_switchover_clause(PlSqlParserParser.Commit_switchover_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#commit_switchover_clause}.
	 * @param ctx the parse tree
	 */
	void exitCommit_switchover_clause(PlSqlParserParser.Commit_switchover_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#start_standby_clause}.
	 * @param ctx the parse tree
	 */
	void enterStart_standby_clause(PlSqlParserParser.Start_standby_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#start_standby_clause}.
	 * @param ctx the parse tree
	 */
	void exitStart_standby_clause(PlSqlParserParser.Start_standby_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#stop_standby_clause}.
	 * @param ctx the parse tree
	 */
	void enterStop_standby_clause(PlSqlParserParser.Stop_standby_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#stop_standby_clause}.
	 * @param ctx the parse tree
	 */
	void exitStop_standby_clause(PlSqlParserParser.Stop_standby_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#convert_database_clause}.
	 * @param ctx the parse tree
	 */
	void enterConvert_database_clause(PlSqlParserParser.Convert_database_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#convert_database_clause}.
	 * @param ctx the parse tree
	 */
	void exitConvert_database_clause(PlSqlParserParser.Convert_database_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#default_settings_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_settings_clause(PlSqlParserParser.Default_settings_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#default_settings_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_settings_clause(PlSqlParserParser.Default_settings_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#set_time_zone_clause}.
	 * @param ctx the parse tree
	 */
	void enterSet_time_zone_clause(PlSqlParserParser.Set_time_zone_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#set_time_zone_clause}.
	 * @param ctx the parse tree
	 */
	void exitSet_time_zone_clause(PlSqlParserParser.Set_time_zone_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#instance_clauses}.
	 * @param ctx the parse tree
	 */
	void enterInstance_clauses(PlSqlParserParser.Instance_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#instance_clauses}.
	 * @param ctx the parse tree
	 */
	void exitInstance_clauses(PlSqlParserParser.Instance_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#security_clause}.
	 * @param ctx the parse tree
	 */
	void enterSecurity_clause(PlSqlParserParser.Security_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#security_clause}.
	 * @param ctx the parse tree
	 */
	void exitSecurity_clause(PlSqlParserParser.Security_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#domain}.
	 * @param ctx the parse tree
	 */
	void enterDomain(PlSqlParserParser.DomainContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#domain}.
	 * @param ctx the parse tree
	 */
	void exitDomain(PlSqlParserParser.DomainContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#database}.
	 * @param ctx the parse tree
	 */
	void enterDatabase(PlSqlParserParser.DatabaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#database}.
	 * @param ctx the parse tree
	 */
	void exitDatabase(PlSqlParserParser.DatabaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#edition_name}.
	 * @param ctx the parse tree
	 */
	void enterEdition_name(PlSqlParserParser.Edition_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#edition_name}.
	 * @param ctx the parse tree
	 */
	void exitEdition_name(PlSqlParserParser.Edition_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#filenumber}.
	 * @param ctx the parse tree
	 */
	void enterFilenumber(PlSqlParserParser.FilenumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#filenumber}.
	 * @param ctx the parse tree
	 */
	void exitFilenumber(PlSqlParserParser.FilenumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#filename}.
	 * @param ctx the parse tree
	 */
	void enterFilename(PlSqlParserParser.FilenameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#filename}.
	 * @param ctx the parse tree
	 */
	void exitFilename(PlSqlParserParser.FilenameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_table}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table(PlSqlParserParser.Alter_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_table}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table(PlSqlParserParser.Alter_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_table_properties}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_properties(PlSqlParserParser.Alter_table_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_table_properties}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_properties(PlSqlParserParser.Alter_table_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_table_properties_1}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_properties_1(PlSqlParserParser.Alter_table_properties_1Context ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_table_properties_1}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_properties_1(PlSqlParserParser.Alter_table_properties_1Context ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_iot_clauses}.
	 * @param ctx the parse tree
	 */
	void enterAlter_iot_clauses(PlSqlParserParser.Alter_iot_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_iot_clauses}.
	 * @param ctx the parse tree
	 */
	void exitAlter_iot_clauses(PlSqlParserParser.Alter_iot_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_mapping_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_mapping_table_clause(PlSqlParserParser.Alter_mapping_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_mapping_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_mapping_table_clause(PlSqlParserParser.Alter_mapping_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_overflow_clause(PlSqlParserParser.Alter_overflow_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_overflow_clause(PlSqlParserParser.Alter_overflow_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#add_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_overflow_clause(PlSqlParserParser.Add_overflow_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#add_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_overflow_clause(PlSqlParserParser.Add_overflow_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#enable_disable_clause}.
	 * @param ctx the parse tree
	 */
	void enterEnable_disable_clause(PlSqlParserParser.Enable_disable_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#enable_disable_clause}.
	 * @param ctx the parse tree
	 */
	void exitEnable_disable_clause(PlSqlParserParser.Enable_disable_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#using_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterUsing_index_clause(PlSqlParserParser.Using_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#using_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitUsing_index_clause(PlSqlParserParser.Using_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#index_attributes}.
	 * @param ctx the parse tree
	 */
	void enterIndex_attributes(PlSqlParserParser.Index_attributesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#index_attributes}.
	 * @param ctx the parse tree
	 */
	void exitIndex_attributes(PlSqlParserParser.Index_attributesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#sort_or_nosort}.
	 * @param ctx the parse tree
	 */
	void enterSort_or_nosort(PlSqlParserParser.Sort_or_nosortContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#sort_or_nosort}.
	 * @param ctx the parse tree
	 */
	void exitSort_or_nosort(PlSqlParserParser.Sort_or_nosortContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#exceptions_clause}.
	 * @param ctx the parse tree
	 */
	void enterExceptions_clause(PlSqlParserParser.Exceptions_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#exceptions_clause}.
	 * @param ctx the parse tree
	 */
	void exitExceptions_clause(PlSqlParserParser.Exceptions_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#move_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterMove_table_clause(PlSqlParserParser.Move_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#move_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitMove_table_clause(PlSqlParserParser.Move_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#index_org_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_org_table_clause(PlSqlParserParser.Index_org_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#index_org_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_org_table_clause(PlSqlParserParser.Index_org_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#mapping_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterMapping_table_clause(PlSqlParserParser.Mapping_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#mapping_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitMapping_table_clause(PlSqlParserParser.Mapping_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#key_compression}.
	 * @param ctx the parse tree
	 */
	void enterKey_compression(PlSqlParserParser.Key_compressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#key_compression}.
	 * @param ctx the parse tree
	 */
	void exitKey_compression(PlSqlParserParser.Key_compressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#index_org_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_org_overflow_clause(PlSqlParserParser.Index_org_overflow_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#index_org_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_org_overflow_clause(PlSqlParserParser.Index_org_overflow_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#column_clauses}.
	 * @param ctx the parse tree
	 */
	void enterColumn_clauses(PlSqlParserParser.Column_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#column_clauses}.
	 * @param ctx the parse tree
	 */
	void exitColumn_clauses(PlSqlParserParser.Column_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#modify_collection_retrieval}.
	 * @param ctx the parse tree
	 */
	void enterModify_collection_retrieval(PlSqlParserParser.Modify_collection_retrievalContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#modify_collection_retrieval}.
	 * @param ctx the parse tree
	 */
	void exitModify_collection_retrieval(PlSqlParserParser.Modify_collection_retrievalContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#collection_item}.
	 * @param ctx the parse tree
	 */
	void enterCollection_item(PlSqlParserParser.Collection_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#collection_item}.
	 * @param ctx the parse tree
	 */
	void exitCollection_item(PlSqlParserParser.Collection_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#rename_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterRename_column_clause(PlSqlParserParser.Rename_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#rename_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitRename_column_clause(PlSqlParserParser.Rename_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#old_column_name}.
	 * @param ctx the parse tree
	 */
	void enterOld_column_name(PlSqlParserParser.Old_column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#old_column_name}.
	 * @param ctx the parse tree
	 */
	void exitOld_column_name(PlSqlParserParser.Old_column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#new_column_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_column_name(PlSqlParserParser.New_column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#new_column_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_column_name(PlSqlParserParser.New_column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#add_modify_drop_column_clauses}.
	 * @param ctx the parse tree
	 */
	void enterAdd_modify_drop_column_clauses(PlSqlParserParser.Add_modify_drop_column_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#add_modify_drop_column_clauses}.
	 * @param ctx the parse tree
	 */
	void exitAdd_modify_drop_column_clauses(PlSqlParserParser.Add_modify_drop_column_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_column_clause(PlSqlParserParser.Drop_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_column_clause(PlSqlParserParser.Drop_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#modify_column_clauses}.
	 * @param ctx the parse tree
	 */
	void enterModify_column_clauses(PlSqlParserParser.Modify_column_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#modify_column_clauses}.
	 * @param ctx the parse tree
	 */
	void exitModify_column_clauses(PlSqlParserParser.Modify_column_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#modify_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterModify_col_properties(PlSqlParserParser.Modify_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#modify_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitModify_col_properties(PlSqlParserParser.Modify_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#modify_col_substitutable}.
	 * @param ctx the parse tree
	 */
	void enterModify_col_substitutable(PlSqlParserParser.Modify_col_substitutableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#modify_col_substitutable}.
	 * @param ctx the parse tree
	 */
	void exitModify_col_substitutable(PlSqlParserParser.Modify_col_substitutableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#add_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_column_clause(PlSqlParserParser.Add_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#add_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_column_clause(PlSqlParserParser.Add_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#alter_varray_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterAlter_varray_col_properties(PlSqlParserParser.Alter_varray_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#alter_varray_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitAlter_varray_col_properties(PlSqlParserParser.Alter_varray_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#varray_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterVarray_col_properties(PlSqlParserParser.Varray_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#varray_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitVarray_col_properties(PlSqlParserParser.Varray_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#varray_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterVarray_storage_clause(PlSqlParserParser.Varray_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#varray_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitVarray_storage_clause(PlSqlParserParser.Varray_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#lob_segname}.
	 * @param ctx the parse tree
	 */
	void enterLob_segname(PlSqlParserParser.Lob_segnameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#lob_segname}.
	 * @param ctx the parse tree
	 */
	void exitLob_segname(PlSqlParserParser.Lob_segnameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#lob_item}.
	 * @param ctx the parse tree
	 */
	void enterLob_item(PlSqlParserParser.Lob_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#lob_item}.
	 * @param ctx the parse tree
	 */
	void exitLob_item(PlSqlParserParser.Lob_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#lob_storage_parameters}.
	 * @param ctx the parse tree
	 */
	void enterLob_storage_parameters(PlSqlParserParser.Lob_storage_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#lob_storage_parameters}.
	 * @param ctx the parse tree
	 */
	void exitLob_storage_parameters(PlSqlParserParser.Lob_storage_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterLob_storage_clause(PlSqlParserParser.Lob_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitLob_storage_clause(PlSqlParserParser.Lob_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#modify_lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterModify_lob_storage_clause(PlSqlParserParser.Modify_lob_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#modify_lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitModify_lob_storage_clause(PlSqlParserParser.Modify_lob_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#modify_lob_parameters}.
	 * @param ctx the parse tree
	 */
	void enterModify_lob_parameters(PlSqlParserParser.Modify_lob_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#modify_lob_parameters}.
	 * @param ctx the parse tree
	 */
	void exitModify_lob_parameters(PlSqlParserParser.Modify_lob_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#lob_parameters}.
	 * @param ctx the parse tree
	 */
	void enterLob_parameters(PlSqlParserParser.Lob_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#lob_parameters}.
	 * @param ctx the parse tree
	 */
	void exitLob_parameters(PlSqlParserParser.Lob_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#lob_deduplicate_clause}.
	 * @param ctx the parse tree
	 */
	void enterLob_deduplicate_clause(PlSqlParserParser.Lob_deduplicate_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#lob_deduplicate_clause}.
	 * @param ctx the parse tree
	 */
	void exitLob_deduplicate_clause(PlSqlParserParser.Lob_deduplicate_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#lob_compression_clause}.
	 * @param ctx the parse tree
	 */
	void enterLob_compression_clause(PlSqlParserParser.Lob_compression_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#lob_compression_clause}.
	 * @param ctx the parse tree
	 */
	void exitLob_compression_clause(PlSqlParserParser.Lob_compression_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#lob_retention_clause}.
	 * @param ctx the parse tree
	 */
	void enterLob_retention_clause(PlSqlParserParser.Lob_retention_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#lob_retention_clause}.
	 * @param ctx the parse tree
	 */
	void exitLob_retention_clause(PlSqlParserParser.Lob_retention_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#encryption_spec}.
	 * @param ctx the parse tree
	 */
	void enterEncryption_spec(PlSqlParserParser.Encryption_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#encryption_spec}.
	 * @param ctx the parse tree
	 */
	void exitEncryption_spec(PlSqlParserParser.Encryption_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#tablespace}.
	 * @param ctx the parse tree
	 */
	void enterTablespace(PlSqlParserParser.TablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#tablespace}.
	 * @param ctx the parse tree
	 */
	void exitTablespace(PlSqlParserParser.TablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#varray_item}.
	 * @param ctx the parse tree
	 */
	void enterVarray_item(PlSqlParserParser.Varray_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#varray_item}.
	 * @param ctx the parse tree
	 */
	void exitVarray_item(PlSqlParserParser.Varray_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#column_properties}.
	 * @param ctx the parse tree
	 */
	void enterColumn_properties(PlSqlParserParser.Column_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#column_properties}.
	 * @param ctx the parse tree
	 */
	void exitColumn_properties(PlSqlParserParser.Column_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#period_definition}.
	 * @param ctx the parse tree
	 */
	void enterPeriod_definition(PlSqlParserParser.Period_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#period_definition}.
	 * @param ctx the parse tree
	 */
	void exitPeriod_definition(PlSqlParserParser.Period_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#start_time_column}.
	 * @param ctx the parse tree
	 */
	void enterStart_time_column(PlSqlParserParser.Start_time_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#start_time_column}.
	 * @param ctx the parse tree
	 */
	void exitStart_time_column(PlSqlParserParser.Start_time_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#end_time_column}.
	 * @param ctx the parse tree
	 */
	void enterEnd_time_column(PlSqlParserParser.End_time_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#end_time_column}.
	 * @param ctx the parse tree
	 */
	void exitEnd_time_column(PlSqlParserParser.End_time_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#column_definition}.
	 * @param ctx the parse tree
	 */
	void enterColumn_definition(PlSqlParserParser.Column_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#column_definition}.
	 * @param ctx the parse tree
	 */
	void exitColumn_definition(PlSqlParserParser.Column_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#virtual_column_definition}.
	 * @param ctx the parse tree
	 */
	void enterVirtual_column_definition(PlSqlParserParser.Virtual_column_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#virtual_column_definition}.
	 * @param ctx the parse tree
	 */
	void exitVirtual_column_definition(PlSqlParserParser.Virtual_column_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#autogenerated_sequence_definition}.
	 * @param ctx the parse tree
	 */
	void enterAutogenerated_sequence_definition(PlSqlParserParser.Autogenerated_sequence_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#autogenerated_sequence_definition}.
	 * @param ctx the parse tree
	 */
	void exitAutogenerated_sequence_definition(PlSqlParserParser.Autogenerated_sequence_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#out_of_line_part_storage}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_part_storage(PlSqlParserParser.Out_of_line_part_storageContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#out_of_line_part_storage}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_part_storage(PlSqlParserParser.Out_of_line_part_storageContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#nested_table_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterNested_table_col_properties(PlSqlParserParser.Nested_table_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#nested_table_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitNested_table_col_properties(PlSqlParserParser.Nested_table_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#nested_item}.
	 * @param ctx the parse tree
	 */
	void enterNested_item(PlSqlParserParser.Nested_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#nested_item}.
	 * @param ctx the parse tree
	 */
	void exitNested_item(PlSqlParserParser.Nested_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#substitutable_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterSubstitutable_column_clause(PlSqlParserParser.Substitutable_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#substitutable_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitSubstitutable_column_clause(PlSqlParserParser.Substitutable_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#partition_name}.
	 * @param ctx the parse tree
	 */
	void enterPartition_name(PlSqlParserParser.Partition_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#partition_name}.
	 * @param ctx the parse tree
	 */
	void exitPartition_name(PlSqlParserParser.Partition_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#supplemental_logging_props}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_logging_props(PlSqlParserParser.Supplemental_logging_propsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#supplemental_logging_props}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_logging_props(PlSqlParserParser.Supplemental_logging_propsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#column_or_attribute}.
	 * @param ctx the parse tree
	 */
	void enterColumn_or_attribute(PlSqlParserParser.Column_or_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#column_or_attribute}.
	 * @param ctx the parse tree
	 */
	void exitColumn_or_attribute(PlSqlParserParser.Column_or_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#object_type_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterObject_type_col_properties(PlSqlParserParser.Object_type_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#object_type_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitObject_type_col_properties(PlSqlParserParser.Object_type_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#constraint_clauses}.
	 * @param ctx the parse tree
	 */
	void enterConstraint_clauses(PlSqlParserParser.Constraint_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#constraint_clauses}.
	 * @param ctx the parse tree
	 */
	void exitConstraint_clauses(PlSqlParserParser.Constraint_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#old_constraint_name}.
	 * @param ctx the parse tree
	 */
	void enterOld_constraint_name(PlSqlParserParser.Old_constraint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#old_constraint_name}.
	 * @param ctx the parse tree
	 */
	void exitOld_constraint_name(PlSqlParserParser.Old_constraint_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#new_constraint_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_constraint_name(PlSqlParserParser.New_constraint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#new_constraint_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_constraint_name(PlSqlParserParser.New_constraint_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_constraint_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_constraint_clause(PlSqlParserParser.Drop_constraint_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_constraint_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_constraint_clause(PlSqlParserParser.Drop_constraint_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_primary_key_or_unique_or_generic_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_primary_key_or_unique_or_generic_clause(PlSqlParserParser.Drop_primary_key_or_unique_or_generic_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_primary_key_or_unique_or_generic_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_primary_key_or_unique_or_generic_clause(PlSqlParserParser.Drop_primary_key_or_unique_or_generic_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#add_constraint}.
	 * @param ctx the parse tree
	 */
	void enterAdd_constraint(PlSqlParserParser.Add_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#add_constraint}.
	 * @param ctx the parse tree
	 */
	void exitAdd_constraint(PlSqlParserParser.Add_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#add_constraint_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_constraint_clause(PlSqlParserParser.Add_constraint_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#add_constraint_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_constraint_clause(PlSqlParserParser.Add_constraint_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#check_constraint}.
	 * @param ctx the parse tree
	 */
	void enterCheck_constraint(PlSqlParserParser.Check_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#check_constraint}.
	 * @param ctx the parse tree
	 */
	void exitCheck_constraint(PlSqlParserParser.Check_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#drop_constraint}.
	 * @param ctx the parse tree
	 */
	void enterDrop_constraint(PlSqlParserParser.Drop_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#drop_constraint}.
	 * @param ctx the parse tree
	 */
	void exitDrop_constraint(PlSqlParserParser.Drop_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#enable_constraint}.
	 * @param ctx the parse tree
	 */
	void enterEnable_constraint(PlSqlParserParser.Enable_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#enable_constraint}.
	 * @param ctx the parse tree
	 */
	void exitEnable_constraint(PlSqlParserParser.Enable_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#disable_constraint}.
	 * @param ctx the parse tree
	 */
	void enterDisable_constraint(PlSqlParserParser.Disable_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#disable_constraint}.
	 * @param ctx the parse tree
	 */
	void exitDisable_constraint(PlSqlParserParser.Disable_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#foreign_key_clause}.
	 * @param ctx the parse tree
	 */
	void enterForeign_key_clause(PlSqlParserParser.Foreign_key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#foreign_key_clause}.
	 * @param ctx the parse tree
	 */
	void exitForeign_key_clause(PlSqlParserParser.Foreign_key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#references_clause}.
	 * @param ctx the parse tree
	 */
	void enterReferences_clause(PlSqlParserParser.References_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#references_clause}.
	 * @param ctx the parse tree
	 */
	void exitReferences_clause(PlSqlParserParser.References_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#on_delete_clause}.
	 * @param ctx the parse tree
	 */
	void enterOn_delete_clause(PlSqlParserParser.On_delete_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#on_delete_clause}.
	 * @param ctx the parse tree
	 */
	void exitOn_delete_clause(PlSqlParserParser.On_delete_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#unique_key_clause}.
	 * @param ctx the parse tree
	 */
	void enterUnique_key_clause(PlSqlParserParser.Unique_key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#unique_key_clause}.
	 * @param ctx the parse tree
	 */
	void exitUnique_key_clause(PlSqlParserParser.Unique_key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#primary_key_clause}.
	 * @param ctx the parse tree
	 */
	void enterPrimary_key_clause(PlSqlParserParser.Primary_key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#primary_key_clause}.
	 * @param ctx the parse tree
	 */
	void exitPrimary_key_clause(PlSqlParserParser.Primary_key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#anonymous_block}.
	 * @param ctx the parse tree
	 */
	void enterAnonymous_block(PlSqlParserParser.Anonymous_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#anonymous_block}.
	 * @param ctx the parse tree
	 */
	void exitAnonymous_block(PlSqlParserParser.Anonymous_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#invoker_rights_clause}.
	 * @param ctx the parse tree
	 */
	void enterInvoker_rights_clause(PlSqlParserParser.Invoker_rights_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#invoker_rights_clause}.
	 * @param ctx the parse tree
	 */
	void exitInvoker_rights_clause(PlSqlParserParser.Invoker_rights_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#call_spec}.
	 * @param ctx the parse tree
	 */
	void enterCall_spec(PlSqlParserParser.Call_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#call_spec}.
	 * @param ctx the parse tree
	 */
	void exitCall_spec(PlSqlParserParser.Call_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#java_spec}.
	 * @param ctx the parse tree
	 */
	void enterJava_spec(PlSqlParserParser.Java_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#java_spec}.
	 * @param ctx the parse tree
	 */
	void exitJava_spec(PlSqlParserParser.Java_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#c_spec}.
	 * @param ctx the parse tree
	 */
	void enterC_spec(PlSqlParserParser.C_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#c_spec}.
	 * @param ctx the parse tree
	 */
	void exitC_spec(PlSqlParserParser.C_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#c_agent_in_clause}.
	 * @param ctx the parse tree
	 */
	void enterC_agent_in_clause(PlSqlParserParser.C_agent_in_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#c_agent_in_clause}.
	 * @param ctx the parse tree
	 */
	void exitC_agent_in_clause(PlSqlParserParser.C_agent_in_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#c_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void enterC_parameters_clause(PlSqlParserParser.C_parameters_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#c_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void exitC_parameters_clause(PlSqlParserParser.C_parameters_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(PlSqlParserParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(PlSqlParserParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#default_value_part}.
	 * @param ctx the parse tree
	 */
	void enterDefault_value_part(PlSqlParserParser.Default_value_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#default_value_part}.
	 * @param ctx the parse tree
	 */
	void exitDefault_value_part(PlSqlParserParser.Default_value_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#seq_of_declare_specs}.
	 * @param ctx the parse tree
	 */
	void enterSeq_of_declare_specs(PlSqlParserParser.Seq_of_declare_specsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#seq_of_declare_specs}.
	 * @param ctx the parse tree
	 */
	void exitSeq_of_declare_specs(PlSqlParserParser.Seq_of_declare_specsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#declare_spec}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_spec(PlSqlParserParser.Declare_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#declare_spec}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_spec(PlSqlParserParser.Declare_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void enterVariable_declaration(PlSqlParserParser.Variable_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void exitVariable_declaration(PlSqlParserParser.Variable_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subtype_declaration}.
	 * @param ctx the parse tree
	 */
	void enterSubtype_declaration(PlSqlParserParser.Subtype_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subtype_declaration}.
	 * @param ctx the parse tree
	 */
	void exitSubtype_declaration(PlSqlParserParser.Subtype_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cursor_declaration}.
	 * @param ctx the parse tree
	 */
	void enterCursor_declaration(PlSqlParserParser.Cursor_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cursor_declaration}.
	 * @param ctx the parse tree
	 */
	void exitCursor_declaration(PlSqlParserParser.Cursor_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#parameter_spec}.
	 * @param ctx the parse tree
	 */
	void enterParameter_spec(PlSqlParserParser.Parameter_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#parameter_spec}.
	 * @param ctx the parse tree
	 */
	void exitParameter_spec(PlSqlParserParser.Parameter_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#exception_declaration}.
	 * @param ctx the parse tree
	 */
	void enterException_declaration(PlSqlParserParser.Exception_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#exception_declaration}.
	 * @param ctx the parse tree
	 */
	void exitException_declaration(PlSqlParserParser.Exception_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#pragma_declaration}.
	 * @param ctx the parse tree
	 */
	void enterPragma_declaration(PlSqlParserParser.Pragma_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#pragma_declaration}.
	 * @param ctx the parse tree
	 */
	void exitPragma_declaration(PlSqlParserParser.Pragma_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#record_type_def}.
	 * @param ctx the parse tree
	 */
	void enterRecord_type_def(PlSqlParserParser.Record_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#record_type_def}.
	 * @param ctx the parse tree
	 */
	void exitRecord_type_def(PlSqlParserParser.Record_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#field_spec}.
	 * @param ctx the parse tree
	 */
	void enterField_spec(PlSqlParserParser.Field_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#field_spec}.
	 * @param ctx the parse tree
	 */
	void exitField_spec(PlSqlParserParser.Field_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#ref_cursor_type_def}.
	 * @param ctx the parse tree
	 */
	void enterRef_cursor_type_def(PlSqlParserParser.Ref_cursor_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#ref_cursor_type_def}.
	 * @param ctx the parse tree
	 */
	void exitRef_cursor_type_def(PlSqlParserParser.Ref_cursor_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#type_declaration}.
	 * @param ctx the parse tree
	 */
	void enterType_declaration(PlSqlParserParser.Type_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#type_declaration}.
	 * @param ctx the parse tree
	 */
	void exitType_declaration(PlSqlParserParser.Type_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#table_type_def}.
	 * @param ctx the parse tree
	 */
	void enterTable_type_def(PlSqlParserParser.Table_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#table_type_def}.
	 * @param ctx the parse tree
	 */
	void exitTable_type_def(PlSqlParserParser.Table_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#table_indexed_by_part}.
	 * @param ctx the parse tree
	 */
	void enterTable_indexed_by_part(PlSqlParserParser.Table_indexed_by_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#table_indexed_by_part}.
	 * @param ctx the parse tree
	 */
	void exitTable_indexed_by_part(PlSqlParserParser.Table_indexed_by_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#varray_type_def}.
	 * @param ctx the parse tree
	 */
	void enterVarray_type_def(PlSqlParserParser.Varray_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#varray_type_def}.
	 * @param ctx the parse tree
	 */
	void exitVarray_type_def(PlSqlParserParser.Varray_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#seq_of_statements}.
	 * @param ctx the parse tree
	 */
	void enterSeq_of_statements(PlSqlParserParser.Seq_of_statementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#seq_of_statements}.
	 * @param ctx the parse tree
	 */
	void exitSeq_of_statements(PlSqlParserParser.Seq_of_statementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#label_declaration}.
	 * @param ctx the parse tree
	 */
	void enterLabel_declaration(PlSqlParserParser.Label_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#label_declaration}.
	 * @param ctx the parse tree
	 */
	void exitLabel_declaration(PlSqlParserParser.Label_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(PlSqlParserParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(PlSqlParserParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#swallow_to_semi}.
	 * @param ctx the parse tree
	 */
	void enterSwallow_to_semi(PlSqlParserParser.Swallow_to_semiContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#swallow_to_semi}.
	 * @param ctx the parse tree
	 */
	void exitSwallow_to_semi(PlSqlParserParser.Swallow_to_semiContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#assignment_statement}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_statement(PlSqlParserParser.Assignment_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#assignment_statement}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_statement(PlSqlParserParser.Assignment_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#continue_statement}.
	 * @param ctx the parse tree
	 */
	void enterContinue_statement(PlSqlParserParser.Continue_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#continue_statement}.
	 * @param ctx the parse tree
	 */
	void exitContinue_statement(PlSqlParserParser.Continue_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#exit_statement}.
	 * @param ctx the parse tree
	 */
	void enterExit_statement(PlSqlParserParser.Exit_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#exit_statement}.
	 * @param ctx the parse tree
	 */
	void exitExit_statement(PlSqlParserParser.Exit_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#goto_statement}.
	 * @param ctx the parse tree
	 */
	void enterGoto_statement(PlSqlParserParser.Goto_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#goto_statement}.
	 * @param ctx the parse tree
	 */
	void exitGoto_statement(PlSqlParserParser.Goto_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void enterIf_statement(PlSqlParserParser.If_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void exitIf_statement(PlSqlParserParser.If_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#elsif_part}.
	 * @param ctx the parse tree
	 */
	void enterElsif_part(PlSqlParserParser.Elsif_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#elsif_part}.
	 * @param ctx the parse tree
	 */
	void exitElsif_part(PlSqlParserParser.Elsif_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#else_part}.
	 * @param ctx the parse tree
	 */
	void enterElse_part(PlSqlParserParser.Else_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#else_part}.
	 * @param ctx the parse tree
	 */
	void exitElse_part(PlSqlParserParser.Else_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#loop_statement}.
	 * @param ctx the parse tree
	 */
	void enterLoop_statement(PlSqlParserParser.Loop_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#loop_statement}.
	 * @param ctx the parse tree
	 */
	void exitLoop_statement(PlSqlParserParser.Loop_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cursor_loop_param}.
	 * @param ctx the parse tree
	 */
	void enterCursor_loop_param(PlSqlParserParser.Cursor_loop_paramContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cursor_loop_param}.
	 * @param ctx the parse tree
	 */
	void exitCursor_loop_param(PlSqlParserParser.Cursor_loop_paramContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#forall_statement}.
	 * @param ctx the parse tree
	 */
	void enterForall_statement(PlSqlParserParser.Forall_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#forall_statement}.
	 * @param ctx the parse tree
	 */
	void exitForall_statement(PlSqlParserParser.Forall_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#bounds_clause}.
	 * @param ctx the parse tree
	 */
	void enterBounds_clause(PlSqlParserParser.Bounds_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#bounds_clause}.
	 * @param ctx the parse tree
	 */
	void exitBounds_clause(PlSqlParserParser.Bounds_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#between_bound}.
	 * @param ctx the parse tree
	 */
	void enterBetween_bound(PlSqlParserParser.Between_boundContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#between_bound}.
	 * @param ctx the parse tree
	 */
	void exitBetween_bound(PlSqlParserParser.Between_boundContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#lower_bound}.
	 * @param ctx the parse tree
	 */
	void enterLower_bound(PlSqlParserParser.Lower_boundContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#lower_bound}.
	 * @param ctx the parse tree
	 */
	void exitLower_bound(PlSqlParserParser.Lower_boundContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#upper_bound}.
	 * @param ctx the parse tree
	 */
	void enterUpper_bound(PlSqlParserParser.Upper_boundContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#upper_bound}.
	 * @param ctx the parse tree
	 */
	void exitUpper_bound(PlSqlParserParser.Upper_boundContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#null_statement}.
	 * @param ctx the parse tree
	 */
	void enterNull_statement(PlSqlParserParser.Null_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#null_statement}.
	 * @param ctx the parse tree
	 */
	void exitNull_statement(PlSqlParserParser.Null_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#raise_statement}.
	 * @param ctx the parse tree
	 */
	void enterRaise_statement(PlSqlParserParser.Raise_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#raise_statement}.
	 * @param ctx the parse tree
	 */
	void exitRaise_statement(PlSqlParserParser.Raise_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void enterReturn_statement(PlSqlParserParser.Return_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void exitReturn_statement(PlSqlParserParser.Return_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#function_call}.
	 * @param ctx the parse tree
	 */
	void enterFunction_call(PlSqlParserParser.Function_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#function_call}.
	 * @param ctx the parse tree
	 */
	void exitFunction_call(PlSqlParserParser.Function_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#procedure_call}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_call(PlSqlParserParser.Procedure_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#procedure_call}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_call(PlSqlParserParser.Procedure_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#pipe_row_statement}.
	 * @param ctx the parse tree
	 */
	void enterPipe_row_statement(PlSqlParserParser.Pipe_row_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#pipe_row_statement}.
	 * @param ctx the parse tree
	 */
	void exitPipe_row_statement(PlSqlParserParser.Pipe_row_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#body}.
	 * @param ctx the parse tree
	 */
	void enterBody(PlSqlParserParser.BodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#body}.
	 * @param ctx the parse tree
	 */
	void exitBody(PlSqlParserParser.BodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#exception_handler}.
	 * @param ctx the parse tree
	 */
	void enterException_handler(PlSqlParserParser.Exception_handlerContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#exception_handler}.
	 * @param ctx the parse tree
	 */
	void exitException_handler(PlSqlParserParser.Exception_handlerContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#trigger_block}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_block(PlSqlParserParser.Trigger_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#trigger_block}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_block(PlSqlParserParser.Trigger_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(PlSqlParserParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(PlSqlParserParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#sql_statement}.
	 * @param ctx the parse tree
	 */
	void enterSql_statement(PlSqlParserParser.Sql_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#sql_statement}.
	 * @param ctx the parse tree
	 */
	void exitSql_statement(PlSqlParserParser.Sql_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#execute_immediate}.
	 * @param ctx the parse tree
	 */
	void enterExecute_immediate(PlSqlParserParser.Execute_immediateContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#execute_immediate}.
	 * @param ctx the parse tree
	 */
	void exitExecute_immediate(PlSqlParserParser.Execute_immediateContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#dynamic_returning_clause}.
	 * @param ctx the parse tree
	 */
	void enterDynamic_returning_clause(PlSqlParserParser.Dynamic_returning_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#dynamic_returning_clause}.
	 * @param ctx the parse tree
	 */
	void exitDynamic_returning_clause(PlSqlParserParser.Dynamic_returning_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#data_manipulation_language_statements}.
	 * @param ctx the parse tree
	 */
	void enterData_manipulation_language_statements(PlSqlParserParser.Data_manipulation_language_statementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#data_manipulation_language_statements}.
	 * @param ctx the parse tree
	 */
	void exitData_manipulation_language_statements(PlSqlParserParser.Data_manipulation_language_statementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cursor_manipulation_statements}.
	 * @param ctx the parse tree
	 */
	void enterCursor_manipulation_statements(PlSqlParserParser.Cursor_manipulation_statementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cursor_manipulation_statements}.
	 * @param ctx the parse tree
	 */
	void exitCursor_manipulation_statements(PlSqlParserParser.Cursor_manipulation_statementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#close_statement}.
	 * @param ctx the parse tree
	 */
	void enterClose_statement(PlSqlParserParser.Close_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#close_statement}.
	 * @param ctx the parse tree
	 */
	void exitClose_statement(PlSqlParserParser.Close_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#open_statement}.
	 * @param ctx the parse tree
	 */
	void enterOpen_statement(PlSqlParserParser.Open_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#open_statement}.
	 * @param ctx the parse tree
	 */
	void exitOpen_statement(PlSqlParserParser.Open_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#fetch_statement}.
	 * @param ctx the parse tree
	 */
	void enterFetch_statement(PlSqlParserParser.Fetch_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#fetch_statement}.
	 * @param ctx the parse tree
	 */
	void exitFetch_statement(PlSqlParserParser.Fetch_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#open_for_statement}.
	 * @param ctx the parse tree
	 */
	void enterOpen_for_statement(PlSqlParserParser.Open_for_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#open_for_statement}.
	 * @param ctx the parse tree
	 */
	void exitOpen_for_statement(PlSqlParserParser.Open_for_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#transaction_control_statements}.
	 * @param ctx the parse tree
	 */
	void enterTransaction_control_statements(PlSqlParserParser.Transaction_control_statementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#transaction_control_statements}.
	 * @param ctx the parse tree
	 */
	void exitTransaction_control_statements(PlSqlParserParser.Transaction_control_statementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#set_transaction_command}.
	 * @param ctx the parse tree
	 */
	void enterSet_transaction_command(PlSqlParserParser.Set_transaction_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#set_transaction_command}.
	 * @param ctx the parse tree
	 */
	void exitSet_transaction_command(PlSqlParserParser.Set_transaction_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#set_constraint_command}.
	 * @param ctx the parse tree
	 */
	void enterSet_constraint_command(PlSqlParserParser.Set_constraint_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#set_constraint_command}.
	 * @param ctx the parse tree
	 */
	void exitSet_constraint_command(PlSqlParserParser.Set_constraint_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#commit_statement}.
	 * @param ctx the parse tree
	 */
	void enterCommit_statement(PlSqlParserParser.Commit_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#commit_statement}.
	 * @param ctx the parse tree
	 */
	void exitCommit_statement(PlSqlParserParser.Commit_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#write_clause}.
	 * @param ctx the parse tree
	 */
	void enterWrite_clause(PlSqlParserParser.Write_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#write_clause}.
	 * @param ctx the parse tree
	 */
	void exitWrite_clause(PlSqlParserParser.Write_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#rollback_statement}.
	 * @param ctx the parse tree
	 */
	void enterRollback_statement(PlSqlParserParser.Rollback_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#rollback_statement}.
	 * @param ctx the parse tree
	 */
	void exitRollback_statement(PlSqlParserParser.Rollback_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#savepoint_statement}.
	 * @param ctx the parse tree
	 */
	void enterSavepoint_statement(PlSqlParserParser.Savepoint_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#savepoint_statement}.
	 * @param ctx the parse tree
	 */
	void exitSavepoint_statement(PlSqlParserParser.Savepoint_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#explain_statement}.
	 * @param ctx the parse tree
	 */
	void enterExplain_statement(PlSqlParserParser.Explain_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#explain_statement}.
	 * @param ctx the parse tree
	 */
	void exitExplain_statement(PlSqlParserParser.Explain_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#select_only_statement}.
	 * @param ctx the parse tree
	 */
	void enterSelect_only_statement(PlSqlParserParser.Select_only_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#select_only_statement}.
	 * @param ctx the parse tree
	 */
	void exitSelect_only_statement(PlSqlParserParser.Select_only_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#select_statement}.
	 * @param ctx the parse tree
	 */
	void enterSelect_statement(PlSqlParserParser.Select_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#select_statement}.
	 * @param ctx the parse tree
	 */
	void exitSelect_statement(PlSqlParserParser.Select_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subquery_factoring_clause}.
	 * @param ctx the parse tree
	 */
	void enterSubquery_factoring_clause(PlSqlParserParser.Subquery_factoring_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subquery_factoring_clause}.
	 * @param ctx the parse tree
	 */
	void exitSubquery_factoring_clause(PlSqlParserParser.Subquery_factoring_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#factoring_element}.
	 * @param ctx the parse tree
	 */
	void enterFactoring_element(PlSqlParserParser.Factoring_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#factoring_element}.
	 * @param ctx the parse tree
	 */
	void exitFactoring_element(PlSqlParserParser.Factoring_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#search_clause}.
	 * @param ctx the parse tree
	 */
	void enterSearch_clause(PlSqlParserParser.Search_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#search_clause}.
	 * @param ctx the parse tree
	 */
	void exitSearch_clause(PlSqlParserParser.Search_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cycle_clause}.
	 * @param ctx the parse tree
	 */
	void enterCycle_clause(PlSqlParserParser.Cycle_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cycle_clause}.
	 * @param ctx the parse tree
	 */
	void exitCycle_clause(PlSqlParserParser.Cycle_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subquery}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(PlSqlParserParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subquery}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(PlSqlParserParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subquery_basic_elements}.
	 * @param ctx the parse tree
	 */
	void enterSubquery_basic_elements(PlSqlParserParser.Subquery_basic_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subquery_basic_elements}.
	 * @param ctx the parse tree
	 */
	void exitSubquery_basic_elements(PlSqlParserParser.Subquery_basic_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subquery_operation_part}.
	 * @param ctx the parse tree
	 */
	void enterSubquery_operation_part(PlSqlParserParser.Subquery_operation_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subquery_operation_part}.
	 * @param ctx the parse tree
	 */
	void exitSubquery_operation_part(PlSqlParserParser.Subquery_operation_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#query_block}.
	 * @param ctx the parse tree
	 */
	void enterQuery_block(PlSqlParserParser.Query_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#query_block}.
	 * @param ctx the parse tree
	 */
	void exitQuery_block(PlSqlParserParser.Query_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#selected_list}.
	 * @param ctx the parse tree
	 */
	void enterSelected_list(PlSqlParserParser.Selected_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#selected_list}.
	 * @param ctx the parse tree
	 */
	void exitSelected_list(PlSqlParserParser.Selected_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#from_clause}.
	 * @param ctx the parse tree
	 */
	void enterFrom_clause(PlSqlParserParser.From_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#from_clause}.
	 * @param ctx the parse tree
	 */
	void exitFrom_clause(PlSqlParserParser.From_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#select_list_elements}.
	 * @param ctx the parse tree
	 */
	void enterSelect_list_elements(PlSqlParserParser.Select_list_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#select_list_elements}.
	 * @param ctx the parse tree
	 */
	void exitSelect_list_elements(PlSqlParserParser.Select_list_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#table_ref_list}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_list(PlSqlParserParser.Table_ref_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#table_ref_list}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_list(PlSqlParserParser.Table_ref_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#table_ref}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref(PlSqlParserParser.Table_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#table_ref}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref(PlSqlParserParser.Table_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#table_ref_aux}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_aux(PlSqlParserParser.Table_ref_auxContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#table_ref_aux}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_aux(PlSqlParserParser.Table_ref_auxContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table_ref_aux_internal_one}
	 * labeled alternative in {@link PlSqlParserParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_aux_internal_one(PlSqlParserParser.Table_ref_aux_internal_oneContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table_ref_aux_internal_one}
	 * labeled alternative in {@link PlSqlParserParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_aux_internal_one(PlSqlParserParser.Table_ref_aux_internal_oneContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table_ref_aux_internal_two}
	 * labeled alternative in {@link PlSqlParserParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_aux_internal_two(PlSqlParserParser.Table_ref_aux_internal_twoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table_ref_aux_internal_two}
	 * labeled alternative in {@link PlSqlParserParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_aux_internal_two(PlSqlParserParser.Table_ref_aux_internal_twoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table_ref_aux_internal_three}
	 * labeled alternative in {@link PlSqlParserParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_aux_internal_three(PlSqlParserParser.Table_ref_aux_internal_threeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table_ref_aux_internal_three}
	 * labeled alternative in {@link PlSqlParserParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_aux_internal_three(PlSqlParserParser.Table_ref_aux_internal_threeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#join_clause}.
	 * @param ctx the parse tree
	 */
	void enterJoin_clause(PlSqlParserParser.Join_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#join_clause}.
	 * @param ctx the parse tree
	 */
	void exitJoin_clause(PlSqlParserParser.Join_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#join_on_part}.
	 * @param ctx the parse tree
	 */
	void enterJoin_on_part(PlSqlParserParser.Join_on_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#join_on_part}.
	 * @param ctx the parse tree
	 */
	void exitJoin_on_part(PlSqlParserParser.Join_on_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#join_using_part}.
	 * @param ctx the parse tree
	 */
	void enterJoin_using_part(PlSqlParserParser.Join_using_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#join_using_part}.
	 * @param ctx the parse tree
	 */
	void exitJoin_using_part(PlSqlParserParser.Join_using_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#outer_join_type}.
	 * @param ctx the parse tree
	 */
	void enterOuter_join_type(PlSqlParserParser.Outer_join_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#outer_join_type}.
	 * @param ctx the parse tree
	 */
	void exitOuter_join_type(PlSqlParserParser.Outer_join_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#query_partition_clause}.
	 * @param ctx the parse tree
	 */
	void enterQuery_partition_clause(PlSqlParserParser.Query_partition_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#query_partition_clause}.
	 * @param ctx the parse tree
	 */
	void exitQuery_partition_clause(PlSqlParserParser.Query_partition_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#flashback_query_clause}.
	 * @param ctx the parse tree
	 */
	void enterFlashback_query_clause(PlSqlParserParser.Flashback_query_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#flashback_query_clause}.
	 * @param ctx the parse tree
	 */
	void exitFlashback_query_clause(PlSqlParserParser.Flashback_query_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#pivot_clause}.
	 * @param ctx the parse tree
	 */
	void enterPivot_clause(PlSqlParserParser.Pivot_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#pivot_clause}.
	 * @param ctx the parse tree
	 */
	void exitPivot_clause(PlSqlParserParser.Pivot_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#pivot_element}.
	 * @param ctx the parse tree
	 */
	void enterPivot_element(PlSqlParserParser.Pivot_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#pivot_element}.
	 * @param ctx the parse tree
	 */
	void exitPivot_element(PlSqlParserParser.Pivot_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#pivot_for_clause}.
	 * @param ctx the parse tree
	 */
	void enterPivot_for_clause(PlSqlParserParser.Pivot_for_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#pivot_for_clause}.
	 * @param ctx the parse tree
	 */
	void exitPivot_for_clause(PlSqlParserParser.Pivot_for_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#pivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void enterPivot_in_clause(PlSqlParserParser.Pivot_in_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#pivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void exitPivot_in_clause(PlSqlParserParser.Pivot_in_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#pivot_in_clause_element}.
	 * @param ctx the parse tree
	 */
	void enterPivot_in_clause_element(PlSqlParserParser.Pivot_in_clause_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#pivot_in_clause_element}.
	 * @param ctx the parse tree
	 */
	void exitPivot_in_clause_element(PlSqlParserParser.Pivot_in_clause_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#pivot_in_clause_elements}.
	 * @param ctx the parse tree
	 */
	void enterPivot_in_clause_elements(PlSqlParserParser.Pivot_in_clause_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#pivot_in_clause_elements}.
	 * @param ctx the parse tree
	 */
	void exitPivot_in_clause_elements(PlSqlParserParser.Pivot_in_clause_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#unpivot_clause}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_clause(PlSqlParserParser.Unpivot_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#unpivot_clause}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_clause(PlSqlParserParser.Unpivot_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#unpivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_in_clause(PlSqlParserParser.Unpivot_in_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#unpivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_in_clause(PlSqlParserParser.Unpivot_in_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#unpivot_in_elements}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_in_elements(PlSqlParserParser.Unpivot_in_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#unpivot_in_elements}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_in_elements(PlSqlParserParser.Unpivot_in_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#hierarchical_query_clause}.
	 * @param ctx the parse tree
	 */
	void enterHierarchical_query_clause(PlSqlParserParser.Hierarchical_query_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#hierarchical_query_clause}.
	 * @param ctx the parse tree
	 */
	void exitHierarchical_query_clause(PlSqlParserParser.Hierarchical_query_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#start_part}.
	 * @param ctx the parse tree
	 */
	void enterStart_part(PlSqlParserParser.Start_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#start_part}.
	 * @param ctx the parse tree
	 */
	void exitStart_part(PlSqlParserParser.Start_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#group_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterGroup_by_clause(PlSqlParserParser.Group_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#group_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitGroup_by_clause(PlSqlParserParser.Group_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#group_by_elements}.
	 * @param ctx the parse tree
	 */
	void enterGroup_by_elements(PlSqlParserParser.Group_by_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#group_by_elements}.
	 * @param ctx the parse tree
	 */
	void exitGroup_by_elements(PlSqlParserParser.Group_by_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#rollup_cube_clause}.
	 * @param ctx the parse tree
	 */
	void enterRollup_cube_clause(PlSqlParserParser.Rollup_cube_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#rollup_cube_clause}.
	 * @param ctx the parse tree
	 */
	void exitRollup_cube_clause(PlSqlParserParser.Rollup_cube_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#grouping_sets_clause}.
	 * @param ctx the parse tree
	 */
	void enterGrouping_sets_clause(PlSqlParserParser.Grouping_sets_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#grouping_sets_clause}.
	 * @param ctx the parse tree
	 */
	void exitGrouping_sets_clause(PlSqlParserParser.Grouping_sets_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#grouping_sets_elements}.
	 * @param ctx the parse tree
	 */
	void enterGrouping_sets_elements(PlSqlParserParser.Grouping_sets_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#grouping_sets_elements}.
	 * @param ctx the parse tree
	 */
	void exitGrouping_sets_elements(PlSqlParserParser.Grouping_sets_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#having_clause}.
	 * @param ctx the parse tree
	 */
	void enterHaving_clause(PlSqlParserParser.Having_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#having_clause}.
	 * @param ctx the parse tree
	 */
	void exitHaving_clause(PlSqlParserParser.Having_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#model_clause}.
	 * @param ctx the parse tree
	 */
	void enterModel_clause(PlSqlParserParser.Model_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#model_clause}.
	 * @param ctx the parse tree
	 */
	void exitModel_clause(PlSqlParserParser.Model_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cell_reference_options}.
	 * @param ctx the parse tree
	 */
	void enterCell_reference_options(PlSqlParserParser.Cell_reference_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cell_reference_options}.
	 * @param ctx the parse tree
	 */
	void exitCell_reference_options(PlSqlParserParser.Cell_reference_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#return_rows_clause}.
	 * @param ctx the parse tree
	 */
	void enterReturn_rows_clause(PlSqlParserParser.Return_rows_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#return_rows_clause}.
	 * @param ctx the parse tree
	 */
	void exitReturn_rows_clause(PlSqlParserParser.Return_rows_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#reference_model}.
	 * @param ctx the parse tree
	 */
	void enterReference_model(PlSqlParserParser.Reference_modelContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#reference_model}.
	 * @param ctx the parse tree
	 */
	void exitReference_model(PlSqlParserParser.Reference_modelContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#main_model}.
	 * @param ctx the parse tree
	 */
	void enterMain_model(PlSqlParserParser.Main_modelContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#main_model}.
	 * @param ctx the parse tree
	 */
	void exitMain_model(PlSqlParserParser.Main_modelContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#model_column_clauses}.
	 * @param ctx the parse tree
	 */
	void enterModel_column_clauses(PlSqlParserParser.Model_column_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#model_column_clauses}.
	 * @param ctx the parse tree
	 */
	void exitModel_column_clauses(PlSqlParserParser.Model_column_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#model_column_partition_part}.
	 * @param ctx the parse tree
	 */
	void enterModel_column_partition_part(PlSqlParserParser.Model_column_partition_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#model_column_partition_part}.
	 * @param ctx the parse tree
	 */
	void exitModel_column_partition_part(PlSqlParserParser.Model_column_partition_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#model_column_list}.
	 * @param ctx the parse tree
	 */
	void enterModel_column_list(PlSqlParserParser.Model_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#model_column_list}.
	 * @param ctx the parse tree
	 */
	void exitModel_column_list(PlSqlParserParser.Model_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#model_column}.
	 * @param ctx the parse tree
	 */
	void enterModel_column(PlSqlParserParser.Model_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#model_column}.
	 * @param ctx the parse tree
	 */
	void exitModel_column(PlSqlParserParser.Model_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#model_rules_clause}.
	 * @param ctx the parse tree
	 */
	void enterModel_rules_clause(PlSqlParserParser.Model_rules_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#model_rules_clause}.
	 * @param ctx the parse tree
	 */
	void exitModel_rules_clause(PlSqlParserParser.Model_rules_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#model_rules_part}.
	 * @param ctx the parse tree
	 */
	void enterModel_rules_part(PlSqlParserParser.Model_rules_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#model_rules_part}.
	 * @param ctx the parse tree
	 */
	void exitModel_rules_part(PlSqlParserParser.Model_rules_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#model_rules_element}.
	 * @param ctx the parse tree
	 */
	void enterModel_rules_element(PlSqlParserParser.Model_rules_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#model_rules_element}.
	 * @param ctx the parse tree
	 */
	void exitModel_rules_element(PlSqlParserParser.Model_rules_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cell_assignment}.
	 * @param ctx the parse tree
	 */
	void enterCell_assignment(PlSqlParserParser.Cell_assignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cell_assignment}.
	 * @param ctx the parse tree
	 */
	void exitCell_assignment(PlSqlParserParser.Cell_assignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#model_iterate_clause}.
	 * @param ctx the parse tree
	 */
	void enterModel_iterate_clause(PlSqlParserParser.Model_iterate_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#model_iterate_clause}.
	 * @param ctx the parse tree
	 */
	void exitModel_iterate_clause(PlSqlParserParser.Model_iterate_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#until_part}.
	 * @param ctx the parse tree
	 */
	void enterUntil_part(PlSqlParserParser.Until_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#until_part}.
	 * @param ctx the parse tree
	 */
	void exitUntil_part(PlSqlParserParser.Until_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#order_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterOrder_by_clause(PlSqlParserParser.Order_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#order_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitOrder_by_clause(PlSqlParserParser.Order_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#order_by_elements}.
	 * @param ctx the parse tree
	 */
	void enterOrder_by_elements(PlSqlParserParser.Order_by_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#order_by_elements}.
	 * @param ctx the parse tree
	 */
	void exitOrder_by_elements(PlSqlParserParser.Order_by_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#offset_clause}.
	 * @param ctx the parse tree
	 */
	void enterOffset_clause(PlSqlParserParser.Offset_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#offset_clause}.
	 * @param ctx the parse tree
	 */
	void exitOffset_clause(PlSqlParserParser.Offset_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#fetch_clause}.
	 * @param ctx the parse tree
	 */
	void enterFetch_clause(PlSqlParserParser.Fetch_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#fetch_clause}.
	 * @param ctx the parse tree
	 */
	void exitFetch_clause(PlSqlParserParser.Fetch_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#for_update_clause}.
	 * @param ctx the parse tree
	 */
	void enterFor_update_clause(PlSqlParserParser.For_update_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#for_update_clause}.
	 * @param ctx the parse tree
	 */
	void exitFor_update_clause(PlSqlParserParser.For_update_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#for_update_of_part}.
	 * @param ctx the parse tree
	 */
	void enterFor_update_of_part(PlSqlParserParser.For_update_of_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#for_update_of_part}.
	 * @param ctx the parse tree
	 */
	void exitFor_update_of_part(PlSqlParserParser.For_update_of_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#for_update_options}.
	 * @param ctx the parse tree
	 */
	void enterFor_update_options(PlSqlParserParser.For_update_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#for_update_options}.
	 * @param ctx the parse tree
	 */
	void exitFor_update_options(PlSqlParserParser.For_update_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#update_statement}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_statement(PlSqlParserParser.Update_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#update_statement}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_statement(PlSqlParserParser.Update_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#update_set_clause}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_set_clause(PlSqlParserParser.Update_set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#update_set_clause}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_set_clause(PlSqlParserParser.Update_set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#column_based_update_set_clause}.
	 * @param ctx the parse tree
	 */
	void enterColumn_based_update_set_clause(PlSqlParserParser.Column_based_update_set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#column_based_update_set_clause}.
	 * @param ctx the parse tree
	 */
	void exitColumn_based_update_set_clause(PlSqlParserParser.Column_based_update_set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#delete_statement}.
	 * @param ctx the parse tree
	 */
	void enterDelete_statement(PlSqlParserParser.Delete_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#delete_statement}.
	 * @param ctx the parse tree
	 */
	void exitDelete_statement(PlSqlParserParser.Delete_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#insert_statement}.
	 * @param ctx the parse tree
	 */
	void enterInsert_statement(PlSqlParserParser.Insert_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#insert_statement}.
	 * @param ctx the parse tree
	 */
	void exitInsert_statement(PlSqlParserParser.Insert_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#single_table_insert}.
	 * @param ctx the parse tree
	 */
	void enterSingle_table_insert(PlSqlParserParser.Single_table_insertContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#single_table_insert}.
	 * @param ctx the parse tree
	 */
	void exitSingle_table_insert(PlSqlParserParser.Single_table_insertContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#multi_table_insert}.
	 * @param ctx the parse tree
	 */
	void enterMulti_table_insert(PlSqlParserParser.Multi_table_insertContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#multi_table_insert}.
	 * @param ctx the parse tree
	 */
	void exitMulti_table_insert(PlSqlParserParser.Multi_table_insertContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#multi_table_element}.
	 * @param ctx the parse tree
	 */
	void enterMulti_table_element(PlSqlParserParser.Multi_table_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#multi_table_element}.
	 * @param ctx the parse tree
	 */
	void exitMulti_table_element(PlSqlParserParser.Multi_table_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#conditional_insert_clause}.
	 * @param ctx the parse tree
	 */
	void enterConditional_insert_clause(PlSqlParserParser.Conditional_insert_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#conditional_insert_clause}.
	 * @param ctx the parse tree
	 */
	void exitConditional_insert_clause(PlSqlParserParser.Conditional_insert_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#conditional_insert_when_part}.
	 * @param ctx the parse tree
	 */
	void enterConditional_insert_when_part(PlSqlParserParser.Conditional_insert_when_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#conditional_insert_when_part}.
	 * @param ctx the parse tree
	 */
	void exitConditional_insert_when_part(PlSqlParserParser.Conditional_insert_when_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#conditional_insert_else_part}.
	 * @param ctx the parse tree
	 */
	void enterConditional_insert_else_part(PlSqlParserParser.Conditional_insert_else_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#conditional_insert_else_part}.
	 * @param ctx the parse tree
	 */
	void exitConditional_insert_else_part(PlSqlParserParser.Conditional_insert_else_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#insert_into_clause}.
	 * @param ctx the parse tree
	 */
	void enterInsert_into_clause(PlSqlParserParser.Insert_into_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#insert_into_clause}.
	 * @param ctx the parse tree
	 */
	void exitInsert_into_clause(PlSqlParserParser.Insert_into_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#values_clause}.
	 * @param ctx the parse tree
	 */
	void enterValues_clause(PlSqlParserParser.Values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#values_clause}.
	 * @param ctx the parse tree
	 */
	void exitValues_clause(PlSqlParserParser.Values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#merge_statement}.
	 * @param ctx the parse tree
	 */
	void enterMerge_statement(PlSqlParserParser.Merge_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#merge_statement}.
	 * @param ctx the parse tree
	 */
	void exitMerge_statement(PlSqlParserParser.Merge_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#merge_update_clause}.
	 * @param ctx the parse tree
	 */
	void enterMerge_update_clause(PlSqlParserParser.Merge_update_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#merge_update_clause}.
	 * @param ctx the parse tree
	 */
	void exitMerge_update_clause(PlSqlParserParser.Merge_update_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#merge_element}.
	 * @param ctx the parse tree
	 */
	void enterMerge_element(PlSqlParserParser.Merge_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#merge_element}.
	 * @param ctx the parse tree
	 */
	void exitMerge_element(PlSqlParserParser.Merge_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#merge_update_delete_part}.
	 * @param ctx the parse tree
	 */
	void enterMerge_update_delete_part(PlSqlParserParser.Merge_update_delete_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#merge_update_delete_part}.
	 * @param ctx the parse tree
	 */
	void exitMerge_update_delete_part(PlSqlParserParser.Merge_update_delete_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#merge_insert_clause}.
	 * @param ctx the parse tree
	 */
	void enterMerge_insert_clause(PlSqlParserParser.Merge_insert_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#merge_insert_clause}.
	 * @param ctx the parse tree
	 */
	void exitMerge_insert_clause(PlSqlParserParser.Merge_insert_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#selected_tableview}.
	 * @param ctx the parse tree
	 */
	void enterSelected_tableview(PlSqlParserParser.Selected_tableviewContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#selected_tableview}.
	 * @param ctx the parse tree
	 */
	void exitSelected_tableview(PlSqlParserParser.Selected_tableviewContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#lock_table_statement}.
	 * @param ctx the parse tree
	 */
	void enterLock_table_statement(PlSqlParserParser.Lock_table_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#lock_table_statement}.
	 * @param ctx the parse tree
	 */
	void exitLock_table_statement(PlSqlParserParser.Lock_table_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#wait_nowait_part}.
	 * @param ctx the parse tree
	 */
	void enterWait_nowait_part(PlSqlParserParser.Wait_nowait_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#wait_nowait_part}.
	 * @param ctx the parse tree
	 */
	void exitWait_nowait_part(PlSqlParserParser.Wait_nowait_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#lock_table_element}.
	 * @param ctx the parse tree
	 */
	void enterLock_table_element(PlSqlParserParser.Lock_table_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#lock_table_element}.
	 * @param ctx the parse tree
	 */
	void exitLock_table_element(PlSqlParserParser.Lock_table_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#lock_mode}.
	 * @param ctx the parse tree
	 */
	void enterLock_mode(PlSqlParserParser.Lock_modeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#lock_mode}.
	 * @param ctx the parse tree
	 */
	void exitLock_mode(PlSqlParserParser.Lock_modeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#general_table_ref}.
	 * @param ctx the parse tree
	 */
	void enterGeneral_table_ref(PlSqlParserParser.General_table_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#general_table_ref}.
	 * @param ctx the parse tree
	 */
	void exitGeneral_table_ref(PlSqlParserParser.General_table_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#static_returning_clause}.
	 * @param ctx the parse tree
	 */
	void enterStatic_returning_clause(PlSqlParserParser.Static_returning_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#static_returning_clause}.
	 * @param ctx the parse tree
	 */
	void exitStatic_returning_clause(PlSqlParserParser.Static_returning_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#error_logging_clause}.
	 * @param ctx the parse tree
	 */
	void enterError_logging_clause(PlSqlParserParser.Error_logging_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#error_logging_clause}.
	 * @param ctx the parse tree
	 */
	void exitError_logging_clause(PlSqlParserParser.Error_logging_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#error_logging_into_part}.
	 * @param ctx the parse tree
	 */
	void enterError_logging_into_part(PlSqlParserParser.Error_logging_into_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#error_logging_into_part}.
	 * @param ctx the parse tree
	 */
	void exitError_logging_into_part(PlSqlParserParser.Error_logging_into_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#error_logging_reject_part}.
	 * @param ctx the parse tree
	 */
	void enterError_logging_reject_part(PlSqlParserParser.Error_logging_reject_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#error_logging_reject_part}.
	 * @param ctx the parse tree
	 */
	void exitError_logging_reject_part(PlSqlParserParser.Error_logging_reject_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#dml_table_expression_clause}.
	 * @param ctx the parse tree
	 */
	void enterDml_table_expression_clause(PlSqlParserParser.Dml_table_expression_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#dml_table_expression_clause}.
	 * @param ctx the parse tree
	 */
	void exitDml_table_expression_clause(PlSqlParserParser.Dml_table_expression_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#table_collection_expression}.
	 * @param ctx the parse tree
	 */
	void enterTable_collection_expression(PlSqlParserParser.Table_collection_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#table_collection_expression}.
	 * @param ctx the parse tree
	 */
	void exitTable_collection_expression(PlSqlParserParser.Table_collection_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#subquery_restriction_clause}.
	 * @param ctx the parse tree
	 */
	void enterSubquery_restriction_clause(PlSqlParserParser.Subquery_restriction_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#subquery_restriction_clause}.
	 * @param ctx the parse tree
	 */
	void exitSubquery_restriction_clause(PlSqlParserParser.Subquery_restriction_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#sample_clause}.
	 * @param ctx the parse tree
	 */
	void enterSample_clause(PlSqlParserParser.Sample_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#sample_clause}.
	 * @param ctx the parse tree
	 */
	void exitSample_clause(PlSqlParserParser.Sample_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#seed_part}.
	 * @param ctx the parse tree
	 */
	void enterSeed_part(PlSqlParserParser.Seed_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#seed_part}.
	 * @param ctx the parse tree
	 */
	void exitSeed_part(PlSqlParserParser.Seed_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCondition(PlSqlParserParser.ConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCondition(PlSqlParserParser.ConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#expressions}.
	 * @param ctx the parse tree
	 */
	void enterExpressions(PlSqlParserParser.ExpressionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#expressions}.
	 * @param ctx the parse tree
	 */
	void exitExpressions(PlSqlParserParser.ExpressionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(PlSqlParserParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(PlSqlParserParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cursor_expression}.
	 * @param ctx the parse tree
	 */
	void enterCursor_expression(PlSqlParserParser.Cursor_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cursor_expression}.
	 * @param ctx the parse tree
	 */
	void exitCursor_expression(PlSqlParserParser.Cursor_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#logical_expression}.
	 * @param ctx the parse tree
	 */
	void enterLogical_expression(PlSqlParserParser.Logical_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#logical_expression}.
	 * @param ctx the parse tree
	 */
	void exitLogical_expression(PlSqlParserParser.Logical_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#unary_logical_expression}.
	 * @param ctx the parse tree
	 */
	void enterUnary_logical_expression(PlSqlParserParser.Unary_logical_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#unary_logical_expression}.
	 * @param ctx the parse tree
	 */
	void exitUnary_logical_expression(PlSqlParserParser.Unary_logical_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#logical_operation}.
	 * @param ctx the parse tree
	 */
	void enterLogical_operation(PlSqlParserParser.Logical_operationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#logical_operation}.
	 * @param ctx the parse tree
	 */
	void exitLogical_operation(PlSqlParserParser.Logical_operationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#multiset_expression}.
	 * @param ctx the parse tree
	 */
	void enterMultiset_expression(PlSqlParserParser.Multiset_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#multiset_expression}.
	 * @param ctx the parse tree
	 */
	void exitMultiset_expression(PlSqlParserParser.Multiset_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#relational_expression}.
	 * @param ctx the parse tree
	 */
	void enterRelational_expression(PlSqlParserParser.Relational_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#relational_expression}.
	 * @param ctx the parse tree
	 */
	void exitRelational_expression(PlSqlParserParser.Relational_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#compound_expression}.
	 * @param ctx the parse tree
	 */
	void enterCompound_expression(PlSqlParserParser.Compound_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#compound_expression}.
	 * @param ctx the parse tree
	 */
	void exitCompound_expression(PlSqlParserParser.Compound_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#relational_operator}.
	 * @param ctx the parse tree
	 */
	void enterRelational_operator(PlSqlParserParser.Relational_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#relational_operator}.
	 * @param ctx the parse tree
	 */
	void exitRelational_operator(PlSqlParserParser.Relational_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#in_elements}.
	 * @param ctx the parse tree
	 */
	void enterIn_elements(PlSqlParserParser.In_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#in_elements}.
	 * @param ctx the parse tree
	 */
	void exitIn_elements(PlSqlParserParser.In_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#between_elements}.
	 * @param ctx the parse tree
	 */
	void enterBetween_elements(PlSqlParserParser.Between_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#between_elements}.
	 * @param ctx the parse tree
	 */
	void exitBetween_elements(PlSqlParserParser.Between_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#concatenation}.
	 * @param ctx the parse tree
	 */
	void enterConcatenation(PlSqlParserParser.ConcatenationContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#concatenation}.
	 * @param ctx the parse tree
	 */
	void exitConcatenation(PlSqlParserParser.ConcatenationContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#interval_expression}.
	 * @param ctx the parse tree
	 */
	void enterInterval_expression(PlSqlParserParser.Interval_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#interval_expression}.
	 * @param ctx the parse tree
	 */
	void exitInterval_expression(PlSqlParserParser.Interval_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#model_expression}.
	 * @param ctx the parse tree
	 */
	void enterModel_expression(PlSqlParserParser.Model_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#model_expression}.
	 * @param ctx the parse tree
	 */
	void exitModel_expression(PlSqlParserParser.Model_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#model_expression_element}.
	 * @param ctx the parse tree
	 */
	void enterModel_expression_element(PlSqlParserParser.Model_expression_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#model_expression_element}.
	 * @param ctx the parse tree
	 */
	void exitModel_expression_element(PlSqlParserParser.Model_expression_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#single_column_for_loop}.
	 * @param ctx the parse tree
	 */
	void enterSingle_column_for_loop(PlSqlParserParser.Single_column_for_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#single_column_for_loop}.
	 * @param ctx the parse tree
	 */
	void exitSingle_column_for_loop(PlSqlParserParser.Single_column_for_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#multi_column_for_loop}.
	 * @param ctx the parse tree
	 */
	void enterMulti_column_for_loop(PlSqlParserParser.Multi_column_for_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#multi_column_for_loop}.
	 * @param ctx the parse tree
	 */
	void exitMulti_column_for_loop(PlSqlParserParser.Multi_column_for_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#unary_expression}.
	 * @param ctx the parse tree
	 */
	void enterUnary_expression(PlSqlParserParser.Unary_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#unary_expression}.
	 * @param ctx the parse tree
	 */
	void exitUnary_expression(PlSqlParserParser.Unary_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#case_statement}.
	 * @param ctx the parse tree
	 */
	void enterCase_statement(PlSqlParserParser.Case_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#case_statement}.
	 * @param ctx the parse tree
	 */
	void exitCase_statement(PlSqlParserParser.Case_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#simple_case_statement}.
	 * @param ctx the parse tree
	 */
	void enterSimple_case_statement(PlSqlParserParser.Simple_case_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#simple_case_statement}.
	 * @param ctx the parse tree
	 */
	void exitSimple_case_statement(PlSqlParserParser.Simple_case_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#simple_case_when_part}.
	 * @param ctx the parse tree
	 */
	void enterSimple_case_when_part(PlSqlParserParser.Simple_case_when_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#simple_case_when_part}.
	 * @param ctx the parse tree
	 */
	void exitSimple_case_when_part(PlSqlParserParser.Simple_case_when_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#searched_case_statement}.
	 * @param ctx the parse tree
	 */
	void enterSearched_case_statement(PlSqlParserParser.Searched_case_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#searched_case_statement}.
	 * @param ctx the parse tree
	 */
	void exitSearched_case_statement(PlSqlParserParser.Searched_case_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#searched_case_when_part}.
	 * @param ctx the parse tree
	 */
	void enterSearched_case_when_part(PlSqlParserParser.Searched_case_when_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#searched_case_when_part}.
	 * @param ctx the parse tree
	 */
	void exitSearched_case_when_part(PlSqlParserParser.Searched_case_when_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#case_else_part}.
	 * @param ctx the parse tree
	 */
	void enterCase_else_part(PlSqlParserParser.Case_else_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#case_else_part}.
	 * @param ctx the parse tree
	 */
	void exitCase_else_part(PlSqlParserParser.Case_else_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterAtom(PlSqlParserParser.AtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitAtom(PlSqlParserParser.AtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#quantified_expression}.
	 * @param ctx the parse tree
	 */
	void enterQuantified_expression(PlSqlParserParser.Quantified_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#quantified_expression}.
	 * @param ctx the parse tree
	 */
	void exitQuantified_expression(PlSqlParserParser.Quantified_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#string_function}.
	 * @param ctx the parse tree
	 */
	void enterString_function(PlSqlParserParser.String_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#string_function}.
	 * @param ctx the parse tree
	 */
	void exitString_function(PlSqlParserParser.String_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#standard_function}.
	 * @param ctx the parse tree
	 */
	void enterStandard_function(PlSqlParserParser.Standard_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#standard_function}.
	 * @param ctx the parse tree
	 */
	void exitStandard_function(PlSqlParserParser.Standard_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(PlSqlParserParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(PlSqlParserParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#numeric_function_wrapper}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_function_wrapper(PlSqlParserParser.Numeric_function_wrapperContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#numeric_function_wrapper}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_function_wrapper(PlSqlParserParser.Numeric_function_wrapperContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#numeric_function}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_function(PlSqlParserParser.Numeric_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#numeric_function}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_function(PlSqlParserParser.Numeric_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#other_function}.
	 * @param ctx the parse tree
	 */
	void enterOther_function(PlSqlParserParser.Other_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#other_function}.
	 * @param ctx the parse tree
	 */
	void exitOther_function(PlSqlParserParser.Other_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#over_clause_keyword}.
	 * @param ctx the parse tree
	 */
	void enterOver_clause_keyword(PlSqlParserParser.Over_clause_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#over_clause_keyword}.
	 * @param ctx the parse tree
	 */
	void exitOver_clause_keyword(PlSqlParserParser.Over_clause_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#within_or_over_clause_keyword}.
	 * @param ctx the parse tree
	 */
	void enterWithin_or_over_clause_keyword(PlSqlParserParser.Within_or_over_clause_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#within_or_over_clause_keyword}.
	 * @param ctx the parse tree
	 */
	void exitWithin_or_over_clause_keyword(PlSqlParserParser.Within_or_over_clause_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#standard_prediction_function_keyword}.
	 * @param ctx the parse tree
	 */
	void enterStandard_prediction_function_keyword(PlSqlParserParser.Standard_prediction_function_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#standard_prediction_function_keyword}.
	 * @param ctx the parse tree
	 */
	void exitStandard_prediction_function_keyword(PlSqlParserParser.Standard_prediction_function_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#over_clause}.
	 * @param ctx the parse tree
	 */
	void enterOver_clause(PlSqlParserParser.Over_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#over_clause}.
	 * @param ctx the parse tree
	 */
	void exitOver_clause(PlSqlParserParser.Over_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#windowing_clause}.
	 * @param ctx the parse tree
	 */
	void enterWindowing_clause(PlSqlParserParser.Windowing_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#windowing_clause}.
	 * @param ctx the parse tree
	 */
	void exitWindowing_clause(PlSqlParserParser.Windowing_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#windowing_type}.
	 * @param ctx the parse tree
	 */
	void enterWindowing_type(PlSqlParserParser.Windowing_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#windowing_type}.
	 * @param ctx the parse tree
	 */
	void exitWindowing_type(PlSqlParserParser.Windowing_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#windowing_elements}.
	 * @param ctx the parse tree
	 */
	void enterWindowing_elements(PlSqlParserParser.Windowing_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#windowing_elements}.
	 * @param ctx the parse tree
	 */
	void exitWindowing_elements(PlSqlParserParser.Windowing_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#using_clause}.
	 * @param ctx the parse tree
	 */
	void enterUsing_clause(PlSqlParserParser.Using_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#using_clause}.
	 * @param ctx the parse tree
	 */
	void exitUsing_clause(PlSqlParserParser.Using_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#using_element}.
	 * @param ctx the parse tree
	 */
	void enterUsing_element(PlSqlParserParser.Using_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#using_element}.
	 * @param ctx the parse tree
	 */
	void exitUsing_element(PlSqlParserParser.Using_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#collect_order_by_part}.
	 * @param ctx the parse tree
	 */
	void enterCollect_order_by_part(PlSqlParserParser.Collect_order_by_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#collect_order_by_part}.
	 * @param ctx the parse tree
	 */
	void exitCollect_order_by_part(PlSqlParserParser.Collect_order_by_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#within_or_over_part}.
	 * @param ctx the parse tree
	 */
	void enterWithin_or_over_part(PlSqlParserParser.Within_or_over_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#within_or_over_part}.
	 * @param ctx the parse tree
	 */
	void exitWithin_or_over_part(PlSqlParserParser.Within_or_over_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cost_matrix_clause}.
	 * @param ctx the parse tree
	 */
	void enterCost_matrix_clause(PlSqlParserParser.Cost_matrix_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cost_matrix_clause}.
	 * @param ctx the parse tree
	 */
	void exitCost_matrix_clause(PlSqlParserParser.Cost_matrix_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xml_passing_clause}.
	 * @param ctx the parse tree
	 */
	void enterXml_passing_clause(PlSqlParserParser.Xml_passing_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xml_passing_clause}.
	 * @param ctx the parse tree
	 */
	void exitXml_passing_clause(PlSqlParserParser.Xml_passing_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xml_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void enterXml_attributes_clause(PlSqlParserParser.Xml_attributes_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xml_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void exitXml_attributes_clause(PlSqlParserParser.Xml_attributes_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xml_namespaces_clause}.
	 * @param ctx the parse tree
	 */
	void enterXml_namespaces_clause(PlSqlParserParser.Xml_namespaces_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xml_namespaces_clause}.
	 * @param ctx the parse tree
	 */
	void exitXml_namespaces_clause(PlSqlParserParser.Xml_namespaces_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xml_table_column}.
	 * @param ctx the parse tree
	 */
	void enterXml_table_column(PlSqlParserParser.Xml_table_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xml_table_column}.
	 * @param ctx the parse tree
	 */
	void exitXml_table_column(PlSqlParserParser.Xml_table_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xml_general_default_part}.
	 * @param ctx the parse tree
	 */
	void enterXml_general_default_part(PlSqlParserParser.Xml_general_default_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xml_general_default_part}.
	 * @param ctx the parse tree
	 */
	void exitXml_general_default_part(PlSqlParserParser.Xml_general_default_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xml_multiuse_expression_element}.
	 * @param ctx the parse tree
	 */
	void enterXml_multiuse_expression_element(PlSqlParserParser.Xml_multiuse_expression_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xml_multiuse_expression_element}.
	 * @param ctx the parse tree
	 */
	void exitXml_multiuse_expression_element(PlSqlParserParser.Xml_multiuse_expression_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xmlroot_param_version_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlroot_param_version_part(PlSqlParserParser.Xmlroot_param_version_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xmlroot_param_version_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlroot_param_version_part(PlSqlParserParser.Xmlroot_param_version_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xmlroot_param_standalone_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlroot_param_standalone_part(PlSqlParserParser.Xmlroot_param_standalone_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xmlroot_param_standalone_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlroot_param_standalone_part(PlSqlParserParser.Xmlroot_param_standalone_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xmlserialize_param_enconding_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlserialize_param_enconding_part(PlSqlParserParser.Xmlserialize_param_enconding_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xmlserialize_param_enconding_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlserialize_param_enconding_part(PlSqlParserParser.Xmlserialize_param_enconding_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xmlserialize_param_version_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlserialize_param_version_part(PlSqlParserParser.Xmlserialize_param_version_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xmlserialize_param_version_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlserialize_param_version_part(PlSqlParserParser.Xmlserialize_param_version_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xmlserialize_param_ident_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlserialize_param_ident_part(PlSqlParserParser.Xmlserialize_param_ident_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xmlserialize_param_ident_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlserialize_param_ident_part(PlSqlParserParser.Xmlserialize_param_ident_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#sql_plus_command}.
	 * @param ctx the parse tree
	 */
	void enterSql_plus_command(PlSqlParserParser.Sql_plus_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#sql_plus_command}.
	 * @param ctx the parse tree
	 */
	void exitSql_plus_command(PlSqlParserParser.Sql_plus_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#whenever_command}.
	 * @param ctx the parse tree
	 */
	void enterWhenever_command(PlSqlParserParser.Whenever_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#whenever_command}.
	 * @param ctx the parse tree
	 */
	void exitWhenever_command(PlSqlParserParser.Whenever_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#set_command}.
	 * @param ctx the parse tree
	 */
	void enterSet_command(PlSqlParserParser.Set_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#set_command}.
	 * @param ctx the parse tree
	 */
	void exitSet_command(PlSqlParserParser.Set_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#partition_extension_clause}.
	 * @param ctx the parse tree
	 */
	void enterPartition_extension_clause(PlSqlParserParser.Partition_extension_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#partition_extension_clause}.
	 * @param ctx the parse tree
	 */
	void exitPartition_extension_clause(PlSqlParserParser.Partition_extension_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#column_alias}.
	 * @param ctx the parse tree
	 */
	void enterColumn_alias(PlSqlParserParser.Column_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#column_alias}.
	 * @param ctx the parse tree
	 */
	void exitColumn_alias(PlSqlParserParser.Column_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#table_alias}.
	 * @param ctx the parse tree
	 */
	void enterTable_alias(PlSqlParserParser.Table_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#table_alias}.
	 * @param ctx the parse tree
	 */
	void exitTable_alias(PlSqlParserParser.Table_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#where_clause}.
	 * @param ctx the parse tree
	 */
	void enterWhere_clause(PlSqlParserParser.Where_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#where_clause}.
	 * @param ctx the parse tree
	 */
	void exitWhere_clause(PlSqlParserParser.Where_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#into_clause}.
	 * @param ctx the parse tree
	 */
	void enterInto_clause(PlSqlParserParser.Into_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#into_clause}.
	 * @param ctx the parse tree
	 */
	void exitInto_clause(PlSqlParserParser.Into_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xml_column_name}.
	 * @param ctx the parse tree
	 */
	void enterXml_column_name(PlSqlParserParser.Xml_column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xml_column_name}.
	 * @param ctx the parse tree
	 */
	void exitXml_column_name(PlSqlParserParser.Xml_column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cost_class_name}.
	 * @param ctx the parse tree
	 */
	void enterCost_class_name(PlSqlParserParser.Cost_class_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cost_class_name}.
	 * @param ctx the parse tree
	 */
	void exitCost_class_name(PlSqlParserParser.Cost_class_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#attribute_name}.
	 * @param ctx the parse tree
	 */
	void enterAttribute_name(PlSqlParserParser.Attribute_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#attribute_name}.
	 * @param ctx the parse tree
	 */
	void exitAttribute_name(PlSqlParserParser.Attribute_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#savepoint_name}.
	 * @param ctx the parse tree
	 */
	void enterSavepoint_name(PlSqlParserParser.Savepoint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#savepoint_name}.
	 * @param ctx the parse tree
	 */
	void exitSavepoint_name(PlSqlParserParser.Savepoint_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#rollback_segment_name}.
	 * @param ctx the parse tree
	 */
	void enterRollback_segment_name(PlSqlParserParser.Rollback_segment_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#rollback_segment_name}.
	 * @param ctx the parse tree
	 */
	void exitRollback_segment_name(PlSqlParserParser.Rollback_segment_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#table_var_name}.
	 * @param ctx the parse tree
	 */
	void enterTable_var_name(PlSqlParserParser.Table_var_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#table_var_name}.
	 * @param ctx the parse tree
	 */
	void exitTable_var_name(PlSqlParserParser.Table_var_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#schema_name}.
	 * @param ctx the parse tree
	 */
	void enterSchema_name(PlSqlParserParser.Schema_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#schema_name}.
	 * @param ctx the parse tree
	 */
	void exitSchema_name(PlSqlParserParser.Schema_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#routine_name}.
	 * @param ctx the parse tree
	 */
	void enterRoutine_name(PlSqlParserParser.Routine_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#routine_name}.
	 * @param ctx the parse tree
	 */
	void exitRoutine_name(PlSqlParserParser.Routine_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#package_name}.
	 * @param ctx the parse tree
	 */
	void enterPackage_name(PlSqlParserParser.Package_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#package_name}.
	 * @param ctx the parse tree
	 */
	void exitPackage_name(PlSqlParserParser.Package_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#implementation_type_name}.
	 * @param ctx the parse tree
	 */
	void enterImplementation_type_name(PlSqlParserParser.Implementation_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#implementation_type_name}.
	 * @param ctx the parse tree
	 */
	void exitImplementation_type_name(PlSqlParserParser.Implementation_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#parameter_name}.
	 * @param ctx the parse tree
	 */
	void enterParameter_name(PlSqlParserParser.Parameter_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#parameter_name}.
	 * @param ctx the parse tree
	 */
	void exitParameter_name(PlSqlParserParser.Parameter_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#reference_model_name}.
	 * @param ctx the parse tree
	 */
	void enterReference_model_name(PlSqlParserParser.Reference_model_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#reference_model_name}.
	 * @param ctx the parse tree
	 */
	void exitReference_model_name(PlSqlParserParser.Reference_model_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#main_model_name}.
	 * @param ctx the parse tree
	 */
	void enterMain_model_name(PlSqlParserParser.Main_model_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#main_model_name}.
	 * @param ctx the parse tree
	 */
	void exitMain_model_name(PlSqlParserParser.Main_model_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#container_tableview_name}.
	 * @param ctx the parse tree
	 */
	void enterContainer_tableview_name(PlSqlParserParser.Container_tableview_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#container_tableview_name}.
	 * @param ctx the parse tree
	 */
	void exitContainer_tableview_name(PlSqlParserParser.Container_tableview_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#aggregate_function_name}.
	 * @param ctx the parse tree
	 */
	void enterAggregate_function_name(PlSqlParserParser.Aggregate_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#aggregate_function_name}.
	 * @param ctx the parse tree
	 */
	void exitAggregate_function_name(PlSqlParserParser.Aggregate_function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#query_name}.
	 * @param ctx the parse tree
	 */
	void enterQuery_name(PlSqlParserParser.Query_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#query_name}.
	 * @param ctx the parse tree
	 */
	void exitQuery_name(PlSqlParserParser.Query_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#grantee_name}.
	 * @param ctx the parse tree
	 */
	void enterGrantee_name(PlSqlParserParser.Grantee_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#grantee_name}.
	 * @param ctx the parse tree
	 */
	void exitGrantee_name(PlSqlParserParser.Grantee_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#role_name}.
	 * @param ctx the parse tree
	 */
	void enterRole_name(PlSqlParserParser.Role_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#role_name}.
	 * @param ctx the parse tree
	 */
	void exitRole_name(PlSqlParserParser.Role_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#constraint_name}.
	 * @param ctx the parse tree
	 */
	void enterConstraint_name(PlSqlParserParser.Constraint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#constraint_name}.
	 * @param ctx the parse tree
	 */
	void exitConstraint_name(PlSqlParserParser.Constraint_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#label_name}.
	 * @param ctx the parse tree
	 */
	void enterLabel_name(PlSqlParserParser.Label_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#label_name}.
	 * @param ctx the parse tree
	 */
	void exitLabel_name(PlSqlParserParser.Label_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#type_name}.
	 * @param ctx the parse tree
	 */
	void enterType_name(PlSqlParserParser.Type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#type_name}.
	 * @param ctx the parse tree
	 */
	void exitType_name(PlSqlParserParser.Type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#sequence_name}.
	 * @param ctx the parse tree
	 */
	void enterSequence_name(PlSqlParserParser.Sequence_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#sequence_name}.
	 * @param ctx the parse tree
	 */
	void exitSequence_name(PlSqlParserParser.Sequence_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#exception_name}.
	 * @param ctx the parse tree
	 */
	void enterException_name(PlSqlParserParser.Exception_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#exception_name}.
	 * @param ctx the parse tree
	 */
	void exitException_name(PlSqlParserParser.Exception_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#function_name}.
	 * @param ctx the parse tree
	 */
	void enterFunction_name(PlSqlParserParser.Function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#function_name}.
	 * @param ctx the parse tree
	 */
	void exitFunction_name(PlSqlParserParser.Function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#procedure_name}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_name(PlSqlParserParser.Procedure_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#procedure_name}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_name(PlSqlParserParser.Procedure_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#trigger_name}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_name(PlSqlParserParser.Trigger_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#trigger_name}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_name(PlSqlParserParser.Trigger_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#variable_name}.
	 * @param ctx the parse tree
	 */
	void enterVariable_name(PlSqlParserParser.Variable_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#variable_name}.
	 * @param ctx the parse tree
	 */
	void exitVariable_name(PlSqlParserParser.Variable_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#index_name}.
	 * @param ctx the parse tree
	 */
	void enterIndex_name(PlSqlParserParser.Index_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#index_name}.
	 * @param ctx the parse tree
	 */
	void exitIndex_name(PlSqlParserParser.Index_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#cursor_name}.
	 * @param ctx the parse tree
	 */
	void enterCursor_name(PlSqlParserParser.Cursor_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#cursor_name}.
	 * @param ctx the parse tree
	 */
	void exitCursor_name(PlSqlParserParser.Cursor_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#record_name}.
	 * @param ctx the parse tree
	 */
	void enterRecord_name(PlSqlParserParser.Record_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#record_name}.
	 * @param ctx the parse tree
	 */
	void exitRecord_name(PlSqlParserParser.Record_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#collection_name}.
	 * @param ctx the parse tree
	 */
	void enterCollection_name(PlSqlParserParser.Collection_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#collection_name}.
	 * @param ctx the parse tree
	 */
	void exitCollection_name(PlSqlParserParser.Collection_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#link_name}.
	 * @param ctx the parse tree
	 */
	void enterLink_name(PlSqlParserParser.Link_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#link_name}.
	 * @param ctx the parse tree
	 */
	void exitLink_name(PlSqlParserParser.Link_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#column_name}.
	 * @param ctx the parse tree
	 */
	void enterColumn_name(PlSqlParserParser.Column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#column_name}.
	 * @param ctx the parse tree
	 */
	void exitColumn_name(PlSqlParserParser.Column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#tableview_name}.
	 * @param ctx the parse tree
	 */
	void enterTableview_name(PlSqlParserParser.Tableview_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#tableview_name}.
	 * @param ctx the parse tree
	 */
	void exitTableview_name(PlSqlParserParser.Tableview_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#xmltable}.
	 * @param ctx the parse tree
	 */
	void enterXmltable(PlSqlParserParser.XmltableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#xmltable}.
	 * @param ctx the parse tree
	 */
	void exitXmltable(PlSqlParserParser.XmltableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#char_set_name}.
	 * @param ctx the parse tree
	 */
	void enterChar_set_name(PlSqlParserParser.Char_set_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#char_set_name}.
	 * @param ctx the parse tree
	 */
	void exitChar_set_name(PlSqlParserParser.Char_set_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#synonym_name}.
	 * @param ctx the parse tree
	 */
	void enterSynonym_name(PlSqlParserParser.Synonym_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#synonym_name}.
	 * @param ctx the parse tree
	 */
	void exitSynonym_name(PlSqlParserParser.Synonym_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#schema_object_name}.
	 * @param ctx the parse tree
	 */
	void enterSchema_object_name(PlSqlParserParser.Schema_object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#schema_object_name}.
	 * @param ctx the parse tree
	 */
	void exitSchema_object_name(PlSqlParserParser.Schema_object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#dir_object_name}.
	 * @param ctx the parse tree
	 */
	void enterDir_object_name(PlSqlParserParser.Dir_object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#dir_object_name}.
	 * @param ctx the parse tree
	 */
	void exitDir_object_name(PlSqlParserParser.Dir_object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#user_object_name}.
	 * @param ctx the parse tree
	 */
	void enterUser_object_name(PlSqlParserParser.User_object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#user_object_name}.
	 * @param ctx the parse tree
	 */
	void exitUser_object_name(PlSqlParserParser.User_object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#grant_object_name}.
	 * @param ctx the parse tree
	 */
	void enterGrant_object_name(PlSqlParserParser.Grant_object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#grant_object_name}.
	 * @param ctx the parse tree
	 */
	void exitGrant_object_name(PlSqlParserParser.Grant_object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#column_list}.
	 * @param ctx the parse tree
	 */
	void enterColumn_list(PlSqlParserParser.Column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#column_list}.
	 * @param ctx the parse tree
	 */
	void exitColumn_list(PlSqlParserParser.Column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#paren_column_list}.
	 * @param ctx the parse tree
	 */
	void enterParen_column_list(PlSqlParserParser.Paren_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#paren_column_list}.
	 * @param ctx the parse tree
	 */
	void exitParen_column_list(PlSqlParserParser.Paren_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#keep_clause}.
	 * @param ctx the parse tree
	 */
	void enterKeep_clause(PlSqlParserParser.Keep_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#keep_clause}.
	 * @param ctx the parse tree
	 */
	void exitKeep_clause(PlSqlParserParser.Keep_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#function_argument}.
	 * @param ctx the parse tree
	 */
	void enterFunction_argument(PlSqlParserParser.Function_argumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#function_argument}.
	 * @param ctx the parse tree
	 */
	void exitFunction_argument(PlSqlParserParser.Function_argumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#function_argument_analytic}.
	 * @param ctx the parse tree
	 */
	void enterFunction_argument_analytic(PlSqlParserParser.Function_argument_analyticContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#function_argument_analytic}.
	 * @param ctx the parse tree
	 */
	void exitFunction_argument_analytic(PlSqlParserParser.Function_argument_analyticContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#function_argument_modeling}.
	 * @param ctx the parse tree
	 */
	void enterFunction_argument_modeling(PlSqlParserParser.Function_argument_modelingContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#function_argument_modeling}.
	 * @param ctx the parse tree
	 */
	void exitFunction_argument_modeling(PlSqlParserParser.Function_argument_modelingContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#respect_or_ignore_nulls}.
	 * @param ctx the parse tree
	 */
	void enterRespect_or_ignore_nulls(PlSqlParserParser.Respect_or_ignore_nullsContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#respect_or_ignore_nulls}.
	 * @param ctx the parse tree
	 */
	void exitRespect_or_ignore_nulls(PlSqlParserParser.Respect_or_ignore_nullsContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(PlSqlParserParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(PlSqlParserParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#type_spec}.
	 * @param ctx the parse tree
	 */
	void enterType_spec(PlSqlParserParser.Type_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#type_spec}.
	 * @param ctx the parse tree
	 */
	void exitType_spec(PlSqlParserParser.Type_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#datatype}.
	 * @param ctx the parse tree
	 */
	void enterDatatype(PlSqlParserParser.DatatypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#datatype}.
	 * @param ctx the parse tree
	 */
	void exitDatatype(PlSqlParserParser.DatatypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#precision_part}.
	 * @param ctx the parse tree
	 */
	void enterPrecision_part(PlSqlParserParser.Precision_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#precision_part}.
	 * @param ctx the parse tree
	 */
	void exitPrecision_part(PlSqlParserParser.Precision_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#native_datatype_element}.
	 * @param ctx the parse tree
	 */
	void enterNative_datatype_element(PlSqlParserParser.Native_datatype_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#native_datatype_element}.
	 * @param ctx the parse tree
	 */
	void exitNative_datatype_element(PlSqlParserParser.Native_datatype_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#bind_variable}.
	 * @param ctx the parse tree
	 */
	void enterBind_variable(PlSqlParserParser.Bind_variableContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#bind_variable}.
	 * @param ctx the parse tree
	 */
	void exitBind_variable(PlSqlParserParser.Bind_variableContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#general_element}.
	 * @param ctx the parse tree
	 */
	void enterGeneral_element(PlSqlParserParser.General_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#general_element}.
	 * @param ctx the parse tree
	 */
	void exitGeneral_element(PlSqlParserParser.General_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#general_element_part}.
	 * @param ctx the parse tree
	 */
	void enterGeneral_element_part(PlSqlParserParser.General_element_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#general_element_part}.
	 * @param ctx the parse tree
	 */
	void exitGeneral_element_part(PlSqlParserParser.General_element_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#table_element}.
	 * @param ctx the parse tree
	 */
	void enterTable_element(PlSqlParserParser.Table_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#table_element}.
	 * @param ctx the parse tree
	 */
	void exitTable_element(PlSqlParserParser.Table_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#object_privilege}.
	 * @param ctx the parse tree
	 */
	void enterObject_privilege(PlSqlParserParser.Object_privilegeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#object_privilege}.
	 * @param ctx the parse tree
	 */
	void exitObject_privilege(PlSqlParserParser.Object_privilegeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#system_privilege}.
	 * @param ctx the parse tree
	 */
	void enterSystem_privilege(PlSqlParserParser.System_privilegeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#system_privilege}.
	 * @param ctx the parse tree
	 */
	void exitSystem_privilege(PlSqlParserParser.System_privilegeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterConstant(PlSqlParserParser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitConstant(PlSqlParserParser.ConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#numeric}.
	 * @param ctx the parse tree
	 */
	void enterNumeric(PlSqlParserParser.NumericContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#numeric}.
	 * @param ctx the parse tree
	 */
	void exitNumeric(PlSqlParserParser.NumericContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#numeric_negative}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_negative(PlSqlParserParser.Numeric_negativeContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#numeric_negative}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_negative(PlSqlParserParser.Numeric_negativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#quoted_string}.
	 * @param ctx the parse tree
	 */
	void enterQuoted_string(PlSqlParserParser.Quoted_stringContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#quoted_string}.
	 * @param ctx the parse tree
	 */
	void exitQuoted_string(PlSqlParserParser.Quoted_stringContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(PlSqlParserParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(PlSqlParserParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#id_expression}.
	 * @param ctx the parse tree
	 */
	void enterId_expression(PlSqlParserParser.Id_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#id_expression}.
	 * @param ctx the parse tree
	 */
	void exitId_expression(PlSqlParserParser.Id_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#outer_join_sign}.
	 * @param ctx the parse tree
	 */
	void enterOuter_join_sign(PlSqlParserParser.Outer_join_signContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#outer_join_sign}.
	 * @param ctx the parse tree
	 */
	void exitOuter_join_sign(PlSqlParserParser.Outer_join_signContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#regular_id}.
	 * @param ctx the parse tree
	 */
	void enterRegular_id(PlSqlParserParser.Regular_idContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#regular_id}.
	 * @param ctx the parse tree
	 */
	void exitRegular_id(PlSqlParserParser.Regular_idContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#non_reserved_keywords_in_12c}.
	 * @param ctx the parse tree
	 */
	void enterNon_reserved_keywords_in_12c(PlSqlParserParser.Non_reserved_keywords_in_12cContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#non_reserved_keywords_in_12c}.
	 * @param ctx the parse tree
	 */
	void exitNon_reserved_keywords_in_12c(PlSqlParserParser.Non_reserved_keywords_in_12cContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#non_reserved_keywords_pre12c}.
	 * @param ctx the parse tree
	 */
	void enterNon_reserved_keywords_pre12c(PlSqlParserParser.Non_reserved_keywords_pre12cContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#non_reserved_keywords_pre12c}.
	 * @param ctx the parse tree
	 */
	void exitNon_reserved_keywords_pre12c(PlSqlParserParser.Non_reserved_keywords_pre12cContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#string_function_name}.
	 * @param ctx the parse tree
	 */
	void enterString_function_name(PlSqlParserParser.String_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#string_function_name}.
	 * @param ctx the parse tree
	 */
	void exitString_function_name(PlSqlParserParser.String_function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link PlSqlParserParser#numeric_function_name}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_function_name(PlSqlParserParser.Numeric_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link PlSqlParserParser#numeric_function_name}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_function_name(PlSqlParserParser.Numeric_function_nameContext ctx);
}