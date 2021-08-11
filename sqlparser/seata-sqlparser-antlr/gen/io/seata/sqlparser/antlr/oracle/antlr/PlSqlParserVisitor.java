// Generated from /Users/apple/Desktop/程序用文件夹/seata/sqlparser/seata-sqlparser-antlr/src/main/java/io/seata/sqlparser/antlr/oracle/antlr/PlSqlParser.g4 by ANTLR 4.9.1
package io.seata.sqlparser.antlr.oracle.antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link PlSqlParserParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface PlSqlParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#sql_script}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_script(PlSqlParserParser.Sql_scriptContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#unit_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnit_statement(PlSqlParserParser.Unit_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_function(PlSqlParserParser.Drop_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_function(PlSqlParserParser.Alter_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_function_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_function_body(PlSqlParserParser.Create_function_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#parallel_enable_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParallel_enable_clause(PlSqlParserParser.Parallel_enable_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#partition_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_by_clause(PlSqlParserParser.Partition_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#result_cache_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResult_cache_clause(PlSqlParserParser.Result_cache_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#relies_on_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelies_on_part(PlSqlParserParser.Relies_on_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#streaming_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStreaming_clause(PlSqlParserParser.Streaming_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_package}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_package(PlSqlParserParser.Drop_packageContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_package}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_package(PlSqlParserParser.Alter_packageContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_package}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_package(PlSqlParserParser.Create_packageContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_package_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_package_body(PlSqlParserParser.Create_package_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#package_obj_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_obj_spec(PlSqlParserParser.Package_obj_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#procedure_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure_spec(PlSqlParserParser.Procedure_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#function_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_spec(PlSqlParserParser.Function_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#package_obj_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_obj_body(PlSqlParserParser.Package_obj_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_procedure}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_procedure(PlSqlParserParser.Drop_procedureContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_procedure}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_procedure(PlSqlParserParser.Alter_procedureContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#function_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_body(PlSqlParserParser.Function_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#procedure_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure_body(PlSqlParserParser.Procedure_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_procedure_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_procedure_body(PlSqlParserParser.Create_procedure_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_trigger(PlSqlParserParser.Drop_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_trigger(PlSqlParserParser.Alter_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_trigger(PlSqlParserParser.Create_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#trigger_follows_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_follows_clause(PlSqlParserParser.Trigger_follows_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#trigger_when_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_when_clause(PlSqlParserParser.Trigger_when_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#simple_dml_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_dml_trigger(PlSqlParserParser.Simple_dml_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#for_each_row}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_each_row(PlSqlParserParser.For_each_rowContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#compound_dml_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompound_dml_trigger(PlSqlParserParser.Compound_dml_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#non_dml_trigger}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNon_dml_trigger(PlSqlParserParser.Non_dml_triggerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#trigger_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_body(PlSqlParserParser.Trigger_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#routine_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoutine_clause(PlSqlParserParser.Routine_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#compound_trigger_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompound_trigger_block(PlSqlParserParser.Compound_trigger_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#timing_point_section}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTiming_point_section(PlSqlParserParser.Timing_point_sectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#non_dml_event}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNon_dml_event(PlSqlParserParser.Non_dml_eventContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#dml_event_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDml_event_clause(PlSqlParserParser.Dml_event_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#dml_event_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDml_event_element(PlSqlParserParser.Dml_event_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#dml_event_nested_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDml_event_nested_clause(PlSqlParserParser.Dml_event_nested_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#referencing_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReferencing_clause(PlSqlParserParser.Referencing_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#referencing_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReferencing_element(PlSqlParserParser.Referencing_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_type(PlSqlParserParser.Drop_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_type(PlSqlParserParser.Alter_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#compile_type_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompile_type_clause(PlSqlParserParser.Compile_type_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#replace_type_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReplace_type_clause(PlSqlParserParser.Replace_type_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_method_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_method_spec(PlSqlParserParser.Alter_method_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_method_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_method_element(PlSqlParserParser.Alter_method_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_attribute_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_attribute_definition(PlSqlParserParser.Alter_attribute_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#attribute_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribute_definition(PlSqlParserParser.Attribute_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_collection_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_collection_clauses(PlSqlParserParser.Alter_collection_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#dependent_handling_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDependent_handling_clause(PlSqlParserParser.Dependent_handling_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#dependent_exceptions_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDependent_exceptions_part(PlSqlParserParser.Dependent_exceptions_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_type(PlSqlParserParser.Create_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#type_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_definition(PlSqlParserParser.Type_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#object_type_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_type_def(PlSqlParserParser.Object_type_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#object_as_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_as_part(PlSqlParserParser.Object_as_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#object_under_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_under_part(PlSqlParserParser.Object_under_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#nested_table_type_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNested_table_type_def(PlSqlParserParser.Nested_table_type_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#sqlj_object_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSqlj_object_type(PlSqlParserParser.Sqlj_object_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#type_body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_body(PlSqlParserParser.Type_bodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#type_body_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_body_elements(PlSqlParserParser.Type_body_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#map_order_func_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMap_order_func_declaration(PlSqlParserParser.Map_order_func_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subprog_decl_in_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubprog_decl_in_type(PlSqlParserParser.Subprog_decl_in_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#proc_decl_in_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProc_decl_in_type(PlSqlParserParser.Proc_decl_in_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#func_decl_in_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc_decl_in_type(PlSqlParserParser.Func_decl_in_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#constructor_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructor_declaration(PlSqlParserParser.Constructor_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#modifier_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModifier_clause(PlSqlParserParser.Modifier_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#object_member_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_member_spec(PlSqlParserParser.Object_member_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#sqlj_object_type_attr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSqlj_object_type_attr(PlSqlParserParser.Sqlj_object_type_attrContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#element_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElement_spec(PlSqlParserParser.Element_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#element_spec_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElement_spec_options(PlSqlParserParser.Element_spec_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subprogram_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubprogram_spec(PlSqlParserParser.Subprogram_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#overriding_subprogram_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOverriding_subprogram_spec(PlSqlParserParser.Overriding_subprogram_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#overriding_function_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOverriding_function_spec(PlSqlParserParser.Overriding_function_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#type_procedure_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_procedure_spec(PlSqlParserParser.Type_procedure_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#type_function_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_function_spec(PlSqlParserParser.Type_function_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#constructor_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstructor_spec(PlSqlParserParser.Constructor_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#map_order_function_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMap_order_function_spec(PlSqlParserParser.Map_order_function_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#pragma_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPragma_clause(PlSqlParserParser.Pragma_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#pragma_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPragma_elements(PlSqlParserParser.Pragma_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#type_elements_parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_elements_parameter(PlSqlParserParser.Type_elements_parameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_sequence(PlSqlParserParser.Drop_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_sequence(PlSqlParserParser.Alter_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_session}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_session(PlSqlParserParser.Alter_sessionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_session_set_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_session_set_clause(PlSqlParserParser.Alter_session_set_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_sequence}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_sequence(PlSqlParserParser.Create_sequenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#sequence_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSequence_spec(PlSqlParserParser.Sequence_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#sequence_start_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSequence_start_clause(PlSqlParserParser.Sequence_start_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_index(PlSqlParserParser.Create_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cluster_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCluster_index_clause(PlSqlParserParser.Cluster_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cluster_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCluster_name(PlSqlParserParser.Cluster_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#table_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_index_clause(PlSqlParserParser.Table_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#bitmap_join_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBitmap_join_index_clause(PlSqlParserParser.Bitmap_join_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#index_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_expr(PlSqlParserParser.Index_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#index_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_properties(PlSqlParserParser.Index_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#domain_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDomain_index_clause(PlSqlParserParser.Domain_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#local_domain_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_domain_index_clause(PlSqlParserParser.Local_domain_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xmlindex_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlindex_clause(PlSqlParserParser.Xmlindex_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#local_xmlindex_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_xmlindex_clause(PlSqlParserParser.Local_xmlindex_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#global_partitioned_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobal_partitioned_index(PlSqlParserParser.Global_partitioned_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#index_partitioning_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_partitioning_clause(PlSqlParserParser.Index_partitioning_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#local_partitioned_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocal_partitioned_index(PlSqlParserParser.Local_partitioned_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#on_range_partitioned_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_range_partitioned_table(PlSqlParserParser.On_range_partitioned_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#on_list_partitioned_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_list_partitioned_table(PlSqlParserParser.On_list_partitioned_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#partitioned_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitioned_table(PlSqlParserParser.Partitioned_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#on_hash_partitioned_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_hash_partitioned_table(PlSqlParserParser.On_hash_partitioned_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#on_hash_partitioned_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_hash_partitioned_clause(PlSqlParserParser.On_hash_partitioned_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#on_comp_partitioned_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_comp_partitioned_table(PlSqlParserParser.On_comp_partitioned_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#on_comp_partitioned_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_comp_partitioned_clause(PlSqlParserParser.On_comp_partitioned_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#index_subpartition_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_subpartition_clause(PlSqlParserParser.Index_subpartition_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#index_subpartition_subclause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_subpartition_subclause(PlSqlParserParser.Index_subpartition_subclauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#odci_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOdci_parameters(PlSqlParserParser.Odci_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#indextype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndextype(PlSqlParserParser.IndextypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_index(PlSqlParserParser.Alter_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_index_ops_set1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_index_ops_set1(PlSqlParserParser.Alter_index_ops_set1Context ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_index_ops_set2}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_index_ops_set2(PlSqlParserParser.Alter_index_ops_set2Context ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#visible_or_invisible}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVisible_or_invisible(PlSqlParserParser.Visible_or_invisibleContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#monitoring_nomonitoring}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMonitoring_nomonitoring(PlSqlParserParser.Monitoring_nomonitoringContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#rebuild_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRebuild_clause(PlSqlParserParser.Rebuild_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_index_partitioning}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_index_partitioning(PlSqlParserParser.Alter_index_partitioningContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#modify_index_default_attrs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_index_default_attrs(PlSqlParserParser.Modify_index_default_attrsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#add_hash_index_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_hash_index_partition(PlSqlParserParser.Add_hash_index_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#coalesce_index_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCoalesce_index_partition(PlSqlParserParser.Coalesce_index_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#modify_index_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_index_partition(PlSqlParserParser.Modify_index_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#modify_index_partitions_ops}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_index_partitions_ops(PlSqlParserParser.Modify_index_partitions_opsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#rename_index_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRename_index_partition(PlSqlParserParser.Rename_index_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_index_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_index_partition(PlSqlParserParser.Drop_index_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#split_index_partition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSplit_index_partition(PlSqlParserParser.Split_index_partitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#index_partition_description}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_partition_description(PlSqlParserParser.Index_partition_descriptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#modify_index_subpartition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_index_subpartition(PlSqlParserParser.Modify_index_subpartitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#partition_name_old}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_name_old(PlSqlParserParser.Partition_name_oldContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#new_partition_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNew_partition_name(PlSqlParserParser.New_partition_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#new_index_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNew_index_name(PlSqlParserParser.New_index_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_user}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_user(PlSqlParserParser.Create_userContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_user}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_user(PlSqlParserParser.Alter_userContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_identified_by}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_identified_by(PlSqlParserParser.Alter_identified_byContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#identified_by}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentified_by(PlSqlParserParser.Identified_byContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#identified_other_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentified_other_clause(PlSqlParserParser.Identified_other_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#user_tablespace_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_tablespace_clause(PlSqlParserParser.User_tablespace_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#quota_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuota_clause(PlSqlParserParser.Quota_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#profile_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProfile_clause(PlSqlParserParser.Profile_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#role_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRole_clause(PlSqlParserParser.Role_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#user_default_role_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_default_role_clause(PlSqlParserParser.User_default_role_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#password_expire_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPassword_expire_clause(PlSqlParserParser.Password_expire_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#user_lock_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_lock_clause(PlSqlParserParser.User_lock_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#user_editions_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_editions_clause(PlSqlParserParser.User_editions_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_user_editions_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_user_editions_clause(PlSqlParserParser.Alter_user_editions_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#proxy_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProxy_clause(PlSqlParserParser.Proxy_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#container_names}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContainer_names(PlSqlParserParser.Container_namesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#set_container_data}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_container_data(PlSqlParserParser.Set_container_dataContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#add_rem_container_data}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_rem_container_data(PlSqlParserParser.Add_rem_container_dataContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#container_data_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContainer_data_clause(PlSqlParserParser.Container_data_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#analyze}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnalyze(PlSqlParserParser.AnalyzeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#partition_extention_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_extention_clause(PlSqlParserParser.Partition_extention_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#validation_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValidation_clauses(PlSqlParserParser.Validation_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#online_or_offline}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOnline_or_offline(PlSqlParserParser.Online_or_offlineContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#into_clause1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInto_clause1(PlSqlParserParser.Into_clause1Context ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#partition_key_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_key_value(PlSqlParserParser.Partition_key_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subpartition_key_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubpartition_key_value(PlSqlParserParser.Subpartition_key_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#associate_statistics}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssociate_statistics(PlSqlParserParser.Associate_statisticsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#column_association}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_association(PlSqlParserParser.Column_associationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#function_association}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_association(PlSqlParserParser.Function_associationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#indextype_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndextype_name(PlSqlParserParser.Indextype_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#using_statistics_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsing_statistics_type(PlSqlParserParser.Using_statistics_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#statistics_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatistics_type_name(PlSqlParserParser.Statistics_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#default_cost_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefault_cost_clause(PlSqlParserParser.Default_cost_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cpu_cost}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCpu_cost(PlSqlParserParser.Cpu_costContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#io_cost}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIo_cost(PlSqlParserParser.Io_costContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#network_cost}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNetwork_cost(PlSqlParserParser.Network_costContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#default_selectivity_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefault_selectivity_clause(PlSqlParserParser.Default_selectivity_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#default_selectivity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefault_selectivity(PlSqlParserParser.Default_selectivityContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#storage_table_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorage_table_clause(PlSqlParserParser.Storage_table_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#unified_auditing}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnified_auditing(PlSqlParserParser.Unified_auditingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#policy_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPolicy_name(PlSqlParserParser.Policy_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#audit_traditional}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_traditional(PlSqlParserParser.Audit_traditionalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#audit_direct_path}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_direct_path(PlSqlParserParser.Audit_direct_pathContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#audit_container_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_container_clause(PlSqlParserParser.Audit_container_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#audit_operation_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_operation_clause(PlSqlParserParser.Audit_operation_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#auditing_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAuditing_by_clause(PlSqlParserParser.Auditing_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#audit_user}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_user(PlSqlParserParser.Audit_userContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#audit_schema_object_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAudit_schema_object_clause(PlSqlParserParser.Audit_schema_object_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#sql_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_operation(PlSqlParserParser.Sql_operationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#auditing_on_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAuditing_on_clause(PlSqlParserParser.Auditing_on_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#model_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_name(PlSqlParserParser.Model_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#object_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_name(PlSqlParserParser.Object_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#profile_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProfile_name(PlSqlParserParser.Profile_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#sql_statement_shortcut}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_statement_shortcut(PlSqlParserParser.Sql_statement_shortcutContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_index}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_index(PlSqlParserParser.Drop_indexContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#rename_object}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRename_object(PlSqlParserParser.Rename_objectContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#grant_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrant_statement(PlSqlParserParser.Grant_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#container_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContainer_clause(PlSqlParserParser.Container_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_directory}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_directory(PlSqlParserParser.Create_directoryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#directory_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectory_name(PlSqlParserParser.Directory_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#directory_path}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDirectory_path(PlSqlParserParser.Directory_pathContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_library}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_library(PlSqlParserParser.Alter_libraryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#library_editionable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibrary_editionable(PlSqlParserParser.Library_editionableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#library_debug}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibrary_debug(PlSqlParserParser.Library_debugContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#compiler_parameters_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompiler_parameters_clause(PlSqlParserParser.Compiler_parameters_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#parameter_value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_value(PlSqlParserParser.Parameter_valueContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#library_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLibrary_name(PlSqlParserParser.Library_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_view}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_view(PlSqlParserParser.Alter_viewContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_view_editionable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_view_editionable(PlSqlParserParser.Alter_view_editionableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_view}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_view(PlSqlParserParser.Create_viewContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#view_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitView_options(PlSqlParserParser.View_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#view_alias_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitView_alias_constraint(PlSqlParserParser.View_alias_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#object_view_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_view_clause(PlSqlParserParser.Object_view_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#inline_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInline_constraint(PlSqlParserParser.Inline_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#inline_ref_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInline_ref_constraint(PlSqlParserParser.Inline_ref_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#out_of_line_ref_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOut_of_line_ref_constraint(PlSqlParserParser.Out_of_line_ref_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#out_of_line_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOut_of_line_constraint(PlSqlParserParser.Out_of_line_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#constraint_state}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraint_state(PlSqlParserParser.Constraint_stateContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_tablespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_tablespace(PlSqlParserParser.Alter_tablespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#datafile_tempfile_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatafile_tempfile_clauses(PlSqlParserParser.Datafile_tempfile_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#tablespace_logging_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace_logging_clauses(PlSqlParserParser.Tablespace_logging_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#tablespace_group_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace_group_clause(PlSqlParserParser.Tablespace_group_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#tablespace_group_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace_group_name(PlSqlParserParser.Tablespace_group_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#tablespace_state_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace_state_clauses(PlSqlParserParser.Tablespace_state_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#flashback_mode_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlashback_mode_clause(PlSqlParserParser.Flashback_mode_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#new_tablespace_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNew_tablespace_name(PlSqlParserParser.New_tablespace_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_tablespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_tablespace(PlSqlParserParser.Create_tablespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#permanent_tablespace_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPermanent_tablespace_clause(PlSqlParserParser.Permanent_tablespace_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#tablespace_encryption_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace_encryption_spec(PlSqlParserParser.Tablespace_encryption_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#logging_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogging_clause(PlSqlParserParser.Logging_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#extent_management_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExtent_management_clause(PlSqlParserParser.Extent_management_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#segment_management_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSegment_management_clause(PlSqlParserParser.Segment_management_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#temporary_tablespace_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTemporary_tablespace_clause(PlSqlParserParser.Temporary_tablespace_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#undo_tablespace_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUndo_tablespace_clause(PlSqlParserParser.Undo_tablespace_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#tablespace_retention_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace_retention_clause(PlSqlParserParser.Tablespace_retention_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#datafile_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatafile_specification(PlSqlParserParser.Datafile_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#tempfile_specification}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTempfile_specification(PlSqlParserParser.Tempfile_specificationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#datafile_tempfile_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatafile_tempfile_spec(PlSqlParserParser.Datafile_tempfile_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#redo_log_file_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRedo_log_file_spec(PlSqlParserParser.Redo_log_file_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#autoextend_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAutoextend_clause(PlSqlParserParser.Autoextend_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#maxsize_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaxsize_clause(PlSqlParserParser.Maxsize_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#build_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBuild_clause(PlSqlParserParser.Build_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#parallel_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParallel_clause(PlSqlParserParser.Parallel_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_materialized_view}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_materialized_view(PlSqlParserParser.Alter_materialized_viewContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_mv_option1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_mv_option1(PlSqlParserParser.Alter_mv_option1Context ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_mv_refresh}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_mv_refresh(PlSqlParserParser.Alter_mv_refreshContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#rollback_segment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRollback_segment(PlSqlParserParser.Rollback_segmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#modify_mv_column_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_mv_column_clause(PlSqlParserParser.Modify_mv_column_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_materialized_view_log}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_materialized_view_log(PlSqlParserParser.Alter_materialized_view_logContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#add_mv_log_column_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_mv_log_column_clause(PlSqlParserParser.Add_mv_log_column_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#move_mv_log_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMove_mv_log_clause(PlSqlParserParser.Move_mv_log_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#mv_log_augmentation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMv_log_augmentation(PlSqlParserParser.Mv_log_augmentationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#datetime_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatetime_expr(PlSqlParserParser.Datetime_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#interval_expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval_expr(PlSqlParserParser.Interval_exprContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#synchronous_or_asynchronous}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSynchronous_or_asynchronous(PlSqlParserParser.Synchronous_or_asynchronousContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#including_or_excluding}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIncluding_or_excluding(PlSqlParserParser.Including_or_excludingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_materialized_view_log}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_materialized_view_log(PlSqlParserParser.Create_materialized_view_logContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#new_values_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNew_values_clause(PlSqlParserParser.New_values_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#mv_log_purge_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMv_log_purge_clause(PlSqlParserParser.Mv_log_purge_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_materialized_view}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_materialized_view(PlSqlParserParser.Create_materialized_viewContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_mv_refresh}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_mv_refresh(PlSqlParserParser.Create_mv_refreshContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_context}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_context(PlSqlParserParser.Create_contextContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#oracle_namespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOracle_namespace(PlSqlParserParser.Oracle_namespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_cluster}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_cluster(PlSqlParserParser.Create_clusterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_table(PlSqlParserParser.Create_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xmltype_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltype_table(PlSqlParserParser.Xmltype_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xmltype_virtual_columns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltype_virtual_columns(PlSqlParserParser.Xmltype_virtual_columnsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xmltype_column_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltype_column_properties(PlSqlParserParser.Xmltype_column_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xmltype_storage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltype_storage(PlSqlParserParser.Xmltype_storageContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xmlschema_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlschema_spec(PlSqlParserParser.Xmlschema_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#object_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_table(PlSqlParserParser.Object_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#oid_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOid_index_clause(PlSqlParserParser.Oid_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#oid_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOid_clause(PlSqlParserParser.Oid_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#object_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_properties(PlSqlParserParser.Object_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#object_table_substitution}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_table_substitution(PlSqlParserParser.Object_table_substitutionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#relational_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelational_table(PlSqlParserParser.Relational_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#relational_property}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelational_property(PlSqlParserParser.Relational_propertyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#table_partitioning_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_partitioning_clauses(PlSqlParserParser.Table_partitioning_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#range_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange_partitions(PlSqlParserParser.Range_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#list_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList_partitions(PlSqlParserParser.List_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#hash_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHash_partitions(PlSqlParserParser.Hash_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#individual_hash_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndividual_hash_partitions(PlSqlParserParser.Individual_hash_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#hash_partitions_by_quantity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHash_partitions_by_quantity(PlSqlParserParser.Hash_partitions_by_quantityContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#hash_partition_quantity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHash_partition_quantity(PlSqlParserParser.Hash_partition_quantityContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#composite_range_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComposite_range_partitions(PlSqlParserParser.Composite_range_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#composite_list_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComposite_list_partitions(PlSqlParserParser.Composite_list_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#composite_hash_partitions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComposite_hash_partitions(PlSqlParserParser.Composite_hash_partitionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#reference_partitioning}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReference_partitioning(PlSqlParserParser.Reference_partitioningContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#reference_partition_desc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReference_partition_desc(PlSqlParserParser.Reference_partition_descContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#system_partitioning}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSystem_partitioning(PlSqlParserParser.System_partitioningContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#range_partition_desc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange_partition_desc(PlSqlParserParser.Range_partition_descContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#list_partition_desc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList_partition_desc(PlSqlParserParser.List_partition_descContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subpartition_template}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubpartition_template(PlSqlParserParser.Subpartition_templateContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#hash_subpartition_quantity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHash_subpartition_quantity(PlSqlParserParser.Hash_subpartition_quantityContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subpartition_by_range}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubpartition_by_range(PlSqlParserParser.Subpartition_by_rangeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subpartition_by_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubpartition_by_list(PlSqlParserParser.Subpartition_by_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subpartition_by_hash}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubpartition_by_hash(PlSqlParserParser.Subpartition_by_hashContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subpartition_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubpartition_name(PlSqlParserParser.Subpartition_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#range_subpartition_desc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange_subpartition_desc(PlSqlParserParser.Range_subpartition_descContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#list_subpartition_desc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList_subpartition_desc(PlSqlParserParser.List_subpartition_descContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#individual_hash_subparts}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndividual_hash_subparts(PlSqlParserParser.Individual_hash_subpartsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#hash_subparts_by_quantity}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHash_subparts_by_quantity(PlSqlParserParser.Hash_subparts_by_quantityContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#range_values_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRange_values_clause(PlSqlParserParser.Range_values_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#list_values_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitList_values_clause(PlSqlParserParser.List_values_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#table_partition_description}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_partition_description(PlSqlParserParser.Table_partition_descriptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#partitioning_storage_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartitioning_storage_clause(PlSqlParserParser.Partitioning_storage_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#lob_partitioning_storage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_partitioning_storage(PlSqlParserParser.Lob_partitioning_storageContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#datatype_null_enable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatatype_null_enable(PlSqlParserParser.Datatype_null_enableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#size_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSize_clause(PlSqlParserParser.Size_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#table_compression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_compression(PlSqlParserParser.Table_compressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#physical_attributes_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPhysical_attributes_clause(PlSqlParserParser.Physical_attributes_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#storage_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStorage_clause(PlSqlParserParser.Storage_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#deferred_segment_creation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeferred_segment_creation(PlSqlParserParser.Deferred_segment_creationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#segment_attributes_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSegment_attributes_clause(PlSqlParserParser.Segment_attributes_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#physical_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPhysical_properties(PlSqlParserParser.Physical_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#row_movement_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRow_movement_clause(PlSqlParserParser.Row_movement_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#flashback_archive_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlashback_archive_clause(PlSqlParserParser.Flashback_archive_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#log_grp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLog_grp(PlSqlParserParser.Log_grpContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#supplemental_table_logging}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSupplemental_table_logging(PlSqlParserParser.Supplemental_table_loggingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#supplemental_log_grp_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSupplemental_log_grp_clause(PlSqlParserParser.Supplemental_log_grp_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#supplemental_id_key_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSupplemental_id_key_clause(PlSqlParserParser.Supplemental_id_key_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#allocate_extent_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllocate_extent_clause(PlSqlParserParser.Allocate_extent_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#deallocate_unused_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeallocate_unused_clause(PlSqlParserParser.Deallocate_unused_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#shrink_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShrink_clause(PlSqlParserParser.Shrink_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#records_per_block_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecords_per_block_clause(PlSqlParserParser.Records_per_block_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#upgrade_table_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpgrade_table_clause(PlSqlParserParser.Upgrade_table_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#truncate_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTruncate_table(PlSqlParserParser.Truncate_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_table(PlSqlParserParser.Drop_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_view}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_view(PlSqlParserParser.Drop_viewContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#comment_on_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComment_on_column(PlSqlParserParser.Comment_on_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#enable_or_disable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnable_or_disable(PlSqlParserParser.Enable_or_disableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#allow_or_disallow}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAllow_or_disallow(PlSqlParserParser.Allow_or_disallowContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_synonym}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_synonym(PlSqlParserParser.Create_synonymContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#comment_on_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitComment_on_table(PlSqlParserParser.Comment_on_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_cluster}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_cluster(PlSqlParserParser.Alter_clusterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cache_or_nocache}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCache_or_nocache(PlSqlParserParser.Cache_or_nocacheContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#database_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatabase_name(PlSqlParserParser.Database_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_database}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_database(PlSqlParserParser.Alter_databaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#startup_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStartup_clauses(PlSqlParserParser.Startup_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#resetlogs_or_noresetlogs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitResetlogs_or_noresetlogs(PlSqlParserParser.Resetlogs_or_noresetlogsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#upgrade_or_downgrade}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpgrade_or_downgrade(PlSqlParserParser.Upgrade_or_downgradeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#recovery_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecovery_clauses(PlSqlParserParser.Recovery_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#begin_or_end}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBegin_or_end(PlSqlParserParser.Begin_or_endContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#general_recovery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneral_recovery(PlSqlParserParser.General_recoveryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#full_database_recovery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFull_database_recovery(PlSqlParserParser.Full_database_recoveryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#partial_database_recovery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartial_database_recovery(PlSqlParserParser.Partial_database_recoveryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#partial_database_recovery_10g}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartial_database_recovery_10g(PlSqlParserParser.Partial_database_recovery_10gContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#managed_standby_recovery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitManaged_standby_recovery(PlSqlParserParser.Managed_standby_recoveryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#db_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDb_name(PlSqlParserParser.Db_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#database_file_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatabase_file_clauses(PlSqlParserParser.Database_file_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#create_datafile_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCreate_datafile_clause(PlSqlParserParser.Create_datafile_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_datafile_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_datafile_clause(PlSqlParserParser.Alter_datafile_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_tempfile_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_tempfile_clause(PlSqlParserParser.Alter_tempfile_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#logfile_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogfile_clauses(PlSqlParserParser.Logfile_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#add_logfile_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_logfile_clauses(PlSqlParserParser.Add_logfile_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#log_file_group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLog_file_group(PlSqlParserParser.Log_file_groupContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_logfile_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_logfile_clauses(PlSqlParserParser.Drop_logfile_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#switch_logfile_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwitch_logfile_clause(PlSqlParserParser.Switch_logfile_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#supplemental_db_logging}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSupplemental_db_logging(PlSqlParserParser.Supplemental_db_loggingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#add_or_drop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_or_drop(PlSqlParserParser.Add_or_dropContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#supplemental_plsql_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSupplemental_plsql_clause(PlSqlParserParser.Supplemental_plsql_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#logfile_descriptor}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogfile_descriptor(PlSqlParserParser.Logfile_descriptorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#controlfile_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitControlfile_clauses(PlSqlParserParser.Controlfile_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#trace_file_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrace_file_clause(PlSqlParserParser.Trace_file_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#standby_database_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStandby_database_clauses(PlSqlParserParser.Standby_database_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#activate_standby_db_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitActivate_standby_db_clause(PlSqlParserParser.Activate_standby_db_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#maximize_standby_db_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMaximize_standby_db_clause(PlSqlParserParser.Maximize_standby_db_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#register_logfile_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegister_logfile_clause(PlSqlParserParser.Register_logfile_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#commit_switchover_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommit_switchover_clause(PlSqlParserParser.Commit_switchover_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#start_standby_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStart_standby_clause(PlSqlParserParser.Start_standby_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#stop_standby_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStop_standby_clause(PlSqlParserParser.Stop_standby_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#convert_database_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConvert_database_clause(PlSqlParserParser.Convert_database_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#default_settings_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefault_settings_clause(PlSqlParserParser.Default_settings_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#set_time_zone_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_time_zone_clause(PlSqlParserParser.Set_time_zone_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#instance_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInstance_clauses(PlSqlParserParser.Instance_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#security_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSecurity_clause(PlSqlParserParser.Security_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#domain}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDomain(PlSqlParserParser.DomainContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#database}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatabase(PlSqlParserParser.DatabaseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#edition_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEdition_name(PlSqlParserParser.Edition_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#filenumber}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilenumber(PlSqlParserParser.FilenumberContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#filename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFilename(PlSqlParserParser.FilenameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_table}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table(PlSqlParserParser.Alter_tableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_table_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table_properties(PlSqlParserParser.Alter_table_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_table_properties_1}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_table_properties_1(PlSqlParserParser.Alter_table_properties_1Context ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_iot_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_iot_clauses(PlSqlParserParser.Alter_iot_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_mapping_table_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_mapping_table_clause(PlSqlParserParser.Alter_mapping_table_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_overflow_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_overflow_clause(PlSqlParserParser.Alter_overflow_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#add_overflow_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_overflow_clause(PlSqlParserParser.Add_overflow_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#enable_disable_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnable_disable_clause(PlSqlParserParser.Enable_disable_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#using_index_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsing_index_clause(PlSqlParserParser.Using_index_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#index_attributes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_attributes(PlSqlParserParser.Index_attributesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#sort_or_nosort}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSort_or_nosort(PlSqlParserParser.Sort_or_nosortContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#exceptions_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExceptions_clause(PlSqlParserParser.Exceptions_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#move_table_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMove_table_clause(PlSqlParserParser.Move_table_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#index_org_table_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_org_table_clause(PlSqlParserParser.Index_org_table_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#mapping_table_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapping_table_clause(PlSqlParserParser.Mapping_table_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#key_compression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKey_compression(PlSqlParserParser.Key_compressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#index_org_overflow_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_org_overflow_clause(PlSqlParserParser.Index_org_overflow_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#column_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_clauses(PlSqlParserParser.Column_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#modify_collection_retrieval}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_collection_retrieval(PlSqlParserParser.Modify_collection_retrievalContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#collection_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollection_item(PlSqlParserParser.Collection_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#rename_column_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRename_column_clause(PlSqlParserParser.Rename_column_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#old_column_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOld_column_name(PlSqlParserParser.Old_column_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#new_column_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNew_column_name(PlSqlParserParser.New_column_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#add_modify_drop_column_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_modify_drop_column_clauses(PlSqlParserParser.Add_modify_drop_column_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_column_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_column_clause(PlSqlParserParser.Drop_column_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#modify_column_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_column_clauses(PlSqlParserParser.Modify_column_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#modify_col_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_col_properties(PlSqlParserParser.Modify_col_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#modify_col_substitutable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_col_substitutable(PlSqlParserParser.Modify_col_substitutableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#add_column_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_column_clause(PlSqlParserParser.Add_column_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#alter_varray_col_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlter_varray_col_properties(PlSqlParserParser.Alter_varray_col_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#varray_col_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarray_col_properties(PlSqlParserParser.Varray_col_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#varray_storage_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarray_storage_clause(PlSqlParserParser.Varray_storage_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#lob_segname}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_segname(PlSqlParserParser.Lob_segnameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#lob_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_item(PlSqlParserParser.Lob_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#lob_storage_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_storage_parameters(PlSqlParserParser.Lob_storage_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#lob_storage_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_storage_clause(PlSqlParserParser.Lob_storage_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#modify_lob_storage_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_lob_storage_clause(PlSqlParserParser.Modify_lob_storage_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#modify_lob_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModify_lob_parameters(PlSqlParserParser.Modify_lob_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#lob_parameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_parameters(PlSqlParserParser.Lob_parametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#lob_deduplicate_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_deduplicate_clause(PlSqlParserParser.Lob_deduplicate_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#lob_compression_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_compression_clause(PlSqlParserParser.Lob_compression_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#lob_retention_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLob_retention_clause(PlSqlParserParser.Lob_retention_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#encryption_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEncryption_spec(PlSqlParserParser.Encryption_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#tablespace}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablespace(PlSqlParserParser.TablespaceContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#varray_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarray_item(PlSqlParserParser.Varray_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#column_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_properties(PlSqlParserParser.Column_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#period_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPeriod_definition(PlSqlParserParser.Period_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#start_time_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStart_time_column(PlSqlParserParser.Start_time_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#end_time_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnd_time_column(PlSqlParserParser.End_time_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#column_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_definition(PlSqlParserParser.Column_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#virtual_column_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVirtual_column_definition(PlSqlParserParser.Virtual_column_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#autogenerated_sequence_definition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAutogenerated_sequence_definition(PlSqlParserParser.Autogenerated_sequence_definitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#out_of_line_part_storage}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOut_of_line_part_storage(PlSqlParserParser.Out_of_line_part_storageContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#nested_table_col_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNested_table_col_properties(PlSqlParserParser.Nested_table_col_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#nested_item}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNested_item(PlSqlParserParser.Nested_itemContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#substitutable_column_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubstitutable_column_clause(PlSqlParserParser.Substitutable_column_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#partition_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_name(PlSqlParserParser.Partition_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#supplemental_logging_props}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSupplemental_logging_props(PlSqlParserParser.Supplemental_logging_propsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#column_or_attribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_or_attribute(PlSqlParserParser.Column_or_attributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#object_type_col_properties}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_type_col_properties(PlSqlParserParser.Object_type_col_propertiesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#constraint_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraint_clauses(PlSqlParserParser.Constraint_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#old_constraint_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOld_constraint_name(PlSqlParserParser.Old_constraint_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#new_constraint_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNew_constraint_name(PlSqlParserParser.New_constraint_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_constraint_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_constraint_clause(PlSqlParserParser.Drop_constraint_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_primary_key_or_unique_or_generic_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_primary_key_or_unique_or_generic_clause(PlSqlParserParser.Drop_primary_key_or_unique_or_generic_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#add_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_constraint(PlSqlParserParser.Add_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#add_constraint_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAdd_constraint_clause(PlSqlParserParser.Add_constraint_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#check_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCheck_constraint(PlSqlParserParser.Check_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#drop_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDrop_constraint(PlSqlParserParser.Drop_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#enable_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEnable_constraint(PlSqlParserParser.Enable_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#disable_constraint}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDisable_constraint(PlSqlParserParser.Disable_constraintContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#foreign_key_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeign_key_clause(PlSqlParserParser.Foreign_key_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#references_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReferences_clause(PlSqlParserParser.References_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#on_delete_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOn_delete_clause(PlSqlParserParser.On_delete_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#unique_key_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnique_key_clause(PlSqlParserParser.Unique_key_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#primary_key_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary_key_clause(PlSqlParserParser.Primary_key_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#anonymous_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnonymous_block(PlSqlParserParser.Anonymous_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#invoker_rights_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInvoker_rights_clause(PlSqlParserParser.Invoker_rights_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#call_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCall_spec(PlSqlParserParser.Call_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#java_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJava_spec(PlSqlParserParser.Java_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#c_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitC_spec(PlSqlParserParser.C_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#c_agent_in_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitC_agent_in_clause(PlSqlParserParser.C_agent_in_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#c_parameters_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitC_parameters_clause(PlSqlParserParser.C_parameters_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#parameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter(PlSqlParserParser.ParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#default_value_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDefault_value_part(PlSqlParserParser.Default_value_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#seq_of_declare_specs}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeq_of_declare_specs(PlSqlParserParser.Seq_of_declare_specsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#declare_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDeclare_spec(PlSqlParserParser.Declare_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#variable_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_declaration(PlSqlParserParser.Variable_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subtype_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubtype_declaration(PlSqlParserParser.Subtype_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cursor_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_declaration(PlSqlParserParser.Cursor_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#parameter_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_spec(PlSqlParserParser.Parameter_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#exception_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitException_declaration(PlSqlParserParser.Exception_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#pragma_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPragma_declaration(PlSqlParserParser.Pragma_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#record_type_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecord_type_def(PlSqlParserParser.Record_type_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#field_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField_spec(PlSqlParserParser.Field_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#ref_cursor_type_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRef_cursor_type_def(PlSqlParserParser.Ref_cursor_type_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#type_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_declaration(PlSqlParserParser.Type_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#table_type_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_type_def(PlSqlParserParser.Table_type_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#table_indexed_by_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_indexed_by_part(PlSqlParserParser.Table_indexed_by_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#varray_type_def}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVarray_type_def(PlSqlParserParser.Varray_type_defContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#seq_of_statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeq_of_statements(PlSqlParserParser.Seq_of_statementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#label_declaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabel_declaration(PlSqlParserParser.Label_declarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(PlSqlParserParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#swallow_to_semi}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSwallow_to_semi(PlSqlParserParser.Swallow_to_semiContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#assignment_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignment_statement(PlSqlParserParser.Assignment_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#continue_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContinue_statement(PlSqlParserParser.Continue_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#exit_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExit_statement(PlSqlParserParser.Exit_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#goto_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGoto_statement(PlSqlParserParser.Goto_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#if_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIf_statement(PlSqlParserParser.If_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#elsif_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElsif_part(PlSqlParserParser.Elsif_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#else_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElse_part(PlSqlParserParser.Else_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#loop_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLoop_statement(PlSqlParserParser.Loop_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cursor_loop_param}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_loop_param(PlSqlParserParser.Cursor_loop_paramContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#forall_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForall_statement(PlSqlParserParser.Forall_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#bounds_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBounds_clause(PlSqlParserParser.Bounds_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#between_bound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBetween_bound(PlSqlParserParser.Between_boundContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#lower_bound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLower_bound(PlSqlParserParser.Lower_boundContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#upper_bound}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpper_bound(PlSqlParserParser.Upper_boundContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#null_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNull_statement(PlSqlParserParser.Null_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#raise_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRaise_statement(PlSqlParserParser.Raise_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#return_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_statement(PlSqlParserParser.Return_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#function_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_call(PlSqlParserParser.Function_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#procedure_call}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure_call(PlSqlParserParser.Procedure_callContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#pipe_row_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPipe_row_statement(PlSqlParserParser.Pipe_row_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#body}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBody(PlSqlParserParser.BodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#exception_handler}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitException_handler(PlSqlParserParser.Exception_handlerContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#trigger_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_block(PlSqlParserParser.Trigger_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlock(PlSqlParserParser.BlockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#sql_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_statement(PlSqlParserParser.Sql_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#execute_immediate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExecute_immediate(PlSqlParserParser.Execute_immediateContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#dynamic_returning_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDynamic_returning_clause(PlSqlParserParser.Dynamic_returning_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#data_manipulation_language_statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitData_manipulation_language_statements(PlSqlParserParser.Data_manipulation_language_statementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cursor_manipulation_statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_manipulation_statements(PlSqlParserParser.Cursor_manipulation_statementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#close_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClose_statement(PlSqlParserParser.Close_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#open_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpen_statement(PlSqlParserParser.Open_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#fetch_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFetch_statement(PlSqlParserParser.Fetch_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#open_for_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOpen_for_statement(PlSqlParserParser.Open_for_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#transaction_control_statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTransaction_control_statements(PlSqlParserParser.Transaction_control_statementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#set_transaction_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_transaction_command(PlSqlParserParser.Set_transaction_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#set_constraint_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_constraint_command(PlSqlParserParser.Set_constraint_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#commit_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCommit_statement(PlSqlParserParser.Commit_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#write_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWrite_clause(PlSqlParserParser.Write_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#rollback_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRollback_statement(PlSqlParserParser.Rollback_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#savepoint_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSavepoint_statement(PlSqlParserParser.Savepoint_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#explain_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplain_statement(PlSqlParserParser.Explain_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#select_only_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_only_statement(PlSqlParserParser.Select_only_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#select_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_statement(PlSqlParserParser.Select_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subquery_factoring_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery_factoring_clause(PlSqlParserParser.Subquery_factoring_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#factoring_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFactoring_element(PlSqlParserParser.Factoring_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#search_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearch_clause(PlSqlParserParser.Search_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cycle_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCycle_clause(PlSqlParserParser.Cycle_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subquery}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery(PlSqlParserParser.SubqueryContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subquery_basic_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery_basic_elements(PlSqlParserParser.Subquery_basic_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subquery_operation_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery_operation_part(PlSqlParserParser.Subquery_operation_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#query_block}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_block(PlSqlParserParser.Query_blockContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#selected_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelected_list(PlSqlParserParser.Selected_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#from_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFrom_clause(PlSqlParserParser.From_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#select_list_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect_list_elements(PlSqlParserParser.Select_list_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#table_ref_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_ref_list(PlSqlParserParser.Table_ref_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#table_ref}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_ref(PlSqlParserParser.Table_refContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#table_ref_aux}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_ref_aux(PlSqlParserParser.Table_ref_auxContext ctx);
	/**
	 * Visit a parse tree produced by the {@code table_ref_aux_internal_one}
	 * labeled alternative in {@link PlSqlParserParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_ref_aux_internal_one(PlSqlParserParser.Table_ref_aux_internal_oneContext ctx);
	/**
	 * Visit a parse tree produced by the {@code table_ref_aux_internal_two}
	 * labeled alternative in {@link PlSqlParserParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_ref_aux_internal_two(PlSqlParserParser.Table_ref_aux_internal_twoContext ctx);
	/**
	 * Visit a parse tree produced by the {@code table_ref_aux_internal_three}
	 * labeled alternative in {@link PlSqlParserParser#table_ref_aux_internal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_ref_aux_internal_three(PlSqlParserParser.Table_ref_aux_internal_threeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#join_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoin_clause(PlSqlParserParser.Join_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#join_on_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoin_on_part(PlSqlParserParser.Join_on_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#join_using_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitJoin_using_part(PlSqlParserParser.Join_using_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#outer_join_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOuter_join_type(PlSqlParserParser.Outer_join_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#query_partition_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_partition_clause(PlSqlParserParser.Query_partition_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#flashback_query_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFlashback_query_clause(PlSqlParserParser.Flashback_query_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#pivot_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot_clause(PlSqlParserParser.Pivot_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#pivot_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot_element(PlSqlParserParser.Pivot_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#pivot_for_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot_for_clause(PlSqlParserParser.Pivot_for_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#pivot_in_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot_in_clause(PlSqlParserParser.Pivot_in_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#pivot_in_clause_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot_in_clause_element(PlSqlParserParser.Pivot_in_clause_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#pivot_in_clause_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPivot_in_clause_elements(PlSqlParserParser.Pivot_in_clause_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#unpivot_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivot_clause(PlSqlParserParser.Unpivot_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#unpivot_in_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivot_in_clause(PlSqlParserParser.Unpivot_in_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#unpivot_in_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnpivot_in_elements(PlSqlParserParser.Unpivot_in_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#hierarchical_query_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHierarchical_query_clause(PlSqlParserParser.Hierarchical_query_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#start_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStart_part(PlSqlParserParser.Start_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#group_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup_by_clause(PlSqlParserParser.Group_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#group_by_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup_by_elements(PlSqlParserParser.Group_by_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#rollup_cube_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRollup_cube_clause(PlSqlParserParser.Rollup_cube_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#grouping_sets_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrouping_sets_clause(PlSqlParserParser.Grouping_sets_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#grouping_sets_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrouping_sets_elements(PlSqlParserParser.Grouping_sets_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#having_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitHaving_clause(PlSqlParserParser.Having_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#model_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_clause(PlSqlParserParser.Model_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cell_reference_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCell_reference_options(PlSqlParserParser.Cell_reference_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#return_rows_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReturn_rows_clause(PlSqlParserParser.Return_rows_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#reference_model}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReference_model(PlSqlParserParser.Reference_modelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#main_model}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMain_model(PlSqlParserParser.Main_modelContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#model_column_clauses}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_column_clauses(PlSqlParserParser.Model_column_clausesContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#model_column_partition_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_column_partition_part(PlSqlParserParser.Model_column_partition_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#model_column_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_column_list(PlSqlParserParser.Model_column_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#model_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_column(PlSqlParserParser.Model_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#model_rules_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_rules_clause(PlSqlParserParser.Model_rules_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#model_rules_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_rules_part(PlSqlParserParser.Model_rules_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#model_rules_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_rules_element(PlSqlParserParser.Model_rules_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cell_assignment}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCell_assignment(PlSqlParserParser.Cell_assignmentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#model_iterate_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_iterate_clause(PlSqlParserParser.Model_iterate_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#until_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUntil_part(PlSqlParserParser.Until_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#order_by_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrder_by_clause(PlSqlParserParser.Order_by_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#order_by_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrder_by_elements(PlSqlParserParser.Order_by_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#offset_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOffset_clause(PlSqlParserParser.Offset_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#fetch_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFetch_clause(PlSqlParserParser.Fetch_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#for_update_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_update_clause(PlSqlParserParser.For_update_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#for_update_of_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_update_of_part(PlSqlParserParser.For_update_of_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#for_update_options}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFor_update_options(PlSqlParserParser.For_update_optionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#update_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_statement(PlSqlParserParser.Update_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#update_set_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate_set_clause(PlSqlParserParser.Update_set_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#column_based_update_set_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_based_update_set_clause(PlSqlParserParser.Column_based_update_set_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#delete_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDelete_statement(PlSqlParserParser.Delete_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#insert_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_statement(PlSqlParserParser.Insert_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#single_table_insert}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_table_insert(PlSqlParserParser.Single_table_insertContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#multi_table_insert}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulti_table_insert(PlSqlParserParser.Multi_table_insertContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#multi_table_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulti_table_element(PlSqlParserParser.Multi_table_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#conditional_insert_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditional_insert_clause(PlSqlParserParser.Conditional_insert_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#conditional_insert_when_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditional_insert_when_part(PlSqlParserParser.Conditional_insert_when_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#conditional_insert_else_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConditional_insert_else_part(PlSqlParserParser.Conditional_insert_else_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#insert_into_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert_into_clause(PlSqlParserParser.Insert_into_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#values_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValues_clause(PlSqlParserParser.Values_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#merge_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_statement(PlSqlParserParser.Merge_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#merge_update_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_update_clause(PlSqlParserParser.Merge_update_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#merge_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_element(PlSqlParserParser.Merge_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#merge_update_delete_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_update_delete_part(PlSqlParserParser.Merge_update_delete_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#merge_insert_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMerge_insert_clause(PlSqlParserParser.Merge_insert_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#selected_tableview}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelected_tableview(PlSqlParserParser.Selected_tableviewContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#lock_table_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLock_table_statement(PlSqlParserParser.Lock_table_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#wait_nowait_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWait_nowait_part(PlSqlParserParser.Wait_nowait_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#lock_table_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLock_table_element(PlSqlParserParser.Lock_table_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#lock_mode}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLock_mode(PlSqlParserParser.Lock_modeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#general_table_ref}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneral_table_ref(PlSqlParserParser.General_table_refContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#static_returning_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatic_returning_clause(PlSqlParserParser.Static_returning_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#error_logging_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitError_logging_clause(PlSqlParserParser.Error_logging_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#error_logging_into_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitError_logging_into_part(PlSqlParserParser.Error_logging_into_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#error_logging_reject_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitError_logging_reject_part(PlSqlParserParser.Error_logging_reject_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#dml_table_expression_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDml_table_expression_clause(PlSqlParserParser.Dml_table_expression_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#table_collection_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_collection_expression(PlSqlParserParser.Table_collection_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#subquery_restriction_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubquery_restriction_clause(PlSqlParserParser.Subquery_restriction_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#sample_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSample_clause(PlSqlParserParser.Sample_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#seed_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSeed_part(PlSqlParserParser.Seed_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#condition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCondition(PlSqlParserParser.ConditionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#expressions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressions(PlSqlParserParser.ExpressionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpression(PlSqlParserParser.ExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cursor_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_expression(PlSqlParserParser.Cursor_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#logical_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogical_expression(PlSqlParserParser.Logical_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#unary_logical_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary_logical_expression(PlSqlParserParser.Unary_logical_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#logical_operation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLogical_operation(PlSqlParserParser.Logical_operationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#multiset_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMultiset_expression(PlSqlParserParser.Multiset_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#relational_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelational_expression(PlSqlParserParser.Relational_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#compound_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompound_expression(PlSqlParserParser.Compound_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#relational_operator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelational_operator(PlSqlParserParser.Relational_operatorContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#in_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIn_elements(PlSqlParserParser.In_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#between_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBetween_elements(PlSqlParserParser.Between_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#concatenation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConcatenation(PlSqlParserParser.ConcatenationContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#interval_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInterval_expression(PlSqlParserParser.Interval_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#model_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_expression(PlSqlParserParser.Model_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#model_expression_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitModel_expression_element(PlSqlParserParser.Model_expression_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#single_column_for_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingle_column_for_loop(PlSqlParserParser.Single_column_for_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#multi_column_for_loop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMulti_column_for_loop(PlSqlParserParser.Multi_column_for_loopContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#unary_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary_expression(PlSqlParserParser.Unary_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#case_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_statement(PlSqlParserParser.Case_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#simple_case_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_case_statement(PlSqlParserParser.Simple_case_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#simple_case_when_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSimple_case_when_part(PlSqlParserParser.Simple_case_when_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#searched_case_statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearched_case_statement(PlSqlParserParser.Searched_case_statementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#searched_case_when_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSearched_case_when_part(PlSqlParserParser.Searched_case_when_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#case_else_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCase_else_part(PlSqlParserParser.Case_else_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#atom}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAtom(PlSqlParserParser.AtomContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#quantified_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuantified_expression(PlSqlParserParser.Quantified_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#string_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString_function(PlSqlParserParser.String_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#standard_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStandard_function(PlSqlParserParser.Standard_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#literal}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteral(PlSqlParserParser.LiteralContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#numeric_function_wrapper}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric_function_wrapper(PlSqlParserParser.Numeric_function_wrapperContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#numeric_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric_function(PlSqlParserParser.Numeric_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#other_function}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOther_function(PlSqlParserParser.Other_functionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#over_clause_keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOver_clause_keyword(PlSqlParserParser.Over_clause_keywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#within_or_over_clause_keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithin_or_over_clause_keyword(PlSqlParserParser.Within_or_over_clause_keywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#standard_prediction_function_keyword}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStandard_prediction_function_keyword(PlSqlParserParser.Standard_prediction_function_keywordContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#over_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOver_clause(PlSqlParserParser.Over_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#windowing_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowing_clause(PlSqlParserParser.Windowing_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#windowing_type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowing_type(PlSqlParserParser.Windowing_typeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#windowing_elements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWindowing_elements(PlSqlParserParser.Windowing_elementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#using_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsing_clause(PlSqlParserParser.Using_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#using_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUsing_element(PlSqlParserParser.Using_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#collect_order_by_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollect_order_by_part(PlSqlParserParser.Collect_order_by_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#within_or_over_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWithin_or_over_part(PlSqlParserParser.Within_or_over_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cost_matrix_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCost_matrix_clause(PlSqlParserParser.Cost_matrix_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xml_passing_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_passing_clause(PlSqlParserParser.Xml_passing_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xml_attributes_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_attributes_clause(PlSqlParserParser.Xml_attributes_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xml_namespaces_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_namespaces_clause(PlSqlParserParser.Xml_namespaces_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xml_table_column}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_table_column(PlSqlParserParser.Xml_table_columnContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xml_general_default_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_general_default_part(PlSqlParserParser.Xml_general_default_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xml_multiuse_expression_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_multiuse_expression_element(PlSqlParserParser.Xml_multiuse_expression_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xmlroot_param_version_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlroot_param_version_part(PlSqlParserParser.Xmlroot_param_version_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xmlroot_param_standalone_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlroot_param_standalone_part(PlSqlParserParser.Xmlroot_param_standalone_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xmlserialize_param_enconding_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlserialize_param_enconding_part(PlSqlParserParser.Xmlserialize_param_enconding_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xmlserialize_param_version_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlserialize_param_version_part(PlSqlParserParser.Xmlserialize_param_version_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xmlserialize_param_ident_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmlserialize_param_ident_part(PlSqlParserParser.Xmlserialize_param_ident_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#sql_plus_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSql_plus_command(PlSqlParserParser.Sql_plus_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#whenever_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhenever_command(PlSqlParserParser.Whenever_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#set_command}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet_command(PlSqlParserParser.Set_commandContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#partition_extension_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPartition_extension_clause(PlSqlParserParser.Partition_extension_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#column_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_alias(PlSqlParserParser.Column_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#table_alias}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_alias(PlSqlParserParser.Table_aliasContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#where_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhere_clause(PlSqlParserParser.Where_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#into_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInto_clause(PlSqlParserParser.Into_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xml_column_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXml_column_name(PlSqlParserParser.Xml_column_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cost_class_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCost_class_name(PlSqlParserParser.Cost_class_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#attribute_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttribute_name(PlSqlParserParser.Attribute_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#savepoint_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSavepoint_name(PlSqlParserParser.Savepoint_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#rollback_segment_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRollback_segment_name(PlSqlParserParser.Rollback_segment_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#table_var_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_var_name(PlSqlParserParser.Table_var_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#schema_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchema_name(PlSqlParserParser.Schema_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#routine_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRoutine_name(PlSqlParserParser.Routine_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#package_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPackage_name(PlSqlParserParser.Package_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#implementation_type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImplementation_type_name(PlSqlParserParser.Implementation_type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#parameter_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParameter_name(PlSqlParserParser.Parameter_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#reference_model_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReference_model_name(PlSqlParserParser.Reference_model_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#main_model_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMain_model_name(PlSqlParserParser.Main_model_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#container_tableview_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitContainer_tableview_name(PlSqlParserParser.Container_tableview_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#aggregate_function_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAggregate_function_name(PlSqlParserParser.Aggregate_function_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#query_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuery_name(PlSqlParserParser.Query_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#grantee_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrantee_name(PlSqlParserParser.Grantee_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#role_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRole_name(PlSqlParserParser.Role_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#constraint_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstraint_name(PlSqlParserParser.Constraint_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#label_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLabel_name(PlSqlParserParser.Label_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#type_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_name(PlSqlParserParser.Type_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#sequence_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSequence_name(PlSqlParserParser.Sequence_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#exception_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitException_name(PlSqlParserParser.Exception_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#function_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_name(PlSqlParserParser.Function_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#procedure_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedure_name(PlSqlParserParser.Procedure_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#trigger_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTrigger_name(PlSqlParserParser.Trigger_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#variable_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable_name(PlSqlParserParser.Variable_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#index_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndex_name(PlSqlParserParser.Index_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#cursor_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCursor_name(PlSqlParserParser.Cursor_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#record_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRecord_name(PlSqlParserParser.Record_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#collection_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCollection_name(PlSqlParserParser.Collection_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#link_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLink_name(PlSqlParserParser.Link_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#column_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_name(PlSqlParserParser.Column_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#tableview_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTableview_name(PlSqlParserParser.Tableview_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#xmltable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitXmltable(PlSqlParserParser.XmltableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#char_set_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChar_set_name(PlSqlParserParser.Char_set_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#synonym_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSynonym_name(PlSqlParserParser.Synonym_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#schema_object_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSchema_object_name(PlSqlParserParser.Schema_object_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#dir_object_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDir_object_name(PlSqlParserParser.Dir_object_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#user_object_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUser_object_name(PlSqlParserParser.User_object_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#grant_object_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGrant_object_name(PlSqlParserParser.Grant_object_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#column_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumn_list(PlSqlParserParser.Column_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#paren_column_list}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitParen_column_list(PlSqlParserParser.Paren_column_listContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#keep_clause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitKeep_clause(PlSqlParserParser.Keep_clauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#function_argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_argument(PlSqlParserParser.Function_argumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#function_argument_analytic}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_argument_analytic(PlSqlParserParser.Function_argument_analyticContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#function_argument_modeling}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunction_argument_modeling(PlSqlParserParser.Function_argument_modelingContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#respect_or_ignore_nulls}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRespect_or_ignore_nulls(PlSqlParserParser.Respect_or_ignore_nullsContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#argument}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgument(PlSqlParserParser.ArgumentContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#type_spec}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType_spec(PlSqlParserParser.Type_specContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#datatype}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDatatype(PlSqlParserParser.DatatypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#precision_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrecision_part(PlSqlParserParser.Precision_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#native_datatype_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNative_datatype_element(PlSqlParserParser.Native_datatype_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#bind_variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBind_variable(PlSqlParserParser.Bind_variableContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#general_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneral_element(PlSqlParserParser.General_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#general_element_part}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneral_element_part(PlSqlParserParser.General_element_partContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#table_element}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTable_element(PlSqlParserParser.Table_elementContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#object_privilege}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObject_privilege(PlSqlParserParser.Object_privilegeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#system_privilege}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSystem_privilege(PlSqlParserParser.System_privilegeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#constant}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConstant(PlSqlParserParser.ConstantContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#numeric}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric(PlSqlParserParser.NumericContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#numeric_negative}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric_negative(PlSqlParserParser.Numeric_negativeContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#quoted_string}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQuoted_string(PlSqlParserParser.Quoted_stringContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#identifier}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIdentifier(PlSqlParserParser.IdentifierContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#id_expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitId_expression(PlSqlParserParser.Id_expressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#outer_join_sign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOuter_join_sign(PlSqlParserParser.Outer_join_signContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#regular_id}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegular_id(PlSqlParserParser.Regular_idContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#non_reserved_keywords_in_12c}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNon_reserved_keywords_in_12c(PlSqlParserParser.Non_reserved_keywords_in_12cContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#non_reserved_keywords_pre12c}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNon_reserved_keywords_pre12c(PlSqlParserParser.Non_reserved_keywords_pre12cContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#string_function_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitString_function_name(PlSqlParserParser.String_function_nameContext ctx);
	/**
	 * Visit a parse tree produced by {@link PlSqlParserParser#numeric_function_name}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNumeric_function_name(PlSqlParserParser.Numeric_function_nameContext ctx);
}