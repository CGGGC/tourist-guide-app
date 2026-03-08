package com.example.recommandtrip

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.* // [수정] 모든 기본 아이콘(ArrowBack, ArrowForward, Send 등) 포함
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DetailScreen(
    spot: TourSpot,
    isLoggedIn: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: (TourSpot) -> Unit,
    onSpotClick: (TourSpot) -> Unit
) {
    var reviewText by remember { mutableStateOf("") }
    // Firestore에서 불러온 리뷰 리스트
    val reviews = remember { mutableStateListOf<Review>() }

    var isFav by remember { mutableStateOf(globalFavorites.any { it.contentId == spot.contentId }) }
    var detailInfo by remember { mutableStateOf(DetailInfo()) }

    val nearbyAttractions = remember { mutableStateListOf<TourSpot>() }
    val nearbyFoods = remember { mutableStateListOf<TourSpot>() }

    var isExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uriHandler = LocalUriHandler.current
    val listState = rememberLazyListState()

    // Firebase 관련 저장&불러오기
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // 1. 찜 상태 초기 확인
    LaunchedEffect(spot.contentId) {
        val user = auth.currentUser
        if (user != null) {
            db.collection("travelers").document(user.uid)
                .collection("favorites").document(spot.contentId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        isFav = true
                        spot.isFavorite = true
                    } else {
                        isFav = false
                        spot.isFavorite = false
                    }
                }
        }
    }

    // 2. 리뷰 실시간 불러오기 (사용자별 리뷰 -> firebase 저장)
    LaunchedEffect(spot.contentId) {
        reviews.clear()

        db.collection("reviews")
            .whereEqualTo("contentId", spot.contentId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Firestore", "리뷰 불러오기 실패", e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    reviews.clear()
                    for (doc in snapshot) {
                        try {
                            val review = doc.toObject(Review::class.java)
                            reviews.add(review)
                        } catch (e: Exception) {
                            Log.e("Firestore", "리뷰 데이터 변환 오류", e)
                        }
                    }
                }
            }
    }

    // 3. 상세 정보 및 주변 추천 불러오기
    LaunchedEffect(spot.contentId) {
        listState.scrollToItem(0)
        scope.launch {
            detailInfo = fetchOverview(spot.contentId, spot.contentTypeId)

            if (spot.mapX > 0 && spot.mapY > 0) {
                val attractions = fetchLocationBasedSpots(spot.mapX, spot.mapY, 5000, "12")
                nearbyAttractions.clear()
                nearbyAttractions.addAll(attractions.filter { it.contentId != spot.contentId }.shuffled().take(10))

                val foods = fetchLocationBasedSpots(spot.mapX, spot.mapY, 5000, "39")
                nearbyFoods.clear()
                nearbyFoods.addAll(foods.filter { it.contentId != spot.contentId }.shuffled().take(10))
            }
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)
            ) {
                // 1. 히어로 이미지
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(350.dp)) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(spot.imageUrl.ifEmpty { "https://via.placeholder.com/400x300?text=No+Image" })
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.White), startY = 500f)))
                    }
                }

                // 2. 메인 정보
                item {
                    Column(modifier = Modifier.fillMaxWidth().offset(y = (-30).dp).padding(horizontal = 24.dp)) {
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(spot.title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF222222), lineHeight = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.Place, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = spot.address, fontSize = 14.sp, color = Color.Gray)
                                }
                            }

                            // 찜버튼
                            Surface(shape = CircleShape, shadowElevation = 4.dp, color = Color.White, modifier = Modifier.size(48.dp).clickable {
                                if (isLoggedIn) {
                                    val user = auth.currentUser
                                    if (user != null) {
                                        // UI 업데이트
                                        isFav = !isFav
                                        spot.isFavorite = isFav

                                        val docRef = db.collection("travelers").document(user.uid)
                                            .collection("favorites").document(spot.contentId)

                                        if (isFav) {
                                            // 저장
                                            docRef.set(spot)
                                                .addOnSuccessListener {
                                                    Log.d("Firestore", "찜 저장 성공: ${spot.title}")
                                                    Toast.makeText(context, "저장되었습니다", Toast.LENGTH_SHORT).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("Firestore", "찜 저장 실패", e)
                                                    Toast.makeText(context, "저장 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                                    // 실패 시 UI 롤백
                                                    isFav = !isFav
                                                    spot.isFavorite = isFav
                                                }
                                        } else {
                                            // 삭제
                                            docRef.delete()
                                                .addOnSuccessListener {
                                                    Log.d("Firestore", "찜 삭제 성공: ${spot.title}")
                                                    Toast.makeText(context, "저장이 취소되었습니다", Toast.LENGTH_SHORT).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("Firestore", "찜 삭제 실패", e)
                                                    Toast.makeText(context, "취소 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                                }
                                        }
                                        // 목록 갱신
                                        onToggleFavorite(spot)
                                    } else {
                                        Toast.makeText(context, "로그인 정보가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(imageVector = if (isFav) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "찜하기", tint = if (isFav) Color(0xFFFF4081) else Color.Gray, modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 관광지 홈페이지
                        if (detailInfo.homepageUrl.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(4.dp),
                                modifier = Modifier.fillMaxWidth().clickable { uriHandler.openUri(detailInfo.homepageUrl) }
                            ) {
                                Box(modifier = Modifier.background(Brush.horizontalGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))).padding(vertical = 16.dp, horizontal = 20.dp), contentAlignment = Alignment.Center) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Outlined.Info, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("공식 홈페이지 방문하기", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.weight(1f))
                                        // [수정] Icons.Default.ArrowForward 사용
                                        Icon(Icons.Default.ArrowForward, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // 소개글
                        Text("소개", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(modifier = Modifier.animateContentSize()) {
                            Text(
                                text = detailInfo.overview,
                                fontSize = 15.sp,
                                color = Color(0xFF444444),
                                lineHeight = 24.sp,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isExpanded) "접기" else "더보기",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.clickable { isExpanded = !isExpanded }
                            )
                        }
                    }
                }

                // 3. 추천 리스트
                if (nearbyAttractions.isNotEmpty()) {
                    item { RecommendationSection("근처 가볼만한 곳 🚩", nearbyAttractions, onSpotClick) }
                }

                if (nearbyFoods.isNotEmpty()) {
                    item { RecommendationSection("근처 맛집 & 카페 🍽️☕️", nearbyFoods, onSpotClick) }
                }

                // 4. 리뷰 섹션
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                        Text("여행자 톡 💬", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        if (reviews.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(80.dp).background(Color(0xFFF9F9F9), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                                Text("첫 번째 팁을 남겨보세요!", color = Color.Gray, fontSize = 14.sp)
                            }
                        } else {
                            reviews.forEach { review -> ReviewItem(review); Spacer(modifier = Modifier.height(12.dp)) }
                        }
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }

            // 뒤로가기 버튼
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(top = 40.dp, start = 16.dp).align(Alignment.TopStart).size(40.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            // 하단 리뷰 입력창
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = Color.White,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = reviewText, onValueChange = { reviewText = it },
                        placeholder = { Text("꿀팁 공유하기...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = {
                            if (isLoggedIn) {
                                if (reviewText.isNotBlank()) {
                                    val user = auth.currentUser
                                    val userId = user?.uid ?: "anonymous"
                                    val userName = user?.email?.substringBefore("@") ?: "여행자"
                                    val date = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date())

                                    val newReview = Review(
                                        userId = userId,
                                        author = userName,
                                        content = reviewText,
                                        date = date,
                                        contentId = spot.contentId,
                                        timestamp = System.currentTimeMillis()
                                    )

                                    db.collection("reviews").add(newReview)
                                        .addOnSuccessListener {
                                            reviewText = ""
                                            keyboardController?.hide()
                                            Toast.makeText(context, "소중한 팁이 등록되었습니다!", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(context, "등록 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            } else Toast.makeText(context, "로그인이 필요한 서비스입니다.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(50.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(Icons.Default.Send, null, tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun RecommendationSection(title: String, recommendList: List<TourSpot>, onItemClick: (TourSpot) -> Unit) {
    Column {
        Spacer(modifier = Modifier.height(30.dp))
        Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(recommendList) { place -> NearbyPlaceCard(place) { onItemClick(place) } }
        }
    }
}

@Composable
fun NearbyPlaceCard(place: TourSpot, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(140.dp).height(180.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(place.imageUrl.ifEmpty { "https://via.placeholder.com/150" }).crossfade(true).build(),
                contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.height(100.dp).fillMaxWidth()
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(place.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(place.address, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun ReviewItem(review: Review) {
    Row(verticalAlignment = Alignment.Top) {
        Box(modifier = Modifier.size(36.dp).background(Color(0xFFEEEEEE), CircleShape), contentAlignment = Alignment.Center) {
            Text(review.author.take(1), fontWeight = FontWeight.Bold, color = Color.Gray)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(review.author, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(review.date, fontSize = 11.sp, color = Color.LightGray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(review.content, fontSize = 14.sp, color = Color(0xFF444444), lineHeight = 20.sp)
        }
    }
}