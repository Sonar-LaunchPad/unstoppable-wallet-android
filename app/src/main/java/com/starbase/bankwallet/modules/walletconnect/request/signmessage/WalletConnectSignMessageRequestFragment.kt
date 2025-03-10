package com.starbase.bankwallet.modules.walletconnect.request.signmessage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.navGraphViewModels
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.setOnSingleClickListener
import io.horizontalsystems.bankwallet.modules.walletconnect.WalletConnectViewModel
import io.horizontalsystems.bankwallet.modules.walletconnect.request.WalletConnectRequestModule.TYPED_MESSAGE
import io.horizontalsystems.bankwallet.modules.walletconnect.request.signmessage.WalletConnectSignMessageRequestService.SignMessage
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.views.ListPosition
import kotlinx.android.synthetic.main.fragment_wallet_connect_sign_message_request.*

class WalletConnectSignMessageRequestFragment : BaseFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wallet_connect_sign_message_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val baseViewModel by navGraphViewModels<WalletConnectViewModel>(R.id.walletConnectMainFragment)
        val vmFactory = WalletConnectSignMessageRequestModule.Factory(baseViewModel.sharedSignMessageRequest!!, baseViewModel.service)
        val viewModel by viewModels<WalletConnectSignMessageRequestViewModel> { vmFactory }

        when (val message = viewModel.message) {
            is SignMessage.Message,
            is SignMessage.PersonalMessage -> {
                messageHint.text = message.data
                typedMessageViews.isVisible = false
            }
            is SignMessage.TypedMessage -> {
                messageHint.text = getString(R.string.WalletConnect_SignMessageRequest_SignMessageHint)
                domain.bind(
                        title = getString(R.string.WalletConnect_SignMessageRequest_Domain),
                        value = message.domain,
                        listPosition = ListPosition.First
                )
                showMessageButton.apply {
                    bind(
                            title = getString(R.string.WalletConnect_SignMessageRequest_ShowMessageTitle),
                            icon = R.drawable.ic_arrow_right,
                            listPosition = ListPosition.Last
                    )
                    setOnClickListener {
                        findNavController().navigate(R.id.walletConnectSignMessageRequestFragment_to_walletConnectDisplayTypedMessageFragment, bundleOf(TYPED_MESSAGE to formatJson(message.data)), navOptions())
                    }
                }
                typedMessageViews.isVisible = true
            }
        }

        btnReject.setOnSingleClickListener {
            viewModel.reject()
        }

        btnSign.setOnSingleClickListener {
            viewModel.sign()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.reject()
        }

        viewModel.closeLiveEvent.observe(viewLifecycleOwner, {
            baseViewModel.sharedSignMessageRequest = null
            findNavController().popBackStack()
        })
    }

    private fun formatJson(text: String): String {
        val json = StringBuilder()
        var indentString = ""
        for (element in text) {
            when (element) {
                '{', '[' -> {
                    json.append("\n$indentString$element\n")
                    indentString += "\t"
                    json.append(indentString)
                }
                '}', ']' -> {
                    indentString = indentString.replaceFirst("\t".toRegex(), "")
                    json.append("\n$indentString$element")
                }
                ',' -> json.append("$element\n$indentString")
                else -> json.append(element)
            }
        }
        return json.toString()
    }

}
