###测试场景
数据库版本：mysql5、mysql8; 
序列化类型：jackson、fastjson
测试数据类型：
###INT TYPE
CREATE TABLE if not exists `t_test_numeric_type` (
`id` int(32) NOT NULL,
`tinyint_type` TINYINT,
`smallint_type` SMALLINT,
`mediumint_type` MEDIUMINT,
`int_type` INT,
`bigint_type` BIGINT,
`float_type` FLOAT,
`double_type` DOUBLE,
`decimal_type` DECIMAL(4,2),
`numeric_type` NUMERIC(4,2),
PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
###DATE TYPE
create table if not EXISTS t_test_date_type(
`id` int,
`date_type` DATE,
`time_type` TIME,
`year_type` YEAR,
`datetime_type` DATETIME,
`timestamp_type` TIMESTAMP,
PRIMARY KEY (`id`)
)ENGINE=INNODB DEFAULT CHARSET=utf8;
###String TYPE
create table if not EXISTS t_test_string_type(
`id` int,
`char_type` char(5),
`varchar_type` varchar(64),
`tinyblob_type` TINYBLOB,
`tinytext_type` TINYTEXT,
`blob_type` BLOB,
`text_type` TEXT,
`mediumblob_type` MEDIUMBLOB,
`mediumtext_type` MEDIUMTEXT,
`longblob_type` LONGBLOB,
`longtext_type` LONGTEXT,
PRIMARY KEY (`id`)
)ENGINE=INNODB DEFAULT CHARSET=utf8;
###POINT TYPE
create table if not EXISTS t_test_point_type(
`id` int,
`point_type` POINT ,
`linestring_type` LINESTRING,
`polygon_type` POLYGON,
`geometry_type` GEOMETRY,
`multipoint_type` MULTIPOINT,
`multi_linestring_type` MULTILINESTRING,
`multi_polygon_type` MULTIPOLYGON,
`geometry_collection_type` GEOMETRYCOLLECTION,
PRIMARY KEY (`id`)
)ENGINE=INNODB DEFAULT CHARSET=utf8;
###SPECIAL TYPE
create table if not EXISTS t_test_special_type(
`id` int,
`bit_type` BIT(5),
`real_type` REAL,
`binary_type` BINARY(8),
`varbinary_type` VARBINARY(8),
`enum_type` ENUM ('1','2','3'),
`set_type` SET ('a','b','c'),
PRIMARY KEY (`id`)
)ENGINE=INNODB DEFAULT CHARSET=utf8;
