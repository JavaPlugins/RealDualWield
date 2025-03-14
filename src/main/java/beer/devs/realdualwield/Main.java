package beer.devs.realdualwield;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin
{
    static boolean HAS_ITEMSADDER = false;

    public static Main inst;

    @Override
    public void onEnable()
    {
        inst = this;

        if (getServer().getPluginManager().getPlugin("ItemsAdder") != null)
            HAS_ITEMSADDER = true;

        new DualWielding();
    }
}
