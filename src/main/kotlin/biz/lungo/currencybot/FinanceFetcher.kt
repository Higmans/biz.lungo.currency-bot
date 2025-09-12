package biz.lungo.currencybot

import biz.lungo.currencybot.data.FinanceRate
import biz.lungo.currencybot.data.FinanceResponse
import biz.lungo.currencybot.data.FinanceSymbol
import biz.lungo.currencybot.data.Values
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq

const val MINIMUM_COUNT = 3

private val ratesDb = mongoClient.getDatabase("rates")
private val financeCollection = ratesDb.getCollection<FinanceRate>()
private val activeSymbols = FinanceSymbol.entries

suspend fun fetchFinanceRates() {
    val financeRates = activeSymbols.map {
        financeClient.get(FINANCE_RATES_PATH_FORMAT.format(it.name)) {
            contentType(ContentType.Application.Json)
        }.body<FinanceResponse>().toFinanceRate(it).takeIf { rate ->
            rate.ask.getCount() >= MINIMUM_COUNT &&
                    rate.bid.getCount() >= MINIMUM_COUNT &&
                    rate.ask?.rate != null && rate.ask.rate > 0 &&
                    rate.bid?.rate != null && rate.bid.rate > 0
        }
    }
    financeRates.filterNotNull().forEach {
        financeCollection.replaceOrInsertOne(it)
    }
}

suspend fun getFinanceRate(symbol: FinanceSymbol): FinanceRate? =
    financeCollection.find(FinanceRate::symbol eq symbol).first()

private suspend fun CoroutineCollection<FinanceRate>.replaceOrInsertOne(financeRate: FinanceRate) {
    if (countDocuments(FinanceRate::symbol eq financeRate.symbol) > 0) {
        findOneAndReplace(FinanceRate::symbol eq financeRate.symbol, financeRate)
    } else {
        insertOne(financeRate)
    }
}

private fun Values?.getCount() = this?.count ?: 0

private fun FinanceResponse.toFinanceRate(symbol: FinanceSymbol) =
    FinanceRate(symbol, data.bid, data.ask)

