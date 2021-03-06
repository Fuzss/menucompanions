package com.fuzs.menucompanions;

import com.fuzs.menucompanions.client.element.MenuEntityElement;
import com.fuzs.puzzleslib_mc.element.AbstractElement;
import com.fuzs.puzzleslib_mc.element.registry.ElementRegistry;
import com.fuzs.puzzleslib_mc.element.side.ISidedElement;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

public class MenuCompanionsElements extends ElementRegistry {

    public static final AbstractElement MENU_ENTITY = register("menu_entity", MenuEntityElement::new, Dist.CLIENT);

    /**
     * create overload so this class and its elements are loaded
     */
    public static void setup(String namespace) {

        ElementRegistry.setup(namespace);
    }

    /**
     * register an element to the namespace of the active mod container
     * @param key identifier for this element
     * @param supplier supplier for element to be registered
     * @return <code>element</code>
     * @param <T> make sure element also extends ISidedElement
     */
    private static <T extends AbstractElement & ISidedElement> AbstractElement register(String key, Supplier<T> supplier) {

        return register(key, supplier, FMLEnvironment.dist);
    }

    /**
     * register an element to the namespace of the active mod container
     * @param key identifier for this element
     * @param supplier supplier for element to be registered
     * @param dist physical side to register on
     * @return <code>element</code>
     * @param <T> make sure element also extends ISidedElement
     */
    private static <T extends AbstractElement & ISidedElement> AbstractElement register(String key, Supplier<T> supplier, Dist dist) {

        return register(getNamespace(), key, supplier, dist);
    }

    /**
     * easy access to modid
     * @return modid of this mod
     */
    private static String getNamespace() {

        return MenuCompanions.MODID;
    }

}
