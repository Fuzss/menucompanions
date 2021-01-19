package com.fuzs.menucompanions.client.element;

import com.google.common.collect.Lists;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;

import java.util.List;
import java.util.function.Consumer;

public interface IEventElement {

    List<EventStorage<? extends Event>> EVENTS = Lists.newArrayList();

    default <T extends Event> void addListener(Consumer<T> consumer) {

        this.addListener(consumer, EventPriority.NORMAL);
    }

    default <T extends Event> void addListener(Consumer<T> consumer, boolean receiveCancelled) {

        this.addListener(consumer, EventPriority.NORMAL, receiveCancelled);
    }

    default <T extends Event> void addListener(Consumer<T> consumer, EventPriority priority) {

        this.addListener(consumer, priority, false);
    }

    default <T extends Event> void addListener(Consumer<T> consumer, EventPriority priority, boolean receiveCancelled) {

        EVENTS.add(new EventStorage<>(consumer, priority, receiveCancelled));
    }

    class EventStorage<T extends Event> {

        private final Consumer<T> event;
        private final EventPriority priority;
        private final boolean receiveCancelled;
        private boolean active;

        EventStorage(Consumer<T> consumer, EventPriority priority, boolean receiveCancelled) {

            this.event = consumer;
            this.priority = priority;
            this.receiveCancelled = receiveCancelled;
        }

        void register() {

            if (this.isActive(true)) {

                MinecraftForge.EVENT_BUS.addListener(this.priority, this.receiveCancelled, this.event);
            }
        }

        void unregister() {

            if (this.isActive(false)) {

                MinecraftForge.EVENT_BUS.unregister(this.event);
            }
        }

        private boolean isActive(boolean state) {

            if (this.active != state) {

                this.active = state;
                return true;
            }

            return false;
        }
        
    }

}
