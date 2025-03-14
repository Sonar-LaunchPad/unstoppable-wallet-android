package com.starbase.bankwallet.modules.releasenotes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.starbase.bankwallet.core.App

object ReleaseNotesModule {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ReleaseNotesViewModel(com.starbase.bankwallet.core.App.appConfigProvider, com.starbase.bankwallet.core.App.releaseNotesManager) as T
        }
    }
}
