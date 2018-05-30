package net.logic_lab.spigot.simpleeconomy;

import com.google.common.util.concurrent.RateLimiter;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
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

import java.sql.*;
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

    private String find_SQL = "SELECT COUNT(*) AS user_count FROM simpleeconomy_account WHERE uuid = ?";

    private String updata_SQL = "UPDATE simpleeconomy_account SET balance = ? , updated_at = NOW() WHERE uuid = ?";

    private String select_SQL = "SELECT balance FROM simpleeconomy_account WHERE uuid = ?";

    private String last_rate_SQL = "SELECT * FROM simpleeconomy_ratetable WHERE created_at > current_timestamp() - interval 1 hour ORDER BY id DESC LIMIT 1";

    private String insert_last_rate_SQL = "INSERT INTO simpleeconomy_ratetable(rate,created_at) VALUES(?,NOW())";

    // 口座作成
    private final String ACCOUNT_INSERT_SQL = "INSERT INTO simpleeconomy_account( name, uuid, balance, created_at, updated_at ) VALUES( ?, ?, ?, NOW(), NOW() )";

    private Boolean createAccount( Player player, int amount ){

        try {

            // 口座情報をInsertする
            PreparedStatement ps = connection.prepareStatement( ACCOUNT_INSERT_SQL );
            ps.setString( 1, player.getName() );
            ps.setString( 2, player.getUniqueId().toString() );
            ps.setInt( 3, amount );

            // SQLを実行して、結果をログ出力
            int rows = ps.executeUpdate();
            getLogger().info( "[Account Create] Player: " + player.getDisplayName() + ", Result: " + rows + " row(s)" );

        }
        catch ( SQLException e ){
            getLogger().warning( "[SQL Error] " + e.getMessage() );
            return false;
        }
        catch ( Exception e ){
            getLogger().warning( "[Unknown Error] " + e.getMessage() );
            return false;
        }

        return true;
    }

    private Boolean addMoney( Player player, int amount) {


        try {

            PreparedStatement ps = connection.prepareStatement(updata_SQL);
            ps.setInt( 1 , getBalance( player ) + amount );
            ps.setString( 2 , player.getUniqueId().toString() );
            int rs = ps.executeUpdate();

        }catch (SQLException e){

            getLogger().warning( "SQL error");

        }




        return false;
    }

    private Boolean subMoney( Player player, int amount ){

        try {

            PreparedStatement ps = connection.prepareStatement( updata_SQL );
            ps.setInt( 1, getBalance( player ) - amount );
            ps.setString( 2, player.getUniqueId().toString() );
            int rs = ps.executeUpdate();

        }catch (SQLException e){

            getLogger().warning( e.toString() );

        }

        return false;
    }

    private Boolean hasAccount( Player player ){

        // アカウントが存在するかのカウント
        int user_count;

        try {

            // 指定されたUUIDで検索するよ
            PreparedStatement ps = connection.prepareStatement( find_SQL );
            ps.setString( 1 , player.getUniqueId().toString() );
            ResultSet rs = ps.executeQuery();

            // COUNT(*)だから、レコードは絶対取れるはず。
            if( rs.next() ){
                user_count = rs.getInt( "user_count" );
            }
            else {
                throw new SQLException( "Couldn't read user_count field. SQL: " + find_SQL );
            }

        }
        catch ( SQLException e ){
            getLogger().warning( "[SQL Error] " + e.getMessage() );
            return false;
        }
        catch ( Exception e ){
            getLogger().warning( "[Unknown Error] " + e.getMessage() );
            return false;
        }

        // 1行でない = おかしい
        if( user_count != 1 ){
            getLogger().info( "[Account not found] Player: " + player.getDisplayName() );
            return false;
        }

        return true;
    }

    private int getBalance( Player player ){

        if ( hasAccount( player ) ){

            try {

                PreparedStatement ps = connection.prepareStatement( select_SQL );
                ps.setString( 1 , player.getUniqueId().toString() );
                ResultSet rs = ps.executeQuery();

                rs.next();

                return rs.getInt( "balance" );

            }catch (SQLException e) {

                getLogger().warning( "SQL error" );
                getLogger().warning( e.toString() );

            }


        }else {

            return -1;

        }

        return -1;

    }

    private int getLastRate(){


        try {

            PreparedStatement ps = connection.prepareStatement( last_rate_SQL );
            ResultSet rs = ps.executeQuery();

            if ( rs.next() ){

                return rs.getInt( "rate" );

            }
            else{

                int rate = 100;

                if ( insert_last_rate(rate) ){

                    return rate;

                }

            }

        }catch (SQLException e){

            getLogger().warning( "SQL error " + e.toString() );

        }

        return -1;

    }

    private boolean insert_last_rate( int rate ) {

        try {

            PreparedStatement ps = connection.prepareStatement( insert_last_rate_SQL );
            ps.setInt( 1 , rate );
            int rs = ps.executeUpdate();

            return true;


        }catch (SQLException e){

            getLogger().warning( "SQL error" );

            return false;

        }

    }

    private void setupTables(){

        HashMap<String,String> sqls = new HashMap<>();

        sqls.put("ratetable",

                "CREATE TABLE IF NOT EXISTS " + config.getTablePrefix() + "ratetable("
                        + "    id           INTEGER     NOT NULL AUTO_INCREMENT,"
                        + "    rate         INTEGER     NOT NULL,"
                        + "    created_at   DATETIME,"
                        + "    PRIMARY KEY(id)"
                        + ");"
        );

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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args ) {
        if ( sender instanceof Player ){

            Player player = (Player)sender;

            if ( cmd.getName().equalsIgnoreCase( "money" )){

                if ( args[0].equalsIgnoreCase( "give" ) ){

                    if ( args.length == 3 ){

                        Player target = getServer().getPlayer( args[1] );
                        getLogger().info( target.toString() );
                        if ( target == null ){

                            sender.sendMessage( ChatColor.RED + args[1] + "が見つかりませんでした" );
                            return false;

                        }

                        if ( hasAccount( target ) == false ){

                            player.sendMessage( ChatColor.RED + "相手の銀行口座が存在しませんでした" );

                            return false;

                        }

                        if ( getBalance( player ) < Integer.parseInt( args[2] ) ){

                            player.sendMessage( ChatColor.RED + "渡すお金が足りませんでした" );
                            player.sendMessage( ChatColor.RED + "" + getBalance( player ) + "円しか持っていません" );

                            return false;
                        }

                        addMoney( target , Integer.parseInt( args[2] ));
                        subMoney( player , Integer.parseInt( args[2] ));
                        target.sendMessage( ChatColor.YELLOW + "お金を" + player.getName() + "から" + Integer.parseInt( args[2] ) + "円受け取りました" );
                        player.sendMessage( ChatColor.YELLOW + "お金を" + target.getName() + "に" +  Integer.parseInt( args[2] ) + "円渡しました" );

                    }
                    else {
                        return false;
                    }

                }
                else if ( args[0].equalsIgnoreCase( "rate" ) ){

                    int Rate = getLastRate();

                    if ( Rate == -1 ){

                        player.sendMessage( ChatColor.RED + "内部エラーが発生しました" );

                        return false;

                    }else {

                        player.sendMessage( ChatColor.YELLOW + "現在のレート：" + getLastRate() + "" );

                        return true;

                    }

                }else if ( args[0].equalsIgnoreCase( "balance" )){

                    int balance = getBalance( player );

                    if ( balance == -1 ){

                        sender.sendMessage( ChatColor.RED + "内部エラーが発生しました。" );
                        return false;
                    }
                    else {

                        sender.sendMessage( ChatColor.YELLOW + "残高:" + balance );
                        return true;

                    }

                }else if ( args.length == 0 ){

                    player.sendMessage( ChatColor.YELLOW + "Moneyコマンドの使い方はMoney helpを実行して確認してください" );

                    return true;

                }else if ( args[0].equalsIgnoreCase( "help" ) ){

                    if ( args.length == 1 ){

                        player.sendMessage( ChatColor.YELLOW + "-----Money help-----" );
                        player.sendMessage( ChatColor.YELLOW + "/Money help - ヘルプの表示" );
                        player.sendMessage( ChatColor.YELLOW + "/Money give <お金を渡す相手の名前> <金額> - お金を相手に渡せます");
                        player.sendMessage( ChatColor.YELLOW + "/Money balance - 自分の所持金を確認します" );
                        player.sendMessage( ChatColor.YELLOW + "/Money rate [エメラルドの個数] - 現在のレートを確認します" );

                        return true;

                    }

                }
                else {

                    return false;

                }
            }


        }

        return false;

    }



}
