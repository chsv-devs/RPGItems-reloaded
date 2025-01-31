package think.rpgitems.power;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import think.rpgitems.Events;
import think.rpgitems.Plugin;
import think.rpgitems.data.Locale;
import think.rpgitems.power.types.PowerRightClick;
import think.rpgitems.utils.ReflectionUtil;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Power projectile.
 * <p>
 * Launches projectile of type {@link #projectileType} with {@link #gravity} when right clicked.
 * If use {@link #cone} mode, {@link #amount} of projectiles will randomly distributed in the cone
 * with angle {@link #range} centered with player's direction.
 * </p>
 */
public class PowerProjectile extends Power implements PowerRightClick {
    /**
     * Z_axis.
     */
    private static final Vector z_axis = new Vector(0, 0, 1);
    /**
     * X_axis.
     */
    private static final Vector x_axis = new Vector(1, 0, 0);
    /**
     * Y_axis.
     */
    private static final Vector y_axis = new Vector(0, 1, 0);

    private Cache<UUID, Integer> burstTask = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).concurrencyLevel(2).build();
    /**
     * Cooldown time of this power
     */
    public long cooldownTime = 20;
    /**
     * Whether launch projectiles in cone
     */
    public boolean cone = false;
    /**
     * Whether the projectile have gravity
     */
    public boolean gravity = true;
    /**
     * Range will projectiles spread, in degree
     */
    public int range = 15;
    /**
     * Amount of projectiles
     */
    public int amount = 5;
    /**
     * Speed of projectiles
     */
    public double speed = 1;
    /**
     * Cost of this power
     */
    public int consumption = 1;
    /**
     * burst count of one shoot
     */
    public int burstCount = 1;
    /**
     * Interval between bursts
     */
    public int burstInterval = 1;

    /**
     * Whether to set Fireball' direction so it won't curve
     */
    public boolean setFireballDirection = false;

    public Double yield = null;

    public Boolean isIncendiary = null;

    private Class<? extends Projectile> projectileType = Snowball.class;

    @Override
    public void init(ConfigurationSection s) {
        cooldownTime = s.getLong("cooldownTime");
        setType(s.getString("projectileType"));
        cone = s.getBoolean("isCone");
        range = s.getInt("range");
        amount = s.getInt("amount");
        consumption = s.getInt("consumption", 1);
        speed = s.getDouble("speed", 1);
        gravity = s.getBoolean("gravity", true);
        burstCount = s.getInt("burstCount", 1);
        burstInterval = s.getInt("burstInterval", 1);
        setFireballDirection = s.getBoolean("setFireballDirection", false);
        yield = s.getDouble("yield", -1);
        if (yield < 0) yield = null;
        String inc = s.getString("isIncendiary", "null");
        if (inc.equals("null")) {
            isIncendiary = null;
        } else {
            isIncendiary = Boolean.getBoolean(inc);
        }
    }

    @Override
    public void save(ConfigurationSection s) {
        s.set("cooldownTime", cooldownTime);
        s.set("projectileType", getType());
        s.set("isCone", cone);
        s.set("range", range);
        s.set("amount", amount);
        s.set("consumption", consumption);
        s.set("speed", speed);
        s.set("gravity", gravity);
        s.set("burstCount", burstCount);
        s.set("burstInterval", burstInterval);
        s.set("setFireballDirection", setFireballDirection);
        s.set("yield", yield == null ? -1 : yield);
        s.set("isIncendiary", isIncendiary == null ? "null" : isIncendiary.toString());
    }

    /**
     * Gets type name
     *
     * @return Type name
     */
    public String getType() {
        if (projectileType == WitherSkull.class)
            return "skull";
        else if (projectileType == Fireball.class)
            return "fireball";
        else if (projectileType == SmallFireball.class)
            return "smallfireball";
        else if (projectileType == Arrow.class)
            return "arrow";
        else if (projectileType == LlamaSpit.class)
            return "llamaspit";
        else if (projectileType == ShulkerBullet.class)
            return "shulkerbullet";
        else if (projectileType == DragonFireball.class)
            return "dragonfireball";
        else
            return "snowball";
    }

    /**
     * Sets type from type name
     *
     * @param type Type name
     */
    public void setType(String type) {
        switch (type) {
            case "skull":
                projectileType = WitherSkull.class;
                break;
            case "fireball":
                projectileType = Fireball.class;
                break;
            case "smallfireball":
                projectileType = SmallFireball.class;
                break;
            case "arrow":
                projectileType = Arrow.class;
                break;
            case "llamaspit":
                projectileType = LlamaSpit.class;
                break;
            case "shulkerbullet":
                projectileType = ShulkerBullet.class;
                break;
            case "dragonfireball":
                projectileType = DragonFireball.class;
                break;
            default:
                projectileType = Snowball.class;
                break;
        }
    }

    /**
     * Check if the type is acceptable
     *
     * @param str Type name
     * @return If acceptable
     */
    public boolean acceptableType(String str) {
        return str.equals("skull") || str.equals("fireball") || str.equals("snowball") || str.equals("smallfireball") || str.equals("llamaspit") || str.equals("arrow") || str.equals("shulkerbullet") || str.equals("dragonfireball");
    }

    @Override
    public String getName() {
        return "projectile";
    }

    @Override
    public String displayText() {
        return ChatColor.GREEN + String.format(Locale.get(cone ? "power.projectile.cone" : "power.projectile"), getType(), (double) cooldownTime / 20d);
    }

    @Override
    public void rightClick(Player player, ItemStack stack, Block clicked) {
        if (!item.checkPermission(player, true)) return;
        if (!checkCooldown(player, cooldownTime, true)) return;
        if (!item.consumeDurability(stack, consumption)) return;
        fire(player);
        UUID uuid = player.getUniqueId();
        if (burstCount > 1) {
            Integer prev = burstTask.getIfPresent(uuid);
            if (prev != null) {
                Bukkit.getScheduler().cancelTask(prev);
            }
            BukkitTask bukkitTask = (new BukkitRunnable() {
                int count = burstCount - 1;

                @Override
                public void run() {
                    if (player.getInventory().getItemInMainHand().equals(stack)) {
                        burstTask.put(uuid, this.getTaskId());
                        if (count-- > 0) {
                            fire(player);
                            return;
                        }
                    }
                    burstTask.invalidate(uuid);
                    this.cancel();
                }
            }).runTaskTimer(Plugin.plugin, burstInterval, burstInterval);
            burstTask.put(uuid, bukkitTask.getTaskId());
        }
    }

    private void fire(Player player) {
        if (!cone) {
            Projectile projectile;
            Vector v = player.getEyeLocation().getDirection().multiply(speed);
            if (projectileType.isAssignableFrom(ShulkerBullet.class) && ReflectionUtil.getVersion().startsWith("v1_11_")) {
                projectile = player.getWorld().spawn(player.getEyeLocation(), ShulkerBullet.class);
                projectile.setShooter(player);
                projectile.setVelocity(v);
            } else {
                projectile = player.launchProjectile(projectileType, v);
            }
            handleProjectile(v, projectile);
        } else {
            Vector loc = player.getEyeLocation().getDirection();
            range = Math.abs(range) % 360;
            double phi = range / 180f * Math.PI;
            Vector a, b;
            Vector ax1 = loc.getCrossProduct(z_axis);
            if (ax1.length() < 0.01) {
                a = x_axis.clone();
                b = y_axis.clone();
            } else {
                a = ax1.normalize();
                b = loc.getCrossProduct(a).normalize();
            }
            for (int i = 0; i < amount; i++) {
                double z = range == 0 ? 1 : ThreadLocalRandom.current().nextDouble(Math.cos(phi), 1);
                double det = ThreadLocalRandom.current().nextDouble(0, 2 * Math.PI);
                double theta = Math.acos(z);
                Vector v = a.clone().multiply(Math.cos(det)).add(b.clone().multiply(Math.sin(det))).multiply(Math.sin(theta)).add(loc.clone().multiply(Math.cos(theta)));
                Projectile projectile;
                if (projectileType.isAssignableFrom(ShulkerBullet.class) && ReflectionUtil.getVersion().startsWith("v1_11_")) {
                    projectile = player.getWorld().spawn(player.getEyeLocation(), ShulkerBullet.class);
                    projectile.setShooter(player);
                    projectile.setVelocity(v.normalize().multiply(speed));
                } else {
                    projectile = player.launchProjectile(projectileType, v.normalize().multiply(speed));
                }
                handleProjectile(v, projectile);
            }
        }
    }

    private void handleProjectile(Vector v, Projectile projectile) {
        Events.rpgProjectiles.put(projectile.getEntityId(), item.getID());
        projectile.setGravity(gravity);
        if (projectile instanceof Explosive) {
            if (yield != null) {
                ((Explosive) projectile).setYield(yield.floatValue());
            }
            if (isIncendiary != null) {
                ((Explosive) projectile).setIsIncendiary(isIncendiary);
            }
        }
        if (projectile instanceof Fireball && setFireballDirection) {
            ((Fireball) projectile).setDirection(v.clone().normalize());
        }
        if (projectileType == Arrow.class)
            Events.removeArrows.add(projectile.getEntityId());
        if (!gravity) {
            Projectile finalProjectile = projectile;
            (new BukkitRunnable() {
                @Override
                public void run() {
                    finalProjectile.remove();
                }
            }).runTaskLater(Plugin.plugin, 80);
        }
    }

}
