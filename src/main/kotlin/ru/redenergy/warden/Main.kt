package ru.redenergy.warden

import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import ru.redenergy.warden.config.Server
import java.io.File
import java.io.FileReader

fun main(args: Array<String>){
    val servers = Gson().fromJson<MutableList<Server>>(JsonReader(FileReader(File("servers.json"))))
    OnlineApplication().launch(servers)
}
