package com.starbase.bankwallet.modules.balance

import java.math.BigDecimal

class BalanceSorter {

    fun sort(items: Iterable<BalanceModule.BalanceItem>, sortType: BalanceSortType): List<BalanceModule.BalanceItem> {
        return when (sortType) {
            BalanceSortType.Value -> sortByBalance(items)
            BalanceSortType.Name -> items.sortedBy { it.wallet.coin.title }
            BalanceSortType.PercentGrowth -> items.sortedByDescending { it.latestRate?.rateDiff24h }
        }
    }

    private fun sortByBalance(items: Iterable<BalanceModule.BalanceItem>): List<BalanceModule.BalanceItem> {
        val comparator =
                compareByDescending<BalanceModule.BalanceItem> {
                    it.balanceData.available > BigDecimal.ZERO
                }.thenByDescending {
                    it.fiatValue ?: BigDecimal.ZERO > BigDecimal.ZERO
                }.thenByDescending {
                    it.fiatValue
                }.thenByDescending {
                    it.balanceData.available
                }.thenBy {
                    it.wallet.coin.title
                }

        return items.sortedWith(comparator)
    }
}
