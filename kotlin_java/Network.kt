package com.example.recommandtrip

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val API_KEY = "input_your_API_KEY"

// 상세 정보 가져오기
suspend fun fetchOverview(contentId: String, contentTypeId: String): DetailInfo = withContext(Dispatchers.IO) {
    var detailInfo = DetailInfo(overview = "상세 정보를 불러오는 중입니다...")

    val baseUrl = "https://apis.data.go.kr/B551011/KorService2/detailCommon2"
    val cleanKey = API_KEY.trim()
    val encodedKey = if (cleanKey.contains("%")) cleanKey else URLEncoder.encode(cleanKey, "UTF-8")

    val queryParams = "?" +
            "serviceKey=$encodedKey" +
            "&MobileOS=AND" +
            "&MobileApp=RecommandTrip" +
            "&_type=json" +
            "&contentId=$contentId"

    try {
        val url = URL(baseUrl + queryParams)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        val responseCode = conn.responseCode

        if (responseCode == 200) {
            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(responseText)

            if (root.has("response")) {
                val response = root.getJSONObject("response")
                val header = response.optJSONObject("header")
                val resultCode = header?.optString("resultCode")

                if (resultCode == "0000") {
                    val body = response.optJSONObject("body")
                    val items = body?.optJSONObject("items")?.optJSONArray("item")

                    if (items != null && items.length() > 0) {
                        val item = items.getJSONObject(0)

                        val overview = item.optString("overview", "")
                            .replace("<br>", "\n")
                            .replace("&nbsp;", " ")
                            .replace(Regex("<.*?>"), "")
                            .trim()

                        val homepageRaw = item.optString("homepage", "")
                        val homepageUrl = if (homepageRaw.contains("href")) {
                            homepageRaw.substringAfter("href=\"").substringBefore("\"")
                        } else {
                            if(homepageRaw.startsWith("http")) homepageRaw else ""
                        }.replace(Regex("<.*?>"), "").trim()

                        val tel = item.optString("tel", "").trim()
                        val addr = item.optString("addr1", "").trim()

                        detailInfo = DetailInfo(
                            overview = overview.ifEmpty { "상세 소개글이 없습니다." },
                            homepageUrl = homepageUrl,
                            tel = tel,
                            address = addr
                        )
                    } else {
                        detailInfo = DetailInfo(overview = "제공된 상세 정보가 없습니다. (데이터 없음)")
                    }
                } else {
                    detailInfo = DetailInfo(overview = "API 오류: ${header?.optString("resultMsg")}")
                }
            } else if (root.has("cmmMsgHeader")) {
                val msgHeader = root.getJSONObject("cmmMsgHeader")
                val errMsg = msgHeader.optString("returnAuthMsg")
                val errReason = msgHeader.optString("returnReasonCode")
                detailInfo = DetailInfo(overview = "API 인증 오류: $errMsg ($errReason)")
            } else {
                detailInfo = DetailInfo(overview = "알 수 없는 API 응답 형식입니다.")
            }
        } else {
            detailInfo = DetailInfo(overview = "서버 통신 오류 (Code: $responseCode)")
        }
    } catch (e: Exception) {
        Log.e("OVERVIEW", "Error: ${e.message}")
        detailInfo = DetailInfo(overview = "네트워크 오류: ${e.message}")
    }
    return@withContext detailInfo
}

// 지역 코드 가져오기
suspend fun fetchAreaCodes(areaCode: String? = null): List<AreaCode> = withContext(Dispatchers.IO) {
    val list = mutableListOf<AreaCode>()
    val baseUrl = "https://apis.data.go.kr/B551011/KorService2/areaCode2"
    val cleanKey = API_KEY.trim()
    val encodedKey = if (cleanKey.contains("%")) cleanKey else URLEncoder.encode(cleanKey, "UTF-8")
    var queryParams = "?serviceKey=$encodedKey&numOfRows=100&pageNo=1&MobileOS=AND&MobileApp=RecommandTrip&_type=json"
    if (areaCode != null) queryParams += "&areaCode=$areaCode"

    try {
        val url = URL(baseUrl + queryParams)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(responseText)
        val items = root.getJSONObject("response").optJSONObject("body")?.optJSONObject("items")?.optJSONArray("item")
        if (items != null) {
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                list.add(AreaCode(item.optString("code"), item.optString("name")))
            }
        }
    } catch (e: Exception) { Log.e("AREA_CODE", e.toString()) }
    return@withContext list
}

// 랜덤 축제 정보
suspend fun fetchRandomFestivals(): List<TourSpot> = withContext(Dispatchers.IO) {
    val resultList = mutableListOf<TourSpot>()
    val cleanKey = API_KEY.trim()
    val encodedKey = if (cleanKey.contains("%")) cleanKey else URLEncoder.encode(cleanKey, "UTF-8")
    val today = SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Date())
    val queryParams = "?serviceKey=$encodedKey&numOfRows=20&pageNo=1&MobileOS=AND&MobileApp=RecommandTrip&_type=json&arrange=A&eventStartDate=$today"

    try {
        val url = URL("https://apis.data.go.kr/B551011/KorService2/searchFestival2" + queryParams)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(responseText)
        val items = root.getJSONObject("response").optJSONObject("body")?.optJSONObject("items")?.optJSONArray("item")

        if (items != null) {
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val img = item.optString("firstimage").ifEmpty { item.optString("firstimage2") }
                if (img.isNotEmpty()) {
                    val start = item.optString("eventstartdate")
                    val end = item.optString("eventenddate")
                    val date = if(start.length == 8 && end.length == 8) "${start.substring(4,6)}.${start.substring(6,8)}~${end.substring(4,6)}.${end.substring(6,8)}" else ""
                    resultList.add(TourSpot(item.optString("contentid"), item.optString("contenttypeid"), item.optString("title"), item.optString("addr1"), img, "📅 $date", false, item.optDouble("mapx"), item.optDouble("mapy")))
                }
            }
        }
    } catch (e: Exception) { Log.e("FESTIVAL", e.toString()) }
    return@withContext resultList.shuffled().take(5)
}

// pageNo 매개변수 추가 (기본값 1)
suspend fun fetchTourDataFromApi(
    areaCode: String,
    sigunguCode: String = "",
    keyword: String,
    contentTypeId: String = "12",
    cat3: String = "",
    pageNo: Int = 1 // 페이지 번호를 받음 -> 지역검색 클릭시 관광지 다양화
): List<TourSpot> = withContext(Dispatchers.IO) {
    val resultList = mutableListOf<TourSpot>()
    val cleanKey = API_KEY.trim()
    val encodedKey = if (cleanKey.contains("%")) cleanKey else URLEncoder.encode(cleanKey, "UTF-8")

    // URL 생성 시 pageNo 변수 사용
    var queryParams = "?serviceKey=$encodedKey&numOfRows=20&pageNo=$pageNo&MobileOS=AND&MobileApp=RecommandTrip&_type=json&arrange=Q"
    val baseUrl: String

    if (keyword.isNotEmpty()) {
        baseUrl = "https://apis.data.go.kr/B551011/KorService2/searchKeyword2"
        queryParams += "&keyword=${URLEncoder.encode(keyword, "UTF-8")}"
    } else {
        baseUrl = "https://apis.data.go.kr/B551011/KorService2/areaBasedList2"
        queryParams += "&contentTypeId=$contentTypeId"

        if (areaCode.isNotEmpty() && areaCode != "RANDOM") {
            queryParams += "&areaCode=$areaCode"
            if (sigunguCode.isNotEmpty()) queryParams += "&sigunguCode=$sigunguCode"
        }
    }

    if (cat3.isNotEmpty()) {
        queryParams += "&cat3=$cat3"
        if (contentTypeId == "39") queryParams += "&cat1=A05&cat2=A0502"
    }

    try {
        val url = URL(baseUrl + queryParams)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(responseText)

        if (root.has("response")) {
            val items = root.getJSONObject("response").optJSONObject("body")?.optJSONObject("items")?.optJSONArray("item")
            if (items != null) {
                val phrases = listOf("힐링 여행 🌿", "떠나볼까요? ✈️", "인생샷 명소 📸", "지금 핫한 곳 🔥")
                for (i in 0 until items.length()) {
                    val item = items.getJSONObject(i)
                    val img = item.optString("firstimage").ifEmpty { item.optString("firstimage2") }

                    val itemTypeId = item.optString("contenttypeid")
                    if (keyword.isNotEmpty() && itemTypeId != contentTypeId) {
                        continue
                    }

                    if (img.isNotEmpty()) {
                        resultList.add(TourSpot(
                            item.optString("contentid"),
                            itemTypeId,
                            item.optString("title"),
                            item.optString("addr1"),
                            img,
                            phrases.random(),
                            false,
                            item.optDouble("mapx"),
                            item.optDouble("mapy")
                        ))
                    }
                }
            }
        }
    } catch (e: Exception) { Log.e("API_ERR", e.toString()) }
    return@withContext resultList
}

// [유지] 위치 기반 검색
suspend fun fetchLocationBasedSpots(mapX: Double, mapY: Double, radius: Int = 20000, contentTypeId: String = "12"): List<TourSpot> = withContext(Dispatchers.IO) {
    val resultList = mutableListOf<TourSpot>()
    val cleanKey = API_KEY.trim()
    val encodedKey = if (cleanKey.contains("%")) cleanKey else URLEncoder.encode(cleanKey, "UTF-8")
    val queryParams = "?serviceKey=$encodedKey&numOfRows=20&pageNo=1&MobileOS=AND&MobileApp=RecommandTrip&_type=json&arrange=E&mapX=$mapX&mapY=$mapY&radius=$radius&contentTypeId=$contentTypeId"

    try {
        val url = URL("https://apis.data.go.kr/B551011/KorService2/locationBasedList2" + queryParams)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 15000
        val responseText = conn.inputStream.bufferedReader().use { it.readText() }
        val root = JSONObject(responseText)
        val items = root.getJSONObject("response").optJSONObject("body")?.optJSONObject("items")?.optJSONArray("item")

        if (items != null) {
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val img = item.optString("firstimage").ifEmpty { item.optString("firstimage2") }
                val dist = item.optDouble("dist", 0.0)
                if (img.isNotEmpty()) {
                    resultList.add(TourSpot(item.optString("contentid"), item.optString("contenttypeid"), item.optString("title"), item.optString("addr1"), img, "거리: ${dist.toInt()}m", false, item.optDouble("mapx"), item.optDouble("mapy")))
                }
            }
        }
    } catch (e: Exception) { Log.e("API_LOC_ERR", e.toString()) }
    return@withContext resultList
}