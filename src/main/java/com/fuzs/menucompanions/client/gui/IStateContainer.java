package com.fuzs.menucompanions.client.gui;

interface IStateContainer {

    ContainerState getState();
    
    void setState(ContainerState containerState);

    default boolean isEnabled() {

        return this.getState() == ContainerState.ENABLED;
    }

    default boolean isNotEnabled() {

        return !this.isEnabled() && this.getState() != ContainerState.UPDATE_REQUIRED;
    }

    default boolean isDisabled() {

        return this.getState() == ContainerState.DISABLED;
    }

    default boolean isInvalid() {

        return this.getState() == ContainerState.INVALID || this.getState() == ContainerState.UPDATE_REQUIRED;
    }

    default void setEnabled(boolean enable) {

        this.setState(enable ? ContainerState.ENABLED : ContainerState.DISABLED);
    }

    default void setUpdateRequired() {

        if (this.isEnabled()) {

            this.setState(ContainerState.UPDATE_REQUIRED);
        } else if (!this.isDisabled()) {

            this.setState(ContainerState.INVALID);
        }
    }

    default void setInvalid() {

        if (!this.isDisabled() && this.getState() != ContainerState.BROKEN) {

            this.setState(ContainerState.INVALID);
        }
    }

    default void setBroken() {

        if (!this.isDisabled()) {

            this.setState(ContainerState.BROKEN);
        }
    }

    default ContainerState getDefaultState() {

        return ContainerState.INVALID;
    }

    enum ContainerState {

        DISABLED, ENABLED, UPDATE_REQUIRED, INVALID, BROKEN
    }
    
}
