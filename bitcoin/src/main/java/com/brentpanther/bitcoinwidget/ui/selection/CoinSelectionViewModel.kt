package com.brentpanther.bitcoinwidget.ui.selection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import com.brentpanther.bitcoinwidget.Coin
import com.brentpanther.bitcoinwidget.CoinEntry
import com.brentpanther.bitcoinwidget.R
import com.brentpanther.bitcoinwidget.db.WidgetDatabase
import com.google.gson.Gson
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import java.io.InputStreamReader
import java.util.*

class CoinSelectionViewModel(application: Application) : AndroidViewModel(application) {

    fun getWidget(widgetId: Int) = WidgetDatabase.getInstance(getApplication()).widgetDao().getByWidgetIdFlow(widgetId)

    private var allCoins = Coin.values().filterNot { it == Coin.CUSTOM }.associateBy { it.name }
    private var fullCoins: List<CoinEntry> = allCoins.map {
        CoinEntry(it.key, it.value.coinName, it.value.getSymbol(), it.value)
    }.sortedWith { o1, o2 ->
        String.CASE_INSENSITIVE_ORDER.compare(o1.coin.coinName, o2.coin.coinName)
    }
    private var coinList: List<CoinEntry> = mutableListOf()
    private val resource = application.resources.openRawResource(R.raw.othercoins)

    val coins = liveData(Dispatchers.IO) {
        // TODO: load coins in parallel
        emit(fullCoins)
        loadOtherCoins()
        emit(coinList)
    }

    private fun loadOtherCoins() {
        val jsonArray = Gson().fromJson(InputStreamReader(resource), JsonArray::class.java)
        val otherCoins = jsonArray.map {
            val obj = it.asJsonObject
            CoinEntry(
                obj.get("id").asString,
                obj.get("name").asString,
                obj.get("symbol").asString.uppercase(Locale.ROOT),
                Coin.CUSTOM,
                obj.get("icon").asString
            )
        }.filterNot {
            allCoins.containsKey(it.symbol)
        }
        coinList = fullCoins.plus(otherCoins)
    }

}