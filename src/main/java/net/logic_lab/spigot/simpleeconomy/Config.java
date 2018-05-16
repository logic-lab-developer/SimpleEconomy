package net.logic_lab.spigot.simpleeconomy;

import org.bukkit.configuration.file.FileConfiguration;

public class Config {

    private FileConfiguration config;

    public Config( FileConfiguration config ){
        this.config = config;
    }

    public String getServerPort(){
        return this.getServer() + ':' + this.getPort().toString();
    }

    public String getServer() {
        return config.getString( "mysql.server", "localhost" );
    }
    public Integer getPort() {
        return config.getInt( "mysql.port", 3306 );
    }
    public String getDatabaseName(){
        return config.getString( "mysql.database" );
    }
    public String getDatabaseUser(){
        return config.getString( "mysql.user", "root" );
    }
    public String getDatabasePassword(){
        return config.getString( "mysql.password", "" );
    }
    public String getTablePrefix(){
        return config.getString( "mysql.table_prefix", "" );
    }

}
