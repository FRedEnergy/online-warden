package ru.redenergy.warden.entity

import com.j256.ormlite.field.DatabaseField as db
import com.j256.ormlite.table.DatabaseTable

/**
 * Represents server online stored in database
 *
 * @constructor
 * @param id - identifier of server
 * @param lastUpdate - timestamp of last server update
 * @param online - server online
 */
@DatabaseTable(tableName = "online")
class ServerOnline(@db(id = true) val id: String, @db val lastUpdate: Long, @db val online: Int){

    //for ormlite
    constructor(): this("", -1, -1)
}