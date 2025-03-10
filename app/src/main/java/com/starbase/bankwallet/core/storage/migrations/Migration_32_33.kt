package com.starbase.bankwallet.core.storage.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migration_32_33 : Migration(32, 33) {

    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE `EnabledWallet` SET `coinSettingsId` = 'derivation:bip49' WHERE `coinId` IN ('bitcoin', 'litecoin') AND `coinSettingsId` = ''")
        database.execSQL("UPDATE `EnabledWallet` SET `coinSettingsId` = 'bitcoinCashCoinType:type145' WHERE `coinId` IN ('bitcoinCash') AND `coinSettingsId` = ''")
    }

}
