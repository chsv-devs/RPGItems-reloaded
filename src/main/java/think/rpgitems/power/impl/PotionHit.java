package think.rpgitems.power.impl;

import java.util.Random;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import think.rpgitems.I18n;
import think.rpgitems.power.*;
import think.rpgitems.utils.PotionEffectUtils;

/**
 * Power potionhit.
 *
 * <p>On hit it will apply {@link #type effect} for {@link #duration} ticks at power {@link
 * #amplifier} with a chance of hitting of 1/{@link #chance}.
 */
@SuppressWarnings("WeakerAccess")
@Meta(
    defaultTrigger = "HIT",
    generalInterface = PowerLivingEntity.class,
    implClass = PotionHit.Impl.class)
public class PotionHit extends BasePower {

  @Property public int chance = 20;

  @Deserializer(PotionEffectUtils.class)
  @Serializer(PotionEffectUtils.class)
  @Property(order = 3, required = true)
  @AcceptedValue(preset = Preset.POTION_EFFECT_TYPE)
  public PotionEffectType type = PotionEffectType.HARM;

  @Property(order = 1)
  public int duration = 20;

  @Property(order = 2)
  public int amplifier = 1;

  private Random rand = new Random();

  /** Amplifier of potion effect */
  public int getAmplifier() {
    return amplifier;
  }

  /** Duration of potion effect */
  public int getDuration() {
    return duration;
  }

  @Override
  public String getName() {
    return "potionhit";
  }

  @Override
  public String displayText() {
    return I18n.formatDefault(
        "power.potionhit",
        (int) ((1d / (double) getChance()) * 100d),
        getType().getName().toLowerCase().replace('_', ' '));
  }

  /** Chance of triggering this power */
  public int getChance() {
    return chance;
  }

  /** Type of potion effect */
  public PotionEffectType getType() {
    return type;
  }

  public Random getRand() {
    return rand;
  }

  public static class Impl implements PowerHit<PotionHit>, PowerLivingEntity<PotionHit> {
    @Override
    public PowerResult<Double> hit(
        PotionHit power,
        Player player,
        ItemStack stack,
        LivingEntity entity,
        double damage,
        EntityDamageByEntityEvent event) {
      return fire(power, player, stack, entity, damage).with(damage);
    }

    @Override
    public PowerResult<Void> fire(
        PotionHit power, Player player, ItemStack stack, LivingEntity entity, Double value) {
      if (power.getRand().nextInt(power.getChance()) != 0) {
        return PowerResult.noop();
      }
      entity.addPotionEffect(
          new PotionEffect(power.getType(), power.getDuration(), power.getAmplifier()), true);
      return PowerResult.ok();
    }

    @Override
    public Class<? extends PotionHit> getPowerClass() {
      return PotionHit.class;
    }
  }
}
