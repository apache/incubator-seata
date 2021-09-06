/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import com.baomidou.mybatisplus.annotation.DbType;
// import com.baomidou.mybatisplus.annotation.FieldFill;
// import com.baomidou.mybatisplus.annotation.IdType;
//
// import com.baomidou.mybatisplus.generator.AutoGenerator;
// import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
// import com.baomidou.mybatisplus.generator.config.GlobalConfig;
// import com.baomidou.mybatisplus.generator.config.PackageConfig;
// import com.baomidou.mybatisplus.generator.config.StrategyConfig;
// import com.baomidou.mybatisplus.generator.config.po.TableFill;
// import com.baomidou.mybatisplus.generator.config.rules.DateType;
// import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
//
// import java.util.ArrayList;
//
// public class CodeGene {
//     public static void main(String[] args) {
//         //代码自动生成器
//
// //        创建一个代码生成器对象
//         AutoGenerator mpg = new AutoGenerator();
// //    配置策略
// //        1.全局配置
//         GlobalConfig gc = new GlobalConfig();
//         // 有问题
//         String objectPath = System.getProperty("user.dir");
//         gc.setOutputDir(objectPath+"/src/main/java");
//         gc.setOpen(false);
//         gc.setServiceName("%sService"); //取出service的I前缀
//         gc.setFileOverride(true); //是否覆盖
//         gc.setIdType(IdType.AUTO);
//         gc.setDateType(DateType.ONLY_DATE);
//         gc.setSwagger2(true);
//         mpg.setGlobalConfig(gc);
//
// //        设置数据源
//         DataSourceConfig dsc = new DataSourceConfig();
//         dsc.setDriverName("com.mysql.cj.jdbc.Driver");
//         dsc.setDbType(DbType.MYSQL);
//         dsc.setUsername("root");
//         dsc.setPassword("123456");
//         dsc.setUrl("jdbc:mysql://127.0.0.1:3306/storage?serverTimezone=GMT%2B8");
//         mpg.setDataSource(dsc);
//
// //        包的配置
//         PackageConfig pc = new PackageConfig();
//         pc.setParent("com.demo");
//         pc.setController("controller");
//         pc.setService("service");
//         pc.setEntity("model");
//         pc.setMapper("mapper");
//         mpg.setPackageInfo(pc);
//
// //        策略配置
//         StrategyConfig strategy = new StrategyConfig();
// //        设置要映射的表明
// //            strategy.setInclude("user");
// //        数据库表映射到实体的命名策略
//         strategy.setNaming(NamingStrategy.underline_to_camel);
// //          设置表的前缀不生成，表的前缀是什么，第一个字符串就是什么
//         strategy.setTablePrefix("edu"+"_");
// //            数据库表字段映射到实体的命名策略
//         strategy.setColumnNaming(NamingStrategy.underline_to_camel);
//         strategy.setEntityLombokModel(true); // 自动lombok；
// //        设置逻辑删除字段
//         strategy.setLogicDeleteFieldName("is_deleted");
// //            去掉布尔值的is前缀
//         strategy.setEntityBooleanColumnRemoveIsPrefix(true);
// //             设置自动填充字段
//         TableFill gmtCreate = new TableFill("gmt_create", FieldFill.INSERT);
//         TableFill gmtModified = new TableFill("gmt_modified", FieldFill.INSERT_UPDATE);
//         ArrayList<TableFill> list = new ArrayList<>();
//         list.add(gmtCreate);
//         list.add(gmtModified);
//         strategy.setTableFillList(list);
//         mpg.setStrategy(strategy);
//         mpg.execute();
//
//     }
// }