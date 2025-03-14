package com.starbase.bankwallet.modules.swap.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.modules.swap.SwapMainModule.ISwapProvider
import io.horizontalsystems.bankwallet.modules.swap.SwapMainViewModel
import io.horizontalsystems.bankwallet.modules.swap.info.SwapInfoModule
import io.horizontalsystems.core.findNavController
import kotlinx.android.synthetic.main.fragment_swap_settings.*

class SwapSettingsMainFragment : BaseFragment() {
    private val mainViewModel by navGraphViewModels<SwapMainViewModel>(R.id.swapFragment)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_swap_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuInfo -> {
                    SwapInfoModule.start(this, navOptions(), mainViewModel.dex)
                    true
                }
                R.id.menuCancel -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        providerButton.setOnClickListener {
            findNavController().navigate(R.id.selectSwapProviderDialog)
        }

        setProviderButtonTitle(mainViewModel.provider.title)
        setProviderView(mainViewModel.provider)

        mainViewModel.providerLiveData.observe(viewLifecycleOwner, { provider ->
            setProviderButtonTitle(provider.title)
            setProviderView(provider)
        })

    }

    private fun setProviderButtonTitle(title: String) {
        providerButton.showValueWithColor(title, R.color.leah)
    }

    private fun setProviderView(provider: ISwapProvider) {
        childFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_placeholder, provider.settingsFragment)
                .commitNow()
    }
}
