package com.alibaba.fescar.common.loader;

/**
 * @author melon.zhao
 * @since 2019/2/26
 */
@LoadLevel(name = "FrenchHello",order = 2)
public class FrenchHello  implements Hello{

    @Override
    public String say() {
        return "Bonjour";
    }
}
