package ru.redenergy.warden

import ch.jamiete.mcping.MinecraftPing
import ch.jamiete.mcping.MinecraftPingOptions
import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.table.TableUtils
import ru.redenergy.warden.config.Server
import ru.redenergy.warden.entity.ServerOnline
import ru.redenergy.warden.entity.TotalOnline
import spark.Spark
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class OnlineApplication(val jdbcPath: String, val jdbcLogin: String, val jdbcPass: String) {
    val totalOnlineTodayQuery = """
                                SELECT MAX(online) as TOTAL_TODAY
                                FROM `total`
                                WHERE `time` > UNIX_TIMESTAMP() - 86400000
                                """
    val totalOnlineMaxQuery =   """
                                SELECT MAX(online) AS TOTAL_MAX
                                FROM `total`
                                """

    val gson = GsonBuilder().create()
    val connection = JdbcConnectionSource(jdbcPath, jdbcLogin, jdbcPass);
    val scheduler = Executors.newScheduledThreadPool(5)
    val servers = arrayListOf<Server>()
    val onlineDao = DaoManager.createDao<Dao<ServerOnline, String>, ServerOnline>(connection, ServerOnline::class.java)
    val totalDao = DaoManager.createDao<Dao<TotalOnline, Int>, TotalOnline>(connection, TotalOnline::class.java)

    fun launch(servers: MutableList<Server>){
        this.servers.addAll(servers)
        TableUtils.createTableIfNotExists(connection, ServerOnline::class.java)
        TableUtils.createTableIfNotExists(connection, TotalOnline::class.java)
        registerRoutes()
        this.scheduler.scheduleAtFixedRate({ tickServers() }, 1, 60, TimeUnit.SECONDS)
    }

    fun tickServers(){
        val records = servers.map { fetchServerOnline(it) } .filter { it != null }
        for(record in records){
            record?.run { onlineDao.createOrUpdate(this) }
        }
        val total = records.sumBy { it!!.online }
        totalDao.create(TotalOnline(time = System.currentTimeMillis(), online = total))

        println("Successfully pinged ${records.size} servers with total online $total players, ${servers.size - records.size} unsuccessful.")
    }

    fun fetchServerOnline(server: Server): ServerOnline? =
        try {
            val ping = MinecraftPing().getPing(MinecraftPingOptions().apply { hostname = server.ip; port = server.port })
            ServerOnline(server.id, System.currentTimeMillis(), ping.players.online)
        } catch(ex: IOException){
            null
        }

    fun registerRoutes(){
        Spark.get("/online", {req, res ->
            val online = onlineDao.queryForAll()
            val totalToday = totalDao.queryRaw(totalOnlineTodayQuery).results[0][0] ?: "-1"
            val totalMax = totalDao.queryRaw(totalOnlineMaxQuery).results[0][0] ?: "-1"
            jsonObject(
                "servers" to gson.toJsonTree(online),
                "total" to online.sumBy { it.online },
                "totalToday" to totalToday.toJson(),
                "totalMax" to totalMax.toJson()
            )
        })

        Spark.get("/total/today", {res, req ->
            buildTotalResponseWithin(TimeUnit.DAYS.toMillis(1)) //one day
        })

        Spark.get("/total/week", {res, req ->
            buildTotalResponseWithin(TimeUnit.DAYS.toMillis(7)) //seven days or one week
        })

        Spark.get("/total/month", {res, req ->
            buildTotalResponseWithin(TimeUnit.DAYS.toMillis(30)) //30 day or one month
        })
    }

    fun buildTotalResponseWithin(interval: Long): JsonObject =
        jsonObject("records" to gson.toJsonTree(totalWithin(interval)))

    fun totalWithin(interval: Long): List<TotalOnline> =
            totalDao.queryBuilder().where().gt("time", System.currentTimeMillis() - interval).query()



}