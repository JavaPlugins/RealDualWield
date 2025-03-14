package beer.devs.realdualwield.api;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerOffhandDelayEvent extends Event implements Cancellable
{
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean isCancelled;
    private Player player;
    private ItemStack itemInOffhand;
    private Entity entity;

    public PlayerOffhandDelayEvent(PlayerInteractEntityEvent event)
    {
        this.player = event.getPlayer();
        this.isCancelled = false;
        this.itemInOffhand = event.getPlayer().getInventory().getItemInOffHand();
        this.entity = event.getRightClicked();
    }

    @Override
    public boolean isCancelled()
    {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean b)
    {
        this.isCancelled = b;
    }

    public static HandlerList getHandlerList()
    {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers()
    {
        return HANDLERS;
    }

    public ItemStack getItemInOffhand()
    {
        return itemInOffhand;
    }

    public Player getPlayer()
    {
        return player;
    }

    public void setPlayer(Player player)
    {
        this.player = player;
    }

    public Entity getEntity()
    {
        return entity;
    }
}