package com.starbase.bankwallet.modules.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.AdapterState
import com.starbase.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.CoinSettings
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.TransactionLockState
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoAddressMapper
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.entities.Currency
import java.math.BigDecimal
import java.util.*

typealias CoinCode = String

data class TransactionViewItem(
    val wallet: TransactionWallet,
    val record: TransactionRecord,
    val type: TransactionType,
    val date: Date?,
    val status: TransactionStatus,
    var mainAmountCurrencyString: String?
) : Comparable<TransactionViewItem> {

    var image: Int
    val topText: String
    val bottomText: String
    var primaryValueText: String? = null
    var primaryValueTextColor: Int = R.color.jacob
    var secondaryValueText: String? = null
    var secondaryValueTextColor: Int = R.color.remus
    var showDoubleSpend = false
    var lockState: TransactionLockState? = null
    var showSentToSelf = false

    init {
        when (type) {
            is TransactionType.Incoming -> {
                image = R.drawable.ic_incoming_20
                topText = Translator.getString(R.string.Transactions_Receive)
                bottomText = type.from?.let {
                    Translator.getString(
                        R.string.Transactions_From,
                        truncated(it)
                    )
                } ?: "---"
                lockState = type.lockState
                showDoubleSpend = type.conflictingTxHash != null

                mainAmountCurrencyString?.let {
                    primaryValueText = it
                    primaryValueTextColor = R.color.remus
                }

                secondaryValueText = type.amount
                secondaryValueTextColor = R.color.grey

            }
            is TransactionType.Outgoing -> {
                image = R.drawable.ic_outgoing_20
                topText = Translator.getString(R.string.Transactions_Send)
                bottomText = type.to?.let {
                    Translator.getString(
                        R.string.Transactions_To,
                        truncated(it)
                    )
                } ?: "---"
                showSentToSelf = type.sentToSelf
                lockState = type.lockState
                showDoubleSpend = type.conflictingTxHash != null

                mainAmountCurrencyString?.let {
                    primaryValueText = it
                    primaryValueTextColor = R.color.jacob
                }

                secondaryValueText = type.amount
                secondaryValueTextColor = R.color.grey
            }
            is TransactionType.Approve -> {
                image = R.drawable.ic_tx_checkmark_20
                topText = Translator.getString(R.string.Transactions_Approve)
                bottomText =
                    Translator.getString(R.string.Transactions_From, truncated(type.spender))

                mainAmountCurrencyString?.let {
                    primaryValueText = if (type.isMaxAmount) "∞" else it
                    primaryValueTextColor = R.color.leah
                }

                secondaryValueText = when {
                    type.isMaxAmount -> Translator.getString(
                        R.string.Transaction_Unlimited,
                        type.coinCode
                    )
                    else -> type.amount
                }
                secondaryValueTextColor = R.color.grey
            }
            is TransactionType.Swap -> {
                image = R.drawable.ic_tx_swap_20
                topText = Translator.getString(R.string.Transactions_Swap)
                bottomText = Translator.getString(
                    R.string.Transactions_From,
                    truncated(type.exchangeAddress)
                )

                primaryValueText = type.amountIn
                primaryValueTextColor = R.color.jacob

                secondaryValueText = type.amountOut
                secondaryValueTextColor = if (type.foreignRecipient) R.color.grey else R.color.remus
            }
            is TransactionType.ContractCall -> {
                image = R.drawable.ic_tx_unordered
                val blockchainName = if (type.blockchainTitle.isNotEmpty()) "${type.blockchainTitle} " else ""
                topText = blockchainName + Translator.getString(R.string.Transactions_ContractCall)
                bottomText = Translator.getString(
                    R.string.Transactions_From,
                    truncated(type.contractAddress)
                )
            }
            is TransactionType.ContractCreation -> {
                image = R.drawable.ic_tx_unordered
                topText = Translator.getString(R.string.Transactions_ContractCreation)
                bottomText = "---"
            }
        }

        if (status == TransactionStatus.Failed) {
            image = R.drawable.ic_attention_red_20
        }
    }

    private fun truncated(string: String): CharSequence {
        return TransactionViewHelper.truncated(string, 75f)
    }

    override fun compareTo(other: TransactionViewItem): Int {
        return record.compareTo(other.record)
    }

    override fun equals(other: Any?): Boolean {
        if (other is TransactionViewItem) {
            return record == other.record
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return record.hashCode()
    }

    fun itemTheSame(other: TransactionViewItem): Boolean {
        return record == other.record
    }

    fun contentTheSame(other: TransactionViewItem): Boolean {
        return mainAmountCurrencyString == other.mainAmountCurrencyString
                && date == other.date
                && status == other.status
                && type == other.type
    }

    fun clearRates() {
        mainAmountCurrencyString = null
    }
}

data class TransactionLockInfo(
    val lockedUntil: Date,
    val originalAddress: String,
    val amount: BigDecimal?
)

sealed class TransactionStatus {
    object Pending : TransactionStatus()
    class Processing(val progress: Double) : TransactionStatus() //progress in 0.0 .. 1.0
    object Completed : TransactionStatus()
    object Failed : TransactionStatus()
}

data class TransactionWallet(
    val coin: Coin?,
    val source: TransactionSource
)

data class TransactionSource(
    val blockchain: Blockchain,
    val account: Account,
    val coinSettings: CoinSettings
) {

    sealed class Blockchain {
        object Bitcoin : Blockchain()
        object Litecoin : Blockchain()
        object BitcoinCash : Blockchain()
        object Dash : Blockchain()
        object Ethereum : Blockchain()
        object Zcash : Blockchain()
        object BinanceSmartChain : Blockchain()
        class Bep2(val symbol: String) : Blockchain(){
            override fun hashCode(): Int {
                return this.symbol.hashCode()
            }
            override fun equals(other: Any?): Boolean {
                return when(other){
                    is Bep2 -> this.symbol == other.symbol
                    else -> false
                }
            }
        }

        fun getTitle(): String {
            return when(this){
                Bitcoin -> "Bitcoin"
                Litecoin -> "Litecoin"
                BitcoinCash -> "BitcoinCash"
                Dash -> "Dash"
                Ethereum -> "Ethereum"
                Zcash -> "Zcash"
                BinanceSmartChain -> "Binance Smart Chain"
                is Bep2 -> "Binance Chain"
            }
        }
    }

}

object TransactionsModule {

    data class FetchData(val wallet: TransactionWallet, val from: TransactionRecord?, val limit: Int)

    interface IView {
        fun showSyncing()
        fun hideSyncing()
        fun showFilters(filters: List<Wallet?>, selectedFilter: Wallet?)
        fun showTransactions(items: List<TransactionViewItem>)
        fun showNoTransactions()
        fun reloadTransactions()
    }

    interface IViewDelegate {
        fun viewDidLoad()
        fun onFilterSelect(wallet: Wallet?)
        fun onClear()

        fun onBottomReached()
        fun willShow(transactionViewItem: TransactionViewItem)
        fun showDetails(item: TransactionViewItem)
    }

    interface IInteractor {
        fun initialFetch()
        fun clear()
        fun fetchRecords(fetchDataList: List<FetchData>, initial: Boolean)
        fun fetchLastBlockHeights(transactionWallets: List<TransactionWallet>)
        fun fetchRate(coin: Coin, timestamp: Long)
        fun observe(wallets: List<TransactionWallet>)
    }

    interface IInteractorDelegate {
        fun didFetchRecords(records: Map<TransactionWallet, List<TransactionRecord>>, initial: Boolean)
        fun onUpdateLastBlock(wallet: TransactionWallet, lastBlockInfo: LastBlockInfo)
        fun onUpdateBaseCurrency()
        fun didFetchRate(rateValue: BigDecimal, coin: Coin, currency: Currency, timestamp: Long)
        fun didUpdateRecords(records: List<TransactionRecord>, wallet: TransactionWallet)
        fun onConnectionRestore()
        fun onUpdateAdapterStates(states: Map<TransactionWallet, AdapterState>)
        fun onUpdateAdapterState(state: AdapterState, wallet: TransactionWallet)
        fun onUpdateWallets(wallets: List<Wallet>)
        fun onUpdateLastBlockInfos(lastBlockInfos: MutableList<Pair<TransactionWallet, LastBlockInfo?>>)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val view = TransactionsViewModel()

            val dataSource = TransactionRecordDataSource(PoolRepo(), TransactionItemDataSource(), 20, TransactionViewItemFactory(
                TransactionInfoAddressMapper, com.starbase.bankwallet.core.App.numberFormatter
            ), TransactionMetadataDataSource())
            val interactor = TransactionsInteractor(com.starbase.bankwallet.core.App.walletManager, com.starbase.bankwallet.core.App.adapterManager, com.starbase.bankwallet.core.App.currencyManager, com.starbase.bankwallet.core.App.xRateManager, com.starbase.bankwallet.core.App.connectivityManager)
            val presenter = TransactionsPresenter(interactor, dataSource)

            presenter.view = view
            interactor.delegate = presenter
            view.delegate = presenter

            presenter.viewDidLoad()

            return view as T
        }
    }


}
