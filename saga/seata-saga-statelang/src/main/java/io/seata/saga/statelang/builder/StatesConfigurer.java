package io.seata.saga.statelang.builder;

import io.seata.saga.statelang.domain.State;

/***
 * Configure states for {@link StateMachineBuilder}.
 *
 * @author PTYin
 */
public interface StatesConfigurer
{
    /**
     * Build a state {@link State}.
     *
     * @param clazz specific builder class
     * @param <B> builder type
     * @return builder for chaining
     */
    <B extends StateBuilder<B, ?>> B build(Class<B> clazz);

    /**
     * Add a built state
     *
     * @param state a built state {@link State}
     * @return configurer for chaining
     */
    StatesConfigurer add(State state);

    /**
     * Configure all built states.
     *
     * @return the working {@link StateMachineBuilder} parent
     */
    StateMachineBuilder configure();
}
