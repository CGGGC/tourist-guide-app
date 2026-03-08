package com.example.recommandtrip

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecommandTripApp()
        }
    }
}

@Composable
fun RecommandTripApp() {
    val appColors = lightColorScheme(
        primary = Color(0xFF62EFFF),
        onPrimary = Color.Black,
        secondary = Color(0xFF03DAC5),
        background = Color(0xFFF7F8FA),
        surface = Color.White
    )

    // Firebase
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // 로그인 상태 감지
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
    var userName by remember { mutableStateOf(auth.currentUser?.email ?: "") }

    // 로그인 상태 변경
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            isLoggedIn = user != null
            userName = user?.email ?: ""
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    var currentScreen by remember { mutableStateOf("home") }

    // 뒤로가기 -> 스택으로(이전 관광지들 복원위함)
    val selectedSpotStack = remember { mutableStateListOf<TourSpot>() }
    val currentSelectedSpot = selectedSpotStack.lastOrNull()

    // 검색 상태 관리
    var searchQuery by remember { mutableStateOf("") }
    var initialTypeId by remember { mutableStateOf("12") }
    var initialCat3 by remember { mutableStateOf("") }

    val searchResults = remember { mutableStateListOf<TourSpot>() }
    var searchFilterMode by remember { mutableStateOf("Rank") }
    val randomSearchResults = remember { mutableStateListOf<TourSpot>() }

    val rankListState = rememberLazyListState()
    val randomListState = rememberLazyListState()
    val recentListState = rememberLazyListState()
    val recentSpots = remember { mutableStateListOf<TourSpot>() }

    val scope = rememberCoroutineScope()

    MaterialTheme(colorScheme = appColors) {
        if (currentSelectedSpot != null) {
            DetailScreen(
                spot = currentSelectedSpot,
                isLoggedIn = isLoggedIn,
                onBack = {
                    if (selectedSpotStack.isNotEmpty()) {
                        selectedSpotStack.removeLast()
                    }
                },
                // 찜하기 DB 연동
                onToggleFavorite = { spot ->
                    val user = auth.currentUser
                    if (user != null) {
                        val docRef = db.collection("travelers").document(user.uid)
                            .collection("favorites").document(spot.contentId)

                        if (spot.isFavorite) {
                            // DB 저장
                            docRef.set(spot)
                                .addOnFailureListener {
                                    Toast.makeText(context, "저장 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            // DB에서 삭제
                            docRef.delete()
                                .addOnFailureListener {
                                    Toast.makeText(context, "삭제 실패: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                    }
                },
                onSpotClick = { spot ->
                    selectedSpotStack.add(spot)
                    addToRecent(recentSpots, spot)
                }
            )
        } else {
            Scaffold(
                bottomBar = {
                    if (currentScreen != "map") {
                        BottomNavigationBar(
                            currentScreen = currentScreen,
                            onItemSelected = { route ->
                                currentScreen = route
                                if (route == "search") {
                                    searchQuery = ""
                                    initialTypeId = "12"
                                    initialCat3 = ""
                                    // 탭 누를 때마다 이전 검색/랜덤 결과 초기화 -> 새로운 랜덤 추천 유도
                                    searchResults.clear()
                                    randomSearchResults.clear()
                                }
                            }
                        )
                    }
                }
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (currentScreen) {
                        "home" -> HomeScreen(
                            onSpotClick = {
                                selectedSpotStack.add(it)
                                addToRecent(recentSpots, it)
                            },
                            onSearchClick = {
                                searchQuery = ""
                                initialTypeId = "12"
                                initialCat3 = ""
                                searchResults.clear()
                                randomSearchResults.clear()
                                currentScreen = "search"
                            },
                            onCategoryClick = { typeId, cat3 ->
                                initialTypeId = typeId
                                initialCat3 = cat3
                                searchQuery = ""
                                searchResults.clear() // 이전 결과 지우기
                                randomSearchResults.clear()
                                currentScreen = "search"
                            }
                        )
                        "search" -> SearchRegionScreen(
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            initialTypeId = initialTypeId,
                            initialCat3 = initialCat3,
                            searchResults = searchResults,
                            filterMode = searchFilterMode,
                            onFilterModeChange = { searchFilterMode = it },
                            randomSearchResults = randomSearchResults,
                            rankState = rankListState,
                            randomState = randomListState,
                            recentState = recentListState,
                            recentSpots = recentSpots,
                            onSpotClick = {
                                selectedSpotStack.add(it)
                                addToRecent(recentSpots, it)
                            },
                            onMapClick = { currentScreen = "map" }
                        )
                        "map" -> MapScreen(
                            onBack = { currentScreen = "search" },
                            onSpotClick = {
                                selectedSpotStack.add(it)
                                addToRecent(recentSpots, it)
                            }
                        )
                        "mypage" -> MyPageScreen(
                            isLoggedIn = isLoggedIn,
                            userName = userName,
                            onLogin = {
                                // 로그인 상태 AuthStateListener 처리
                            },
                            onLogout = {
                                auth.signOut()
                            },
                            onSpotClick = {
                                selectedSpotStack.add(it)
                                addToRecent(recentSpots, it)
                            }
                        )
                    }
                }
            }
        }
    }
}

fun addToRecent(list: MutableList<TourSpot>, spot: TourSpot) {
    list.removeIf { it.contentId == spot.contentId }
    list.add(0, spot)
    if (list.size > 20) list.removeLast()
}

@Composable
fun BottomNavigationBar(currentScreen: String, onItemSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 10.dp,
        modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        val items = listOf(
            Triple("home", "홈", Icons.Default.Home),
            Triple("search", "지역검색", Icons.Default.Search),
            Triple("mypage", "내 정보", Icons.Default.AccountCircle)
        )

        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontWeight = FontWeight.SemiBold) },
                selected = currentScreen == route,
                onClick = { onItemSelected(route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
            )
        }
    }
}