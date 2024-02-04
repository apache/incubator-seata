package org.apache.seata.common.exception;

import java.util.HashSet;
import java.util.MissingResourceException;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class ResourceBundleUtilTest {

    @Test
    void getInstance() {
        Assertions.assertNotNull(ResourceBundleUtil.getInstance());
    }

    @Test
    void getMessage() {
        ResourceBundleUtil resourceBundleUtil = ResourceBundleUtil.getInstance();
        String emptyKeyMsg = resourceBundleUtil.getMessage("", "param1");
        Assertions.assertNull(emptyKeyMsg);
        Assertions.assertThrows(MissingResourceException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                resourceBundleUtil.getMessage("NotExist");
            }
        });
        String configErrorMsgWithoutParams = resourceBundleUtil.getMessage("ERR_CONFIG");
        Assertions.assertEquals("config error, {0}", configErrorMsgWithoutParams);

        String configErrorMsgWithParams = resourceBundleUtil.getMessage("ERR_CONFIG", "vgroup_mapping_test");
        Assertions.assertEquals("config error, vgroup_mapping_test", configErrorMsgWithParams);
    }

    @Test
    void testGetMessage() {
        ResourceBundleUtil resourceBundleUtil = ResourceBundleUtil.getInstance();
        String emptyKeyMsg = resourceBundleUtil.getMessage("", ErrorCode.ERR_CONFIG.getCode(),
            ErrorCode.ERR_CONFIG.getType());
        Assertions.assertNull(emptyKeyMsg);
        String errorConfigMsg = resourceBundleUtil.getMessage(ErrorCode.ERR_CONFIG.name(),
            ErrorCode.ERR_CONFIG.getCode(), ErrorCode.ERR_CONFIG.getType());
        Assertions.assertEquals("ERR-CODE: [Seata-1][ERR_CONFIG] config error, {0} More: [https://seata.apache"
            + ".org/docs/next/overview/faq#1]", errorConfigMsg);
        String errorConfigMsgWithParams = resourceBundleUtil.getMessage(ErrorCode.ERR_CONFIG.name(),
            ErrorCode.ERR_CONFIG.getCode(), ErrorCode.ERR_CONFIG.getType(), "vgroup_mapping_test");
        Assertions.assertEquals(
            "ERR-CODE: [Seata-1][ERR_CONFIG] config error, vgroup_mapping_test More: [https://seata.apache"
                + ".org/docs/next/overview/faq#1]", errorConfigMsgWithParams);
    }

    @Test
    void getFormattedMessage() {
        ResourceBundleUtil resourceBundleUtil = ResourceBundleUtil.getInstance();
        Assertions.assertThrows(MissingResourceException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                resourceBundleUtil.getFormattedMessage("NotExist");
            }
        });
        String configErrorMsg = resourceBundleUtil.getFormattedMessage("ERR_CONFIG");
        Assertions.assertEquals("config error, {0}", configErrorMsg);
    }

    @Test
    void parseStringValue() {
        ResourceBundleUtil resourceBundleUtil = ResourceBundleUtil.getInstance();
        String strVal = "str val without placeholder";
        String parseValue = resourceBundleUtil.parseStringValue(strVal, new HashSet<>());
        Assertions.assertEquals(strVal, parseValue);
        strVal = "str val without placeholder ${";
        parseValue = resourceBundleUtil.parseStringValue(strVal, new HashSet<>());
        Assertions.assertEquals(strVal, parseValue);
        strVal = "str val without placeholder }";
        parseValue = resourceBundleUtil.parseStringValue(strVal, new HashSet<>());
        Assertions.assertEquals(strVal, parseValue);

        final String strValWithEmptyPlaceHolder = "str val with placeholder ${}";
        Assertions.assertThrows(SeataRuntimeException.class, new Executable() {
            @Override
            public void execute() throws Throwable {
                resourceBundleUtil.parseStringValue(strValWithEmptyPlaceHolder, new HashSet<>());
            }
        }, "Could not resolve placeholder 'str val with placeholder ${}'");
        String strValWithPlaceHolder = "str val with placeholder ${ERR_CONFIG}";
        Set<String> holderSet = new HashSet<>();
        parseValue = resourceBundleUtil.parseStringValue(strValWithPlaceHolder, holderSet);
        Assertions.assertEquals("str val with placeholder config error, {0}", parseValue);
        Assertions.assertEquals(0, holderSet.size());

        String multiSamePlaceHolder = "str val with placeholder ${ERR_CONFIG},${ERR_CONFIG}";
        parseValue = resourceBundleUtil.parseStringValue(multiSamePlaceHolder, holderSet);
        Assertions.assertEquals("str val with placeholder config error, {0},config error, {0}", parseValue);

        final String strValWithEmptyPlaceHolderValue = "str val with placeholder ${ERR_NOT_EXIST}";
        Assertions.assertDoesNotThrow(new Executable() {
            @Override
            public void execute() throws Throwable {
                Set<String> placeholderSet = new HashSet<>();
                resourceBundleUtil.parseStringValue(strValWithEmptyPlaceHolderValue, placeholderSet);
                Assertions.assertEquals(0, placeholderSet.size());
            }
        });

        final String strValWithNestPlaceHolderValue = "str val with placeholder ${${${ERROR_LOOP}}}";
        Set<String> placeholderSet = new HashSet<>();
        parseValue = resourceBundleUtil.parseStringValue(strValWithNestPlaceHolderValue, new HashSet<>());
        Assertions.assertEquals("str val with placeholder ERROR_LOOP", parseValue);
        Assertions.assertEquals(0, placeholderSet.size());

        String strValWithNestPlaceHolder = "str val with placeholder ${${ERR_NEST2}}";
        parseValue = resourceBundleUtil.parseStringValue(strValWithNestPlaceHolder, new HashSet<>());
        Assertions.assertEquals("str val with placeholder ERR NEST TEST", parseValue);
        Assertions.assertEquals(0, placeholderSet.size());
    }
}