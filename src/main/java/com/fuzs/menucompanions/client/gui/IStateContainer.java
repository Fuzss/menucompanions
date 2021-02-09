package com.fuzs.menucompanions.client.gui;

interface IStateContainer {

    ContainerState getState();
    
    void setState(ContainerState containerState);

    default boolean isEnabled() {

        return this.getState() == ContainerState.ENABLED;
    }

    default boolean isDisabled() {

        return this.getState() == ContainerState.DISABLED;
    }

    default boolean isInvalid() {

        return this.getState() == ContainerState.INVALID;
    }

    default void invalidate() {

        if (this.getState() != ContainerState.DISABLED) {

            this.setState(ContainerState.INVALID);
        }
    }

    default void setEnabled(boolean enabled) {

        if (!enabled) {

            this.setState(ContainerState.DISABLED);
        } else {

            this.setState(ContainerState.ENABLED);
        }
    }

    default void setBroken() {

        if (this.getState() == ContainerState.INVALID) {

            this.setState(ContainerState.BROKEN);
        }
    }

    default ContainerState getDefaultState() {

        return ContainerState.INVALID;
    }

    enum ContainerState {

        DISABLED, ENABLED, INVALID, BROKEN
    }
    
}
