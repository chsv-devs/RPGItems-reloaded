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
package think.rpgitems.power.impl;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.I18n;
import think.rpgitems.item.RPGItem;
import think.rpgitems.power.*;

import java.util.Collections;

import static think.rpgitems.power.Utils.checkCooldown;

/**
 * Power consume.
 * <p>
 * The consume power will remove one item on click.
 * With {@link #cooldown cooldown} time (ticks).
 * </p>
 */
@PowerMeta(defaultTrigger = "RIGHT_CLICK")
public class PowerConsume extends BasePower implements PowerRightClick, PowerLeftClick, PowerSneak, PowerHitTaken, PowerHurt, PowerSprint, PowerAttachment {
    /**
     * Cooldown time of this power
     */
    @Property(order = 1)
    public int cooldown = 0;

    /**
     * Cost of this power
     */
    @Property
    public int cost = 0;

    @Property
    public boolean requireHurtByEntity = true;

    @Override
    public void init(ConfigurationSection section) {
        boolean isRight = section.getBoolean("isRight", true);
        triggers = Collections.singleton(isRight ? Trigger.RIGHT_CLICK : Trigger.LEFT_CLICK);
        super.init(section);
    }

    @Override
    public PowerResult<Void> rightClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> leftClick(final Player player, ItemStack stack, PlayerInteractEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
        return fire(player, stack);
    }

    @Override
    public PowerResult<Double> takeHit(Player target, ItemStack stack, double damage, EntityDamageEvent event) {
        if (!requireHurtByEntity || event instanceof EntityDamageByEntityEvent) {
            return fire(target, stack).with(damage);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> hurt(Player target, ItemStack stack, EntityDamageEvent event) {
        if (!requireHurtByEntity || event instanceof EntityDamageByEntityEvent) {
            return fire(target, stack);
        }
        return PowerResult.noop();
    }

    @Override
    public PowerResult<Void> attachment(Player player, ItemStack stack, RPGItem originItem, Event originEvent, ItemStack originStack) {
        return fire(player, stack);
    }

    public PowerResult<Void> fire(final Player player, ItemStack s) {
        if (!checkCooldown(this, player, cooldown, false, true)) return PowerResult.cd();
        if (!getItem().consumeDurability(s, cost)) return PowerResult.cost();
        int count = s.getAmount() - 1;
        if (count == 0) {
            s.setAmount(0);
            s.setType(Material.AIR);
        } else {
            s.setAmount(count);
        }

        return PowerResult.ok();
    }

    @Override
    public String getName() {
        return "consume";
    }

    @Override
    public String displayText() {
        return I18n.format("power.consume");
    }
}
