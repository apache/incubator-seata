/**
 * @author xjl
 * @Description:
 */
import java.io.File;
import java.io.FileWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量给项目的java文件添加licence文件头
 * <p>
 *
 * ---1.使用步骤---
 * 1.修改projectPath项目路径
 * 2.修改licence文件信（编辑好的文本直接复制过来）
 * 3.执行main方法
 *
 *
 * ---2.注意事项---
 * 如果已经有同样的licence，多次执行，内容相同的不会替换，所以有新增的文件也可以再次执行
 * </p>
 */
public class Test {
    /**
     * 项目根目录
     */
    private static String projectPath = "E:\\JavaEEStudyProject\\seata-e2e";
    /**
     * java文件头部的的License文本
     */
    private static String licence = "/*\n" +
            " *  Copyright 1999-2019 Seata.io Group.\n" +
            " *\n" +
            " *  Licensed under the Apache License, Version 2.0 (the \"License\");\n" +
            " *  you may not use this file except in compliance with the License.\n" +
            " *  You may obtain a copy of the License at\n" +
            " *\n" +
            " *       http://www.apache.org/licenses/LICENSE-2.0\n" +
            " *\n" +
            " *  Unless required by applicable law or agreed to in writing, software\n" +
            " *  distributed under the License is distributed on an \"AS IS\" BASIS,\n" +
            " *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n" +
            " *  See the License for the specific language governing permissions and\n" +
            " *  limitations under the License.\n" +
            " */";

    /**
     * 文件处理异常记录
     */
    private static List<String> fail = new ArrayList<>();
    /**
     * JAVA文件关键字内容错误记录
     */
    private static List<String> wrong = new ArrayList<>();
    /**
     * 已处理的java文件数量
     */
    private static int count = 0;

    public static void main(String[] args) {

        addLicence(new File(projectPath), licence);

        System.out.println("为 " + count + " 个Java源代码文件添加licence信息头");

        // 文件处理异常
        if (fail.size() > 0) {
            System.out.println("文件处理异常个数 " + fail.size());
            // 异常文件路径输出
            for (String f : fail) {
                System.out.println("------1-------" + f + "------1-------");
            }
        }

        // JAVA文件关键字内容错误
        if (wrong.size() > 0) {
            System.out.println("JAVA源代码错误个数 " + wrong.size());
            // JAVA文件关键字内容错误路径输出
            for (String w : wrong) {
                System.out.println("------2-------" + w + "------2-------");
            }
        }
    }

    /**
     * 添加licence信息头文本（只处理java文件，其他文件跳过）
     *
     * @param path    项目根目录
     * @param licence licence文本
     */
    private static void addLicence(File path, String licence) {
        if (path != null && path.exists()) {
            // 处理的文件夹
            if (path.isDirectory()) {
                String[] children = path.list();
                for (int i = 0; i < children.length; i++) {
                    File child = new File(path.getPath() + System.getProperty("file.separator") + children[i]);
                    addLicence(child, licence);
                }
            } else {
                // 处理java文件
                if (path.getName().toLowerCase().endsWith(".java")) {
                    System.out.println(path.getAbsolutePath());
                    count++;
                    try {
                        byte[] content;
                        try (RandomAccessFile f = new RandomAccessFile(path, "rw")) {
                            content = new byte[(int) f.length()];
                            f.readFully(content);
                        }
                        String text = new String(content);
                        text = text.trim();
                        while (text.startsWith("/n")) {
                            text = text.substring(1);
                        }
                        // 如果已经有同样的licence，则忽略
                        int pos = text.indexOf(licence);
                        if (pos != -1) {
                            return;
                        }
                        // 有package声明的，保留package以后的内容
                        if (text.indexOf("package") != -1) {
                            text = text.substring(text.indexOf("package"));
                        }
                        // 没有package声明的，有import声明的，保留import以后的内容
                        else if (text.indexOf("package") == -1 && text.indexOf("import") != -1) {
                            text = text.substring(text.indexOf("import"));
                        }
                        // 没有package声明也没有import声明的，有类级别注释的，则保留类级别注释以后的内容
                        else if (text.indexOf("package") == -1 && text.indexOf("import") == -1 && text.indexOf("/**") != -1
                                && text.indexOf("public class") != -1 && text.indexOf("/**") < text.indexOf("public class")) {
                            text = text.substring(text.indexOf("/**"));
                        }
                        // 没有package声明也没有import声明的，也没有类级别注释的则保留public class以后的内容
                        else if (text.indexOf("package") == -1 && text.indexOf("import") == -1 && text.indexOf("public class") != -1
                                && (text.indexOf("/**") > text.indexOf("public class") || text.indexOf("/**") == -1)) {
                            text = text.substring(text.indexOf("public class"));
                        } else {
                            // 是java文件的后缀，文本内容没有关键字
                            wrong.add(path.getAbsolutePath());
                            return;
                        }
                        try (FileWriter writer = new FileWriter(path)) {
                            writer.write(licence);
                            writer.write("\n\n");
                            writer.write(text);
                        }
                    } catch (Exception ex) {
                        // 修改文件异常
                        fail.add(path.getAbsolutePath());
                    }
                }
            }
        }
    }
}

