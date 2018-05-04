package net.logic_lab.spigot.simpleeconomy;

import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;

public class SimpleEconomyCore extends JavaPlugin {

    private class SimpleEconomyListener implements Listener {

        @EventHandler
        public void onPickupMoney( EntityPickupItemEvent event ){

            // プレイヤーでないなら終わり
            if( event.getEntity() instanceof Player == false ) return;

            // 拾ったアイテム
            Item pickupItem = event.getItem();

            // 木のクワでないなら終わり
            if( pickupItem.getItemStack().getType() != Material.WOOD_HOE ) return;

            // お金でないなら終わり
            if( !isMoney(pickupItem.getItemStack()) ) return;

            // 拾ったプレイヤー
            Player player = (Player)event.getEntity();

            // プレイヤーが木のクワを持っていなければ終わり
            if( !player.getInventory().contains(Material.WOOD_HOE) ) return;

            // 何個拾ったか
            int pickupStack = event.getItem().getItemStack().getAmount();

            for( ItemStack itemStack : player.getInventory().all(Material.WOOD_HOE).values() ) {

                ItemMeta meta = itemStack.getItemMeta();

                // 名前が設定されている
                if( meta.getLocalizedName() != null ){

                    // お金である
                    if( isMoney(itemStack) ){

                        // フルスタックでない
                        if( itemStack.getAmount() < 64 ){

                            int stack = 64 - itemStack.getAmount();

                            if( pickupStack < stack ){
                                itemStack.setAmount( itemStack.getAmount() + pickupStack );
                                pickupStack = 0;
                                break;
                            }

                            itemStack.setAmount( 64 );
                            pickupStack -= stack;
                        }

                    }

                }

            }

            if( pickupStack != 0 ){
                player.getInventory().addItem( createMoneyItem( pickupStack ) );
            }

            event.getItem().remove();
            event.setCancelled(true);
        }

        private Boolean isMoney( ItemStack itemStack ){
            ItemMeta meta = itemStack.getItemMeta();
            return meta.getLocalizedName() != null && meta.getLocalizedName().equals("money");
        }

        private ItemStack createMoneyItem( int amount ){

            ItemStack hoe = new ItemStack( Material.WOOD_HOE, amount, (short)1 );
            ItemMeta meta = hoe.getItemMeta();

            meta.setDisplayName("お金");
            ArrayList<String> lore = new ArrayList<String>();
            meta.setLore(lore);
            meta.setLocalizedName("money");

            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES,ItemFlag.HIDE_UNBREAKABLE);

            hoe.setItemMeta(meta);

            return hoe;
        }


        @EventHandler
        public void onPlayerJoinEvent( PlayerJoinEvent event ){

            // プレイヤーがログインした際にUUIDを取得し、
            // その人用の銀行口座を作る

            event.getPlayer().getInventory().addItem(createMoneyItem(64));

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
