package com.oorimea.potionban;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Material;
import org.bukkit.potion.PotionType;

import java.util.*;

public class PotionListener implements Listener {

    private final PotionBanPlugin plugin;
    private final Set<EntityType> ignoredEntities = new HashSet<>(Arrays.asList(EntityType.WITHER,
            EntityType.ELDER_GUARDIAN, EntityType.HUSK, EntityType.STRAY, EntityType.WITCH, EntityType.WARDEN));
    private final Set<PotionEffectType> ignoredEffects = new HashSet<>(Collections.singletonList(PotionEffectType.BAD_OMEN));

    public PotionListener(PotionBanPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPotionConsume(PlayerInteractEvent event) {
        if (event.getAction().name().contains("RIGHT_CLICK") &&
                event.getHand() == EquipmentSlot.HAND || event.getHand() == EquipmentSlot.OFF_HAND &&
                event.getItem() != null) {

            ItemStack item = event.getItem();
            Material potionType = item.getType();

            if (potionType == Material.POTION) {
                event.getPlayer().sendMessage("You are not allowed to consume this potion as it is banned from the server!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPotionThrow(PlayerInteractEvent event) {
        if (event.getAction().name().contains("RIGHT_CLICK") &&
                event.getHand() == EquipmentSlot.HAND &&
                event.getItem() != null) {

            ItemStack item = event.getItem();
            Material potionType = item.getType();

            if (potionType == Material.SPLASH_POTION) {
                event.getPlayer().sendMessage("You are not allowed to throw splash potions!");
                event.setCancelled(true);
            }
            if (potionType == Material.LINGERING_POTION) {
                event.getPlayer().sendMessage("You are not allowed to throw lingering potions!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ItemStack potion = event.getEntity().getItem();
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        if (checkPotionForBannedEffects(potionMeta.getBasePotionType())) {
            event.setCancelled(true);
            for (LivingEntity entity : event.getAffectedEntities()) {
                if (entity instanceof Player player) {
                    player.sendMessage("You were splashed with a banned potion!");
                }
                removePotionEffects(entity, potion);
            }
        }
    }

    @EventHandler
    public void onLingeringPotionSplash(LingeringPotionSplashEvent event) {
        AreaEffectCloud cloud = event.getAreaEffectCloud();
        if (!cloud.isDead()) {
            ThrownPotion potion = event.getEntity();

            if (!potion.getEffects().isEmpty()) {
                Optional<PotionEffect> firstEffect = potion.getEffects().stream().findFirst();
                PotionType potionType = PotionType.getByEffect(firstEffect.get().getType()); // Get PotionType from the effect

                if (checkPotionForBannedEffects(potionType)) {
                    event.setCancelled(true);
                    removeLingeringCloud(event);
                    for (Entity entity : cloud.getNearbyEntities(cloud.getRadius(), cloud.getRadius(), cloud.getRadius())) {
                        if (entity instanceof Player player) {
                            player.sendMessage("You were affected by a banned potion!");

                            player.removePotionEffect((PotionEffectType) potion.getEffects());
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPotionApply(EntityPotionEffectEvent event) {
        LivingEntity entity = (LivingEntity) event.getEntity();
        PotionEffect effect = event.getNewEffect();

        if (!(entity instanceof Player) || effect == null) return;

        EntityPotionEffectEvent.Cause cause = event.getCause();

        if (cause == EntityPotionEffectEvent.Cause.ATTACK || cause == EntityPotionEffectEvent.Cause.POTION_SPLASH ||
                cause == EntityPotionEffectEvent.Cause.AREA_EFFECT_CLOUD) {

            Entity applier = event.getEntity();
            EntityType applierType = applier.getType();

            if (ignoredEntities.contains(applierType)) {
                return;
            }
        }

        if (ignoredEffects.contains(effect.getType())) {
            return;
        }

        if (plugin.getBannedEffects().contains(effect.getType())) {
            event.setCancelled(true);
            entity.sendMessage("You are immune to the banned potion effect");
        }
    }

    private boolean checkPotionForBannedEffects(PotionType potionType) {
        if (potionType == null) return false;
        for (PotionEffect effect : potionType.getPotionEffects()) {
            if (plugin.getBannedEffects().contains(effect.getType())) {
                return true;
            }
        }
        PotionEffectType baseEffect = (PotionEffectType) potionType.getPotionEffects();
        return plugin.getBannedEffects().contains(baseEffect);
    }

    private void removePotionEffects(LivingEntity entity, ItemStack potion) {
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        if (potionMeta != null) {
            removePotionEffects(entity, potionMeta);
        }
    }

    private void removePotionEffects(LivingEntity entity, PotionMeta potionMeta) {
        for (PotionEffect effect : potionMeta.getCustomEffects()) {
            entity.removePotionEffect(effect.getType());
        }
        PotionEffectType baseEffect = potionMeta.getBasePotionData().getType().getEffectType();
        entity.removePotionEffect(baseEffect);
    }

    private void removeLingeringCloud(LingeringPotionSplashEvent event) {
        AreaEffectCloud cloud = event.getAreaEffectCloud();
        cloud.remove();
    }
}
