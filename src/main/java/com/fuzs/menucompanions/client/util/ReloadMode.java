package com.fuzs.menucompanions.client.util;

@SuppressWarnings("unused")
public enum ReloadMode {

    NEVER(false, false, false),
    RIGHT_CONTROL(false, true, false),
    RIGHT_ALWAYS(false, false, true),
    LEFT_CONTROL(true, true, false),
    LEFT_ALWAYS(true, false, true);

    private final boolean left;
    private final boolean control;
    private final boolean always;

    ReloadMode(boolean left, boolean control, boolean always) {

        this.left = left;
        this.control = control;
        this.always = always;
    }

    public boolean isLeft() {

        return this.left;
    }

    public boolean requiresControl() {

        return this.control;
    }

    public boolean isAlways() {

        return this.always;
    }

}
