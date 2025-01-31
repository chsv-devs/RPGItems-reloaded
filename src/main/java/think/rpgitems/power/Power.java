/*
 *  This file is part of RPG Items.
 *
 *  RPG Items is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RPG Items is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RPG Items.  If not, see <http://www.gnu.org/licenses/>.
 */
package think.rpgitems.power;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import think.rpgitems.Plugin;
import think.rpgitems.data.RPGValue;
import think.rpgitems.item.RPGItem;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Base class for all powers
 */
public abstract class Power implements Serializable {

    /**
     * Power by name, and name by power
     */
    public static BiMap<String, Class<? extends Power>> powers = HashBiMap.create();

    /**
     * Usage count by power name
     */
    public static Multiset<String> powerUsage = HashMultiset.create();

    /**
     * Item it belongs to
     */
    public RPGItem item;

    /**
     * Placeholder
     */
    public Power() {

    }

    static {
        Power.powers.put("aoe", PowerAOE.class);
        Power.powers.put("arrow", PowerArrow.class);
        Power.powers.put("tntcannon", PowerTNTCannon.class);
        Power.powers.put("rainbow", PowerRainbow.class);
        Power.powers.put("flame", PowerFlame.class);
        Power.powers.put("lightning", PowerLightning.class);
        Power.powers.put("command", PowerCommand.class);
        Power.powers.put("potionhit", PowerPotionHit.class);
        Power.powers.put("teleport", PowerTeleport.class);
        Power.powers.put("fireball", PowerFireball.class);
        Power.powers.put("ice", PowerIce.class);
        Power.powers.put("knockup", PowerKnockup.class);
        Power.powers.put("potionself", PowerPotionSelf.class);
        Power.powers.put("consume", PowerConsume.class);
        Power.powers.put("unbreakable", PowerUnbreakable.class);
        Power.powers.put("rescue", PowerRescue.class);
        Power.powers.put("rumble", PowerRumble.class);
        Power.powers.put("skyhook", PowerSkyHook.class);
        Power.powers.put("potiontick", PowerPotionTick.class);
        Power.powers.put("food", PowerFood.class);
        Power.powers.put("lifesteal", PowerLifeSteal.class);
        Power.powers.put("torch", PowerTorch.class);
        Power.powers.put("fire", PowerFire.class);
        Power.powers.put("projectile", PowerProjectile.class);
        Power.powers.put("deathcommand", PowerDeathCommand.class);
        Power.powers.put("forcefield", PowerForceField.class);
        Power.powers.put("attract", PowerAttract.class);
        Power.powers.put("color", PowerColor.class);
        Power.powers.put("pumpkin", PowerPumpkin.class);
        Power.powers.put("particle", PowerParticle.class);
        Power.powers.put("particletick", PowerParticleTick.class);
        Power.powers.put("delayedcommand", PowerDelayedCommand.class);
        Power.powers.put("lorefilter", PowerLoreFilter.class);
        Power.powers.put("commandhit", PowerCommandHit.class);
        Power.powers.put("tippedarrow", PowerTippedArrow.class);
        Power.powers.put("consumehit", PowerConsumeHit.class);
        Power.powers.put("aoecommand", PowerAOECommand.class);
        Power.powers.put("ranged", PowerRanged.class);
        Power.powers.put("rangedonly", PowerRangedOnly.class);
        Power.powers.put("deflect", PowerDeflect.class);
        Power.powers.put("realdamage", PowerRealDamage.class);
        Power.powers.put("selector", PowerSelector.class);
        Power.powers.put("noimmutabletick", PowerNoImmutableTick.class);
        Power.powers.put("stuck", PowerStuck.class);
        Power.powers.put("shulkerbullet", PowerShulkerBullet.class);
        Power.powers.put("throw", PowerThrow.class);
        Power.powers.put("repair", PowerRepair.class);
        Power.powers.put("sound", PowerSound.class);
    }

    /**
     * Gets entities in cone.
     *
     * @param entities  List of nearby entities
     * @param startPos  starting position
     * @param degrees   angle of cone
     * @param direction direction of the cone
     * @return All entities inside the cone
     */
    public static List<LivingEntity> getEntitiesInCone(List<LivingEntity> entities, org.bukkit.util.Vector startPos, double degrees, org.bukkit.util.Vector direction) {
        List<LivingEntity> newEntities = new ArrayList<>();
        for (LivingEntity e : entities) {
            org.bukkit.util.Vector relativePosition = e.getEyeLocation().toVector();
            relativePosition.subtract(startPos);
            if (getAngleBetweenVectors(direction, relativePosition) > degrees) continue;
            newEntities.add(e);
        }
        return newEntities;
    }

    public static boolean checkCooldownByString(Player player, RPGItem item, String command, long cooldownTime, boolean showWarn) {
        long cooldown;
        RPGValue value = RPGValue.get(player, item, "command." + command + ".cooldown");
        long nowTick = System.currentTimeMillis() / 50;
        if (value == null) {
            cooldown = nowTick;
            value = new RPGValue(player, item, "command." + command + ".cooldown", cooldown);
        } else {
            cooldown = value.asLong();
        }
        if (cooldown <= nowTick) {
            value.set(nowTick + cooldownTime);
            return true;
        } else {
            if (showWarn)
                player.sendMessage(ChatColor.AQUA + String.format(think.rpgitems.data.Locale.get("message.cooldown"), ((double) (cooldown - nowTick)) / 20d));
            return false;
        }
    }

    /**
     * Get nearby entities entity [ ].
     *
     * @param l      the l
     * @param radius the radius
     * @return the entity [ ]
     */
    public List<Entity> getNearbyEntities(Location l, Player player, double radius) {
        return getNearbyEntities(l, player, radius, radius, radius, radius);
    }

    /**
     * Get nearby entities entity [ ].
     *
     * @param l      the l
     * @param dx     the dx
     * @param dy     the dy
     * @param dz     the dz
     * @param radius the radius
     * @return the entity [ ]
     */
    public List<Entity> getNearbyEntities(Location l, Player player, double dx, double dy, double dz, double radius) {
        List<Entity> entities = new ArrayList<>();
        for (Entity e : l.getWorld().getNearbyEntities(l, dx, dy, dz)) {
            try {
                if (l.distance(e.getLocation()) <= radius) {
                    entities.add(e);
                }
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
        item.powers.stream().filter(power -> power instanceof PowerSelector).forEach(
                selector -> {
                    if (((PowerSelector) selector).canApplyTo(getClass())) {
                        ((PowerSelector) selector).inPlaceFilter(player, entities);
                    }
                }
        );
        return entities;
    }

    /**
     * Gets angle between vectors.
     *
     * @param v1 the v 1
     * @param v2 the v 2
     * @return the angle between vectors
     */
    public static float getAngleBetweenVectors(org.bukkit.util.Vector v1, org.bukkit.util.Vector v2) {
        return Math.abs((float) Math.toDegrees(v1.angle(v2)));
    }

    /**
     * Get nearest living entities living entity [ ].
     *
     * @param l      the l
     * @param radius the radius
     * @param min    the min
     * @return the living entity [ ]
     */
    public List<LivingEntity> getNearestLivingEntities(Location l, Player player, double radius, double min) {
        final java.util.List<java.util.Map.Entry<LivingEntity, Double>> entities = new java.util.ArrayList<>();
        for (Entity e : getNearbyEntities(l, player, radius)) {
            try {
                if (e instanceof LivingEntity && !player.equals(e)) {
                    double d = l.distance(e.getLocation());
                    if (d <= radius && d >= min) {
                        entities.add(new AbstractMap.SimpleImmutableEntry<>((LivingEntity) e, d));
                    }
                }
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
        List<LivingEntity> entity = new ArrayList<>();
        entities.sort(Comparator.comparing(java.util.Map.Entry::getValue));
        entities.forEach((k) -> entity.add(k.getKey()));
        return entity;
    }

    public static void AttachPermission(Player player, String permission) {
        if (permission.length() != 0 && !permission.equals("*")) {
            PermissionAttachment attachment = player.addAttachment(Plugin.plugin, 1);
            String[] perms = permission.split("\\.");
            StringBuilder p = new StringBuilder();
            for (String perm : perms) {
                p.append(perm);
                attachment.setPermission(p.toString(), true);
                p.append('.');
            }
        }
    }

    /**
     * Loads configuration for this power
     *
     * @param s Configuration
     */
    public abstract void init(ConfigurationSection s);

    /**
     * Saves configuration for this power
     *
     * @param s Configuration
     */
    public abstract void save(ConfigurationSection s);

    /**
     * Name of this power
     *
     * @return name
     */
    public abstract String getName();

    /**
     * Display text of this power
     *
     * @return Display text
     */
    public abstract String displayText();

    /**
     * Check cooldown boolean.
     *
     * @param p        the p
     * @param cdTicks  the cd ticks
     * @param showWarn whether to show warning to player
     * @return the boolean
     */
    protected final boolean checkCooldown(Player p, long cdTicks, boolean showWarn) {
        long cooldown;
        RPGValue value = RPGValue.get(p, item, getName() + ".cooldown");
        long nowTick = System.currentTimeMillis() / 50;
        if (value == null) {
            cooldown = nowTick;
            value = new RPGValue(p, item, getName() + ".cooldown", cooldown);
        } else {
            cooldown = value.asLong();
        }
        if (cooldown <= nowTick) {
            value.set(nowTick + cdTicks);
            return true;
        } else {
            if (showWarn)
                p.sendMessage(ChatColor.AQUA + String.format(think.rpgitems.data.Locale.get("message.cooldown"), ((double) (cooldown - nowTick)) / 20d));
            return false;
        }
    }
}
