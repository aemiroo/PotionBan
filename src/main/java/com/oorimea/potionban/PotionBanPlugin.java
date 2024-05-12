package com.oorimea.potionban;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PotionBanPlugin extends JavaPlugin {

    private Set<PotionEffectType> bannedEffects = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        getServer().getPluginManager().registerEvents(new PotionListener(this), this);

        getLogger().info("Banned Effects: " + String.join(", ", getBannedEffects().stream().map(PotionEffectType::getName).toList()));
    }

    public void reloadConfig() {
        super.reloadConfig();
        bannedEffects = getConfig().getStringList("banned-effects").stream()
                .map(PotionEffectType::getByName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public Set<PotionEffectType> getBannedEffects() {
        return bannedEffects;
    }
}
