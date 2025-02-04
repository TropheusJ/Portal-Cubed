package com.fusionflux.portalcubed.entity;

public interface Fizzleable {
    void startFizzlingProgress();

    void fizzle();

    float getFizzleProgress();

    boolean fizzling();

    boolean fizzlesInGoo();

    FizzleType getFizzleType();

    enum FizzleType {
        NOT, OBJECT, LIVING
    }
}
