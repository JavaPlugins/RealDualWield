package beer.devs.realdualwield;

import beer.devs.realdualwield.api.*;
import dev.lone.itemsadder.api.CustomBlock;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DualWielding implements Listener, CommandExecutor
{
    public static List<String> DUAL_WIELD_ENABLED_MATERIALS = new ArrayList<>();
    public static boolean DENY_LONGPRESS_RIGHTCLICK;
    public static boolean BREAK_PLANTS_BARE_HAND;

    private FileConfiguration config;

    private final HashMap<Player, Wielder> wielders = new HashMap<>();

    static final List<String> COOLDOWN_ANIM;
    static
    {
        COOLDOWN_ANIM = new ArrayList<>();
        @SuppressWarnings("UnnecessaryUnicodeEscape")
        String DOT = "\u00B7"; // MIDDLE DOT
        COOLDOWN_ANIM.add(ChatColor.GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT);
        COOLDOWN_ANIM.add(ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT);
        COOLDOWN_ANIM.add(ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT);
        COOLDOWN_ANIM.add(ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT);
        COOLDOWN_ANIM.add(ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT);
        COOLDOWN_ANIM.add(ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.DARK_GRAY + DOT + ChatColor.DARK_GRAY + DOT);
        COOLDOWN_ANIM.add(ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.DARK_GRAY + DOT);
        COOLDOWN_ANIM.add(ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT + ChatColor.GRAY + DOT);
    }

    public DualWielding()
    {
        loadConfiguration();
        initConfig();

        Bukkit.getServer().getPluginManager().registerEvents(this, Main.inst);

        Main.inst.getCommand("rdwreload").setExecutor(this);
    }

    void loadConfiguration()
    {
        Main.inst.getConfig().options().copyDefaults(true);
        Main.inst.saveConfig();
        Main.inst.getConfig().options().copyDefaults(false);
    }

    void initConfig()
    {
        config = Main.inst.getConfig();
        DUAL_WIELD_ENABLED_MATERIALS = config.getStringList("dual_wield_enabled.materials");
        DENY_LONGPRESS_RIGHTCLICK = config.getBoolean("deny-longpress-rightclick");
        BREAK_PLANTS_BARE_HAND = config.getBoolean("break_plants_bare_hand");
    }

    private Wielder getPlayerData(Player player)
    {
        return wielders.computeIfAbsent(player, Wielder::new);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String[] args)
    {
        if (cmd.getName().equalsIgnoreCase("rdwreload"))
        {
            if (sender.hasPermission("rdw.reload"))
            {
                Main.inst.reloadConfig();
                initConfig();
                sender.sendMessage(ChatColor.GREEN + "[RealDualWield] Reloaded config.");
            }
        }
        return true;
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent e)
    {
        wielders.remove(e.getPlayer());
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent e)
    {
        if(Utils.isOffHand(e))
            return;

        if (!(e.getRightClicked() instanceof LivingEntity damaged) || !e.getPlayer().hasPermission("rdw.use"))
            return;

        Player player = e.getPlayer();
        Wielder wielder = getPlayerData(player);
        if (config.getBoolean("deny-longpress-rightclick") && wielder.isHoldingRightClick())
            return;

        ItemStack weapon = e.getPlayer().getInventory().getItemInOffHand();
        if (damaged.isInvulnerable() || damaged.isDead() || !Utils.canDamage(player, damaged) || isIgnorable(damaged) || !isEnabled(weapon))
            return;

        wielder.setTimeNow();

        @Nullable Integer delay = wielder.getDelay();
        boolean critical = !player.isOnGround() && player.getFallDistance() > 0.0F && !player.hasPotionEffect(PotionEffectType.BLINDNESS) && player.getVehicle() == null;
        if (Events.call(new PlayerOffhandAnimationEvent(e)))
        {
            if(critical)
            {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 10.0F, 1.0F);
                player.spawnParticle(Particle.CRIT, damaged.getLocation().getX(), damaged.getLocation().getY() + 1, damaged.getLocation().getZ(), 10, 0.5, 0.5, 0.5);
            }
            else
            {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 10.0F, 1.0F);

                wielder.offhandAnimation();

                if (Utils.isASword(weapon))
                {
                    Location particleLoc = player.getLocation().toVector().add(player.getLocation().getDirection().multiply(1.5f)).toLocation(player.getWorld());
                    particleLoc.setY(particleLoc.getY() + player.getEyeHeight());
                    player.spawnParticle(Particle.SWEEP_ATTACK, particleLoc.getX(), particleLoc.getY(), particleLoc.getZ(), 1, 0, 0, 0);
                    player.spawnParticle(Particle.DAMAGE_INDICATOR, damaged.getLocation().getX(), damaged.getLocation().getY(), damaged.getLocation().getZ() + 0.5f, (int) Utils.randomNumber(1, 4), 0.1, 0.1, 1, 0.2);
                }
            }
        }

        if (Events.call(new PlayerOffhandReduceDurabilityEvent(e)))
        {
            int maxDurability = weapon.getType().getMaxDurability();
            if(weapon.getItemMeta() != null && !weapon.getItemMeta().isUnbreakable() && weapon.getType().getMaxDurability() != 0)
            {
                int remainingDurability = maxDurability - weapon.getDurability();
                if (remainingDurability - 1 > 0)
                    weapon.setDurability((short) (weapon.getDurability() + 1));
                else
                {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 10.0F, 1.0F);
                    player.getInventory().setItemInOffHand(null);
                }
            }
        }

        PlayerDamageEntityWithOffhandEvent apiDamageEvent = new PlayerDamageEntityWithOffhandEvent(e, getDamage(weapon, player, damaged, critical, delay));
        if (Events.call(apiDamageEvent))
        {
            if (!damaged.isInvulnerable())
            {
                float multiply = 0.5f;
                float height = 0.5f;
                if (weapon.containsEnchantment(Enchantment.KNOCKBACK))
                {
                    multiply += weapon.getEnchantmentLevel(Enchantment.KNOCKBACK) * 0.5f;
                    height += weapon.getEnchantmentLevel(Enchantment.KNOCKBACK) * 0.3f;
                }

                boolean isOnHair = damaged.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR;

                if (config.getBoolean("can-attack-mob-in-air") && isOnHair)
                {
                    Vector direction = player.getLocation().getDirection().multiply(multiply);
                    direction.setY(direction.getY() + height);
                    damaged.setVelocity(direction);
                }
                else if (!config.getBoolean("can-attack-mob-in-air") && !isOnHair)
                {
                    Vector direction = player.getLocation().getDirection().multiply(multiply);
                    direction.setY(direction.getY() + height);
                    damaged.setVelocity(direction);
                }


                if (weapon.containsEnchantment(Enchantment.FIRE_ASPECT))
                {
                    damaged.setFireTicks(80 * weapon.getEnchantmentLevel(Enchantment.FIRE_ASPECT));
                }

                double damage = apiDamageEvent.getDamage();
                damaged.damage(damage, player);
            }
        }

        if (Events.call(new PlayerOffhandDelayEvent(e)))
        {
            if (!wielder.isUsingLeftWeapon())
            {
                wielder.setUsingLeftWeapon(true);
                playCooldownAnimation(wielder);
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e)
    {
        if(Utils.isOffHand(e))
            return;

        Player player = e.getPlayer();
        Block block = e.getClickedBlock();
        Wielder wielder = getPlayerData(player);
        wielder.setTimeNow();

        if(!e.isCancelled())
        {
            if (config.getBoolean("break-plants"))
                wielder.instamine(block, e.getItem());
        }

        // Handle blocks interaction particle.
        if (!player.hasPermission("rdw.use") || wielder.isUsingLeftWeapon())
            return;

        if (config.getBoolean("deny-longpress-rightclick") && e.getAction().equals(Action.RIGHT_CLICK_AIR) && wielder.isHoldingRightClick())
            return;

        if (!Events.call(new PlayerOffhandAnimationEvent(e)))
            return;

        ItemStack itemMainHand = player.getInventory().getItemInMainHand();
        ItemStack itemOffHand = player.getInventory().getItemInOffHand();

        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(Action.RIGHT_CLICK_AIR))
        {
            //noinspection ConstantValue
            if(itemMainHand.getType().isBlock() || (Main.HAS_ITEMSADDER && CustomBlock.byItemStack(itemMainHand) != null))
                return;

            if (isEnabled(itemOffHand) && !wielder.isUsingLeftWeapon())
            {
                wielder.offhandAnimation();

                if (player.getGameMode() == GameMode.ADVENTURE)
                    return;

                if (block == null)
                    return;

                Block target = block.getRelative(e.getBlockFace());
                Location loc = target.getLocation();

                Vector offset = new Vector(0, 0, 0);
                switch (e.getBlockFace())
                {
                    case UP:
                        loc.setZ(loc.getZ() + 0.5f);
                        loc.setX(loc.getX() + 0.5f);
                        offset.setZ(0.3f);
                        offset.setX(0.3f);

                        break;
                    case EAST:
                        loc.setZ(loc.getZ() + 0.5f);
                        loc.setY(loc.getY() + 0.5f);
                        offset.setZ(0.3f);
                        offset.setY(0.3f);
                        break;
                    case WEST:
                        loc.setZ(loc.getZ() + 0.5f);
                        loc.setX(loc.getX() + 1f);
                        loc.setY(loc.getY() + 0.5f);
                        offset.setZ(0.3f);
                        offset.setX(0.3f);
                        offset.setY(0.3f);
                        break;
                    case NORTH:
                        loc.setZ(loc.getZ() + 1f);
                        loc.setX(loc.getX() + 0.5f);
                        loc.setY(loc.getY() + 0.5f);
                        offset.setZ(0.3f);
                        offset.setX(0.3f);
                        offset.setY(0.3f);
                        break;
                    case SOUTH:
                        loc.setZ(loc.getZ());
                        loc.setX(loc.getX() + 0.5f);
                        loc.setY(loc.getY() + 0.5f);
                        offset.setZ(0.3f);
                        offset.setX(0.3f);
                        offset.setY(0.3f);
                        break;
                    case DOWN:
                        loc.setZ(loc.getZ() + 0.5f);
                        loc.setX(loc.getX() + 0.5f);
                        loc.setY(loc.getY() + 1f);
                        offset.setZ(0.3f);
                        offset.setX(0.3f);
                        offset.setY(0.3f);
                        break;
                }

                player.spawnParticle(Particle.BLOCK, loc.getX(), loc.getY(), loc.getZ(), 2, offset.getX(), offset.getY(), offset.getZ(), block.getType().createBlockData());
            }
        }
    }

    private void playCooldownAnimation(Wielder wielder)
    {
        if (wielder.getDelay() != null)
            return;

        new BukkitRunnable()
        {
            int count = 0;
            int animIndex = 0;

            public void run()
            {
                count += 1;
                // 1 : 12 = x : dualWieldDelays.get(player)
                // dualWieldDelays.get(player) * 1 / 12
                if (12 - count > 0)
                    wielder.setDelay(12 - count);

                if (count <= 8)
                {
                    if (config.getBoolean("show-cooldown-bar"))
                    {
                        wielder.getPlayer().sendTitle(" ", COOLDOWN_ANIM.get(animIndex), 0, 500, 0);
                        animIndex++;
                    }
                }
                else if (count > 12)
                {
                    if (config.getBoolean("show-cooldown-bar"))
                    {
                        wielder.getPlayer().sendTitle(" ", " ", 0, 500, 0);
                    }

                    wielder.setUsingLeftWeapon(false);
                    wielder.setDelay(null);
                    this.cancel();
                }
            }
        }.runTaskTimerAsynchronously(Main.inst, 0, 1);
    }

    boolean isEnabled(ItemStack offHand)
    {
        if (offHand == null || offHand.getType() == Material.AIR)
            return false;

        return DUAL_WIELD_ENABLED_MATERIALS.contains(offHand.getType().toString());
    }

    boolean isIgnorable(Entity entity)
    {
        return entity.getType() == EntityType.ARMOR_STAND || entity.getType() == EntityType.ITEM_FRAME;
    }

    public static double getDamage(ItemStack item, Player player, LivingEntity target, boolean critical)
    {
        double damage = getMaterialAttackDamage(item.getType());
        if (item.getItemMeta() != null && item.getItemMeta().getAttributeModifiers() != null)
        {
            for (Map.Entry<Attribute, AttributeModifier> entry : item.getItemMeta().getAttributeModifiers().entries())
            {
                if (entry.getKey() == Attribute.ATTACK_DAMAGE)
                {
                    AttributeModifier.Operation operation = entry.getValue().getOperation();
                    if (operation.equals(AttributeModifier.Operation.ADD_NUMBER))
                        damage += entry.getValue().getAmount();
                    else if (operation.equals(AttributeModifier.Operation.ADD_SCALAR))
                        damage += entry.getValue().getAmount() * 1.6;
                    else if (operation.equals(AttributeModifier.Operation.MULTIPLY_SCALAR_1))
                        damage *= entry.getValue().getAmount();
                }
            }
        }

        if (player.hasPotionEffect(PotionEffectType.STRENGTH))
        {
            Collection<PotionEffect> pe = player.getActivePotionEffects();
            for (PotionEffect effect : pe)
            {
                if (effect.getType().equals(PotionEffectType.STRENGTH))
                {
                    if (effect.getAmplifier() == 0)
                        damage += 3;
                    if (effect.getAmplifier() == 1)
                        damage += 6;
                }
            }
        }

        if (item.containsEnchantment(Enchantment.SHARPNESS))
        {
            float damageAllValue = 1;
            if (item.getEnchantmentLevel(Enchantment.SHARPNESS) > 1)
                damageAllValue += (item.getEnchantmentLevel(Enchantment.SHARPNESS) - 1) * 0.5f;
            damage += damageAllValue;
        }

        if (critical)
            damage *= 1.5;

        double armorPoints = target.getAttribute(Attribute.ARMOR).getValue();
        double armorToughness = target.getAttribute(Attribute.ARMOR_TOUGHNESS).getValue();
        double reducedDamage = damage * (1 - Math.min(20.0, armorPoints / (5 + armorToughness / 2)) / 25.0);

        if (target.hasPotionEffect(PotionEffectType.RESISTANCE))
        {
            int resistanceLevel = target.getPotionEffect(PotionEffectType.RESISTANCE).getAmplifier();
            reducedDamage *= 1 - (0.2 * (resistanceLevel + 1));
        }

        return Math.max(reducedDamage, 0);
    }

    static double getMaterialAttackDamage(Material material)
    {
        return switch (material)
        {
            case NETHERITE_SWORD -> 8;
            case NETHERITE_AXE, DIAMOND_SWORD, GOLDEN_AXE, WOODEN_AXE -> 7;
            case NETHERITE_HOE, NETHERITE_SHOVEL, NETHERITE_PICKAXE, IRON_SWORD -> 6;
            case WOODEN_SWORD, GOLDEN_SWORD, IRON_PICKAXE -> 4;
            case STONE_SWORD, DIAMOND_PICKAXE -> 5;
            case WOODEN_SHOVEL, GOLDEN_SHOVEL -> 2.5d;
            case STONE_SHOVEL -> 3.5;
            case IRON_SHOVEL -> 4.5d;
            case DIAMOND_SHOVEL -> 5.5d;
            case WOODEN_PICKAXE, GOLDEN_PICKAXE -> 2;
            case STONE_PICKAXE -> 3;
            case STONE_AXE, IRON_AXE, DIAMOND_AXE, TRIDENT -> 9;
            case WOODEN_HOE, GOLDEN_HOE, STONE_HOE, IRON_HOE, DIAMOND_HOE -> 1;
            // HAND
            default -> 1;
        };
    }

    static double getDamage(ItemStack item, Player player, LivingEntity damaged, boolean critical, @Nullable Integer delay)
    {
        if (delay != null)
            return getDamage(item, player, damaged, critical) * (delay * 1.0f / 12.0f);
        return getDamage(item, player, damaged, critical);
    }
}
