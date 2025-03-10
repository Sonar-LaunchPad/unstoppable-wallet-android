package com.starbase.bankwallet.core.providers

import io.horizontalsystems.coinkit.CoinKit
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.coinkit.models.CoinType

class FeeCoinProvider(private val coinKit: CoinKit) {

    fun feeCoinData(coin: Coin): Pair<Coin, String>? = when (val type = coin.type) {
        is CoinType.Erc20 -> erc20()
        is CoinType.Bep20 -> bep20()
        is CoinType.Bep2 -> binance(type.symbol)
        else -> null
    }

    fun feeCoinType(coinType: CoinType): CoinType? = when (coinType) {
        is CoinType.Erc20 -> CoinType.Ethereum
        is CoinType.Bep20 -> CoinType.BinanceSmartChain
        is CoinType.Bep2 -> {
            when (coinType.symbol) {
                "BNB" -> null
                else -> CoinType.Bep2("BNB")
            }
        }
        else -> null
    }

    private fun erc20(): Pair<Coin, String>? {
        return coinKit.getCoin(CoinType.Ethereum)?.let {
            Pair(it, "ERC20")
        }
    }

    private fun bep20(): Pair<Coin, String>? {
        return coinKit.getCoin(CoinType.BinanceSmartChain)?.let {
            Pair(it, "BEP20")
        }
    }

    private fun binance(symbol: String): Pair<Coin, String>? {
        if (symbol == "BNB") {
            return null
        }
        return coinKit.getCoin(CoinType.Bep2("BNB"))?.let {
            Pair(it, "BEP2")
        }
    }

}
