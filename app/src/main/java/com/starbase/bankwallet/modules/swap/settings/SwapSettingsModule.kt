package com.starbase.bankwallet.modules.swap.settings

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.providers.Translator
import java.math.BigDecimal

object SwapSettingsModule {

    sealed class InvalidSlippageType {
        class Lower(val min: BigDecimal) : InvalidSlippageType()
        class Higher(val max: BigDecimal) : InvalidSlippageType()
    }

    sealed class SwapSettingsError : Exception() {
        object ZeroSlippage : SwapSettingsError() {
            override fun getLocalizedMessage() = Translator.getString(R.string.SwapSettings_Error_SlippageZero)
        }

        object ZeroDeadline : SwapSettingsError() {
            override fun getLocalizedMessage() = Translator.getString(R.string.SwapSettings_Error_DeadlineZero)
        }

        class InvalidSlippage(val invalidSlippageType: InvalidSlippageType) : SwapSettingsError() {
            override fun getLocalizedMessage(): String {
                return when (invalidSlippageType) {
                    is InvalidSlippageType.Lower -> Translator.getString(R.string.SwapSettings_Error_SlippageTooLow)
                    is InvalidSlippageType.Higher -> Translator.getString(R.string.SwapSettings_Error_SlippageTooHigh, invalidSlippageType.max)
                }
            }
        }

        object InvalidAddress : SwapSettingsError() {
            override fun getLocalizedMessage(): String {
                return Translator.getString(R.string.SwapSettings_Error_InvalidAddress)
            }
        }
    }


}
