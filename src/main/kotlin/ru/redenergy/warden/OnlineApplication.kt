package ru.redenergy.warden

import ch.jamiete.mcping.MinecraftPing
import ch.jamiete.mcping.MinecraftPingOptions
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.table.TableUtils
import ru.redenergy.warden.config.Server
import ru.redenergy.warden.entity.ServerOnline
import ru.redenergy.warden.entity.TotalOnline
import java.io.IOException
import java.sql.Timestamp
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OnlineApplication {

    val connection = JdbcConnectionSource("jdbc:mysql://localhost:3306/test", "root", "mysql");
    val scheduler = Executors.newScheduledThreadPool(5)
    val servers = arrayListOf<Server>()
    val onlineDao = DaoManager.createDao<Dao<ServerOnline, String>, ServerOnline>(connection, ServerOnline::class.java)
    val totalDao = DaoManager.createDao<Dao<TotalOnline, Int>, TotalOnline>(connection, TotalOnline::class.java)

    fun launch(servers: MutableList<Server>){
        this.servers.addAll(servers)
        TableUtils.createTableIfNotExists(connection, ServerOnline::class.java)
        TableUtils.createTableIfNotExists(connection, TotalOnline::class.java)
        this.scheduler.scheduleAtFixedRate({ tickServers() }, 1, 60, TimeUnit.SECONDS)
    }

    fun tickServers(){
        val records = servers.map { fetchServerOnline(it) } .filter { it != null }
        for(record in records){
            record?.run { onlineDao.createOrUpdate(this) }
        }
        val total = records.sumBy { it!!.online }
        totalDao.create(TotalOnline(time = Timestamp(System.currentTimeMillis()), online = total))

        println("Successfully pinged ${records.size} servers with total online $total players, ${servers.size - records.size} unsuccessful.")
    }

    fun fetchServerOnline(server: Server): ServerOnline? =
        try {
            val ping = MinecraftPing().getPing(MinecraftPingOptions().apply { hostname = server.ip; port = server.port })
            ServerOnline(server.id, Timestamp(System.currentTimeMillis()), ping.players.online)
        } catch(ex: IOException){
            null
        }


}