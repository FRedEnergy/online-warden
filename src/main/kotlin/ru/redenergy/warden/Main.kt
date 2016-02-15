package ru.redenergy.warden

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import ru.redenergy.warden.config.Server
import java.io.File
import java.io.FileReader

fun main(args: Array<String>){
    var jdbcPath = "jdbc:mysql://localhost:3306/test"
    var jdbcLogin = "root"
    var jdbcPass = "mysql"
    var serversConfig = "servers.json"
    for((i, arg) in args.withIndex()){
        when(arg){
            "-path" -> jdbcPath = args[i + 1]
            "-login" -> jdbcLogin = args[i + 1]
            "-pass" -> jdbcPass = args[i + 1]
            "-servers" -> serversConfig = args[i + 1]
        }
    }
    val servers = Gson().fromJson<MutableList<Server>>(JsonReader(FileReader(File(serversConfig))))
    OnlineApplication(jdbcPath, jdbcLogin, jdbcPass).launch(servers)
}
