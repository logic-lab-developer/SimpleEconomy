package net.logic_lab.spigot.simpleeconomy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class SimpleEconomyCore extends JavaPlugin {

    private class SimpleEconomyListener implements Listener {

        @EventHandler
        public void onPlayerJoinEvent( PlayerJoinEvent event ){

            ItemStack item = new ItemStack( Material.APPLE, 1, (short)1 );
            ItemMeta meta = item.getItemMeta();

            meta.setDisplayName("オレのリンゴ");
            ArrayList<String> lore = new ArrayList<String>();
            lore.add(ChatColor.AQUA+"これはオレのためのリンゴだ");
            lore.add("たいせつにとっておこう");
            meta.setLore(lore);

            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE,ItemFlag.HIDE_ATTRIBUTES);



            item.setItemMeta(meta);
            event.getPlayer().getInventory().addItem( item );

            ItemStack hoe = new ItemStack( Material.WOOD_HOE, 1, (short)1 );
            ItemMeta meta2 = hoe.getItemMeta();
            meta2.setUnbreakable(true);
            meta2.addItemFlags(ItemFlag.HIDE_ATTRIBUTES,ItemFlag.HIDE_UNBREAKABLE);
            hoe.setItemMeta(meta2);
            event.getPlayer().getInventory().addItem(hoe);

        }

    }

    SimpleEconomyListener listener;

    @Override
    public void onEnable() {
        getLogger().info("SimpleEconomy loaded.");

        // イベントの登録
        listener = new SimpleEconomyListener();
        getServer().getPluginManager().registerEvents( listener, this );

    }

    @Override
    public void onDisable(){
        getLogger().info("SimpleEconomy disabled.");

        // イベントの抹消
        HandlerList.unregisterAll(listener);
    }



}
