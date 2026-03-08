package com.example.recommandtrip

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowForward // 화살표 아이콘 추가
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onSpotClick: (TourSpot) -> Unit,
    onSearchClick: () -> Unit,
    onCategoryClick: (String, String) -> Unit
) {
    val tourSpots = remember { mutableStateListOf<TourSpot>() }
    val hotSpots = remember { mutableStateListOf<TourSpot>() }
    val nearbySpots = remember { mutableStateListOf<TourSpot>() }
    val festivalSpots = remember { mutableStateListOf<TourSpot>() }

    val isLoading = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(pageCount = { if (festivalSpots.isEmpty()) 1 else festivalSpots.size })

    // [수정] 멘트를 조금 더 차분하고 감성적인 톤으로 변경
    val greetings = listOf(
        "여행 떠나기 좋은 날씨네요 ☀️",
        "반복되는 일상, 환기가 필요하다면 🌿",
        "이번 주말, 특별한 추억을 만들어보세요 📸",
        "지친 마음을 달래줄 여행지를 찾아봤어요 ☕",
        "훌쩍 떠나고 싶은 순간이 있죠 ✈️"
    )

    val greeting = remember { greetings.random() }

    LaunchedEffect(Unit) {
        while(true) {
            delay(4000)
            if (pagerState.pageCount > 1) {
                val nextPage = (pagerState.currentPage + 1) % pagerState.pageCount
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val festivals = fetchRandomFestivals()
                festivalSpots.addAll(festivals)

                val allData = fetchTourDataFromApi(areaCode = "", keyword = "")
                val shuffled = allData.shuffled()
                hotSpots.addAll(shuffled.take(5))
                tourSpots.addAll(shuffled.drop(5))

                val myLocationData = fetchLocationBasedSpots(126.9780, 37.5665, 10000)
                nearbySpots.addAll(myLocationData.take(5))

            } catch (e: Exception) {
                Log.e("HOME_ERR", e.message.toString())
            } finally {
                isLoading.value = false
            }
        }
    }

    if (isLoading.value) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF7F8FA))
        ) {
            // [디자인 수정] 상단 영역을 '헤더'처럼 깔끔하게 정리
            // 랜덤 멘트를 타이틀이 아닌, 앱 이름과 함께 작게 배치하여 시각적 피로도를 줄임
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 12.dp, start = 24.dp, end = 24.dp)
                ) {
                    // 앱 이름 (브랜딩)
                    Text(
                        text = "RecommendTrip",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // 랜덤 멘트 (부드러운 인사말)
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF555555),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 검색버튼
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .height(72.dp)
                        .clickable { onSearchClick() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp) // 그림자로 강조
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "어디로 떠나고 싶으신가요?",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "지역별 추천 여행지 찾아보기",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "검색",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                if (festivalSpots.isNotEmpty()) {
                    HorizontalPager(state = pagerState, contentPadding = PaddingValues(horizontal = 20.dp), pageSpacing = 16.dp) { page ->
                        val safeIndex = page % festivalSpots.size
                        FestivalBannerCard(festivalSpots[safeIndex], onClick = { onSpotClick(festivalSpots[safeIndex]) })
                    }
                } else {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 20.dp)
                        .background(Color.LightGray, RoundedCornerShape(16.dp)))
                }
            }

            // 퀵 메뉴(카테고리)
            item {
                Spacer(modifier = Modifier.height(28.dp))

                val categories = listOf(
                    CategoryData("🌊", "바다", "12", "A01011200"),
                    CategoryData("⛰️", "산", "12", "A01010400"),
                    CategoryData("🏕️", "캠핑", "32", "A05020900"),
                    CategoryData("☕", "카페", "39", "A05020900"),
                    CategoryData("🏨", "호텔", "32", "B02010100"),
                    CategoryData("🎡", "테마파크", "12", "A02020700"),
                    CategoryData("🍖", "맛집", "39", ""),
                    CategoryData("🏛️", "박물관", "14", "A02060100")
                )

                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(categories) { data ->
                        QuickMenuIcon(data.icon, data.label) {
                            // 카테고리별 검색
                            onCategoryClick(data.typeId, data.catCode)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
                SectionTitle("가장 핫한 여행지 (전국)")
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(hotSpots) { spot -> HorizontalSpotCard(spot, onClick = { onSpotClick(spot) }) }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
                SectionTitle("이번 주말엔 여기 어때요? (내 주변) 📍")
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(nearbySpots) { spot -> HorizontalSpotCard(spot, onClick = { onSpotClick(spot) }) }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
                SectionTitle("랜덤 추천 여행지 👀")
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(tourSpots) { spot ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    MainRecommendCard(spot, onClick = { onSpotClick(spot) })
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

data class CategoryData(
    val icon: String,
    val label: String,
    val typeId: String,
    val catCode: String
)

@Composable
fun SectionTitle(title: String) {
    Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333), modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
fun FestivalBannerCard(festival: TourSpot, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(220.dp).clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(festival.imageUrl.ifEmpty { "https://via.placeholder.com/400x200" })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)), startY = 300f))
            )
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(text = festival.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = festival.summary, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = festival.address, color = Color.LightGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun QuickMenuIcon(icon: String, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(modifier = Modifier.size(60.dp).background(Color.White, CircleShape).border(1.dp, Color(0xFFEEEEEE), CircleShape), contentAlignment = Alignment.Center) { Text(icon, fontSize = 24.sp) }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF555555))
    }
}

@Composable
fun HorizontalSpotCard(spot: TourSpot, onClick: () -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.width(160.dp).height(220.dp).clickable { onClick() }, elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column {
            AsyncImage(model = spot.imageUrl.ifEmpty { "https://via.placeholder.com/150" }, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.height(120.dp).fillMaxWidth())
            Column(modifier = Modifier.padding(12.dp)) {
                Text(spot.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(4.dp))
                Text(spot.address, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}