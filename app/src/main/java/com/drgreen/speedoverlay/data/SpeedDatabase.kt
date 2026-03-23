/*
 * Copyright 2026 Hendrik Jüchter
 */

package com.drgreen.speedoverlay.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "osm_tiles")
data class OsmTile(
    @PrimaryKey val tileId: String, // Format: "latIndex_lonIndex"
    val data: String,              // JSON-String der Overpass-Antwort
    val timestamp: Long
)

@Dao
interface TileDao {
    @Query("SELECT * FROM osm_tiles WHERE tileId = :id")
    suspend fun getTile(id: String): OsmTile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTile(tile: OsmTile)

    @Query("DELETE FROM osm_tiles WHERE timestamp < :expiry")
    suspend fun deleteOldTiles(expiry: Long)
}

@Database(entities = [OsmTile::class], version = 2, exportSchema = false)
abstract class SpeedDatabase : RoomDatabase() {
    abstract fun tileDao(): TileDao
}
