package beer.devs.realdualwield;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Random;

public class Utils
{
    static Random RANDOM = new Random();

    @SuppressWarnings("unused")
    public static float randomNumber(float a, float b)
    {
        return RANDOM.nextFloat() * (b - a) + a;
    }

    public static boolean isOffHand(PlayerInteractEntityEvent event)
    {
        return event.getHand() == EquipmentSlot.OFF_HAND;
    }

    public static boolean isOffHand(PlayerInteractEvent event)
    {
        return event.getHand() == EquipmentSlot.OFF_HAND;
    }

    public static boolean canInstaMine(Block block)
    {
        if (block == null)
            return false;
        return block.getType().getHardness() == 0;
    }

    public static boolean isASword(ItemStack item)
    {
        return item.getType().toString().contains("SWORD");
    }

    public static boolean canBreak(Block block, Player player)
    {
        BlockBreakEvent b = new BlockBreakEvent(block, player);
        Bukkit.getPluginManager().callEvent(b);
        boolean can = !b.isCancelled();
        b.setCancelled(true);
        return can;
    }

    @SuppressWarnings("removal")
    public static boolean canDamage(Player attacker, Entity damaged)
    {
        EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(attacker, damaged, EntityDamageEvent.DamageCause.ENTITY_ATTACK,
                new EnumMap<>(ImmutableMap.of(EntityDamageEvent.DamageModifier.BASE, 0.0D)),
                new EnumMap<EntityDamageEvent.DamageModifier, Function<? super Double, Double>>(ImmutableMap.of(EntityDamageEvent.DamageModifier.BASE, Functions.constant(-0.0D))));

        Bukkit.getPluginManager().callEvent(event);
        boolean canDamage = !event.isCancelled();
        event.setCancelled(true);
        return canDamage;
    }
}
