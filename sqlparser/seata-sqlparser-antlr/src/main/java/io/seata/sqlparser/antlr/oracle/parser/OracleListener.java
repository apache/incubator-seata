// Generated from Oracle.g4 by ANTLR 4.8
package io.seata.sqlparser.antlr.oracle.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link OracleParser}.
 */
public interface OracleListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link OracleParser#sql_script}.
	 * @param ctx the parse tree
	 */
	void enterSql_script(OracleParser.Sql_scriptContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sql_script}.
	 * @param ctx the parse tree
	 */
	void exitSql_script(OracleParser.Sql_scriptContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unit_statement}.
	 * @param ctx the parse tree
	 */
	void enterUnit_statement(OracleParser.Unit_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unit_statement}.
	 * @param ctx the parse tree
	 */
	void exitUnit_statement(OracleParser.Unit_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_function}.
	 * @param ctx the parse tree
	 */
	void enterDrop_function(OracleParser.Drop_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_function}.
	 * @param ctx the parse tree
	 */
	void exitDrop_function(OracleParser.Drop_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_function}.
	 * @param ctx the parse tree
	 */
	void enterAlter_function(OracleParser.Alter_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_function}.
	 * @param ctx the parse tree
	 */
	void exitAlter_function(OracleParser.Alter_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_function_body}.
	 * @param ctx the parse tree
	 */
	void enterCreate_function_body(OracleParser.Create_function_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_function_body}.
	 * @param ctx the parse tree
	 */
	void exitCreate_function_body(OracleParser.Create_function_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#parallel_enable_clause}.
	 * @param ctx the parse tree
	 */
	void enterParallel_enable_clause(OracleParser.Parallel_enable_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#parallel_enable_clause}.
	 * @param ctx the parse tree
	 */
	void exitParallel_enable_clause(OracleParser.Parallel_enable_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partition_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterPartition_by_clause(OracleParser.Partition_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partition_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitPartition_by_clause(OracleParser.Partition_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#result_cache_clause}.
	 * @param ctx the parse tree
	 */
	void enterResult_cache_clause(OracleParser.Result_cache_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#result_cache_clause}.
	 * @param ctx the parse tree
	 */
	void exitResult_cache_clause(OracleParser.Result_cache_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#relies_on_part}.
	 * @param ctx the parse tree
	 */
	void enterRelies_on_part(OracleParser.Relies_on_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#relies_on_part}.
	 * @param ctx the parse tree
	 */
	void exitRelies_on_part(OracleParser.Relies_on_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#streaming_clause}.
	 * @param ctx the parse tree
	 */
	void enterStreaming_clause(OracleParser.Streaming_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#streaming_clause}.
	 * @param ctx the parse tree
	 */
	void exitStreaming_clause(OracleParser.Streaming_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_package}.
	 * @param ctx the parse tree
	 */
	void enterDrop_package(OracleParser.Drop_packageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_package}.
	 * @param ctx the parse tree
	 */
	void exitDrop_package(OracleParser.Drop_packageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_package}.
	 * @param ctx the parse tree
	 */
	void enterAlter_package(OracleParser.Alter_packageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_package}.
	 * @param ctx the parse tree
	 */
	void exitAlter_package(OracleParser.Alter_packageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_package}.
	 * @param ctx the parse tree
	 */
	void enterCreate_package(OracleParser.Create_packageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_package}.
	 * @param ctx the parse tree
	 */
	void exitCreate_package(OracleParser.Create_packageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_package_body}.
	 * @param ctx the parse tree
	 */
	void enterCreate_package_body(OracleParser.Create_package_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_package_body}.
	 * @param ctx the parse tree
	 */
	void exitCreate_package_body(OracleParser.Create_package_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#package_obj_spec}.
	 * @param ctx the parse tree
	 */
	void enterPackage_obj_spec(OracleParser.Package_obj_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#package_obj_spec}.
	 * @param ctx the parse tree
	 */
	void exitPackage_obj_spec(OracleParser.Package_obj_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#procedure_spec}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_spec(OracleParser.Procedure_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#procedure_spec}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_spec(OracleParser.Procedure_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_spec}.
	 * @param ctx the parse tree
	 */
	void enterFunction_spec(OracleParser.Function_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_spec}.
	 * @param ctx the parse tree
	 */
	void exitFunction_spec(OracleParser.Function_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#package_obj_body}.
	 * @param ctx the parse tree
	 */
	void enterPackage_obj_body(OracleParser.Package_obj_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#package_obj_body}.
	 * @param ctx the parse tree
	 */
	void exitPackage_obj_body(OracleParser.Package_obj_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_procedure}.
	 * @param ctx the parse tree
	 */
	void enterDrop_procedure(OracleParser.Drop_procedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_procedure}.
	 * @param ctx the parse tree
	 */
	void exitDrop_procedure(OracleParser.Drop_procedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_procedure}.
	 * @param ctx the parse tree
	 */
	void enterAlter_procedure(OracleParser.Alter_procedureContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_procedure}.
	 * @param ctx the parse tree
	 */
	void exitAlter_procedure(OracleParser.Alter_procedureContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_body}.
	 * @param ctx the parse tree
	 */
	void enterFunction_body(OracleParser.Function_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_body}.
	 * @param ctx the parse tree
	 */
	void exitFunction_body(OracleParser.Function_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#procedure_body}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_body(OracleParser.Procedure_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#procedure_body}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_body(OracleParser.Procedure_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_procedure_body}.
	 * @param ctx the parse tree
	 */
	void enterCreate_procedure_body(OracleParser.Create_procedure_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_procedure_body}.
	 * @param ctx the parse tree
	 */
	void exitCreate_procedure_body(OracleParser.Create_procedure_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_trigger}.
	 * @param ctx the parse tree
	 */
	void enterDrop_trigger(OracleParser.Drop_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_trigger}.
	 * @param ctx the parse tree
	 */
	void exitDrop_trigger(OracleParser.Drop_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_trigger}.
	 * @param ctx the parse tree
	 */
	void enterAlter_trigger(OracleParser.Alter_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_trigger}.
	 * @param ctx the parse tree
	 */
	void exitAlter_trigger(OracleParser.Alter_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_trigger}.
	 * @param ctx the parse tree
	 */
	void enterCreate_trigger(OracleParser.Create_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_trigger}.
	 * @param ctx the parse tree
	 */
	void exitCreate_trigger(OracleParser.Create_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#trigger_follows_clause}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_follows_clause(OracleParser.Trigger_follows_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#trigger_follows_clause}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_follows_clause(OracleParser.Trigger_follows_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#trigger_when_clause}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_when_clause(OracleParser.Trigger_when_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#trigger_when_clause}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_when_clause(OracleParser.Trigger_when_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#simple_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void enterSimple_dml_trigger(OracleParser.Simple_dml_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#simple_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void exitSimple_dml_trigger(OracleParser.Simple_dml_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#for_each_row}.
	 * @param ctx the parse tree
	 */
	void enterFor_each_row(OracleParser.For_each_rowContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#for_each_row}.
	 * @param ctx the parse tree
	 */
	void exitFor_each_row(OracleParser.For_each_rowContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#compound_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void enterCompound_dml_trigger(OracleParser.Compound_dml_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#compound_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void exitCompound_dml_trigger(OracleParser.Compound_dml_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#non_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void enterNon_dml_trigger(OracleParser.Non_dml_triggerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#non_dml_trigger}.
	 * @param ctx the parse tree
	 */
	void exitNon_dml_trigger(OracleParser.Non_dml_triggerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#trigger_body}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_body(OracleParser.Trigger_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#trigger_body}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_body(OracleParser.Trigger_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#routine_clause}.
	 * @param ctx the parse tree
	 */
	void enterRoutine_clause(OracleParser.Routine_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#routine_clause}.
	 * @param ctx the parse tree
	 */
	void exitRoutine_clause(OracleParser.Routine_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#compound_trigger_block}.
	 * @param ctx the parse tree
	 */
	void enterCompound_trigger_block(OracleParser.Compound_trigger_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#compound_trigger_block}.
	 * @param ctx the parse tree
	 */
	void exitCompound_trigger_block(OracleParser.Compound_trigger_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#timing_point_section}.
	 * @param ctx the parse tree
	 */
	void enterTiming_point_section(OracleParser.Timing_point_sectionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#timing_point_section}.
	 * @param ctx the parse tree
	 */
	void exitTiming_point_section(OracleParser.Timing_point_sectionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#non_dml_event}.
	 * @param ctx the parse tree
	 */
	void enterNon_dml_event(OracleParser.Non_dml_eventContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#non_dml_event}.
	 * @param ctx the parse tree
	 */
	void exitNon_dml_event(OracleParser.Non_dml_eventContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dml_event_clause}.
	 * @param ctx the parse tree
	 */
	void enterDml_event_clause(OracleParser.Dml_event_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dml_event_clause}.
	 * @param ctx the parse tree
	 */
	void exitDml_event_clause(OracleParser.Dml_event_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dml_event_element}.
	 * @param ctx the parse tree
	 */
	void enterDml_event_element(OracleParser.Dml_event_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dml_event_element}.
	 * @param ctx the parse tree
	 */
	void exitDml_event_element(OracleParser.Dml_event_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dml_event_nested_clause}.
	 * @param ctx the parse tree
	 */
	void enterDml_event_nested_clause(OracleParser.Dml_event_nested_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dml_event_nested_clause}.
	 * @param ctx the parse tree
	 */
	void exitDml_event_nested_clause(OracleParser.Dml_event_nested_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#referencing_clause}.
	 * @param ctx the parse tree
	 */
	void enterReferencing_clause(OracleParser.Referencing_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#referencing_clause}.
	 * @param ctx the parse tree
	 */
	void exitReferencing_clause(OracleParser.Referencing_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#referencing_element}.
	 * @param ctx the parse tree
	 */
	void enterReferencing_element(OracleParser.Referencing_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#referencing_element}.
	 * @param ctx the parse tree
	 */
	void exitReferencing_element(OracleParser.Referencing_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_type}.
	 * @param ctx the parse tree
	 */
	void enterDrop_type(OracleParser.Drop_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_type}.
	 * @param ctx the parse tree
	 */
	void exitDrop_type(OracleParser.Drop_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_type}.
	 * @param ctx the parse tree
	 */
	void enterAlter_type(OracleParser.Alter_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_type}.
	 * @param ctx the parse tree
	 */
	void exitAlter_type(OracleParser.Alter_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#compile_type_clause}.
	 * @param ctx the parse tree
	 */
	void enterCompile_type_clause(OracleParser.Compile_type_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#compile_type_clause}.
	 * @param ctx the parse tree
	 */
	void exitCompile_type_clause(OracleParser.Compile_type_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#replace_type_clause}.
	 * @param ctx the parse tree
	 */
	void enterReplace_type_clause(OracleParser.Replace_type_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#replace_type_clause}.
	 * @param ctx the parse tree
	 */
	void exitReplace_type_clause(OracleParser.Replace_type_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_method_spec}.
	 * @param ctx the parse tree
	 */
	void enterAlter_method_spec(OracleParser.Alter_method_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_method_spec}.
	 * @param ctx the parse tree
	 */
	void exitAlter_method_spec(OracleParser.Alter_method_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_method_element}.
	 * @param ctx the parse tree
	 */
	void enterAlter_method_element(OracleParser.Alter_method_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_method_element}.
	 * @param ctx the parse tree
	 */
	void exitAlter_method_element(OracleParser.Alter_method_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_attribute_definition}.
	 * @param ctx the parse tree
	 */
	void enterAlter_attribute_definition(OracleParser.Alter_attribute_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_attribute_definition}.
	 * @param ctx the parse tree
	 */
	void exitAlter_attribute_definition(OracleParser.Alter_attribute_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#attribute_definition}.
	 * @param ctx the parse tree
	 */
	void enterAttribute_definition(OracleParser.Attribute_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#attribute_definition}.
	 * @param ctx the parse tree
	 */
	void exitAttribute_definition(OracleParser.Attribute_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_collection_clauses}.
	 * @param ctx the parse tree
	 */
	void enterAlter_collection_clauses(OracleParser.Alter_collection_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_collection_clauses}.
	 * @param ctx the parse tree
	 */
	void exitAlter_collection_clauses(OracleParser.Alter_collection_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dependent_handling_clause}.
	 * @param ctx the parse tree
	 */
	void enterDependent_handling_clause(OracleParser.Dependent_handling_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dependent_handling_clause}.
	 * @param ctx the parse tree
	 */
	void exitDependent_handling_clause(OracleParser.Dependent_handling_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dependent_exceptions_part}.
	 * @param ctx the parse tree
	 */
	void enterDependent_exceptions_part(OracleParser.Dependent_exceptions_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dependent_exceptions_part}.
	 * @param ctx the parse tree
	 */
	void exitDependent_exceptions_part(OracleParser.Dependent_exceptions_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_type}.
	 * @param ctx the parse tree
	 */
	void enterCreate_type(OracleParser.Create_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_type}.
	 * @param ctx the parse tree
	 */
	void exitCreate_type(OracleParser.Create_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_definition}.
	 * @param ctx the parse tree
	 */
	void enterType_definition(OracleParser.Type_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_definition}.
	 * @param ctx the parse tree
	 */
	void exitType_definition(OracleParser.Type_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_type_def}.
	 * @param ctx the parse tree
	 */
	void enterObject_type_def(OracleParser.Object_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_type_def}.
	 * @param ctx the parse tree
	 */
	void exitObject_type_def(OracleParser.Object_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_as_part}.
	 * @param ctx the parse tree
	 */
	void enterObject_as_part(OracleParser.Object_as_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_as_part}.
	 * @param ctx the parse tree
	 */
	void exitObject_as_part(OracleParser.Object_as_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_under_part}.
	 * @param ctx the parse tree
	 */
	void enterObject_under_part(OracleParser.Object_under_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_under_part}.
	 * @param ctx the parse tree
	 */
	void exitObject_under_part(OracleParser.Object_under_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#nested_table_type_def}.
	 * @param ctx the parse tree
	 */
	void enterNested_table_type_def(OracleParser.Nested_table_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#nested_table_type_def}.
	 * @param ctx the parse tree
	 */
	void exitNested_table_type_def(OracleParser.Nested_table_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sqlj_object_type}.
	 * @param ctx the parse tree
	 */
	void enterSqlj_object_type(OracleParser.Sqlj_object_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sqlj_object_type}.
	 * @param ctx the parse tree
	 */
	void exitSqlj_object_type(OracleParser.Sqlj_object_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_body}.
	 * @param ctx the parse tree
	 */
	void enterType_body(OracleParser.Type_bodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_body}.
	 * @param ctx the parse tree
	 */
	void exitType_body(OracleParser.Type_bodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_body_elements}.
	 * @param ctx the parse tree
	 */
	void enterType_body_elements(OracleParser.Type_body_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_body_elements}.
	 * @param ctx the parse tree
	 */
	void exitType_body_elements(OracleParser.Type_body_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#map_order_func_declaration}.
	 * @param ctx the parse tree
	 */
	void enterMap_order_func_declaration(OracleParser.Map_order_func_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#map_order_func_declaration}.
	 * @param ctx the parse tree
	 */
	void exitMap_order_func_declaration(OracleParser.Map_order_func_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subprog_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void enterSubprog_decl_in_type(OracleParser.Subprog_decl_in_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subprog_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void exitSubprog_decl_in_type(OracleParser.Subprog_decl_in_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#proc_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void enterProc_decl_in_type(OracleParser.Proc_decl_in_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#proc_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void exitProc_decl_in_type(OracleParser.Proc_decl_in_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#func_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void enterFunc_decl_in_type(OracleParser.Func_decl_in_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#func_decl_in_type}.
	 * @param ctx the parse tree
	 */
	void exitFunc_decl_in_type(OracleParser.Func_decl_in_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#constructor_declaration}.
	 * @param ctx the parse tree
	 */
	void enterConstructor_declaration(OracleParser.Constructor_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#constructor_declaration}.
	 * @param ctx the parse tree
	 */
	void exitConstructor_declaration(OracleParser.Constructor_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modifier_clause}.
	 * @param ctx the parse tree
	 */
	void enterModifier_clause(OracleParser.Modifier_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modifier_clause}.
	 * @param ctx the parse tree
	 */
	void exitModifier_clause(OracleParser.Modifier_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_member_spec}.
	 * @param ctx the parse tree
	 */
	void enterObject_member_spec(OracleParser.Object_member_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_member_spec}.
	 * @param ctx the parse tree
	 */
	void exitObject_member_spec(OracleParser.Object_member_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sqlj_object_type_attr}.
	 * @param ctx the parse tree
	 */
	void enterSqlj_object_type_attr(OracleParser.Sqlj_object_type_attrContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sqlj_object_type_attr}.
	 * @param ctx the parse tree
	 */
	void exitSqlj_object_type_attr(OracleParser.Sqlj_object_type_attrContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#element_spec}.
	 * @param ctx the parse tree
	 */
	void enterElement_spec(OracleParser.Element_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#element_spec}.
	 * @param ctx the parse tree
	 */
	void exitElement_spec(OracleParser.Element_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#element_spec_options}.
	 * @param ctx the parse tree
	 */
	void enterElement_spec_options(OracleParser.Element_spec_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#element_spec_options}.
	 * @param ctx the parse tree
	 */
	void exitElement_spec_options(OracleParser.Element_spec_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subprogram_spec}.
	 * @param ctx the parse tree
	 */
	void enterSubprogram_spec(OracleParser.Subprogram_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subprogram_spec}.
	 * @param ctx the parse tree
	 */
	void exitSubprogram_spec(OracleParser.Subprogram_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#overriding_subprogram_spec}.
	 * @param ctx the parse tree
	 */
	void enterOverriding_subprogram_spec(OracleParser.Overriding_subprogram_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#overriding_subprogram_spec}.
	 * @param ctx the parse tree
	 */
	void exitOverriding_subprogram_spec(OracleParser.Overriding_subprogram_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#overriding_function_spec}.
	 * @param ctx the parse tree
	 */
	void enterOverriding_function_spec(OracleParser.Overriding_function_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#overriding_function_spec}.
	 * @param ctx the parse tree
	 */
	void exitOverriding_function_spec(OracleParser.Overriding_function_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_procedure_spec}.
	 * @param ctx the parse tree
	 */
	void enterType_procedure_spec(OracleParser.Type_procedure_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_procedure_spec}.
	 * @param ctx the parse tree
	 */
	void exitType_procedure_spec(OracleParser.Type_procedure_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_function_spec}.
	 * @param ctx the parse tree
	 */
	void enterType_function_spec(OracleParser.Type_function_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_function_spec}.
	 * @param ctx the parse tree
	 */
	void exitType_function_spec(OracleParser.Type_function_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#constructor_spec}.
	 * @param ctx the parse tree
	 */
	void enterConstructor_spec(OracleParser.Constructor_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#constructor_spec}.
	 * @param ctx the parse tree
	 */
	void exitConstructor_spec(OracleParser.Constructor_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#map_order_function_spec}.
	 * @param ctx the parse tree
	 */
	void enterMap_order_function_spec(OracleParser.Map_order_function_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#map_order_function_spec}.
	 * @param ctx the parse tree
	 */
	void exitMap_order_function_spec(OracleParser.Map_order_function_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pragma_clause}.
	 * @param ctx the parse tree
	 */
	void enterPragma_clause(OracleParser.Pragma_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pragma_clause}.
	 * @param ctx the parse tree
	 */
	void exitPragma_clause(OracleParser.Pragma_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pragma_elements}.
	 * @param ctx the parse tree
	 */
	void enterPragma_elements(OracleParser.Pragma_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pragma_elements}.
	 * @param ctx the parse tree
	 */
	void exitPragma_elements(OracleParser.Pragma_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_elements_parameter}.
	 * @param ctx the parse tree
	 */
	void enterType_elements_parameter(OracleParser.Type_elements_parameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_elements_parameter}.
	 * @param ctx the parse tree
	 */
	void exitType_elements_parameter(OracleParser.Type_elements_parameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_sequence}.
	 * @param ctx the parse tree
	 */
	void enterDrop_sequence(OracleParser.Drop_sequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_sequence}.
	 * @param ctx the parse tree
	 */
	void exitDrop_sequence(OracleParser.Drop_sequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_sequence}.
	 * @param ctx the parse tree
	 */
	void enterAlter_sequence(OracleParser.Alter_sequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_sequence}.
	 * @param ctx the parse tree
	 */
	void exitAlter_sequence(OracleParser.Alter_sequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_session}.
	 * @param ctx the parse tree
	 */
	void enterAlter_session(OracleParser.Alter_sessionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_session}.
	 * @param ctx the parse tree
	 */
	void exitAlter_session(OracleParser.Alter_sessionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_session_set_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_session_set_clause(OracleParser.Alter_session_set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_session_set_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_session_set_clause(OracleParser.Alter_session_set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_sequence}.
	 * @param ctx the parse tree
	 */
	void enterCreate_sequence(OracleParser.Create_sequenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_sequence}.
	 * @param ctx the parse tree
	 */
	void exitCreate_sequence(OracleParser.Create_sequenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sequence_spec}.
	 * @param ctx the parse tree
	 */
	void enterSequence_spec(OracleParser.Sequence_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sequence_spec}.
	 * @param ctx the parse tree
	 */
	void exitSequence_spec(OracleParser.Sequence_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sequence_start_clause}.
	 * @param ctx the parse tree
	 */
	void enterSequence_start_clause(OracleParser.Sequence_start_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sequence_start_clause}.
	 * @param ctx the parse tree
	 */
	void exitSequence_start_clause(OracleParser.Sequence_start_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_index}.
	 * @param ctx the parse tree
	 */
	void enterCreate_index(OracleParser.Create_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_index}.
	 * @param ctx the parse tree
	 */
	void exitCreate_index(OracleParser.Create_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cluster_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterCluster_index_clause(OracleParser.Cluster_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cluster_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitCluster_index_clause(OracleParser.Cluster_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cluster_name}.
	 * @param ctx the parse tree
	 */
	void enterCluster_name(OracleParser.Cluster_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cluster_name}.
	 * @param ctx the parse tree
	 */
	void exitCluster_name(OracleParser.Cluster_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterTable_index_clause(OracleParser.Table_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitTable_index_clause(OracleParser.Table_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#bitmap_join_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterBitmap_join_index_clause(OracleParser.Bitmap_join_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#bitmap_join_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitBitmap_join_index_clause(OracleParser.Bitmap_join_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_expr}.
	 * @param ctx the parse tree
	 */
	void enterIndex_expr(OracleParser.Index_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_expr}.
	 * @param ctx the parse tree
	 */
	void exitIndex_expr(OracleParser.Index_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_properties}.
	 * @param ctx the parse tree
	 */
	void enterIndex_properties(OracleParser.Index_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_properties}.
	 * @param ctx the parse tree
	 */
	void exitIndex_properties(OracleParser.Index_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#domain_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterDomain_index_clause(OracleParser.Domain_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#domain_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitDomain_index_clause(OracleParser.Domain_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#local_domain_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterLocal_domain_index_clause(OracleParser.Local_domain_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#local_domain_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitLocal_domain_index_clause(OracleParser.Local_domain_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlindex_clause}.
	 * @param ctx the parse tree
	 */
	void enterXmlindex_clause(OracleParser.Xmlindex_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlindex_clause}.
	 * @param ctx the parse tree
	 */
	void exitXmlindex_clause(OracleParser.Xmlindex_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#local_xmlindex_clause}.
	 * @param ctx the parse tree
	 */
	void enterLocal_xmlindex_clause(OracleParser.Local_xmlindex_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#local_xmlindex_clause}.
	 * @param ctx the parse tree
	 */
	void exitLocal_xmlindex_clause(OracleParser.Local_xmlindex_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#global_partitioned_index}.
	 * @param ctx the parse tree
	 */
	void enterGlobal_partitioned_index(OracleParser.Global_partitioned_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#global_partitioned_index}.
	 * @param ctx the parse tree
	 */
	void exitGlobal_partitioned_index(OracleParser.Global_partitioned_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_partitioning_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_partitioning_clause(OracleParser.Index_partitioning_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_partitioning_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_partitioning_clause(OracleParser.Index_partitioning_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#local_partitioned_index}.
	 * @param ctx the parse tree
	 */
	void enterLocal_partitioned_index(OracleParser.Local_partitioned_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#local_partitioned_index}.
	 * @param ctx the parse tree
	 */
	void exitLocal_partitioned_index(OracleParser.Local_partitioned_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_range_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterOn_range_partitioned_table(OracleParser.On_range_partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_range_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitOn_range_partitioned_table(OracleParser.On_range_partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_list_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterOn_list_partitioned_table(OracleParser.On_list_partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_list_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitOn_list_partitioned_table(OracleParser.On_list_partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterPartitioned_table(OracleParser.Partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitPartitioned_table(OracleParser.Partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_hash_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterOn_hash_partitioned_table(OracleParser.On_hash_partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_hash_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitOn_hash_partitioned_table(OracleParser.On_hash_partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_hash_partitioned_clause}.
	 * @param ctx the parse tree
	 */
	void enterOn_hash_partitioned_clause(OracleParser.On_hash_partitioned_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_hash_partitioned_clause}.
	 * @param ctx the parse tree
	 */
	void exitOn_hash_partitioned_clause(OracleParser.On_hash_partitioned_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_comp_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void enterOn_comp_partitioned_table(OracleParser.On_comp_partitioned_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_comp_partitioned_table}.
	 * @param ctx the parse tree
	 */
	void exitOn_comp_partitioned_table(OracleParser.On_comp_partitioned_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_comp_partitioned_clause}.
	 * @param ctx the parse tree
	 */
	void enterOn_comp_partitioned_clause(OracleParser.On_comp_partitioned_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_comp_partitioned_clause}.
	 * @param ctx the parse tree
	 */
	void exitOn_comp_partitioned_clause(OracleParser.On_comp_partitioned_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_subpartition_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_subpartition_clause(OracleParser.Index_subpartition_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_subpartition_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_subpartition_clause(OracleParser.Index_subpartition_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_subpartition_subclause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_subpartition_subclause(OracleParser.Index_subpartition_subclauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_subpartition_subclause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_subpartition_subclause(OracleParser.Index_subpartition_subclauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#odci_parameters}.
	 * @param ctx the parse tree
	 */
	void enterOdci_parameters(OracleParser.Odci_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#odci_parameters}.
	 * @param ctx the parse tree
	 */
	void exitOdci_parameters(OracleParser.Odci_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#indextype}.
	 * @param ctx the parse tree
	 */
	void enterIndextype(OracleParser.IndextypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#indextype}.
	 * @param ctx the parse tree
	 */
	void exitIndextype(OracleParser.IndextypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_index}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index(OracleParser.Alter_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_index}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index(OracleParser.Alter_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_index_ops_set1}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_ops_set1(OracleParser.Alter_index_ops_set1Context ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_index_ops_set1}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_ops_set1(OracleParser.Alter_index_ops_set1Context ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_index_ops_set2}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_ops_set2(OracleParser.Alter_index_ops_set2Context ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_index_ops_set2}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_ops_set2(OracleParser.Alter_index_ops_set2Context ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#visible_or_invisible}.
	 * @param ctx the parse tree
	 */
	void enterVisible_or_invisible(OracleParser.Visible_or_invisibleContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#visible_or_invisible}.
	 * @param ctx the parse tree
	 */
	void exitVisible_or_invisible(OracleParser.Visible_or_invisibleContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#monitoring_nomonitoring}.
	 * @param ctx the parse tree
	 */
	void enterMonitoring_nomonitoring(OracleParser.Monitoring_nomonitoringContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#monitoring_nomonitoring}.
	 * @param ctx the parse tree
	 */
	void exitMonitoring_nomonitoring(OracleParser.Monitoring_nomonitoringContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rebuild_clause}.
	 * @param ctx the parse tree
	 */
	void enterRebuild_clause(OracleParser.Rebuild_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rebuild_clause}.
	 * @param ctx the parse tree
	 */
	void exitRebuild_clause(OracleParser.Rebuild_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_index_partitioning}.
	 * @param ctx the parse tree
	 */
	void enterAlter_index_partitioning(OracleParser.Alter_index_partitioningContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_index_partitioning}.
	 * @param ctx the parse tree
	 */
	void exitAlter_index_partitioning(OracleParser.Alter_index_partitioningContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_index_default_attrs}.
	 * @param ctx the parse tree
	 */
	void enterModify_index_default_attrs(OracleParser.Modify_index_default_attrsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_index_default_attrs}.
	 * @param ctx the parse tree
	 */
	void exitModify_index_default_attrs(OracleParser.Modify_index_default_attrsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_hash_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterAdd_hash_index_partition(OracleParser.Add_hash_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_hash_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitAdd_hash_index_partition(OracleParser.Add_hash_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#coalesce_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterCoalesce_index_partition(OracleParser.Coalesce_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#coalesce_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitCoalesce_index_partition(OracleParser.Coalesce_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterModify_index_partition(OracleParser.Modify_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitModify_index_partition(OracleParser.Modify_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_index_partitions_ops}.
	 * @param ctx the parse tree
	 */
	void enterModify_index_partitions_ops(OracleParser.Modify_index_partitions_opsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_index_partitions_ops}.
	 * @param ctx the parse tree
	 */
	void exitModify_index_partitions_ops(OracleParser.Modify_index_partitions_opsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rename_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterRename_index_partition(OracleParser.Rename_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rename_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitRename_index_partition(OracleParser.Rename_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterDrop_index_partition(OracleParser.Drop_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitDrop_index_partition(OracleParser.Drop_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#split_index_partition}.
	 * @param ctx the parse tree
	 */
	void enterSplit_index_partition(OracleParser.Split_index_partitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#split_index_partition}.
	 * @param ctx the parse tree
	 */
	void exitSplit_index_partition(OracleParser.Split_index_partitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_partition_description}.
	 * @param ctx the parse tree
	 */
	void enterIndex_partition_description(OracleParser.Index_partition_descriptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_partition_description}.
	 * @param ctx the parse tree
	 */
	void exitIndex_partition_description(OracleParser.Index_partition_descriptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_index_subpartition}.
	 * @param ctx the parse tree
	 */
	void enterModify_index_subpartition(OracleParser.Modify_index_subpartitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_index_subpartition}.
	 * @param ctx the parse tree
	 */
	void exitModify_index_subpartition(OracleParser.Modify_index_subpartitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partition_name_old}.
	 * @param ctx the parse tree
	 */
	void enterPartition_name_old(OracleParser.Partition_name_oldContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partition_name_old}.
	 * @param ctx the parse tree
	 */
	void exitPartition_name_old(OracleParser.Partition_name_oldContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#new_partition_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_partition_name(OracleParser.New_partition_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#new_partition_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_partition_name(OracleParser.New_partition_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#new_index_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_index_name(OracleParser.New_index_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#new_index_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_index_name(OracleParser.New_index_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_user}.
	 * @param ctx the parse tree
	 */
	void enterCreate_user(OracleParser.Create_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_user}.
	 * @param ctx the parse tree
	 */
	void exitCreate_user(OracleParser.Create_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_user}.
	 * @param ctx the parse tree
	 */
	void enterAlter_user(OracleParser.Alter_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_user}.
	 * @param ctx the parse tree
	 */
	void exitAlter_user(OracleParser.Alter_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_identified_by}.
	 * @param ctx the parse tree
	 */
	void enterAlter_identified_by(OracleParser.Alter_identified_byContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_identified_by}.
	 * @param ctx the parse tree
	 */
	void exitAlter_identified_by(OracleParser.Alter_identified_byContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#identified_by}.
	 * @param ctx the parse tree
	 */
	void enterIdentified_by(OracleParser.Identified_byContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#identified_by}.
	 * @param ctx the parse tree
	 */
	void exitIdentified_by(OracleParser.Identified_byContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#identified_other_clause}.
	 * @param ctx the parse tree
	 */
	void enterIdentified_other_clause(OracleParser.Identified_other_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#identified_other_clause}.
	 * @param ctx the parse tree
	 */
	void exitIdentified_other_clause(OracleParser.Identified_other_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#user_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void enterUser_tablespace_clause(OracleParser.User_tablespace_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#user_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void exitUser_tablespace_clause(OracleParser.User_tablespace_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#quota_clause}.
	 * @param ctx the parse tree
	 */
	void enterQuota_clause(OracleParser.Quota_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#quota_clause}.
	 * @param ctx the parse tree
	 */
	void exitQuota_clause(OracleParser.Quota_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#profile_clause}.
	 * @param ctx the parse tree
	 */
	void enterProfile_clause(OracleParser.Profile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#profile_clause}.
	 * @param ctx the parse tree
	 */
	void exitProfile_clause(OracleParser.Profile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#role_clause}.
	 * @param ctx the parse tree
	 */
	void enterRole_clause(OracleParser.Role_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#role_clause}.
	 * @param ctx the parse tree
	 */
	void exitRole_clause(OracleParser.Role_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#user_default_role_clause}.
	 * @param ctx the parse tree
	 */
	void enterUser_default_role_clause(OracleParser.User_default_role_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#user_default_role_clause}.
	 * @param ctx the parse tree
	 */
	void exitUser_default_role_clause(OracleParser.User_default_role_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#password_expire_clause}.
	 * @param ctx the parse tree
	 */
	void enterPassword_expire_clause(OracleParser.Password_expire_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#password_expire_clause}.
	 * @param ctx the parse tree
	 */
	void exitPassword_expire_clause(OracleParser.Password_expire_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#user_lock_clause}.
	 * @param ctx the parse tree
	 */
	void enterUser_lock_clause(OracleParser.User_lock_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#user_lock_clause}.
	 * @param ctx the parse tree
	 */
	void exitUser_lock_clause(OracleParser.User_lock_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#user_editions_clause}.
	 * @param ctx the parse tree
	 */
	void enterUser_editions_clause(OracleParser.User_editions_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#user_editions_clause}.
	 * @param ctx the parse tree
	 */
	void exitUser_editions_clause(OracleParser.User_editions_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_user_editions_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_user_editions_clause(OracleParser.Alter_user_editions_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_user_editions_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_user_editions_clause(OracleParser.Alter_user_editions_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#proxy_clause}.
	 * @param ctx the parse tree
	 */
	void enterProxy_clause(OracleParser.Proxy_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#proxy_clause}.
	 * @param ctx the parse tree
	 */
	void exitProxy_clause(OracleParser.Proxy_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#container_names}.
	 * @param ctx the parse tree
	 */
	void enterContainer_names(OracleParser.Container_namesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#container_names}.
	 * @param ctx the parse tree
	 */
	void exitContainer_names(OracleParser.Container_namesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#set_container_data}.
	 * @param ctx the parse tree
	 */
	void enterSet_container_data(OracleParser.Set_container_dataContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#set_container_data}.
	 * @param ctx the parse tree
	 */
	void exitSet_container_data(OracleParser.Set_container_dataContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_rem_container_data}.
	 * @param ctx the parse tree
	 */
	void enterAdd_rem_container_data(OracleParser.Add_rem_container_dataContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_rem_container_data}.
	 * @param ctx the parse tree
	 */
	void exitAdd_rem_container_data(OracleParser.Add_rem_container_dataContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#container_data_clause}.
	 * @param ctx the parse tree
	 */
	void enterContainer_data_clause(OracleParser.Container_data_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#container_data_clause}.
	 * @param ctx the parse tree
	 */
	void exitContainer_data_clause(OracleParser.Container_data_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#analyze}.
	 * @param ctx the parse tree
	 */
	void enterAnalyze(OracleParser.AnalyzeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#analyze}.
	 * @param ctx the parse tree
	 */
	void exitAnalyze(OracleParser.AnalyzeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partition_extention_clause}.
	 * @param ctx the parse tree
	 */
	void enterPartition_extention_clause(OracleParser.Partition_extention_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partition_extention_clause}.
	 * @param ctx the parse tree
	 */
	void exitPartition_extention_clause(OracleParser.Partition_extention_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#validation_clauses}.
	 * @param ctx the parse tree
	 */
	void enterValidation_clauses(OracleParser.Validation_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#validation_clauses}.
	 * @param ctx the parse tree
	 */
	void exitValidation_clauses(OracleParser.Validation_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#online_or_offline}.
	 * @param ctx the parse tree
	 */
	void enterOnline_or_offline(OracleParser.Online_or_offlineContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#online_or_offline}.
	 * @param ctx the parse tree
	 */
	void exitOnline_or_offline(OracleParser.Online_or_offlineContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#into_clause1}.
	 * @param ctx the parse tree
	 */
	void enterInto_clause1(OracleParser.Into_clause1Context ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#into_clause1}.
	 * @param ctx the parse tree
	 */
	void exitInto_clause1(OracleParser.Into_clause1Context ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partition_key_value}.
	 * @param ctx the parse tree
	 */
	void enterPartition_key_value(OracleParser.Partition_key_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partition_key_value}.
	 * @param ctx the parse tree
	 */
	void exitPartition_key_value(OracleParser.Partition_key_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subpartition_key_value}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_key_value(OracleParser.Subpartition_key_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subpartition_key_value}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_key_value(OracleParser.Subpartition_key_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#associate_statistics}.
	 * @param ctx the parse tree
	 */
	void enterAssociate_statistics(OracleParser.Associate_statisticsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#associate_statistics}.
	 * @param ctx the parse tree
	 */
	void exitAssociate_statistics(OracleParser.Associate_statisticsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_association}.
	 * @param ctx the parse tree
	 */
	void enterColumn_association(OracleParser.Column_associationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_association}.
	 * @param ctx the parse tree
	 */
	void exitColumn_association(OracleParser.Column_associationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_association}.
	 * @param ctx the parse tree
	 */
	void enterFunction_association(OracleParser.Function_associationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_association}.
	 * @param ctx the parse tree
	 */
	void exitFunction_association(OracleParser.Function_associationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#indextype_name}.
	 * @param ctx the parse tree
	 */
	void enterIndextype_name(OracleParser.Indextype_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#indextype_name}.
	 * @param ctx the parse tree
	 */
	void exitIndextype_name(OracleParser.Indextype_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#using_statistics_type}.
	 * @param ctx the parse tree
	 */
	void enterUsing_statistics_type(OracleParser.Using_statistics_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#using_statistics_type}.
	 * @param ctx the parse tree
	 */
	void exitUsing_statistics_type(OracleParser.Using_statistics_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#statistics_type_name}.
	 * @param ctx the parse tree
	 */
	void enterStatistics_type_name(OracleParser.Statistics_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#statistics_type_name}.
	 * @param ctx the parse tree
	 */
	void exitStatistics_type_name(OracleParser.Statistics_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_cost_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_cost_clause(OracleParser.Default_cost_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_cost_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_cost_clause(OracleParser.Default_cost_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cpu_cost}.
	 * @param ctx the parse tree
	 */
	void enterCpu_cost(OracleParser.Cpu_costContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cpu_cost}.
	 * @param ctx the parse tree
	 */
	void exitCpu_cost(OracleParser.Cpu_costContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#io_cost}.
	 * @param ctx the parse tree
	 */
	void enterIo_cost(OracleParser.Io_costContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#io_cost}.
	 * @param ctx the parse tree
	 */
	void exitIo_cost(OracleParser.Io_costContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#network_cost}.
	 * @param ctx the parse tree
	 */
	void enterNetwork_cost(OracleParser.Network_costContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#network_cost}.
	 * @param ctx the parse tree
	 */
	void exitNetwork_cost(OracleParser.Network_costContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_selectivity_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_selectivity_clause(OracleParser.Default_selectivity_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_selectivity_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_selectivity_clause(OracleParser.Default_selectivity_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_selectivity}.
	 * @param ctx the parse tree
	 */
	void enterDefault_selectivity(OracleParser.Default_selectivityContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_selectivity}.
	 * @param ctx the parse tree
	 */
	void exitDefault_selectivity(OracleParser.Default_selectivityContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#storage_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterStorage_table_clause(OracleParser.Storage_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#storage_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitStorage_table_clause(OracleParser.Storage_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unified_auditing}.
	 * @param ctx the parse tree
	 */
	void enterUnified_auditing(OracleParser.Unified_auditingContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unified_auditing}.
	 * @param ctx the parse tree
	 */
	void exitUnified_auditing(OracleParser.Unified_auditingContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#policy_name}.
	 * @param ctx the parse tree
	 */
	void enterPolicy_name(OracleParser.Policy_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#policy_name}.
	 * @param ctx the parse tree
	 */
	void exitPolicy_name(OracleParser.Policy_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#audit_traditional}.
	 * @param ctx the parse tree
	 */
	void enterAudit_traditional(OracleParser.Audit_traditionalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#audit_traditional}.
	 * @param ctx the parse tree
	 */
	void exitAudit_traditional(OracleParser.Audit_traditionalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#audit_direct_path}.
	 * @param ctx the parse tree
	 */
	void enterAudit_direct_path(OracleParser.Audit_direct_pathContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#audit_direct_path}.
	 * @param ctx the parse tree
	 */
	void exitAudit_direct_path(OracleParser.Audit_direct_pathContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#audit_container_clause}.
	 * @param ctx the parse tree
	 */
	void enterAudit_container_clause(OracleParser.Audit_container_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#audit_container_clause}.
	 * @param ctx the parse tree
	 */
	void exitAudit_container_clause(OracleParser.Audit_container_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#audit_operation_clause}.
	 * @param ctx the parse tree
	 */
	void enterAudit_operation_clause(OracleParser.Audit_operation_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#audit_operation_clause}.
	 * @param ctx the parse tree
	 */
	void exitAudit_operation_clause(OracleParser.Audit_operation_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#auditing_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterAuditing_by_clause(OracleParser.Auditing_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#auditing_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitAuditing_by_clause(OracleParser.Auditing_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#audit_user}.
	 * @param ctx the parse tree
	 */
	void enterAudit_user(OracleParser.Audit_userContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#audit_user}.
	 * @param ctx the parse tree
	 */
	void exitAudit_user(OracleParser.Audit_userContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#audit_schema_object_clause}.
	 * @param ctx the parse tree
	 */
	void enterAudit_schema_object_clause(OracleParser.Audit_schema_object_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#audit_schema_object_clause}.
	 * @param ctx the parse tree
	 */
	void exitAudit_schema_object_clause(OracleParser.Audit_schema_object_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sql_operation}.
	 * @param ctx the parse tree
	 */
	void enterSql_operation(OracleParser.Sql_operationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sql_operation}.
	 * @param ctx the parse tree
	 */
	void exitSql_operation(OracleParser.Sql_operationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#auditing_on_clause}.
	 * @param ctx the parse tree
	 */
	void enterAuditing_on_clause(OracleParser.Auditing_on_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#auditing_on_clause}.
	 * @param ctx the parse tree
	 */
	void exitAuditing_on_clause(OracleParser.Auditing_on_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_name}.
	 * @param ctx the parse tree
	 */
	void enterModel_name(OracleParser.Model_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_name}.
	 * @param ctx the parse tree
	 */
	void exitModel_name(OracleParser.Model_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_name}.
	 * @param ctx the parse tree
	 */
	void enterObject_name(OracleParser.Object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_name}.
	 * @param ctx the parse tree
	 */
	void exitObject_name(OracleParser.Object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#profile_name}.
	 * @param ctx the parse tree
	 */
	void enterProfile_name(OracleParser.Profile_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#profile_name}.
	 * @param ctx the parse tree
	 */
	void exitProfile_name(OracleParser.Profile_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sql_statement_shortcut}.
	 * @param ctx the parse tree
	 */
	void enterSql_statement_shortcut(OracleParser.Sql_statement_shortcutContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sql_statement_shortcut}.
	 * @param ctx the parse tree
	 */
	void exitSql_statement_shortcut(OracleParser.Sql_statement_shortcutContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_index}.
	 * @param ctx the parse tree
	 */
	void enterDrop_index(OracleParser.Drop_indexContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_index}.
	 * @param ctx the parse tree
	 */
	void exitDrop_index(OracleParser.Drop_indexContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rename_object}.
	 * @param ctx the parse tree
	 */
	void enterRename_object(OracleParser.Rename_objectContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rename_object}.
	 * @param ctx the parse tree
	 */
	void exitRename_object(OracleParser.Rename_objectContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#grant_statement}.
	 * @param ctx the parse tree
	 */
	void enterGrant_statement(OracleParser.Grant_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#grant_statement}.
	 * @param ctx the parse tree
	 */
	void exitGrant_statement(OracleParser.Grant_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#container_clause}.
	 * @param ctx the parse tree
	 */
	void enterContainer_clause(OracleParser.Container_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#container_clause}.
	 * @param ctx the parse tree
	 */
	void exitContainer_clause(OracleParser.Container_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_directory}.
	 * @param ctx the parse tree
	 */
	void enterCreate_directory(OracleParser.Create_directoryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_directory}.
	 * @param ctx the parse tree
	 */
	void exitCreate_directory(OracleParser.Create_directoryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#directory_name}.
	 * @param ctx the parse tree
	 */
	void enterDirectory_name(OracleParser.Directory_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#directory_name}.
	 * @param ctx the parse tree
	 */
	void exitDirectory_name(OracleParser.Directory_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#directory_path}.
	 * @param ctx the parse tree
	 */
	void enterDirectory_path(OracleParser.Directory_pathContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#directory_path}.
	 * @param ctx the parse tree
	 */
	void exitDirectory_path(OracleParser.Directory_pathContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_library}.
	 * @param ctx the parse tree
	 */
	void enterAlter_library(OracleParser.Alter_libraryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_library}.
	 * @param ctx the parse tree
	 */
	void exitAlter_library(OracleParser.Alter_libraryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#library_editionable}.
	 * @param ctx the parse tree
	 */
	void enterLibrary_editionable(OracleParser.Library_editionableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#library_editionable}.
	 * @param ctx the parse tree
	 */
	void exitLibrary_editionable(OracleParser.Library_editionableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#library_debug}.
	 * @param ctx the parse tree
	 */
	void enterLibrary_debug(OracleParser.Library_debugContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#library_debug}.
	 * @param ctx the parse tree
	 */
	void exitLibrary_debug(OracleParser.Library_debugContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#compiler_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void enterCompiler_parameters_clause(OracleParser.Compiler_parameters_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#compiler_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void exitCompiler_parameters_clause(OracleParser.Compiler_parameters_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#parameter_value}.
	 * @param ctx the parse tree
	 */
	void enterParameter_value(OracleParser.Parameter_valueContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#parameter_value}.
	 * @param ctx the parse tree
	 */
	void exitParameter_value(OracleParser.Parameter_valueContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#library_name}.
	 * @param ctx the parse tree
	 */
	void enterLibrary_name(OracleParser.Library_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#library_name}.
	 * @param ctx the parse tree
	 */
	void exitLibrary_name(OracleParser.Library_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_view}.
	 * @param ctx the parse tree
	 */
	void enterAlter_view(OracleParser.Alter_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_view}.
	 * @param ctx the parse tree
	 */
	void exitAlter_view(OracleParser.Alter_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_view_editionable}.
	 * @param ctx the parse tree
	 */
	void enterAlter_view_editionable(OracleParser.Alter_view_editionableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_view_editionable}.
	 * @param ctx the parse tree
	 */
	void exitAlter_view_editionable(OracleParser.Alter_view_editionableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_view}.
	 * @param ctx the parse tree
	 */
	void enterCreate_view(OracleParser.Create_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_view}.
	 * @param ctx the parse tree
	 */
	void exitCreate_view(OracleParser.Create_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#view_options}.
	 * @param ctx the parse tree
	 */
	void enterView_options(OracleParser.View_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#view_options}.
	 * @param ctx the parse tree
	 */
	void exitView_options(OracleParser.View_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#view_alias_constraint}.
	 * @param ctx the parse tree
	 */
	void enterView_alias_constraint(OracleParser.View_alias_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#view_alias_constraint}.
	 * @param ctx the parse tree
	 */
	void exitView_alias_constraint(OracleParser.View_alias_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_view_clause}.
	 * @param ctx the parse tree
	 */
	void enterObject_view_clause(OracleParser.Object_view_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_view_clause}.
	 * @param ctx the parse tree
	 */
	void exitObject_view_clause(OracleParser.Object_view_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#inline_constraint}.
	 * @param ctx the parse tree
	 */
	void enterInline_constraint(OracleParser.Inline_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#inline_constraint}.
	 * @param ctx the parse tree
	 */
	void exitInline_constraint(OracleParser.Inline_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#inline_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void enterInline_ref_constraint(OracleParser.Inline_ref_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#inline_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void exitInline_ref_constraint(OracleParser.Inline_ref_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#out_of_line_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_ref_constraint(OracleParser.Out_of_line_ref_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#out_of_line_ref_constraint}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_ref_constraint(OracleParser.Out_of_line_ref_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#out_of_line_constraint}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_constraint(OracleParser.Out_of_line_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#out_of_line_constraint}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_constraint(OracleParser.Out_of_line_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#constraint_state}.
	 * @param ctx the parse tree
	 */
	void enterConstraint_state(OracleParser.Constraint_stateContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#constraint_state}.
	 * @param ctx the parse tree
	 */
	void exitConstraint_state(OracleParser.Constraint_stateContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_tablespace}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tablespace(OracleParser.Alter_tablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_tablespace}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tablespace(OracleParser.Alter_tablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#datafile_tempfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDatafile_tempfile_clauses(OracleParser.Datafile_tempfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#datafile_tempfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDatafile_tempfile_clauses(OracleParser.Datafile_tempfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_logging_clauses}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_logging_clauses(OracleParser.Tablespace_logging_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_logging_clauses}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_logging_clauses(OracleParser.Tablespace_logging_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_group_clause}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_group_clause(OracleParser.Tablespace_group_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_group_clause}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_group_clause(OracleParser.Tablespace_group_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_group_name}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_group_name(OracleParser.Tablespace_group_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_group_name}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_group_name(OracleParser.Tablespace_group_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_state_clauses}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_state_clauses(OracleParser.Tablespace_state_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_state_clauses}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_state_clauses(OracleParser.Tablespace_state_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#flashback_mode_clause}.
	 * @param ctx the parse tree
	 */
	void enterFlashback_mode_clause(OracleParser.Flashback_mode_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#flashback_mode_clause}.
	 * @param ctx the parse tree
	 */
	void exitFlashback_mode_clause(OracleParser.Flashback_mode_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#new_tablespace_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_tablespace_name(OracleParser.New_tablespace_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#new_tablespace_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_tablespace_name(OracleParser.New_tablespace_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_tablespace}.
	 * @param ctx the parse tree
	 */
	void enterCreate_tablespace(OracleParser.Create_tablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_tablespace}.
	 * @param ctx the parse tree
	 */
	void exitCreate_tablespace(OracleParser.Create_tablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#permanent_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void enterPermanent_tablespace_clause(OracleParser.Permanent_tablespace_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#permanent_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void exitPermanent_tablespace_clause(OracleParser.Permanent_tablespace_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_encryption_spec}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_encryption_spec(OracleParser.Tablespace_encryption_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_encryption_spec}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_encryption_spec(OracleParser.Tablespace_encryption_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#logging_clause}.
	 * @param ctx the parse tree
	 */
	void enterLogging_clause(OracleParser.Logging_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#logging_clause}.
	 * @param ctx the parse tree
	 */
	void exitLogging_clause(OracleParser.Logging_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#extent_management_clause}.
	 * @param ctx the parse tree
	 */
	void enterExtent_management_clause(OracleParser.Extent_management_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#extent_management_clause}.
	 * @param ctx the parse tree
	 */
	void exitExtent_management_clause(OracleParser.Extent_management_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#segment_management_clause}.
	 * @param ctx the parse tree
	 */
	void enterSegment_management_clause(OracleParser.Segment_management_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#segment_management_clause}.
	 * @param ctx the parse tree
	 */
	void exitSegment_management_clause(OracleParser.Segment_management_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#temporary_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void enterTemporary_tablespace_clause(OracleParser.Temporary_tablespace_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#temporary_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void exitTemporary_tablespace_clause(OracleParser.Temporary_tablespace_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#undo_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void enterUndo_tablespace_clause(OracleParser.Undo_tablespace_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#undo_tablespace_clause}.
	 * @param ctx the parse tree
	 */
	void exitUndo_tablespace_clause(OracleParser.Undo_tablespace_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace_retention_clause}.
	 * @param ctx the parse tree
	 */
	void enterTablespace_retention_clause(OracleParser.Tablespace_retention_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace_retention_clause}.
	 * @param ctx the parse tree
	 */
	void exitTablespace_retention_clause(OracleParser.Tablespace_retention_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#datafile_specification}.
	 * @param ctx the parse tree
	 */
	void enterDatafile_specification(OracleParser.Datafile_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#datafile_specification}.
	 * @param ctx the parse tree
	 */
	void exitDatafile_specification(OracleParser.Datafile_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tempfile_specification}.
	 * @param ctx the parse tree
	 */
	void enterTempfile_specification(OracleParser.Tempfile_specificationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tempfile_specification}.
	 * @param ctx the parse tree
	 */
	void exitTempfile_specification(OracleParser.Tempfile_specificationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#datafile_tempfile_spec}.
	 * @param ctx the parse tree
	 */
	void enterDatafile_tempfile_spec(OracleParser.Datafile_tempfile_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#datafile_tempfile_spec}.
	 * @param ctx the parse tree
	 */
	void exitDatafile_tempfile_spec(OracleParser.Datafile_tempfile_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#redo_log_file_spec}.
	 * @param ctx the parse tree
	 */
	void enterRedo_log_file_spec(OracleParser.Redo_log_file_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#redo_log_file_spec}.
	 * @param ctx the parse tree
	 */
	void exitRedo_log_file_spec(OracleParser.Redo_log_file_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#autoextend_clause}.
	 * @param ctx the parse tree
	 */
	void enterAutoextend_clause(OracleParser.Autoextend_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#autoextend_clause}.
	 * @param ctx the parse tree
	 */
	void exitAutoextend_clause(OracleParser.Autoextend_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#maxsize_clause}.
	 * @param ctx the parse tree
	 */
	void enterMaxsize_clause(OracleParser.Maxsize_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#maxsize_clause}.
	 * @param ctx the parse tree
	 */
	void exitMaxsize_clause(OracleParser.Maxsize_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#build_clause}.
	 * @param ctx the parse tree
	 */
	void enterBuild_clause(OracleParser.Build_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#build_clause}.
	 * @param ctx the parse tree
	 */
	void exitBuild_clause(OracleParser.Build_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#parallel_clause}.
	 * @param ctx the parse tree
	 */
	void enterParallel_clause(OracleParser.Parallel_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#parallel_clause}.
	 * @param ctx the parse tree
	 */
	void exitParallel_clause(OracleParser.Parallel_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_materialized_view}.
	 * @param ctx the parse tree
	 */
	void enterAlter_materialized_view(OracleParser.Alter_materialized_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_materialized_view}.
	 * @param ctx the parse tree
	 */
	void exitAlter_materialized_view(OracleParser.Alter_materialized_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_mv_option1}.
	 * @param ctx the parse tree
	 */
	void enterAlter_mv_option1(OracleParser.Alter_mv_option1Context ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_mv_option1}.
	 * @param ctx the parse tree
	 */
	void exitAlter_mv_option1(OracleParser.Alter_mv_option1Context ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_mv_refresh}.
	 * @param ctx the parse tree
	 */
	void enterAlter_mv_refresh(OracleParser.Alter_mv_refreshContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_mv_refresh}.
	 * @param ctx the parse tree
	 */
	void exitAlter_mv_refresh(OracleParser.Alter_mv_refreshContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rollback_segment}.
	 * @param ctx the parse tree
	 */
	void enterRollback_segment(OracleParser.Rollback_segmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rollback_segment}.
	 * @param ctx the parse tree
	 */
	void exitRollback_segment(OracleParser.Rollback_segmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_mv_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterModify_mv_column_clause(OracleParser.Modify_mv_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_mv_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitModify_mv_column_clause(OracleParser.Modify_mv_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_materialized_view_log}.
	 * @param ctx the parse tree
	 */
	void enterAlter_materialized_view_log(OracleParser.Alter_materialized_view_logContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_materialized_view_log}.
	 * @param ctx the parse tree
	 */
	void exitAlter_materialized_view_log(OracleParser.Alter_materialized_view_logContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_mv_log_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_mv_log_column_clause(OracleParser.Add_mv_log_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_mv_log_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_mv_log_column_clause(OracleParser.Add_mv_log_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#move_mv_log_clause}.
	 * @param ctx the parse tree
	 */
	void enterMove_mv_log_clause(OracleParser.Move_mv_log_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#move_mv_log_clause}.
	 * @param ctx the parse tree
	 */
	void exitMove_mv_log_clause(OracleParser.Move_mv_log_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#mv_log_augmentation}.
	 * @param ctx the parse tree
	 */
	void enterMv_log_augmentation(OracleParser.Mv_log_augmentationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#mv_log_augmentation}.
	 * @param ctx the parse tree
	 */
	void exitMv_log_augmentation(OracleParser.Mv_log_augmentationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#datetime_expr}.
	 * @param ctx the parse tree
	 */
	void enterDatetime_expr(OracleParser.Datetime_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#datetime_expr}.
	 * @param ctx the parse tree
	 */
	void exitDatetime_expr(OracleParser.Datetime_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#interval_expr}.
	 * @param ctx the parse tree
	 */
	void enterInterval_expr(OracleParser.Interval_exprContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#interval_expr}.
	 * @param ctx the parse tree
	 */
	void exitInterval_expr(OracleParser.Interval_exprContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#synchronous_or_asynchronous}.
	 * @param ctx the parse tree
	 */
	void enterSynchronous_or_asynchronous(OracleParser.Synchronous_or_asynchronousContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#synchronous_or_asynchronous}.
	 * @param ctx the parse tree
	 */
	void exitSynchronous_or_asynchronous(OracleParser.Synchronous_or_asynchronousContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#including_or_excluding}.
	 * @param ctx the parse tree
	 */
	void enterIncluding_or_excluding(OracleParser.Including_or_excludingContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#including_or_excluding}.
	 * @param ctx the parse tree
	 */
	void exitIncluding_or_excluding(OracleParser.Including_or_excludingContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_materialized_view_log}.
	 * @param ctx the parse tree
	 */
	void enterCreate_materialized_view_log(OracleParser.Create_materialized_view_logContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_materialized_view_log}.
	 * @param ctx the parse tree
	 */
	void exitCreate_materialized_view_log(OracleParser.Create_materialized_view_logContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#new_values_clause}.
	 * @param ctx the parse tree
	 */
	void enterNew_values_clause(OracleParser.New_values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#new_values_clause}.
	 * @param ctx the parse tree
	 */
	void exitNew_values_clause(OracleParser.New_values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#mv_log_purge_clause}.
	 * @param ctx the parse tree
	 */
	void enterMv_log_purge_clause(OracleParser.Mv_log_purge_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#mv_log_purge_clause}.
	 * @param ctx the parse tree
	 */
	void exitMv_log_purge_clause(OracleParser.Mv_log_purge_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_materialized_view}.
	 * @param ctx the parse tree
	 */
	void enterCreate_materialized_view(OracleParser.Create_materialized_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_materialized_view}.
	 * @param ctx the parse tree
	 */
	void exitCreate_materialized_view(OracleParser.Create_materialized_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_mv_refresh}.
	 * @param ctx the parse tree
	 */
	void enterCreate_mv_refresh(OracleParser.Create_mv_refreshContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_mv_refresh}.
	 * @param ctx the parse tree
	 */
	void exitCreate_mv_refresh(OracleParser.Create_mv_refreshContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_context}.
	 * @param ctx the parse tree
	 */
	void enterCreate_context(OracleParser.Create_contextContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_context}.
	 * @param ctx the parse tree
	 */
	void exitCreate_context(OracleParser.Create_contextContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#oracle_namespace}.
	 * @param ctx the parse tree
	 */
	void enterOracle_namespace(OracleParser.Oracle_namespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#oracle_namespace}.
	 * @param ctx the parse tree
	 */
	void exitOracle_namespace(OracleParser.Oracle_namespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_cluster}.
	 * @param ctx the parse tree
	 */
	void enterCreate_cluster(OracleParser.Create_clusterContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_cluster}.
	 * @param ctx the parse tree
	 */
	void exitCreate_cluster(OracleParser.Create_clusterContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_table}.
	 * @param ctx the parse tree
	 */
	void enterCreate_table(OracleParser.Create_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_table}.
	 * @param ctx the parse tree
	 */
	void exitCreate_table(OracleParser.Create_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmltype_table}.
	 * @param ctx the parse tree
	 */
	void enterXmltype_table(OracleParser.Xmltype_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmltype_table}.
	 * @param ctx the parse tree
	 */
	void exitXmltype_table(OracleParser.Xmltype_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmltype_virtual_columns}.
	 * @param ctx the parse tree
	 */
	void enterXmltype_virtual_columns(OracleParser.Xmltype_virtual_columnsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmltype_virtual_columns}.
	 * @param ctx the parse tree
	 */
	void exitXmltype_virtual_columns(OracleParser.Xmltype_virtual_columnsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmltype_column_properties}.
	 * @param ctx the parse tree
	 */
	void enterXmltype_column_properties(OracleParser.Xmltype_column_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmltype_column_properties}.
	 * @param ctx the parse tree
	 */
	void exitXmltype_column_properties(OracleParser.Xmltype_column_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmltype_storage}.
	 * @param ctx the parse tree
	 */
	void enterXmltype_storage(OracleParser.Xmltype_storageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmltype_storage}.
	 * @param ctx the parse tree
	 */
	void exitXmltype_storage(OracleParser.Xmltype_storageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlschema_spec}.
	 * @param ctx the parse tree
	 */
	void enterXmlschema_spec(OracleParser.Xmlschema_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlschema_spec}.
	 * @param ctx the parse tree
	 */
	void exitXmlschema_spec(OracleParser.Xmlschema_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_table}.
	 * @param ctx the parse tree
	 */
	void enterObject_table(OracleParser.Object_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_table}.
	 * @param ctx the parse tree
	 */
	void exitObject_table(OracleParser.Object_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#oid_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterOid_index_clause(OracleParser.Oid_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#oid_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitOid_index_clause(OracleParser.Oid_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#oid_clause}.
	 * @param ctx the parse tree
	 */
	void enterOid_clause(OracleParser.Oid_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#oid_clause}.
	 * @param ctx the parse tree
	 */
	void exitOid_clause(OracleParser.Oid_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_properties}.
	 * @param ctx the parse tree
	 */
	void enterObject_properties(OracleParser.Object_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_properties}.
	 * @param ctx the parse tree
	 */
	void exitObject_properties(OracleParser.Object_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_table_substitution}.
	 * @param ctx the parse tree
	 */
	void enterObject_table_substitution(OracleParser.Object_table_substitutionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_table_substitution}.
	 * @param ctx the parse tree
	 */
	void exitObject_table_substitution(OracleParser.Object_table_substitutionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#relational_table}.
	 * @param ctx the parse tree
	 */
	void enterRelational_table(OracleParser.Relational_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#relational_table}.
	 * @param ctx the parse tree
	 */
	void exitRelational_table(OracleParser.Relational_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#relational_property}.
	 * @param ctx the parse tree
	 */
	void enterRelational_property(OracleParser.Relational_propertyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#relational_property}.
	 * @param ctx the parse tree
	 */
	void exitRelational_property(OracleParser.Relational_propertyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_partitioning_clauses}.
	 * @param ctx the parse tree
	 */
	void enterTable_partitioning_clauses(OracleParser.Table_partitioning_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_partitioning_clauses}.
	 * @param ctx the parse tree
	 */
	void exitTable_partitioning_clauses(OracleParser.Table_partitioning_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#range_partitions}.
	 * @param ctx the parse tree
	 */
	void enterRange_partitions(OracleParser.Range_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#range_partitions}.
	 * @param ctx the parse tree
	 */
	void exitRange_partitions(OracleParser.Range_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#list_partitions}.
	 * @param ctx the parse tree
	 */
	void enterList_partitions(OracleParser.List_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#list_partitions}.
	 * @param ctx the parse tree
	 */
	void exitList_partitions(OracleParser.List_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hash_partitions}.
	 * @param ctx the parse tree
	 */
	void enterHash_partitions(OracleParser.Hash_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hash_partitions}.
	 * @param ctx the parse tree
	 */
	void exitHash_partitions(OracleParser.Hash_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#individual_hash_partitions}.
	 * @param ctx the parse tree
	 */
	void enterIndividual_hash_partitions(OracleParser.Individual_hash_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#individual_hash_partitions}.
	 * @param ctx the parse tree
	 */
	void exitIndividual_hash_partitions(OracleParser.Individual_hash_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hash_partitions_by_quantity}.
	 * @param ctx the parse tree
	 */
	void enterHash_partitions_by_quantity(OracleParser.Hash_partitions_by_quantityContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hash_partitions_by_quantity}.
	 * @param ctx the parse tree
	 */
	void exitHash_partitions_by_quantity(OracleParser.Hash_partitions_by_quantityContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hash_partition_quantity}.
	 * @param ctx the parse tree
	 */
	void enterHash_partition_quantity(OracleParser.Hash_partition_quantityContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hash_partition_quantity}.
	 * @param ctx the parse tree
	 */
	void exitHash_partition_quantity(OracleParser.Hash_partition_quantityContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#composite_range_partitions}.
	 * @param ctx the parse tree
	 */
	void enterComposite_range_partitions(OracleParser.Composite_range_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#composite_range_partitions}.
	 * @param ctx the parse tree
	 */
	void exitComposite_range_partitions(OracleParser.Composite_range_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#composite_list_partitions}.
	 * @param ctx the parse tree
	 */
	void enterComposite_list_partitions(OracleParser.Composite_list_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#composite_list_partitions}.
	 * @param ctx the parse tree
	 */
	void exitComposite_list_partitions(OracleParser.Composite_list_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#composite_hash_partitions}.
	 * @param ctx the parse tree
	 */
	void enterComposite_hash_partitions(OracleParser.Composite_hash_partitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#composite_hash_partitions}.
	 * @param ctx the parse tree
	 */
	void exitComposite_hash_partitions(OracleParser.Composite_hash_partitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#reference_partitioning}.
	 * @param ctx the parse tree
	 */
	void enterReference_partitioning(OracleParser.Reference_partitioningContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#reference_partitioning}.
	 * @param ctx the parse tree
	 */
	void exitReference_partitioning(OracleParser.Reference_partitioningContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#reference_partition_desc}.
	 * @param ctx the parse tree
	 */
	void enterReference_partition_desc(OracleParser.Reference_partition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#reference_partition_desc}.
	 * @param ctx the parse tree
	 */
	void exitReference_partition_desc(OracleParser.Reference_partition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#system_partitioning}.
	 * @param ctx the parse tree
	 */
	void enterSystem_partitioning(OracleParser.System_partitioningContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#system_partitioning}.
	 * @param ctx the parse tree
	 */
	void exitSystem_partitioning(OracleParser.System_partitioningContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#range_partition_desc}.
	 * @param ctx the parse tree
	 */
	void enterRange_partition_desc(OracleParser.Range_partition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#range_partition_desc}.
	 * @param ctx the parse tree
	 */
	void exitRange_partition_desc(OracleParser.Range_partition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#list_partition_desc}.
	 * @param ctx the parse tree
	 */
	void enterList_partition_desc(OracleParser.List_partition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#list_partition_desc}.
	 * @param ctx the parse tree
	 */
	void exitList_partition_desc(OracleParser.List_partition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subpartition_template}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_template(OracleParser.Subpartition_templateContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subpartition_template}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_template(OracleParser.Subpartition_templateContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hash_subpartition_quantity}.
	 * @param ctx the parse tree
	 */
	void enterHash_subpartition_quantity(OracleParser.Hash_subpartition_quantityContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hash_subpartition_quantity}.
	 * @param ctx the parse tree
	 */
	void exitHash_subpartition_quantity(OracleParser.Hash_subpartition_quantityContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subpartition_by_range}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_by_range(OracleParser.Subpartition_by_rangeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subpartition_by_range}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_by_range(OracleParser.Subpartition_by_rangeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subpartition_by_list}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_by_list(OracleParser.Subpartition_by_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subpartition_by_list}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_by_list(OracleParser.Subpartition_by_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subpartition_by_hash}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_by_hash(OracleParser.Subpartition_by_hashContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subpartition_by_hash}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_by_hash(OracleParser.Subpartition_by_hashContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subpartition_name}.
	 * @param ctx the parse tree
	 */
	void enterSubpartition_name(OracleParser.Subpartition_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subpartition_name}.
	 * @param ctx the parse tree
	 */
	void exitSubpartition_name(OracleParser.Subpartition_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#range_subpartition_desc}.
	 * @param ctx the parse tree
	 */
	void enterRange_subpartition_desc(OracleParser.Range_subpartition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#range_subpartition_desc}.
	 * @param ctx the parse tree
	 */
	void exitRange_subpartition_desc(OracleParser.Range_subpartition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#list_subpartition_desc}.
	 * @param ctx the parse tree
	 */
	void enterList_subpartition_desc(OracleParser.List_subpartition_descContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#list_subpartition_desc}.
	 * @param ctx the parse tree
	 */
	void exitList_subpartition_desc(OracleParser.List_subpartition_descContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#individual_hash_subparts}.
	 * @param ctx the parse tree
	 */
	void enterIndividual_hash_subparts(OracleParser.Individual_hash_subpartsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#individual_hash_subparts}.
	 * @param ctx the parse tree
	 */
	void exitIndividual_hash_subparts(OracleParser.Individual_hash_subpartsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hash_subparts_by_quantity}.
	 * @param ctx the parse tree
	 */
	void enterHash_subparts_by_quantity(OracleParser.Hash_subparts_by_quantityContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hash_subparts_by_quantity}.
	 * @param ctx the parse tree
	 */
	void exitHash_subparts_by_quantity(OracleParser.Hash_subparts_by_quantityContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#range_values_clause}.
	 * @param ctx the parse tree
	 */
	void enterRange_values_clause(OracleParser.Range_values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#range_values_clause}.
	 * @param ctx the parse tree
	 */
	void exitRange_values_clause(OracleParser.Range_values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#list_values_clause}.
	 * @param ctx the parse tree
	 */
	void enterList_values_clause(OracleParser.List_values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#list_values_clause}.
	 * @param ctx the parse tree
	 */
	void exitList_values_clause(OracleParser.List_values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_partition_description}.
	 * @param ctx the parse tree
	 */
	void enterTable_partition_description(OracleParser.Table_partition_descriptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_partition_description}.
	 * @param ctx the parse tree
	 */
	void exitTable_partition_description(OracleParser.Table_partition_descriptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partitioning_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterPartitioning_storage_clause(OracleParser.Partitioning_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partitioning_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitPartitioning_storage_clause(OracleParser.Partitioning_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_partitioning_storage}.
	 * @param ctx the parse tree
	 */
	void enterLob_partitioning_storage(OracleParser.Lob_partitioning_storageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_partitioning_storage}.
	 * @param ctx the parse tree
	 */
	void exitLob_partitioning_storage(OracleParser.Lob_partitioning_storageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#datatype_null_enable}.
	 * @param ctx the parse tree
	 */
	void enterDatatype_null_enable(OracleParser.Datatype_null_enableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#datatype_null_enable}.
	 * @param ctx the parse tree
	 */
	void exitDatatype_null_enable(OracleParser.Datatype_null_enableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#size_clause}.
	 * @param ctx the parse tree
	 */
	void enterSize_clause(OracleParser.Size_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#size_clause}.
	 * @param ctx the parse tree
	 */
	void exitSize_clause(OracleParser.Size_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_compression}.
	 * @param ctx the parse tree
	 */
	void enterTable_compression(OracleParser.Table_compressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_compression}.
	 * @param ctx the parse tree
	 */
	void exitTable_compression(OracleParser.Table_compressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#physical_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void enterPhysical_attributes_clause(OracleParser.Physical_attributes_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#physical_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void exitPhysical_attributes_clause(OracleParser.Physical_attributes_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterStorage_clause(OracleParser.Storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitStorage_clause(OracleParser.Storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#deferred_segment_creation}.
	 * @param ctx the parse tree
	 */
	void enterDeferred_segment_creation(OracleParser.Deferred_segment_creationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#deferred_segment_creation}.
	 * @param ctx the parse tree
	 */
	void exitDeferred_segment_creation(OracleParser.Deferred_segment_creationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#segment_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void enterSegment_attributes_clause(OracleParser.Segment_attributes_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#segment_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void exitSegment_attributes_clause(OracleParser.Segment_attributes_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#physical_properties}.
	 * @param ctx the parse tree
	 */
	void enterPhysical_properties(OracleParser.Physical_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#physical_properties}.
	 * @param ctx the parse tree
	 */
	void exitPhysical_properties(OracleParser.Physical_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#row_movement_clause}.
	 * @param ctx the parse tree
	 */
	void enterRow_movement_clause(OracleParser.Row_movement_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#row_movement_clause}.
	 * @param ctx the parse tree
	 */
	void exitRow_movement_clause(OracleParser.Row_movement_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#flashback_archive_clause}.
	 * @param ctx the parse tree
	 */
	void enterFlashback_archive_clause(OracleParser.Flashback_archive_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#flashback_archive_clause}.
	 * @param ctx the parse tree
	 */
	void exitFlashback_archive_clause(OracleParser.Flashback_archive_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#log_grp}.
	 * @param ctx the parse tree
	 */
	void enterLog_grp(OracleParser.Log_grpContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#log_grp}.
	 * @param ctx the parse tree
	 */
	void exitLog_grp(OracleParser.Log_grpContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#supplemental_table_logging}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_table_logging(OracleParser.Supplemental_table_loggingContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#supplemental_table_logging}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_table_logging(OracleParser.Supplemental_table_loggingContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#supplemental_log_grp_clause}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_log_grp_clause(OracleParser.Supplemental_log_grp_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#supplemental_log_grp_clause}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_log_grp_clause(OracleParser.Supplemental_log_grp_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#supplemental_id_key_clause}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_id_key_clause(OracleParser.Supplemental_id_key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#supplemental_id_key_clause}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_id_key_clause(OracleParser.Supplemental_id_key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#allocate_extent_clause}.
	 * @param ctx the parse tree
	 */
	void enterAllocate_extent_clause(OracleParser.Allocate_extent_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#allocate_extent_clause}.
	 * @param ctx the parse tree
	 */
	void exitAllocate_extent_clause(OracleParser.Allocate_extent_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#deallocate_unused_clause}.
	 * @param ctx the parse tree
	 */
	void enterDeallocate_unused_clause(OracleParser.Deallocate_unused_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#deallocate_unused_clause}.
	 * @param ctx the parse tree
	 */
	void exitDeallocate_unused_clause(OracleParser.Deallocate_unused_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#shrink_clause}.
	 * @param ctx the parse tree
	 */
	void enterShrink_clause(OracleParser.Shrink_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#shrink_clause}.
	 * @param ctx the parse tree
	 */
	void exitShrink_clause(OracleParser.Shrink_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#records_per_block_clause}.
	 * @param ctx the parse tree
	 */
	void enterRecords_per_block_clause(OracleParser.Records_per_block_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#records_per_block_clause}.
	 * @param ctx the parse tree
	 */
	void exitRecords_per_block_clause(OracleParser.Records_per_block_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#upgrade_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterUpgrade_table_clause(OracleParser.Upgrade_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#upgrade_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitUpgrade_table_clause(OracleParser.Upgrade_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#truncate_table}.
	 * @param ctx the parse tree
	 */
	void enterTruncate_table(OracleParser.Truncate_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#truncate_table}.
	 * @param ctx the parse tree
	 */
	void exitTruncate_table(OracleParser.Truncate_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_table}.
	 * @param ctx the parse tree
	 */
	void enterDrop_table(OracleParser.Drop_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_table}.
	 * @param ctx the parse tree
	 */
	void exitDrop_table(OracleParser.Drop_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_view}.
	 * @param ctx the parse tree
	 */
	void enterDrop_view(OracleParser.Drop_viewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_view}.
	 * @param ctx the parse tree
	 */
	void exitDrop_view(OracleParser.Drop_viewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#comment_on_column}.
	 * @param ctx the parse tree
	 */
	void enterComment_on_column(OracleParser.Comment_on_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#comment_on_column}.
	 * @param ctx the parse tree
	 */
	void exitComment_on_column(OracleParser.Comment_on_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#enable_or_disable}.
	 * @param ctx the parse tree
	 */
	void enterEnable_or_disable(OracleParser.Enable_or_disableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#enable_or_disable}.
	 * @param ctx the parse tree
	 */
	void exitEnable_or_disable(OracleParser.Enable_or_disableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#allow_or_disallow}.
	 * @param ctx the parse tree
	 */
	void enterAllow_or_disallow(OracleParser.Allow_or_disallowContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#allow_or_disallow}.
	 * @param ctx the parse tree
	 */
	void exitAllow_or_disallow(OracleParser.Allow_or_disallowContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_synonym}.
	 * @param ctx the parse tree
	 */
	void enterCreate_synonym(OracleParser.Create_synonymContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_synonym}.
	 * @param ctx the parse tree
	 */
	void exitCreate_synonym(OracleParser.Create_synonymContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#comment_on_table}.
	 * @param ctx the parse tree
	 */
	void enterComment_on_table(OracleParser.Comment_on_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#comment_on_table}.
	 * @param ctx the parse tree
	 */
	void exitComment_on_table(OracleParser.Comment_on_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_cluster}.
	 * @param ctx the parse tree
	 */
	void enterAlter_cluster(OracleParser.Alter_clusterContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_cluster}.
	 * @param ctx the parse tree
	 */
	void exitAlter_cluster(OracleParser.Alter_clusterContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cache_or_nocache}.
	 * @param ctx the parse tree
	 */
	void enterCache_or_nocache(OracleParser.Cache_or_nocacheContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cache_or_nocache}.
	 * @param ctx the parse tree
	 */
	void exitCache_or_nocache(OracleParser.Cache_or_nocacheContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#database_name}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_name(OracleParser.Database_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#database_name}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_name(OracleParser.Database_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_database}.
	 * @param ctx the parse tree
	 */
	void enterAlter_database(OracleParser.Alter_databaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_database}.
	 * @param ctx the parse tree
	 */
	void exitAlter_database(OracleParser.Alter_databaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#startup_clauses}.
	 * @param ctx the parse tree
	 */
	void enterStartup_clauses(OracleParser.Startup_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#startup_clauses}.
	 * @param ctx the parse tree
	 */
	void exitStartup_clauses(OracleParser.Startup_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#resetlogs_or_noresetlogs}.
	 * @param ctx the parse tree
	 */
	void enterResetlogs_or_noresetlogs(OracleParser.Resetlogs_or_noresetlogsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#resetlogs_or_noresetlogs}.
	 * @param ctx the parse tree
	 */
	void exitResetlogs_or_noresetlogs(OracleParser.Resetlogs_or_noresetlogsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#upgrade_or_downgrade}.
	 * @param ctx the parse tree
	 */
	void enterUpgrade_or_downgrade(OracleParser.Upgrade_or_downgradeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#upgrade_or_downgrade}.
	 * @param ctx the parse tree
	 */
	void exitUpgrade_or_downgrade(OracleParser.Upgrade_or_downgradeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#recovery_clauses}.
	 * @param ctx the parse tree
	 */
	void enterRecovery_clauses(OracleParser.Recovery_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#recovery_clauses}.
	 * @param ctx the parse tree
	 */
	void exitRecovery_clauses(OracleParser.Recovery_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#begin_or_end}.
	 * @param ctx the parse tree
	 */
	void enterBegin_or_end(OracleParser.Begin_or_endContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#begin_or_end}.
	 * @param ctx the parse tree
	 */
	void exitBegin_or_end(OracleParser.Begin_or_endContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#general_recovery}.
	 * @param ctx the parse tree
	 */
	void enterGeneral_recovery(OracleParser.General_recoveryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#general_recovery}.
	 * @param ctx the parse tree
	 */
	void exitGeneral_recovery(OracleParser.General_recoveryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#full_database_recovery}.
	 * @param ctx the parse tree
	 */
	void enterFull_database_recovery(OracleParser.Full_database_recoveryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#full_database_recovery}.
	 * @param ctx the parse tree
	 */
	void exitFull_database_recovery(OracleParser.Full_database_recoveryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partial_database_recovery}.
	 * @param ctx the parse tree
	 */
	void enterPartial_database_recovery(OracleParser.Partial_database_recoveryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partial_database_recovery}.
	 * @param ctx the parse tree
	 */
	void exitPartial_database_recovery(OracleParser.Partial_database_recoveryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partial_database_recovery_10g}.
	 * @param ctx the parse tree
	 */
	void enterPartial_database_recovery_10g(OracleParser.Partial_database_recovery_10gContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partial_database_recovery_10g}.
	 * @param ctx the parse tree
	 */
	void exitPartial_database_recovery_10g(OracleParser.Partial_database_recovery_10gContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#managed_standby_recovery}.
	 * @param ctx the parse tree
	 */
	void enterManaged_standby_recovery(OracleParser.Managed_standby_recoveryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#managed_standby_recovery}.
	 * @param ctx the parse tree
	 */
	void exitManaged_standby_recovery(OracleParser.Managed_standby_recoveryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#db_name}.
	 * @param ctx the parse tree
	 */
	void enterDb_name(OracleParser.Db_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#db_name}.
	 * @param ctx the parse tree
	 */
	void exitDb_name(OracleParser.Db_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#database_file_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDatabase_file_clauses(OracleParser.Database_file_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#database_file_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDatabase_file_clauses(OracleParser.Database_file_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#create_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void enterCreate_datafile_clause(OracleParser.Create_datafile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#create_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void exitCreate_datafile_clause(OracleParser.Create_datafile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_datafile_clause(OracleParser.Alter_datafile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_datafile_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_datafile_clause(OracleParser.Alter_datafile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_tempfile_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_tempfile_clause(OracleParser.Alter_tempfile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_tempfile_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_tempfile_clause(OracleParser.Alter_tempfile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterLogfile_clauses(OracleParser.Logfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitLogfile_clauses(OracleParser.Logfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterAdd_logfile_clauses(OracleParser.Add_logfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitAdd_logfile_clauses(OracleParser.Add_logfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#log_file_group}.
	 * @param ctx the parse tree
	 */
	void enterLog_file_group(OracleParser.Log_file_groupContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#log_file_group}.
	 * @param ctx the parse tree
	 */
	void exitLog_file_group(OracleParser.Log_file_groupContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterDrop_logfile_clauses(OracleParser.Drop_logfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_logfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitDrop_logfile_clauses(OracleParser.Drop_logfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#switch_logfile_clause}.
	 * @param ctx the parse tree
	 */
	void enterSwitch_logfile_clause(OracleParser.Switch_logfile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#switch_logfile_clause}.
	 * @param ctx the parse tree
	 */
	void exitSwitch_logfile_clause(OracleParser.Switch_logfile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#supplemental_db_logging}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_db_logging(OracleParser.Supplemental_db_loggingContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#supplemental_db_logging}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_db_logging(OracleParser.Supplemental_db_loggingContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_or_drop}.
	 * @param ctx the parse tree
	 */
	void enterAdd_or_drop(OracleParser.Add_or_dropContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_or_drop}.
	 * @param ctx the parse tree
	 */
	void exitAdd_or_drop(OracleParser.Add_or_dropContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#supplemental_plsql_clause}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_plsql_clause(OracleParser.Supplemental_plsql_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#supplemental_plsql_clause}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_plsql_clause(OracleParser.Supplemental_plsql_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#logfile_descriptor}.
	 * @param ctx the parse tree
	 */
	void enterLogfile_descriptor(OracleParser.Logfile_descriptorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#logfile_descriptor}.
	 * @param ctx the parse tree
	 */
	void exitLogfile_descriptor(OracleParser.Logfile_descriptorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#controlfile_clauses}.
	 * @param ctx the parse tree
	 */
	void enterControlfile_clauses(OracleParser.Controlfile_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#controlfile_clauses}.
	 * @param ctx the parse tree
	 */
	void exitControlfile_clauses(OracleParser.Controlfile_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#trace_file_clause}.
	 * @param ctx the parse tree
	 */
	void enterTrace_file_clause(OracleParser.Trace_file_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#trace_file_clause}.
	 * @param ctx the parse tree
	 */
	void exitTrace_file_clause(OracleParser.Trace_file_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#standby_database_clauses}.
	 * @param ctx the parse tree
	 */
	void enterStandby_database_clauses(OracleParser.Standby_database_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#standby_database_clauses}.
	 * @param ctx the parse tree
	 */
	void exitStandby_database_clauses(OracleParser.Standby_database_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#activate_standby_db_clause}.
	 * @param ctx the parse tree
	 */
	void enterActivate_standby_db_clause(OracleParser.Activate_standby_db_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#activate_standby_db_clause}.
	 * @param ctx the parse tree
	 */
	void exitActivate_standby_db_clause(OracleParser.Activate_standby_db_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#maximize_standby_db_clause}.
	 * @param ctx the parse tree
	 */
	void enterMaximize_standby_db_clause(OracleParser.Maximize_standby_db_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#maximize_standby_db_clause}.
	 * @param ctx the parse tree
	 */
	void exitMaximize_standby_db_clause(OracleParser.Maximize_standby_db_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#register_logfile_clause}.
	 * @param ctx the parse tree
	 */
	void enterRegister_logfile_clause(OracleParser.Register_logfile_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#register_logfile_clause}.
	 * @param ctx the parse tree
	 */
	void exitRegister_logfile_clause(OracleParser.Register_logfile_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#commit_switchover_clause}.
	 * @param ctx the parse tree
	 */
	void enterCommit_switchover_clause(OracleParser.Commit_switchover_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#commit_switchover_clause}.
	 * @param ctx the parse tree
	 */
	void exitCommit_switchover_clause(OracleParser.Commit_switchover_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#start_standby_clause}.
	 * @param ctx the parse tree
	 */
	void enterStart_standby_clause(OracleParser.Start_standby_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#start_standby_clause}.
	 * @param ctx the parse tree
	 */
	void exitStart_standby_clause(OracleParser.Start_standby_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#stop_standby_clause}.
	 * @param ctx the parse tree
	 */
	void enterStop_standby_clause(OracleParser.Stop_standby_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#stop_standby_clause}.
	 * @param ctx the parse tree
	 */
	void exitStop_standby_clause(OracleParser.Stop_standby_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#convert_database_clause}.
	 * @param ctx the parse tree
	 */
	void enterConvert_database_clause(OracleParser.Convert_database_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#convert_database_clause}.
	 * @param ctx the parse tree
	 */
	void exitConvert_database_clause(OracleParser.Convert_database_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_settings_clause}.
	 * @param ctx the parse tree
	 */
	void enterDefault_settings_clause(OracleParser.Default_settings_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_settings_clause}.
	 * @param ctx the parse tree
	 */
	void exitDefault_settings_clause(OracleParser.Default_settings_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#set_time_zone_clause}.
	 * @param ctx the parse tree
	 */
	void enterSet_time_zone_clause(OracleParser.Set_time_zone_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#set_time_zone_clause}.
	 * @param ctx the parse tree
	 */
	void exitSet_time_zone_clause(OracleParser.Set_time_zone_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#instance_clauses}.
	 * @param ctx the parse tree
	 */
	void enterInstance_clauses(OracleParser.Instance_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#instance_clauses}.
	 * @param ctx the parse tree
	 */
	void exitInstance_clauses(OracleParser.Instance_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#security_clause}.
	 * @param ctx the parse tree
	 */
	void enterSecurity_clause(OracleParser.Security_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#security_clause}.
	 * @param ctx the parse tree
	 */
	void exitSecurity_clause(OracleParser.Security_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#domain}.
	 * @param ctx the parse tree
	 */
	void enterDomain(OracleParser.DomainContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#domain}.
	 * @param ctx the parse tree
	 */
	void exitDomain(OracleParser.DomainContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#database}.
	 * @param ctx the parse tree
	 */
	void enterDatabase(OracleParser.DatabaseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#database}.
	 * @param ctx the parse tree
	 */
	void exitDatabase(OracleParser.DatabaseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#edition_name}.
	 * @param ctx the parse tree
	 */
	void enterEdition_name(OracleParser.Edition_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#edition_name}.
	 * @param ctx the parse tree
	 */
	void exitEdition_name(OracleParser.Edition_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#filenumber}.
	 * @param ctx the parse tree
	 */
	void enterFilenumber(OracleParser.FilenumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#filenumber}.
	 * @param ctx the parse tree
	 */
	void exitFilenumber(OracleParser.FilenumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#filename}.
	 * @param ctx the parse tree
	 */
	void enterFilename(OracleParser.FilenameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#filename}.
	 * @param ctx the parse tree
	 */
	void exitFilename(OracleParser.FilenameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_table}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table(OracleParser.Alter_tableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_table}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table(OracleParser.Alter_tableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_table_properties}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_properties(OracleParser.Alter_table_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_table_properties}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_properties(OracleParser.Alter_table_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_table_properties_1}.
	 * @param ctx the parse tree
	 */
	void enterAlter_table_properties_1(OracleParser.Alter_table_properties_1Context ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_table_properties_1}.
	 * @param ctx the parse tree
	 */
	void exitAlter_table_properties_1(OracleParser.Alter_table_properties_1Context ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_iot_clauses}.
	 * @param ctx the parse tree
	 */
	void enterAlter_iot_clauses(OracleParser.Alter_iot_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_iot_clauses}.
	 * @param ctx the parse tree
	 */
	void exitAlter_iot_clauses(OracleParser.Alter_iot_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_mapping_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_mapping_table_clause(OracleParser.Alter_mapping_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_mapping_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_mapping_table_clause(OracleParser.Alter_mapping_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void enterAlter_overflow_clause(OracleParser.Alter_overflow_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void exitAlter_overflow_clause(OracleParser.Alter_overflow_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_overflow_clause(OracleParser.Add_overflow_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_overflow_clause(OracleParser.Add_overflow_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#enable_disable_clause}.
	 * @param ctx the parse tree
	 */
	void enterEnable_disable_clause(OracleParser.Enable_disable_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#enable_disable_clause}.
	 * @param ctx the parse tree
	 */
	void exitEnable_disable_clause(OracleParser.Enable_disable_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#using_index_clause}.
	 * @param ctx the parse tree
	 */
	void enterUsing_index_clause(OracleParser.Using_index_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#using_index_clause}.
	 * @param ctx the parse tree
	 */
	void exitUsing_index_clause(OracleParser.Using_index_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_attributes}.
	 * @param ctx the parse tree
	 */
	void enterIndex_attributes(OracleParser.Index_attributesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_attributes}.
	 * @param ctx the parse tree
	 */
	void exitIndex_attributes(OracleParser.Index_attributesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sort_or_nosort}.
	 * @param ctx the parse tree
	 */
	void enterSort_or_nosort(OracleParser.Sort_or_nosortContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sort_or_nosort}.
	 * @param ctx the parse tree
	 */
	void exitSort_or_nosort(OracleParser.Sort_or_nosortContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#exceptions_clause}.
	 * @param ctx the parse tree
	 */
	void enterExceptions_clause(OracleParser.Exceptions_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#exceptions_clause}.
	 * @param ctx the parse tree
	 */
	void exitExceptions_clause(OracleParser.Exceptions_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#move_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterMove_table_clause(OracleParser.Move_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#move_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitMove_table_clause(OracleParser.Move_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_org_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_org_table_clause(OracleParser.Index_org_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_org_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_org_table_clause(OracleParser.Index_org_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#mapping_table_clause}.
	 * @param ctx the parse tree
	 */
	void enterMapping_table_clause(OracleParser.Mapping_table_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#mapping_table_clause}.
	 * @param ctx the parse tree
	 */
	void exitMapping_table_clause(OracleParser.Mapping_table_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#key_compression}.
	 * @param ctx the parse tree
	 */
	void enterKey_compression(OracleParser.Key_compressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#key_compression}.
	 * @param ctx the parse tree
	 */
	void exitKey_compression(OracleParser.Key_compressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_org_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void enterIndex_org_overflow_clause(OracleParser.Index_org_overflow_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_org_overflow_clause}.
	 * @param ctx the parse tree
	 */
	void exitIndex_org_overflow_clause(OracleParser.Index_org_overflow_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_clauses}.
	 * @param ctx the parse tree
	 */
	void enterColumn_clauses(OracleParser.Column_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_clauses}.
	 * @param ctx the parse tree
	 */
	void exitColumn_clauses(OracleParser.Column_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_collection_retrieval}.
	 * @param ctx the parse tree
	 */
	void enterModify_collection_retrieval(OracleParser.Modify_collection_retrievalContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_collection_retrieval}.
	 * @param ctx the parse tree
	 */
	void exitModify_collection_retrieval(OracleParser.Modify_collection_retrievalContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#collection_item}.
	 * @param ctx the parse tree
	 */
	void enterCollection_item(OracleParser.Collection_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#collection_item}.
	 * @param ctx the parse tree
	 */
	void exitCollection_item(OracleParser.Collection_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rename_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterRename_column_clause(OracleParser.Rename_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rename_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitRename_column_clause(OracleParser.Rename_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#old_column_name}.
	 * @param ctx the parse tree
	 */
	void enterOld_column_name(OracleParser.Old_column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#old_column_name}.
	 * @param ctx the parse tree
	 */
	void exitOld_column_name(OracleParser.Old_column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#new_column_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_column_name(OracleParser.New_column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#new_column_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_column_name(OracleParser.New_column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_modify_drop_column_clauses}.
	 * @param ctx the parse tree
	 */
	void enterAdd_modify_drop_column_clauses(OracleParser.Add_modify_drop_column_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_modify_drop_column_clauses}.
	 * @param ctx the parse tree
	 */
	void exitAdd_modify_drop_column_clauses(OracleParser.Add_modify_drop_column_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_column_clause(OracleParser.Drop_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_column_clause(OracleParser.Drop_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_column_clauses}.
	 * @param ctx the parse tree
	 */
	void enterModify_column_clauses(OracleParser.Modify_column_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_column_clauses}.
	 * @param ctx the parse tree
	 */
	void exitModify_column_clauses(OracleParser.Modify_column_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterModify_col_properties(OracleParser.Modify_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitModify_col_properties(OracleParser.Modify_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_col_substitutable}.
	 * @param ctx the parse tree
	 */
	void enterModify_col_substitutable(OracleParser.Modify_col_substitutableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_col_substitutable}.
	 * @param ctx the parse tree
	 */
	void exitModify_col_substitutable(OracleParser.Modify_col_substitutableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_column_clause(OracleParser.Add_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_column_clause(OracleParser.Add_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#alter_varray_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterAlter_varray_col_properties(OracleParser.Alter_varray_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#alter_varray_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitAlter_varray_col_properties(OracleParser.Alter_varray_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#varray_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterVarray_col_properties(OracleParser.Varray_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#varray_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitVarray_col_properties(OracleParser.Varray_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#varray_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterVarray_storage_clause(OracleParser.Varray_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#varray_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitVarray_storage_clause(OracleParser.Varray_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_segname}.
	 * @param ctx the parse tree
	 */
	void enterLob_segname(OracleParser.Lob_segnameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_segname}.
	 * @param ctx the parse tree
	 */
	void exitLob_segname(OracleParser.Lob_segnameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_item}.
	 * @param ctx the parse tree
	 */
	void enterLob_item(OracleParser.Lob_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_item}.
	 * @param ctx the parse tree
	 */
	void exitLob_item(OracleParser.Lob_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_storage_parameters}.
	 * @param ctx the parse tree
	 */
	void enterLob_storage_parameters(OracleParser.Lob_storage_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_storage_parameters}.
	 * @param ctx the parse tree
	 */
	void exitLob_storage_parameters(OracleParser.Lob_storage_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterLob_storage_clause(OracleParser.Lob_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitLob_storage_clause(OracleParser.Lob_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void enterModify_lob_storage_clause(OracleParser.Modify_lob_storage_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_lob_storage_clause}.
	 * @param ctx the parse tree
	 */
	void exitModify_lob_storage_clause(OracleParser.Modify_lob_storage_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#modify_lob_parameters}.
	 * @param ctx the parse tree
	 */
	void enterModify_lob_parameters(OracleParser.Modify_lob_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#modify_lob_parameters}.
	 * @param ctx the parse tree
	 */
	void exitModify_lob_parameters(OracleParser.Modify_lob_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_parameters}.
	 * @param ctx the parse tree
	 */
	void enterLob_parameters(OracleParser.Lob_parametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_parameters}.
	 * @param ctx the parse tree
	 */
	void exitLob_parameters(OracleParser.Lob_parametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_deduplicate_clause}.
	 * @param ctx the parse tree
	 */
	void enterLob_deduplicate_clause(OracleParser.Lob_deduplicate_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_deduplicate_clause}.
	 * @param ctx the parse tree
	 */
	void exitLob_deduplicate_clause(OracleParser.Lob_deduplicate_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_compression_clause}.
	 * @param ctx the parse tree
	 */
	void enterLob_compression_clause(OracleParser.Lob_compression_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_compression_clause}.
	 * @param ctx the parse tree
	 */
	void exitLob_compression_clause(OracleParser.Lob_compression_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lob_retention_clause}.
	 * @param ctx the parse tree
	 */
	void enterLob_retention_clause(OracleParser.Lob_retention_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lob_retention_clause}.
	 * @param ctx the parse tree
	 */
	void exitLob_retention_clause(OracleParser.Lob_retention_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#encryption_spec}.
	 * @param ctx the parse tree
	 */
	void enterEncryption_spec(OracleParser.Encryption_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#encryption_spec}.
	 * @param ctx the parse tree
	 */
	void exitEncryption_spec(OracleParser.Encryption_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tablespace}.
	 * @param ctx the parse tree
	 */
	void enterTablespace(OracleParser.TablespaceContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tablespace}.
	 * @param ctx the parse tree
	 */
	void exitTablespace(OracleParser.TablespaceContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#varray_item}.
	 * @param ctx the parse tree
	 */
	void enterVarray_item(OracleParser.Varray_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#varray_item}.
	 * @param ctx the parse tree
	 */
	void exitVarray_item(OracleParser.Varray_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_properties}.
	 * @param ctx the parse tree
	 */
	void enterColumn_properties(OracleParser.Column_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_properties}.
	 * @param ctx the parse tree
	 */
	void exitColumn_properties(OracleParser.Column_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#period_definition}.
	 * @param ctx the parse tree
	 */
	void enterPeriod_definition(OracleParser.Period_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#period_definition}.
	 * @param ctx the parse tree
	 */
	void exitPeriod_definition(OracleParser.Period_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#start_time_column}.
	 * @param ctx the parse tree
	 */
	void enterStart_time_column(OracleParser.Start_time_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#start_time_column}.
	 * @param ctx the parse tree
	 */
	void exitStart_time_column(OracleParser.Start_time_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#end_time_column}.
	 * @param ctx the parse tree
	 */
	void enterEnd_time_column(OracleParser.End_time_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#end_time_column}.
	 * @param ctx the parse tree
	 */
	void exitEnd_time_column(OracleParser.End_time_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_definition}.
	 * @param ctx the parse tree
	 */
	void enterColumn_definition(OracleParser.Column_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_definition}.
	 * @param ctx the parse tree
	 */
	void exitColumn_definition(OracleParser.Column_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#virtual_column_definition}.
	 * @param ctx the parse tree
	 */
	void enterVirtual_column_definition(OracleParser.Virtual_column_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#virtual_column_definition}.
	 * @param ctx the parse tree
	 */
	void exitVirtual_column_definition(OracleParser.Virtual_column_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#autogenerated_sequence_definition}.
	 * @param ctx the parse tree
	 */
	void enterAutogenerated_sequence_definition(OracleParser.Autogenerated_sequence_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#autogenerated_sequence_definition}.
	 * @param ctx the parse tree
	 */
	void exitAutogenerated_sequence_definition(OracleParser.Autogenerated_sequence_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#out_of_line_part_storage}.
	 * @param ctx the parse tree
	 */
	void enterOut_of_line_part_storage(OracleParser.Out_of_line_part_storageContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#out_of_line_part_storage}.
	 * @param ctx the parse tree
	 */
	void exitOut_of_line_part_storage(OracleParser.Out_of_line_part_storageContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#nested_table_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterNested_table_col_properties(OracleParser.Nested_table_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#nested_table_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitNested_table_col_properties(OracleParser.Nested_table_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#nested_item}.
	 * @param ctx the parse tree
	 */
	void enterNested_item(OracleParser.Nested_itemContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#nested_item}.
	 * @param ctx the parse tree
	 */
	void exitNested_item(OracleParser.Nested_itemContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#substitutable_column_clause}.
	 * @param ctx the parse tree
	 */
	void enterSubstitutable_column_clause(OracleParser.Substitutable_column_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#substitutable_column_clause}.
	 * @param ctx the parse tree
	 */
	void exitSubstitutable_column_clause(OracleParser.Substitutable_column_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partition_name}.
	 * @param ctx the parse tree
	 */
	void enterPartition_name(OracleParser.Partition_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partition_name}.
	 * @param ctx the parse tree
	 */
	void exitPartition_name(OracleParser.Partition_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#supplemental_logging_props}.
	 * @param ctx the parse tree
	 */
	void enterSupplemental_logging_props(OracleParser.Supplemental_logging_propsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#supplemental_logging_props}.
	 * @param ctx the parse tree
	 */
	void exitSupplemental_logging_props(OracleParser.Supplemental_logging_propsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_or_attribute}.
	 * @param ctx the parse tree
	 */
	void enterColumn_or_attribute(OracleParser.Column_or_attributeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_or_attribute}.
	 * @param ctx the parse tree
	 */
	void exitColumn_or_attribute(OracleParser.Column_or_attributeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_type_col_properties}.
	 * @param ctx the parse tree
	 */
	void enterObject_type_col_properties(OracleParser.Object_type_col_propertiesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_type_col_properties}.
	 * @param ctx the parse tree
	 */
	void exitObject_type_col_properties(OracleParser.Object_type_col_propertiesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#constraint_clauses}.
	 * @param ctx the parse tree
	 */
	void enterConstraint_clauses(OracleParser.Constraint_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#constraint_clauses}.
	 * @param ctx the parse tree
	 */
	void exitConstraint_clauses(OracleParser.Constraint_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#old_constraint_name}.
	 * @param ctx the parse tree
	 */
	void enterOld_constraint_name(OracleParser.Old_constraint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#old_constraint_name}.
	 * @param ctx the parse tree
	 */
	void exitOld_constraint_name(OracleParser.Old_constraint_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#new_constraint_name}.
	 * @param ctx the parse tree
	 */
	void enterNew_constraint_name(OracleParser.New_constraint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#new_constraint_name}.
	 * @param ctx the parse tree
	 */
	void exitNew_constraint_name(OracleParser.New_constraint_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_constraint_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_constraint_clause(OracleParser.Drop_constraint_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_constraint_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_constraint_clause(OracleParser.Drop_constraint_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_primary_key_or_unique_or_generic_clause}.
	 * @param ctx the parse tree
	 */
	void enterDrop_primary_key_or_unique_or_generic_clause(OracleParser.Drop_primary_key_or_unique_or_generic_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_primary_key_or_unique_or_generic_clause}.
	 * @param ctx the parse tree
	 */
	void exitDrop_primary_key_or_unique_or_generic_clause(OracleParser.Drop_primary_key_or_unique_or_generic_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_constraint}.
	 * @param ctx the parse tree
	 */
	void enterAdd_constraint(OracleParser.Add_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_constraint}.
	 * @param ctx the parse tree
	 */
	void exitAdd_constraint(OracleParser.Add_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#add_constraint_clause}.
	 * @param ctx the parse tree
	 */
	void enterAdd_constraint_clause(OracleParser.Add_constraint_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#add_constraint_clause}.
	 * @param ctx the parse tree
	 */
	void exitAdd_constraint_clause(OracleParser.Add_constraint_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#check_constraint}.
	 * @param ctx the parse tree
	 */
	void enterCheck_constraint(OracleParser.Check_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#check_constraint}.
	 * @param ctx the parse tree
	 */
	void exitCheck_constraint(OracleParser.Check_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#drop_constraint}.
	 * @param ctx the parse tree
	 */
	void enterDrop_constraint(OracleParser.Drop_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#drop_constraint}.
	 * @param ctx the parse tree
	 */
	void exitDrop_constraint(OracleParser.Drop_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#enable_constraint}.
	 * @param ctx the parse tree
	 */
	void enterEnable_constraint(OracleParser.Enable_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#enable_constraint}.
	 * @param ctx the parse tree
	 */
	void exitEnable_constraint(OracleParser.Enable_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#disable_constraint}.
	 * @param ctx the parse tree
	 */
	void enterDisable_constraint(OracleParser.Disable_constraintContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#disable_constraint}.
	 * @param ctx the parse tree
	 */
	void exitDisable_constraint(OracleParser.Disable_constraintContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#foreign_key_clause}.
	 * @param ctx the parse tree
	 */
	void enterForeign_key_clause(OracleParser.Foreign_key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#foreign_key_clause}.
	 * @param ctx the parse tree
	 */
	void exitForeign_key_clause(OracleParser.Foreign_key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#references_clause}.
	 * @param ctx the parse tree
	 */
	void enterReferences_clause(OracleParser.References_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#references_clause}.
	 * @param ctx the parse tree
	 */
	void exitReferences_clause(OracleParser.References_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#on_delete_clause}.
	 * @param ctx the parse tree
	 */
	void enterOn_delete_clause(OracleParser.On_delete_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#on_delete_clause}.
	 * @param ctx the parse tree
	 */
	void exitOn_delete_clause(OracleParser.On_delete_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unique_key_clause}.
	 * @param ctx the parse tree
	 */
	void enterUnique_key_clause(OracleParser.Unique_key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unique_key_clause}.
	 * @param ctx the parse tree
	 */
	void exitUnique_key_clause(OracleParser.Unique_key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#primary_key_clause}.
	 * @param ctx the parse tree
	 */
	void enterPrimary_key_clause(OracleParser.Primary_key_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#primary_key_clause}.
	 * @param ctx the parse tree
	 */
	void exitPrimary_key_clause(OracleParser.Primary_key_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#anonymous_block}.
	 * @param ctx the parse tree
	 */
	void enterAnonymous_block(OracleParser.Anonymous_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#anonymous_block}.
	 * @param ctx the parse tree
	 */
	void exitAnonymous_block(OracleParser.Anonymous_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#invoker_rights_clause}.
	 * @param ctx the parse tree
	 */
	void enterInvoker_rights_clause(OracleParser.Invoker_rights_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#invoker_rights_clause}.
	 * @param ctx the parse tree
	 */
	void exitInvoker_rights_clause(OracleParser.Invoker_rights_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#call_spec}.
	 * @param ctx the parse tree
	 */
	void enterCall_spec(OracleParser.Call_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#call_spec}.
	 * @param ctx the parse tree
	 */
	void exitCall_spec(OracleParser.Call_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#java_spec}.
	 * @param ctx the parse tree
	 */
	void enterJava_spec(OracleParser.Java_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#java_spec}.
	 * @param ctx the parse tree
	 */
	void exitJava_spec(OracleParser.Java_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#c_spec}.
	 * @param ctx the parse tree
	 */
	void enterC_spec(OracleParser.C_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#c_spec}.
	 * @param ctx the parse tree
	 */
	void exitC_spec(OracleParser.C_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#c_agent_in_clause}.
	 * @param ctx the parse tree
	 */
	void enterC_agent_in_clause(OracleParser.C_agent_in_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#c_agent_in_clause}.
	 * @param ctx the parse tree
	 */
	void exitC_agent_in_clause(OracleParser.C_agent_in_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#c_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void enterC_parameters_clause(OracleParser.C_parameters_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#c_parameters_clause}.
	 * @param ctx the parse tree
	 */
	void exitC_parameters_clause(OracleParser.C_parameters_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(OracleParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(OracleParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#default_value_part}.
	 * @param ctx the parse tree
	 */
	void enterDefault_value_part(OracleParser.Default_value_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#default_value_part}.
	 * @param ctx the parse tree
	 */
	void exitDefault_value_part(OracleParser.Default_value_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#seq_of_declare_specs}.
	 * @param ctx the parse tree
	 */
	void enterSeq_of_declare_specs(OracleParser.Seq_of_declare_specsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#seq_of_declare_specs}.
	 * @param ctx the parse tree
	 */
	void exitSeq_of_declare_specs(OracleParser.Seq_of_declare_specsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#declare_spec}.
	 * @param ctx the parse tree
	 */
	void enterDeclare_spec(OracleParser.Declare_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#declare_spec}.
	 * @param ctx the parse tree
	 */
	void exitDeclare_spec(OracleParser.Declare_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void enterVariable_declaration(OracleParser.Variable_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#variable_declaration}.
	 * @param ctx the parse tree
	 */
	void exitVariable_declaration(OracleParser.Variable_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subtype_declaration}.
	 * @param ctx the parse tree
	 */
	void enterSubtype_declaration(OracleParser.Subtype_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subtype_declaration}.
	 * @param ctx the parse tree
	 */
	void exitSubtype_declaration(OracleParser.Subtype_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cursor_declaration}.
	 * @param ctx the parse tree
	 */
	void enterCursor_declaration(OracleParser.Cursor_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cursor_declaration}.
	 * @param ctx the parse tree
	 */
	void exitCursor_declaration(OracleParser.Cursor_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#parameter_spec}.
	 * @param ctx the parse tree
	 */
	void enterParameter_spec(OracleParser.Parameter_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#parameter_spec}.
	 * @param ctx the parse tree
	 */
	void exitParameter_spec(OracleParser.Parameter_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#exception_declaration}.
	 * @param ctx the parse tree
	 */
	void enterException_declaration(OracleParser.Exception_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#exception_declaration}.
	 * @param ctx the parse tree
	 */
	void exitException_declaration(OracleParser.Exception_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pragma_declaration}.
	 * @param ctx the parse tree
	 */
	void enterPragma_declaration(OracleParser.Pragma_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pragma_declaration}.
	 * @param ctx the parse tree
	 */
	void exitPragma_declaration(OracleParser.Pragma_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#record_type_def}.
	 * @param ctx the parse tree
	 */
	void enterRecord_type_def(OracleParser.Record_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#record_type_def}.
	 * @param ctx the parse tree
	 */
	void exitRecord_type_def(OracleParser.Record_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#field_spec}.
	 * @param ctx the parse tree
	 */
	void enterField_spec(OracleParser.Field_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#field_spec}.
	 * @param ctx the parse tree
	 */
	void exitField_spec(OracleParser.Field_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#ref_cursor_type_def}.
	 * @param ctx the parse tree
	 */
	void enterRef_cursor_type_def(OracleParser.Ref_cursor_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#ref_cursor_type_def}.
	 * @param ctx the parse tree
	 */
	void exitRef_cursor_type_def(OracleParser.Ref_cursor_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_declaration}.
	 * @param ctx the parse tree
	 */
	void enterType_declaration(OracleParser.Type_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_declaration}.
	 * @param ctx the parse tree
	 */
	void exitType_declaration(OracleParser.Type_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_type_def}.
	 * @param ctx the parse tree
	 */
	void enterTable_type_def(OracleParser.Table_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_type_def}.
	 * @param ctx the parse tree
	 */
	void exitTable_type_def(OracleParser.Table_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_indexed_by_part}.
	 * @param ctx the parse tree
	 */
	void enterTable_indexed_by_part(OracleParser.Table_indexed_by_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_indexed_by_part}.
	 * @param ctx the parse tree
	 */
	void exitTable_indexed_by_part(OracleParser.Table_indexed_by_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#varray_type_def}.
	 * @param ctx the parse tree
	 */
	void enterVarray_type_def(OracleParser.Varray_type_defContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#varray_type_def}.
	 * @param ctx the parse tree
	 */
	void exitVarray_type_def(OracleParser.Varray_type_defContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#seq_of_statements}.
	 * @param ctx the parse tree
	 */
	void enterSeq_of_statements(OracleParser.Seq_of_statementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#seq_of_statements}.
	 * @param ctx the parse tree
	 */
	void exitSeq_of_statements(OracleParser.Seq_of_statementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#label_declaration}.
	 * @param ctx the parse tree
	 */
	void enterLabel_declaration(OracleParser.Label_declarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#label_declaration}.
	 * @param ctx the parse tree
	 */
	void exitLabel_declaration(OracleParser.Label_declarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(OracleParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(OracleParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#swallow_to_semi}.
	 * @param ctx the parse tree
	 */
	void enterSwallow_to_semi(OracleParser.Swallow_to_semiContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#swallow_to_semi}.
	 * @param ctx the parse tree
	 */
	void exitSwallow_to_semi(OracleParser.Swallow_to_semiContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#assignment_statement}.
	 * @param ctx the parse tree
	 */
	void enterAssignment_statement(OracleParser.Assignment_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#assignment_statement}.
	 * @param ctx the parse tree
	 */
	void exitAssignment_statement(OracleParser.Assignment_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#continue_statement}.
	 * @param ctx the parse tree
	 */
	void enterContinue_statement(OracleParser.Continue_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#continue_statement}.
	 * @param ctx the parse tree
	 */
	void exitContinue_statement(OracleParser.Continue_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#exit_statement}.
	 * @param ctx the parse tree
	 */
	void enterExit_statement(OracleParser.Exit_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#exit_statement}.
	 * @param ctx the parse tree
	 */
	void exitExit_statement(OracleParser.Exit_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#goto_statement}.
	 * @param ctx the parse tree
	 */
	void enterGoto_statement(OracleParser.Goto_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#goto_statement}.
	 * @param ctx the parse tree
	 */
	void exitGoto_statement(OracleParser.Goto_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void enterIf_statement(OracleParser.If_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#if_statement}.
	 * @param ctx the parse tree
	 */
	void exitIf_statement(OracleParser.If_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#elsif_part}.
	 * @param ctx the parse tree
	 */
	void enterElsif_part(OracleParser.Elsif_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#elsif_part}.
	 * @param ctx the parse tree
	 */
	void exitElsif_part(OracleParser.Elsif_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#else_part}.
	 * @param ctx the parse tree
	 */
	void enterElse_part(OracleParser.Else_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#else_part}.
	 * @param ctx the parse tree
	 */
	void exitElse_part(OracleParser.Else_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#loop_statement}.
	 * @param ctx the parse tree
	 */
	void enterLoop_statement(OracleParser.Loop_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#loop_statement}.
	 * @param ctx the parse tree
	 */
	void exitLoop_statement(OracleParser.Loop_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cursor_loop_param}.
	 * @param ctx the parse tree
	 */
	void enterCursor_loop_param(OracleParser.Cursor_loop_paramContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cursor_loop_param}.
	 * @param ctx the parse tree
	 */
	void exitCursor_loop_param(OracleParser.Cursor_loop_paramContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#forall_statement}.
	 * @param ctx the parse tree
	 */
	void enterForall_statement(OracleParser.Forall_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#forall_statement}.
	 * @param ctx the parse tree
	 */
	void exitForall_statement(OracleParser.Forall_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#bounds_clause}.
	 * @param ctx the parse tree
	 */
	void enterBounds_clause(OracleParser.Bounds_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#bounds_clause}.
	 * @param ctx the parse tree
	 */
	void exitBounds_clause(OracleParser.Bounds_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#between_bound}.
	 * @param ctx the parse tree
	 */
	void enterBetween_bound(OracleParser.Between_boundContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#between_bound}.
	 * @param ctx the parse tree
	 */
	void exitBetween_bound(OracleParser.Between_boundContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lower_bound}.
	 * @param ctx the parse tree
	 */
	void enterLower_bound(OracleParser.Lower_boundContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lower_bound}.
	 * @param ctx the parse tree
	 */
	void exitLower_bound(OracleParser.Lower_boundContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#upper_bound}.
	 * @param ctx the parse tree
	 */
	void enterUpper_bound(OracleParser.Upper_boundContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#upper_bound}.
	 * @param ctx the parse tree
	 */
	void exitUpper_bound(OracleParser.Upper_boundContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#null_statement}.
	 * @param ctx the parse tree
	 */
	void enterNull_statement(OracleParser.Null_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#null_statement}.
	 * @param ctx the parse tree
	 */
	void exitNull_statement(OracleParser.Null_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#raise_statement}.
	 * @param ctx the parse tree
	 */
	void enterRaise_statement(OracleParser.Raise_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#raise_statement}.
	 * @param ctx the parse tree
	 */
	void exitRaise_statement(OracleParser.Raise_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void enterReturn_statement(OracleParser.Return_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#return_statement}.
	 * @param ctx the parse tree
	 */
	void exitReturn_statement(OracleParser.Return_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_call}.
	 * @param ctx the parse tree
	 */
	void enterFunction_call(OracleParser.Function_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_call}.
	 * @param ctx the parse tree
	 */
	void exitFunction_call(OracleParser.Function_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#procedure_call}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_call(OracleParser.Procedure_callContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#procedure_call}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_call(OracleParser.Procedure_callContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pipe_row_statement}.
	 * @param ctx the parse tree
	 */
	void enterPipe_row_statement(OracleParser.Pipe_row_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pipe_row_statement}.
	 * @param ctx the parse tree
	 */
	void exitPipe_row_statement(OracleParser.Pipe_row_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#body}.
	 * @param ctx the parse tree
	 */
	void enterBody(OracleParser.BodyContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#body}.
	 * @param ctx the parse tree
	 */
	void exitBody(OracleParser.BodyContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#exception_handler}.
	 * @param ctx the parse tree
	 */
	void enterException_handler(OracleParser.Exception_handlerContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#exception_handler}.
	 * @param ctx the parse tree
	 */
	void exitException_handler(OracleParser.Exception_handlerContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#trigger_block}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_block(OracleParser.Trigger_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#trigger_block}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_block(OracleParser.Trigger_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#block}.
	 * @param ctx the parse tree
	 */
	void enterBlock(OracleParser.BlockContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#block}.
	 * @param ctx the parse tree
	 */
	void exitBlock(OracleParser.BlockContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sql_statement}.
	 * @param ctx the parse tree
	 */
	void enterSql_statement(OracleParser.Sql_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sql_statement}.
	 * @param ctx the parse tree
	 */
	void exitSql_statement(OracleParser.Sql_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#execute_immediate}.
	 * @param ctx the parse tree
	 */
	void enterExecute_immediate(OracleParser.Execute_immediateContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#execute_immediate}.
	 * @param ctx the parse tree
	 */
	void exitExecute_immediate(OracleParser.Execute_immediateContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dynamic_returning_clause}.
	 * @param ctx the parse tree
	 */
	void enterDynamic_returning_clause(OracleParser.Dynamic_returning_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dynamic_returning_clause}.
	 * @param ctx the parse tree
	 */
	void exitDynamic_returning_clause(OracleParser.Dynamic_returning_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#data_manipulation_language_statements}.
	 * @param ctx the parse tree
	 */
	void enterData_manipulation_language_statements(OracleParser.Data_manipulation_language_statementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#data_manipulation_language_statements}.
	 * @param ctx the parse tree
	 */
	void exitData_manipulation_language_statements(OracleParser.Data_manipulation_language_statementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cursor_manipulation_statements}.
	 * @param ctx the parse tree
	 */
	void enterCursor_manipulation_statements(OracleParser.Cursor_manipulation_statementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cursor_manipulation_statements}.
	 * @param ctx the parse tree
	 */
	void exitCursor_manipulation_statements(OracleParser.Cursor_manipulation_statementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#close_statement}.
	 * @param ctx the parse tree
	 */
	void enterClose_statement(OracleParser.Close_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#close_statement}.
	 * @param ctx the parse tree
	 */
	void exitClose_statement(OracleParser.Close_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#open_statement}.
	 * @param ctx the parse tree
	 */
	void enterOpen_statement(OracleParser.Open_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#open_statement}.
	 * @param ctx the parse tree
	 */
	void exitOpen_statement(OracleParser.Open_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#fetch_statement}.
	 * @param ctx the parse tree
	 */
	void enterFetch_statement(OracleParser.Fetch_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#fetch_statement}.
	 * @param ctx the parse tree
	 */
	void exitFetch_statement(OracleParser.Fetch_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#open_for_statement}.
	 * @param ctx the parse tree
	 */
	void enterOpen_for_statement(OracleParser.Open_for_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#open_for_statement}.
	 * @param ctx the parse tree
	 */
	void exitOpen_for_statement(OracleParser.Open_for_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#transaction_control_statements}.
	 * @param ctx the parse tree
	 */
	void enterTransaction_control_statements(OracleParser.Transaction_control_statementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#transaction_control_statements}.
	 * @param ctx the parse tree
	 */
	void exitTransaction_control_statements(OracleParser.Transaction_control_statementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#set_transaction_command}.
	 * @param ctx the parse tree
	 */
	void enterSet_transaction_command(OracleParser.Set_transaction_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#set_transaction_command}.
	 * @param ctx the parse tree
	 */
	void exitSet_transaction_command(OracleParser.Set_transaction_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#set_constraint_command}.
	 * @param ctx the parse tree
	 */
	void enterSet_constraint_command(OracleParser.Set_constraint_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#set_constraint_command}.
	 * @param ctx the parse tree
	 */
	void exitSet_constraint_command(OracleParser.Set_constraint_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#commit_statement}.
	 * @param ctx the parse tree
	 */
	void enterCommit_statement(OracleParser.Commit_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#commit_statement}.
	 * @param ctx the parse tree
	 */
	void exitCommit_statement(OracleParser.Commit_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#write_clause}.
	 * @param ctx the parse tree
	 */
	void enterWrite_clause(OracleParser.Write_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#write_clause}.
	 * @param ctx the parse tree
	 */
	void exitWrite_clause(OracleParser.Write_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rollback_statement}.
	 * @param ctx the parse tree
	 */
	void enterRollback_statement(OracleParser.Rollback_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rollback_statement}.
	 * @param ctx the parse tree
	 */
	void exitRollback_statement(OracleParser.Rollback_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#savepoint_statement}.
	 * @param ctx the parse tree
	 */
	void enterSavepoint_statement(OracleParser.Savepoint_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#savepoint_statement}.
	 * @param ctx the parse tree
	 */
	void exitSavepoint_statement(OracleParser.Savepoint_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#explain_statement}.
	 * @param ctx the parse tree
	 */
	void enterExplain_statement(OracleParser.Explain_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#explain_statement}.
	 * @param ctx the parse tree
	 */
	void exitExplain_statement(OracleParser.Explain_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#select_only_statement}.
	 * @param ctx the parse tree
	 */
	void enterSelect_only_statement(OracleParser.Select_only_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#select_only_statement}.
	 * @param ctx the parse tree
	 */
	void exitSelect_only_statement(OracleParser.Select_only_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#select_statement}.
	 * @param ctx the parse tree
	 */
	void enterSelect_statement(OracleParser.Select_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#select_statement}.
	 * @param ctx the parse tree
	 */
	void exitSelect_statement(OracleParser.Select_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subquery_factoring_clause}.
	 * @param ctx the parse tree
	 */
	void enterSubquery_factoring_clause(OracleParser.Subquery_factoring_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subquery_factoring_clause}.
	 * @param ctx the parse tree
	 */
	void exitSubquery_factoring_clause(OracleParser.Subquery_factoring_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#factoring_element}.
	 * @param ctx the parse tree
	 */
	void enterFactoring_element(OracleParser.Factoring_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#factoring_element}.
	 * @param ctx the parse tree
	 */
	void exitFactoring_element(OracleParser.Factoring_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#search_clause}.
	 * @param ctx the parse tree
	 */
	void enterSearch_clause(OracleParser.Search_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#search_clause}.
	 * @param ctx the parse tree
	 */
	void exitSearch_clause(OracleParser.Search_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cycle_clause}.
	 * @param ctx the parse tree
	 */
	void enterCycle_clause(OracleParser.Cycle_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cycle_clause}.
	 * @param ctx the parse tree
	 */
	void exitCycle_clause(OracleParser.Cycle_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subquery}.
	 * @param ctx the parse tree
	 */
	void enterSubquery(OracleParser.SubqueryContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subquery}.
	 * @param ctx the parse tree
	 */
	void exitSubquery(OracleParser.SubqueryContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subquery_basic_elements}.
	 * @param ctx the parse tree
	 */
	void enterSubquery_basic_elements(OracleParser.Subquery_basic_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subquery_basic_elements}.
	 * @param ctx the parse tree
	 */
	void exitSubquery_basic_elements(OracleParser.Subquery_basic_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subquery_operation_part}.
	 * @param ctx the parse tree
	 */
	void enterSubquery_operation_part(OracleParser.Subquery_operation_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subquery_operation_part}.
	 * @param ctx the parse tree
	 */
	void exitSubquery_operation_part(OracleParser.Subquery_operation_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#query_block}.
	 * @param ctx the parse tree
	 */
	void enterQuery_block(OracleParser.Query_blockContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#query_block}.
	 * @param ctx the parse tree
	 */
	void exitQuery_block(OracleParser.Query_blockContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#selected_list}.
	 * @param ctx the parse tree
	 */
	void enterSelected_list(OracleParser.Selected_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#selected_list}.
	 * @param ctx the parse tree
	 */
	void exitSelected_list(OracleParser.Selected_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#from_clause}.
	 * @param ctx the parse tree
	 */
	void enterFrom_clause(OracleParser.From_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#from_clause}.
	 * @param ctx the parse tree
	 */
	void exitFrom_clause(OracleParser.From_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#select_list_elements}.
	 * @param ctx the parse tree
	 */
	void enterSelect_list_elements(OracleParser.Select_list_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#select_list_elements}.
	 * @param ctx the parse tree
	 */
	void exitSelect_list_elements(OracleParser.Select_list_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_ref_list}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_list(OracleParser.Table_ref_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_ref_list}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_list(OracleParser.Table_ref_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_ref}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref(OracleParser.Table_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_ref}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref(OracleParser.Table_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_ref_aux}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_aux(OracleParser.Table_ref_auxContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_ref_aux}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_aux(OracleParser.Table_ref_auxContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table_ref_aux_internal_one}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_aux_internal_one(OracleParser.Table_ref_aux_internal_oneContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table_ref_aux_internal_one}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_aux_internal_one(OracleParser.Table_ref_aux_internal_oneContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table_ref_aux_internal_two}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_aux_internal_two(OracleParser.Table_ref_aux_internal_twoContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table_ref_aux_internal_two}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_aux_internal_two(OracleParser.Table_ref_aux_internal_twoContext ctx);
	/**
	 * Enter a parse tree produced by the {@code table_ref_aux_internal_three}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void enterTable_ref_aux_internal_three(OracleParser.Table_ref_aux_internal_threeContext ctx);
	/**
	 * Exit a parse tree produced by the {@code table_ref_aux_internal_three}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 */
	void exitTable_ref_aux_internal_three(OracleParser.Table_ref_aux_internal_threeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#join_clause}.
	 * @param ctx the parse tree
	 */
	void enterJoin_clause(OracleParser.Join_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#join_clause}.
	 * @param ctx the parse tree
	 */
	void exitJoin_clause(OracleParser.Join_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#join_on_part}.
	 * @param ctx the parse tree
	 */
	void enterJoin_on_part(OracleParser.Join_on_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#join_on_part}.
	 * @param ctx the parse tree
	 */
	void exitJoin_on_part(OracleParser.Join_on_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#join_using_part}.
	 * @param ctx the parse tree
	 */
	void enterJoin_using_part(OracleParser.Join_using_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#join_using_part}.
	 * @param ctx the parse tree
	 */
	void exitJoin_using_part(OracleParser.Join_using_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#outer_join_type}.
	 * @param ctx the parse tree
	 */
	void enterOuter_join_type(OracleParser.Outer_join_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#outer_join_type}.
	 * @param ctx the parse tree
	 */
	void exitOuter_join_type(OracleParser.Outer_join_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#query_partition_clause}.
	 * @param ctx the parse tree
	 */
	void enterQuery_partition_clause(OracleParser.Query_partition_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#query_partition_clause}.
	 * @param ctx the parse tree
	 */
	void exitQuery_partition_clause(OracleParser.Query_partition_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#flashback_query_clause}.
	 * @param ctx the parse tree
	 */
	void enterFlashback_query_clause(OracleParser.Flashback_query_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#flashback_query_clause}.
	 * @param ctx the parse tree
	 */
	void exitFlashback_query_clause(OracleParser.Flashback_query_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pivot_clause}.
	 * @param ctx the parse tree
	 */
	void enterPivot_clause(OracleParser.Pivot_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pivot_clause}.
	 * @param ctx the parse tree
	 */
	void exitPivot_clause(OracleParser.Pivot_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pivot_element}.
	 * @param ctx the parse tree
	 */
	void enterPivot_element(OracleParser.Pivot_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pivot_element}.
	 * @param ctx the parse tree
	 */
	void exitPivot_element(OracleParser.Pivot_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pivot_for_clause}.
	 * @param ctx the parse tree
	 */
	void enterPivot_for_clause(OracleParser.Pivot_for_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pivot_for_clause}.
	 * @param ctx the parse tree
	 */
	void exitPivot_for_clause(OracleParser.Pivot_for_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void enterPivot_in_clause(OracleParser.Pivot_in_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void exitPivot_in_clause(OracleParser.Pivot_in_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pivot_in_clause_element}.
	 * @param ctx the parse tree
	 */
	void enterPivot_in_clause_element(OracleParser.Pivot_in_clause_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pivot_in_clause_element}.
	 * @param ctx the parse tree
	 */
	void exitPivot_in_clause_element(OracleParser.Pivot_in_clause_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#pivot_in_clause_elements}.
	 * @param ctx the parse tree
	 */
	void enterPivot_in_clause_elements(OracleParser.Pivot_in_clause_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#pivot_in_clause_elements}.
	 * @param ctx the parse tree
	 */
	void exitPivot_in_clause_elements(OracleParser.Pivot_in_clause_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unpivot_clause}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_clause(OracleParser.Unpivot_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unpivot_clause}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_clause(OracleParser.Unpivot_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unpivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_in_clause(OracleParser.Unpivot_in_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unpivot_in_clause}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_in_clause(OracleParser.Unpivot_in_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unpivot_in_elements}.
	 * @param ctx the parse tree
	 */
	void enterUnpivot_in_elements(OracleParser.Unpivot_in_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unpivot_in_elements}.
	 * @param ctx the parse tree
	 */
	void exitUnpivot_in_elements(OracleParser.Unpivot_in_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#hierarchical_query_clause}.
	 * @param ctx the parse tree
	 */
	void enterHierarchical_query_clause(OracleParser.Hierarchical_query_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#hierarchical_query_clause}.
	 * @param ctx the parse tree
	 */
	void exitHierarchical_query_clause(OracleParser.Hierarchical_query_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#start_part}.
	 * @param ctx the parse tree
	 */
	void enterStart_part(OracleParser.Start_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#start_part}.
	 * @param ctx the parse tree
	 */
	void exitStart_part(OracleParser.Start_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#group_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterGroup_by_clause(OracleParser.Group_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#group_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitGroup_by_clause(OracleParser.Group_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#group_by_elements}.
	 * @param ctx the parse tree
	 */
	void enterGroup_by_elements(OracleParser.Group_by_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#group_by_elements}.
	 * @param ctx the parse tree
	 */
	void exitGroup_by_elements(OracleParser.Group_by_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rollup_cube_clause}.
	 * @param ctx the parse tree
	 */
	void enterRollup_cube_clause(OracleParser.Rollup_cube_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rollup_cube_clause}.
	 * @param ctx the parse tree
	 */
	void exitRollup_cube_clause(OracleParser.Rollup_cube_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#grouping_sets_clause}.
	 * @param ctx the parse tree
	 */
	void enterGrouping_sets_clause(OracleParser.Grouping_sets_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#grouping_sets_clause}.
	 * @param ctx the parse tree
	 */
	void exitGrouping_sets_clause(OracleParser.Grouping_sets_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#grouping_sets_elements}.
	 * @param ctx the parse tree
	 */
	void enterGrouping_sets_elements(OracleParser.Grouping_sets_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#grouping_sets_elements}.
	 * @param ctx the parse tree
	 */
	void exitGrouping_sets_elements(OracleParser.Grouping_sets_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#having_clause}.
	 * @param ctx the parse tree
	 */
	void enterHaving_clause(OracleParser.Having_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#having_clause}.
	 * @param ctx the parse tree
	 */
	void exitHaving_clause(OracleParser.Having_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_clause}.
	 * @param ctx the parse tree
	 */
	void enterModel_clause(OracleParser.Model_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_clause}.
	 * @param ctx the parse tree
	 */
	void exitModel_clause(OracleParser.Model_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cell_reference_options}.
	 * @param ctx the parse tree
	 */
	void enterCell_reference_options(OracleParser.Cell_reference_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cell_reference_options}.
	 * @param ctx the parse tree
	 */
	void exitCell_reference_options(OracleParser.Cell_reference_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#return_rows_clause}.
	 * @param ctx the parse tree
	 */
	void enterReturn_rows_clause(OracleParser.Return_rows_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#return_rows_clause}.
	 * @param ctx the parse tree
	 */
	void exitReturn_rows_clause(OracleParser.Return_rows_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#reference_model}.
	 * @param ctx the parse tree
	 */
	void enterReference_model(OracleParser.Reference_modelContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#reference_model}.
	 * @param ctx the parse tree
	 */
	void exitReference_model(OracleParser.Reference_modelContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#main_model}.
	 * @param ctx the parse tree
	 */
	void enterMain_model(OracleParser.Main_modelContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#main_model}.
	 * @param ctx the parse tree
	 */
	void exitMain_model(OracleParser.Main_modelContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_column_clauses}.
	 * @param ctx the parse tree
	 */
	void enterModel_column_clauses(OracleParser.Model_column_clausesContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_column_clauses}.
	 * @param ctx the parse tree
	 */
	void exitModel_column_clauses(OracleParser.Model_column_clausesContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_column_partition_part}.
	 * @param ctx the parse tree
	 */
	void enterModel_column_partition_part(OracleParser.Model_column_partition_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_column_partition_part}.
	 * @param ctx the parse tree
	 */
	void exitModel_column_partition_part(OracleParser.Model_column_partition_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_column_list}.
	 * @param ctx the parse tree
	 */
	void enterModel_column_list(OracleParser.Model_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_column_list}.
	 * @param ctx the parse tree
	 */
	void exitModel_column_list(OracleParser.Model_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_column}.
	 * @param ctx the parse tree
	 */
	void enterModel_column(OracleParser.Model_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_column}.
	 * @param ctx the parse tree
	 */
	void exitModel_column(OracleParser.Model_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_rules_clause}.
	 * @param ctx the parse tree
	 */
	void enterModel_rules_clause(OracleParser.Model_rules_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_rules_clause}.
	 * @param ctx the parse tree
	 */
	void exitModel_rules_clause(OracleParser.Model_rules_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_rules_part}.
	 * @param ctx the parse tree
	 */
	void enterModel_rules_part(OracleParser.Model_rules_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_rules_part}.
	 * @param ctx the parse tree
	 */
	void exitModel_rules_part(OracleParser.Model_rules_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_rules_element}.
	 * @param ctx the parse tree
	 */
	void enterModel_rules_element(OracleParser.Model_rules_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_rules_element}.
	 * @param ctx the parse tree
	 */
	void exitModel_rules_element(OracleParser.Model_rules_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cell_assignment}.
	 * @param ctx the parse tree
	 */
	void enterCell_assignment(OracleParser.Cell_assignmentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cell_assignment}.
	 * @param ctx the parse tree
	 */
	void exitCell_assignment(OracleParser.Cell_assignmentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_iterate_clause}.
	 * @param ctx the parse tree
	 */
	void enterModel_iterate_clause(OracleParser.Model_iterate_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_iterate_clause}.
	 * @param ctx the parse tree
	 */
	void exitModel_iterate_clause(OracleParser.Model_iterate_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#until_part}.
	 * @param ctx the parse tree
	 */
	void enterUntil_part(OracleParser.Until_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#until_part}.
	 * @param ctx the parse tree
	 */
	void exitUntil_part(OracleParser.Until_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#order_by_clause}.
	 * @param ctx the parse tree
	 */
	void enterOrder_by_clause(OracleParser.Order_by_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#order_by_clause}.
	 * @param ctx the parse tree
	 */
	void exitOrder_by_clause(OracleParser.Order_by_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#order_by_elements}.
	 * @param ctx the parse tree
	 */
	void enterOrder_by_elements(OracleParser.Order_by_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#order_by_elements}.
	 * @param ctx the parse tree
	 */
	void exitOrder_by_elements(OracleParser.Order_by_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#offset_clause}.
	 * @param ctx the parse tree
	 */
	void enterOffset_clause(OracleParser.Offset_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#offset_clause}.
	 * @param ctx the parse tree
	 */
	void exitOffset_clause(OracleParser.Offset_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#fetch_clause}.
	 * @param ctx the parse tree
	 */
	void enterFetch_clause(OracleParser.Fetch_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#fetch_clause}.
	 * @param ctx the parse tree
	 */
	void exitFetch_clause(OracleParser.Fetch_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#for_update_clause}.
	 * @param ctx the parse tree
	 */
	void enterFor_update_clause(OracleParser.For_update_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#for_update_clause}.
	 * @param ctx the parse tree
	 */
	void exitFor_update_clause(OracleParser.For_update_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#for_update_of_part}.
	 * @param ctx the parse tree
	 */
	void enterFor_update_of_part(OracleParser.For_update_of_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#for_update_of_part}.
	 * @param ctx the parse tree
	 */
	void exitFor_update_of_part(OracleParser.For_update_of_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#for_update_options}.
	 * @param ctx the parse tree
	 */
	void enterFor_update_options(OracleParser.For_update_optionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#for_update_options}.
	 * @param ctx the parse tree
	 */
	void exitFor_update_options(OracleParser.For_update_optionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#update_statement}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_statement(OracleParser.Update_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#update_statement}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_statement(OracleParser.Update_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#update_set_clause}.
	 * @param ctx the parse tree
	 */
	void enterUpdate_set_clause(OracleParser.Update_set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#update_set_clause}.
	 * @param ctx the parse tree
	 */
	void exitUpdate_set_clause(OracleParser.Update_set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_based_update_set_clause}.
	 * @param ctx the parse tree
	 */
	void enterColumn_based_update_set_clause(OracleParser.Column_based_update_set_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_based_update_set_clause}.
	 * @param ctx the parse tree
	 */
	void exitColumn_based_update_set_clause(OracleParser.Column_based_update_set_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#delete_statement}.
	 * @param ctx the parse tree
	 */
	void enterDelete_statement(OracleParser.Delete_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#delete_statement}.
	 * @param ctx the parse tree
	 */
	void exitDelete_statement(OracleParser.Delete_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#insert_statement}.
	 * @param ctx the parse tree
	 */
	void enterInsert_statement(OracleParser.Insert_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#insert_statement}.
	 * @param ctx the parse tree
	 */
	void exitInsert_statement(OracleParser.Insert_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#single_table_insert}.
	 * @param ctx the parse tree
	 */
	void enterSingle_table_insert(OracleParser.Single_table_insertContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#single_table_insert}.
	 * @param ctx the parse tree
	 */
	void exitSingle_table_insert(OracleParser.Single_table_insertContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#multi_table_insert}.
	 * @param ctx the parse tree
	 */
	void enterMulti_table_insert(OracleParser.Multi_table_insertContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#multi_table_insert}.
	 * @param ctx the parse tree
	 */
	void exitMulti_table_insert(OracleParser.Multi_table_insertContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#multi_table_element}.
	 * @param ctx the parse tree
	 */
	void enterMulti_table_element(OracleParser.Multi_table_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#multi_table_element}.
	 * @param ctx the parse tree
	 */
	void exitMulti_table_element(OracleParser.Multi_table_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#conditional_insert_clause}.
	 * @param ctx the parse tree
	 */
	void enterConditional_insert_clause(OracleParser.Conditional_insert_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#conditional_insert_clause}.
	 * @param ctx the parse tree
	 */
	void exitConditional_insert_clause(OracleParser.Conditional_insert_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#conditional_insert_when_part}.
	 * @param ctx the parse tree
	 */
	void enterConditional_insert_when_part(OracleParser.Conditional_insert_when_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#conditional_insert_when_part}.
	 * @param ctx the parse tree
	 */
	void exitConditional_insert_when_part(OracleParser.Conditional_insert_when_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#conditional_insert_else_part}.
	 * @param ctx the parse tree
	 */
	void enterConditional_insert_else_part(OracleParser.Conditional_insert_else_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#conditional_insert_else_part}.
	 * @param ctx the parse tree
	 */
	void exitConditional_insert_else_part(OracleParser.Conditional_insert_else_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#insert_into_clause}.
	 * @param ctx the parse tree
	 */
	void enterInsert_into_clause(OracleParser.Insert_into_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#insert_into_clause}.
	 * @param ctx the parse tree
	 */
	void exitInsert_into_clause(OracleParser.Insert_into_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#values_clause}.
	 * @param ctx the parse tree
	 */
	void enterValues_clause(OracleParser.Values_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#values_clause}.
	 * @param ctx the parse tree
	 */
	void exitValues_clause(OracleParser.Values_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#merge_statement}.
	 * @param ctx the parse tree
	 */
	void enterMerge_statement(OracleParser.Merge_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#merge_statement}.
	 * @param ctx the parse tree
	 */
	void exitMerge_statement(OracleParser.Merge_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#merge_update_clause}.
	 * @param ctx the parse tree
	 */
	void enterMerge_update_clause(OracleParser.Merge_update_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#merge_update_clause}.
	 * @param ctx the parse tree
	 */
	void exitMerge_update_clause(OracleParser.Merge_update_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#merge_element}.
	 * @param ctx the parse tree
	 */
	void enterMerge_element(OracleParser.Merge_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#merge_element}.
	 * @param ctx the parse tree
	 */
	void exitMerge_element(OracleParser.Merge_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#merge_update_delete_part}.
	 * @param ctx the parse tree
	 */
	void enterMerge_update_delete_part(OracleParser.Merge_update_delete_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#merge_update_delete_part}.
	 * @param ctx the parse tree
	 */
	void exitMerge_update_delete_part(OracleParser.Merge_update_delete_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#merge_insert_clause}.
	 * @param ctx the parse tree
	 */
	void enterMerge_insert_clause(OracleParser.Merge_insert_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#merge_insert_clause}.
	 * @param ctx the parse tree
	 */
	void exitMerge_insert_clause(OracleParser.Merge_insert_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#selected_tableview}.
	 * @param ctx the parse tree
	 */
	void enterSelected_tableview(OracleParser.Selected_tableviewContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#selected_tableview}.
	 * @param ctx the parse tree
	 */
	void exitSelected_tableview(OracleParser.Selected_tableviewContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lock_table_statement}.
	 * @param ctx the parse tree
	 */
	void enterLock_table_statement(OracleParser.Lock_table_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lock_table_statement}.
	 * @param ctx the parse tree
	 */
	void exitLock_table_statement(OracleParser.Lock_table_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#wait_nowait_part}.
	 * @param ctx the parse tree
	 */
	void enterWait_nowait_part(OracleParser.Wait_nowait_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#wait_nowait_part}.
	 * @param ctx the parse tree
	 */
	void exitWait_nowait_part(OracleParser.Wait_nowait_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lock_table_element}.
	 * @param ctx the parse tree
	 */
	void enterLock_table_element(OracleParser.Lock_table_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lock_table_element}.
	 * @param ctx the parse tree
	 */
	void exitLock_table_element(OracleParser.Lock_table_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#lock_mode}.
	 * @param ctx the parse tree
	 */
	void enterLock_mode(OracleParser.Lock_modeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#lock_mode}.
	 * @param ctx the parse tree
	 */
	void exitLock_mode(OracleParser.Lock_modeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#general_table_ref}.
	 * @param ctx the parse tree
	 */
	void enterGeneral_table_ref(OracleParser.General_table_refContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#general_table_ref}.
	 * @param ctx the parse tree
	 */
	void exitGeneral_table_ref(OracleParser.General_table_refContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#static_returning_clause}.
	 * @param ctx the parse tree
	 */
	void enterStatic_returning_clause(OracleParser.Static_returning_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#static_returning_clause}.
	 * @param ctx the parse tree
	 */
	void exitStatic_returning_clause(OracleParser.Static_returning_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#error_logging_clause}.
	 * @param ctx the parse tree
	 */
	void enterError_logging_clause(OracleParser.Error_logging_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#error_logging_clause}.
	 * @param ctx the parse tree
	 */
	void exitError_logging_clause(OracleParser.Error_logging_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#error_logging_into_part}.
	 * @param ctx the parse tree
	 */
	void enterError_logging_into_part(OracleParser.Error_logging_into_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#error_logging_into_part}.
	 * @param ctx the parse tree
	 */
	void exitError_logging_into_part(OracleParser.Error_logging_into_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#error_logging_reject_part}.
	 * @param ctx the parse tree
	 */
	void enterError_logging_reject_part(OracleParser.Error_logging_reject_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#error_logging_reject_part}.
	 * @param ctx the parse tree
	 */
	void exitError_logging_reject_part(OracleParser.Error_logging_reject_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dml_table_expression_clause}.
	 * @param ctx the parse tree
	 */
	void enterDml_table_expression_clause(OracleParser.Dml_table_expression_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dml_table_expression_clause}.
	 * @param ctx the parse tree
	 */
	void exitDml_table_expression_clause(OracleParser.Dml_table_expression_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_collection_expression}.
	 * @param ctx the parse tree
	 */
	void enterTable_collection_expression(OracleParser.Table_collection_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_collection_expression}.
	 * @param ctx the parse tree
	 */
	void exitTable_collection_expression(OracleParser.Table_collection_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#subquery_restriction_clause}.
	 * @param ctx the parse tree
	 */
	void enterSubquery_restriction_clause(OracleParser.Subquery_restriction_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#subquery_restriction_clause}.
	 * @param ctx the parse tree
	 */
	void exitSubquery_restriction_clause(OracleParser.Subquery_restriction_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sample_clause}.
	 * @param ctx the parse tree
	 */
	void enterSample_clause(OracleParser.Sample_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sample_clause}.
	 * @param ctx the parse tree
	 */
	void exitSample_clause(OracleParser.Sample_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#seed_part}.
	 * @param ctx the parse tree
	 */
	void enterSeed_part(OracleParser.Seed_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#seed_part}.
	 * @param ctx the parse tree
	 */
	void exitSeed_part(OracleParser.Seed_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#condition}.
	 * @param ctx the parse tree
	 */
	void enterCondition(OracleParser.ConditionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#condition}.
	 * @param ctx the parse tree
	 */
	void exitCondition(OracleParser.ConditionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#expressions}.
	 * @param ctx the parse tree
	 */
	void enterExpressions(OracleParser.ExpressionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#expressions}.
	 * @param ctx the parse tree
	 */
	void exitExpressions(OracleParser.ExpressionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(OracleParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(OracleParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cursor_expression}.
	 * @param ctx the parse tree
	 */
	void enterCursor_expression(OracleParser.Cursor_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cursor_expression}.
	 * @param ctx the parse tree
	 */
	void exitCursor_expression(OracleParser.Cursor_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#logical_expression}.
	 * @param ctx the parse tree
	 */
	void enterLogical_expression(OracleParser.Logical_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#logical_expression}.
	 * @param ctx the parse tree
	 */
	void exitLogical_expression(OracleParser.Logical_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unary_logical_expression}.
	 * @param ctx the parse tree
	 */
	void enterUnary_logical_expression(OracleParser.Unary_logical_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unary_logical_expression}.
	 * @param ctx the parse tree
	 */
	void exitUnary_logical_expression(OracleParser.Unary_logical_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#logical_operation}.
	 * @param ctx the parse tree
	 */
	void enterLogical_operation(OracleParser.Logical_operationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#logical_operation}.
	 * @param ctx the parse tree
	 */
	void exitLogical_operation(OracleParser.Logical_operationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#multiset_expression}.
	 * @param ctx the parse tree
	 */
	void enterMultiset_expression(OracleParser.Multiset_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#multiset_expression}.
	 * @param ctx the parse tree
	 */
	void exitMultiset_expression(OracleParser.Multiset_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#relational_expression}.
	 * @param ctx the parse tree
	 */
	void enterRelational_expression(OracleParser.Relational_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#relational_expression}.
	 * @param ctx the parse tree
	 */
	void exitRelational_expression(OracleParser.Relational_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#compound_expression}.
	 * @param ctx the parse tree
	 */
	void enterCompound_expression(OracleParser.Compound_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#compound_expression}.
	 * @param ctx the parse tree
	 */
	void exitCompound_expression(OracleParser.Compound_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#relational_operator}.
	 * @param ctx the parse tree
	 */
	void enterRelational_operator(OracleParser.Relational_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#relational_operator}.
	 * @param ctx the parse tree
	 */
	void exitRelational_operator(OracleParser.Relational_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#in_elements}.
	 * @param ctx the parse tree
	 */
	void enterIn_elements(OracleParser.In_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#in_elements}.
	 * @param ctx the parse tree
	 */
	void exitIn_elements(OracleParser.In_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#between_elements}.
	 * @param ctx the parse tree
	 */
	void enterBetween_elements(OracleParser.Between_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#between_elements}.
	 * @param ctx the parse tree
	 */
	void exitBetween_elements(OracleParser.Between_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#concatenation}.
	 * @param ctx the parse tree
	 */
	void enterConcatenation(OracleParser.ConcatenationContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#concatenation}.
	 * @param ctx the parse tree
	 */
	void exitConcatenation(OracleParser.ConcatenationContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#interval_expression}.
	 * @param ctx the parse tree
	 */
	void enterInterval_expression(OracleParser.Interval_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#interval_expression}.
	 * @param ctx the parse tree
	 */
	void exitInterval_expression(OracleParser.Interval_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_expression}.
	 * @param ctx the parse tree
	 */
	void enterModel_expression(OracleParser.Model_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_expression}.
	 * @param ctx the parse tree
	 */
	void exitModel_expression(OracleParser.Model_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#model_expression_element}.
	 * @param ctx the parse tree
	 */
	void enterModel_expression_element(OracleParser.Model_expression_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#model_expression_element}.
	 * @param ctx the parse tree
	 */
	void exitModel_expression_element(OracleParser.Model_expression_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#single_column_for_loop}.
	 * @param ctx the parse tree
	 */
	void enterSingle_column_for_loop(OracleParser.Single_column_for_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#single_column_for_loop}.
	 * @param ctx the parse tree
	 */
	void exitSingle_column_for_loop(OracleParser.Single_column_for_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#multi_column_for_loop}.
	 * @param ctx the parse tree
	 */
	void enterMulti_column_for_loop(OracleParser.Multi_column_for_loopContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#multi_column_for_loop}.
	 * @param ctx the parse tree
	 */
	void exitMulti_column_for_loop(OracleParser.Multi_column_for_loopContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#unary_expression}.
	 * @param ctx the parse tree
	 */
	void enterUnary_expression(OracleParser.Unary_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#unary_expression}.
	 * @param ctx the parse tree
	 */
	void exitUnary_expression(OracleParser.Unary_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#case_statement}.
	 * @param ctx the parse tree
	 */
	void enterCase_statement(OracleParser.Case_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#case_statement}.
	 * @param ctx the parse tree
	 */
	void exitCase_statement(OracleParser.Case_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#simple_case_statement}.
	 * @param ctx the parse tree
	 */
	void enterSimple_case_statement(OracleParser.Simple_case_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#simple_case_statement}.
	 * @param ctx the parse tree
	 */
	void exitSimple_case_statement(OracleParser.Simple_case_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#simple_case_when_part}.
	 * @param ctx the parse tree
	 */
	void enterSimple_case_when_part(OracleParser.Simple_case_when_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#simple_case_when_part}.
	 * @param ctx the parse tree
	 */
	void exitSimple_case_when_part(OracleParser.Simple_case_when_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#searched_case_statement}.
	 * @param ctx the parse tree
	 */
	void enterSearched_case_statement(OracleParser.Searched_case_statementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#searched_case_statement}.
	 * @param ctx the parse tree
	 */
	void exitSearched_case_statement(OracleParser.Searched_case_statementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#searched_case_when_part}.
	 * @param ctx the parse tree
	 */
	void enterSearched_case_when_part(OracleParser.Searched_case_when_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#searched_case_when_part}.
	 * @param ctx the parse tree
	 */
	void exitSearched_case_when_part(OracleParser.Searched_case_when_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#case_else_part}.
	 * @param ctx the parse tree
	 */
	void enterCase_else_part(OracleParser.Case_else_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#case_else_part}.
	 * @param ctx the parse tree
	 */
	void exitCase_else_part(OracleParser.Case_else_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterAtom(OracleParser.AtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitAtom(OracleParser.AtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#quantified_expression}.
	 * @param ctx the parse tree
	 */
	void enterQuantified_expression(OracleParser.Quantified_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#quantified_expression}.
	 * @param ctx the parse tree
	 */
	void exitQuantified_expression(OracleParser.Quantified_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#string_function}.
	 * @param ctx the parse tree
	 */
	void enterString_function(OracleParser.String_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#string_function}.
	 * @param ctx the parse tree
	 */
	void exitString_function(OracleParser.String_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#standard_function}.
	 * @param ctx the parse tree
	 */
	void enterStandard_function(OracleParser.Standard_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#standard_function}.
	 * @param ctx the parse tree
	 */
	void exitStandard_function(OracleParser.Standard_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(OracleParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(OracleParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#numeric_function_wrapper}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_function_wrapper(OracleParser.Numeric_function_wrapperContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#numeric_function_wrapper}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_function_wrapper(OracleParser.Numeric_function_wrapperContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#numeric_function}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_function(OracleParser.Numeric_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#numeric_function}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_function(OracleParser.Numeric_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#other_function}.
	 * @param ctx the parse tree
	 */
	void enterOther_function(OracleParser.Other_functionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#other_function}.
	 * @param ctx the parse tree
	 */
	void exitOther_function(OracleParser.Other_functionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#over_clause_keyword}.
	 * @param ctx the parse tree
	 */
	void enterOver_clause_keyword(OracleParser.Over_clause_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#over_clause_keyword}.
	 * @param ctx the parse tree
	 */
	void exitOver_clause_keyword(OracleParser.Over_clause_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#within_or_over_clause_keyword}.
	 * @param ctx the parse tree
	 */
	void enterWithin_or_over_clause_keyword(OracleParser.Within_or_over_clause_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#within_or_over_clause_keyword}.
	 * @param ctx the parse tree
	 */
	void exitWithin_or_over_clause_keyword(OracleParser.Within_or_over_clause_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#standard_prediction_function_keyword}.
	 * @param ctx the parse tree
	 */
	void enterStandard_prediction_function_keyword(OracleParser.Standard_prediction_function_keywordContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#standard_prediction_function_keyword}.
	 * @param ctx the parse tree
	 */
	void exitStandard_prediction_function_keyword(OracleParser.Standard_prediction_function_keywordContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#over_clause}.
	 * @param ctx the parse tree
	 */
	void enterOver_clause(OracleParser.Over_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#over_clause}.
	 * @param ctx the parse tree
	 */
	void exitOver_clause(OracleParser.Over_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#windowing_clause}.
	 * @param ctx the parse tree
	 */
	void enterWindowing_clause(OracleParser.Windowing_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#windowing_clause}.
	 * @param ctx the parse tree
	 */
	void exitWindowing_clause(OracleParser.Windowing_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#windowing_type}.
	 * @param ctx the parse tree
	 */
	void enterWindowing_type(OracleParser.Windowing_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#windowing_type}.
	 * @param ctx the parse tree
	 */
	void exitWindowing_type(OracleParser.Windowing_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#windowing_elements}.
	 * @param ctx the parse tree
	 */
	void enterWindowing_elements(OracleParser.Windowing_elementsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#windowing_elements}.
	 * @param ctx the parse tree
	 */
	void exitWindowing_elements(OracleParser.Windowing_elementsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#using_clause}.
	 * @param ctx the parse tree
	 */
	void enterUsing_clause(OracleParser.Using_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#using_clause}.
	 * @param ctx the parse tree
	 */
	void exitUsing_clause(OracleParser.Using_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#using_element}.
	 * @param ctx the parse tree
	 */
	void enterUsing_element(OracleParser.Using_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#using_element}.
	 * @param ctx the parse tree
	 */
	void exitUsing_element(OracleParser.Using_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#collect_order_by_part}.
	 * @param ctx the parse tree
	 */
	void enterCollect_order_by_part(OracleParser.Collect_order_by_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#collect_order_by_part}.
	 * @param ctx the parse tree
	 */
	void exitCollect_order_by_part(OracleParser.Collect_order_by_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#within_or_over_part}.
	 * @param ctx the parse tree
	 */
	void enterWithin_or_over_part(OracleParser.Within_or_over_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#within_or_over_part}.
	 * @param ctx the parse tree
	 */
	void exitWithin_or_over_part(OracleParser.Within_or_over_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cost_matrix_clause}.
	 * @param ctx the parse tree
	 */
	void enterCost_matrix_clause(OracleParser.Cost_matrix_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cost_matrix_clause}.
	 * @param ctx the parse tree
	 */
	void exitCost_matrix_clause(OracleParser.Cost_matrix_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_passing_clause}.
	 * @param ctx the parse tree
	 */
	void enterXml_passing_clause(OracleParser.Xml_passing_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_passing_clause}.
	 * @param ctx the parse tree
	 */
	void exitXml_passing_clause(OracleParser.Xml_passing_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void enterXml_attributes_clause(OracleParser.Xml_attributes_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_attributes_clause}.
	 * @param ctx the parse tree
	 */
	void exitXml_attributes_clause(OracleParser.Xml_attributes_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_namespaces_clause}.
	 * @param ctx the parse tree
	 */
	void enterXml_namespaces_clause(OracleParser.Xml_namespaces_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_namespaces_clause}.
	 * @param ctx the parse tree
	 */
	void exitXml_namespaces_clause(OracleParser.Xml_namespaces_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_table_column}.
	 * @param ctx the parse tree
	 */
	void enterXml_table_column(OracleParser.Xml_table_columnContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_table_column}.
	 * @param ctx the parse tree
	 */
	void exitXml_table_column(OracleParser.Xml_table_columnContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_general_default_part}.
	 * @param ctx the parse tree
	 */
	void enterXml_general_default_part(OracleParser.Xml_general_default_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_general_default_part}.
	 * @param ctx the parse tree
	 */
	void exitXml_general_default_part(OracleParser.Xml_general_default_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_multiuse_expression_element}.
	 * @param ctx the parse tree
	 */
	void enterXml_multiuse_expression_element(OracleParser.Xml_multiuse_expression_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_multiuse_expression_element}.
	 * @param ctx the parse tree
	 */
	void exitXml_multiuse_expression_element(OracleParser.Xml_multiuse_expression_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlroot_param_version_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlroot_param_version_part(OracleParser.Xmlroot_param_version_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlroot_param_version_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlroot_param_version_part(OracleParser.Xmlroot_param_version_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlroot_param_standalone_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlroot_param_standalone_part(OracleParser.Xmlroot_param_standalone_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlroot_param_standalone_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlroot_param_standalone_part(OracleParser.Xmlroot_param_standalone_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlserialize_param_enconding_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlserialize_param_enconding_part(OracleParser.Xmlserialize_param_enconding_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlserialize_param_enconding_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlserialize_param_enconding_part(OracleParser.Xmlserialize_param_enconding_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlserialize_param_version_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlserialize_param_version_part(OracleParser.Xmlserialize_param_version_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlserialize_param_version_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlserialize_param_version_part(OracleParser.Xmlserialize_param_version_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmlserialize_param_ident_part}.
	 * @param ctx the parse tree
	 */
	void enterXmlserialize_param_ident_part(OracleParser.Xmlserialize_param_ident_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmlserialize_param_ident_part}.
	 * @param ctx the parse tree
	 */
	void exitXmlserialize_param_ident_part(OracleParser.Xmlserialize_param_ident_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sql_plus_command}.
	 * @param ctx the parse tree
	 */
	void enterSql_plus_command(OracleParser.Sql_plus_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sql_plus_command}.
	 * @param ctx the parse tree
	 */
	void exitSql_plus_command(OracleParser.Sql_plus_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#whenever_command}.
	 * @param ctx the parse tree
	 */
	void enterWhenever_command(OracleParser.Whenever_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#whenever_command}.
	 * @param ctx the parse tree
	 */
	void exitWhenever_command(OracleParser.Whenever_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#set_command}.
	 * @param ctx the parse tree
	 */
	void enterSet_command(OracleParser.Set_commandContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#set_command}.
	 * @param ctx the parse tree
	 */
	void exitSet_command(OracleParser.Set_commandContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#partition_extension_clause}.
	 * @param ctx the parse tree
	 */
	void enterPartition_extension_clause(OracleParser.Partition_extension_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#partition_extension_clause}.
	 * @param ctx the parse tree
	 */
	void exitPartition_extension_clause(OracleParser.Partition_extension_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_alias}.
	 * @param ctx the parse tree
	 */
	void enterColumn_alias(OracleParser.Column_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_alias}.
	 * @param ctx the parse tree
	 */
	void exitColumn_alias(OracleParser.Column_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_alias}.
	 * @param ctx the parse tree
	 */
	void enterTable_alias(OracleParser.Table_aliasContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_alias}.
	 * @param ctx the parse tree
	 */
	void exitTable_alias(OracleParser.Table_aliasContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#where_clause}.
	 * @param ctx the parse tree
	 */
	void enterWhere_clause(OracleParser.Where_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#where_clause}.
	 * @param ctx the parse tree
	 */
	void exitWhere_clause(OracleParser.Where_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#into_clause}.
	 * @param ctx the parse tree
	 */
	void enterInto_clause(OracleParser.Into_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#into_clause}.
	 * @param ctx the parse tree
	 */
	void exitInto_clause(OracleParser.Into_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xml_column_name}.
	 * @param ctx the parse tree
	 */
	void enterXml_column_name(OracleParser.Xml_column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xml_column_name}.
	 * @param ctx the parse tree
	 */
	void exitXml_column_name(OracleParser.Xml_column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cost_class_name}.
	 * @param ctx the parse tree
	 */
	void enterCost_class_name(OracleParser.Cost_class_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cost_class_name}.
	 * @param ctx the parse tree
	 */
	void exitCost_class_name(OracleParser.Cost_class_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#attribute_name}.
	 * @param ctx the parse tree
	 */
	void enterAttribute_name(OracleParser.Attribute_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#attribute_name}.
	 * @param ctx the parse tree
	 */
	void exitAttribute_name(OracleParser.Attribute_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#savepoint_name}.
	 * @param ctx the parse tree
	 */
	void enterSavepoint_name(OracleParser.Savepoint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#savepoint_name}.
	 * @param ctx the parse tree
	 */
	void exitSavepoint_name(OracleParser.Savepoint_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#rollback_segment_name}.
	 * @param ctx the parse tree
	 */
	void enterRollback_segment_name(OracleParser.Rollback_segment_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#rollback_segment_name}.
	 * @param ctx the parse tree
	 */
	void exitRollback_segment_name(OracleParser.Rollback_segment_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_var_name}.
	 * @param ctx the parse tree
	 */
	void enterTable_var_name(OracleParser.Table_var_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_var_name}.
	 * @param ctx the parse tree
	 */
	void exitTable_var_name(OracleParser.Table_var_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#schema_name}.
	 * @param ctx the parse tree
	 */
	void enterSchema_name(OracleParser.Schema_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#schema_name}.
	 * @param ctx the parse tree
	 */
	void exitSchema_name(OracleParser.Schema_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#routine_name}.
	 * @param ctx the parse tree
	 */
	void enterRoutine_name(OracleParser.Routine_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#routine_name}.
	 * @param ctx the parse tree
	 */
	void exitRoutine_name(OracleParser.Routine_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#package_name}.
	 * @param ctx the parse tree
	 */
	void enterPackage_name(OracleParser.Package_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#package_name}.
	 * @param ctx the parse tree
	 */
	void exitPackage_name(OracleParser.Package_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#implementation_type_name}.
	 * @param ctx the parse tree
	 */
	void enterImplementation_type_name(OracleParser.Implementation_type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#implementation_type_name}.
	 * @param ctx the parse tree
	 */
	void exitImplementation_type_name(OracleParser.Implementation_type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#parameter_name}.
	 * @param ctx the parse tree
	 */
	void enterParameter_name(OracleParser.Parameter_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#parameter_name}.
	 * @param ctx the parse tree
	 */
	void exitParameter_name(OracleParser.Parameter_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#reference_model_name}.
	 * @param ctx the parse tree
	 */
	void enterReference_model_name(OracleParser.Reference_model_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#reference_model_name}.
	 * @param ctx the parse tree
	 */
	void exitReference_model_name(OracleParser.Reference_model_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#main_model_name}.
	 * @param ctx the parse tree
	 */
	void enterMain_model_name(OracleParser.Main_model_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#main_model_name}.
	 * @param ctx the parse tree
	 */
	void exitMain_model_name(OracleParser.Main_model_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#container_tableview_name}.
	 * @param ctx the parse tree
	 */
	void enterContainer_tableview_name(OracleParser.Container_tableview_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#container_tableview_name}.
	 * @param ctx the parse tree
	 */
	void exitContainer_tableview_name(OracleParser.Container_tableview_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#aggregate_function_name}.
	 * @param ctx the parse tree
	 */
	void enterAggregate_function_name(OracleParser.Aggregate_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#aggregate_function_name}.
	 * @param ctx the parse tree
	 */
	void exitAggregate_function_name(OracleParser.Aggregate_function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#query_name}.
	 * @param ctx the parse tree
	 */
	void enterQuery_name(OracleParser.Query_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#query_name}.
	 * @param ctx the parse tree
	 */
	void exitQuery_name(OracleParser.Query_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#grantee_name}.
	 * @param ctx the parse tree
	 */
	void enterGrantee_name(OracleParser.Grantee_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#grantee_name}.
	 * @param ctx the parse tree
	 */
	void exitGrantee_name(OracleParser.Grantee_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#role_name}.
	 * @param ctx the parse tree
	 */
	void enterRole_name(OracleParser.Role_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#role_name}.
	 * @param ctx the parse tree
	 */
	void exitRole_name(OracleParser.Role_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#constraint_name}.
	 * @param ctx the parse tree
	 */
	void enterConstraint_name(OracleParser.Constraint_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#constraint_name}.
	 * @param ctx the parse tree
	 */
	void exitConstraint_name(OracleParser.Constraint_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#label_name}.
	 * @param ctx the parse tree
	 */
	void enterLabel_name(OracleParser.Label_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#label_name}.
	 * @param ctx the parse tree
	 */
	void exitLabel_name(OracleParser.Label_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_name}.
	 * @param ctx the parse tree
	 */
	void enterType_name(OracleParser.Type_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_name}.
	 * @param ctx the parse tree
	 */
	void exitType_name(OracleParser.Type_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#sequence_name}.
	 * @param ctx the parse tree
	 */
	void enterSequence_name(OracleParser.Sequence_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#sequence_name}.
	 * @param ctx the parse tree
	 */
	void exitSequence_name(OracleParser.Sequence_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#exception_name}.
	 * @param ctx the parse tree
	 */
	void enterException_name(OracleParser.Exception_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#exception_name}.
	 * @param ctx the parse tree
	 */
	void exitException_name(OracleParser.Exception_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_name}.
	 * @param ctx the parse tree
	 */
	void enterFunction_name(OracleParser.Function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_name}.
	 * @param ctx the parse tree
	 */
	void exitFunction_name(OracleParser.Function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#procedure_name}.
	 * @param ctx the parse tree
	 */
	void enterProcedure_name(OracleParser.Procedure_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#procedure_name}.
	 * @param ctx the parse tree
	 */
	void exitProcedure_name(OracleParser.Procedure_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#trigger_name}.
	 * @param ctx the parse tree
	 */
	void enterTrigger_name(OracleParser.Trigger_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#trigger_name}.
	 * @param ctx the parse tree
	 */
	void exitTrigger_name(OracleParser.Trigger_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#variable_name}.
	 * @param ctx the parse tree
	 */
	void enterVariable_name(OracleParser.Variable_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#variable_name}.
	 * @param ctx the parse tree
	 */
	void exitVariable_name(OracleParser.Variable_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#index_name}.
	 * @param ctx the parse tree
	 */
	void enterIndex_name(OracleParser.Index_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#index_name}.
	 * @param ctx the parse tree
	 */
	void exitIndex_name(OracleParser.Index_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#cursor_name}.
	 * @param ctx the parse tree
	 */
	void enterCursor_name(OracleParser.Cursor_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#cursor_name}.
	 * @param ctx the parse tree
	 */
	void exitCursor_name(OracleParser.Cursor_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#record_name}.
	 * @param ctx the parse tree
	 */
	void enterRecord_name(OracleParser.Record_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#record_name}.
	 * @param ctx the parse tree
	 */
	void exitRecord_name(OracleParser.Record_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#collection_name}.
	 * @param ctx the parse tree
	 */
	void enterCollection_name(OracleParser.Collection_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#collection_name}.
	 * @param ctx the parse tree
	 */
	void exitCollection_name(OracleParser.Collection_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#link_name}.
	 * @param ctx the parse tree
	 */
	void enterLink_name(OracleParser.Link_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#link_name}.
	 * @param ctx the parse tree
	 */
	void exitLink_name(OracleParser.Link_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_name}.
	 * @param ctx the parse tree
	 */
	void enterColumn_name(OracleParser.Column_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_name}.
	 * @param ctx the parse tree
	 */
	void exitColumn_name(OracleParser.Column_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#tableview_name}.
	 * @param ctx the parse tree
	 */
	void enterTableview_name(OracleParser.Tableview_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#tableview_name}.
	 * @param ctx the parse tree
	 */
	void exitTableview_name(OracleParser.Tableview_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#xmltable}.
	 * @param ctx the parse tree
	 */
	void enterXmltable(OracleParser.XmltableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#xmltable}.
	 * @param ctx the parse tree
	 */
	void exitXmltable(OracleParser.XmltableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#char_set_name}.
	 * @param ctx the parse tree
	 */
	void enterChar_set_name(OracleParser.Char_set_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#char_set_name}.
	 * @param ctx the parse tree
	 */
	void exitChar_set_name(OracleParser.Char_set_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#synonym_name}.
	 * @param ctx the parse tree
	 */
	void enterSynonym_name(OracleParser.Synonym_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#synonym_name}.
	 * @param ctx the parse tree
	 */
	void exitSynonym_name(OracleParser.Synonym_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#schema_object_name}.
	 * @param ctx the parse tree
	 */
	void enterSchema_object_name(OracleParser.Schema_object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#schema_object_name}.
	 * @param ctx the parse tree
	 */
	void exitSchema_object_name(OracleParser.Schema_object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#dir_object_name}.
	 * @param ctx the parse tree
	 */
	void enterDir_object_name(OracleParser.Dir_object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#dir_object_name}.
	 * @param ctx the parse tree
	 */
	void exitDir_object_name(OracleParser.Dir_object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#user_object_name}.
	 * @param ctx the parse tree
	 */
	void enterUser_object_name(OracleParser.User_object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#user_object_name}.
	 * @param ctx the parse tree
	 */
	void exitUser_object_name(OracleParser.User_object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#grant_object_name}.
	 * @param ctx the parse tree
	 */
	void enterGrant_object_name(OracleParser.Grant_object_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#grant_object_name}.
	 * @param ctx the parse tree
	 */
	void exitGrant_object_name(OracleParser.Grant_object_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#column_list}.
	 * @param ctx the parse tree
	 */
	void enterColumn_list(OracleParser.Column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#column_list}.
	 * @param ctx the parse tree
	 */
	void exitColumn_list(OracleParser.Column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#paren_column_list}.
	 * @param ctx the parse tree
	 */
	void enterParen_column_list(OracleParser.Paren_column_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#paren_column_list}.
	 * @param ctx the parse tree
	 */
	void exitParen_column_list(OracleParser.Paren_column_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#keep_clause}.
	 * @param ctx the parse tree
	 */
	void enterKeep_clause(OracleParser.Keep_clauseContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#keep_clause}.
	 * @param ctx the parse tree
	 */
	void exitKeep_clause(OracleParser.Keep_clauseContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_argument}.
	 * @param ctx the parse tree
	 */
	void enterFunction_argument(OracleParser.Function_argumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_argument}.
	 * @param ctx the parse tree
	 */
	void exitFunction_argument(OracleParser.Function_argumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_argument_analytic}.
	 * @param ctx the parse tree
	 */
	void enterFunction_argument_analytic(OracleParser.Function_argument_analyticContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_argument_analytic}.
	 * @param ctx the parse tree
	 */
	void exitFunction_argument_analytic(OracleParser.Function_argument_analyticContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#function_argument_modeling}.
	 * @param ctx the parse tree
	 */
	void enterFunction_argument_modeling(OracleParser.Function_argument_modelingContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#function_argument_modeling}.
	 * @param ctx the parse tree
	 */
	void exitFunction_argument_modeling(OracleParser.Function_argument_modelingContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#respect_or_ignore_nulls}.
	 * @param ctx the parse tree
	 */
	void enterRespect_or_ignore_nulls(OracleParser.Respect_or_ignore_nullsContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#respect_or_ignore_nulls}.
	 * @param ctx the parse tree
	 */
	void exitRespect_or_ignore_nulls(OracleParser.Respect_or_ignore_nullsContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#argument}.
	 * @param ctx the parse tree
	 */
	void enterArgument(OracleParser.ArgumentContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#argument}.
	 * @param ctx the parse tree
	 */
	void exitArgument(OracleParser.ArgumentContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#type_spec}.
	 * @param ctx the parse tree
	 */
	void enterType_spec(OracleParser.Type_specContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#type_spec}.
	 * @param ctx the parse tree
	 */
	void exitType_spec(OracleParser.Type_specContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#datatype}.
	 * @param ctx the parse tree
	 */
	void enterDatatype(OracleParser.DatatypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#datatype}.
	 * @param ctx the parse tree
	 */
	void exitDatatype(OracleParser.DatatypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#precision_part}.
	 * @param ctx the parse tree
	 */
	void enterPrecision_part(OracleParser.Precision_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#precision_part}.
	 * @param ctx the parse tree
	 */
	void exitPrecision_part(OracleParser.Precision_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#native_datatype_element}.
	 * @param ctx the parse tree
	 */
	void enterNative_datatype_element(OracleParser.Native_datatype_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#native_datatype_element}.
	 * @param ctx the parse tree
	 */
	void exitNative_datatype_element(OracleParser.Native_datatype_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#bind_variable}.
	 * @param ctx the parse tree
	 */
	void enterBind_variable(OracleParser.Bind_variableContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#bind_variable}.
	 * @param ctx the parse tree
	 */
	void exitBind_variable(OracleParser.Bind_variableContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#general_element}.
	 * @param ctx the parse tree
	 */
	void enterGeneral_element(OracleParser.General_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#general_element}.
	 * @param ctx the parse tree
	 */
	void exitGeneral_element(OracleParser.General_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#general_element_part}.
	 * @param ctx the parse tree
	 */
	void enterGeneral_element_part(OracleParser.General_element_partContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#general_element_part}.
	 * @param ctx the parse tree
	 */
	void exitGeneral_element_part(OracleParser.General_element_partContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#table_element}.
	 * @param ctx the parse tree
	 */
	void enterTable_element(OracleParser.Table_elementContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#table_element}.
	 * @param ctx the parse tree
	 */
	void exitTable_element(OracleParser.Table_elementContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#object_privilege}.
	 * @param ctx the parse tree
	 */
	void enterObject_privilege(OracleParser.Object_privilegeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#object_privilege}.
	 * @param ctx the parse tree
	 */
	void exitObject_privilege(OracleParser.Object_privilegeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#system_privilege}.
	 * @param ctx the parse tree
	 */
	void enterSystem_privilege(OracleParser.System_privilegeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#system_privilege}.
	 * @param ctx the parse tree
	 */
	void exitSystem_privilege(OracleParser.System_privilegeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#constant}.
	 * @param ctx the parse tree
	 */
	void enterConstant(OracleParser.ConstantContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#constant}.
	 * @param ctx the parse tree
	 */
	void exitConstant(OracleParser.ConstantContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#numeric}.
	 * @param ctx the parse tree
	 */
	void enterNumeric(OracleParser.NumericContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#numeric}.
	 * @param ctx the parse tree
	 */
	void exitNumeric(OracleParser.NumericContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#numeric_negative}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_negative(OracleParser.Numeric_negativeContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#numeric_negative}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_negative(OracleParser.Numeric_negativeContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#quoted_string}.
	 * @param ctx the parse tree
	 */
	void enterQuoted_string(OracleParser.Quoted_stringContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#quoted_string}.
	 * @param ctx the parse tree
	 */
	void exitQuoted_string(OracleParser.Quoted_stringContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#identifier}.
	 * @param ctx the parse tree
	 */
	void enterIdentifier(OracleParser.IdentifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#identifier}.
	 * @param ctx the parse tree
	 */
	void exitIdentifier(OracleParser.IdentifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#id_expression}.
	 * @param ctx the parse tree
	 */
	void enterId_expression(OracleParser.Id_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#id_expression}.
	 * @param ctx the parse tree
	 */
	void exitId_expression(OracleParser.Id_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#outer_join_sign}.
	 * @param ctx the parse tree
	 */
	void enterOuter_join_sign(OracleParser.Outer_join_signContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#outer_join_sign}.
	 * @param ctx the parse tree
	 */
	void exitOuter_join_sign(OracleParser.Outer_join_signContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#regular_id}.
	 * @param ctx the parse tree
	 */
	void enterRegular_id(OracleParser.Regular_idContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#regular_id}.
	 * @param ctx the parse tree
	 */
	void exitRegular_id(OracleParser.Regular_idContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#non_reserved_keywords_in_12c}.
	 * @param ctx the parse tree
	 */
	void enterNon_reserved_keywords_in_12c(OracleParser.Non_reserved_keywords_in_12cContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#non_reserved_keywords_in_12c}.
	 * @param ctx the parse tree
	 */
	void exitNon_reserved_keywords_in_12c(OracleParser.Non_reserved_keywords_in_12cContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#non_reserved_keywords_pre12c}.
	 * @param ctx the parse tree
	 */
	void enterNon_reserved_keywords_pre12c(OracleParser.Non_reserved_keywords_pre12cContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#non_reserved_keywords_pre12c}.
	 * @param ctx the parse tree
	 */
	void exitNon_reserved_keywords_pre12c(OracleParser.Non_reserved_keywords_pre12cContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#string_function_name}.
	 * @param ctx the parse tree
	 */
	void enterString_function_name(OracleParser.String_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#string_function_name}.
	 * @param ctx the parse tree
	 */
	void exitString_function_name(OracleParser.String_function_nameContext ctx);
	/**
	 * Enter a parse tree produced by {@link OracleParser#numeric_function_name}.
	 * @param ctx the parse tree
	 */
	void enterNumeric_function_name(OracleParser.Numeric_function_nameContext ctx);
	/**
	 * Exit a parse tree produced by {@link OracleParser#numeric_function_name}.
	 * @param ctx the parse tree
	 */
	void exitNumeric_function_name(OracleParser.Numeric_function_nameContext ctx);
}