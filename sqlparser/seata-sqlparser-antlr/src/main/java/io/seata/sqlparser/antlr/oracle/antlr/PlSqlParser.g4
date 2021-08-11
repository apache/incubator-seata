 /**
 * Oracle(c) PL/SQL 11g Parser
 *
 * Copyright (c) 2009-2011 Alexandre Porcelli <alexandre.porcelli@gmail.com>
 * Copyright (c) 2015-2019 Ivan Kochurkin (KvanTTT, kvanttt@gmail.com, Positive Technologies).
 * Copyright (c) 2017-2018 Mark Adams <madams51703@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

grammar PlSqlParser;

import PlSqlLexer;


@parser::postinclude {
#include <PlSqlParserBase.h>
}

sql_script
    : ((unit_statement | sql_plus_command) SEMICOLON?)* EOF
    ;

unit_statement
    : transaction_control_statements
    | alter_cluster
    | alter_database
    | alter_function
    | alter_package
    | alter_procedure
    | alter_sequence
    | alter_session
    | alter_trigger
    | alter_type
    | alter_table
    | alter_tablespace
    | alter_index
    | alter_library
    | alter_materialized_view
    | alter_materialized_view_log
    | alter_user
    | alter_view

    | analyze
    | associate_statistics
    | audit_traditional
    | unified_auditing

    | create_function_body
    | create_procedure_body
    | create_package
    | create_package_body

    | create_index
    | create_table
    | create_tablespace
    | create_cluster
    | create_context
    | create_view //TODO
    | create_directory
    | create_materialized_view
    | create_materialized_view_log
    | create_user

    | create_sequence
    | create_trigger
    | create_type
    | create_synonym

    | drop_function
    | drop_package
    | drop_procedure
    | drop_sequence
    | drop_trigger
    | drop_type
    | data_manipulation_language_statements
    | truncate_table
    | drop_table
    | drop_view
    | drop_index

    | rename_object

    | comment_on_column
    | comment_on_table

    | anonymous_block

    | grant_statement

    | procedure_call
    ;

// DDL -> SQL Statements for Stored PL/SQL Units

// Function DDLs

drop_function
    : DROP FUNCTION function_name ';'
    ;

alter_function
    : ALTER FUNCTION function_name COMPILE DEBUG? compiler_parameters_clause* (REUSE SETTINGS)? ';'
    ;

create_function_body
    : CREATE (OR REPLACE)? FUNCTION function_name ('(' parameter (',' parameter)* ')')?
      RETURN type_spec (invoker_rights_clause | parallel_enable_clause | result_cache_clause | DETERMINISTIC)*
      ((PIPELINED? (IS | AS) (DECLARE? seq_of_declare_specs? body | call_spec)) | (PIPELINED | AGGREGATE) USING implementation_type_name) ';'
    ;

// Creation Function - Specific Clauses

parallel_enable_clause
    : PARALLEL_ENABLE partition_by_clause?
    ;

partition_by_clause
    : '(' PARTITION expression BY (ANY | (HASH | RANGE | LIST) paren_column_list) streaming_clause? ')'
    ;

result_cache_clause
    : RESULT_CACHE relies_on_part?
    ;

relies_on_part
    : RELIES_ON '(' tableview_name (',' tableview_name)* ')'
    ;

streaming_clause
    : (ORDER | CLUSTER) expression BY paren_column_list
    ;

// Package DDLs

drop_package
    : DROP PACKAGE BODY? (schema_object_name '.')? package_name ';'
    ;

alter_package
    : ALTER PACKAGE package_name COMPILE DEBUG? (PACKAGE | BODY | SPECIFICATION)? compiler_parameters_clause* (REUSE SETTINGS)? ';'
    ;

create_package
    : CREATE (OR REPLACE)? PACKAGE (schema_object_name '.')? package_name invoker_rights_clause? (IS | AS) package_obj_spec* END package_name? ';'
    ;

create_package_body
    : CREATE (OR REPLACE)? PACKAGE BODY (schema_object_name '.')? package_name (IS | AS) package_obj_body* (BEGIN seq_of_statements)? END package_name? ';'
    ;

// Create Package Specific Clauses

package_obj_spec
    : pragma_declaration
    | exception_declaration
    | variable_declaration
    | subtype_declaration
    | cursor_declaration
    | type_declaration
    | procedure_spec
    | function_spec
    ;

procedure_spec
    : PROCEDURE identifier ('(' parameter ( ',' parameter )* ')')? ';'
    ;

function_spec
    : FUNCTION identifier ('(' parameter ( ',' parameter)* ')')?
      RETURN type_spec PIPELINED? DETERMINISTIC? (RESULT_CACHE)? ';'
    ;

package_obj_body
    : exception_declaration
    | subtype_declaration
    | cursor_declaration
    | variable_declaration
    | type_declaration
    | procedure_body
    | function_body
    | procedure_spec
    | function_spec
    ;

// Procedure DDLs

drop_procedure
    : DROP PROCEDURE procedure_name ';'
    ;

alter_procedure
    : ALTER PROCEDURE procedure_name COMPILE DEBUG? compiler_parameters_clause* (REUSE SETTINGS)? ';'
    ;

function_body
    : FUNCTION identifier ('(' parameter (',' parameter)* ')')?
      RETURN type_spec (invoker_rights_clause | parallel_enable_clause | result_cache_clause | DETERMINISTIC)*
      ((PIPELINED? DETERMINISTIC? (IS | AS) (DECLARE? seq_of_declare_specs? body | call_spec)) | (PIPELINED | AGGREGATE) USING implementation_type_name) ';'
    ;

procedure_body
    : PROCEDURE identifier ('(' parameter (',' parameter)* ')')? (IS | AS)
      (DECLARE? seq_of_declare_specs? body | call_spec | EXTERNAL) ';'
    ;

create_procedure_body
    : CREATE (OR REPLACE)? PROCEDURE procedure_name ('(' parameter (',' parameter)* ')')?
      invoker_rights_clause? (IS | AS)
      (DECLARE? seq_of_declare_specs? body | call_spec | EXTERNAL) ';'
    ;

// Trigger DDLs

drop_trigger
    : DROP TRIGGER trigger_name ';'
    ;

alter_trigger
    : ALTER TRIGGER alter_trigger_name=trigger_name
      ((ENABLE | DISABLE) | RENAME TO rename_trigger_name=trigger_name | COMPILE DEBUG? compiler_parameters_clause* (REUSE SETTINGS)?) ';'
    ;

create_trigger
    : CREATE ( OR REPLACE )? TRIGGER trigger_name
      (simple_dml_trigger | compound_dml_trigger | non_dml_trigger)
      trigger_follows_clause? (ENABLE | DISABLE)? trigger_when_clause? trigger_body ';'
    ;

trigger_follows_clause
    : FOLLOWS trigger_name (',' trigger_name)*
    ;

trigger_when_clause
    : WHEN '(' condition ')'
    ;

// Create Trigger Specific Clauses

simple_dml_trigger
    : (BEFORE | AFTER | INSTEAD OF) dml_event_clause referencing_clause? for_each_row?
    ;

for_each_row
    : FOR EACH ROW
    ;

compound_dml_trigger
    : FOR dml_event_clause referencing_clause?
    ;

non_dml_trigger
    : (BEFORE | AFTER) non_dml_event (OR non_dml_event)* ON (DATABASE | (schema_name '.')? SCHEMA)
    ;

trigger_body
    : COMPOUND TRIGGER
    | CALL identifier
    | trigger_block
    ;

routine_clause
    : routine_name function_argument?
    ;

compound_trigger_block
    : COMPOUND TRIGGER seq_of_declare_specs? timing_point_section+ END trigger_name
    ;

timing_point_section
    : bk=BEFORE STATEMENT IS trigger_block BEFORE STATEMENT ';'
    | bk=BEFORE EACH ROW IS trigger_block BEFORE EACH ROW ';'
    | ak=AFTER STATEMENT IS trigger_block AFTER STATEMENT ';'
    | ak=AFTER EACH ROW IS trigger_block AFTER EACH ROW ';'
    ;

non_dml_event
    : ALTER
    | ANALYZE
    | ASSOCIATE STATISTICS
    | AUDIT
    | COMMENT
    | CREATE
    | DISASSOCIATE STATISTICS
    | DROP
    | GRANT
    | NOAUDIT
    | RENAME
    | REVOKE
    | TRUNCATE
    | DDL
    | STARTUP
    | SHUTDOWN
    | DB_ROLE_CHANGE
    | LOGON
    | LOGOFF
    | SERVERERROR
    | SUSPEND
    | DATABASE
    | SCHEMA
    | FOLLOWS
    ;

dml_event_clause
    : dml_event_element (OR dml_event_element)* ON dml_event_nested_clause? tableview_name
    ;

dml_event_element
    : (DELETE | INSERT | UPDATE) (OF column_list)?
    ;

dml_event_nested_clause
    : NESTED TABLE tableview_name OF
    ;

referencing_clause
    : REFERENCING referencing_element+
    ;

referencing_element
    : (NEW | OLD | PARENT) column_alias
    ;

// DDLs

drop_type
    : DROP TYPE BODY? type_name (FORCE | VALIDATE)? ';'
    ;

alter_type
    : ALTER TYPE type_name
    (compile_type_clause
    | replace_type_clause
    //TODO | {input.LT(2).getText().equalsIgnoreCase("attribute")}? alter_attribute_definition
    | alter_method_spec
    | alter_collection_clauses
    | modifier_clause
    | overriding_subprogram_spec
    ) dependent_handling_clause? ';'
    ;

// Alter Type Specific Clauses

compile_type_clause
    : COMPILE DEBUG? (SPECIFICATION | BODY)? compiler_parameters_clause* (REUSE SETTINGS)?
    ;

replace_type_clause
    : REPLACE invoker_rights_clause? AS OBJECT '(' object_member_spec (',' object_member_spec)* ')'
    ;

alter_method_spec
    : alter_method_element (',' alter_method_element)*
    ;

alter_method_element
    : (ADD | DROP) (map_order_function_spec | subprogram_spec)
    ;

alter_attribute_definition
    : (ADD | MODIFY | DROP) ATTRIBUTE (attribute_definition | '(' attribute_definition (',' attribute_definition)* ')')
    ;

attribute_definition
    : attribute_name type_spec?
    ;

alter_collection_clauses
    : MODIFY (LIMIT expression | ELEMENT TYPE type_spec)
    ;

dependent_handling_clause
    : INVALIDATE
    | CASCADE (CONVERT TO SUBSTITUTABLE | NOT? INCLUDING TABLE DATA)? dependent_exceptions_part?
    ;

dependent_exceptions_part
    : FORCE? EXCEPTIONS INTO tableview_name
    ;

create_type
    : CREATE (OR REPLACE)? TYPE (type_definition | type_body) ';'
    ;

// Create Type Specific Clauses

type_definition
    : type_name (OID CHAR_STRING)? FORCE? object_type_def?
    ;

object_type_def
    : invoker_rights_clause? (object_as_part | object_under_part) sqlj_object_type?
      ('(' object_member_spec (',' object_member_spec)* ')')? modifier_clause*
    ;

object_as_part
    : (IS | AS) (OBJECT | varray_type_def | nested_table_type_def)
    ;

object_under_part
    : UNDER type_spec
    ;

nested_table_type_def
    : TABLE OF type_spec (NOT NULL_)?
    ;

sqlj_object_type
    : EXTERNAL NAME expression LANGUAGE JAVA USING (SQLDATA | CUSTOMDATUM | ORADATA)
    ;

type_body
    : BODY type_name (IS | AS) (type_body_elements)+ END
    ;

type_body_elements
    : map_order_func_declaration
    | subprog_decl_in_type
    | overriding_subprogram_spec
    ;

map_order_func_declaration
    : (MAP | ORDER) MEMBER func_decl_in_type
    ;

subprog_decl_in_type
    : (MEMBER | STATIC) (proc_decl_in_type | func_decl_in_type | constructor_declaration)
    ;

proc_decl_in_type
    : PROCEDURE procedure_name '(' type_elements_parameter (',' type_elements_parameter)* ')'
      (IS | AS) (call_spec | DECLARE? seq_of_declare_specs? body ';')
    ;

func_decl_in_type
    : FUNCTION function_name ('(' type_elements_parameter (',' type_elements_parameter)* ')')?
      RETURN type_spec (IS | AS) (call_spec | DECLARE? seq_of_declare_specs? body ';')
    ;

constructor_declaration
    : FINAL? INSTANTIABLE? CONSTRUCTOR FUNCTION type_spec
      ('(' (SELF IN OUT type_spec ',') type_elements_parameter (',' type_elements_parameter)*  ')')?
      RETURN SELF AS RESULT (IS | AS) (call_spec | DECLARE? seq_of_declare_specs? body ';')
    ;

// Common Type Clauses

modifier_clause
    : NOT? (INSTANTIABLE | FINAL | OVERRIDING)
    ;

object_member_spec
    : identifier type_spec sqlj_object_type_attr?
    | element_spec
    ;

sqlj_object_type_attr
    : EXTERNAL NAME expression
    ;

element_spec
    : modifier_clause? element_spec_options+ (',' pragma_clause)?
    ;

element_spec_options
    : subprogram_spec
    | constructor_spec
    | map_order_function_spec
    ;

subprogram_spec
    : (MEMBER | STATIC) (type_procedure_spec | type_function_spec)
    ;

// TODO: should be refactored such as Procedure body and Function body, maybe Type_Function_Body and overriding_function_body
overriding_subprogram_spec
    : OVERRIDING MEMBER overriding_function_spec
    ;

overriding_function_spec
    : FUNCTION function_name ('(' type_elements_parameter (',' type_elements_parameter)* ')')?
      RETURN (type_spec | SELF AS RESULT)
     (PIPELINED? (IS | AS) (DECLARE? seq_of_declare_specs? body))? ';'?
    ;

type_procedure_spec
    : PROCEDURE procedure_name '(' type_elements_parameter (',' type_elements_parameter)* ')' ((IS | AS) call_spec)?
    ;

type_function_spec
    : FUNCTION function_name ('(' type_elements_parameter (',' type_elements_parameter)* ')')?
      RETURN (type_spec | SELF AS RESULT) ((IS | AS) call_spec | EXTERNAL VARIABLE? NAME expression)?
    ;

constructor_spec
    : FINAL? INSTANTIABLE? CONSTRUCTOR FUNCTION
      type_spec ('(' (SELF IN OUT type_spec ',') type_elements_parameter (',' type_elements_parameter)*  ')')?
      RETURN SELF AS RESULT ((IS | AS) call_spec)?
    ;

map_order_function_spec
    : (MAP | ORDER) MEMBER type_function_spec
    ;

pragma_clause
    : PRAGMA RESTRICT_REFERENCES '(' pragma_elements (',' pragma_elements)* ')'
    ;

pragma_elements
    : identifier
    | DEFAULT
    ;

type_elements_parameter
    : parameter_name type_spec
    ;

// Sequence DDLs

drop_sequence
    : DROP SEQUENCE sequence_name ';'
    ;

alter_sequence
    : ALTER SEQUENCE sequence_name sequence_spec+ ';'
    ;

alter_session
    : ALTER SESSION (
        ADVISE ( COMMIT | ROLLBACK | NOTHING )
        | CLOSE DATABASE LINK parameter_name
        | enable_or_disable COMMIT IN PROCEDURE
        | enable_or_disable GUARD
        | (enable_or_disable | FORCE) PARALLEL (DML | DDL | QUERY) (PARALLEL (literal | parameter_name))?
        | SET alter_session_set_clause
    )
    ;

alter_session_set_clause
    : parameter_name '=' parameter_value
    ;

create_sequence
    : CREATE SEQUENCE sequence_name (sequence_start_clause | sequence_spec)* ';'
    ;

// Common Sequence

sequence_spec
    : INCREMENT BY UNSIGNED_INTEGER
    | MAXVALUE UNSIGNED_INTEGER
    | NOMAXVALUE
    | MINVALUE UNSIGNED_INTEGER
    | NOMINVALUE
    | CYCLE
    | NOCYCLE
    | CACHE UNSIGNED_INTEGER
    | NOCACHE
    | ORDER
    | NOORDER
    ;

sequence_start_clause
    : START WITH UNSIGNED_INTEGER
    ;

create_index
    : CREATE (UNIQUE | BITMAP)? INDEX index_name
       ON (cluster_index_clause | table_index_clause | bitmap_join_index_clause)
       UNUSABLE?
       ';'
    ;

cluster_index_clause
    : CLUSTER cluster_name index_attributes?
    ;

cluster_name
    : (id_expression '.')? id_expression
    ;

table_index_clause
    : tableview_name table_alias? '(' index_expr (ASC | DESC)?  (',' index_expr (ASC | DESC)? )* ')'
          index_properties?
    ;
bitmap_join_index_clause
    : tableview_name '(' (tableview_name | table_alias)? column_name (ASC | DESC)?  (',' (tableview_name | table_alias)? column_name (ASC | DESC)? )* ')'
        FROM tableview_name table_alias (',' tableview_name table_alias)*
        where_clause local_partitioned_index? index_attributes?
    ;

index_expr
    : column_name
    | expression
    ;

index_properties
    : (global_partitioned_index | local_partitioned_index | index_attributes)+
    | INDEXTYPE IS (domain_index_clause | xmlindex_clause)
    ;

domain_index_clause
    : indextype local_domain_index_clause? parallel_clause? (PARAMETERS '(' odci_parameters ')' )?
    ;

local_domain_index_clause
    : LOCAL ('(' PARTITION partition_name (PARAMETERS '(' odci_parameters ')' )?  (',' PARTITION partition_name (PARAMETERS '(' odci_parameters ')' )? )* ')' )?
    ;

xmlindex_clause
    : (XDB '.')? XMLINDEX local_xmlindex_clause?
        parallel_clause? //TODO xmlindex_parameters_clause?
    ;

local_xmlindex_clause
    : LOCAL ('(' PARTITION partition_name (',' PARTITION partition_name //TODO xmlindex_parameters_clause?
                                                       )* ')')?
    ;

global_partitioned_index
    : GLOBAL PARTITION BY (RANGE '(' column_name (',' column_name)* ')' '(' index_partitioning_clause ')'
                          | HASH '(' column_name (',' column_name)* ')'
                                            (individual_hash_partitions
                                            | hash_partitions_by_quantity
                                            )
                          )
    ;

index_partitioning_clause
    : PARTITION partition_name? VALUES LESS THAN '(' literal (',' literal)* ')'
        segment_attributes_clause?
    ;

local_partitioned_index
    : LOCAL (on_range_partitioned_table
            | on_list_partitioned_table
            | on_hash_partitioned_table
            | on_comp_partitioned_table
            )?
    ;

on_range_partitioned_table
    : '(' partitioned_table (',' partitioned_table)* ')'
    ;

on_list_partitioned_table
    : '(' partitioned_table (',' partitioned_table)* ')'
    ;

partitioned_table
    :  PARTITION partition_name?
        (segment_attributes_clause | key_compression)*
        UNUSABLE?
    ;

on_hash_partitioned_table
    : STORE IN '(' tablespace (',' tablespace)* ')'
    | '(' on_hash_partitioned_clause (',' on_hash_partitioned_clause)* ')'
    ;

on_hash_partitioned_clause
    : PARTITION partition_name? (TABLESPACE tablespace)?
        key_compression? UNUSABLE?
    ;
on_comp_partitioned_table
    : (STORE IN '(' tablespace (',' tablespace)* ')' )?
        '(' on_comp_partitioned_clause (',' on_comp_partitioned_clause)* ')'
    ;

on_comp_partitioned_clause
    : PARTITION partition_name?
        (segment_attributes_clause | key_compression)*
        UNUSABLE index_subpartition_clause?
    ;

index_subpartition_clause
    : STORE IN '(' tablespace (',' tablespace)* ')'
    | '(' index_subpartition_subclause (',' index_subpartition_subclause)* ')'
    ;

index_subpartition_subclause
    : SUBPARTITION subpartition_name? (TABLESPACE tablespace)?
        key_compression? UNUSABLE?
    ;

odci_parameters
    : CHAR_STRING
    ;

indextype
    : (id_expression '.')? id_expression
    ;

//https://docs.oracle.com/cd/E11882_01/server.112/e41084/statements_1010.htm#SQLRF00805
alter_index
    : ALTER INDEX index_name (alter_index_ops_set1 | alter_index_ops_set2) ';'
    ;

alter_index_ops_set1
    : ( deallocate_unused_clause
      | allocate_extent_clause
      | shrink_clause
      | parallel_clause
      | physical_attributes_clause
      | logging_clause
      )+
    ;

alter_index_ops_set2
    : rebuild_clause
    | PARAMETERS '(' odci_parameters ')'
    | COMPILE
    | enable_or_disable
    | UNUSABLE
    | visible_or_invisible
    | RENAME TO new_index_name
    | COALESCE
    | monitoring_nomonitoring USAGE
    | UPDATE BLOCK REFERENCES
    | alter_index_partitioning
    ;

visible_or_invisible
    : VISIBLE
    | INVISIBLE
    ;

monitoring_nomonitoring
    : MONITORING
    | NOMONITORING
    ;

rebuild_clause
    : REBUILD ( PARTITION partition_name
              | SUBPARTITION subpartition_name
              | REVERSE
              | NOREVERSE
              )?
              ( parallel_clause
              | TABLESPACE tablespace
              | PARAMETERS '(' odci_parameters ')'
//TODO        | xmlindex_parameters_clause
              | ONLINE
              | physical_attributes_clause
              | key_compression
              | logging_clause
              )*
    ;

alter_index_partitioning
    : modify_index_default_attrs
    | add_hash_index_partition
    | modify_index_partition
    | rename_index_partition
    | drop_index_partition
    | split_index_partition
    | coalesce_index_partition
    | modify_index_subpartition
    ;

modify_index_default_attrs
    : MODIFY DEFAULT ATTRIBUTES (FOR PARTITION partition_name)?
         ( physical_attributes_clause
         | TABLESPACE (tablespace | DEFAULT)
         | logging_clause
         )
    ;

add_hash_index_partition
    : ADD PARTITION partition_name? (TABLESPACE tablespace)?
        key_compression? parallel_clause?
    ;

coalesce_index_partition
    : COALESCE PARTITION parallel_clause?
    ;

modify_index_partition
    : MODIFY PARTITION partition_name
        ( modify_index_partitions_ops+
        | PARAMETERS '(' odci_parameters ')'
        | COALESCE
        | UPDATE BLOCK REFERENCES
        | UNUSABLE
        )
    ;

modify_index_partitions_ops
    : deallocate_unused_clause
    | allocate_extent_clause
    | physical_attributes_clause
    | logging_clause
    | key_compression
    ;

rename_index_partition
    : RENAME (PARTITION partition_name | SUBPARTITION subpartition_name)
         TO new_partition_name
    ;

drop_index_partition
    : DROP PARTITION partition_name
    ;

split_index_partition
    : SPLIT PARTITION partition_name_old AT '(' literal (',' literal)* ')'
        (INTO '(' index_partition_description ',' index_partition_description ')' ) ? parallel_clause?
    ;

index_partition_description
    : PARTITION (partition_name ( (segment_attributes_clause | key_compression)+
                                | PARAMETERS '(' odci_parameters ')'
                                )
                                UNUSABLE?
                )?
    ;

modify_index_subpartition
    : MODIFY SUBPARTITION subpartition_name (UNUSABLE
                                            | allocate_extent_clause
                                            | deallocate_unused_clause
                                            )
    ;

partition_name_old
    : partition_name
    ;

new_partition_name
    : partition_name
    ;

new_index_name
    : index_name
    ;

create_user
    : CREATE USER
      user_object_name
        ( identified_by
          | identified_other_clause
          | user_tablespace_clause
          | quota_clause
          | profile_clause
          | password_expire_clause
          | user_lock_clause
          | user_editions_clause
          | container_clause
        )+ ';'
    ;

// The standard clauses only permit one user per statement.
// The proxy clause allows multiple users for a proxy designation.
alter_user
    : ALTER USER
      user_object_name
        ( alter_identified_by
        | identified_other_clause
        | user_tablespace_clause
        | quota_clause
        | profile_clause
        | user_default_role_clause
        | password_expire_clause
        | user_lock_clause
        | alter_user_editions_clause
        | container_clause
        | container_data_clause
        )+
      ';'
      | user_object_name (',' user_object_name)* proxy_clause ';'
    ;

alter_identified_by
    : identified_by (REPLACE id_expression)?
    ;

identified_by
    : IDENTIFIED BY id_expression
    ;

identified_other_clause
    : IDENTIFIED (EXTERNALLY | GLOBALLY) (AS quoted_string)?
    ;

user_tablespace_clause
    : (DEFAULT | TEMPORARY) TABLESPACE id_expression
    ;

quota_clause
    : QUOTA (size_clause | UNLIMITED) ON id_expression
    ;

profile_clause
    : PROFILE id_expression
    ;

role_clause
    : role_name (',' role_name)*
    | ALL (EXCEPT role_name (',' role_name)*)*
    ;

user_default_role_clause
    : DEFAULT ROLE (NONE | role_clause)
    ;

password_expire_clause
    : PASSWORD EXPIRE
    ;

user_lock_clause
    : ACCOUNT (LOCK | UNLOCK)
    ;

user_editions_clause
    : ENABLE EDITIONS
    ;

alter_user_editions_clause
    : user_editions_clause (FOR regular_id (',' regular_id)*)? FORCE?
    ;

proxy_clause
    : REVOKE CONNECT THROUGH (ENTERPRISE USERS | user_object_name)
    | GRANT CONNECT THROUGH
        ( ENTERPRISE USERS
        | user_object_name
            (WITH (NO ROLES | ROLE role_clause))?
            (AUTHENTICATION REQUIRED)?
            (AUTHENTICATED USING (PASSWORD | CERTIFICATE | DISTINGUISHED NAME))?
        )
    ;

container_names
    : LEFT_PAREN id_expression (',' id_expression)* RIGHT_PAREN
    ;

set_container_data
    : SET CONTAINER_DATA EQUALS_OP (ALL | DEFAULT | container_names)
    ;

add_rem_container_data
    : (ADD | REMOVE) CONTAINER_DATA EQUALS_OP container_names
    ;

container_data_clause
    : set_container_data
    | add_rem_container_data (FOR container_tableview_name)?
    ;

// https://docs.oracle.com/cd/E11882_01/server.112/e41084/statements_4005.htm#SQLRF01105
analyze
    : ( ANALYZE (TABLE tableview_name | INDEX index_name) partition_extention_clause?
      | ANALYZE CLUSTER cluster_name
      )

      ( validation_clauses
      | LIST CHAINED ROWS into_clause1?
      | DELETE SYSTEM? STATISTICS
      )
      ';'
    ;

partition_extention_clause
    : PARTITION ( '(' partition_name ')'
                | FOR '(' partition_key_value (',' partition_key_value)* ')'
                )
    | SUBPARTITION ( '(' subpartition_name ')'
                   | FOR '(' subpartition_key_value (',' subpartition_key_value)* ')'
                   )
    ;

validation_clauses
    : VALIDATE REF UPDATE (SET DANGLING TO NULL_)?
    | VALIDATE STRUCTURE
        ( CASCADE FAST
        | CASCADE online_or_offline? into_clause?
        | CASCADE
        )?
        online_or_offline? into_clause?
    ;

online_or_offline
    : OFFLINE
    | ONLINE
    ;

into_clause1
    : INTO tableview_name?
    ;

//Making assumption on partition ad subpartition key value clauses
partition_key_value
    : literal
    ;

subpartition_key_value
    : literal
    ;

//https://docs.oracle.com/cd/E11882_01/server.112/e41084/statements_4006.htm#SQLRF01106
associate_statistics
    : ASSOCIATE STATISTICS
        WITH (column_association | function_association)
        storage_table_clause?
      ';'
    ;

column_association
    : COLUMNS tableview_name '.' column_name (',' tableview_name '.' column_name)* using_statistics_type
    ;

function_association
    : ( FUNCTIONS function_name (',' function_name)*
      | PACKAGES package_name (',' package_name)*
      | TYPES type_name (',' type_name)*
      | INDEXES index_name (',' index_name)*
      | INDEXTYPES indextype_name (',' indextype_name)*
      )

      ( using_statistics_type
      | default_cost_clause (',' default_selectivity_clause)?
      | default_selectivity_clause (',' default_cost_clause)?
      )
    ;

indextype_name
    : id_expression
    ;


using_statistics_type
    : USING (statistics_type_name | NULL_)
    ;

statistics_type_name
    : regular_id
    ;

default_cost_clause
    : DEFAULT COST '(' cpu_cost ',' io_cost ',' network_cost ')'
    ;

cpu_cost
    : UNSIGNED_INTEGER
    ;

io_cost
    : UNSIGNED_INTEGER
    ;

network_cost
    : UNSIGNED_INTEGER
    ;

default_selectivity_clause
    : DEFAULT SELECTIVITY default_selectivity
    ;

default_selectivity
    : UNSIGNED_INTEGER
    ;

storage_table_clause
    : WITH (SYSTEM | USER) MANAGED STORAGE TABLES
    ;

// https://docs.oracle.com/database/121/SQLRF/statements_4008.htm#SQLRF56110
unified_auditing
    : {isVersion12()}?
      AUDIT (POLICY policy_name ((BY | EXCEPT) audit_user (',' audit_user)* )?
                                (WHENEVER NOT? SUCCESSFUL)?
            | CONTEXT NAMESPACE oracle_namespace
                      ATTRIBUTES attribute_name (',' attribute_name)* (BY audit_user (',' audit_user)*)?
            )
      ';'
    ;

policy_name
    : identifier
    ;

// https://docs.oracle.com/cd/E11882_01/server.112/e41084/statements_4007.htm#SQLRF01107
// https://docs.oracle.com/database/121/SQLRF/statements_4007.htm#SQLRF01107

audit_traditional
    : AUDIT ( audit_operation_clause (auditing_by_clause | IN SESSION CURRENT)?
            | audit_schema_object_clause
            | NETWORK
            | audit_direct_path
            )
        (BY (SESSION | ACCESS) )? (WHENEVER NOT? SUCCESSFUL)?
        audit_container_clause?
      ';'
    ;

audit_direct_path
    : {isVersion12()}? DIRECT_PATH auditing_by_clause
    ;

audit_container_clause
    : {isVersion12()}? (CONTAINER EQUALS_OP (CURRENT | ALL))
    ;

audit_operation_clause
    : ( (sql_statement_shortcut | ALL STATEMENTS?)  (',' (sql_statement_shortcut | ALL STATEMENTS?) )*
      | (system_privilege | ALL PRIVILEGES)  (',' (system_privilege | ALL PRIVILEGES) )*
      )
    ;

auditing_by_clause
    : BY audit_user (',' audit_user)*
    ;

audit_user
    : regular_id
    ;

audit_schema_object_clause
    : ( sql_operation (',' sql_operation)* | ALL) auditing_on_clause
    ;

sql_operation
    : ALTER
    | AUDIT
    | COMMENT
    | DELETE
    | EXECUTE
    | FLASHBACK
    | GRANT
    | INDEX
    | INSERT
    | LOCK
    | READ
    | RENAME
    | SELECT
    | UPDATE
    ;

auditing_on_clause
    : ON ( object_name
         | DIRECTORY regular_id
         | MINING MODEL model_name
         | {isVersion12()}? SQL TRANSLATION PROFILE profile_name
         | DEFAULT
         )
    ;

model_name
    : (id_expression '.')? id_expression
    ;

object_name
    : (id_expression '.')? id_expression
    ;

profile_name
    : (id_expression '.')? id_expression
    ;

sql_statement_shortcut
    : ALTER SYSTEM
    | CLUSTER
    | CONTEXT
    | DATABASE LINK
    | DIMENSION
    | DIRECTORY
    | INDEX
    | MATERIALIZED VIEW
    | NOT EXISTS
    | OUTLINE
    | {isVersion12()}? PLUGGABLE DATABASE
    | PROCEDURE
    | PROFILE
    | PUBLIC DATABASE LINK
    | PUBLIC SYNONYM
    | ROLE
    | ROLLBACK SEGMENT
    | SEQUENCE
    | SESSION
    | SYNONYM
    | SYSTEM AUDIT
    | SYSTEM GRANT
    | TABLE
    | TABLESPACE
    | TRIGGER
    | TYPE
    | USER
    | VIEW
    | ALTER SEQUENCE
    | ALTER TABLE
    | COMMENT TABLE
    | DELETE TABLE
    | EXECUTE PROCEDURE
    | GRANT DIRECTORY
    | GRANT PROCEDURE
    | GRANT SEQUENCE
    | GRANT TABLE
    | GRANT TYPE
    | INSERT TABLE
    | LOCK TABLE
    | SELECT SEQUENCE
    | SELECT TABLE
    | UPDATE TABLE
    ;

drop_index
    : DROP INDEX index_name ';'
    ;

rename_object
    : RENAME object_name TO object_name ';'
    ;

grant_statement
    : GRANT
        ( ','?
          (role_name
          | system_privilege
          | object_privilege paren_column_list?
          )
        )+
      (ON grant_object_name)?
      TO (grantee_name | PUBLIC) (',' (grantee_name | PUBLIC) )*
      (WITH (ADMIN | DELEGATE) OPTION)?
      (WITH HIERARCHY OPTION)?
      (WITH GRANT OPTION)?
      container_clause? ';'
    ;

container_clause
    : CONTAINER EQUALS_OP (CURRENT | ALL)
    ;

create_directory
    : CREATE (OR REPLACE)? DIRECTORY directory_name AS directory_path
      ';'
    ;

directory_name
    : regular_id
    ;

directory_path
    : CHAR_STRING
    ;

// https://docs.oracle.com/cd/E11882_01/appdev.112/e25519/alter_library.htm#LNPLS99946
// https://docs.oracle.com/database/121/LNPLS/alter_library.htm#LNPLS99946
alter_library
    : ALTER LIBRARY library_name
       ( COMPILE library_debug? compiler_parameters_clause* (REUSE SETTINGS)?
       | library_editionable
       )
     ';'
    ;

library_editionable
    : {isVersion12()}? (EDITIONABLE | NONEDITIONABLE)
    ;

library_debug
    : {isVersion12()}? DEBUG
    ;


compiler_parameters_clause
    : parameter_name EQUALS_OP parameter_value
    ;

parameter_value
    : regular_id
    ;

library_name
    : (regular_id '.')? regular_id
    ;

// https://docs.oracle.com/cd/E11882_01/server.112/e41084/statements_4004.htm#SQLRF01104
// https://docs.oracle.com/database/121/SQLRF/statements_4004.htm#SQLRF01104
alter_view
    : ALTER VIEW tableview_name
       ( ADD out_of_line_constraint
       | MODIFY CONSTRAINT constraint_name (RELY | NORELY)
       | DROP ( CONSTRAINT constraint_name
              | PRIMARY KEY
              | UNIQUE '(' column_name (',' column_name)* ')'
              )
       | COMPILE
       | READ (ONLY | WRITE)
       | alter_view_editionable?
       )
      ';'
    ;

alter_view_editionable
    : {isVersion12()}? (EDITIONABLE | NONEDITIONABLE)
    ;

create_view
    : CREATE (OR REPLACE)? (OR? FORCE)? EDITIONABLE? EDITIONING? VIEW
      tableview_name view_options?
      AS select_only_statement subquery_restriction_clause?
    ;

view_options
    :  view_alias_constraint
    | object_view_clause
//  | xmltype_view_clause //TODO
    ;

view_alias_constraint
    : '(' ( ','? (table_alias inline_constraint* | out_of_line_constraint) )+ ')'
    ;

object_view_clause
    : OF type_name
       ( WITH OBJECT (IDENTIFIER|ID|OID) ( DEFAULT | '(' REGULAR_ID (',' REGULAR_ID)* ')' )
       | UNDER tableview_name
       )
       ( '(' ( ','? (out_of_line_constraint | REGULAR_ID inline_constraint ) )+ ')' )*
    ;

inline_constraint
    : (CONSTRAINT constraint_name)?
        ( NOT? NULL_
        | UNIQUE
        | PRIMARY KEY
        | references_clause
        | check_constraint
        )
      constraint_state?
    ;

inline_ref_constraint
    : SCOPE IS tableview_name
    | WITH ROWID
    | (CONSTRAINT constraint_name)? references_clause constraint_state?
    ;

out_of_line_ref_constraint
    : SCOPE FOR '(' ref_col_or_attr=regular_id ')' IS tableview_name
    | REF '(' ref_col_or_attr=regular_id ')' WITH ROWID
    | (CONSTRAINT constraint_name)? FOREIGN KEY '(' ( ','? ref_col_or_attr=regular_id)+ ')' references_clause constraint_state?
    ;

out_of_line_constraint
    : ( (CONSTRAINT constraint_name)?
          ( UNIQUE '(' column_name (',' column_name)* ')'
          | PRIMARY KEY '(' column_name (',' column_name)* ')'
          | foreign_key_clause
          | CHECK '(' expression ')'
          )
       )
      constraint_state?
    ;

constraint_state
    : ( NOT? DEFERRABLE
      | INITIALLY (IMMEDIATE|DEFERRED)
      | (RELY|NORELY)
      | (ENABLE|DISABLE)
      | (VALIDATE|NOVALIDATE)
      | using_index_clause
      )+
    ;

alter_tablespace
    : ALTER TABLESPACE tablespace
       ( DEFAULT table_compression? storage_clause?
       | MINIMUM EXTENT size_clause
       | RESIZE size_clause
       | COALESCE
       | SHRINK SPACE_KEYWORD (KEEP size_clause)?
       | RENAME TO new_tablespace_name
       | begin_or_end BACKUP
       | datafile_tempfile_clauses
       | tablespace_logging_clauses
       | tablespace_group_clause
       | tablespace_state_clauses
       | autoextend_clause
       | flashback_mode_clause
       | tablespace_retention_clause
       )
     ';'
    ;

datafile_tempfile_clauses
    : ADD (datafile_specification | tempfile_specification)
    | DROP (DATAFILE | TEMPFILE) (filename | UNSIGNED_INTEGER) (KEEP size_clause)?
    | SHRINK TEMPFILE (filename | UNSIGNED_INTEGER) (KEEP size_clause)?
    | RENAME DATAFILE filename (',' filename)* TO filename (',' filename)*
    | (DATAFILE | TEMPFILE) (online_or_offline)
    ;

tablespace_logging_clauses
    : logging_clause
    | NO? FORCE LOGGING
    ;

tablespace_group_clause
    : TABLESPACE GROUP (tablespace_group_name | CHAR_STRING)
    ;

tablespace_group_name
    : regular_id
    ;

tablespace_state_clauses
    : ONLINE
    | OFFLINE (NORMAL | TEMPORARY | IMMEDIATE)?
    | READ (ONLY | WRITE)
    | PERMANENT
    | TEMPORARY
    ;

flashback_mode_clause
    : FLASHBACK (ON | OFF)
    ;

new_tablespace_name
    : tablespace
    ;

create_tablespace
    : CREATE (BIGFILE | SMALLFILE)?
        ( permanent_tablespace_clause
        | temporary_tablespace_clause
        | undo_tablespace_clause
        )
      ';'
    ;

permanent_tablespace_clause
    : TABLESPACE id_expression datafile_specification?
        ( MINIMUM EXTENT size_clause
        | BLOCKSIZE size_clause
        | logging_clause
        | FORCE LOGGING
        | (ONLINE | OFFLINE)
        | ENCRYPTION tablespace_encryption_spec
        | DEFAULT //TODO table_compression? storage_clause?
        | extent_management_clause
        | segment_management_clause
        | flashback_mode_clause
        )*
    ;

tablespace_encryption_spec
    : USING encrypt_algorithm=CHAR_STRING
    ;

logging_clause
    : LOGGING
     | NOLOGGING
     | FILESYSTEM_LIKE_LOGGING
    ;

extent_management_clause
    : EXTENT MANAGEMENT LOCAL
        ( AUTOALLOCATE
        | UNIFORM (SIZE size_clause)?
        )?
    ;

segment_management_clause
    : SEGMENT SPACE_KEYWORD MANAGEMENT (AUTO | MANUAL)
    ;

temporary_tablespace_clause
    : TEMPORARY TABLESPACE tablespace_name=id_expression
        tempfile_specification?
        tablespace_group_clause? extent_management_clause?
    ;

undo_tablespace_clause
    : UNDO TABLESPACE tablespace_name=id_expression
        datafile_specification?
        extent_management_clause? tablespace_retention_clause?
    ;

tablespace_retention_clause
    : RETENTION (GUARANTEE | NOGUARANTEE)
    ;

// asm_filename is just a charater string.  Would need to parse the string
// to find diskgroup...
datafile_specification
    : DATAFILE
	  (','? datafile_tempfile_spec)
    ;

tempfile_specification
    : TEMPFILE
	  (','? datafile_tempfile_spec)
    ;

datafile_tempfile_spec
    : (CHAR_STRING)? (SIZE size_clause)? REUSE? autoextend_clause?
    ;


redo_log_file_spec
    : (DATAFILE CHAR_STRING
      | '(' ( ','? CHAR_STRING )+ ')'
      )?
        (SIZE size_clause)?
        (BLOCKSIZE size_clause)?
        REUSE?
    ;

autoextend_clause
    : AUTOEXTEND (OFF | ON (NEXT size_clause)? maxsize_clause? )
    ;

maxsize_clause
    : MAXSIZE (UNLIMITED | size_clause)
    ;

build_clause
    : BUILD (IMMEDIATE | DEFERRED)
    ;

parallel_clause
    : NOPARALLEL
    | PARALLEL parallel_count=UNSIGNED_INTEGER?
    ;

alter_materialized_view
    : ALTER MATERIALIZED VIEW tableview_name
       ( physical_attributes_clause
       | modify_mv_column_clause
       | table_compression
       | lob_storage_clause (',' lob_storage_clause)*
       | modify_lob_storage_clause (',' modify_lob_storage_clause)*
//TODO | alter_table_partitioning
       | parallel_clause
       | logging_clause
       | allocate_extent_clause
       | deallocate_unused_clause
       | shrink_clause
       | (cache_or_nocache)
       )?
       alter_iot_clauses?
       (USING INDEX physical_attributes_clause)?
       alter_mv_option1?
       ( enable_or_disable QUERY REWRITE
       | COMPILE
       | CONSIDER FRESH
       )?
     ';'
    ;

alter_mv_option1
    : alter_mv_refresh
//TODO  | MODIFY scoped_table_ref_constraint
    ;

alter_mv_refresh
    : REFRESH ( FAST
              | COMPLETE
              | FORCE
              | ON (DEMAND | COMMIT)
              | START WITH expression
              | NEXT expression
              | WITH PRIMARY KEY
              | USING DEFAULT? MASTER ROLLBACK SEGMENT rollback_segment?
              | USING (ENFORCED | TRUSTED) CONSTRAINTS
              )+
    ;

rollback_segment
    : regular_id
    ;

modify_mv_column_clause
    : MODIFY '(' column_name (ENCRYPT encryption_spec | DECRYPT)? ')'
    ;

alter_materialized_view_log
    : ALTER MATERIALIZED VIEW LOG FORCE? ON tableview_name
       ( physical_attributes_clause
       | add_mv_log_column_clause
//TODO | alter_table_partitioning
       | parallel_clause
       | logging_clause
       | allocate_extent_clause
       | shrink_clause
       | move_mv_log_clause
       | cache_or_nocache
       )?
       mv_log_augmentation? mv_log_purge_clause?
      ';'
    ;
add_mv_log_column_clause
    : ADD '(' column_name ')'
    ;

move_mv_log_clause
    : MOVE segment_attributes_clause parallel_clause?
    ;

mv_log_augmentation
    : ADD ( ( OBJECT ID
            | PRIMARY KEY
            | ROWID
            | SEQUENCE
            )
            ('(' column_name (',' column_name)* ')')?

          | '(' column_name (',' column_name)* ')'
          )
          new_values_clause?
    ;

// Should bound this to just date/time expr
datetime_expr
    : expression
    ;

// Should bound this to just interval expr
interval_expr
    : expression
    ;

synchronous_or_asynchronous
    : SYNCHRONOUS
    | ASYNCHRONOUS
    ;

including_or_excluding
    : INCLUDING
    | EXCLUDING
    ;


create_materialized_view_log
    : CREATE MATERIALIZED VIEW LOG ON tableview_name
        ( ( physical_attributes_clause
          | TABLESPACE tablespace_name=id_expression
          | logging_clause
          | (CACHE | NOCACHE)
          )+
         )?
        parallel_clause?
        // table_partitioning_clauses TODO
        ( WITH
           ( ','?
             ( OBJECT ID
             | PRIMARY KEY
             | ROWID
             | SEQUENCE
             | COMMIT SCN
             )
           )*
           ('(' ( ','? regular_id )+ ')' new_values_clause? )?
           mv_log_purge_clause?
        )*
    ;

new_values_clause
    : (INCLUDING | EXCLUDING ) NEW VALUES
    ;

mv_log_purge_clause
    : PURGE
         ( IMMEDIATE (SYNCHRONOUS | ASYNCHRONOUS)?
      // |START WITH CLAUSES TODO
         )
    ;

create_materialized_view
    : CREATE MATERIALIZED VIEW tableview_name
      (OF type_name )?
//scoped_table_ref and column alias goes here  TODO
        ( ON PREBUILT TABLE ( (WITH | WITHOUT) REDUCED PRECISION)?
        | physical_properties?  (CACHE | NOCACHE)? parallel_clause? build_clause?
        )
        ( USING INDEX ( (physical_attributes_clause | TABLESPACE mv_tablespace=id_expression)+ )*
        | USING NO INDEX
        )?
        create_mv_refresh?
        (FOR UPDATE)?
        ( (DISABLE | ENABLE) QUERY REWRITE )?
        AS select_only_statement
        ';'
    ;

create_mv_refresh
    : ( NEVER REFRESH
      | REFRESH
         ( (FAST | COMPLETE | FORCE)
         | ON (DEMAND | COMMIT)
         | (START WITH | NEXT) //date goes here TODO
         | WITH (PRIMARY KEY | ROWID)
         | USING
             ( DEFAULT (MASTER | LOCAL)? ROLLBACK SEGMENT
             | (MASTER | LOCAL)? ROLLBACK SEGMENT rb_segment=REGULAR_ID
             )
         | USING (ENFORCED | TRUSTED) CONSTRAINTS
         )+
      )
    ;

create_context
    : CREATE (OR REPLACE)? CONTEXT oracle_namespace USING (schema_object_name '.')? package_name
           (INITIALIZED (EXTERNALLY | GLOBALLY)
           | ACCESSED GLOBALLY
           )?
      ';'
    ;

oracle_namespace
    : id_expression
    ;

//https://docs.oracle.com/cd/E11882_01/server.112/e41084/statements_5001.htm#SQLRF01201
create_cluster
    : CREATE CLUSTER  cluster_name '(' column_name datatype SORT? (',' column_name datatype SORT?)* ')'
          ( physical_attributes_clause
          | SIZE size_clause
          | TABLESPACE tablespace
          | INDEX
          | (SINGLE TABLE)? HASHKEYS UNSIGNED_INTEGER (HASH IS expression)?
          )*
          parallel_clause? (ROWDEPENDENCIES | NOROWDEPENDENCIES)?
          (CACHE | NOCACHE)?
          ';'
    ;

create_table
    : CREATE (GLOBAL TEMPORARY)? TABLE tableview_name
        (relational_table | object_table | xmltype_table) (AS select_only_statement)?
      ';'
    ;

xmltype_table
    : OF XMLTYPE ('(' object_properties ')')?
         (XMLTYPE xmltype_storage)? xmlschema_spec? xmltype_virtual_columns?
         (ON COMMIT (DELETE | PRESERVE) ROWS)? oid_clause? oid_index_clause?
         physical_properties? column_properties? table_partitioning_clauses?
         (CACHE | NOCACHE)? (RESULT_CACHE '(' MODE (DEFAULT | FORCE) ')')?
         parallel_clause? (ROWDEPENDENCIES | NOROWDEPENDENCIES)?
	 (enable_disable_clause+)? row_movement_clause?
         flashback_archive_clause?
    ;

xmltype_virtual_columns
    : VIRTUAL COLUMNS '(' column_name AS '(' expression ')' (',' column_name AS '(' expression ')')* ')'
    ;

xmltype_column_properties
    : XMLTYPE COLUMN? column_name xmltype_storage? xmlschema_spec?
    ;

xmltype_storage
    : STORE  AS (OBJECT RELATIONAL
                | (SECUREFILE | BASICFILE)? (CLOB | BINARY XML) (lob_segname ('(' lob_parameters ')')? | '(' lob_parameters ')')?
                )
    | STORE VARRAYS AS (LOBS | TABLES)
    ;

xmlschema_spec
    : (XMLSCHEMA DELIMITED_ID)? ELEMENT DELIMITED_ID
         (allow_or_disallow NONSCHEMA)?
         (allow_or_disallow ANYSCHEMA)?
    ;

object_table
    : OF type_name object_table_substitution?
      ('(' object_properties (',' object_properties)* ')')?
      (ON COMMIT (DELETE | PRESERVE) ROWS)? oid_clause? oid_index_clause?
      physical_properties? column_properties? table_partitioning_clauses?
      (CACHE | NOCACHE)? (RESULT_CACHE '(' MODE (DEFAULT | FORCE) ')')?
      parallel_clause? (ROWDEPENDENCIES | NOROWDEPENDENCIES)?
      (enable_disable_clause+)? row_movement_clause? flashback_archive_clause?
    ;

oid_index_clause
    : OIDINDEX index_name? '(' (physical_attributes_clause | TABLESPACE tablespace)+ ')'
    ;

oid_clause
    : OBJECT IDENTIFIER IS (SYSTEM GENERATED | PRIMARY KEY)
    ;

object_properties
    : (column_name | attribute_name) (DEFAULT expression)? (inline_constraint (',' inline_constraint)* | inline_ref_constraint)?
    | out_of_line_constraint
    | out_of_line_ref_constraint
    | supplemental_logging_props
    ;

object_table_substitution
    : NOT? SUBSTITUTABLE AT ALL LEVELS
    ;

relational_table
    : ('(' relational_property (',' relational_property)* ')')?
      (ON COMMIT (DELETE | PRESERVE) ROWS)?
      physical_properties? column_properties? table_partitioning_clauses?
      (CACHE | NOCACHE)? (RESULT_CACHE '(' MODE (DEFAULT | FORCE) ')')?
      parallel_clause?
      (ROWDEPENDENCIES | NOROWDEPENDENCIES)?
      (enable_disable_clause+)? row_movement_clause? flashback_archive_clause?
    ;

relational_property
    : (column_definition
        | virtual_column_definition
        | out_of_line_constraint
        | out_of_line_ref_constraint
        | supplemental_logging_props
        )
    ;

table_partitioning_clauses
    : range_partitions
    | list_partitions
    | hash_partitions
    | composite_range_partitions
    | composite_list_partitions
    | composite_hash_partitions
    | reference_partitioning
    | system_partitioning
    ;

range_partitions
    : PARTITION BY RANGE '(' column_name (',' column_name)* ')'
        (INTERVAL '(' expression ')' (STORE IN '(' tablespace (',' tablespace)* ')' )? )?
          '(' PARTITION partition_name? range_values_clause table_partition_description (',' PARTITION partition_name? range_values_clause table_partition_description)* ')'
    ;

list_partitions
    : PARTITION BY LIST '(' column_name ')'
        '(' PARTITION partition_name? list_values_clause table_partition_description  (',' PARTITION partition_name? list_values_clause table_partition_description )* ')'
    ;

hash_partitions
    : PARTITION BY HASH '(' column_name (',' column_name)* ')'
        (individual_hash_partitions | hash_partitions_by_quantity)
    ;

individual_hash_partitions
    : '(' PARTITION partition_name? partitioning_storage_clause? (',' PARTITION partition_name? partitioning_storage_clause?)* ')'
    ;

hash_partitions_by_quantity
    : PARTITIONS hash_partition_quantity
       (STORE IN '(' tablespace (',' tablespace)* ')')?
         (table_compression | key_compression)?
         (OVERFLOW STORE IN '(' tablespace (',' tablespace)* ')' )?
    ;

hash_partition_quantity
    : UNSIGNED_INTEGER
    ;

composite_range_partitions
    : PARTITION BY RANGE '(' column_name (',' column_name)* ')'
       (INTERVAL '(' expression ')' (STORE IN '(' tablespace (',' tablespace)* ')' )? )?
       (subpartition_by_range | subpartition_by_list | subpartition_by_hash)
         '(' range_partition_desc (',' range_partition_desc)* ')'
    ;

composite_list_partitions
    : PARTITION BY LIST '(' column_name ')'
       (subpartition_by_range | subpartition_by_list | subpartition_by_hash)
        '(' list_partition_desc (',' list_partition_desc)* ')'
    ;

composite_hash_partitions
    : PARTITION BY HASH '(' (',' column_name)+ ')'
       (subpartition_by_range | subpartition_by_list | subpartition_by_hash)
         (individual_hash_partitions | hash_partitions_by_quantity)
    ;

reference_partitioning
    : PARTITION BY REFERENCE '(' regular_id ')'
             ('(' reference_partition_desc (',' reference_partition_desc)* ')')?
    ;

reference_partition_desc
    : PARTITION partition_name? table_partition_description
    ;

system_partitioning
    : PARTITION BY SYSTEM
       (PARTITIONS UNSIGNED_INTEGER | reference_partition_desc (',' reference_partition_desc)*)?
    ;

range_partition_desc
    : PARTITION partition_name? range_values_clause table_partition_description
        ( ( '(' ( range_subpartition_desc (',' range_subpartition_desc)*
                | list_subpartition_desc (',' list_subpartition_desc)*
                | individual_hash_subparts (',' individual_hash_subparts)*
                )
            ')'
          | hash_subparts_by_quantity
          )
        )?
    ;

list_partition_desc
    : PARTITION partition_name? list_values_clause table_partition_description
        ( ( '(' ( range_subpartition_desc (',' range_subpartition_desc)*
                | list_subpartition_desc (',' list_subpartition_desc)*
                | individual_hash_subparts (',' individual_hash_subparts)*
                )
            ')'
          | hash_subparts_by_quantity
          )
        )?
    ;

subpartition_template
    : SUBPARTITION TEMPLATE
        ( ( '(' ( range_subpartition_desc (',' range_subpartition_desc)*
                | list_subpartition_desc (',' list_subpartition_desc)*
                | individual_hash_subparts (',' individual_hash_subparts)*
                )
            ')'
          | hash_subpartition_quantity
          )
        )
    ;

hash_subpartition_quantity
    : UNSIGNED_INTEGER
    ;

subpartition_by_range
    : SUBPARTITION BY RANGE '(' column_name (',' column_name)* ')' subpartition_template?
    ;

subpartition_by_list
    : SUBPARTITION BY LIST '(' column_name ')' subpartition_template?
    ;

subpartition_by_hash
    : SUBPARTITION BY HASH '(' column_name (',' column_name)* ')'
       (SUBPARTITIONS UNSIGNED_INTEGER (STORE IN '(' tablespace (',' tablespace)* ')' )?
       | subpartition_template
       )?
    ;

subpartition_name
    : partition_name
    ;

range_subpartition_desc
    : SUBPARTITION subpartition_name? range_values_clause partitioning_storage_clause?
    ;

list_subpartition_desc
    : SUBPARTITION subpartition_name? list_values_clause partitioning_storage_clause?
    ;

individual_hash_subparts
    : SUBPARTITION subpartition_name? partitioning_storage_clause?
    ;

hash_subparts_by_quantity
    : SUBPARTITIONS UNSIGNED_INTEGER (STORE IN '(' tablespace (',' tablespace)* ')' )?
    ;

range_values_clause
    : VALUES LESS THAN '(' literal (',' literal)* ')'
    ;

list_values_clause
    : VALUES '(' (literal (',' literal)* | DEFAULT) ')'
    ;

table_partition_description
    : deferred_segment_creation? segment_attributes_clause?
        (table_compression | key_compression)?
        (OVERFLOW segment_attributes_clause? )?
        (lob_storage_clause | varray_col_properties | nested_table_col_properties)?
    ;

partitioning_storage_clause
    : ( TABLESPACE tablespace
      | OVERFLOW (TABLESPACE tablespace)?
      | table_compression
      | key_compression
      | lob_partitioning_storage
      | VARRAY varray_item STORE AS (BASICFILE | SECUREFILE)? LOB lob_segname
      )+
    ;

lob_partitioning_storage
    : LOB '(' lob_item ')'
       STORE AS (BASICFILE | SECUREFILE)?
               (lob_segname ('(' TABLESPACE tablespace ')' )?
               | '(' TABLESPACE tablespace ')'
               )
    ;

datatype_null_enable
   : column_name datatype
         SORT?  (DEFAULT expression)? (ENCRYPT ( USING  CHAR_STRING )? (IDENTIFIED BY REGULAR_ID)? CHAR_STRING? ( NO? SALT )? )?
         (NOT NULL_)? (ENABLE | DISABLE)?
   ;

//Technically, this should only allow 'K' | 'M' | 'G' | 'T' | 'P' | 'E'
// but having issues with examples/numbers01.sql line 11 "sysdate -1m"
size_clause
    : UNSIGNED_INTEGER REGULAR_ID?
    ;


table_compression
    : COMPRESS
        ( BASIC
        | FOR ( OLTP
              | (QUERY | ARCHIVE) (LOW | HIGH)?
              )
        )?
    | NOCOMPRESS
    ;

physical_attributes_clause
    : (PCTFREE pctfree=UNSIGNED_INTEGER
      | PCTUSED pctused=UNSIGNED_INTEGER
      | INITRANS inittrans=UNSIGNED_INTEGER
      | storage_clause
      )+
    ;

storage_clause
    : STORAGE '('
         (INITIAL initial_size=size_clause
         | NEXT next_size=size_clause
         | MINEXTENTS minextents=(UNSIGNED_INTEGER | UNLIMITED)
         | MAXEXTENTS minextents=(UNSIGNED_INTEGER | UNLIMITED)
         | PCTINCREASE pctincrease=UNSIGNED_INTEGER
         | FREELISTS freelists=UNSIGNED_INTEGER
         | FREELIST GROUPS freelist_groups=UNSIGNED_INTEGER
         | OPTIMAL (size_clause | NULL_ )
         | BUFFER_POOL (KEEP | RECYCLE | DEFAULT)
         | FLASH_CACHE (KEEP | NONE | DEFAULT)
         | ENCRYPT
         )+
       ')'
    ;

deferred_segment_creation
    : SEGMENT CREATION (IMMEDIATE | DEFERRED)
    ;

segment_attributes_clause
    : ( physical_attributes_clause
      | TABLESPACE tablespace_name=id_expression
      | logging_clause
      )+
    ;

physical_properties
    : deferred_segment_creation?  segment_attributes_clause table_compression?
    ;

row_movement_clause
    : (ENABLE | DISABLE)? ROW MOVEMENT
    ;

flashback_archive_clause
    : FLASHBACK ARCHIVE flashback_archive=REGULAR_ID
    | NO FLASHBACK ARCHIVE
    ;

log_grp
    : UNSIGNED_INTEGER
    ;

supplemental_table_logging
    : ADD SUPPLEMENTAL LOG  (supplemental_log_grp_clause | supplemental_id_key_clause)
       (',' SUPPLEMENTAL LOG  (supplemental_log_grp_clause | supplemental_id_key_clause) )*
    | DROP SUPPLEMENTAL LOG (supplemental_id_key_clause | GROUP log_grp)
        (',' SUPPLEMENTAL LOG (supplemental_id_key_clause | GROUP log_grp) )*
    ;

supplemental_log_grp_clause
    : GROUP log_grp '(' regular_id (NO LOG)? (',' regular_id (NO LOG)?)* ')' ALWAYS?
    ;

supplemental_id_key_clause
    : DATA '('( ','? ( ALL
                     | PRIMARY KEY
                     | UNIQUE
                     | FOREIGN KEY
                     )
              )+
           ')'
      COLUMNS
    ;

allocate_extent_clause
    : ALLOCATE EXTENT
       ( '(' ( SIZE size_clause
             | DATAFILE datafile=CHAR_STRING
             | INSTANCE inst_num=UNSIGNED_INTEGER
             )+
         ')'
       )?
    ;

deallocate_unused_clause
    : DEALLOCATE UNUSED (KEEP size_clause)?
    ;

shrink_clause
    : SHRINK SPACE_KEYWORD COMPACT? CASCADE?
    ;

records_per_block_clause
    : (MINIMIZE | NOMINIMIZE)? RECORDS_PER_BLOCK
    ;

upgrade_table_clause
    : UPGRADE (NOT? INCLUDING DATA) column_properties
    ;

truncate_table
    : TRUNCATE TABLE tableview_name PURGE? SEMICOLON
    ;

drop_table
    : DROP TABLE tableview_name PURGE? SEMICOLON
    ;

drop_view
    : DROP VIEW tableview_name (CASCADE CONSTRAINT)? SEMICOLON
    ;

comment_on_column
    : COMMENT ON COLUMN column_name IS quoted_string
    ;

enable_or_disable
    : ENABLE
    | DISABLE
    ;
allow_or_disallow
    : ALLOW
    | DISALLOW
    ;

// Synonym DDL Clauses

create_synonym
    // Synonym's schema cannot be specified for public synonyms
    : CREATE (OR REPLACE)? PUBLIC SYNONYM synonym_name FOR (schema_name PERIOD)? schema_object_name (AT_SIGN link_name)?
    | CREATE (OR REPLACE)? SYNONYM (schema_name PERIOD)? synonym_name FOR (schema_name PERIOD)? schema_object_name (AT_SIGN link_name)?
    ;

comment_on_table
    : COMMENT ON TABLE tableview_name IS quoted_string
    ;

alter_cluster
    : ALTER CLUSTER  cluster_name
        ( physical_attributes_clause
        | SIZE size_clause
        | allocate_extent_clause
        | deallocate_unused_clause
        | cache_or_nocache
        )+
        parallel_clause?
        ';'
    ;

cache_or_nocache
    : CACHE
    | NOCACHE
    ;

database_name
    : regular_id
    ;

alter_database
    : ALTER DATABASE database_name?
       ( startup_clauses
       | recovery_clauses
       | database_file_clauses
       | logfile_clauses
       | controlfile_clauses
       | standby_database_clauses
       | default_settings_clause
       | instance_clauses
       | security_clause
       )
      ';'
    ;

startup_clauses
    : MOUNT ((STANDBY | CLONE) DATABASE)?
    | OPEN (READ WRITE)? resetlogs_or_noresetlogs? upgrade_or_downgrade?
    | OPEN READ ONLY
    ;

resetlogs_or_noresetlogs
    : RESETLOGS
    | NORESETLOGS
    ;

upgrade_or_downgrade
    : UPGRADE
    | DOWNGRADE
    ;

recovery_clauses
    : general_recovery
    | managed_standby_recovery
    | begin_or_end BACKUP
    ;

begin_or_end
    : BEGIN
    | END
    ;

general_recovery
    : RECOVER AUTOMATIC? (FROM CHAR_STRING)?
       ( (full_database_recovery | partial_database_recovery | LOGFILE CHAR_STRING )?
         ((TEST | ALLOW UNSIGNED_INTEGER CORRUPTION | parallel_clause)+ )?
       | CONTINUE DEFAULT?
       | CANCEL
       )
    ;

//Need to come back to
full_database_recovery
    : STANDBY? DATABASE
          ((UNTIL (CANCEL |TIME CHAR_STRING | CHANGE UNSIGNED_INTEGER | CONSISTENT)
           | USING BACKUP CONTROLFILE
           )+
          )?
    ;

partial_database_recovery
    : TABLESPACE tablespace (',' tablespace)*
    | DATAFILE CHAR_STRING | filenumber (',' CHAR_STRING | filenumber)*
    | partial_database_recovery_10g
    ;

partial_database_recovery_10g
    : {isVersion10()}? STANDBY
      ( TABLESPACE tablespace (',' tablespace)*
      | DATAFILE CHAR_STRING | filenumber (',' CHAR_STRING | filenumber)*
      )
      UNTIL (CONSISTENT WITH)? CONTROLFILE
    ;


managed_standby_recovery
    : RECOVER (MANAGED STANDBY DATABASE
               ((USING CURRENT LOGFILE
                | DISCONNECT (FROM SESSION)?
                | NODELAY
                | UNTIL CHANGE UNSIGNED_INTEGER
                | UNTIL CONSISTENT
                | parallel_clause
                )+
               | FINISH
               | CANCEL
               )?
              | TO LOGICAL STANDBY (db_name | KEEP IDENTITY)
              )
    ;

db_name
    : regular_id
    ;
database_file_clauses
    : RENAME FILE filename (',' filename)* TO filename
    | create_datafile_clause
    | alter_datafile_clause
    | alter_tempfile_clause
    ;

create_datafile_clause
    : CREATE DATAFILE (filename | filenumber) (',' (filename | filenumber) )*
        (AS (//TODO (','? file_specification)+ |
              NEW) )?
    ;

alter_datafile_clause
    : DATAFILE (filename|filenumber) (',' (filename|filenumber) )*
        ( ONLINE
        | OFFLINE (FOR DROP)?
        | RESIZE size_clause
        | autoextend_clause
        | END BACKUP
        )
    ;

alter_tempfile_clause
    : TEMPFILE (filename | filenumber) (',' (filename | filenumber) )*
        ( RESIZE size_clause
        | autoextend_clause
        | DROP (INCLUDING DATAFILES)
        | ONLINE
        | OFFLINE
        )
    ;

logfile_clauses
    : (ARCHIVELOG MANUAL? | NOARCHIVELOG)
    | NO? FORCE LOGGING
    | RENAME FILE filename (',' filename)* TO filename
    | CLEAR UNARCHIVED? LOGFILE logfile_descriptor (',' logfile_descriptor)* (UNRECOVERABLE DATAFILE)?
    | add_logfile_clauses
    | drop_logfile_clauses
    | switch_logfile_clause
    | supplemental_db_logging
    ;

add_logfile_clauses
    : ADD STANDBY? LOGFILE
             (
//TODO        (INSTANCE CHAR_STRING | THREAD UNSIGNED_INTEGER)?
               (log_file_group   redo_log_file_spec)+
             | MEMBER filename REUSE? (',' filename REUSE?)* TO logfile_descriptor (',' logfile_descriptor)*
             )
    ;

log_file_group
    :(','? (THREAD UNSIGNED_INTEGER)? GROUP UNSIGNED_INTEGER)
    ;

drop_logfile_clauses
    : DROP STANDBY?
          LOGFILE (logfile_descriptor (',' logfile_descriptor)*
                  | MEMBER filename (',' filename)*
                  )
    ;

switch_logfile_clause
    : SWITCH ALL LOGFILES TO BLOCKSIZE UNSIGNED_INTEGER
    ;

supplemental_db_logging
    :  add_or_drop
          SUPPLEMENTAL LOG (DATA
                           | supplemental_id_key_clause
                           | supplemental_plsql_clause
                           )
    ;

add_or_drop
    : ADD
    | DROP
    ;

supplemental_plsql_clause
    : DATA FOR PROCEDURAL REPLICATION
    ;

logfile_descriptor
    : GROUP UNSIGNED_INTEGER
    | '(' filename (',' filename)* ')'
    | filename
    ;

controlfile_clauses
    : CREATE (LOGICAL | PHYSICAL)? STANDBY CONTROLFILE AS filename REUSE?
    | BACKUP CONTROLFILE TO (filename REUSE? | trace_file_clause)
    ;

trace_file_clause
    : TRACE (AS filename REUSE?)? (RESETLOGS|NORESETLOGS)?
    ;

standby_database_clauses
    : ( activate_standby_db_clause
      | maximize_standby_db_clause
      | register_logfile_clause
      | commit_switchover_clause
      | start_standby_clause
      | stop_standby_clause
      | convert_database_clause
      )
      parallel_clause?
    ;

activate_standby_db_clause
    : ACTIVATE (PHYSICAL | LOGICAL)? STANDBY DATABASE (FINISH APPLY)?
    ;

maximize_standby_db_clause
    : SET STANDBY DATABASE TO MAXIMIZE (PROTECTION | AVAILABILITY | PERFORMANCE)
    ;

register_logfile_clause
    : REGISTER (OR REPLACE)? (PHYSICAL | LOGICAL) LOGFILE //TODO (','? file_specification)+
    //TODO   (FOR logminer_session_name)?
    ;

commit_switchover_clause
    : (PREPARE | COMMIT) TO SWITCHOVER
        ((TO (((PHYSICAL | LOGICAL)? PRIMARY |  PHYSICAL? STANDBY)
           ((WITH | WITHOUT)? SESSION SHUTDOWN (WAIT | NOWAIT) )?
          | LOGICAL STANDBY
          )
         | LOGICAL STANDBY
         )
        | CANCEL
        )?
    ;

start_standby_clause
    : START LOGICAL STANDBY APPLY IMMEDIATE? NODELAY?
        ( NEW PRIMARY regular_id
        | INITIAL scn_value=UNSIGNED_INTEGER?
        | SKIP_ FAILED TRANSACTION
        | FINISH
        )?
    ;

stop_standby_clause
    : (STOP | ABORT) LOGICAL STANDBY APPLY
    ;

convert_database_clause
    : CONVERT TO (PHYSICAL | SNAPSHOT) STANDBY
    ;

default_settings_clause
    : DEFAULT EDITION EQUALS_OP edition_name
    | SET DEFAULT (BIGFILE | SMALLFILE) TABLESPACE
    | DEFAULT TABLESPACE tablespace
    | DEFAULT TEMPORARY TABLESPACE (tablespace | tablespace_group_name)
    | RENAME GLOBAL_NAME TO database ('.' domain)+
    | ENABLE BLOCK CHANGE TRACKING (USING FILE filename REUSE?)?
    | DISABLE BLOCK CHANGE TRACKING
    | flashback_mode_clause
    | set_time_zone_clause
    ;

set_time_zone_clause
    : SET TIMEZONE EQUALS_OP CHAR_STRING
    ;

instance_clauses
    : enable_or_disable INSTANCE CHAR_STRING
    ;

security_clause
    : GUARD (ALL | STANDBY | NONE)
    ;

domain
    : regular_id
    ;

database
    : regular_id
    ;

edition_name
    : regular_id
    ;

filenumber
    : UNSIGNED_INTEGER
    ;

filename
    : CHAR_STRING
    ;

alter_table
    : ALTER TABLE tableview_name
      (
      | alter_table_properties
      | constraint_clauses
      | column_clauses
//TODO      | alter_table_partitioning
//TODO      | alter_external_table
      | move_table_clause
      )
      ((enable_disable_clause | enable_or_disable (TABLE LOCK | ALL TRIGGERS) )+)?
      ';'
    ;

alter_table_properties
    : alter_table_properties_1
    | RENAME TO tableview_name
    | shrink_clause
    | READ ONLY
    | READ WRITE
    | REKEY CHAR_STRING
    ;

alter_table_properties_1
    : ( physical_attributes_clause
      | logging_clause
      | table_compression
      | supplemental_table_logging
      | allocate_extent_clause
      | deallocate_unused_clause
      | (CACHE | NOCACHE)
      | RESULT_CACHE '(' MODE (DEFAULT | FORCE) ')'
      | upgrade_table_clause
      | records_per_block_clause
      | parallel_clause
      | row_movement_clause
      | flashback_archive_clause
      )+
      alter_iot_clauses?
    ;

alter_iot_clauses
    : index_org_table_clause
    | alter_overflow_clause
    | alter_mapping_table_clause
    | COALESCE
    ;

alter_mapping_table_clause
    : MAPPING TABLE (allocate_extent_clause | deallocate_unused_clause)
    ;

alter_overflow_clause
    : add_overflow_clause
    | OVERFLOW (segment_attributes_clause | allocate_extent_clause | shrink_clause | deallocate_unused_clause)+
    ;

add_overflow_clause
    : ADD OVERFLOW segment_attributes_clause? ('(' PARTITION segment_attributes_clause? (',' PARTITION segment_attributes_clause?)*  ')' )?
    ;


enable_disable_clause
    : (ENABLE | DISABLE) (VALIDATE | NOVALIDATE)?
         (UNIQUE '(' column_name (',' column_name)* ')'
         | PRIMARY KEY
         | CONSTRAINT constraint_name
         ) using_index_clause? exceptions_clause?
         CASCADE? ((KEEP | DROP) INDEX)?
    ;

using_index_clause
    : USING INDEX (index_name | '(' create_index ')' | index_attributes )?
    ;

index_attributes
    : ( physical_attributes_clause
      | logging_clause
      | TABLESPACE (tablespace | DEFAULT)
      | key_compression
      | sort_or_nosort
      | REVERSE
      | visible_or_invisible
      | parallel_clause
      )+
    ;

sort_or_nosort
    : SORT
    | NOSORT
    ;

exceptions_clause
    : EXCEPTIONS INTO tableview_name
    ;

move_table_clause
    : MOVE ONLINE? segment_attributes_clause? table_compression? index_org_table_clause? ((lob_storage_clause | varray_col_properties)+)? parallel_clause?
    ;

index_org_table_clause
    : (mapping_table_clause | PCTTHRESHOLD UNSIGNED_INTEGER | key_compression) index_org_overflow_clause?
    ;

mapping_table_clause
    : MAPPING TABLE
    | NOMAPPING
    ;

key_compression
    : NOCOMPRESS
    | COMPRESS UNSIGNED_INTEGER
    ;

index_org_overflow_clause
    : (INCLUDING column_name)? OVERFLOW segment_attributes_clause?
    ;

column_clauses
    : add_modify_drop_column_clauses
    | rename_column_clause
    | modify_collection_retrieval
    | modify_lob_storage_clause
    ;

modify_collection_retrieval
    : MODIFY NESTED TABLE collection_item RETURN AS (LOCATOR | VALUE)
    ;

collection_item
    : tableview_name
    ;

rename_column_clause
    : RENAME COLUMN old_column_name TO new_column_name
    ;

old_column_name
    : column_name
    ;

new_column_name
    : column_name
    ;

add_modify_drop_column_clauses
    : (add_column_clause
      |modify_column_clauses
      |drop_column_clause
      )+
    ;

drop_column_clause
    : SET UNUSED (COLUMN column_name| ('(' column_name (',' column_name)* ')' )) (CASCADE CONSTRAINTS | INVALIDATE)*
    | DROP (COLUMN column_name | '(' column_name (',' column_name)* ')' ) (CASCADE CONSTRAINTS | INVALIDATE)* (CHECKPOINT UNSIGNED_INTEGER)?
    | DROP (UNUSED COLUMNS | COLUMNS CONTINUE) (CHECKPOINT UNSIGNED_INTEGER)
    ;

modify_column_clauses
    : MODIFY ('(' modify_col_properties (',' modify_col_properties)* ')'
             | modify_col_properties
             | modify_col_substitutable
             )
    ;

modify_col_properties
    : column_name datatype? (DEFAULT expression)? (ENCRYPT encryption_spec | DECRYPT)? inline_constraint* lob_storage_clause? //TODO alter_xmlschema_clause
    ;

modify_col_substitutable
    : COLUMN column_name NOT? SUBSTITUTABLE AT ALL LEVELS FORCE?
    ;

add_column_clause
    : ADD ('(' (column_definition | virtual_column_definition) (',' (column_definition
              | virtual_column_definition)
              )*
          ')'
          | ( column_definition | virtual_column_definition ))
       column_properties?
//TODO       (','? out_of_line_part_storage )
    ;

alter_varray_col_properties
    : MODIFY VARRAY varray_item '(' modify_lob_parameters ')'
    ;

varray_col_properties
    : VARRAY varray_item ( substitutable_column_clause? varray_storage_clause
                         | substitutable_column_clause
                         )
    ;

varray_storage_clause
    : STORE AS (SECUREFILE|BASICFILE)? LOB ( lob_segname? '(' lob_storage_parameters ')'
                                           | lob_segname
                                           )
    ;

lob_segname
    : regular_id
    ;

lob_item
    : regular_id
    ;

lob_storage_parameters
    :  TABLESPACE tablespace | (lob_parameters storage_clause? )
    |  storage_clause
    ;

lob_storage_clause
    : LOB ( '(' lob_item (',' lob_item)* ')' STORE AS ( (SECUREFILE|BASICFILE) | '(' lob_storage_parameters ')' )+
          | '(' lob_item ')' STORE AS ( (SECUREFILE | BASICFILE) | lob_segname | '(' lob_storage_parameters ')' )+
          )
    ;

modify_lob_storage_clause
    : MODIFY LOB '(' lob_item ')' '(' modify_lob_parameters ')'
    ;

modify_lob_parameters
    : ( storage_clause
      | (PCTVERSION | FREEPOOLS) UNSIGNED_INTEGER
      | REBUILD FREEPOOLS
      | lob_retention_clause
      | lob_deduplicate_clause
      | lob_compression_clause
      | ENCRYPT encryption_spec
      | DECRYPT
      | CACHE
      | (CACHE | NOCACHE | CACHE READS) logging_clause?
      | allocate_extent_clause
      | shrink_clause
      | deallocate_unused_clause
     )+
    ;

lob_parameters
    : ( (ENABLE | DISABLE) STORAGE IN ROW
      | CHUNK UNSIGNED_INTEGER
      | PCTVERSION UNSIGNED_INTEGER
      | FREEPOOLS UNSIGNED_INTEGER
      | lob_retention_clause
      | lob_deduplicate_clause
      | lob_compression_clause
      | ENCRYPT encryption_spec
      | DECRYPT
      | (CACHE | NOCACHE | CACHE READS) logging_clause?
      )+
    ;

lob_deduplicate_clause
    : DEDUPLICATE
    | KEEP_DUPLICATES
    ;

lob_compression_clause
    : NOCOMPRESS
    | COMPRESS (HIGH | MEDIUM | LOW)?
    ;

lob_retention_clause
    : RETENTION (MAX | MIN UNSIGNED_INTEGER | AUTO | NONE)?
    ;

encryption_spec
    : (USING  CHAR_STRING)? (IDENTIFIED BY REGULAR_ID)? CHAR_STRING? (NO? SALT)?
    ;
tablespace
    : regular_id
    ;

varray_item
    : (id_expression '.')? (id_expression '.')? id_expression
    ;

column_properties
    : object_type_col_properties
    | nested_table_col_properties
    | (varray_col_properties | lob_storage_clause) //TODO '(' ( ','? lob_partition_storage)+ ')'
    | xmltype_column_properties
    ;

period_definition
    : {isVersion12()}? PERIOD FOR column_name
        ( '(' start_time_column ',' end_time_column ')' )?
    ;

start_time_column
    : column_name
    ;

end_time_column
    : column_name
    ;

column_definition
    : column_name (datatype | type_name)
         SORT?  (DEFAULT expression)? (ENCRYPT (USING  CHAR_STRING)? (IDENTIFIED BY regular_id)? CHAR_STRING? (NO? SALT)? )?  (inline_constraint* | inline_ref_constraint)
    ;

virtual_column_definition
    : column_name datatype? autogenerated_sequence_definition?
        VIRTUAL? inline_constraint*
    ;

autogenerated_sequence_definition
    : GENERATED (ALWAYS | BY DEFAULT (ON NULL_)?)? AS IDENTITY ( '(' (sequence_start_clause | sequence_spec)* ')' )?
    ;

out_of_line_part_storage
    : PARTITION partition_name
    ;

nested_table_col_properties
    : NESTED TABLE  (nested_item | COLUMN_VALUE) substitutable_column_clause? (LOCAL | GLOBAL)?
       STORE AS tableview_name ( '(' ( '(' object_properties ')'
                                     | physical_properties
                                     | column_properties
                                     )+
                                  ')'
                               )?
        (RETURN AS? (LOCATOR | VALUE) )?
     ;

nested_item
    : regular_id
    ;

substitutable_column_clause
    : ELEMENT? IS OF TYPE? '(' type_name ')'
    | NOT? SUBSTITUTABLE AT ALL LEVELS
    ;

partition_name
    : regular_id
    ;

supplemental_logging_props
    : SUPPLEMENTAL LOG (supplemental_log_grp_clause | supplemental_id_key_clause)
    ;

column_or_attribute
    : regular_id
    ;

object_type_col_properties
    : COLUMN column=regular_id substitutable_column_clause
    ;

constraint_clauses
    : ADD '(' (out_of_line_constraint* | out_of_line_ref_constraint) ')'
    | ADD  (out_of_line_constraint* | out_of_line_ref_constraint)
    | MODIFY (CONSTRAINT constraint_name | PRIMARY KEY | UNIQUE '(' column_name (',' column_name)* ')')  constraint_state CASCADE?
    | RENAME CONSTRAINT old_constraint_name TO new_constraint_name
    | drop_constraint_clause+
    ;

old_constraint_name
    : constraint_name
    ;

new_constraint_name
    : constraint_name
    ;

drop_constraint_clause
    : DROP  drop_primary_key_or_unique_or_generic_clause
    ;

drop_primary_key_or_unique_or_generic_clause
    : (PRIMARY KEY | UNIQUE '(' column_name (',' column_name)* ')') CASCADE? (KEEP | DROP)?
    | CONSTRAINT constraint_name CASCADE?
    ;

add_constraint
    : ADD (CONSTRAINT constraint_name)? add_constraint_clause (',' (CONSTRAINT constraint_name)? add_constraint_clause)+
    ;

add_constraint_clause
    : primary_key_clause
     | foreign_key_clause
     | unique_key_clause
     | check_constraint
     ;

check_constraint
    : CHECK '(' condition ')' DISABLE?
    ;

drop_constraint
    : DROP CONSTRAINT constraint_name
    ;

enable_constraint
    : ENABLE CONSTRAINT constraint_name
    ;

disable_constraint
    : DISABLE CONSTRAINT constraint_name
    ;

foreign_key_clause
    : FOREIGN KEY paren_column_list references_clause on_delete_clause?
    ;

references_clause
    : REFERENCES tableview_name paren_column_list
    ;

on_delete_clause
    : ON DELETE (CASCADE | SET NULL_)
    ;

unique_key_clause
    : UNIQUE paren_column_list using_index_clause?
    ;

primary_key_clause
    : PRIMARY KEY paren_column_list using_index_clause?
    ;

// Anonymous PL/SQL code block

anonymous_block
    : (DECLARE seq_of_declare_specs)? BEGIN seq_of_statements (EXCEPTION exception_handler+)? END SEMICOLON
    ;

// Common DDL Clauses

invoker_rights_clause
    : AUTHID (CURRENT_USER | DEFINER)
    ;

call_spec
    : LANGUAGE (java_spec | c_spec)
    ;

// Call Spec Specific Clauses

java_spec
    : JAVA NAME CHAR_STRING
    ;

c_spec
    : C_LETTER (NAME CHAR_STRING)? LIBRARY identifier c_agent_in_clause? (WITH CONTEXT)? c_parameters_clause?
    ;

c_agent_in_clause
    : AGENT IN '(' expressions ')'
    ;

c_parameters_clause
    : PARAMETERS '(' (expressions | '.' '.' '.') ')'
    ;

parameter
    : parameter_name (IN | OUT | INOUT | NOCOPY)* type_spec? default_value_part?
    ;

default_value_part
    : (ASSIGN_OP | DEFAULT) expression
    ;

// Elements Declarations

seq_of_declare_specs
    : declare_spec+
    ;

declare_spec
    : pragma_declaration
    | exception_declaration
    | variable_declaration
    | subtype_declaration
    | cursor_declaration
    | type_declaration
    | procedure_spec
    | function_spec
    | procedure_body
    | function_body
    ;

// incorporates constant_declaration
variable_declaration
    : identifier CONSTANT? type_spec (NOT NULL_)? default_value_part? ';'
    ;

subtype_declaration
    : SUBTYPE identifier IS type_spec (RANGE expression '..' expression)? (NOT NULL_)? ';'
    ;

// cursor_declaration incorportates curscursor_body and cursor_spec

cursor_declaration
    : CURSOR identifier ('(' parameter_spec (',' parameter_spec)* ')' )? (RETURN type_spec)? (IS select_statement)? ';'
    ;

parameter_spec
    : parameter_name (IN? type_spec)? default_value_part?
    ;

exception_declaration
    : identifier EXCEPTION ';'
    ;

pragma_declaration
    : PRAGMA (SERIALLY_REUSABLE
    | AUTONOMOUS_TRANSACTION
    | EXCEPTION_INIT '(' exception_name ',' numeric_negative ')'
    | INLINE '(' id1=identifier ',' expression ')'
    | RESTRICT_REFERENCES '(' (identifier | DEFAULT) (',' identifier)+ ')') ';'
    ;

// Record Declaration Specific Clauses

// incorporates ref_cursor_type_definition

record_type_def
    : RECORD '(' field_spec (',' field_spec)* ')'
    ;

field_spec
    : column_name type_spec? (NOT NULL_)? default_value_part?
    ;

ref_cursor_type_def
    : REF CURSOR (RETURN type_spec)?
    ;

type_declaration
    : TYPE identifier IS (table_type_def | varray_type_def | record_type_def | ref_cursor_type_def) ';'
    ;

table_type_def
    : TABLE OF type_spec table_indexed_by_part? (NOT NULL_)?
    ;

table_indexed_by_part
    : (idx1=INDEXED | idx2=INDEX) BY type_spec
    ;

varray_type_def
    : (VARRAY | VARYING ARRAY) '(' expression ')' OF type_spec (NOT NULL_)?
    ;

// Statements

seq_of_statements
    : (statement (';' | EOF) | label_declaration)+
    ;

label_declaration
    : ltp1= '<' '<' label_name '>' '>'
    ;

statement
    : body
    | block
    | assignment_statement
    | continue_statement
    | exit_statement
    | goto_statement
    | if_statement
    | loop_statement
    | forall_statement
    | null_statement
    | raise_statement
    | return_statement
    | case_statement
    | sql_statement
    | function_call
    | pipe_row_statement
    | procedure_call
    ;

swallow_to_semi
    : ~';'+
    ;

assignment_statement
    : (general_element | bind_variable) ASSIGN_OP expression
    ;

continue_statement
    : CONTINUE label_name? (WHEN condition)?
    ;

exit_statement
    : EXIT label_name? (WHEN condition)?
    ;

goto_statement
    : GOTO label_name
    ;

if_statement
    : IF condition THEN seq_of_statements elsif_part* else_part? END IF
    ;

elsif_part
    : ELSIF condition THEN seq_of_statements
    ;

else_part
    : ELSE seq_of_statements
    ;

loop_statement
    : label_declaration? (WHILE condition | FOR cursor_loop_param)? LOOP seq_of_statements END LOOP label_name?
    ;

// Loop Specific Clause

cursor_loop_param
    : index_name IN REVERSE? lower_bound range_separator='..' upper_bound
    | record_name IN (cursor_name ('(' expressions? ')')? | '(' select_statement ')')
    ;

forall_statement
    : FORALL index_name IN bounds_clause sql_statement (SAVE EXCEPTIONS)?
    ;

bounds_clause
    : lower_bound '..' upper_bound
    | INDICES OF collection_name between_bound?
    | VALUES OF index_name
    ;

between_bound
    : BETWEEN lower_bound AND upper_bound
    ;

lower_bound
    : concatenation
    ;

upper_bound
    : concatenation
    ;

null_statement
    : NULL_
    ;

raise_statement
    : RAISE exception_name?
    ;

return_statement
    : RETURN expression?
    ;

function_call
    : CALL? routine_name function_argument?
    ;

procedure_call
    : routine_name function_argument?
    ;

pipe_row_statement
    : PIPE ROW '(' expression ')';

body
    : BEGIN seq_of_statements (EXCEPTION exception_handler+)? END label_name?
    ;

// Body Specific Clause

exception_handler
    : WHEN exception_name (OR exception_name)* THEN seq_of_statements
    ;

trigger_block
    : (DECLARE declare_spec*)? body
    ;

block
    : DECLARE? declare_spec+ body
    ;

// SQL Statements

sql_statement
    : execute_immediate
    | data_manipulation_language_statements
    | cursor_manipulation_statements
    | transaction_control_statements
    ;

execute_immediate
    : EXECUTE IMMEDIATE expression (into_clause using_clause? | using_clause dynamic_returning_clause? | dynamic_returning_clause)?
    ;

// Execute Immediate Specific Clause

dynamic_returning_clause
    : (RETURNING | RETURN) into_clause
    ;

// DML Statements

data_manipulation_language_statements
    : merge_statement
    | lock_table_statement
    | select_statement
    | update_statement
    | delete_statement
    | insert_statement
    | explain_statement
    ;

// Cursor Manipulation Statements

cursor_manipulation_statements
    : close_statement
    | open_statement
    | fetch_statement
    | open_for_statement
    ;

close_statement
    : CLOSE cursor_name
    ;

open_statement
    : OPEN cursor_name ('(' expressions? ')')?
    ;

fetch_statement
    : FETCH cursor_name (it1=INTO variable_name (',' variable_name)* | BULK COLLECT INTO variable_name (',' variable_name)* (LIMIT (numeric | variable_name))?)
    ;

open_for_statement
    : OPEN variable_name FOR (select_statement | expression) using_clause?
    ;

// Transaction Control SQL Statements

transaction_control_statements
    : set_transaction_command
    | set_constraint_command
    | commit_statement
    | rollback_statement
    | savepoint_statement
    ;

set_transaction_command
    : SET TRANSACTION
      (READ (ONLY | WRITE) | ISOLATION LEVEL (SERIALIZABLE | READ COMMITTED) | USE ROLLBACK SEGMENT rollback_segment_name)?
      (NAME quoted_string)?
    ;

set_constraint_command
    : SET (CONSTRAINT | CONSTRAINTS) (ALL | constraint_name (',' constraint_name)*) (IMMEDIATE | DEFERRED)
    ;

commit_statement
    : COMMIT WORK?
      (COMMENT expression | FORCE (CORRUPT_XID expression | CORRUPT_XID_ALL | expression (',' expression)?))?
      write_clause?
    ;

write_clause
    : WRITE (WAIT | NOWAIT)? (IMMEDIATE | BATCH)?
    ;

rollback_statement
    : ROLLBACK WORK? (TO SAVEPOINT? savepoint_name | FORCE quoted_string)?
    ;

savepoint_statement
    : SAVEPOINT savepoint_name
    ;

// Dml

/* TODO
//SHOULD BE OVERRIDEN!
compilation_unit
    : seq_of_statements* EOF
    ;

//SHOULD BE OVERRIDEN!
seq_of_statements
    : select_statement
    | update_statement
    | delete_statement
    | insert_statement
    | lock_table_statement
    | merge_statement
    | explain_statement
//    | case_statement[true]
    ;
*/

explain_statement
    : EXPLAIN PLAN (SET STATEMENT_ID '=' quoted_string)? (INTO tableview_name)?
      FOR (select_statement | update_statement | delete_statement | insert_statement | merge_statement)
    ;

select_only_statement
    : subquery_factoring_clause? subquery
    ;

select_statement
    : select_only_statement (for_update_clause | order_by_clause | offset_clause | fetch_clause)*
    ;

// Select Specific Clauses

subquery_factoring_clause
    : WITH factoring_element (',' factoring_element)*
    ;

factoring_element
    : query_name paren_column_list? AS '(' subquery order_by_clause? ')'
      search_clause? cycle_clause?
    ;

search_clause
    : SEARCH (DEPTH | BREADTH) FIRST BY column_name ASC? DESC? (NULLS FIRST)? (NULLS LAST)?
      (',' column_name ASC? DESC? (NULLS FIRST)? (NULLS LAST)?)* SET column_name
    ;

cycle_clause
    : CYCLE column_list SET column_name TO expression DEFAULT expression
    ;

subquery
    : subquery_basic_elements subquery_operation_part*
    ;

subquery_basic_elements
    : query_block
    | '(' subquery ')'
    ;

subquery_operation_part
    : (UNION ALL? | INTERSECT | MINUS) subquery_basic_elements
    ;

query_block
    : SELECT (DISTINCT | UNIQUE | ALL)? selected_list
      into_clause? from_clause where_clause? hierarchical_query_clause? group_by_clause? model_clause? order_by_clause? fetch_clause?
    ;

selected_list
    : '*'
    | select_list_elements (',' select_list_elements)*
    ;

from_clause
    : FROM table_ref_list
    ;

select_list_elements
    : tableview_name '.' ASTERISK
    | expression column_alias?
    ;

table_ref_list
    : table_ref (',' table_ref)*
    ;

// NOTE to PIVOT clause
// according the SQL reference this should not be possible
// according to he reality it is. Here we probably apply pivot/unpivot onto whole join clause
// eventhough it is not enclosed in parenthesis. See pivot examples 09,10,11

table_ref
    : table_ref_aux join_clause* (pivot_clause | unpivot_clause)?
    ;

table_ref_aux
    : table_ref_aux_internal flashback_query_clause* (/*{isTableAlias()}?*/ table_alias)?
    ;

table_ref_aux_internal
    : dml_table_expression_clause (pivot_clause | unpivot_clause)?                 # table_ref_aux_internal_one
    | '(' table_ref subquery_operation_part* ')' (pivot_clause | unpivot_clause)?  # table_ref_aux_internal_two
    | ONLY '(' dml_table_expression_clause ')'                                     # table_ref_aux_internal_three
    ;

join_clause
    : query_partition_clause? (CROSS | NATURAL)? (INNER | outer_join_type)?
      JOIN table_ref_aux query_partition_clause? (join_on_part | join_using_part)*
    ;

join_on_part
    : ON condition
    ;

join_using_part
    : USING paren_column_list
    ;

outer_join_type
    : (FULL | LEFT | RIGHT) OUTER?
    ;

query_partition_clause
    : PARTITION BY (('(' (subquery | expressions)? ')') | expressions)
    ;

flashback_query_clause
    : VERSIONS BETWEEN (SCN | TIMESTAMP) expression
    | AS OF (SCN | TIMESTAMP | SNAPSHOT) expression
    ;

pivot_clause
    : PIVOT XML? '(' pivot_element (',' pivot_element)* pivot_for_clause pivot_in_clause ')'
    ;

pivot_element
    : aggregate_function_name '(' expression ')' column_alias?
    ;

pivot_for_clause
    : FOR (column_name | paren_column_list)
    ;

pivot_in_clause
    : IN '(' (subquery | ANY (',' ANY)* | pivot_in_clause_element (',' pivot_in_clause_element)*) ')'
    ;

pivot_in_clause_element
    : pivot_in_clause_elements column_alias?
    ;

pivot_in_clause_elements
    : expression
    | '(' expressions? ')'
    ;

unpivot_clause
    : UNPIVOT ((INCLUDE | EXCLUDE) NULLS)?
    '(' (column_name | paren_column_list) pivot_for_clause unpivot_in_clause ')'
    ;

unpivot_in_clause
    : IN '(' unpivot_in_elements (',' unpivot_in_elements)* ')'
    ;

unpivot_in_elements
    : (column_name | paren_column_list)
      (AS (constant | '(' constant (',' constant)* ')'))?
    ;

hierarchical_query_clause
    : CONNECT BY NOCYCLE? condition start_part?
    | start_part CONNECT BY NOCYCLE? condition
    ;

start_part
    : START WITH condition
    ;

group_by_clause
    : GROUP BY group_by_elements (',' group_by_elements)* having_clause?
    | having_clause (GROUP BY group_by_elements (',' group_by_elements)*)?
    ;

group_by_elements
    : grouping_sets_clause
    | rollup_cube_clause
    | expression
    ;

rollup_cube_clause
    : (ROLLUP | CUBE) '(' grouping_sets_elements (',' grouping_sets_elements)* ')'
    ;

grouping_sets_clause
    : GROUPING SETS '(' grouping_sets_elements (',' grouping_sets_elements)* ')'
    ;

grouping_sets_elements
    : rollup_cube_clause
    | '(' expressions? ')'
    | expression
    ;

having_clause
    : HAVING condition
    ;

model_clause
    : MODEL cell_reference_options* return_rows_clause? reference_model* main_model
    ;

cell_reference_options
    : (IGNORE | KEEP) NAV
    | UNIQUE (DIMENSION | SINGLE REFERENCE)
    ;

return_rows_clause
    : RETURN (UPDATED | ALL) ROWS
    ;

reference_model
    : REFERENCE reference_model_name ON '(' subquery ')' model_column_clauses cell_reference_options*
    ;

main_model
    : (MAIN main_model_name)? model_column_clauses cell_reference_options* model_rules_clause
    ;

model_column_clauses
    : model_column_partition_part? DIMENSION BY model_column_list MEASURES model_column_list
    ;

model_column_partition_part
    : PARTITION BY model_column_list
    ;

model_column_list
    : '(' model_column (',' model_column)*  ')'
    ;

model_column
    : (expression | query_block) column_alias?
    ;

model_rules_clause
    : model_rules_part? '(' (model_rules_element (',' model_rules_element)*)? ')'
    ;

model_rules_part
    : RULES (UPDATE | UPSERT ALL?)? ((AUTOMATIC | SEQUENTIAL) ORDER)? model_iterate_clause?
    ;

model_rules_element
    : (UPDATE | UPSERT ALL?)? cell_assignment order_by_clause? '=' expression
    ;

cell_assignment
    : model_expression
    ;

model_iterate_clause
    : ITERATE '(' expression ')' until_part?
    ;

until_part
    : UNTIL '(' condition ')'
    ;

order_by_clause
    : ORDER SIBLINGS? BY order_by_elements (',' order_by_elements)*
    ;

order_by_elements
    : expression (ASC | DESC)? (NULLS (FIRST | LAST))?
    ;

offset_clause
    : OFFSET expression (ROW | ROWS)
    ;

fetch_clause
    : FETCH (FIRST | NEXT) (expression PERCENT_KEYWORD?)? (ROW | ROWS) (ONLY | WITH TIES)
    ;

for_update_clause
    : FOR UPDATE for_update_of_part? for_update_options?
    ;

for_update_of_part
    : OF column_list
    ;

for_update_options
    : SKIP_ LOCKED
    | NOWAIT
    | WAIT expression
    ;

update_statement
    : UPDATE general_table_ref update_set_clause where_clause? static_returning_clause? error_logging_clause?
    ;

// Update Specific Clauses

update_set_clause
    : SET
      (column_based_update_set_clause (',' column_based_update_set_clause)* | VALUE '(' identifier ')' '=' expression)
    ;

column_based_update_set_clause
    : column_name '=' expression
    | paren_column_list '=' subquery
    ;

delete_statement
    : DELETE FROM? general_table_ref where_clause? static_returning_clause? error_logging_clause?
    ;

insert_statement
    : INSERT (single_table_insert | multi_table_insert)
    ;

// Insert Specific Clauses

single_table_insert
    : insert_into_clause (values_clause static_returning_clause? | select_statement) error_logging_clause?
    ;

multi_table_insert
    : (ALL multi_table_element+ | conditional_insert_clause) select_statement
    ;

multi_table_element
    : insert_into_clause values_clause? error_logging_clause?
    ;

conditional_insert_clause
    : (ALL | FIRST)? conditional_insert_when_part+ conditional_insert_else_part?
    ;

conditional_insert_when_part
    : WHEN condition THEN multi_table_element+
    ;

conditional_insert_else_part
    : ELSE multi_table_element+
    ;

insert_into_clause
    : INTO general_table_ref paren_column_list?
    ;

values_clause
    : VALUES (REGULAR_ID | '(' expressions ')')
    ;

merge_statement
    : MERGE INTO tableview_name table_alias? USING selected_tableview ON '(' condition ')'
      (merge_update_clause merge_insert_clause? | merge_insert_clause merge_update_clause?)?
      error_logging_clause?
    ;

// Merge Specific Clauses

merge_update_clause
    : WHEN MATCHED THEN UPDATE SET merge_element (',' merge_element)* where_clause? merge_update_delete_part?
    ;

merge_element
    : column_name '=' expression
    ;

merge_update_delete_part
    : DELETE where_clause
    ;

merge_insert_clause
    : WHEN NOT MATCHED THEN INSERT paren_column_list?
      values_clause where_clause?
    ;

selected_tableview
    : (tableview_name | '(' select_statement ')') table_alias?
    ;

lock_table_statement
    : LOCK TABLE lock_table_element (',' lock_table_element)* IN lock_mode MODE wait_nowait_part?
    ;

wait_nowait_part
    : WAIT expression
    | NOWAIT
    ;

// Lock Specific Clauses

lock_table_element
    : tableview_name partition_extension_clause?
    ;

lock_mode
    : ROW SHARE
    | ROW EXCLUSIVE
    | SHARE UPDATE?
    | SHARE ROW EXCLUSIVE
    | EXCLUSIVE
    ;

// Common DDL Clauses

general_table_ref
    : (dml_table_expression_clause | ONLY '(' dml_table_expression_clause ')') table_alias?
    ;

static_returning_clause
    : (RETURNING | RETURN) expressions into_clause
    ;

error_logging_clause
    : LOG ERRORS error_logging_into_part? expression? error_logging_reject_part?
    ;

error_logging_into_part
    : INTO tableview_name
    ;

error_logging_reject_part
    : REJECT LIMIT (UNLIMITED | expression)
    ;

dml_table_expression_clause
    : table_collection_expression
    | '(' select_statement subquery_restriction_clause? ')'
    | tableview_name sample_clause?
    ;

table_collection_expression
    : (TABLE | THE) ('(' subquery ')' | '(' expression ')' outer_join_sign?)
    ;

subquery_restriction_clause
    : WITH (READ ONLY | CHECK OPTION (CONSTRAINT constraint_name)?)
    ;

sample_clause
    : SAMPLE BLOCK? '(' expression (',' expression)? ')' seed_part?
    ;

seed_part
    : SEED '(' expression ')'
    ;

// Expression & Condition

condition
    : expression
    ;

expressions
    : expression (',' expression)*
    ;

expression
    : cursor_expression
    | logical_expression
    ;

cursor_expression
    : CURSOR '(' subquery ')'
    ;

logical_expression
    : unary_logical_expression
    | logical_expression AND logical_expression
    | logical_expression OR logical_expression
    ;

unary_logical_expression
    : NOT? multiset_expression (IS NOT? logical_operation)*
    ;

logical_operation:
        (NULL_
        | NAN | PRESENT
        | INFINITE | A_LETTER SET | EMPTY
        | OF TYPE? '(' ONLY? type_spec (',' type_spec)* ')')
    ;

multiset_expression
    : relational_expression (multiset_type=(MEMBER | SUBMULTISET) OF? concatenation)?
    ;

relational_expression
    : relational_expression relational_operator relational_expression
    | compound_expression
    ;

compound_expression
    : concatenation
      (NOT? ( IN in_elements
            | BETWEEN between_elements
            | like_type=(LIKE | LIKEC | LIKE2 | LIKE4) concatenation (ESCAPE concatenation)?))?
    ;

relational_operator
    : '='
    | (NOT_EQUAL_OP | '<' '>' | '!' '=' | '^' '=')
    | ('<' | '>') '='?
    ;

in_elements
    : '(' subquery ')'
    | '(' concatenation (',' concatenation)* ')'
    | constant
    | bind_variable
    | general_element
    ;

between_elements
    : concatenation AND concatenation
    ;

concatenation
    : model_expression
        (AT (LOCAL | TIME ZONE concatenation) | interval_expression)?
        (ON OVERFLOW (TRUNCATE | ERROR))?
    | concatenation op=(ASTERISK | SOLIDUS) concatenation
    | concatenation op=(PLUS_SIGN | MINUS_SIGN) concatenation
    | concatenation BAR BAR concatenation
    ;

interval_expression
    : DAY ('(' concatenation ')')? TO SECOND ('(' concatenation ')')?
    | YEAR ('(' concatenation ')')? TO MONTH
    ;

model_expression
    : unary_expression ('[' model_expression_element ']')?
    ;

model_expression_element
    : (ANY | expression) (',' (ANY | expression))*
    | single_column_for_loop (',' single_column_for_loop)*
    | multi_column_for_loop
    ;

single_column_for_loop
    : FOR column_name
       ( IN '(' expressions? ')'
       | (LIKE expression)? FROM fromExpr=expression TO toExpr=expression
         action_type=(INCREMENT | DECREMENT) action_expr=expression)
    ;

multi_column_for_loop
    : FOR paren_column_list
      IN  '(' (subquery | '(' expressions? ')') ')'
    ;

unary_expression
    : ('-' | '+') unary_expression
    | PRIOR unary_expression
    | CONNECT_BY_ROOT unary_expression
    | /*TODO {input.LT(1).getText().equalsIgnoreCase("new") && !input.LT(2).getText().equals(".")}?*/ NEW unary_expression
    |  DISTINCT unary_expression
    |  ALL unary_expression
    |  /*TODO{(input.LA(1) == CASE || input.LA(2) == CASE)}?*/ case_statement/*[false]*/
    |  quantified_expression
    |  standard_function
    |  atom
    ;

case_statement /*TODO [boolean isStatementParameter]
TODO scope    {
    boolean isStatement;
}
@init    {$case_statement::isStatement = $isStatementParameter;}*/
    : searched_case_statement
    | simple_case_statement
    ;

// CASE

simple_case_statement
    : label_name? ck1=CASE expression simple_case_when_part+  case_else_part? END CASE? label_name?
    ;

simple_case_when_part
    : WHEN expression THEN (/*TODO{$case_statement::isStatement}?*/ seq_of_statements | expression)
    ;

searched_case_statement
    : label_name? ck1=CASE searched_case_when_part+ case_else_part? END CASE? label_name?
    ;

searched_case_when_part
    : WHEN expression THEN (/*TODO{$case_statement::isStatement}?*/ seq_of_statements | expression)
    ;

case_else_part
    : ELSE (/*{$case_statement::isStatement}?*/ seq_of_statements | expression)
    ;

atom
    : table_element outer_join_sign
    | bind_variable
    | constant
    | general_element
    | '(' subquery ')' subquery_operation_part*
    | '(' expressions ')'
    ;

quantified_expression
    : (SOME | EXISTS | ALL | ANY) ('(' select_only_statement ')' | '(' expression ')')
    ;

string_function
    : SUBSTR '(' expression ',' expression (',' expression)? ')'
    | TO_CHAR '(' (table_element | standard_function | expression)
                  (',' quoted_string)? (',' quoted_string)? ')'
    | DECODE '(' expressions  ')'
    | CHR '(' concatenation USING NCHAR_CS ')'
    | NVL '(' expression ',' expression ')'
    | TRIM '(' ((LEADING | TRAILING | BOTH)? quoted_string? FROM)? concatenation ')'
    | TO_DATE '(' (table_element | standard_function | expression) (',' quoted_string)? ')'
    ;

standard_function
    : string_function
    | numeric_function_wrapper
    | other_function
    ;

literal
    : CHAR_STRING
    | string_function
    | numeric
    | MAXVALUE
    ;

numeric_function_wrapper
    : numeric_function (single_column_for_loop | multi_column_for_loop)?
    ;

numeric_function
   : SUM '(' (DISTINCT | ALL)? expression ')'
   | COUNT '(' ( ASTERISK | ((DISTINCT | UNIQUE | ALL)? concatenation)? ) ')' over_clause?
   | ROUND '(' expression (',' UNSIGNED_INTEGER)?  ')'
   | AVG '(' (DISTINCT | ALL)? expression ')'
   | MAX '(' (DISTINCT | ALL)? expression ')'
   | LEAST '(' expressions ')'
   | GREATEST '(' expressions ')'
   ;

other_function
    : over_clause_keyword function_argument_analytic over_clause?
    | /*TODO stantard_function_enabling_using*/ regular_id function_argument_modeling using_clause?
    | COUNT '(' ( ASTERISK | (DISTINCT | UNIQUE | ALL)? concatenation) ')' over_clause?
    | (CAST | XMLCAST) '(' (MULTISET '(' subquery ')' | concatenation) AS type_spec ')'
    | COALESCE '(' table_element (',' (numeric | quoted_string))? ')'
    | COLLECT '(' (DISTINCT | UNIQUE)? concatenation collect_order_by_part? ')'
    | within_or_over_clause_keyword function_argument within_or_over_part+
    | cursor_name ( PERCENT_ISOPEN | PERCENT_FOUND | PERCENT_NOTFOUND | PERCENT_ROWCOUNT )
    | DECOMPOSE '(' concatenation (CANONICAL | COMPATIBILITY)? ')'
    | EXTRACT '(' regular_id FROM concatenation ')'
    | (FIRST_VALUE | LAST_VALUE) function_argument_analytic respect_or_ignore_nulls? over_clause
    | standard_prediction_function_keyword
      '(' expressions cost_matrix_clause? using_clause? ')'
    | TRANSLATE '(' expression (USING (CHAR_CS | NCHAR_CS))? (',' expression)* ')'
    | TREAT '(' expression AS REF? type_spec ')'
    | TRIM '(' ((LEADING | TRAILING | BOTH)? quoted_string? FROM)? concatenation ')'
    | XMLAGG '(' expression order_by_clause? ')' ('.' general_element_part)?
    | (XMLCOLATTVAL | XMLFOREST)
      '(' xml_multiuse_expression_element (',' xml_multiuse_expression_element)* ')' ('.' general_element_part)?
    | XMLELEMENT
      '(' (ENTITYESCAPING | NOENTITYESCAPING)? (NAME | EVALNAME)? expression
       (/*TODO{input.LT(2).getText().equalsIgnoreCase("xmlattributes")}?*/ ',' xml_attributes_clause)?
       (',' expression column_alias?)* ')' ('.' general_element_part)?
    | XMLEXISTS '(' expression xml_passing_clause? ')'
    | XMLPARSE '(' (DOCUMENT | CONTENT) concatenation WELLFORMED? ')' ('.' general_element_part)?
    | XMLPI
      '(' (NAME identifier | EVALNAME concatenation) (',' concatenation)? ')' ('.' general_element_part)?
    | XMLQUERY
      '(' concatenation xml_passing_clause? RETURNING CONTENT (NULL_ ON EMPTY)? ')' ('.' general_element_part)?
    | XMLROOT
      '(' concatenation (',' xmlroot_param_version_part)? (',' xmlroot_param_standalone_part)? ')' ('.' general_element_part)?
    | XMLSERIALIZE
      '(' (DOCUMENT | CONTENT) concatenation (AS type_spec)?
      xmlserialize_param_enconding_part? xmlserialize_param_version_part? xmlserialize_param_ident_part? ((HIDE | SHOW) DEFAULTS)? ')'
      ('.' general_element_part)?
    | xmltable
    ;

over_clause_keyword
    : AVG
    | CORR
    | LAG
    | LEAD
    | MAX
    | MEDIAN
    | MIN
    | NTILE
    | RATIO_TO_REPORT
    | ROW_NUMBER
    | SUM
    | VARIANCE
    | REGR_
    | STDDEV
    | VAR_
    | COVAR_
    ;

within_or_over_clause_keyword
    : CUME_DIST
    | DENSE_RANK
    | LISTAGG
    | PERCENT_RANK
    | PERCENTILE_CONT
    | PERCENTILE_DISC
    | RANK
    ;

standard_prediction_function_keyword
    : PREDICTION
    | PREDICTION_BOUNDS
    | PREDICTION_COST
    | PREDICTION_DETAILS
    | PREDICTION_PROBABILITY
    | PREDICTION_SET
    ;

over_clause
    : OVER '(' query_partition_clause? (order_by_clause windowing_clause?)? ')'
    ;

windowing_clause
    : windowing_type
      (BETWEEN windowing_elements AND windowing_elements | windowing_elements)
    ;

windowing_type
    : ROWS
    | RANGE
    ;

windowing_elements
    : UNBOUNDED PRECEDING
    | CURRENT ROW
    | concatenation (PRECEDING | FOLLOWING)
    ;

using_clause
    : USING (ASTERISK | using_element (',' using_element)*)
    ;

using_element
    : (IN OUT? | OUT)? select_list_elements
    ;

collect_order_by_part
    : ORDER BY concatenation
    ;

within_or_over_part
    : WITHIN GROUP '(' order_by_clause ')'
    | over_clause
    ;

cost_matrix_clause
    : COST (MODEL AUTO? | '(' cost_class_name (',' cost_class_name)* ')' VALUES '(' expressions? ')')
    ;

xml_passing_clause
    : PASSING (BY VALUE)? expression column_alias? (',' expression column_alias?)*
    ;

xml_attributes_clause
    : XMLATTRIBUTES
     '(' (ENTITYESCAPING | NOENTITYESCAPING)? (SCHEMACHECK | NOSCHEMACHECK)?
     xml_multiuse_expression_element (',' xml_multiuse_expression_element)* ')'
    ;

xml_namespaces_clause
    : XMLNAMESPACES
      '(' (concatenation column_alias)? (',' concatenation column_alias)* xml_general_default_part? ')'
    ;

xml_table_column
    : xml_column_name
      (FOR ORDINALITY | type_spec (PATH concatenation)? xml_general_default_part?)
    ;

xml_general_default_part
    : DEFAULT concatenation
    ;

xml_multiuse_expression_element
    : expression (AS (id_expression | EVALNAME concatenation))?
    ;

xmlroot_param_version_part
    : VERSION (NO VALUE | expression)
    ;

xmlroot_param_standalone_part
    : STANDALONE (YES | NO VALUE?)
    ;

xmlserialize_param_enconding_part
    : ENCODING concatenation
    ;

xmlserialize_param_version_part
    : VERSION concatenation
    ;

xmlserialize_param_ident_part
    : NO INDENT
    | INDENT (SIZE '=' concatenation)?
    ;

// SqlPlus

sql_plus_command
    : '/'
    | EXIT
    | PROMPT_MESSAGE
    | SHOW (ERR | ERRORS)
    | START_CMD
    | whenever_command
    | set_command
    ;

whenever_command
    : WHENEVER (SQLERROR | OSERROR)
         ( EXIT (SUCCESS | FAILURE | WARNING | variable_name) (COMMIT | ROLLBACK)
         | CONTINUE (COMMIT | ROLLBACK | NONE))
    ;

set_command
    : SET regular_id (CHAR_STRING | ON | OFF | /*EXACT_NUM_LIT*/numeric | regular_id)
    ;

// Common

partition_extension_clause
    : (SUBPARTITION | PARTITION) FOR? '(' expressions? ')'
    ;

column_alias
    : AS? (identifier | quoted_string)
    | AS
    ;

table_alias
    : identifier
    | quoted_string
    ;

where_clause
    : WHERE (CURRENT OF cursor_name | expression)
    ;

into_clause
    : (BULK COLLECT)? INTO (general_element | bind_variable) (',' (general_element | bind_variable))*
    ;

// Common Named Elements

xml_column_name
    : identifier
    | quoted_string
    ;

cost_class_name
    : identifier
    ;

attribute_name
    : identifier
    ;

savepoint_name
    : identifier
    ;

rollback_segment_name
    : identifier
    ;

table_var_name
    : identifier
    ;

schema_name
    : identifier
    ;

routine_name
    : identifier ('.' id_expression)* ('@' link_name)?
    ;

package_name
    : identifier
    ;

implementation_type_name
    : identifier ('.' id_expression)?
    ;

parameter_name
    : identifier
    ;

reference_model_name
    : identifier
    ;

main_model_name
    : identifier
    ;

container_tableview_name
    : identifier ('.' id_expression)?
    ;

aggregate_function_name
    : identifier ('.' id_expression)*
    ;

query_name
    : identifier
    ;

grantee_name
    : id_expression identified_by?
    ;

role_name
    : id_expression
    | CONNECT
    ;

constraint_name
    : identifier ('.' id_expression)* ('@' link_name)?
    ;

label_name
    : id_expression
    ;

type_name
    : id_expression ('.' id_expression)*
    ;

sequence_name
    : id_expression ('.' id_expression)*
    ;

exception_name
    : identifier ('.' id_expression)*
    ;

function_name
    : identifier ('.' id_expression)?
    ;

procedure_name
    : identifier ('.' id_expression)?
    ;

trigger_name
    : identifier ('.' id_expression)?
    ;

variable_name
    : (INTRODUCER char_set_name)? id_expression ('.' id_expression)?
    | bind_variable
    ;

index_name
    : identifier ('.' id_expression)?
    ;

cursor_name
    : general_element
    | bind_variable
    ;

record_name
    : identifier
    | bind_variable
    ;

collection_name
    : identifier ('.' id_expression)?
    ;

link_name
    : identifier
    ;

column_name
    : identifier ('.' id_expression)*
    ;

tableview_name
    : identifier ('.' id_expression)?
          (AT_SIGN link_name (PERIOD link_name)? | /*TODO{!(input.LA(2) == BY)}?*/ partition_extension_clause)?
    | xmltable outer_join_sign?
    ;

xmltable
    : XMLTABLE '(' (xml_namespaces_clause ',')? concatenation xml_passing_clause? (COLUMNS xml_table_column (',' xml_table_column)*)? ')' ('.' general_element_part)?
    ;

char_set_name
    : id_expression ('.' id_expression)*
    ;

synonym_name
    : identifier
    ;

// Represents a valid DB object name in DDL commands which are valid for several DB (or schema) objects.
// For instance, create synonym ... for <DB object name>, or rename <old DB object name> to <new DB object name>.
// Both are valid for sequences, tables, views, etc.
schema_object_name
    : id_expression
    ;

dir_object_name
    : id_expression
    ;

user_object_name
    : id_expression
    ;

grant_object_name
    : tableview_name
    | USER user_object_name (',' user_object_name)*
    | DIRECTORY dir_object_name
    | EDITION schema_object_name
    | MINING MODEL schema_object_name
    | JAVA (SOURCE | RESOURCE) schema_object_name
    | SQL TRANSLATION PROFILE schema_object_name
    ;

column_list
    : column_name (',' column_name)*
    ;

paren_column_list
    : LEFT_PAREN column_list RIGHT_PAREN
    ;

// PL/SQL Specs

// NOTE: In reality this applies to aggregate functions only
keep_clause
    : KEEP '(' DENSE_RANK (FIRST | LAST) order_by_clause ')' over_clause?
    ;

function_argument
    : '(' (argument (',' argument)*)? ')' keep_clause?
    ;

function_argument_analytic
    : '(' (argument respect_or_ignore_nulls? (',' argument respect_or_ignore_nulls?)*)? ')' keep_clause?
    ;

function_argument_modeling
    : '(' column_name (',' (numeric | NULL_) (',' (numeric | NULL_))?)?
      USING (tableview_name '.' ASTERISK | ASTERISK | expression column_alias? (',' expression column_alias?)*)
      ')' keep_clause?
    ;

respect_or_ignore_nulls
    : (RESPECT | IGNORE) NULLS
    ;

argument
    : (identifier '=' '>')? expression
    ;

type_spec
    : datatype
    | REF? type_name (PERCENT_ROWTYPE | PERCENT_TYPE)?
    ;

datatype
    : native_datatype_element precision_part? (WITH LOCAL? TIME ZONE | CHARACTER SET char_set_name)?
    | INTERVAL (YEAR | DAY) ('(' expression ')')? TO (MONTH | SECOND) ('(' expression ')')?
    ;

precision_part
    : '(' (numeric | ASTERISK) (',' (numeric | numeric_negative))? (CHAR | BYTE)? ')'
    ;

native_datatype_element
    : BINARY_INTEGER
    | PLS_INTEGER
    | NATURAL
    | BINARY_FLOAT
    | BINARY_DOUBLE
    | NATURALN
    | POSITIVE
    | POSITIVEN
    | SIGNTYPE
    | SIMPLE_INTEGER
    | NVARCHAR2
    | DEC
    | INTEGER
    | INT
    | NUMERIC
    | SMALLINT
    | NUMBER
    | DECIMAL
    | DOUBLE PRECISION?
    | FLOAT
    | REAL
    | NCHAR
    | LONG RAW?
    | CHAR
    | CHARACTER
    | VARCHAR2
    | VARCHAR
    | STRING
    | RAW
    | BOOLEAN
    | DATE
    | ROWID
    | UROWID
    | YEAR
    | MONTH
    | DAY
    | HOUR
    | MINUTE
    | SECOND
    | TIMEZONE_HOUR
    | TIMEZONE_MINUTE
    | TIMEZONE_REGION
    | TIMEZONE_ABBR
    | TIMESTAMP
    | TIMESTAMP_UNCONSTRAINED
    | TIMESTAMP_TZ_UNCONSTRAINED
    | TIMESTAMP_LTZ_UNCONSTRAINED
    | YMINTERVAL_UNCONSTRAINED
    | DSINTERVAL_UNCONSTRAINED
    | BFILE
    | BLOB
    | CLOB
    | NCLOB
    | MLSLABEL
    ;

bind_variable
    : (BINDVAR | ':' UNSIGNED_INTEGER)
      // Pro*C/C++ indicator variables
      (INDICATOR? (BINDVAR | ':' UNSIGNED_INTEGER))?
      ('.' general_element_part)*
    ;

general_element
    : general_element_part ('.' general_element_part)*
    ;

general_element_part
    : (INTRODUCER char_set_name)? id_expression ('.' id_expression)* ('@' link_name)? function_argument?
    ;

table_element
    : (INTRODUCER char_set_name)? id_expression ('.' id_expression)*
    ;

object_privilege
    : ALL PRIVILEGES?
    | ALTER
    | DEBUG
    | DELETE
    | EXECUTE
    | FLASHBACK ARCHIVE
    | INDEX
    | INHERIT PRIVILEGES
    | INSERT
    | KEEP SEQUENCE
    | MERGE VIEW
    | ON COMMIT REFRESH
    | QUERY REWRITE
    | READ
    | REFERENCES
    | SELECT
    | TRANSLATE SQL
    | UNDER
    | UPDATE
    | USE
    | WRITE
    ;

//Ordered by type rather than alphabetically
system_privilege
    : ALL PRIVILEGES
    | ADVISOR
    | ADMINISTER ANY? SQL TUNING SET
    | (ALTER | CREATE | DROP) ANY SQL PROFILE
    | ADMINISTER SQL MANAGEMENT OBJECT
    | CREATE ANY? CLUSTER
    | (ALTER | DROP) ANY CLUSTER
    | (CREATE | DROP) ANY CONTEXT
    | EXEMPT REDACTION POLICY
    | ALTER DATABASE
    | (ALTER | CREATE) PUBLIC? DATABASE LINK
    | DROP PUBLIC DATABASE LINK
    | DEBUG CONNECT SESSION
    | DEBUG ANY PROCEDURE
    | ANALYZE ANY DICTIONARY
    | CREATE ANY? DIMENSION
    | (ALTER | DROP) ANY DIMENSION
    | (CREATE | DROP) ANY DIRECTORY
    | (CREATE | DROP) ANY EDITION
    | FLASHBACK (ARCHIVE ADMINISTER | ANY TABLE)
    | (ALTER | CREATE | DROP) ANY INDEX
    | CREATE ANY? INDEXTYPE
    | (ALTER | DROP | EXECUTE) ANY INDEXTYPE
    | CREATE (ANY | EXTERNAL)? JOB
    | EXECUTE ANY (CLASS | PROGRAM)
    | MANAGE SCHEDULER
    | ADMINISTER KEY MANAGEMENT
    | CREATE ANY? LIBRARY
    | (ALTER | DROP | EXECUTE) ANY LIBRARY
    | LOGMINING
    | CREATE ANY? MATERIALIZED VIEW
    | (ALTER | DROP) ANY MATERIALIZED VIEW
    | GLOBAL? QUERY REWRITE
    | ON COMMIT REFRESH
    | CREATE ANY? MINING MODEL
    | (ALTER | DROP | SELECT | COMMENT) ANY MINING MODEL
    | CREATE ANY? CUBE
    | (ALTER | DROP | SELECT | UPDATE) ANY CUBE
    | CREATE ANY? MEASURE FOLDER
    | (DELETE | DROP | INSERT) ANY MEASURE FOLDER
    | CREATE ANY? CUBE DIMENSION
    | (ALTER | DELETE | DROP | INSERT | SELECT | UPDATE) ANY CUBE DIMENSION
    | CREATE ANY? CUBE BUILD PROCESS
    | (DROP | UPDATE) ANY CUBE BUILD PROCESS
    | CREATE ANY? OPERATOR
    | (ALTER | DROP | EXECUTE) ANY OPERATOR
    | (CREATE | ALTER | DROP) ANY OUTLINE
    | CREATE PLUGGABLE DATABASE
    | SET CONTAINER
    | CREATE ANY? PROCEDURE
    | (ALTER | DROP | EXECUTE) ANY PROCEDURE
    | (CREATE | ALTER | DROP ) PROFILE
    | CREATE ROLE
    | (ALTER | DROP | GRANT) ANY ROLE
    | (CREATE | ALTER | DROP) ROLLBACK SEGMENT
    | CREATE ANY? SEQUENCE
    | (ALTER | DROP | SELECT) ANY SEQUENCE
    | (ALTER | CREATE | RESTRICTED) SESSION
    | ALTER RESOURCE COST
    | CREATE ANY? SQL TRANSLATION PROFILE
    | (ALTER | DROP | USE) ANY SQL TRANSLATION PROFILE
    | TRANSLATE ANY SQL
    | CREATE ANY? SYNONYM
    | DROP ANY SYNONYM
    | (CREATE | DROP) PUBLIC SYNONYM
    | CREATE ANY? TABLE
    | (ALTER | BACKUP | COMMENT | DELETE | DROP | INSERT | LOCK | READ | SELECT | UPDATE) ANY TABLE
    | (CREATE | ALTER | DROP | MANAGE | UNLIMITED) TABLESPACE
    | CREATE ANY? TRIGGER
    | (ALTER | DROP) ANY TRIGGER
    | ADMINISTER DATABASE TRIGGER
    | CREATE ANY? TYPE
    | (ALTER | DROP | EXECUTE | UNDER) ANY TYPE
    | (CREATE | ALTER | DROP) USER
    | CREATE ANY? VIEW
    | (DROP | UNDER | MERGE) ANY VIEW
    | (ANALYZE | AUDIT) ANY
    | BECOME USER
    | CHANGE NOTIFICATION
    | EXEMPT ACCESS POLICY
    | FORCE ANY? TRANSACTION
    | GRANT ANY OBJECT? PRIVILEGE
    | INHERIT ANY PRIVILEGES
    | KEEP DATE TIME
    | KEEP SYSGUID
    | PURGE DBA_RECYCLEBIN
    | RESUMABLE
    | SELECT ANY (DICTIONARY | TRANSACTION)
    | SYSBACKUP
    | SYSDBA
    | SYSDG
    | SYSKM
    | SYSOPER
    ;

// $>

// $<Lexer Mappings

constant
    : TIMESTAMP (quoted_string | bind_variable) (AT TIME ZONE quoted_string)?
    | INTERVAL (quoted_string | bind_variable | general_element_part)
      (YEAR | MONTH | DAY | HOUR | MINUTE | SECOND)
      ('(' (UNSIGNED_INTEGER | bind_variable) (',' (UNSIGNED_INTEGER | bind_variable) )? ')')?
      (TO ( DAY | HOUR | MINUTE | SECOND ('(' (UNSIGNED_INTEGER | bind_variable) ')')?))?
    | numeric
    | DATE quoted_string
    | quoted_string
    | NULL_
    | TRUE
    | FALSE
    | DBTIMEZONE
    | SESSIONTIMEZONE
    | MINVALUE
    | MAXVALUE
    | DEFAULT
    ;

numeric
    : UNSIGNED_INTEGER
    | APPROXIMATE_NUM_LIT
    ;

numeric_negative
    : MINUS_SIGN numeric
    ;

quoted_string
    : variable_name
    | CHAR_STRING
    //| CHAR_STRING_PERL
    | NATIONAL_CHAR_STRING_LIT
    ;

identifier
    : (INTRODUCER char_set_name)? id_expression
    ;

id_expression
    : regular_id
    | DELIMITED_ID
    ;

outer_join_sign
    : '(' '+' ')'
    ;

regular_id
    : non_reserved_keywords_pre12c
    | non_reserved_keywords_in_12c
    | REGULAR_ID
    | A_LETTER
    | AGENT
    | AGGREGATE
    | ANALYZE
    | AUTONOMOUS_TRANSACTION
    | BATCH
    | BINARY_INTEGER
    | BOOLEAN
    | C_LETTER
    | CHAR
    | CLUSTER
    | CONSTRUCTOR
    | CUSTOMDATUM
    | DECIMAL
    | DELETE
    | DETERMINISTIC
    | DSINTERVAL_UNCONSTRAINED
    | ERR
    | EXCEPTION
    | EXCEPTION_INIT
    | EXCEPTIONS
    | EXISTS
    | EXIT
    | FLOAT
    | FORALL
    | INDICES
    | INOUT
    | INTEGER
    | LANGUAGE
    | LONG
    | LOOP
    | NUMBER
    | ORADATA
    | OSERROR
    | OUT
    | OVERRIDING
    | PARALLEL_ENABLE
    | PIPELINED
    | PLS_INTEGER
    | POSITIVE
    | POSITIVEN
    | PRAGMA
    | RAISE
    | RAW
    | RECORD
    | REF
    | RENAME
    | RESTRICT_REFERENCES
    | RESULT
    | SELF
    | SERIALLY_REUSABLE
    | SET
    | SIGNTYPE
    | SIMPLE_INTEGER
    | SMALLINT
    | SQLDATA
    | SQLERROR
    | SUBTYPE
    | TIMESTAMP_LTZ_UNCONSTRAINED
    | TIMESTAMP_TZ_UNCONSTRAINED
    | TIMESTAMP_UNCONSTRAINED
    | TRIGGER
    | VARCHAR
    | VARCHAR2
    | VARIABLE
    | WARNING
    | WHILE
    | XMLAGG
    | YMINTERVAL_UNCONSTRAINED
    | REGR_
    | VAR_
    | COVAR_
    ;

non_reserved_keywords_in_12c
    : ACL
    | ACTION
    | ACTIONS
    | ACTIVE
    | ACTIVE_DATA
    | ACTIVITY
    | ADAPTIVE_PLAN
    | ADVANCED
    | AFD_DISKSTRING
    | ANOMALY
    | ANSI_REARCH
    | APPLICATION
    | APPROX_COUNT_DISTINCT
    | ARCHIVAL
    | ARCHIVED
    | ASIS
    | ASSIGN
    | AUTO_LOGIN
    | AUTO_REOPTIMIZE
    | AVRO
    | BACKGROUND
    | BATCHSIZE
    | BATCH_TABLE_ACCESS_BY_ROWID
    | BEGINNING
    | BEQUEATH
    | BITMAP_AND
    | BSON
    | CACHING
    | CALCULATED
    | CALLBACK
    | CAPACITY
    | CDBDEFAULT
    | CLASSIFIER
    | CLEANUP
    | CLIENT
    | CLUSTER_DETAILS
    | CLUSTER_DISTANCE
    | CLUSTERING
    | COMMON_DATA
    | COMPONENT
    | COMPONENTS
    | CON_DBID_TO_ID
    | CONDITION
    | CONDITIONAL
    | CON_GUID_TO_ID
    | CON_ID
    | CON_NAME_TO_ID
    | CONTAINER_DATA
    | CONTAINERS
    | CON_UID_TO_ID
    | COOKIE
    | COPY
    | CREATE_FILE_DEST
    | CREDENTIAL
    | CRITICAL
    | CUBE_AJ
    | CUBE_SJ
    | DATAMOVEMENT
    | DATAOBJ_TO_MAT_PARTITION
    | DATAPUMP
    | DATA_SECURITY_REWRITE_LIMIT
    | DAYS
    | DB_UNIQUE_NAME
    | DECORRELATE
    | DEFINE
    | DELEGATE
    | DELETE_ALL
    | DESTROY
    | DIMENSIONS
    | DISABLE_ALL
    | DISABLE_PARALLEL_DML
    | DISCARD
    | DISTRIBUTE
    | DUPLICATE
    | DV
    | EDITIONABLE
    | ELIM_GROUPBY
    | EM
    | ENABLE_ALL
    | ENABLE_PARALLEL_DML
    | EQUIPART
    | EVAL
    | EVALUATE
    | EXISTING
    | EXPRESS
    | EXTRACTCLOBXML
    | FACTOR
    | FAILOVER
    | FAILURE
    | FAMILY
    | FAR
    | FASTSTART
    | FEATURE_DETAILS
    | FETCH
    | FILE_NAME_CONVERT
    | FIXED_VIEW_DATA
    | FORMAT
    | GATHER_OPTIMIZER_STATISTICS
    | GET
    | ILM
    | INACTIVE
    | INDEXING
    | INHERIT
    | INMEMORY
    | INMEMORY_PRUNING
    | INPLACE
    | INTERLEAVED
    | JSON
    | JSON_ARRAY
    | JSON_ARRAYAGG
    | JSON_EQUAL
    | JSON_EXISTS
    | JSON_EXISTS2
    | JSONGET
    | JSON_OBJECT
    | JSON_OBJECTAGG
    | JSONPARSE
    | JSON_QUERY
    | JSON_SERIALIZE
    | JSON_TABLE
    | JSON_TEXTCONTAINS
    | JSON_TEXTCONTAINS2
    | JSON_VALUE
    | KEYSTORE
    | LABEL
    | LAX
    | LIFECYCLE
    | LINEAR
    | LOCKING
    | LOGMINING
    | MAP
    | MATCH
    | MATCHES
    | MATCH_NUMBER
    | MATCH_RECOGNIZE
    | MAX_SHARED_TEMP_SIZE
    | MEMCOMPRESS
    | METADATA
    | MODEL_NB
    | MODEL_SV
    | MODIFICATION
    | MODULE
    | MONTHS
    | MULTIDIMENSIONAL
    | NEG
    | NO_ADAPTIVE_PLAN
    | NO_ANSI_REARCH
    | NO_AUTO_REOPTIMIZE
    | NO_BATCH_TABLE_ACCESS_BY_ROWID
    | NO_CLUSTERING
    | NO_COMMON_DATA
    | NOCOPY
    | NO_DATA_SECURITY_REWRITE
    | NO_DECORRELATE
    | NO_ELIM_GROUPBY
    | NO_GATHER_OPTIMIZER_STATISTICS
    | NO_INMEMORY
    | NO_INMEMORY_PRUNING
    | NOKEEP
    | NONEDITIONABLE
    | NO_OBJECT_LINK
    | NO_PARTIAL_JOIN
    | NO_PARTIAL_ROLLUP_PUSHDOWN
    | NOPARTITION
    | NO_PQ_CONCURRENT_UNION
    | NO_PQ_REPLICATE
    | NO_PQ_SKEW
    | NO_PX_FAULT_TOLERANCE
    | NORELOCATE
    | NOREPLAY
    | NO_ROOT_SW_FOR_LOCAL
    | NO_SQL_TRANSLATION
    | NO_USE_CUBE
    | NO_USE_VECTOR_AGGREGATION
    | NO_VECTOR_TRANSFORM
    | NO_VECTOR_TRANSFORM_DIMS
    | NO_VECTOR_TRANSFORM_FACT
    | NO_ZONEMAP
    | OBJ_ID
    | OFFSET
    | OLS
    | OMIT
    | ONE
    | ORA_CHECK_ACL
    | ORA_CHECK_PRIVILEGE
    | ORA_CLUSTERING
    | ORA_INVOKING_USER
    | ORA_INVOKING_USERID
    | ORA_INVOKING_XS_USER
    | ORA_INVOKING_XS_USER_GUID
    | ORA_RAWCOMPARE
    | ORA_RAWCONCAT
    | ORA_WRITE_TIME
    | PARTIAL
    | PARTIAL_JOIN
    | PARTIAL_ROLLUP_PUSHDOWN
    | PAST
    | PATCH
    | PATH_PREFIX
    | PATTERN
    | PER
    | PERIOD
    | PERIOD_KEYWORD
    | PERMUTE
    | PLUGGABLE
    | POOL_16K
    | POOL_2K
    | POOL_32K
    | POOL_4K
    | POOL_8K
    | PQ_CONCURRENT_UNION
    | PQ_DISTRIBUTE_WINDOW
    | PQ_FILTER
    | PQ_REPLICATE
    | PQ_SKEW
    | PRELOAD
    | PRETTY
    | PREV
    | PRINTBLOBTOCLOB
    | PRIORITY
    | PRIVILEGED
    | PROXY
    | PRUNING
    | PX_FAULT_TOLERANCE
    | REALM
    | REDEFINE
    | RELOCATE
    | RESTART
    | ROLESET
    | ROWID_MAPPING_TABLE
    | RUNNING
    | SAVE
    | SCRUB
    | SDO_GEOM_MBR
    | SECRET
    | SERIAL
    | SERVICE_NAME_CONVERT
    | SERVICES
    | SHARING
    | SHELFLIFE
    | SOURCE_FILE_DIRECTORY
    | SOURCE_FILE_NAME_CONVERT
    | SQL_TRANSLATION_PROFILE
    | STANDARD_HASH
    | STANDBYS
    | STATE
    | STATEMENT
    | STREAM
    | SUBSCRIBE
    | SUBSET
    | SUCCESS
    | SYSBACKUP
    | SYS_CHECK_PRIVILEGE
    | SYSDG
    | SYS_GET_COL_ACLIDS
    | SYSGUID
    | SYSKM
    | SYS_MKXTI
    | SYSOBJ
    | SYS_OP_CYCLED_SEQ
    | SYS_OP_HASH
    | SYS_OP_KEY_VECTOR_CREATE
    | SYS_OP_KEY_VECTOR_FILTER
    | SYS_OP_KEY_VECTOR_FILTER_LIST
    | SYS_OP_KEY_VECTOR_SUCCEEDED
    | SYS_OP_KEY_VECTOR_USE
    | SYS_OP_PART_ID
    | SYS_OP_ZONE_ID
    | SYS_RAW_TO_XSID
    | SYS_XSID_TO_RAW
    | SYS_ZMAP_FILTER
    | SYS_ZMAP_REFRESH
    | TAG
    | TEXT
    | TIER
    | TIES
    | TO_ACLID
    | TRANSLATION
    | TRUST
    | UCS2
    | UNCONDITIONAL
    | UNMATCHED
    | UNPLUG
    | UNSUBSCRIBE
    | USABLE
    | USE_CUBE
    | USE_HIDDEN_PARTITIONS
    | USER_DATA
    | USER_TABLESPACES
    | USE_VECTOR_AGGREGATION
    | USING_NO_EXPAND
    | UTF16BE
    | UTF16LE
    | UTF32
    | UTF8
    | V1
    | V2
    | VALID_TIME_END
    | VECTOR_TRANSFORM
    | VECTOR_TRANSFORM_DIMS
    | VECTOR_TRANSFORM_FACT
    | VERIFIER
    | VIOLATION
    | VISIBILITY
    | WEEK
    | WEEKS
    | WITH_PLSQL
    | WRAPPER
    | XS
    | YEARS
    | ZONEMAP
    ;

non_reserved_keywords_pre12c
    : ABORT
    | ABS
    | ACCESSED
    | ACCESS
    | ACCOUNT
    | ACOS
    | ACTIVATE
    | ACTIVE_COMPONENT
    | ACTIVE_FUNCTION
    | ACTIVE_TAG
    | ADD_COLUMN
    | ADD_GROUP
    | ADD_MONTHS
    | ADD
    | ADJ_DATE
    | ADMINISTER
    | ADMINISTRATOR
    | ADMIN
    | ADVISE
    | ADVISOR
    | AFTER
    | ALIAS
    | ALLOCATE
    | ALLOW
    | ALL_ROWS
    | ALWAYS
    | ANALYZE
    | ANCILLARY
    | AND_EQUAL
    | ANTIJOIN
    | ANYSCHEMA
    | APPENDCHILDXML
    | APPEND
    | APPEND_VALUES
    | APPLY
    | ARCHIVELOG
    | ARCHIVE
    | ARRAY
    | ASCII
    | ASCIISTR
    | ASIN
    | ASSEMBLY
    | ASSOCIATE
    | ASYNCHRONOUS
    | ASYNC
    | ATAN2
    | ATAN
    | AT
    | ATTRIBUTE
    | ATTRIBUTES
    | AUTHENTICATED
    | AUTHENTICATION
    | AUTHID
    | AUTHORIZATION
    | AUTOALLOCATE
    | AUTOEXTEND
    | AUTOMATIC
    | AUTO
    | AVAILABILITY
    | AVG
    | BACKUP
    | BASICFILE
    | BASIC
    | BATCH
    | BECOME
    | BEFORE
    | BEGIN
    | BEGIN_OUTLINE_DATA
    | BEHALF
    | BFILE
    | BFILENAME
    | BIGFILE
    | BINARY_DOUBLE_INFINITY
    | BINARY_DOUBLE
    | BINARY_DOUBLE_NAN
    | BINARY_FLOAT_INFINITY
    | BINARY_FLOAT
    | BINARY_FLOAT_NAN
    | BINARY
    | BIND_AWARE
    | BINDING
    | BIN_TO_NUM
    | BITAND
    | BITMAP
    | BITMAPS
    | BITMAP_TREE
    | BITS
    | BLOB
    | BLOCK
    | BLOCK_RANGE
    | BLOCKSIZE
    | BLOCKS
    | BODY
    | BOTH
    | BOUND
    | BRANCH
    | BREADTH
    | BROADCAST
    | BUFFER_CACHE
    | BUFFER
    | BUFFER_POOL
    | BUILD
    | BULK
    | BYPASS_RECURSIVE_CHECK
    | BYPASS_UJVC
    | BYTE
    | CACHE_CB
    | CACHE_INSTANCES
    | CACHE
    | CACHE_TEMP_TABLE
    | CALL
    | CANCEL
    | CARDINALITY
    | CASCADE
    | CASE
    | CAST
    | CATEGORY
    | CEIL
    | CELL_FLASH_CACHE
    | CERTIFICATE
    | CFILE
    | CHAINED
    | CHANGE_DUPKEY_ERROR_INDEX
    | CHANGE
    | CHARACTER
    | CHAR_CS
    | CHARTOROWID
    | CHECK_ACL_REWRITE
    | CHECKPOINT
    | CHILD
    | CHOOSE
    | CHR
    | CHUNK
    | CLASS
    | CLEAR
    | CLOB
    | CLONE
    | CLOSE_CACHED_OPEN_CURSORS
    | CLOSE
    | CLUSTER_BY_ROWID
    | CLUSTER_ID
    | CLUSTERING_FACTOR
    | CLUSTER_PROBABILITY
    | CLUSTER_SET
    | COALESCE
    | COALESCE_SQ
    | COARSE
    | CO_AUTH_IND
    | COLD
    | COLLECT
    | COLUMNAR
    | COLUMN_AUTH_INDICATOR
    | COLUMN
    | COLUMNS
    | COLUMN_STATS
    | COLUMN_VALUE
    | COMMENT
    | COMMIT
    | COMMITTED
    | COMPACT
    | COMPATIBILITY
    | COMPILE
    | COMPLETE
    | COMPLIANCE
    | COMPOSE
    | COMPOSITE_LIMIT
    | COMPOSITE
    | COMPOUND
    | COMPUTE
    | CONCAT
    | CONFIRM
    | CONFORMING
    | CONNECT_BY_CB_WHR_ONLY
    | CONNECT_BY_COMBINE_SW
    | CONNECT_BY_COST_BASED
    | CONNECT_BY_ELIM_DUPS
    | CONNECT_BY_FILTERING
    | CONNECT_BY_ISCYCLE
    | CONNECT_BY_ISLEAF
    | CONNECT_BY_ROOT
    | CONNECT_TIME
    | CONSIDER
    | CONSISTENT
    | CONSTANT
    | CONST
    | CONSTRAINT
    | CONSTRAINTS
    | CONTAINER
    | CONTENT
    | CONTENTS
    | CONTEXT
    | CONTINUE
    | CONTROLFILE
    | CONVERT
    | CORR_K
    | CORR
    | CORR_S
    | CORRUPTION
    | CORRUPT_XID_ALL
    | CORRUPT_XID
    | COSH
    | COS
    | COST
    | COST_XML_QUERY_REWRITE
    | COUNT
    | COVAR_POP
    | COVAR_SAMP
    | CPU_COSTING
    | CPU_PER_CALL
    | CPU_PER_SESSION
    | CRASH
    | CREATE_STORED_OUTLINES
    | CREATION
    | CROSSEDITION
    | CROSS
    | CSCONVERT
    | CUBE_GB
    | CUBE
    | CUME_DISTM
    | CUME_DIST
    | CURRENT_DATE
    | CURRENT
    | CURRENT_SCHEMA
    | CURRENT_TIME
    | CURRENT_TIMESTAMP
    | CURRENT_USER
    | CURRENTV
    | CURSOR
    | CURSOR_SHARING_EXACT
    | CURSOR_SPECIFIC_SEGMENT
    | CV
    | CYCLE
    | DANGLING
    | DATABASE
    | DATAFILE
    | DATAFILES
    | DATA
    | DATAOBJNO
    | DATAOBJ_TO_PARTITION
    | DATE_MODE
    | DAY
    | DBA
    | DBA_RECYCLEBIN
    | DBMS_STATS
    | DB_ROLE_CHANGE
    | DBTIMEZONE
    | DB_VERSION
    | DDL
    | DEALLOCATE
    | DEBUGGER
    | DEBUG
    | DECLARE
    | DEC
    | DECOMPOSE
    | DECREMENT
    | DECR
    | DECRYPT
    | DEDUPLICATE
    | DEFAULTS
    | DEFERRABLE
    | DEFERRED
    | DEFINED
    | DEFINER
    | DEGREE
    | DELAY
    | DELETEXML
    | DEMAND
    | DENSE_RANKM
    | DENSE_RANK
    | DEPENDENT
    | DEPTH
    | DEQUEUE
    | DEREF
    | DEREF_NO_REWRITE
    | DETACHED
    | DETERMINES
    | DICTIONARY
    | DIMENSION
    | DIRECT_LOAD
    | DIRECTORY
    | DIRECT_PATH
    | DISABLE
    | DISABLE_PRESET
    | DISABLE_RPKE
    | DISALLOW
    | DISASSOCIATE
    | DISCONNECT
    | DISKGROUP
    | DISK
    | DISKS
    | DISMOUNT
    | DISTINGUISHED
    | DISTRIBUTED
    | DML
    | DML_UPDATE
    | DOCFIDELITY
    | DOCUMENT
    | DOMAIN_INDEX_FILTER
    | DOMAIN_INDEX_NO_SORT
    | DOMAIN_INDEX_SORT
    | DOUBLE
    | DOWNGRADE
    | DRIVING_SITE
    | DROP_COLUMN
    | DROP_GROUP
    | DST_UPGRADE_INSERT_CONV
    | DUMP
    | DYNAMIC
    | DYNAMIC_SAMPLING_EST_CDN
    | DYNAMIC_SAMPLING
    | EACH
    | EDITIONING
    | EDITION
    | EDITIONS
    | ELEMENT
    | ELIMINATE_JOIN
    | ELIMINATE_OBY
    | ELIMINATE_OUTER_JOIN
    | EMPTY_BLOB
    | EMPTY_CLOB
    | EMPTY
    | ENABLE
    | ENABLE_PRESET
    | ENCODING
    | ENCRYPTION
    | ENCRYPT
    | END_OUTLINE_DATA
    | ENFORCED
    | ENFORCE
    | ENQUEUE
    | ENTERPRISE
    | ENTITYESCAPING
    | ENTRY
    | ERROR_ARGUMENT
    | ERROR
    | ERROR_ON_OVERLAP_TIME
    | ERRORS
    | ESCAPE
    | ESTIMATE
    | EVALNAME
    | EVALUATION
    | EVENTS
    | EVERY
    | EXCEPTIONS
    | EXCEPT
    | EXCHANGE
    | EXCLUDE
    | EXCLUDING
    | EXECUTE
    | EXEMPT
    | EXISTSNODE
    | EXPAND_GSET_TO_UNION
    | EXPAND_TABLE
    | EXPIRE
    | EXPLAIN
    | EXPLOSION
    | EXP
    | EXPORT
    | EXPR_CORR_CHECK
    | EXTENDS
    | EXTENT
    | EXTENTS
    | EXTERNALLY
    | EXTERNAL
    | EXTRACT
    | EXTRACTVALUE
    | EXTRA
    | FACILITY
    | FACT
    | FACTORIZE_JOIN
    | FAILED_LOGIN_ATTEMPTS
    | FAILED
    | FAILGROUP
    | FALSE
    | FAST
    | FBTSCAN
    | FEATURE_ID
    | FEATURE_SET
    | FEATURE_VALUE
    | FILE
    | FILESYSTEM_LIKE_LOGGING
    | FILTER
    | FINAL
    | FINE
    | FINISH
    | FIRSTM
    | FIRST
    | FIRST_ROWS
    | FIRST_VALUE
    | FLAGGER
    | FLASHBACK
    | FLASH_CACHE
    | FLOB
    | FLOOR
    | FLUSH
    | FOLDER
    | FOLLOWING
    | FOLLOWS
    | FORCE
    | FORCE_XML_QUERY_REWRITE
    | FOREIGN
    | FOREVER
    | FORWARD
    | FRAGMENT_NUMBER
    | FREELIST
    | FREELISTS
    | FREEPOOLS
    | FRESH
    | FROM_TZ
    | FULL
    | FULL_OUTER_JOIN_TO_OUTER
    | FUNCTION
    | FUNCTIONS
    | GATHER_PLAN_STATISTICS
    | GBY_CONC_ROLLUP
    | GBY_PUSHDOWN
    | GENERATED
    | GLOBALLY
    | GLOBAL
    | GLOBAL_NAME
    | GLOBAL_TOPIC_ENABLED
    | GREATEST
    | GROUP_BY
    | GROUP_ID
    | GROUPING_ID
    | GROUPING
    | GROUPS
    | GUARANTEED
    | GUARANTEE
    | GUARD
    | HASH_AJ
    | HASHKEYS
    | HASH
    | HASH_SJ
    | HEADER
    | HEAP
    | HELP
    | HEXTORAW
    | HEXTOREF
    | HIDDEN_KEYWORD
    | HIDE
    | HIERARCHY
    | HIGH
    | HINTSET_BEGIN
    | HINTSET_END
    | HOT
    | HOUR
    | HWM_BROKERED
    | HYBRID
    | IDENTIFIER
    | IDENTITY
    | IDGENERATORS
    | IDLE_TIME
    | ID
    | IF
    | IGNORE
    | IGNORE_OPTIM_EMBEDDED_HINTS
    | IGNORE_ROW_ON_DUPKEY_INDEX
    | IGNORE_WHERE_CLAUSE
    | IMMEDIATE
    | IMPACT
    | IMPORT
    | INCLUDE
    | INCLUDE_VERSION
    | INCLUDING
    | INCREMENTAL
    | INCREMENT
    | INCR
    | INDENT
    | INDEX_ASC
    | INDEX_COMBINE
    | INDEX_DESC
    | INDEXED
    | INDEXES
    | INDEX_FFS
    | INDEX_FILTER
    | INDEX_JOIN
    | INDEX_ROWS
    | INDEX_RRS
    | INDEX_RS_ASC
    | INDEX_RS_DESC
    | INDEX_RS
    | INDEX_SCAN
    | INDEX_SKIP_SCAN
    | INDEX_SS_ASC
    | INDEX_SS_DESC
    | INDEX_SS
    | INDEX_STATS
    | INDEXTYPE
    | INDEXTYPES
    | INDICATOR
    | INFINITE
    | INFORMATIONAL
    | INITCAP
    | INITIALIZED
    | INITIALLY
    | INITIAL
    | INITRANS
    | INLINE
    | INLINE_XMLTYPE_NT
    | IN_MEMORY_METADATA
    | INNER
    | INSERTCHILDXMLAFTER
    | INSERTCHILDXMLBEFORE
    | INSERTCHILDXML
    | INSERTXMLAFTER
    | INSERTXMLBEFORE
    | INSTANCE
    | INSTANCES
    | INSTANTIABLE
    | INSTANTLY
    | INSTEAD
    | INSTR2
    | INSTR4
    | INSTRB
    | INSTRC
    | INSTR
    | INTERMEDIATE
    | INTERNAL_CONVERT
    | INTERNAL_USE
    | INTERPRETED
    | INTERVAL
    | INT
    | INVALIDATE
    | INVISIBLE
    | IN_XQUERY
    | ISOLATION_LEVEL
    | ISOLATION
    | ITERATE
    | ITERATION_NUMBER
    | JAVA
    | JOB
    | JOIN
    | KEEP_DUPLICATES
    | KEEP
    | KERBEROS
    | KEY_LENGTH
    | KEY
    | KEYSIZE
    | KEYS
    | KILL
    | LAG
    | LAST_DAY
    | LAST
    | LAST_VALUE
    | LATERAL
    | LAYER
    | LDAP_REGISTRATION_ENABLED
    | LDAP_REGISTRATION
    | LDAP_REG_SYNC_INTERVAL
    | LEADING
    | LEAD
    | LEAST
    | LEFT
    | LENGTH2
    | LENGTH4
    | LENGTHB
    | LENGTHC
    | LENGTH
    | LESS
    | LEVEL
    | LEVELS
    | LIBRARY
    | LIFE
    | LIFETIME
    | LIKE2
    | LIKE4
    | LIKEC
    | LIKE_EXPAND
    | LIMIT
    | LINK
    | LISTAGG
    | LIST
    | LN
    | LNNVL
    | LOAD
    | LOB
    | LOBNVL
    | LOBS
    | LOCAL_INDEXES
    | LOCAL
    | LOCALTIME
    | LOCALTIMESTAMP
    | LOCATION
    | LOCATOR
    | LOCKED
    | LOGFILE
    | LOGFILES
    | LOGGING
    | LOGICAL
    | LOGICAL_READS_PER_CALL
    | LOGICAL_READS_PER_SESSION
    | LOG
    | LOGOFF
    | LOGON
    | LOG_READ_ONLY_VIOLATIONS
    | LOWER
    | LOW
    | LPAD
    | LTRIM
    | MAIN
    | MAKE_REF
    | MANAGED
    | MANAGEMENT
    | MANAGE
    | MANAGER
    | MANUAL
    | MAPPING
    | MASTER
    | MATCHED
    | MATERIALIZED
    | MATERIALIZE
    | MAXARCHLOGS
    | MAXDATAFILES
    | MAXEXTENTS
    | MAXIMIZE
    | MAXINSTANCES
    | MAXLOGFILES
    | MAXLOGHISTORY
    | MAXLOGMEMBERS
    | MAX
    | MAXSIZE
    | MAXTRANS
    | MAXVALUE
    | MEASURE
    | MEASURES
    | MEDIAN
    | MEDIUM
    | MEMBER
    | MEMORY
    | MERGEACTIONS
    | MERGE_AJ
    | MERGE_CONST_ON
    | MERGE
    | MERGE_SJ
    | METHOD
    | MIGRATE
    | MIGRATION
    | MINEXTENTS
    | MINIMIZE
    | MINIMUM
    | MINING
    | MIN
    | MINUS_NULL
    | MINUTE
    | MINVALUE
    | MIRRORCOLD
    | MIRRORHOT
    | MIRROR
    | MLSLABEL
    | MODEL_COMPILE_SUBQUERY
    | MODEL_DONTVERIFY_UNIQUENESS
    | MODEL_DYNAMIC_SUBQUERY
    | MODEL_MIN_ANALYSIS
    | MODEL
    | MODEL_NO_ANALYSIS
    | MODEL_PBY
    | MODEL_PUSH_REF
    | MODIFY_COLUMN_TYPE
    | MODIFY
    | MOD
    | MONITORING
    | MONITOR
    | MONTH
    | MONTHS_BETWEEN
    | MOUNT
    | MOUNTPATH
    | MOVEMENT
    | MOVE
    | MULTISET
    | MV_MERGE
    | NAMED
    | NAME
    | NAMESPACE
    | NAN
    | NANVL
    | NATIONAL
    | NATIVE_FULL_OUTER_JOIN
    | NATIVE
    | NATURAL
    | NAV
    | NCHAR_CS
    | NCHAR
    | NCHR
    | NCLOB
    | NEEDED
    | NESTED
    | NESTED_TABLE_FAST_INSERT
    | NESTED_TABLE_GET_REFS
    | NESTED_TABLE_ID
    | NESTED_TABLE_SET_REFS
    | NESTED_TABLE_SET_SETID
    | NETWORK
    | NEVER
    | NEW
    | NEW_TIME
    | NEXT_DAY
    | NEXT
    | NL_AJ
    | NLJ_BATCHING
    | NLJ_INDEX_FILTER
    | NLJ_INDEX_SCAN
    | NLJ_PREFETCH
    | NLS_CALENDAR
    | NLS_CHARACTERSET
    | NLS_CHARSET_DECL_LEN
    | NLS_CHARSET_ID
    | NLS_CHARSET_NAME
    | NLS_COMP
    | NLS_CURRENCY
    | NLS_DATE_FORMAT
    | NLS_DATE_LANGUAGE
    | NLS_INITCAP
    | NLS_ISO_CURRENCY
    | NL_SJ
    | NLS_LANG
    | NLS_LANGUAGE
    | NLS_LENGTH_SEMANTICS
    | NLS_LOWER
    | NLS_NCHAR_CONV_EXCP
    | NLS_NUMERIC_CHARACTERS
    | NLS_SORT
    | NLSSORT
    | NLS_SPECIAL_CHARS
    | NLS_TERRITORY
    | NLS_UPPER
    | NO_ACCESS
    | NOAPPEND
    | NOARCHIVELOG
    | NOAUDIT
    | NO_BASETABLE_MULTIMV_REWRITE
    | NO_BIND_AWARE
    | NO_BUFFER
    | NOCACHE
    | NO_CARTESIAN
    | NO_CHECK_ACL_REWRITE
    | NO_CLUSTER_BY_ROWID
    | NO_COALESCE_SQ
    | NO_CONNECT_BY_CB_WHR_ONLY
    | NO_CONNECT_BY_COMBINE_SW
    | NO_CONNECT_BY_COST_BASED
    | NO_CONNECT_BY_ELIM_DUPS
    | NO_CONNECT_BY_FILTERING
    | NO_COST_XML_QUERY_REWRITE
    | NO_CPU_COSTING
    | NOCPU_COSTING
    | NOCYCLE
    | NODELAY
    | NO_DOMAIN_INDEX_FILTER
    | NO_DST_UPGRADE_INSERT_CONV
    | NO_ELIMINATE_JOIN
    | NO_ELIMINATE_OBY
    | NO_ELIMINATE_OUTER_JOIN
    | NOENTITYESCAPING
    | NO_EXPAND_GSET_TO_UNION
    | NO_EXPAND
    | NO_EXPAND_TABLE
    | NO_FACT
    | NO_FACTORIZE_JOIN
    | NO_FILTERING
    | NOFORCE
    | NO_FULL_OUTER_JOIN_TO_OUTER
    | NO_GBY_PUSHDOWN
    | NOGUARANTEE
    | NO_INDEX_FFS
    | NO_INDEX
    | NO_INDEX_SS
    | NO_LOAD
    | NOLOCAL
    | NOLOGGING
    | NOMAPPING
    | NOMAXVALUE
    | NO_MERGE
    | NOMINIMIZE
    | NOMINVALUE
    | NO_MODEL_PUSH_REF
    | NO_MONITORING
    | NOMONITORING
    | NO_MONITOR
    | NO_MULTIMV_REWRITE
    | NO
    | NO_NATIVE_FULL_OUTER_JOIN
    | NONBLOCKING
    | NONE
    | NO_NLJ_BATCHING
    | NO_NLJ_PREFETCH
    | NONSCHEMA
    | NOORDER
    | NO_ORDER_ROLLUPS
    | NO_OUTER_JOIN_TO_ANTI
    | NO_OUTER_JOIN_TO_INNER
    | NOOVERRIDE
    | NO_PARALLEL_INDEX
    | NOPARALLEL_INDEX
    | NO_PARALLEL
    | NOPARALLEL
    | NO_PARTIAL_COMMIT
    | NO_PLACE_DISTINCT
    | NO_PLACE_GROUP_BY
    | NO_PQ_MAP
    | NO_PRUNE_GSETS
    | NO_PULL_PRED
    | NO_PUSH_PRED
    | NO_PUSH_SUBQ
    | NO_PX_JOIN_FILTER
    | NO_QKN_BUFF
    | NO_QUERY_TRANSFORMATION
    | NO_REF_CASCADE
    | NORELY
    | NOREPAIR
    | NORESETLOGS
    | NO_RESULT_CACHE
    | NOREVERSE
    | NO_REWRITE
    | NOREWRITE
    | NORMAL
    | NOROWDEPENDENCIES
    | NOSCHEMACHECK
    | NOSEGMENT
    | NO_SEMIJOIN
    | NO_SEMI_TO_INNER
    | NO_SET_TO_JOIN
    | NOSORT
    | NO_SQL_TUNE
    | NO_STAR_TRANSFORMATION
    | NO_STATEMENT_QUEUING
    | NO_STATS_GSETS
    | NOSTRICT
    | NO_SUBQUERY_PRUNING
    | NO_SUBSTRB_PAD
    | NO_SWAP_JOIN_INPUTS
    | NOSWITCH
    | NO_TABLE_LOOKUP_BY_NL
    | NO_TEMP_TABLE
    | NOTHING
    | NOTIFICATION
    | NO_TRANSFORM_DISTINCT_AGG
    | NO_UNNEST
    | NO_USE_HASH_AGGREGATION
    | NO_USE_HASH_GBY_FOR_PUSHDOWN
    | NO_USE_HASH
    | NO_USE_INVISIBLE_INDEXES
    | NO_USE_MERGE
    | NO_USE_NL
    | NOVALIDATE
    | NO_XDB_FASTPATH_INSERT
    | NO_XML_DML_REWRITE
    | NO_XMLINDEX_REWRITE_IN_SELECT
    | NO_XMLINDEX_REWRITE
    | NO_XML_QUERY_REWRITE
    | NTH_VALUE
    | NTILE
    | NULLIF
    | NULLS
    | NUMERIC
    | NUM_INDEX_KEYS
    | NUMTODSINTERVAL
    | NUMTOYMINTERVAL
    | NVARCHAR2
    | NVL2
    | NVL
    | OBJECT2XML
    | OBJECT
    | OBJNO
    | OBJNO_REUSE
    | OCCURENCES
    | OFFLINE
    | OFF
    | OIDINDEX
    | OID
    | OLAP
    | OLD
    | OLD_PUSH_PRED
    | OLTP
    | ONLINE
    | ONLY
    | OPAQUE
    | OPAQUE_TRANSFORM
    | OPAQUE_XCANONICAL
    | OPCODE
    | OPEN
    | OPERATIONS
    | OPERATOR
    | OPT_ESTIMATE
    | OPTIMAL
    | OPTIMIZE
    | OPTIMIZER_FEATURES_ENABLE
    | OPTIMIZER_GOAL
    | OPT_PARAM
    | ORA_BRANCH
    | ORADEBUG
    | ORA_DST_AFFECTED
    | ORA_DST_CONVERT
    | ORA_DST_ERROR
    | ORA_GET_ACLIDS
    | ORA_GET_PRIVILEGES
    | ORA_HASH
    | ORA_ROWSCN
    | ORA_ROWSCN_RAW
    | ORA_ROWVERSION
    | ORA_TABVERSION
    | ORDERED
    | ORDERED_PREDICATES
    | ORDINALITY
    | OR_EXPAND
    | ORGANIZATION
    | OR_PREDICATES
    | OTHER
    | OUTER_JOIN_TO_ANTI
    | OUTER_JOIN_TO_INNER
    | OUTER
    | OUTLINE_LEAF
    | OUTLINE
    | OUT_OF_LINE
    | OVERFLOW
    | OVERFLOW_NOMOVE
    | OVERLAPS
    | OVER
    | OWNER
    | OWNERSHIP
    | OWN
    | PACKAGE
    | PACKAGES
    | PARALLEL_INDEX
    | PARALLEL
    | PARAMETERS
    | PARAM
    | PARENT
    | PARITY
    | PARTIALLY
    | PARTITION_HASH
    | PARTITION_LIST
    | PARTITION
    | PARTITION_RANGE
    | PARTITIONS
    | PARTNUMINST
    | PASSING
    | PASSWORD_GRACE_TIME
    | PASSWORD_LIFE_TIME
    | PASSWORD_LOCK_TIME
    | PASSWORD
    | PASSWORD_REUSE_MAX
    | PASSWORD_REUSE_TIME
    | PASSWORD_VERIFY_FUNCTION
    | PATH
    | PATHS
    | PBL_HS_BEGIN
    | PBL_HS_END
    | PCTINCREASE
    | PCTTHRESHOLD
    | PCTUSED
    | PCTVERSION
    | PENDING
    | PERCENTILE_CONT
    | PERCENTILE_DISC
    | PERCENT_KEYWORD
    | PERCENT_RANKM
    | PERCENT_RANK
    | PERFORMANCE
    | PERMANENT
    | PERMISSION
    | PFILE
    | PHYSICAL
    | PIKEY
    | PIV_GB
    | PIVOT
    | PIV_SSF
    | PLACE_DISTINCT
    | PLACE_GROUP_BY
    | PLAN
    | PLSCOPE_SETTINGS
    | PLSQL_CCFLAGS
    | PLSQL_CODE_TYPE
    | PLSQL_DEBUG
    | PLSQL_OPTIMIZE_LEVEL
    | PLSQL_WARNINGS
    | POINT
    | POLICY
    | POST_TRANSACTION
    | POWERMULTISET_BY_CARDINALITY
    | POWERMULTISET
    | POWER
    | PQ_DISTRIBUTE
    | PQ_MAP
    | PQ_NOMAP
    | PREBUILT
    | PRECEDES
    | PRECEDING
    | PRECISION
    | PRECOMPUTE_SUBQUERY
    | PREDICATE_REORDERS
    | PREDICTION_BOUNDS
    | PREDICTION_COST
    | PREDICTION_DETAILS
    | PREDICTION
    | PREDICTION_PROBABILITY
    | PREDICTION_SET
    | PREPARE
    | PRESENT
    | PRESENTNNV
    | PRESENTV
    | PRESERVE
    | PRESERVE_OID
    | PREVIOUS
    | PRIMARY
    | PRIVATE
    | PRIVATE_SGA
    | PRIVILEGE
    | PRIVILEGES
    | PROCEDURAL
    | PROCEDURE
    | PROCESS
    | PROFILE
    | PROGRAM
    | PROJECT
    | PROPAGATE
    | PROTECTED
    | PROTECTION
    | PULL_PRED
    | PURGE
    | PUSH_PRED
    | PUSH_SUBQ
    | PX_GRANULE
    | PX_JOIN_FILTER
    | QB_NAME
    | QUERY_BLOCK
    | QUERY
    | QUEUE_CURR
    | QUEUE
    | QUEUE_ROWP
    | QUIESCE
    | QUORUM
    | QUOTA
    | RANDOM_LOCAL
    | RANDOM
    | RANGE
    | RANKM
    | RANK
    | RAPIDLY
    | RATIO_TO_REPORT
    | RAWTOHEX
    | RAWTONHEX
    | RBA
    | RBO_OUTLINE
    | RDBA
    | READ
    | READS
    | REAL
    | REBALANCE
    | REBUILD
    | RECORDS_PER_BLOCK
    | RECOVERABLE
    | RECOVER
    | RECOVERY
    | RECYCLEBIN
    | RECYCLE
    | REDACTION
    | REDO
    | REDUCED
    | REDUNDANCY
    | REF_CASCADE_CURSOR
    | REFERENCED
    | REFERENCE
    | REFERENCES
    | REFERENCING
    | REF
    | REFRESH
    | REFTOHEX
    | REGEXP_COUNT
    | REGEXP_INSTR
    | REGEXP_LIKE
    | REGEXP_REPLACE
    | REGEXP_SUBSTR
    | REGISTER
    | REGR_AVGX
    | REGR_AVGY
    | REGR_COUNT
    | REGR_INTERCEPT
    | REGR_R2
    | REGR_SLOPE
    | REGR_SXX
    | REGR_SXY
    | REGR_SYY
    | REGULAR
    | REJECT
    | REKEY
    | RELATIONAL
    | RELY
    | REMAINDER
    | REMOTE_MAPPED
    | REMOVE
    | REPAIR
    | REPEAT
    | REPLACE
    | REPLICATION
    | REQUIRED
    | RESETLOGS
    | RESET
    | RESIZE
    | RESOLVE
    | RESOLVER
    | RESPECT
    | RESTORE_AS_INTERVALS
    | RESTORE
    | RESTRICT_ALL_REF_CONS
    | RESTRICTED
    | RESTRICT
    | RESULT_CACHE
    | RESUMABLE
    | RESUME
    | RETENTION
    | RETRY_ON_ROW_CHANGE
    | RETURNING
    | RETURN
    | REUSE
    | REVERSE
    | REWRITE
    | REWRITE_OR_ERROR
    | RIGHT
    | ROLE
    | ROLES
    | ROLLBACK
    | ROLLING
    | ROLLUP
    | ROUND
    | ROWDEPENDENCIES
    | ROWID
    | ROWIDTOCHAR
    | ROWIDTONCHAR
    | ROW_LENGTH
    | ROW
    | ROW_NUMBER
    | ROWNUM
    | ROWS
    | RPAD
    | RTRIM
    | RULE
    | RULES
    | SALT
    | SAMPLE
    | SAVE_AS_INTERVALS
    | SAVEPOINT
    | SB4
    | SCALE
    | SCALE_ROWS
    | SCAN_INSTANCES
    | SCAN
    | SCHEDULER
    | SCHEMACHECK
    | SCHEMA
    | SCN_ASCENDING
    | SCN
    | SCOPE
    | SD_ALL
    | SD_INHIBIT
    | SD_SHOW
    | SEARCH
    | SECOND
    | SECUREFILE_DBA
    | SECUREFILE
    | SECURITY
    | SEED
    | SEG_BLOCK
    | SEG_FILE
    | SEGMENT
    | SELECTIVITY
    | SEMIJOIN_DRIVER
    | SEMIJOIN
    | SEMI_TO_INNER
    | SEQUENCED
    | SEQUENCE
    | SEQUENTIAL
    | SERIALIZABLE
    | SERVERERROR
    | SESSION_CACHED_CURSORS
    | SESSION
    | SESSIONS_PER_USER
    | SESSIONTIMEZONE
    | SESSIONTZNAME
    | SETS
    | SETTINGS
    | SET_TO_JOIN
    | SEVERE
    | SHARED
    | SHARED_POOL
    | SHOW
    | SHRINK
    | SHUTDOWN
    | SIBLINGS
    | SID
    | SIGNAL_COMPONENT
    | SIGNAL_FUNCTION
    | SIGN
    | SIMPLE
    | SINGLE
    | SINGLETASK
    | SINH
    | SIN
    | SKIP_EXT_OPTIMIZER
    | SKIP_
    | SKIP_UNQ_UNUSABLE_IDX
    | SKIP_UNUSABLE_INDEXES
    | SMALLFILE
    | SNAPSHOT
    | SOME
    | SORT
    | SOUNDEX
    | SOURCE
    | SPACE_KEYWORD
    | SPECIFICATION
    | SPFILE
    | SPLIT
    | SPREADSHEET
    | SQLLDR
    | SQL
    | SQL_TRACE
    | SQRT
    | STALE
    | STANDALONE
    | STANDBY_MAX_DATA_DELAY
    | STANDBY
    | STAR
    | STAR_TRANSFORMATION
    | STARTUP
    | STATEMENT_ID
    | STATEMENT_QUEUING
    | STATEMENTS
    | STATIC
    | STATISTICS
    | STATS_BINOMIAL_TEST
    | STATS_CROSSTAB
    | STATS_F_TEST
    | STATS_KS_TEST
    | STATS_MODE
    | STATS_MW_TEST
    | STATS_ONE_WAY_ANOVA
    | STATS_T_TEST_INDEP
    | STATS_T_TEST_INDEPU
    | STATS_T_TEST_ONE
    | STATS_T_TEST_PAIRED
    | STATS_WSR_TEST
    | STDDEV
    | STDDEV_POP
    | STDDEV_SAMP
    | STOP
    | STORAGE
    | STORE
    | STREAMS
    | STRICT
    | STRING
    | STRIPE_COLUMNS
    | STRIPE_WIDTH
    | STRIP
    | STRUCTURE
    | SUBMULTISET
    | SUBPARTITION
    | SUBPARTITION_REL
    | SUBPARTITIONS
    | SUBQUERIES
    | SUBQUERY_PRUNING
    | SUBSTITUTABLE
    | SUBSTR2
    | SUBSTR4
    | SUBSTRB
    | SUBSTRC
    | SUBSTR
    | SUCCESSFUL
    | SUMMARY
    | SUM
    | SUPPLEMENTAL
    | SUSPEND
    | SWAP_JOIN_INPUTS
    | SWITCH
    | SWITCHOVER
    | SYNCHRONOUS
    | SYNC
    | SYSASM
    | SYS_AUDIT
    | SYSAUX
    | SYS_CHECKACL
    | SYS_CONNECT_BY_PATH
    | SYS_CONTEXT
    | SYSDATE
    | SYSDBA
    | SYS_DBURIGEN
    | SYS_DL_CURSOR
    | SYS_DM_RXFORM_CHR
    | SYS_DM_RXFORM_NUM
    | SYS_DOM_COMPARE
    | SYS_DST_PRIM2SEC
    | SYS_DST_SEC2PRIM
    | SYS_ET_BFILE_TO_RAW
    | SYS_ET_BLOB_TO_IMAGE
    | SYS_ET_IMAGE_TO_BLOB
    | SYS_ET_RAW_TO_BFILE
    | SYS_EXTPDTXT
    | SYS_EXTRACT_UTC
    | SYS_FBT_INSDEL
    | SYS_FILTER_ACLS
    | SYS_FNMATCHES
    | SYS_FNREPLACE
    | SYS_GET_ACLIDS
    | SYS_GET_PRIVILEGES
    | SYS_GETTOKENID
    | SYS_GETXTIVAL
    | SYS_GUID
    | SYS_MAKEXML
    | SYS_MAKE_XMLNODEID
    | SYS_MKXMLATTR
    | SYS_OP_ADT2BIN
    | SYS_OP_ADTCONS
    | SYS_OP_ALSCRVAL
    | SYS_OP_ATG
    | SYS_OP_BIN2ADT
    | SYS_OP_BITVEC
    | SYS_OP_BL2R
    | SYS_OP_BLOOM_FILTER_LIST
    | SYS_OP_BLOOM_FILTER
    | SYS_OP_C2C
    | SYS_OP_CAST
    | SYS_OP_CEG
    | SYS_OP_CL2C
    | SYS_OP_COMBINED_HASH
    | SYS_OP_COMP
    | SYS_OP_CONVERT
    | SYS_OP_COUNTCHG
    | SYS_OP_CSCONV
    | SYS_OP_CSCONVTEST
    | SYS_OP_CSR
    | SYS_OP_CSX_PATCH
    | SYS_OP_DECOMP
    | SYS_OP_DESCEND
    | SYS_OP_DISTINCT
    | SYS_OP_DRA
    | SYS_OP_DUMP
    | SYS_OP_DV_CHECK
    | SYS_OP_ENFORCE_NOT_NULL
    | SYSOPER
    | SYS_OP_EXTRACT
    | SYS_OP_GROUPING
    | SYS_OP_GUID
    | SYS_OP_IIX
    | SYS_OP_ITR
    | SYS_OP_LBID
    | SYS_OP_LOBLOC2BLOB
    | SYS_OP_LOBLOC2CLOB
    | SYS_OP_LOBLOC2ID
    | SYS_OP_LOBLOC2NCLOB
    | SYS_OP_LOBLOC2TYP
    | SYS_OP_LSVI
    | SYS_OP_LVL
    | SYS_OP_MAKEOID
    | SYS_OP_MAP_NONNULL
    | SYS_OP_MSR
    | SYS_OP_NICOMBINE
    | SYS_OP_NIEXTRACT
    | SYS_OP_NII
    | SYS_OP_NIX
    | SYS_OP_NOEXPAND
    | SYS_OP_NTCIMG
    | SYS_OP_NUMTORAW
    | SYS_OP_OIDVALUE
    | SYS_OP_OPNSIZE
    | SYS_OP_PAR_1
    | SYS_OP_PARGID_1
    | SYS_OP_PARGID
    | SYS_OP_PAR
    | SYS_OP_PIVOT
    | SYS_OP_R2O
    | SYS_OP_RAWTONUM
    | SYS_OP_RDTM
    | SYS_OP_REF
    | SYS_OP_RMTD
    | SYS_OP_ROWIDTOOBJ
    | SYS_OP_RPB
    | SYS_OPTLOBPRBSC
    | SYS_OP_TOSETID
    | SYS_OP_TPR
    | SYS_OP_TRTB
    | SYS_OPTXICMP
    | SYS_OPTXQCASTASNQ
    | SYS_OP_UNDESCEND
    | SYS_OP_VECAND
    | SYS_OP_VECBIT
    | SYS_OP_VECOR
    | SYS_OP_VECXOR
    | SYS_OP_VERSION
    | SYS_OP_VREF
    | SYS_OP_VVD
    | SYS_OP_XMLCONS_FOR_CSX
    | SYS_OP_XPTHATG
    | SYS_OP_XPTHIDX
    | SYS_OP_XPTHOP
    | SYS_OP_XTXT2SQLT
    | SYS_ORDERKEY_DEPTH
    | SYS_ORDERKEY_MAXCHILD
    | SYS_ORDERKEY_PARENT
    | SYS_PARALLEL_TXN
    | SYS_PATHID_IS_ATTR
    | SYS_PATHID_IS_NMSPC
    | SYS_PATHID_LASTNAME
    | SYS_PATHID_LASTNMSPC
    | SYS_PATH_REVERSE
    | SYS_PXQEXTRACT
    | SYS_RID_ORDER
    | SYS_ROW_DELTA
    | SYS_SC_2_XMLT
    | SYS_SYNRCIREDO
    | SYSTEM_DEFINED
    | SYSTEM
    | SYSTIMESTAMP
    | SYS_TYPEID
    | SYS_UMAKEXML
    | SYS_XMLANALYZE
    | SYS_XMLCONTAINS
    | SYS_XMLCONV
    | SYS_XMLEXNSURI
    | SYS_XMLGEN
    | SYS_XMLI_LOC_ISNODE
    | SYS_XMLI_LOC_ISTEXT
    | SYS_XMLINSTR
    | SYS_XMLLOCATOR_GETSVAL
    | SYS_XMLNODEID_GETCID
    | SYS_XMLNODEID_GETLOCATOR
    | SYS_XMLNODEID_GETOKEY
    | SYS_XMLNODEID_GETPATHID
    | SYS_XMLNODEID_GETPTRID
    | SYS_XMLNODEID_GETRID
    | SYS_XMLNODEID_GETSVAL
    | SYS_XMLNODEID_GETTID
    | SYS_XMLNODEID
    | SYS_XMLT_2_SC
    | SYS_XMLTRANSLATE
    | SYS_XMLTYPE2SQL
    | SYS_XQ_ASQLCNV
    | SYS_XQ_ATOMCNVCHK
    | SYS_XQBASEURI
    | SYS_XQCASTABLEERRH
    | SYS_XQCODEP2STR
    | SYS_XQCODEPEQ
    | SYS_XQCON2SEQ
    | SYS_XQCONCAT
    | SYS_XQDELETE
    | SYS_XQDFLTCOLATION
    | SYS_XQDOC
    | SYS_XQDOCURI
    | SYS_XQDURDIV
    | SYS_XQED4URI
    | SYS_XQENDSWITH
    | SYS_XQERRH
    | SYS_XQERR
    | SYS_XQESHTMLURI
    | SYS_XQEXLOBVAL
    | SYS_XQEXSTWRP
    | SYS_XQEXTRACT
    | SYS_XQEXTRREF
    | SYS_XQEXVAL
    | SYS_XQFB2STR
    | SYS_XQFNBOOL
    | SYS_XQFNCMP
    | SYS_XQFNDATIM
    | SYS_XQFNLNAME
    | SYS_XQFNNM
    | SYS_XQFNNSURI
    | SYS_XQFNPREDTRUTH
    | SYS_XQFNQNM
    | SYS_XQFNROOT
    | SYS_XQFORMATNUM
    | SYS_XQFTCONTAIN
    | SYS_XQFUNCR
    | SYS_XQGETCONTENT
    | SYS_XQINDXOF
    | SYS_XQINSERT
    | SYS_XQINSPFX
    | SYS_XQIRI2URI
    | SYS_XQLANG
    | SYS_XQLLNMFRMQNM
    | SYS_XQMKNODEREF
    | SYS_XQNILLED
    | SYS_XQNODENAME
    | SYS_XQNORMSPACE
    | SYS_XQNORMUCODE
    | SYS_XQ_NRNG
    | SYS_XQNSP4PFX
    | SYS_XQNSPFRMQNM
    | SYS_XQPFXFRMQNM
    | SYS_XQ_PKSQL2XML
    | SYS_XQPOLYABS
    | SYS_XQPOLYADD
    | SYS_XQPOLYCEL
    | SYS_XQPOLYCSTBL
    | SYS_XQPOLYCST
    | SYS_XQPOLYDIV
    | SYS_XQPOLYFLR
    | SYS_XQPOLYMOD
    | SYS_XQPOLYMUL
    | SYS_XQPOLYRND
    | SYS_XQPOLYSQRT
    | SYS_XQPOLYSUB
    | SYS_XQPOLYUMUS
    | SYS_XQPOLYUPLS
    | SYS_XQPOLYVEQ
    | SYS_XQPOLYVGE
    | SYS_XQPOLYVGT
    | SYS_XQPOLYVLE
    | SYS_XQPOLYVLT
    | SYS_XQPOLYVNE
    | SYS_XQREF2VAL
    | SYS_XQRENAME
    | SYS_XQREPLACE
    | SYS_XQRESVURI
    | SYS_XQRNDHALF2EVN
    | SYS_XQRSLVQNM
    | SYS_XQRYENVPGET
    | SYS_XQRYVARGET
    | SYS_XQRYWRP
    | SYS_XQSEQ2CON4XC
    | SYS_XQSEQ2CON
    | SYS_XQSEQDEEPEQ
    | SYS_XQSEQINSB
    | SYS_XQSEQRM
    | SYS_XQSEQRVS
    | SYS_XQSEQSUB
    | SYS_XQSEQTYPMATCH
    | SYS_XQSTARTSWITH
    | SYS_XQSTATBURI
    | SYS_XQSTR2CODEP
    | SYS_XQSTRJOIN
    | SYS_XQSUBSTRAFT
    | SYS_XQSUBSTRBEF
    | SYS_XQTOKENIZE
    | SYS_XQTREATAS
    | SYS_XQ_UPKXML2SQL
    | SYS_XQXFORM
    | TABLE
    | TABLE_LOOKUP_BY_NL
    | TABLES
    | TABLESPACE
    | TABLESPACE_NO
    | TABLE_STATS
    | TABNO
    | TANH
    | TAN
    | TBLORIDXPARTNUM
    | TEMPFILE
    | TEMPLATE
    | TEMPORARY
    | TEMP_TABLE
    | TEST
    | THAN
    | THE
    | THEN
    | THREAD
    | THROUGH
    | TIME
    | TIMEOUT
    | TIMES
    | TIMESTAMP
    | TIMEZONE_ABBR
    | TIMEZONE_HOUR
    | TIMEZONE_MINUTE
    | TIME_ZONE
    | TIMEZONE_OFFSET
    | TIMEZONE_REGION
    | TIV_GB
    | TIV_SSF
    | TO_BINARY_DOUBLE
    | TO_BINARY_FLOAT
    | TO_BLOB
    | TO_CHAR
    | TO_CLOB
    | TO_DATE
    | TO_DSINTERVAL
    | TO_LOB
    | TO_MULTI_BYTE
    | TO_NCHAR
    | TO_NCLOB
    | TO_NUMBER
    | TOPLEVEL
    | TO_SINGLE_BYTE
    | TO_TIME
    | TO_TIMESTAMP
    | TO_TIMESTAMP_TZ
    | TO_TIME_TZ
    | TO_YMINTERVAL
    | TRACE
    | TRACING
    | TRACKING
    | TRAILING
    | TRANSACTION
    | TRANSFORM_DISTINCT_AGG
    | TRANSITIONAL
    | TRANSITION
    | TRANSLATE
    | TREAT
    | TRIGGERS
    | TRIM
    | TRUE
    | TRUNCATE
    | TRUNC
    | TRUSTED
    | TUNING
    | TX
    | TYPE
    | TYPES
    | TZ_OFFSET
    | UB2
    | UBA
    | UID
    | UNARCHIVED
    | UNBOUNDED
    | UNBOUND
    | UNDER
    | UNDO
    | UNDROP
    | UNIFORM
    | UNISTR
    | UNLIMITED
    | UNLOAD
    | UNLOCK
    | UNNEST_INNERJ_DISTINCT_VIEW
    | UNNEST
    | UNNEST_NOSEMIJ_NODISTINCTVIEW
    | UNNEST_SEMIJ_VIEW
    | UNPACKED
    | UNPIVOT
    | UNPROTECTED
    | UNQUIESCE
    | UNRECOVERABLE
    | UNRESTRICTED
    | UNTIL
    | UNUSABLE
    | UNUSED
    | UPDATABLE
    | UPDATED
    | UPDATEXML
    | UPD_INDEXES
    | UPD_JOININDEX
    | UPGRADE
    | UPPER
    | UPSERT
    | UROWID
    | USAGE
    | USE_ANTI
    | USE_CONCAT
    | USE_HASH_AGGREGATION
    | USE_HASH_GBY_FOR_PUSHDOWN
    | USE_HASH
    | USE_INVISIBLE_INDEXES
    | USE_MERGE_CARTESIAN
    | USE_MERGE
    | USE
    | USE_NL
    | USE_NL_WITH_INDEX
    | USE_PRIVATE_OUTLINES
    | USER_DEFINED
    | USERENV
    | USERGROUP
    | USER
    | USER_RECYCLEBIN
    | USERS
    | USE_SEMI
    | USE_STORED_OUTLINES
    | USE_TTT_FOR_GSETS
    | USE_WEAK_NAME_RESL
    | USING
    | VALIDATE
    | VALIDATION
    | VALUE
    | VARIANCE
    | VAR_POP
    | VARRAY
    | VARRAYS
    | VAR_SAMP
    | VARYING
    | VECTOR_READ
    | VECTOR_READ_TRACE
    | VERIFY
    | VERSIONING
    | VERSION
    | VERSIONS_ENDSCN
    | VERSIONS_ENDTIME
    | VERSIONS
    | VERSIONS_OPERATION
    | VERSIONS_STARTSCN
    | VERSIONS_STARTTIME
    | VERSIONS_XID
    | VIRTUAL
    | VISIBLE
    | VOLUME
    | VSIZE
    | WAIT
    | WALLET
    | WELLFORMED
    | WHENEVER
    | WHEN
    | WHITESPACE
    | WIDTH_BUCKET
    | WITHIN
    | WITHOUT
    | WORK
    | WRAPPED
    | WRITE
    | XDB_FASTPATH_INSERT
    | X_DYN_PRUNE
    | XID
    | XML2OBJECT
    | XMLATTRIBUTES
    | XMLCAST
    | XMLCDATA
    | XMLCOLATTVAL
    | XMLCOMMENT
    | XMLCONCAT
    | XMLDIFF
    | XML_DML_RWT_STMT
    | XMLELEMENT
    | XMLEXISTS2
    | XMLEXISTS
    | XMLFOREST
    | XMLINDEX_REWRITE_IN_SELECT
    | XMLINDEX_REWRITE
    | XMLINDEX_SEL_IDX_TBL
    | XMLISNODE
    | XMLISVALID
    | XML
    | XMLNAMESPACES
    | XMLPARSE
    | XMLPATCH
    | XMLPI
    | XMLQUERY
    | XMLQUERYVAL
    | XMLROOT
    | XMLSCHEMA
    | XMLSERIALIZE
    | XMLTABLE
    | XMLTRANSFORMBLOB
    | XMLTRANSFORM
    | XMLTYPE
    | XPATHTABLE
    | XS_SYS_CONTEXT
    | YEAR
    | YES
    | ZONE
    ;

string_function_name
    : CHR
    | DECODE
    | SUBSTR
    | TO_CHAR
    | TRIM
    ;

numeric_function_name
    : AVG
    | COUNT
    | NVL
    | ROUND
    | SUM
    ;
