package beer.devs.realdualwield;

import beer.devs.realdualwield.api.Events;
import beer.devs.realdualwield.api.PlayerOffhandPlantBreakEvent;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

class Wielder
{
    public final Player player;
    private long time;
    private boolean usingLeftWeapon;
    private @Nullable Integer delay;

    public Wielder(Player player)
    {
        this.player = player;
    }

    public long getTime()
    {
        return time;
    }

    public void setTime(long time)
    {
        this.time = time;
    }

    public void setTimeNow()
    {
        this.time = System.currentTimeMillis();
    }

    public boolean isUsingLeftWeapon()
    {
        return usingLeftWeapon;
    }

    public void setUsingLeftWeapon(boolean usingLeftWeapon)
    {
        this.usingLeftWeapon = usingLeftWeapon;
    }

    @Nullable
    public Integer getDelay()
    {
        return delay;
    }

    public void setDelay(@Nullable Integer delay)
    {
        this.delay = delay;
    }

    public Player getPlayer()
    {
        return player;
    }

    public boolean isHoldingRightClick()
    {
        if(!DualWielding.DENY_LONGPRESS_RIGHTCLICK)
            return false;

        return System.currentTimeMillis() - getTime() > 120;
    }

    public void instamine(Block block, ItemStack weapon)
    {
        if (player.getGameMode() == GameMode.ADVENTURE)
            return;

        if (player.getInventory().getItemInOffHand().getType() == Material.AIR)
        {
            if (DualWielding.BREAK_PLANTS_BARE_HAND)
                offhandAnimation();
            return;
        }

        if(!Utils.canInstaMine(block))
            return;
        if (!Utils.canBreak(block, player))
            return;

        if (!Events.call(new PlayerOffhandPlantBreakEvent(player, weapon, block)))
            return;

        Location loc = block.getLocation();
        loc.setX(loc.getX() + 0.5f);
        loc.setY(loc.getY() + 0.5f);
        loc.setZ(loc.getZ() + 0.5f);
        block.getWorld().spawnParticle(Particle.BLOCK, loc.getX(), loc.getY(), loc.getZ(), 30, 0.2f, 0.2f, 0.2f, block.getType().createBlockData());
        block.getWorld().playSound(loc, Sound.BLOCK_GRASS_BREAK, 1, 1);
        for (ItemStack drop : block.getDrops())
            block.getWorld().dropItemNaturally(block.getLocation(), drop);
        block.getLocation().getBlock().setType(Material.AIR);
    }

    public void offhandAnimation()
    {
        PacketContainer pack = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ANIMATION, false);

        pack.getEntityModifier(player.getWorld()).write(0, player);
        pack.getIntegers().write(1, 3);

        for (Player nearPlayer : player.getWorld().getPlayers())
        {
            if (nearPlayer.getWorld() != player.getWorld() || nearPlayer.getLocation().distance(player.getLocation()) > 32)
                continue;

            try
            {
                ProtocolLibrary.getProtocolManager().sendServerPacket(nearPlayer, pack);
            }
            catch (Throwable ignored) {}
        }
    }
}
