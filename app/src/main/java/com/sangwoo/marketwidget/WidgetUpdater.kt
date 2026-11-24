package com.sangwoo.marketwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WidgetUpdater {

    private const val BASE = "https://raw.githubusercontent.com/sangwoo1102-gif/market-widget-data/main/"
    private const val MARKETS_URL = BASE + "markets.json"
    private const val FED_URL = BASE + "fed.json"
    const val DASHBOARD_URL = "https://claude.ai/artifact/BzKxcXniNsWAJUhWFeSVAB"

    private val LEFT = listOf("spx" to "S&P500", "ixic" to "나스닥", "dji" to "다우", "sox" to "반도체(SOX)")
    private val RIGHT = listOf(
        "ust2y" to "美 2년", "ust10y" to "美 10년", "ust30y" to "美 30년",
        "wti" to "WTI", "brent" to "브렌트"
    )

    private val num2 = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    fun update(context: Context) {
        val prefs = context.getSharedPreferences("cache", Context.MODE_PRIVATE)
        var failed = false
        try {
            val stamp = System.currentTimeMillis() / 60000
            val markets = fetch("$MARKETS_URL?t=$stamp")
            val fed = fetch("$FED_URL?t=$stamp")
            JSONObject(markets).getJSONArray("rows")
            JSONObject(fed).getJSONArray("rows")
            prefs.edit()
                .putString("markets", markets)
                .putString("fed", fed)
                .putLong("fetchedAt", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            failed = true
        }

        val views = render(
            context,
            prefs.getString("markets", null),
            prefs.getString("fed", null),
            prefs.getLong("fetchedAt", 0L),
            failed
        )
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, MarketWidgetProvider::class.java))
        if (ids.isNotEmpty()) manager.updateAppWidget(ids, views)
    }

    private fun fetch(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 4000
        conn.readTimeout = 4000
        conn.useCaches = false
        try {
            if (conn.responseCode != 200) throw IllegalStateException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun rowsById(json: String?): Map<String, JSONObject> {
        if (json == null) return emptyMap()
        return try {
            val arr: JSONArray = JSONObject(json).getJSONArray("rows")
            (0 until arr.length()).map { arr.getJSONObject(it) }.associateBy { it.optString("id") }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun JSONObject.num(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key).takeIf { !it.isNaN() }

    private fun render(context: Context, marketsJson: String?, fedJson: String?, fetchedAt: Long, failed: Boolean): RemoteViews {
        val pkg = context.packageName
        val views = RemoteViews(pkg, R.layout.widget)
        val markets = rowsById(marketsJson)
        val fed = rowsById(fedJson)

        val up = context.getColor(R.color.up)
        val down = context.getColor(R.color.down)
        val flat = context.getColor(R.color.mute)
        fun colorOf(v: Double?) = when {
            v == null -> flat
            v > 0 -> up
            v < 0 -> down
            else -> flat
        }

        // 제목: 종가 기준일
        val date = markets["spx"]?.optString("date").orEmpty()
        val title = if (date.length >= 10) {
            "美 증시 마감 · ${date.substring(5, 7).trimStart('0')}/${date.substring(8, 10).trimStart('0')}"
        } else if (markets.isEmpty()) {
            if (failed) "데이터를 불러오지 못했습니다" else "불러오는 중…"
        } else "美 증시 마감"
        views.setTextViewText(R.id.title, title)

        val status = when {
            failed && markets.isNotEmpty() -> "갱신 실패"
            fetchedAt > 0 -> SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(fetchedAt)) + " 갱신"
            else -> ""
        }
        views.setTextViewText(R.id.status, status)

        // 왼쪽: 지수
        views.removeAllViews(R.id.col_left)
        for ((id, label) in LEFT) {
            val r = markets[id] ?: continue
            val pct = r.num("chg_pct")
            val row = RemoteViews(pkg, R.layout.widget_row)
            row.setTextViewText(R.id.name, label)
            row.setTextViewText(R.id.value, r.num("value")?.let { num2.format(it) } ?: "—")
            row.setTextViewText(R.id.chg, pct?.let { String.format(Locale.US, "%+.2f%%", it) } ?: "")
            row.setTextColor(R.id.chg, colorOf(pct))
            views.addView(R.id.col_left, row)
        }

        // 오른쪽: 국채 금리, 유가
        views.removeAllViews(R.id.col_right)
        for ((id, label) in RIGHT) {
            val r = markets[id] ?: continue
            val row = RemoteViews(pkg, R.layout.widget_row)
            row.setTextViewText(R.id.name, label)
            val value = r.num("value")
            if (r.optString("unit") == "%") {
                val bp = r.num("chg_bp")
                row.setTextViewText(R.id.value, value?.let { num2.format(it) + "%" } ?: "—")
                row.setTextViewText(R.id.chg, when {
                    bp == null -> ""
                    bp == 0.0 -> "보합"
                    else -> String.format(Locale.US, "%+.0fbp", bp)
                })
                row.setTextColor(R.id.chg, colorOf(bp))
            } else {
                val pct = r.num("chg_pct")
                row.setTextViewText(R.id.value, value?.let { num2.format(it) } ?: "—")
                row.setTextViewText(R.id.chg, pct?.let { String.format(Locale.US, "%+.2f%%", it) } ?: "")
                row.setTextColor(R.id.chg, colorOf(pct))
            }
            views.addView(R.id.col_right, row)
        }

        // 아래: FOMC 한 줄
        val fedLine = fed["hike_prob"]?.let {
            it.optString("item").replace("(CME FedWatch)", "").trim() + " " + it.optString("value")
        } ?: fed["next_fomc"]?.let { "다음 FOMC " + it.optString("value") } ?: ""
        views.setTextViewText(R.id.fed, fedLine)

        // 탭: 대시보드 열기 / ↻: 새로고침
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(Intent.ACTION_VIEW, Uri.parse(DASHBOARD_URL)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.root, open)

        val refresh = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, MarketWidgetProvider::class.java).setAction(MarketWidgetProvider.ACTION_REFRESH),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        views.setOnClickPendingIntent(R.id.refresh, refresh)

        return views
    }
}
