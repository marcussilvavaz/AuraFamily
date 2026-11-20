package com.example.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class FrankfurterResponse(
    val amount: Double = 1.0,
    val base: String = "BRL",
    val date: String = "",
    val rates: Map<String, Double> = emptyMap()
)

interface FrankfurterApi {
    @GET("v1/latest")
    suspend fun getLatestRates(
        @Query("base") base: String = "BRL",
        @Query("symbols") symbols: String = "USD,EUR,GBP"
    ): FrankfurterResponse
}

class CurrencyExchangeService {

    private val api: FrankfurterApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.frankfurter.dev/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(FrankfurterApi::class.java)
    }

    suspend fun fetchRatesToBrl(current: CurrencyRatesState): CurrencyRatesState = withContext(Dispatchers.IO) {
        try {
            val response = api.getLatestRates(base = "BRL", symbols = "USD,EUR,GBP")
            val brlToUsd = response.rates["USD"]
            val brlToEur = response.rates["EUR"]
            val brlToGbp = response.rates["GBP"]

            val usdToBrl = if (brlToUsd != null && brlToUsd > 0) 1.0 / brlToUsd else current.usdToBrl
            val eurToBrl = if (brlToEur != null && brlToEur > 0) 1.0 / brlToEur else current.eurToBrl
            val gbpToBrl = if (brlToGbp != null && brlToGbp > 0) 1.0 / brlToGbp else current.gbpToBrl

            CurrencyRatesState(
                usdToBrl = (Math.round(usdToBrl * 100.0) / 100.0),
                eurToBrl = (Math.round(eurToBrl * 100.0) / 100.0),
                gbpToBrl = (Math.round(gbpToBrl * 100.0) / 100.0),
                lastUpdatedLabel = "Câmbio online (${response.date.ifBlank { DateCalendarUtils.currentTimestampLabel() }})",
                isLive = true
            )
        } catch (_: Exception) {
            current.copy(
                lastUpdatedLabel = "Câmbio local (${DateCalendarUtils.currentTimestampLabel()})",
                isLive = false
            )
        }
    }
}
