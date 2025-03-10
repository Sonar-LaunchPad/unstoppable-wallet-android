package com.starbase.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.TransactionLockState
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoActionButton.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoButtonType.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionInfoItemType.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.TransactionStatusViewItem.*
import io.horizontalsystems.bankwallet.modules.transactionInfo.adapters.TransactionInfoViewItem
import io.horizontalsystems.bankwallet.modules.transactions.TransactionStatus
import io.horizontalsystems.coinkit.models.Coin
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.ListPosition.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.math.min

class TransactionInfoViewItemFactory(
    private val numberFormatter: IAppNumberFormatter,
    private val translator: Translator,
    private val dateHelper: DateHelper,
    private val transactionInfoAddressMapper: TransactionInfoAddressMapper
) {

    fun getMiddleSectionItems(
        transaction: TransactionRecord,
        rates: Map<Coin, CurrencyValue>,
        lastBlockInfo: LastBlockInfo?,
        explorerData: TransactionInfoModule.ExplorerData
    ): List<TransactionInfoViewItem?> {

        val status = transaction.status(lastBlockInfo?.height)

        val statusType = Status(
            getString(R.string.TransactionInfo_Status),
            R.drawable.ic_info_24,
            getStatusViewItem(status)
        )

        val date = Value(
            getString(R.string.TransactionInfo_Date),
            dateHelper.getFullDate(Date(transaction.timestamp * 1000))
        )
        val items = mutableListOf<TransactionInfoViewItem?>()

        when (transaction) {
            is EvmIncomingTransactionRecord -> {
                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                middleSectionTypes.add(statusType)
                middleSectionTypes.add(date)

                rates[transaction.value.coin]?.let {
                    middleSectionTypes.add(getHistoricalRate(it, transaction.value))
                }

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_From),
                        transaction.from
                    )
                )
                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                //Top section
                items.addAll(
                    getActionsSection(
                        getString(R.string.Transactions_Receive),
                        transaction.value,
                        rates[transaction.value.coin],
                        true
                    )
                )

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            is EvmOutgoingTransactionRecord -> {
                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                middleSectionTypes.add(statusType)
                middleSectionTypes.add(date)
                middleSectionTypes.add(getEvmFeeItem(transaction.fee, rates[transaction.value.coin], status))

                rates[transaction.value.coin]?.let {
                    middleSectionTypes.add(getHistoricalRate(it, transaction.value))
                }

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_To),
                        transaction.to
                    )
                )
                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                //Top section
                items.addAll(
                    getActionsSection(
                        getString(R.string.Transactions_Send),
                        transaction.value,
                        rates[transaction.value.coin],
                        false
                    )
                )

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            is SwapTransactionRecord -> {
                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                middleSectionTypes.add(statusType)
                middleSectionTypes.add(date)
                middleSectionTypes.add(getEvmFeeItem(transaction.fee, rates[transaction.fee.coin], status))

                transaction.valueOut?.let { valueOut ->
                    val valueIn = transaction.valueIn
                    val price = valueIn.value.divide(valueOut.value, min(valueOut.coin.decimal, valueIn.coin.decimal), RoundingMode.HALF_EVEN).abs()
                    val priceValue = numberFormatter.formatCoin(price, transaction.valueIn.coin.code, 0, 8)
                    middleSectionTypes.add(
                        Value(
                            getString(R.string.TransactionInfo_Price),
                            "${valueOut.coin.code} = $priceValue"
                        )
                    )
                }

                middleSectionTypes.add(
                    Value(
                        getString(R.string.TransactionInfo_Service),
                        TransactionInfoAddressMapper.map(transaction.exchangeAddress)
                    )
                )

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                items.addAll(
                    getActionsSection(
                        getYouPayString(status),
                        transaction.valueIn,
                        rates[transaction.valueIn.coin],
                        false
                    )
                )

                //Top section
                transaction.valueOut?.let {
                    if (!transaction.foreignRecipient) {
                        items.addAll(
                            getActionsSection(
                                getYouGetString(status),
                                transaction.valueOut,
                                rates[transaction.valueOut.coin],
                                true
                            )
                        )
                    }
                }

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            is ApproveTransactionRecord -> {

                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()
                val rate = rates[transaction.value.coin]

                middleSectionTypes.add(date)
                middleSectionTypes.add(statusType)
                middleSectionTypes.add(getEvmFeeItem(transaction.fee, rates[transaction.fee.coin], status))

                rate?.let {
                    middleSectionTypes.add(getHistoricalRate(it, transaction.value))
                }

                middleSectionTypes.add(
                    Decorated(getString(R.string.TransactionInfo_To), transaction.spender)
                )

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                val fiatAmountFormatted = rate?.let {
                    numberFormatter.formatFiat(
                        (it.value * transaction.value.value).abs(),
                        it.currency.symbol,
                        0,
                        2
                    )
                } ?: "---"

                val coinAmountFormatted =
                    numberFormatter.formatCoin(
                        transaction.value.value,
                        transaction.value.coin.code,
                        0,
                        8
                    )

                val coinAmountString = if (transaction.value.isMaxValue) translator.getString(
                    R.string.Transaction_Unlimited,
                    transaction.value.coin.code
                ) else coinAmountFormatted

                val fiatAmountColoredValue = ColoredValue(
                    if (transaction.value.isMaxValue) "∞" else fiatAmountFormatted,
                    getAmountColor(null)
                )

                //Top Section
                items.add(
                    TransactionInfoViewItem(
                        TransactionType(
                            getString(R.string.Transactions_Approve),
                            transaction.value.coin.title
                        ), First
                    )
                )
                items.add(TransactionInfoViewItem(Amount(coinAmountString, fiatAmountColoredValue), Last))
                items.add(null)

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            is ContractCallTransactionRecord -> {
                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                items.add(
                    TransactionInfoViewItem(
                        TransactionType(
                            getString(R.string.Transactions_ContractCall),
                            getNameOrAddress(transaction.contractAddress)
                        ), Single
                    )
                )
                items.add(null)

                val transactionValue = transaction.value

                if (transaction.outgoingEip20Events.isNotEmpty() || (transactionValue.value != BigDecimal.ZERO && !transaction.foreignTransaction) ) {
                    val youPaySection = mutableListOf<TransactionInfoItemType>()
                    youPaySection.add(
                        TransactionType(
                            getYouPayString(status),
                            null
                        )
                    )

                    if (transactionValue.value != BigDecimal.ZERO && !transaction.foreignTransaction) {
                        youPaySection.add(
                            getAmount(
                                rates[transaction.value.coin],
                                transactionValue,
                                false
                            )
                        )
                    }

                    transaction.outgoingEip20Events.forEachIndexed { index, (_, eventCoinValue) ->
                        youPaySection.add(
                            getAmount(
                                rates[eventCoinValue.coin],
                                eventCoinValue,
                                false
                            )
                        )
                    }

                    items.addAll(getViewItems(youPaySection))
                    items.add(null)
                }

                if (transaction.incomingEip20Events.isNotEmpty() || transaction.incomingInternalETHs.isNotEmpty()) {
                    val youGotSection = mutableListOf<TransactionInfoItemType>()
                    youGotSection.add(
                        TransactionType(
                            getYouGetString(status),
                            null
                        )
                    )

                    transaction.incomingInternalETHs.firstOrNull()?.let { (_, coinValue) ->
                        val ethCoin = coinValue.coin
                        val ethSum =
                            transaction.incomingInternalETHs.sumOf { (_, eventCoinValue) -> eventCoinValue.value }

                        youGotSection.add(
                            getAmount(
                                rates[ethCoin],
                                CoinValue(ethCoin, ethSum),
                                true
                            )
                        )
                    }

                    transaction.incomingEip20Events.forEach { (_, eventCoinValue) ->
                        youGotSection.add(
                            getAmount(
                                rates[eventCoinValue.coin],
                                eventCoinValue,
                                true
                            )
                        )
                    }

                    items.addAll(getViewItems(youGotSection))
                    items.add(null)
                }

                middleSectionTypes.add(date)
                middleSectionTypes.add(statusType)
                middleSectionTypes.add(getEvmFeeItem(transaction.fee, rates[transaction.fee.coin], status))

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            is BitcoinIncomingTransactionRecord -> {

                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                middleSectionTypes.add(date)
                middleSectionTypes.add(statusType)

                rates[transaction.value.coin]?.let {
                    middleSectionTypes.add(getHistoricalRate(it, transaction.value))
                }

                transaction.from?.let {
                    middleSectionTypes.add(Decorated(getString(R.string.TransactionInfo_From), it))
                }

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                transaction.conflictingHash?.let { conflictingHash ->
                    middleSectionTypes.add(
                        getDoubleSpendViewItem(
                            transaction.transactionHash,
                            conflictingHash
                        )
                    )
                }

                if (transaction.showRawTransaction) {
                    middleSectionTypes.add(
                        RawTransaction(
                            getString(R.string.TransactionInfo_RawTransaction),
                            CopyButton
                        )
                    )
                }

                val lockState = transaction.lockState(lastBlockInfo?.timestamp)

                getLockStateItem(lockState)?.let {
                    middleSectionTypes.add(it)
                }

                if (!transaction.memo.isNullOrBlank()){
                    middleSectionTypes.add(Value(getString(R.string.TransactionInfo_Memo), transaction.memo))
                }

                //Top section
                items.addAll(
                    getActionsSection(
                        getString(R.string.Transactions_Receive),
                        transaction.value,
                        rates[transaction.value.coin],
                        true
                    )
                )

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            is BitcoinOutgoingTransactionRecord -> {

                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                middleSectionTypes.add(date)
                middleSectionTypes.add(statusType)

                transaction.fee?.let {
                    middleSectionTypes.add(getFee(rates[transaction.value.coin], it))
                }

                rates[transaction.value.coin]?.let {
                    middleSectionTypes.add(getHistoricalRate(it, transaction.value))
                }

                transaction.to?.let {
                    middleSectionTypes.add(Decorated(getString(R.string.TransactionInfo_To), it))
                }

                transaction.conflictingHash?.let { conflictingHash ->
                    middleSectionTypes.add(
                        getDoubleSpendViewItem(
                            transaction.transactionHash,
                            conflictingHash
                        )
                    )
                }

                val lockState = transaction.lockState(lastBlockInfo?.timestamp)

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                getLockStateItem(lockState)?.let {
                    middleSectionTypes.add(it)
                }

                if (transaction.showRawTransaction) {
                    middleSectionTypes.add(
                        RawTransaction(
                            getString(R.string.TransactionInfo_RawTransaction),
                            CopyButton
                        )
                    )
                }

                if (!transaction.memo.isNullOrBlank()){
                    middleSectionTypes.add(Value(getString(R.string.TransactionInfo_Memo), transaction.memo))
                }

                //Top section
                items.addAll(
                    getActionsSection(
                        getString(R.string.Transactions_Send),
                        transaction.value,
                        rates[transaction.value.coin],
                        false
                    )
                )

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            is BinanceChainIncomingTransactionRecord -> {
                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                middleSectionTypes.add(date)
                middleSectionTypes.add(statusType)

                rates[transaction.value.coin]?.let {
                    middleSectionTypes.add(getHistoricalRate(it, transaction.value))
                }

                middleSectionTypes.add(Decorated(getString(R.string.TransactionInfo_From), transaction.from))

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                if (!transaction.memo.isNullOrBlank()){
                    middleSectionTypes.add(Value(getString(R.string.TransactionInfo_Memo), transaction.memo))
                }

                //Top section
                items.addAll(
                    getActionsSection(
                        getString(R.string.Transactions_Receive),
                        transaction.value,
                        rates[transaction.value.coin],
                        true
                    )
                )

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            is BinanceChainOutgoingTransactionRecord -> {
                val middleSectionTypes = mutableListOf<TransactionInfoItemType>()

                middleSectionTypes.add(date)
                middleSectionTypes.add(statusType)

                middleSectionTypes.add(getFee(rates[transaction.value.coin], transaction.fee))

                rates[transaction.value.coin]?.let {
                    middleSectionTypes.add(getHistoricalRate(it, transaction.value))
                }

                middleSectionTypes.add(Decorated(getString(R.string.TransactionInfo_To), transaction.to))

                middleSectionTypes.add(
                    Decorated(
                        getString(R.string.TransactionInfo_Id),
                        transaction.transactionHash,
                        ShareButton(transaction.transactionHash)
                    )
                )

                if (!transaction.memo.isNullOrBlank()){
                    middleSectionTypes.add(Value(getString(R.string.TransactionInfo_Memo), transaction.memo))
                }

                //Top section
                items.addAll(
                    getActionsSection(
                        getString(R.string.Transactions_Send),
                        transaction.value,
                        rates[transaction.value.coin],
                        false
                    )
                )

                //Middle section
                items.addAll(getViewItems(middleSectionTypes))
                items.add(null)
                items.addAll(getAdditionalButtons(explorerData))

                return items
            }

            else -> return emptyList()
        }
    }

    private fun getDoubleSpendViewItem(transactionHash: String, conflictingHash: String) = DoubleSpend(
        getString(R.string.TransactionInfo_DoubleSpendNote),
        R.drawable.ic_double_spend_20,
        transactionHash,
        conflictingHash
    )

    private fun getYouPayString(status: TransactionStatus): String{
        return if (status == TransactionStatus.Completed){
            getString(R.string.TransactionInfo_YouPaid)
        } else {
            getString(R.string.TransactionInfo_YouPay)
        }
    }

    private fun getYouGetString(status: TransactionStatus): String{
        return if (status == TransactionStatus.Completed){
            getString(R.string.TransactionInfo_YouGot)
        } else {
            getString(R.string.TransactionInfo_YouGet)
        }
    }

    private fun getViewItems(viewItemTypes: MutableList<TransactionInfoItemType>): List<TransactionInfoViewItem> {
        return viewItemTypes.mapIndexed { index, itemType ->
            TransactionInfoViewItem(itemType, Companion.getListPosition(viewItemTypes.size, index))
        }
    }

    private fun getLockStateItem(lockState: TransactionLockState?): TransactionInfoItemType? {
        return lockState?.let {
            val leftIcon = if (it.locked) R.drawable.ic_lock_20 else R.drawable.ic_unlock_20
            val date = DateHelper.getFullDate(it.date)
            val title = translator.getString(
                if (it.locked) R.string.TransactionInfo_LockedUntil else R.string.TransactionInfo_UnlockedAt,
                date
            )
            LockState(title, leftIcon, it.date, it.locked)
        }
    }

    private fun getNameOrAddress(address: String): String {
        return transactionInfoAddressMapper.title(address)
            ?: "${address.take(5)}...${address.takeLast(5)}"
    }

    private fun getAdditionalButtons(
        explorerData: TransactionInfoModule.ExplorerData,
        isResend: Boolean = false
    ): List<TransactionInfoViewItem?> {
        val items = mutableListOf<TransactionInfoViewItem?>()
        val title = translator.getString(
            R.string.TransactionInfo_ButtonViewOnExplorerName,
            explorerData.title
        )
        items.add(
            TransactionInfoViewItem(
                Button(
                    title,
                    R.drawable.ic_language,
                    OpenExplorer(explorerData.url)
                ),
                Single
            )
        )
        items.add(null)

        if (isResend) {
            items.add(
                TransactionInfoViewItem(
                    Button(
                        getString(R.string.TransactionInfo_Resend),
                        R.drawable.ic_resend_20,
                        Resend
                    ),
                    Single
                )
            )
            items.add(null)
        }

        return items
    }

    private fun getActionsSection(
        title: String,
        coinValue: CoinValue,
        rate: CurrencyValue?,
        incoming: Boolean?
    ): List<TransactionInfoViewItem?> {
        val items = mutableListOf<TransactionInfoViewItem?>()

        items.add(TransactionInfoViewItem(TransactionType(title, coinValue.coin.title), First))

        items.add(TransactionInfoViewItem(getAmount(rate, coinValue, incoming), Last))

        items.add(null) //add divider

        return items
    }

    private fun getAmountColor(incoming: Boolean?): Int {
        return when (incoming) {
            true -> R.color.remus
            false -> R.color.jacob
            else -> R.color.oz
        }
    }

    private fun getString(resId: Int): String {
        return translator.getString(resId)
    }

    private fun getStatusViewItem(
        status: TransactionStatus
    ): TransactionStatusViewItem {

        return when (status) {
            TransactionStatus.Failed -> Failed
            TransactionStatus.Pending -> Pending(getString(R.string.Transactions_Pending))
            TransactionStatus.Completed -> Completed(getString(R.string.Transactions_Completed))
            is TransactionStatus.Processing -> Processing(
                status.progress,
                getString(R.string.Transactions_Processing)
            )
        }
    }

    private fun getAmount(
        rate: CurrencyValue?,
        coinValue: CoinValue,
        incoming: Boolean?
    ): TransactionInfoItemType {
        val valueInFiat = rate?.let {
            numberFormatter.formatFiat(
                (it.value * coinValue.value).abs(),
                it.currency.symbol,
                0,
                2
            )
        } ?: "---"
        val fiatValueColored = ColoredValue(valueInFiat, getAmountColor(incoming))
        val coinValueFormatted =
            numberFormatter.formatCoin(coinValue.value.abs(), coinValue.coin.code, 0, 8)

        return Amount(coinValueFormatted, fiatValueColored)
    }

    private fun getHistoricalRate(
        rate: CurrencyValue,
        coinValue: CoinValue,
    ): TransactionInfoItemType {
        val rateFormatted =
            numberFormatter.formatFiat(rate.value, rate.currency.symbol, 2, 4)
        val rateValue = translator.getString(
            R.string.Balance_RatePerCoin,
            rateFormatted,
            coinValue.coin.code
        )
        return Value(getString(R.string.TransactionInfo_HistoricalRate), rateValue)
    }

    private fun getFee(rate: CurrencyValue?, coinValue: CoinValue): TransactionInfoItemType {
        val feeAmountString = getFeeAmountString(rate, coinValue)

        return Value(getString(R.string.TransactionInfo_Fee), feeAmountString)
    }

    private fun getEvmFeeItem(
        coinValue: CoinValue,
        rate: CurrencyValue?,
        status: TransactionStatus
    ): TransactionInfoItemType {
        val feeAmountString = getFeeAmountString(rate, coinValue)
        val feeTitle: String = when (status) {
            TransactionStatus.Pending -> getString(R.string.TransactionInfo_FeeEstimated)
            is TransactionStatus.Processing,
            TransactionStatus.Failed,
            TransactionStatus.Completed -> getString(R.string.TransactionInfo_Fee)
        }

        return Value(feeTitle, feeAmountString)
    }

    private fun getFeeAmountString(rate: CurrencyValue?, coinValue: CoinValue): String {
        val feeInFiat = rate?.let {
            numberFormatter.formatFiat(
                it.value * coinValue.value,
                it.currency.symbol,
                2,
                4
            )
        }
        val feeInCoin =
            numberFormatter.formatCoin(coinValue.value, coinValue.coin.code, 0, 8)

        return feeInCoin + (if (feeInFiat != null) " | $feeInFiat" else "")
    }

}
