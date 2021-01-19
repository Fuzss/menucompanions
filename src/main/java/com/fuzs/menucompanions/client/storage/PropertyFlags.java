package com.fuzs.menucompanions.client.storage;

public enum PropertyFlags {

    TICK("tick"),
    ON_GROUND("on_ground"),
    IN_WATER("in_water"),
    AGGRESSIVE("aggressive"),
    WALKING("walking"),
    IN_LOVE("in_love");

    private final int identifier = 1 << this.ordinal();
    private final String name;

    PropertyFlags(String name) {

        this.name = name;
    }

    public int getPropertyMask() {

        return this.identifier;
    }

    @Override
    public String toString() {

        return this.name;
    }

}