package com.starbase.bankwallet.modules.coin.coinmarkets

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.modules.coin.MarketTickerViewItem
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.view_holder_market_ticker.*

class CoinMarketItemAdapter : ListAdapter<MarketTickerViewItem, ViewHolderMarketTicker>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderMarketTicker {
        return ViewHolderMarketTicker.create(parent)
    }

    override fun onBindViewHolder(holder: ViewHolderMarketTicker, position: Int) = Unit

    override fun onBindViewHolder(holder: ViewHolderMarketTicker, position: Int, payloads: MutableList<Any>) {
        holder.bind(getItem(position), payloads.firstOrNull { it is MarketTickerViewItem } as? MarketTickerViewItem)
    }

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<MarketTickerViewItem>() {
            override fun areItemsTheSame(oldItem: MarketTickerViewItem, newItem: MarketTickerViewItem): Boolean {
                return oldItem.areItemsTheSame(newItem)
            }

            override fun areContentsTheSame(oldItem: MarketTickerViewItem, newItem: MarketTickerViewItem): Boolean {
                return oldItem.areContentsTheSame(newItem)
            }

            override fun getChangePayload(oldItem: MarketTickerViewItem, newItem: MarketTickerViewItem): Any? {
                return oldItem
            }
        }
    }
}

class ViewHolderMarketTicker(override val containerView: View) : RecyclerView.ViewHolder(containerView), LayoutContainer {

    fun bind(item: MarketTickerViewItem, prev: MarketTickerViewItem?) {
        if (item.imageUrl != prev?.imageUrl){
            icon.loadImage(item.imageUrl)
        }

        if (item.title != prev?.title) {
            title.text = item.title
        }

        if (item.subtitle != prev?.subtitle) {
            subtitle.text = item.subtitle
        }

        if (item.value != prev?.value) {
            rate.text = item.value
        }

        if (item.subvalue != prev?.subvalue) {
            marketFieldValue.text = item.subvalue
        }
    }

    companion object {
        fun create(parent: ViewGroup): ViewHolderMarketTicker {
            return ViewHolderMarketTicker(LayoutInflater.from(parent.context).inflate(R.layout.view_holder_market_ticker, parent, false))
        }
    }
}
