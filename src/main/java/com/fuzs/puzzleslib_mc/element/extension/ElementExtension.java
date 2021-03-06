package com.fuzs.puzzleslib_mc.element.extension;

import com.fuzs.puzzleslib_mc.element.EventListener;
import net.minecraftforge.eventbus.api.Event;

import java.util.List;

/**
 * abstract template for sided elements complementing a common element
 */
public abstract class ElementExtension<T extends ExtensibleElement<?>> extends EventListener {

    /**
     * common element this belongs to
     */
    public final T parent;

    /**
     * create new with parent
     * @param parent parent
     */
    public ElementExtension(T parent) {

        this.parent = parent;
    }

    @Override
    public final List<EventStorage<? extends Event>> getEvents() {

        return this.parent.getEvents();
    }

}
