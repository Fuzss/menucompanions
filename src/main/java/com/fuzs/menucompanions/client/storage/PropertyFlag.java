package com.fuzs.menucompanions.client.storage;

public enum PropertyFlag {

    TICK("tick"),
    ON_GROUND("ground"),
    IN_WATER("water"),
    AGGRESSIVE("aggressive"),
    WALKING("walking"),
    IN_LOVE("love"),
    CROUCHING("crouching");

    private final int identifier = 1 << this.ordinal();
    private final String name;

    PropertyFlag(String name) {

        this.name = name;
    }

    public int getPropertyMask() {

        return this.identifier;
    }

    @Override
    public String toString() {

        return this.name;
    }

    public static boolean readProperty(byte data, PropertyFlag flag) {

        return (data & flag.identifier) == flag.identifier;
    }

    public static class Builder {

        private byte data;

        public Builder() {

        }

        public Builder(byte data) {

            this.data = data;
        }

        public byte get() {

            return this.data;
        }

        public Builder add(PropertyFlag flag) {

            this.data |= flag.identifier;
            return this;
        }

    }

}