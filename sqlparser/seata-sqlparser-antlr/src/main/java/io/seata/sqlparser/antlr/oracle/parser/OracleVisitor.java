// Generated from Oracle.g4 by ANTLR 4.8
package io.seata.sqlparser.antlr.oracle.parser;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link OracleParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface OracleVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link OracleParser#sql_script}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_script(OracleParser.Sql_scriptContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#unit_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnit_statement(OracleParser.Unit_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_function(OracleParser.Drop_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_function(OracleParser.Alter_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_function_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_function_body(OracleParser.Create_function_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#parallel_enable_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParallel_enable_clause(OracleParser.Parallel_enable_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#partition_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_by_clause(OracleParser.Partition_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#result_cache_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResult_cache_clause(OracleParser.Result_cache_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#relies_on_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelies_on_part(OracleParser.Relies_on_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#streaming_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStreaming_clause(OracleParser.Streaming_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_package}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_package(OracleParser.Drop_packageContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_package}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_package(OracleParser.Alter_packageContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_package}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_package(OracleParser.Create_packageContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_package_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_package_body(OracleParser.Create_package_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#package_obj_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_obj_spec(OracleParser.Package_obj_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#procedure_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure_spec(OracleParser.Procedure_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#function_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_spec(OracleParser.Function_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#package_obj_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_obj_body(OracleParser.Package_obj_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_procedure}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_procedure(OracleParser.Drop_procedureContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_procedure}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_procedure(OracleParser.Alter_procedureContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#function_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_body(OracleParser.Function_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#procedure_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure_body(OracleParser.Procedure_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_procedure_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_procedure_body(OracleParser.Create_procedure_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_trigger(OracleParser.Drop_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_trigger(OracleParser.Alter_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_trigger(OracleParser.Create_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#trigger_follows_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_follows_clause(OracleParser.Trigger_follows_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#trigger_when_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_when_clause(OracleParser.Trigger_when_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#simple_dml_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_dml_trigger(OracleParser.Simple_dml_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#for_each_row}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_each_row(OracleParser.For_each_rowContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#compound_dml_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompound_dml_trigger(OracleParser.Compound_dml_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#non_dml_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNon_dml_trigger(OracleParser.Non_dml_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#trigger_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_body(OracleParser.Trigger_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#routine_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoutine_clause(OracleParser.Routine_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#compound_trigger_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompound_trigger_block(OracleParser.Compound_trigger_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#timing_point_section}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTiming_point_section(OracleParser.Timing_point_sectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#non_dml_event}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNon_dml_event(OracleParser.Non_dml_eventContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#dml_event_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDml_event_clause(OracleParser.Dml_event_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#dml_event_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDml_event_element(OracleParser.Dml_event_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#dml_event_nested_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDml_event_nested_clause(OracleParser.Dml_event_nested_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#referencing_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReferencing_clause(OracleParser.Referencing_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#referencing_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReferencing_element(OracleParser.Referencing_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_type(OracleParser.Drop_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_type(OracleParser.Alter_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#compile_type_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompile_type_clause(OracleParser.Compile_type_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#replace_type_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplace_type_clause(OracleParser.Replace_type_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_method_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_method_spec(OracleParser.Alter_method_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_method_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_method_element(OracleParser.Alter_method_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_attribute_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_attribute_definition(OracleParser.Alter_attribute_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#attribute_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribute_definition(OracleParser.Attribute_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_collection_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_collection_clauses(OracleParser.Alter_collection_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#dependent_handling_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDependent_handling_clause(OracleParser.Dependent_handling_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#dependent_exceptions_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDependent_exceptions_part(OracleParser.Dependent_exceptions_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_type(OracleParser.Create_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#type_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_definition(OracleParser.Type_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#object_type_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_type_def(OracleParser.Object_type_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#object_as_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_as_part(OracleParser.Object_as_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#object_under_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_under_part(OracleParser.Object_under_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#nested_table_type_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNested_table_type_def(OracleParser.Nested_table_type_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#sqlj_object_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSqlj_object_type(OracleParser.Sqlj_object_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#type_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_body(OracleParser.Type_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#type_body_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_body_elements(OracleParser.Type_body_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#map_order_func_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMap_order_func_declaration(OracleParser.Map_order_func_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subprog_decl_in_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubprog_decl_in_type(OracleParser.Subprog_decl_in_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#proc_decl_in_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc_decl_in_type(OracleParser.Proc_decl_in_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#func_decl_in_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_decl_in_type(OracleParser.Func_decl_in_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#constructor_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructor_declaration(OracleParser.Constructor_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#modifier_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModifier_clause(OracleParser.Modifier_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#object_member_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_member_spec(OracleParser.Object_member_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#sqlj_object_type_attr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSqlj_object_type_attr(OracleParser.Sqlj_object_type_attrContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#element_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElement_spec(OracleParser.Element_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#element_spec_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElement_spec_options(OracleParser.Element_spec_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subprogram_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubprogram_spec(OracleParser.Subprogram_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#overriding_subprogram_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOverriding_subprogram_spec(OracleParser.Overriding_subprogram_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#overriding_function_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOverriding_function_spec(OracleParser.Overriding_function_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#type_procedure_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_procedure_spec(OracleParser.Type_procedure_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#type_function_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_function_spec(OracleParser.Type_function_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#constructor_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructor_spec(OracleParser.Constructor_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#map_order_function_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMap_order_function_spec(OracleParser.Map_order_function_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#pragma_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPragma_clause(OracleParser.Pragma_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#pragma_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPragma_elements(OracleParser.Pragma_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#type_elements_parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_elements_parameter(OracleParser.Type_elements_parameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_sequence(OracleParser.Drop_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_sequence(OracleParser.Alter_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_session}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_session(OracleParser.Alter_sessionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_session_set_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_session_set_clause(OracleParser.Alter_session_set_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_sequence(OracleParser.Create_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#sequence_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSequence_spec(OracleParser.Sequence_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#sequence_start_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSequence_start_clause(OracleParser.Sequence_start_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_index(OracleParser.Create_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cluster_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCluster_index_clause(OracleParser.Cluster_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cluster_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCluster_name(OracleParser.Cluster_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#table_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_index_clause(OracleParser.Table_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#bitmap_join_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitmap_join_index_clause(OracleParser.Bitmap_join_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#index_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_expr(OracleParser.Index_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#index_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_properties(OracleParser.Index_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#domain_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDomain_index_clause(OracleParser.Domain_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#local_domain_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_domain_index_clause(OracleParser.Local_domain_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xmlindex_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlindex_clause(OracleParser.Xmlindex_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#local_xmlindex_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_xmlindex_clause(OracleParser.Local_xmlindex_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#global_partitioned_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobal_partitioned_index(OracleParser.Global_partitioned_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#index_partitioning_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_partitioning_clause(OracleParser.Index_partitioning_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#local_partitioned_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_partitioned_index(OracleParser.Local_partitioned_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#on_range_partitioned_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_range_partitioned_table(OracleParser.On_range_partitioned_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#on_list_partitioned_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_list_partitioned_table(OracleParser.On_list_partitioned_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#partitioned_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitioned_table(OracleParser.Partitioned_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#on_hash_partitioned_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_hash_partitioned_table(OracleParser.On_hash_partitioned_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#on_hash_partitioned_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_hash_partitioned_clause(OracleParser.On_hash_partitioned_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#on_comp_partitioned_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_comp_partitioned_table(OracleParser.On_comp_partitioned_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#on_comp_partitioned_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_comp_partitioned_clause(OracleParser.On_comp_partitioned_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#index_subpartition_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_subpartition_clause(OracleParser.Index_subpartition_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#index_subpartition_subclause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_subpartition_subclause(OracleParser.Index_subpartition_subclauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#odci_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOdci_parameters(OracleParser.Odci_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#indextype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndextype(OracleParser.IndextypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_index(OracleParser.Alter_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_index_ops_set1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_index_ops_set1(OracleParser.Alter_index_ops_set1Context ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_index_ops_set2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_index_ops_set2(OracleParser.Alter_index_ops_set2Context ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#visible_or_invisible}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVisible_or_invisible(OracleParser.Visible_or_invisibleContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#monitoring_nomonitoring}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMonitoring_nomonitoring(OracleParser.Monitoring_nomonitoringContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#rebuild_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRebuild_clause(OracleParser.Rebuild_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_index_partitioning}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_index_partitioning(OracleParser.Alter_index_partitioningContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#modify_index_default_attrs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_index_default_attrs(OracleParser.Modify_index_default_attrsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#add_hash_index_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_hash_index_partition(OracleParser.Add_hash_index_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#coalesce_index_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoalesce_index_partition(OracleParser.Coalesce_index_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#modify_index_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_index_partition(OracleParser.Modify_index_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#modify_index_partitions_ops}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_index_partitions_ops(OracleParser.Modify_index_partitions_opsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#rename_index_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRename_index_partition(OracleParser.Rename_index_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_index_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_index_partition(OracleParser.Drop_index_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#split_index_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSplit_index_partition(OracleParser.Split_index_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#index_partition_description}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_partition_description(OracleParser.Index_partition_descriptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#modify_index_subpartition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_index_subpartition(OracleParser.Modify_index_subpartitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#partition_name_old}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_name_old(OracleParser.Partition_name_oldContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#new_partition_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNew_partition_name(OracleParser.New_partition_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#new_index_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNew_index_name(OracleParser.New_index_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_user}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_user(OracleParser.Create_userContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_user}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_user(OracleParser.Alter_userContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_identified_by}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_identified_by(OracleParser.Alter_identified_byContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#identified_by}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentified_by(OracleParser.Identified_byContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#identified_other_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentified_other_clause(OracleParser.Identified_other_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#user_tablespace_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_tablespace_clause(OracleParser.User_tablespace_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#quota_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuota_clause(OracleParser.Quota_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#profile_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProfile_clause(OracleParser.Profile_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#role_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRole_clause(OracleParser.Role_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#user_default_role_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_default_role_clause(OracleParser.User_default_role_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#password_expire_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPassword_expire_clause(OracleParser.Password_expire_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#user_lock_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_lock_clause(OracleParser.User_lock_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#user_editions_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_editions_clause(OracleParser.User_editions_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_user_editions_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_user_editions_clause(OracleParser.Alter_user_editions_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#proxy_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProxy_clause(OracleParser.Proxy_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#container_names}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContainer_names(OracleParser.Container_namesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#set_container_data}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_container_data(OracleParser.Set_container_dataContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#add_rem_container_data}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_rem_container_data(OracleParser.Add_rem_container_dataContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#container_data_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContainer_data_clause(OracleParser.Container_data_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#analyze}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnalyze(OracleParser.AnalyzeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#partition_extention_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_extention_clause(OracleParser.Partition_extention_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#validation_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValidation_clauses(OracleParser.Validation_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#online_or_offline}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnline_or_offline(OracleParser.Online_or_offlineContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#into_clause1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInto_clause1(OracleParser.Into_clause1Context ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#partition_key_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_key_value(OracleParser.Partition_key_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subpartition_key_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubpartition_key_value(OracleParser.Subpartition_key_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#associate_statistics}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssociate_statistics(OracleParser.Associate_statisticsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#column_association}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_association(OracleParser.Column_associationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#function_association}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_association(OracleParser.Function_associationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#indextype_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndextype_name(OracleParser.Indextype_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#using_statistics_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsing_statistics_type(OracleParser.Using_statistics_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#statistics_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatistics_type_name(OracleParser.Statistics_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#default_cost_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefault_cost_clause(OracleParser.Default_cost_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cpu_cost}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCpu_cost(OracleParser.Cpu_costContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#io_cost}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIo_cost(OracleParser.Io_costContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#network_cost}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNetwork_cost(OracleParser.Network_costContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#default_selectivity_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefault_selectivity_clause(OracleParser.Default_selectivity_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#default_selectivity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefault_selectivity(OracleParser.Default_selectivityContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#storage_table_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorage_table_clause(OracleParser.Storage_table_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#unified_auditing}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnified_auditing(OracleParser.Unified_auditingContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#policy_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPolicy_name(OracleParser.Policy_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#audit_traditional}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_traditional(OracleParser.Audit_traditionalContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#audit_direct_path}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_direct_path(OracleParser.Audit_direct_pathContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#audit_container_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_container_clause(OracleParser.Audit_container_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#audit_operation_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_operation_clause(OracleParser.Audit_operation_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#auditing_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAuditing_by_clause(OracleParser.Auditing_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#audit_user}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_user(OracleParser.Audit_userContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#audit_schema_object_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_schema_object_clause(OracleParser.Audit_schema_object_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#sql_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_operation(OracleParser.Sql_operationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#auditing_on_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAuditing_on_clause(OracleParser.Auditing_on_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#model_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_name(OracleParser.Model_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#object_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_name(OracleParser.Object_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#profile_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProfile_name(OracleParser.Profile_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#sql_statement_shortcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_statement_shortcut(OracleParser.Sql_statement_shortcutContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_index(OracleParser.Drop_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#rename_object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRename_object(OracleParser.Rename_objectContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#grant_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrant_statement(OracleParser.Grant_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#container_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContainer_clause(OracleParser.Container_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_directory}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_directory(OracleParser.Create_directoryContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#directory_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectory_name(OracleParser.Directory_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#directory_path}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectory_path(OracleParser.Directory_pathContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_library}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_library(OracleParser.Alter_libraryContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#library_editionable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibrary_editionable(OracleParser.Library_editionableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#library_debug}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibrary_debug(OracleParser.Library_debugContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#compiler_parameters_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompiler_parameters_clause(OracleParser.Compiler_parameters_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#parameter_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_value(OracleParser.Parameter_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#library_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibrary_name(OracleParser.Library_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_view}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_view(OracleParser.Alter_viewContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_view_editionable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_view_editionable(OracleParser.Alter_view_editionableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_view}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_view(OracleParser.Create_viewContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#view_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitView_options(OracleParser.View_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#view_alias_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitView_alias_constraint(OracleParser.View_alias_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#object_view_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_view_clause(OracleParser.Object_view_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#inline_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInline_constraint(OracleParser.Inline_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#inline_ref_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInline_ref_constraint(OracleParser.Inline_ref_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#out_of_line_ref_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOut_of_line_ref_constraint(OracleParser.Out_of_line_ref_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#out_of_line_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOut_of_line_constraint(OracleParser.Out_of_line_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#constraint_state}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraint_state(OracleParser.Constraint_stateContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_tablespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_tablespace(OracleParser.Alter_tablespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#datafile_tempfile_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatafile_tempfile_clauses(OracleParser.Datafile_tempfile_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#tablespace_logging_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace_logging_clauses(OracleParser.Tablespace_logging_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#tablespace_group_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace_group_clause(OracleParser.Tablespace_group_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#tablespace_group_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace_group_name(OracleParser.Tablespace_group_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#tablespace_state_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace_state_clauses(OracleParser.Tablespace_state_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#flashback_mode_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlashback_mode_clause(OracleParser.Flashback_mode_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#new_tablespace_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNew_tablespace_name(OracleParser.New_tablespace_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_tablespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_tablespace(OracleParser.Create_tablespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#permanent_tablespace_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPermanent_tablespace_clause(OracleParser.Permanent_tablespace_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#tablespace_encryption_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace_encryption_spec(OracleParser.Tablespace_encryption_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#logging_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogging_clause(OracleParser.Logging_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#extent_management_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtent_management_clause(OracleParser.Extent_management_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#segment_management_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSegment_management_clause(OracleParser.Segment_management_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#temporary_tablespace_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemporary_tablespace_clause(OracleParser.Temporary_tablespace_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#undo_tablespace_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUndo_tablespace_clause(OracleParser.Undo_tablespace_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#tablespace_retention_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace_retention_clause(OracleParser.Tablespace_retention_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#datafile_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatafile_specification(OracleParser.Datafile_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#tempfile_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTempfile_specification(OracleParser.Tempfile_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#datafile_tempfile_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatafile_tempfile_spec(OracleParser.Datafile_tempfile_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#redo_log_file_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRedo_log_file_spec(OracleParser.Redo_log_file_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#autoextend_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAutoextend_clause(OracleParser.Autoextend_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#maxsize_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaxsize_clause(OracleParser.Maxsize_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#build_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBuild_clause(OracleParser.Build_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#parallel_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParallel_clause(OracleParser.Parallel_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_materialized_view}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_materialized_view(OracleParser.Alter_materialized_viewContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_mv_option1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_mv_option1(OracleParser.Alter_mv_option1Context ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_mv_refresh}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_mv_refresh(OracleParser.Alter_mv_refreshContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#rollback_segment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRollback_segment(OracleParser.Rollback_segmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#modify_mv_column_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_mv_column_clause(OracleParser.Modify_mv_column_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_materialized_view_log}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_materialized_view_log(OracleParser.Alter_materialized_view_logContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#add_mv_log_column_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_mv_log_column_clause(OracleParser.Add_mv_log_column_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#move_mv_log_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMove_mv_log_clause(OracleParser.Move_mv_log_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#mv_log_augmentation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMv_log_augmentation(OracleParser.Mv_log_augmentationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#datetime_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatetime_expr(OracleParser.Datetime_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#interval_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval_expr(OracleParser.Interval_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#synchronous_or_asynchronous}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSynchronous_or_asynchronous(OracleParser.Synchronous_or_asynchronousContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#including_or_excluding}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIncluding_or_excluding(OracleParser.Including_or_excludingContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_materialized_view_log}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_materialized_view_log(OracleParser.Create_materialized_view_logContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#new_values_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNew_values_clause(OracleParser.New_values_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#mv_log_purge_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMv_log_purge_clause(OracleParser.Mv_log_purge_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_materialized_view}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_materialized_view(OracleParser.Create_materialized_viewContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_mv_refresh}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_mv_refresh(OracleParser.Create_mv_refreshContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_context}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_context(OracleParser.Create_contextContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#oracle_namespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOracle_namespace(OracleParser.Oracle_namespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_cluster}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_cluster(OracleParser.Create_clusterContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table(OracleParser.Create_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xmltype_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltype_table(OracleParser.Xmltype_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xmltype_virtual_columns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltype_virtual_columns(OracleParser.Xmltype_virtual_columnsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xmltype_column_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltype_column_properties(OracleParser.Xmltype_column_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xmltype_storage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltype_storage(OracleParser.Xmltype_storageContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xmlschema_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlschema_spec(OracleParser.Xmlschema_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#object_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_table(OracleParser.Object_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#oid_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOid_index_clause(OracleParser.Oid_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#oid_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOid_clause(OracleParser.Oid_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#object_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_properties(OracleParser.Object_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#object_table_substitution}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_table_substitution(OracleParser.Object_table_substitutionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#relational_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelational_table(OracleParser.Relational_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#relational_property}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelational_property(OracleParser.Relational_propertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#table_partitioning_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_partitioning_clauses(OracleParser.Table_partitioning_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#range_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange_partitions(OracleParser.Range_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#list_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList_partitions(OracleParser.List_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#hash_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHash_partitions(OracleParser.Hash_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#individual_hash_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndividual_hash_partitions(OracleParser.Individual_hash_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#hash_partitions_by_quantity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHash_partitions_by_quantity(OracleParser.Hash_partitions_by_quantityContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#hash_partition_quantity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHash_partition_quantity(OracleParser.Hash_partition_quantityContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#composite_range_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComposite_range_partitions(OracleParser.Composite_range_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#composite_list_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComposite_list_partitions(OracleParser.Composite_list_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#composite_hash_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComposite_hash_partitions(OracleParser.Composite_hash_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#reference_partitioning}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReference_partitioning(OracleParser.Reference_partitioningContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#reference_partition_desc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReference_partition_desc(OracleParser.Reference_partition_descContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#system_partitioning}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSystem_partitioning(OracleParser.System_partitioningContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#range_partition_desc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange_partition_desc(OracleParser.Range_partition_descContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#list_partition_desc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList_partition_desc(OracleParser.List_partition_descContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subpartition_template}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubpartition_template(OracleParser.Subpartition_templateContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#hash_subpartition_quantity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHash_subpartition_quantity(OracleParser.Hash_subpartition_quantityContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subpartition_by_range}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubpartition_by_range(OracleParser.Subpartition_by_rangeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subpartition_by_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubpartition_by_list(OracleParser.Subpartition_by_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subpartition_by_hash}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubpartition_by_hash(OracleParser.Subpartition_by_hashContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subpartition_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubpartition_name(OracleParser.Subpartition_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#range_subpartition_desc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange_subpartition_desc(OracleParser.Range_subpartition_descContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#list_subpartition_desc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList_subpartition_desc(OracleParser.List_subpartition_descContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#individual_hash_subparts}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndividual_hash_subparts(OracleParser.Individual_hash_subpartsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#hash_subparts_by_quantity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHash_subparts_by_quantity(OracleParser.Hash_subparts_by_quantityContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#range_values_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange_values_clause(OracleParser.Range_values_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#list_values_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList_values_clause(OracleParser.List_values_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#table_partition_description}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_partition_description(OracleParser.Table_partition_descriptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#partitioning_storage_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitioning_storage_clause(OracleParser.Partitioning_storage_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#lob_partitioning_storage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_partitioning_storage(OracleParser.Lob_partitioning_storageContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#datatype_null_enable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatatype_null_enable(OracleParser.Datatype_null_enableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#size_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSize_clause(OracleParser.Size_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#table_compression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_compression(OracleParser.Table_compressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#physical_attributes_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPhysical_attributes_clause(OracleParser.Physical_attributes_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#storage_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorage_clause(OracleParser.Storage_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#deferred_segment_creation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeferred_segment_creation(OracleParser.Deferred_segment_creationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#segment_attributes_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSegment_attributes_clause(OracleParser.Segment_attributes_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#physical_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPhysical_properties(OracleParser.Physical_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#row_movement_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRow_movement_clause(OracleParser.Row_movement_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#flashback_archive_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlashback_archive_clause(OracleParser.Flashback_archive_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#log_grp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLog_grp(OracleParser.Log_grpContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#supplemental_table_logging}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSupplemental_table_logging(OracleParser.Supplemental_table_loggingContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#supplemental_log_grp_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSupplemental_log_grp_clause(OracleParser.Supplemental_log_grp_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#supplemental_id_key_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSupplemental_id_key_clause(OracleParser.Supplemental_id_key_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#allocate_extent_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllocate_extent_clause(OracleParser.Allocate_extent_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#deallocate_unused_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeallocate_unused_clause(OracleParser.Deallocate_unused_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#shrink_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShrink_clause(OracleParser.Shrink_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#records_per_block_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecords_per_block_clause(OracleParser.Records_per_block_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#upgrade_table_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpgrade_table_clause(OracleParser.Upgrade_table_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#truncate_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTruncate_table(OracleParser.Truncate_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_table(OracleParser.Drop_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_view}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_view(OracleParser.Drop_viewContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#comment_on_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComment_on_column(OracleParser.Comment_on_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#enable_or_disable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnable_or_disable(OracleParser.Enable_or_disableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#allow_or_disallow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllow_or_disallow(OracleParser.Allow_or_disallowContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_synonym}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_synonym(OracleParser.Create_synonymContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#comment_on_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComment_on_table(OracleParser.Comment_on_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_cluster}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_cluster(OracleParser.Alter_clusterContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cache_or_nocache}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCache_or_nocache(OracleParser.Cache_or_nocacheContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#database_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatabase_name(OracleParser.Database_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_database}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_database(OracleParser.Alter_databaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#startup_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartup_clauses(OracleParser.Startup_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#resetlogs_or_noresetlogs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResetlogs_or_noresetlogs(OracleParser.Resetlogs_or_noresetlogsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#upgrade_or_downgrade}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpgrade_or_downgrade(OracleParser.Upgrade_or_downgradeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#recovery_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecovery_clauses(OracleParser.Recovery_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#begin_or_end}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBegin_or_end(OracleParser.Begin_or_endContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#general_recovery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneral_recovery(OracleParser.General_recoveryContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#full_database_recovery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFull_database_recovery(OracleParser.Full_database_recoveryContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#partial_database_recovery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartial_database_recovery(OracleParser.Partial_database_recoveryContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#partial_database_recovery_10g}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartial_database_recovery_10g(OracleParser.Partial_database_recovery_10gContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#managed_standby_recovery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitManaged_standby_recovery(OracleParser.Managed_standby_recoveryContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#db_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDb_name(OracleParser.Db_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#database_file_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatabase_file_clauses(OracleParser.Database_file_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#create_datafile_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_datafile_clause(OracleParser.Create_datafile_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_datafile_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_datafile_clause(OracleParser.Alter_datafile_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_tempfile_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_tempfile_clause(OracleParser.Alter_tempfile_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#logfile_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogfile_clauses(OracleParser.Logfile_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#add_logfile_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_logfile_clauses(OracleParser.Add_logfile_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#log_file_group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLog_file_group(OracleParser.Log_file_groupContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_logfile_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_logfile_clauses(OracleParser.Drop_logfile_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#switch_logfile_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch_logfile_clause(OracleParser.Switch_logfile_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#supplemental_db_logging}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSupplemental_db_logging(OracleParser.Supplemental_db_loggingContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#add_or_drop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_or_drop(OracleParser.Add_or_dropContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#supplemental_plsql_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSupplemental_plsql_clause(OracleParser.Supplemental_plsql_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#logfile_descriptor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogfile_descriptor(OracleParser.Logfile_descriptorContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#controlfile_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitControlfile_clauses(OracleParser.Controlfile_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#trace_file_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrace_file_clause(OracleParser.Trace_file_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#standby_database_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStandby_database_clauses(OracleParser.Standby_database_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#activate_standby_db_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitActivate_standby_db_clause(OracleParser.Activate_standby_db_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#maximize_standby_db_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaximize_standby_db_clause(OracleParser.Maximize_standby_db_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#register_logfile_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegister_logfile_clause(OracleParser.Register_logfile_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#commit_switchover_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommit_switchover_clause(OracleParser.Commit_switchover_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#start_standby_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStart_standby_clause(OracleParser.Start_standby_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#stop_standby_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStop_standby_clause(OracleParser.Stop_standby_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#convert_database_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConvert_database_clause(OracleParser.Convert_database_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#default_settings_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefault_settings_clause(OracleParser.Default_settings_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#set_time_zone_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_time_zone_clause(OracleParser.Set_time_zone_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#instance_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstance_clauses(OracleParser.Instance_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#security_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSecurity_clause(OracleParser.Security_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#domain}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDomain(OracleParser.DomainContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#database}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatabase(OracleParser.DatabaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#edition_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEdition_name(OracleParser.Edition_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#filenumber}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilenumber(OracleParser.FilenumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#filename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilename(OracleParser.FilenameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table(OracleParser.Alter_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_table_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table_properties(OracleParser.Alter_table_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_table_properties_1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table_properties_1(OracleParser.Alter_table_properties_1Context ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_iot_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_iot_clauses(OracleParser.Alter_iot_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_mapping_table_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_mapping_table_clause(OracleParser.Alter_mapping_table_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_overflow_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_overflow_clause(OracleParser.Alter_overflow_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#add_overflow_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_overflow_clause(OracleParser.Add_overflow_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#enable_disable_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnable_disable_clause(OracleParser.Enable_disable_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#using_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsing_index_clause(OracleParser.Using_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#index_attributes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_attributes(OracleParser.Index_attributesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#sort_or_nosort}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSort_or_nosort(OracleParser.Sort_or_nosortContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#exceptions_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExceptions_clause(OracleParser.Exceptions_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#move_table_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMove_table_clause(OracleParser.Move_table_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#index_org_table_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_org_table_clause(OracleParser.Index_org_table_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#mapping_table_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapping_table_clause(OracleParser.Mapping_table_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#key_compression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey_compression(OracleParser.Key_compressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#index_org_overflow_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_org_overflow_clause(OracleParser.Index_org_overflow_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#column_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_clauses(OracleParser.Column_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#modify_collection_retrieval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_collection_retrieval(OracleParser.Modify_collection_retrievalContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#collection_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollection_item(OracleParser.Collection_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#rename_column_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRename_column_clause(OracleParser.Rename_column_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#old_column_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOld_column_name(OracleParser.Old_column_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#new_column_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNew_column_name(OracleParser.New_column_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#add_modify_drop_column_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_modify_drop_column_clauses(OracleParser.Add_modify_drop_column_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_column_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_column_clause(OracleParser.Drop_column_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#modify_column_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_column_clauses(OracleParser.Modify_column_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#modify_col_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_col_properties(OracleParser.Modify_col_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#modify_col_substitutable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_col_substitutable(OracleParser.Modify_col_substitutableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#add_column_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_column_clause(OracleParser.Add_column_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#alter_varray_col_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_varray_col_properties(OracleParser.Alter_varray_col_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#varray_col_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarray_col_properties(OracleParser.Varray_col_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#varray_storage_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarray_storage_clause(OracleParser.Varray_storage_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#lob_segname}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_segname(OracleParser.Lob_segnameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#lob_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_item(OracleParser.Lob_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#lob_storage_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_storage_parameters(OracleParser.Lob_storage_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#lob_storage_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_storage_clause(OracleParser.Lob_storage_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#modify_lob_storage_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_lob_storage_clause(OracleParser.Modify_lob_storage_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#modify_lob_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_lob_parameters(OracleParser.Modify_lob_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#lob_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_parameters(OracleParser.Lob_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#lob_deduplicate_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_deduplicate_clause(OracleParser.Lob_deduplicate_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#lob_compression_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_compression_clause(OracleParser.Lob_compression_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#lob_retention_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_retention_clause(OracleParser.Lob_retention_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#encryption_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEncryption_spec(OracleParser.Encryption_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#tablespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace(OracleParser.TablespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#varray_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarray_item(OracleParser.Varray_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#column_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_properties(OracleParser.Column_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#period_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPeriod_definition(OracleParser.Period_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#start_time_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStart_time_column(OracleParser.Start_time_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#end_time_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnd_time_column(OracleParser.End_time_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#column_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_definition(OracleParser.Column_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#virtual_column_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVirtual_column_definition(OracleParser.Virtual_column_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#autogenerated_sequence_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAutogenerated_sequence_definition(OracleParser.Autogenerated_sequence_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#out_of_line_part_storage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOut_of_line_part_storage(OracleParser.Out_of_line_part_storageContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#nested_table_col_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNested_table_col_properties(OracleParser.Nested_table_col_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#nested_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNested_item(OracleParser.Nested_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#substitutable_column_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubstitutable_column_clause(OracleParser.Substitutable_column_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#partition_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_name(OracleParser.Partition_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#supplemental_logging_props}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSupplemental_logging_props(OracleParser.Supplemental_logging_propsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#column_or_attribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_or_attribute(OracleParser.Column_or_attributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#object_type_col_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_type_col_properties(OracleParser.Object_type_col_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#constraint_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraint_clauses(OracleParser.Constraint_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#old_constraint_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOld_constraint_name(OracleParser.Old_constraint_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#new_constraint_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNew_constraint_name(OracleParser.New_constraint_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_constraint_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_constraint_clause(OracleParser.Drop_constraint_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_primary_key_or_unique_or_generic_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_primary_key_or_unique_or_generic_clause(OracleParser.Drop_primary_key_or_unique_or_generic_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#add_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_constraint(OracleParser.Add_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#add_constraint_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_constraint_clause(OracleParser.Add_constraint_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#check_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCheck_constraint(OracleParser.Check_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#drop_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_constraint(OracleParser.Drop_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#enable_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnable_constraint(OracleParser.Enable_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#disable_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDisable_constraint(OracleParser.Disable_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#foreign_key_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeign_key_clause(OracleParser.Foreign_key_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#references_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReferences_clause(OracleParser.References_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#on_delete_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_delete_clause(OracleParser.On_delete_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#unique_key_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnique_key_clause(OracleParser.Unique_key_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#primary_key_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary_key_clause(OracleParser.Primary_key_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#anonymous_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnonymous_block(OracleParser.Anonymous_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#invoker_rights_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvoker_rights_clause(OracleParser.Invoker_rights_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#call_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCall_spec(OracleParser.Call_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#java_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJava_spec(OracleParser.Java_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#c_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitC_spec(OracleParser.C_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#c_agent_in_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitC_agent_in_clause(OracleParser.C_agent_in_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#c_parameters_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitC_parameters_clause(OracleParser.C_parameters_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(OracleParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#default_value_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefault_value_part(OracleParser.Default_value_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#seq_of_declare_specs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeq_of_declare_specs(OracleParser.Seq_of_declare_specsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#declare_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_spec(OracleParser.Declare_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#variable_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_declaration(OracleParser.Variable_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subtype_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubtype_declaration(OracleParser.Subtype_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cursor_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_declaration(OracleParser.Cursor_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#parameter_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_spec(OracleParser.Parameter_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#exception_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitException_declaration(OracleParser.Exception_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#pragma_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPragma_declaration(OracleParser.Pragma_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#record_type_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecord_type_def(OracleParser.Record_type_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#field_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField_spec(OracleParser.Field_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#ref_cursor_type_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRef_cursor_type_def(OracleParser.Ref_cursor_type_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#type_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_declaration(OracleParser.Type_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#table_type_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_type_def(OracleParser.Table_type_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#table_indexed_by_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_indexed_by_part(OracleParser.Table_indexed_by_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#varray_type_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarray_type_def(OracleParser.Varray_type_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#seq_of_statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeq_of_statements(OracleParser.Seq_of_statementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#label_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabel_declaration(OracleParser.Label_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(OracleParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#swallow_to_semi}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwallow_to_semi(OracleParser.Swallow_to_semiContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#assignment_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_statement(OracleParser.Assignment_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#continue_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinue_statement(OracleParser.Continue_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#exit_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExit_statement(OracleParser.Exit_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#goto_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGoto_statement(OracleParser.Goto_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#if_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_statement(OracleParser.If_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#elsif_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElsif_part(OracleParser.Elsif_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#else_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElse_part(OracleParser.Else_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#loop_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_statement(OracleParser.Loop_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cursor_loop_param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_loop_param(OracleParser.Cursor_loop_paramContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#forall_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForall_statement(OracleParser.Forall_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#bounds_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBounds_clause(OracleParser.Bounds_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#between_bound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBetween_bound(OracleParser.Between_boundContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#lower_bound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLower_bound(OracleParser.Lower_boundContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#upper_bound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpper_bound(OracleParser.Upper_boundContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#null_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNull_statement(OracleParser.Null_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#raise_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRaise_statement(OracleParser.Raise_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#return_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_statement(OracleParser.Return_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_call(OracleParser.Function_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#procedure_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure_call(OracleParser.Procedure_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#pipe_row_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPipe_row_statement(OracleParser.Pipe_row_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBody(OracleParser.BodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#exception_handler}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitException_handler(OracleParser.Exception_handlerContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#trigger_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_block(OracleParser.Trigger_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(OracleParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#sql_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_statement(OracleParser.Sql_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#execute_immediate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_immediate(OracleParser.Execute_immediateContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#dynamic_returning_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDynamic_returning_clause(OracleParser.Dynamic_returning_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#data_manipulation_language_statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitData_manipulation_language_statements(OracleParser.Data_manipulation_language_statementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cursor_manipulation_statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_manipulation_statements(OracleParser.Cursor_manipulation_statementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#close_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClose_statement(OracleParser.Close_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#open_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpen_statement(OracleParser.Open_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#fetch_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFetch_statement(OracleParser.Fetch_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#open_for_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpen_for_statement(OracleParser.Open_for_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#transaction_control_statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransaction_control_statements(OracleParser.Transaction_control_statementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#set_transaction_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_transaction_command(OracleParser.Set_transaction_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#set_constraint_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_constraint_command(OracleParser.Set_constraint_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#commit_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommit_statement(OracleParser.Commit_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#write_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWrite_clause(OracleParser.Write_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#rollback_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRollback_statement(OracleParser.Rollback_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#savepoint_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSavepoint_statement(OracleParser.Savepoint_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#explain_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplain_statement(OracleParser.Explain_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#select_only_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_only_statement(OracleParser.Select_only_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#select_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_statement(OracleParser.Select_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subquery_factoring_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery_factoring_clause(OracleParser.Subquery_factoring_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#factoring_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFactoring_element(OracleParser.Factoring_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#search_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearch_clause(OracleParser.Search_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cycle_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCycle_clause(OracleParser.Cycle_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subquery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery(OracleParser.SubqueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subquery_basic_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery_basic_elements(OracleParser.Subquery_basic_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subquery_operation_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery_operation_part(OracleParser.Subquery_operation_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#query_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_block(OracleParser.Query_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#selected_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelected_list(OracleParser.Selected_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#from_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_clause(OracleParser.From_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#select_list_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_list_elements(OracleParser.Select_list_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#table_ref_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_ref_list(OracleParser.Table_ref_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#table_ref}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_ref(OracleParser.Table_refContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#table_ref_aux}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_ref_aux(OracleParser.Table_ref_auxContext ctx);
	/**
	 * Visit a parse tree produced by the {@code table_ref_aux_internal_one}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_ref_aux_internal_one(OracleParser.Table_ref_aux_internal_oneContext ctx);
	/**
	 * Visit a parse tree produced by the {@code table_ref_aux_internal_two}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_ref_aux_internal_two(OracleParser.Table_ref_aux_internal_twoContext ctx);
	/**
	 * Visit a parse tree produced by the {@code table_ref_aux_internal_three}
	 * labeled alternative in {@link OracleParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_ref_aux_internal_three(OracleParser.Table_ref_aux_internal_threeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#join_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoin_clause(OracleParser.Join_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#join_on_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoin_on_part(OracleParser.Join_on_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#join_using_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoin_using_part(OracleParser.Join_using_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#outer_join_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOuter_join_type(OracleParser.Outer_join_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#query_partition_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_partition_clause(OracleParser.Query_partition_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#flashback_query_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlashback_query_clause(OracleParser.Flashback_query_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#pivot_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot_clause(OracleParser.Pivot_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#pivot_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot_element(OracleParser.Pivot_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#pivot_for_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot_for_clause(OracleParser.Pivot_for_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#pivot_in_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot_in_clause(OracleParser.Pivot_in_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#pivot_in_clause_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot_in_clause_element(OracleParser.Pivot_in_clause_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#pivot_in_clause_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot_in_clause_elements(OracleParser.Pivot_in_clause_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#unpivot_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivot_clause(OracleParser.Unpivot_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#unpivot_in_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivot_in_clause(OracleParser.Unpivot_in_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#unpivot_in_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivot_in_elements(OracleParser.Unpivot_in_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#hierarchical_query_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHierarchical_query_clause(OracleParser.Hierarchical_query_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#start_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStart_part(OracleParser.Start_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#group_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup_by_clause(OracleParser.Group_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#group_by_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup_by_elements(OracleParser.Group_by_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#rollup_cube_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRollup_cube_clause(OracleParser.Rollup_cube_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#grouping_sets_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrouping_sets_clause(OracleParser.Grouping_sets_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#grouping_sets_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrouping_sets_elements(OracleParser.Grouping_sets_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#having_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHaving_clause(OracleParser.Having_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#model_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_clause(OracleParser.Model_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cell_reference_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCell_reference_options(OracleParser.Cell_reference_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#return_rows_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_rows_clause(OracleParser.Return_rows_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#reference_model}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReference_model(OracleParser.Reference_modelContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#main_model}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMain_model(OracleParser.Main_modelContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#model_column_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_column_clauses(OracleParser.Model_column_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#model_column_partition_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_column_partition_part(OracleParser.Model_column_partition_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#model_column_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_column_list(OracleParser.Model_column_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#model_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_column(OracleParser.Model_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#model_rules_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_rules_clause(OracleParser.Model_rules_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#model_rules_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_rules_part(OracleParser.Model_rules_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#model_rules_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_rules_element(OracleParser.Model_rules_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cell_assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCell_assignment(OracleParser.Cell_assignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#model_iterate_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_iterate_clause(OracleParser.Model_iterate_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#until_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUntil_part(OracleParser.Until_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#order_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrder_by_clause(OracleParser.Order_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#order_by_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrder_by_elements(OracleParser.Order_by_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#offset_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOffset_clause(OracleParser.Offset_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#fetch_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFetch_clause(OracleParser.Fetch_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#for_update_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_update_clause(OracleParser.For_update_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#for_update_of_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_update_of_part(OracleParser.For_update_of_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#for_update_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_update_options(OracleParser.For_update_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#update_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_statement(OracleParser.Update_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#update_set_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_set_clause(OracleParser.Update_set_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#column_based_update_set_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_based_update_set_clause(OracleParser.Column_based_update_set_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#delete_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete_statement(OracleParser.Delete_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#insert_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_statement(OracleParser.Insert_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#single_table_insert}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_table_insert(OracleParser.Single_table_insertContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#multi_table_insert}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulti_table_insert(OracleParser.Multi_table_insertContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#multi_table_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulti_table_element(OracleParser.Multi_table_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#conditional_insert_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditional_insert_clause(OracleParser.Conditional_insert_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#conditional_insert_when_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditional_insert_when_part(OracleParser.Conditional_insert_when_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#conditional_insert_else_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditional_insert_else_part(OracleParser.Conditional_insert_else_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#insert_into_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_into_clause(OracleParser.Insert_into_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#values_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValues_clause(OracleParser.Values_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#merge_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_statement(OracleParser.Merge_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#merge_update_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_update_clause(OracleParser.Merge_update_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#merge_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_element(OracleParser.Merge_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#merge_update_delete_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_update_delete_part(OracleParser.Merge_update_delete_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#merge_insert_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_insert_clause(OracleParser.Merge_insert_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#selected_tableview}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelected_tableview(OracleParser.Selected_tableviewContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#lock_table_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLock_table_statement(OracleParser.Lock_table_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#wait_nowait_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWait_nowait_part(OracleParser.Wait_nowait_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#lock_table_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLock_table_element(OracleParser.Lock_table_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#lock_mode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLock_mode(OracleParser.Lock_modeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#general_table_ref}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneral_table_ref(OracleParser.General_table_refContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#static_returning_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatic_returning_clause(OracleParser.Static_returning_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#error_logging_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitError_logging_clause(OracleParser.Error_logging_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#error_logging_into_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitError_logging_into_part(OracleParser.Error_logging_into_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#error_logging_reject_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitError_logging_reject_part(OracleParser.Error_logging_reject_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#dml_table_expression_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDml_table_expression_clause(OracleParser.Dml_table_expression_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#table_collection_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_collection_expression(OracleParser.Table_collection_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#subquery_restriction_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery_restriction_clause(OracleParser.Subquery_restriction_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#sample_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSample_clause(OracleParser.Sample_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#seed_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeed_part(OracleParser.Seed_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCondition(OracleParser.ConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#expressions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressions(OracleParser.ExpressionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(OracleParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cursor_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_expression(OracleParser.Cursor_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#logical_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogical_expression(OracleParser.Logical_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#unary_logical_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary_logical_expression(OracleParser.Unary_logical_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#logical_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogical_operation(OracleParser.Logical_operationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#multiset_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiset_expression(OracleParser.Multiset_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#relational_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelational_expression(OracleParser.Relational_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#compound_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompound_expression(OracleParser.Compound_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#relational_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelational_operator(OracleParser.Relational_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#in_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIn_elements(OracleParser.In_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#between_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBetween_elements(OracleParser.Between_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#concatenation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConcatenation(OracleParser.ConcatenationContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#interval_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval_expression(OracleParser.Interval_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#model_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_expression(OracleParser.Model_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#model_expression_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_expression_element(OracleParser.Model_expression_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#single_column_for_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_column_for_loop(OracleParser.Single_column_for_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#multi_column_for_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulti_column_for_loop(OracleParser.Multi_column_for_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#unary_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary_expression(OracleParser.Unary_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#case_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_statement(OracleParser.Case_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#simple_case_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_case_statement(OracleParser.Simple_case_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#simple_case_when_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_case_when_part(OracleParser.Simple_case_when_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#searched_case_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearched_case_statement(OracleParser.Searched_case_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#searched_case_when_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearched_case_when_part(OracleParser.Searched_case_when_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#case_else_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_else_part(OracleParser.Case_else_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtom(OracleParser.AtomContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#quantified_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuantified_expression(OracleParser.Quantified_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#string_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString_function(OracleParser.String_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#standard_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStandard_function(OracleParser.Standard_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(OracleParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#numeric_function_wrapper}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric_function_wrapper(OracleParser.Numeric_function_wrapperContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#numeric_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric_function(OracleParser.Numeric_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#other_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOther_function(OracleParser.Other_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#over_clause_keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOver_clause_keyword(OracleParser.Over_clause_keywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#within_or_over_clause_keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithin_or_over_clause_keyword(OracleParser.Within_or_over_clause_keywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#standard_prediction_function_keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStandard_prediction_function_keyword(OracleParser.Standard_prediction_function_keywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#over_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOver_clause(OracleParser.Over_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#windowing_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowing_clause(OracleParser.Windowing_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#windowing_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowing_type(OracleParser.Windowing_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#windowing_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowing_elements(OracleParser.Windowing_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#using_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsing_clause(OracleParser.Using_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#using_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsing_element(OracleParser.Using_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#collect_order_by_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollect_order_by_part(OracleParser.Collect_order_by_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#within_or_over_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithin_or_over_part(OracleParser.Within_or_over_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cost_matrix_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCost_matrix_clause(OracleParser.Cost_matrix_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xml_passing_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_passing_clause(OracleParser.Xml_passing_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xml_attributes_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_attributes_clause(OracleParser.Xml_attributes_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xml_namespaces_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_namespaces_clause(OracleParser.Xml_namespaces_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xml_table_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_table_column(OracleParser.Xml_table_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xml_general_default_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_general_default_part(OracleParser.Xml_general_default_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xml_multiuse_expression_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_multiuse_expression_element(OracleParser.Xml_multiuse_expression_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xmlroot_param_version_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlroot_param_version_part(OracleParser.Xmlroot_param_version_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xmlroot_param_standalone_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlroot_param_standalone_part(OracleParser.Xmlroot_param_standalone_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xmlserialize_param_enconding_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlserialize_param_enconding_part(OracleParser.Xmlserialize_param_enconding_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xmlserialize_param_version_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlserialize_param_version_part(OracleParser.Xmlserialize_param_version_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xmlserialize_param_ident_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlserialize_param_ident_part(OracleParser.Xmlserialize_param_ident_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#sql_plus_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_plus_command(OracleParser.Sql_plus_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#whenever_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhenever_command(OracleParser.Whenever_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#set_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_command(OracleParser.Set_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#partition_extension_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_extension_clause(OracleParser.Partition_extension_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#column_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_alias(OracleParser.Column_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#table_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_alias(OracleParser.Table_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#where_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhere_clause(OracleParser.Where_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#into_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInto_clause(OracleParser.Into_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xml_column_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_column_name(OracleParser.Xml_column_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cost_class_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCost_class_name(OracleParser.Cost_class_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#attribute_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribute_name(OracleParser.Attribute_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#savepoint_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSavepoint_name(OracleParser.Savepoint_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#rollback_segment_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRollback_segment_name(OracleParser.Rollback_segment_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#table_var_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_var_name(OracleParser.Table_var_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#schema_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchema_name(OracleParser.Schema_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#routine_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoutine_name(OracleParser.Routine_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#package_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_name(OracleParser.Package_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#implementation_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImplementation_type_name(OracleParser.Implementation_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#parameter_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_name(OracleParser.Parameter_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#reference_model_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReference_model_name(OracleParser.Reference_model_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#main_model_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMain_model_name(OracleParser.Main_model_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#container_tableview_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContainer_tableview_name(OracleParser.Container_tableview_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#aggregate_function_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregate_function_name(OracleParser.Aggregate_function_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#query_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_name(OracleParser.Query_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#grantee_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrantee_name(OracleParser.Grantee_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#role_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRole_name(OracleParser.Role_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#constraint_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraint_name(OracleParser.Constraint_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#label_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabel_name(OracleParser.Label_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_name(OracleParser.Type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#sequence_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSequence_name(OracleParser.Sequence_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#exception_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitException_name(OracleParser.Exception_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#function_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_name(OracleParser.Function_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#procedure_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure_name(OracleParser.Procedure_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#trigger_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_name(OracleParser.Trigger_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#variable_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_name(OracleParser.Variable_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#index_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_name(OracleParser.Index_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#cursor_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_name(OracleParser.Cursor_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#record_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecord_name(OracleParser.Record_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#collection_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollection_name(OracleParser.Collection_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#link_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLink_name(OracleParser.Link_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#column_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_name(OracleParser.Column_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#tableview_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableview_name(OracleParser.Tableview_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#xmltable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltable(OracleParser.XmltableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#char_set_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChar_set_name(OracleParser.Char_set_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#synonym_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSynonym_name(OracleParser.Synonym_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#schema_object_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchema_object_name(OracleParser.Schema_object_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#dir_object_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDir_object_name(OracleParser.Dir_object_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#user_object_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_object_name(OracleParser.User_object_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#grant_object_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrant_object_name(OracleParser.Grant_object_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#column_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_list(OracleParser.Column_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#paren_column_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParen_column_list(OracleParser.Paren_column_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#keep_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeep_clause(OracleParser.Keep_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#function_argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_argument(OracleParser.Function_argumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#function_argument_analytic}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_argument_analytic(OracleParser.Function_argument_analyticContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#function_argument_modeling}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_argument_modeling(OracleParser.Function_argument_modelingContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#respect_or_ignore_nulls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRespect_or_ignore_nulls(OracleParser.Respect_or_ignore_nullsContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(OracleParser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#type_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_spec(OracleParser.Type_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#datatype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatatype(OracleParser.DatatypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#precision_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrecision_part(OracleParser.Precision_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#native_datatype_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNative_datatype_element(OracleParser.Native_datatype_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#bind_variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBind_variable(OracleParser.Bind_variableContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#general_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneral_element(OracleParser.General_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#general_element_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneral_element_part(OracleParser.General_element_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#table_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_element(OracleParser.Table_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#object_privilege}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_privilege(OracleParser.Object_privilegeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#system_privilege}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSystem_privilege(OracleParser.System_privilegeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant(OracleParser.ConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#numeric}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric(OracleParser.NumericContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#numeric_negative}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric_negative(OracleParser.Numeric_negativeContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#quoted_string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuoted_string(OracleParser.Quoted_stringContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(OracleParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#id_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId_expression(OracleParser.Id_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#outer_join_sign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOuter_join_sign(OracleParser.Outer_join_signContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#regular_id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegular_id(OracleParser.Regular_idContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#non_reserved_keywords_in_12c}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNon_reserved_keywords_in_12c(OracleParser.Non_reserved_keywords_in_12cContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#non_reserved_keywords_pre12c}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNon_reserved_keywords_pre12c(OracleParser.Non_reserved_keywords_pre12cContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#string_function_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString_function_name(OracleParser.String_function_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link OracleParser#numeric_function_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric_function_name(OracleParser.Numeric_function_nameContext ctx);
}