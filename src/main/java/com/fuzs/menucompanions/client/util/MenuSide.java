package com.fuzs.menucompanions.client.util;

public enum MenuSide {

    LEFT, RIGHT, BOTH;

    public MenuSide inverse() {

        return MenuSide.values()[(this.ordinal() + 1) % 2];
    }

}
