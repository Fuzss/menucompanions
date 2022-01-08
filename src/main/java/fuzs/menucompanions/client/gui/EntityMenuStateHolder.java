package fuzs.menucompanions.client.gui;

interface EntityMenuStateHolder {
    EntityMenuState getState();
    
    void setState(EntityMenuState entityMenuState);

    default boolean isEnabled() {
        return this.getState() == EntityMenuState.ENABLED;
    }

    default boolean isNotEnabled() {
        return !this.isEnabled() && this.getState() != EntityMenuState.UPDATE_REQUIRED;
    }

    default boolean isDisabled() {
        return this.getState() == EntityMenuState.DISABLED;
    }

    default boolean isInvalid() {
        return this.getState() == EntityMenuState.INVALID || this.getState() == EntityMenuState.UPDATE_REQUIRED;
    }

    default void setEnabled(boolean enable) {
        this.setState(enable ? EntityMenuState.ENABLED : EntityMenuState.DISABLED);
    }

    default void setUpdateRequired() {
        if (this.isEnabled()) {
            this.setState(EntityMenuState.UPDATE_REQUIRED);
        } else if (!this.isDisabled()) {
            this.setState(EntityMenuState.INVALID);
        }
    }

    default void setInvalid() {
        if (!this.isDisabled() && this.getState() != EntityMenuState.BROKEN) {
            this.setState(EntityMenuState.INVALID);
        }
    }

    default void setBroken() {
        if (!this.isDisabled()) {
            this.setState(EntityMenuState.BROKEN);
        }
    }

    default EntityMenuState getDefaultState() {
        return EntityMenuState.INVALID;
    }

    enum EntityMenuState {
        DISABLED, ENABLED, UPDATE_REQUIRED, INVALID, BROKEN
    }
}
