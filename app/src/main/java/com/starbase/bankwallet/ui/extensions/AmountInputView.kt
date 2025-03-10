package com.starbase.bankwallet.ui.extensions

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.fiat.AmountTypeSwitchService
import io.horizontalsystems.core.helpers.KeyboardHelper
import kotlinx.android.synthetic.main.view_input_amount.view.*

class AmountInputView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : ConstraintLayout(context, attrs, defStyleAttr) {

    var maxButtonVisible: Boolean = false
        set(value) {
            field = value
            syncButtonStates()
        }

    var onTextChangeCallback: ((prevText: String?, newText: String?) -> Unit)? = null
    var onTapMaxCallback: (() -> Unit)? = null
    var onTapSecondaryCallback: (() -> Unit)? = null

    private val textWatcher = object : TextWatcher {
        private var prevValue: String? = null

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChangeCallback?.invoke(prevValue, s?.toString())
            syncButtonStates()
        }

        override fun afterTextChanged(s: Editable?) {}
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            prevValue = s?.toString()
        }
    }

    init {
        inflate(context, R.layout.view_input_amount, this)

        editTxtAmount.addTextChangedListener(textWatcher)

        btnMax.setOnClickListener {
            onTapMaxCallback?.invoke()
        }
    }

    fun getAmount(): String? {
        return editTxtAmount.text?.toString()
    }

    fun setAmount(text: String?, skipChangeEvent: Boolean = true) {
        editTxtAmount.apply {
            if (skipChangeEvent) {
                removeTextChangedListener(textWatcher)
            }

            setText(text)
            editTxtAmount.setSelection(text?.length ?: 0)
            syncButtonStates()

            if (skipChangeEvent) {
                addTextChangedListener(textWatcher)
            }
        }
    }

    fun setInputParams(inputParams: InputParams) {
        val primaryColor = getPrimaryTextColor(inputParams.amountType)
        topAmountPrefix.setTextColor(context.getColor(primaryColor))
        editTxtAmount.setTextColor(context.getColor(primaryColor))

        topAmountPrefix.text = inputParams.primaryPrefix
        topAmountPrefix.isVisible = inputParams.primaryPrefix?.isNotBlank() ?: false

        val hintTextColor = getSecondaryTextColor(inputParams.amountType, inputParams.switchEnabled)

        txtHintInfo.setTextColor(context.getColor(hintTextColor))

        secondaryArea.setOnClickListener {
            if (inputParams.switchEnabled) onTapSecondaryCallback?.invoke() else null
        }
    }

    private fun getPrimaryTextColor(type: AmountTypeSwitchService.AmountType): Int {
        return when (type) {
            AmountTypeSwitchService.AmountType.Currency -> R.color.jacob
            AmountTypeSwitchService.AmountType.Coin -> R.color.oz
        }
    }

    private fun getSecondaryTextColor(type: AmountTypeSwitchService.AmountType, switchEnabled: Boolean): Int {
        return when {
            !switchEnabled -> R.color.grey_50
            type == AmountTypeSwitchService.AmountType.Coin -> R.color.jacob
            else -> R.color.oz
        }
    }

    fun setSecondaryText(text: String?) {
        txtHintInfo.text = text
    }

    fun setFocus() {
        KeyboardHelper.showKeyboard(context, editTxtAmount)
    }

    fun revertAmount(amount: String?) {
        amount ?: return

        setAmount(amount)
        val shake = AnimationUtils.loadAnimation(context, R.anim.shake_edittext)
        editTxtAmount.startAnimation(shake)
    }

    fun setEstimated(visible: Boolean) {
        estimatedLabel.isVisible = visible
    }

    fun setAmountEnabled(enabled: Boolean) {
        editTxtAmount.isEnabled = enabled
    }

    private fun syncButtonStates() {
        btnMax.isVisible = maxButtonVisible && editTxtAmount.text.isNullOrBlank()
    }

    class InputParams(val amountType: AmountTypeSwitchService.AmountType, val primaryPrefix: String?, val switchEnabled: Boolean)
}
