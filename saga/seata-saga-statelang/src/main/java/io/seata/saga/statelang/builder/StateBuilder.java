package io.seata.saga.statelang.builder;

import io.seata.saga.statelang.domain.State;

/***
 * Configure transitions for {@link StateMachineBuilder}.
 *
 * @param <B> builder type
 * @param <S> state type
 * @author ptyin
 */
public interface StateBuilder<B extends StateBuilder<B, S>, S extends State>
{
    /**
     * Configure name.
     *
     * @param name name of state
     * @return builder for chaining
     */
    B withName(String name);

    /**
     * Configure comment.
     *
     * @param comment comment of state
     * @return builder for chaining
     */
    B withComment(String comment);

    /**
     * Configure next state.
     *
     * @param next next state
     * @return builder for chaining
     */
    B withNext(String next);

    /**
     *  Build a state.
     *
     * @return built state
     */
    S build();

    /**
     * Get the working {@link StatesConfigurer} parent.
     *
     * @return parent
     */
    StatesConfigurer and();
}
