package net.logic_lab.spigot.simpleeconomy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;

public class SimpleEconomyCore extends JavaPlugin {

    private Config config;

    private void loadConfig(){

        // 初期設定ファイルの保存
        saveDefaultConfig();

        // リロードの場合
        if( config != null ){
            reloadConfig();
        }

        // コンフィグの取得
        config = new Config(getConfig());

    }

    private class SimpleEconomyListener implements Listener {

        @EventHandler
        public void onPlayerInteractEvent( PlayerInteractEvent event ){
            getLogger().info( "PlayerInteractEvent" );
            if ( event.getAction() == Action.RIGHT_CLICK_BLOCK ){
                getLogger().info( event.getItem().toString() );
                ItemStack itemStack = event.getItem();
                if ( itemStack != null ){
                    if ( isMoney( itemStack ) ){
                        event.setCancelled( true );
                    }
                }
            }
        }

        @EventHandler
        public void  onPlayerJoin( PlayerJoinEvent event ){

            Player player = event.getPlayer();

            if ( !hasAccount( player ) ){

                boolean result = createAccount( player , 0 );

                if ( result == false ){

                    player.sendMessage( "銀行口座の作成に失敗しました。" );
                    player.sendMessage( "管理者にお知らせください。" );

                    getLogger().warning( "銀行口座作成に失敗しました。" );

                }

            }

        }

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

        private Boolean createAccount( Player player, int amount ){
            return false;
        }

        private Boolean addManey( Player player, int amount) {
            return false;
        }

        private Boolean subManey( Player player, int amount ){
            return false;
        }

        private Boolean hasAccount( Player player ){
            return false;
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

    private void setupTables(){

        HashMap<String,String> sqls = new HashMap<>();

        sqls.put("account",
                "CREATE TABLE IF NOT EXISTS " + config.getTablePrefix() + "account("
                        + "    id         INTEGER     NOT NULL AUTO_INCREMENT,"
                        + "    name       VARCHAR(40) NOT NULL,"
                        + "    uuid       VARCHAR(40) NOT NULL,"
                        + "    balance    INTEGER     NOT NULL DEFAULT 0,"
                        + "    created_at DATETIME,"
                        + "    updated_at DATETIME,"
                        + "    PRIMARY KEY(id),"
                        + "    UNIQUE INDEX uuid_uniq_idx(uuid)"
                        + ");"
        );

        try {

            for( String key : sqls.keySet() ){
                Statement statement = connection.createStatement();
                int count = statement.executeUpdate(sqls.get(key));
                if( count != 0 ){
                    getLogger().info( key + " table: OK result: " + count );
                }
                else {
                    getLogger().info( key + " table: NG" );
                }
            }

        }
        catch ( SQLException e ) {
            getLogger().warning( "Table create failed: " + e.getMessage() );
        }

    }


    SimpleEconomyListener listener;

    Connection connection;

    @Override
    public void onEnable() {

        this.loadConfig();

        getLogger().info("SimpleEconomy loaded.");

        // イベントの登録
        listener = new SimpleEconomyListener();
        getServer().getPluginManager().registerEvents( listener, this );

        if( connection == null ){
            getLogger().info("Try connect to MySQL server: " + config.getServerPort() );
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                connection = DriverManager.getConnection("jdbc:mysql://" + config.getServerPort() + "/" + config.getDatabaseName(), config.getDatabaseUser(), config.getDatabasePassword());
                getLogger().info("Connected to MySQL server.");
            }
            catch( InstantiationException | IllegalAccessException | ClassNotFoundException e ){
                getLogger().warning("Could not load JDBC driver.");
            }
            catch( SQLException e ){
                getLogger().warning("Could not connect MySQL server.");
            }
        }

        this.setupTables();

    }

    @Override
    public void onDisable(){
        getLogger().info("SimpleEconomy disabled.");

        if( connection != null ){
            try {
                getLogger().info("Disconnect from server.");
                connection.close();
                connection = null;
            }
            catch( SQLException e ){
                getLogger().warning("Failed to disconnect from server.");
            }
        }

        // イベントの抹消
        HandlerList.unregisterAll(listener);
    }



}
