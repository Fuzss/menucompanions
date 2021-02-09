package com.fuzs.puzzleslib_mc.util;

import com.fuzs.menucompanions.MenuCompanions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * small helper methods
 */
public class PuzzlesLibUtil {

    /**
     * run an action, if an exception occurs run a different action
     * @param object object for actions
     * @param action action to attempt
     * @param orElse action in case <code>action</code> throws an exception
     * @param <T> type of object
     * @return was there an exception
     */
    public static <T> boolean runOrElse(@Nonnull T object, Consumer<T> action, Consumer<T> orElse) {

        try {

            action.accept(object);
        } catch (Exception e) {

            MenuCompanions.LOGGER.error("Unable to handle object {}: {}", object.getClass().getSimpleName(), e.getMessage());
            orElse.accept(object);

            return false;
        }

        return true;
    }

    /**
     * perform action for nullable object
     * @param object nullable object
     * @param action action to perform
     * @param <T> type of object
     */
    public static <T> void acceptIfPresent(@Nullable T object, Consumer<T> action) {

        Optional.ofNullable(object).ifPresent(action);
    }
    
}
