package net.meatwo310.examplemod.client.mdk.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig.Entry;
import net.meatwo310.examplemod.mdk.config.ConfigReadableNames;
import net.meatwo310.examplemod.mdk.config.ConfigScreenTranslations;
import net.meatwo310.examplemod.mdk.config.ConfigTranslationKeys;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.ConfigurationScreen.ConfigurationSectionScreen;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;
import net.neoforged.neoforge.common.ModConfigSpec.ListValueSpec;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class KeyedConfigScreen extends ConfigurationSectionScreen {
    public KeyedConfigScreen(Screen parent, ModConfig.Type type, ModConfig modConfig, Component title) {
        super(parent, type, modConfig, title);
    }

    private KeyedConfigScreen(
            Context parentContext,
            Screen parent,
            Map<String, Object> valueSpecs,
            String key,
            Set<? extends Entry> entrySet
    ) {
        super(
                Context.section(parentContext, parent, entrySet, valueSpecs, key),
                Component.translatable(ConfigTranslationKeys.BREADCRUMB_ORDER, parent.getTitle(), ConfigurationScreen.CRUMB_SEPARATOR, getTranslationComponent(parentContext, key))
        );
    }

    @Override
    protected @NonNull MutableComponent getTranslationComponent(@NonNull String key) {
        return getTranslationComponent(context, key);
    }

    @Override
    protected Element createSection(@NonNull String key, UnmodifiableConfig subconfig, @NonNull UnmodifiableConfig subsection) {
        if (subconfig.isEmpty()) {
            return null;
        }

        return new Element(getTranslationComponent(key),
                getTooltipComponent(key, null),
                buildSectionButton(key, subconfig, subsection),
                false
        );
    }

    @Override
    protected <T> Element createList(@NonNull String key, @NonNull ListValueSpec spec, @NonNull ConfigValue<List<T>> list) {
        return new Element(getTranslationComponent(key),
                getTooltipComponent(key, null),
                Button.builder(
                                Component.translatable(ConfigTranslationKeys.SECTION, Component.translatable(ConfigTranslationKeys.SECTION_TEXT)),
                                _ -> openList(key, spec, list)
                        )
                        .tooltip(Tooltip.create(getTooltipComponent(key, null)))
                        .width(Button.DEFAULT_WIDTH)
                        .build(),
                false
        );
    }

    private Button buildSectionButton(String key, UnmodifiableConfig subconfig, UnmodifiableConfig subsection) {
        return Button.builder(
                        Component.translatable(ConfigTranslationKeys.SECTION, Component.translatable(ConfigTranslationKeys.SECTION_TEXT)),
                        _ -> openSection(key, subconfig, subsection)
                )
                .tooltip(Tooltip.create(getTooltipComponent(key, null)))
                .width(Button.DEFAULT_WIDTH)
                .build();
    }

    @SuppressWarnings("deprecation")
    private void openSection(String key, UnmodifiableConfig subconfig, UnmodifiableConfig subsection) {
        minecraft.setScreen(sectionCache.computeIfAbsent(key, _ ->
                new KeyedConfigScreen(context, this, subconfig.valueMap(), key, subsection.entrySet()).rebuild()
        ));
    }

    private <T> void openList(String key, ListValueSpec spec, ConfigValue<List<T>> list) {
        var component = Component.translatable(
                ConfigTranslationKeys.BREADCRUMB_ORDER,
                this.getTitle(),
                ConfigurationScreen.CRUMB_SEPARATOR,
                getTranslationComponent(key)
        );

        minecraft.setScreen(sectionCache.computeIfAbsent(key, _ ->
                new KeyedConfigListScreen<>(Context.list(context, this), key, component, spec, list).rebuilt()
        ));
    }

    private static MutableComponent getTranslationComponent(Context context, String key) {
        String translationKey = ConfigScreenTranslations.getTranslationKey(context.modId(), context.modSpec(), context.valueSpecs(), context.keylist(), key);
        return I18n.exists(translationKey) ? Component.translatable(translationKey) : Component.literal(ConfigReadableNames.fromKey(key));
    }

    private static final class KeyedConfigListScreen<T> extends ConfigurationScreen.ConfigurationListScreen<T> {
        private KeyedConfigListScreen(Context context, String key, Component title, ListValueSpec spec, ConfigValue<List<T>> valueList) {
            super(context, key, title, spec, valueList);
        }

        @Override
        protected @NonNull MutableComponent getTranslationComponent(@NonNull String key) {
            return KeyedConfigScreen.getTranslationComponent(context, key);
        }

        private KeyedConfigListScreen<T> rebuilt() {
            rebuild();
            return this;
        }
    }
}
