package com.starbase.bankwallet.modules.walletconnect.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.starbase.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ethereum.EthereumFeeViewModel
import io.horizontalsystems.bankwallet.core.ethereum.EvmCoinServiceFactory
import io.horizontalsystems.bankwallet.core.ethereum.EvmTransactionService
import io.horizontalsystems.bankwallet.core.factories.FeeRateProviderFactory
import io.horizontalsystems.bankwallet.modules.sendevm.SendEvmData
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionService
import io.horizontalsystems.bankwallet.modules.sendevmtransaction.SendEvmTransactionViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectSendEthereumTransactionRequest
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectService
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WalletConnectSendEthereumTransactionRequestService
import io.horizontalsystems.bankwallet.modules.walletconnect.request.sendtransaction.WalletConnectSendEthereumTransactionRequestViewModel
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.ethereumkit.core.EthereumKit.NetworkType
import io.horizontalsystems.ethereumkit.models.Address
import java.math.BigInteger

object WalletConnectRequestModule {
    const val TYPED_MESSAGE = "typed_message"

    class Factory(
            private val request: WalletConnectSendEthereumTransactionRequest,
            private val baseService: WalletConnectService
    ) : ViewModelProvider.Factory {
        private val evmKit by lazy { baseService.evmKit!! }
        private val coin by lazy {
            when (evmKit.networkType) {
                NetworkType.EthMainNet,
                NetworkType.EthRopsten,
                NetworkType.EthKovan,
                NetworkType.EthGoerli,
                NetworkType.EthRinkeby -> com.starbase.bankwallet.core.App.coinManager.getCoin(CoinType.Ethereum)!!
                NetworkType.BscMainNet -> com.starbase.bankwallet.core.App.coinManager.getCoin(CoinType.BinanceSmartChain)!!
            }
        }
        private val service by lazy { WalletConnectSendEthereumTransactionRequestService(request, baseService) }
        private val coinServiceFactory by lazy { EvmCoinServiceFactory(coin, com.starbase.bankwallet.core.App.coinKit, com.starbase.bankwallet.core.App.currencyManager, com.starbase.bankwallet.core.App.xRateManager) }
        private val transactionService by lazy {
            val feeRateProvider = FeeRateProviderFactory.provider(coin)!!
            EvmTransactionService(evmKit, feeRateProvider, 10)
        }
        private val sendService by lazy { SendEvmTransactionService(SendEvmData(service.transactionData), evmKit, transactionService, com.starbase.bankwallet.core.App.activateCoinManager, service.gasPrice) }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                WalletConnectSendEthereumTransactionRequestViewModel::class.java -> {
                    WalletConnectSendEthereumTransactionRequestViewModel(service) as T
                }
                EthereumFeeViewModel::class.java -> {
                    EthereumFeeViewModel(transactionService, coinServiceFactory.baseCoinService) as T
                }
                SendEvmTransactionViewModel::class.java -> {
                    SendEvmTransactionViewModel(sendService, coinServiceFactory) as T
                }
                else -> throw IllegalArgumentException()
            }
        }
    }

}

data class WalletConnectTransaction(
        val from: Address,
        val to: Address,
        val nonce: Long?,
        val gasPrice: Long?,
        val gasLimit: Long?,
        val value: BigInteger,
        val data: ByteArray
)
