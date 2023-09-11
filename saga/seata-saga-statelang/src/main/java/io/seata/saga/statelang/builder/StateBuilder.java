package io.seata.saga.statelang.builder;

import io.seata.saga.statelang.domain.State;

/***
 * State builder.
 *
 * @param <S> state type
 * @author ptyin
 */
public interface StateBuilder<S extends State> {
    /**
     * Build a state.
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
