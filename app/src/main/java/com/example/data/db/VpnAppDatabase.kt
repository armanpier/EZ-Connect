package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PanelEntity::class,
        InboundCacheEntity::class,
        VpnLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VpnAppDatabase : RoomDatabase() {

    abstract fun panelDao(): PanelDao
    abstract fun inboundDao(): InboundDao
    abstract fun vpnLogDao(): VpnLogDao

    companion object {
        @Volatile
        private var INSTANCE: VpnAppDatabase? = null

        fun getDatabase(context: Context): VpnAppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VpnAppDatabase::class.java,
                    "xui_vpn_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
