package ru.redenergy.warden.entity

import com.j256.ormlite.field.DatabaseField as db
import com.j256.ormlite.table.DatabaseTable
import java.sql.Timestamp

/**
 * Represents server online stored in database
 *
 * @constructor
 * @param id - identifier of server
 * @param lastUpdate - timestamp of last server update
 * @param online - server online
 */
@DatabaseTable(tableName = "online")
class ServerOnline(@com.j256.ormlite.field.DatabaseField(id = true) val id: String, @com.j256.ormlite.field.DatabaseField val lastUpdate: Timestamp, @com.j256.ormlite.field.DatabaseField val online: Int){

    //for ormlite
    constructor(): this("", Timestamp(-1), -1)
}