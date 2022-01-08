package fuzs.menucompanions.client.util;

public enum ReloadMode {
    NEVER(false, false, false),
    RIGHT_CONTROL(false, true, false),
    RIGHT_ALWAYS(false, false, true),
    LEFT_CONTROL(true, true, false),
    LEFT_ALWAYS(true, false, true);

    private final boolean left;
    private final boolean modifier;
    private final boolean always;

    ReloadMode(boolean left, boolean modifier, boolean always) {
        this.left = left;
        this.modifier = modifier;
        this.always = always;
    }

    public boolean left() {
        return this.left;
    }

    public boolean withModifierKey() {
        return this.modifier;
    }

    public boolean isAlways() {
        return this.always;
    }
}
