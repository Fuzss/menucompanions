package fuzs.menucompanions.client.storage.entry;

import java.util.Locale;

public enum MenuPropertyFlags {
    TICK, ON_GROUND, IN_WATER, AGGRESSIVE, IN_LOVE, WALK, CROUCH;

    private final int identifier = 1 << this.ordinal();

    public int getPropertyMask() {
        return this.identifier;
    }

    @Override
    public String toString() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static boolean readProperty(byte data, MenuPropertyFlags flag) {
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

        public Builder add(MenuPropertyFlags flag) {
            this.data |= flag.identifier;
            return this;
        }

        public Builder addAll(MenuPropertyFlags... flags) {
            for (MenuPropertyFlags flag : flags) {
                this.add(flag);
            }
            return this;
        }
    }
}