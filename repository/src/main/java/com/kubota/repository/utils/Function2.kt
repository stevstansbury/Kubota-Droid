package com.kubota.repository.utils

interface Function2<I, J, O> {
    /**
     * Applies this function to the given inputs.
     *
     * @param input1 the first input
     * @param input2 the second input
     * @return the function result.
     */
    fun apply(input1: I, input2: J): O
}