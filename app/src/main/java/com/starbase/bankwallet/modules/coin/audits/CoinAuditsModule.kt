package com.starbase.bankwallet.modules.coin.audits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.starbase.bankwallet.core.App
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.views.ListPosition
import java.util.*

object CoinAuditsModule {
    class Factory(private val coinType: CoinType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return CoinAuditsViewModel(
                com.starbase.bankwallet.core.App.xRateManager,
                coinType
            ) as T
        }
    }
}

sealed class CoinAuditItem {
    class Header(val name: String) : CoinAuditItem()
    class Report(
        val name: String,
        val date: Date,
        val issues: Int = 0,
        val link: String?,
        val position: ListPosition
    ) : CoinAuditItem()
}
