package com.starbase.bankwallet.modules.restoremnemonic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.starbase.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.PassphraseValidator

object RestoreMnemonicModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = RestoreMnemonicService(com.starbase.bankwallet.core.App.wordsManager, PassphraseValidator())

            return RestoreMnemonicViewModel(service, listOf(service)) as T
        }
    }

}
