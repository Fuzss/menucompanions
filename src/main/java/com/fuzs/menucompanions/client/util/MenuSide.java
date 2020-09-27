package com.fuzs.menucompanions.client.util;

public enum MenuSide {

    BOTH(-1), LEFT(0), RIGHT(1);

    private final int offsetPos;

    MenuSide(int pos) {

        this.offsetPos = pos;
    }

    public int getOffsetPos() {

        return this.offsetPos;
    }

}
