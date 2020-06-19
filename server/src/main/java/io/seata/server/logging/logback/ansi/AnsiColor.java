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
package io.seata.server.logging.logback.ansi;

/**
 * {@link AnsiElement Ansi} colors.
 *
 * @author Phillip Webb
 * @author Geoffrey Chandler
 * @origin Copied from spring-boot.jar by wang.liang
 */
public enum AnsiColor implements AnsiElement {

    /**
     * default is light-grey
     */
    DEFAULT("39"),

    /**
     * black
     */
    BLACK("30"),

    /**
     * red
     */
    RED("31"),

    /**
     * green
     */
    GREEN("32"),

    /**
     * yellow
     */
    YELLOW("33"),

    /**
     * blue
     */
    BLUE("34"),

    /**
     * magenta
     */
    MAGENTA("35"),

    /**
     * cyan
     */
    CYAN("36"),

    /**
     * white
     */
    WHITE("37"),

    /**
     * bright black
     */
    BRIGHT_BLACK("90"),

    /**
     * bright red
     */
    BRIGHT_RED("91"),

    /**
     * bright green
     */
    BRIGHT_GREEN("92"),

    /**
     * bright yellow
     */
    BRIGHT_YELLOW("93"),

    /**
     * bright blue
     */
    BRIGHT_BLUE("94"),

    /**
     * bright magenta
     */
    BRIGHT_MAGENTA("95"),

    /**
     * bright cyan
     */
    BRIGHT_CYAN("96"),

    /**
     * bright white
     */
    BRIGHT_WHITE("97");

    /**
     * code of color
     */
    private final String code;

    AnsiColor(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return this.code;
    }

}
