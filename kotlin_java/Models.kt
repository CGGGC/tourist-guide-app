package com.example.recommandtrip

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf

// Firebase 연동 위해 모든 필드에 기본값(= "") 추가
data class TourSpot(
    val contentId: String = "",
    val contentTypeId: String = "",
    val title: String = "",
    val address: String = "",
    val imageUrl: String = "",
    val summary: String = "",
    var isFavorite: Boolean = false,
    val mapX: Double = 0.0,
    val mapY: Double = 0.0
)

// userId 필드 추가 (이게 없어서 Detail.kt 오류남)
data class Review(
    val userId: String = "",
    val author: String = "",
    val content: String = "",
    val date: String = "",
    val contentId: String = "", // 리뷰가 달린 관광지 ID 식별용
    val timestamp: Long = 0L    // 정렬용 타임스탬프
)

data class AreaCode(
    val code: String = "",
    val name: String = ""
)

data class DetailInfo(
    val overview: String = "",
    val homepageUrl: String = "",
    val tel: String = "",
    val address: String = ""
)

val globalReviews = mutableStateMapOf<String, MutableList<Review>>()
val globalFavorites = mutableStateListOf<TourSpot>()