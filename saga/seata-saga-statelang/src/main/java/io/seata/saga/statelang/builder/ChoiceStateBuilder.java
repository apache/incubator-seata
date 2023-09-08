package io.seata.saga.statelang.builder;

import io.seata.saga.statelang.domain.ChoiceState;

/***
 *
 * @author ptyin
 */
public interface ChoiceStateBuilder extends StateBuilder<ChoiceStateBuilder, ChoiceState> {

    /**
     * Put (expression, next state) pair into choices.
     *
     * @param expression expression to evaluate
     * @param next name of next state
     * @return builder for chaining
     */
    ChoiceStateBuilder withChoice(String expression, String next);

    /**
     * Configure default choice when no valid choices.
     *
     * @param defaultChoice default choice
     * @return builder for chaining
     */
    ChoiceStateBuilder withDefault(String defaultChoice);
}
