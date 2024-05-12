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
    public void onPotionInteraction(PlayerInteractEvent event) {
        ItemStack item = event.getItem();

        if (item != null && item.getType().toString().endsWith("_POTION")) {
            PotionMeta potionMeta = (PotionMeta) item.getItemMeta();

            if (checkPotionForBannedEffects(potionMeta)
                    && event.getAction().name().contains("RIGHT_CLICK")
                    && event.getHand() == EquipmentSlot.HAND) {

                event.getPlayer().sendMessage("You're not allowed to use this potion as it is banned from the server.");
                event.setCancelled(true);

                if (!event.getAction().name().contains("INTERACT")) {
                    removePotionEffects(event.getPlayer(), potionMeta);
                }
            }
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ItemStack potion = event.getEntity().getItem();
        PotionMeta potionMeta = (PotionMeta) potion.getItemMeta();
        if (checkPotionForBannedEffects(potionMeta)) {
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

            ItemStack potionItem = potion.getItem();

            PotionMeta potionMeta = (PotionMeta) potionItem.getItemMeta();

            if (checkPotionForBannedEffects(potionMeta)) {
                event.setCancelled(true);
                removeLingeringCloud(event);

                for (Entity entity : cloud.getNearbyEntities(cloud.getRadius(), cloud.getRadius(), cloud.getRadius())) {
                    if (entity instanceof Player player) {
                        player.sendMessage("You were affected by a banned potion!");
                        removePotionEffects(player, potionMeta);
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
            entity.sendMessage("You're not allowed to use this potion as it is banned from the server.");
        }
    }

    private boolean checkPotionForBannedEffects(PotionMeta potionMeta) {
        if (potionMeta == null) return false;
        for (PotionEffect effect : potionMeta.getCustomEffects()) {
            if (plugin.getBannedEffects().contains(effect.getType())) {
                return true;
            }
        }

        PotionEffectType baseEffect = potionMeta.getBasePotionData().getType().getEffectType();
        return baseEffect != null && plugin.getBannedEffects().contains(baseEffect);
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
