package io.seata.saga.statelang.builder;

import io.seata.saga.statelang.domain.ChoiceState;

/***
 *
 * @author ptyin
 */
public interface ChoiceStateBuilder extends StateBuilder<ChoiceStateBuilder, ChoiceState> {

    ChoiceStateBuilder put(String expression, String next);

}
