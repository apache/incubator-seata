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
package io.seata.common.aot;

/**
 * The native utils
 *
 * @author wang.liang
 */
public class NativeUtils {

    /**
     * The native-image code
     *
     * @see <a href="https://github.com/oracle/graal/blob/master/sdk/src/org.graalvm.nativeimage/src/org/graalvm/nativeimage/ImageInfo.java">ImageInfo.java</a>
     */
    private static final String NATIVE_IMAGE_CODE = System.getProperty("org.graalvm.nativeimage.imagecode");


    public static final String SPRING_AOT_PROCESSING = "spring.aot.processing";


    /**
     * Whether Spring-AOT processing
     *
     * @return the boolean
     */
    public static boolean isSpringAotProcessing() {
        return "true".equalsIgnoreCase(System.getProperty(SPRING_AOT_PROCESSING));
    }

    /**
     * Gets the native-image code.
     *
     * @return the native-image code
     */
    public static String getNativeImageCode() {
        return NATIVE_IMAGE_CODE;
    }

    /**
     * Whether run in native-image
     *
     * @return the boolean
     * @see org.springframework.core.NativeDetector#inNativeImage()
     */
    public static boolean inNativeImage() {
        return NATIVE_IMAGE_CODE != null;
    }
}
