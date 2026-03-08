package com.example.recommandtrip

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.util.UUID

data class CategoryItem(val name: String, val code: String, val cat3: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchRegionScreen(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    initialTypeId: String,
    initialCat3: String,
    searchResults: MutableList<TourSpot>,
    filterMode: String,
    onFilterModeChange: (String) -> Unit,
    randomSearchResults: MutableList<TourSpot>,
    rankState: LazyListState,
    randomState: LazyListState,
    recentState: LazyListState,
    recentSpots: List<TourSpot>,
    onSpotClick: (TourSpot) -> Unit,
    onMapClick: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    val sheetPeekHeight = 140.dp

    var showRegionSheet by rememberSaveable { mutableStateOf(false) }
    var selectedAreaName by rememberSaveable { mutableStateOf("전국") }
    var selectedSigunguName by rememberSaveable { mutableStateOf("전체") }
    var currentAreaCode by rememberSaveable { mutableStateOf("") }
    var currentSigunguCode by rememberSaveable { mutableStateOf("") }
    var isKeywordSearchMode by rememberSaveable { mutableStateOf(false) }

    val entryKey = rememberSaveable { UUID.randomUUID().toString() }

    val categories = listOf(
        CategoryItem("관광지", "12"),
        CategoryItem("음식점", "39"),
        CategoryItem("카페", "39", "A05020900"),
        CategoryItem("숙소", "32"),
        CategoryItem("축제", "15"),
        CategoryItem("쇼핑", "38"),
        CategoryItem("레포츠", "28"),
        CategoryItem("박물관", "14", "A02060100")
    )

    var selectedCategoryIndex by rememberSaveable(initialTypeId, initialCat3) {
        val index = categories.indexOfFirst {
            if (initialCat3.isNotEmpty()) it.code == initialTypeId && it.cat3 == initialCat3
            else it.code == initialTypeId
        }
        mutableIntStateOf(if (index >= 0) index else 0)
    }

    val selectedCategory = categories[selectedCategoryIndex]

    val randomStartPoints = listOf(
        LatLng(37.5665, 126.9780), // 서울
        LatLng(35.1796, 129.0756), // 부산
        LatLng(35.8714, 128.6014), // 대구
        LatLng(37.4563, 126.7052), // 인천
        LatLng(35.1595, 126.8526), // 광주
        LatLng(36.3504, 127.3845), // 대전
        LatLng(35.5384, 129.3114), // 울산
        LatLng(33.4996, 126.5312), // 제주
        LatLng(37.7519, 128.8760), // 강릉
        LatLng(35.8242, 127.1480)  // 전주
    )
    val startPoint = rememberSaveable { randomStartPoints.random() }

    var mapLat by rememberSaveable { mutableDoubleStateOf(startPoint.latitude) }
    var mapLng by rememberSaveable { mutableDoubleStateOf(startPoint.longitude) }
    var mapZoom by rememberSaveable { mutableFloatStateOf(13f) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(mapLat, mapLng), mapZoom)
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            mapLat = cameraPositionState.position.target.latitude
            mapLng = cameraPositionState.position.target.longitude
            mapZoom = cameraPositionState.position.zoom
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState()

    fun fitCameraToSpots(spots: List<TourSpot>) {
        if (spots.isEmpty()) return
        val validSpots = spots.filter { it.mapX > 0.0 && it.mapY > 0.0 }
        if (validSpots.isEmpty()) return

        scope.launch {
            try {
                if (validSpots.size == 1) {
                    val spot = validSpots[0]
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(spot.mapY, spot.mapX), 14f))
                } else {
                    val builder = LatLngBounds.Builder()
                    validSpots.forEach { builder.include(LatLng(it.mapY, it.mapX)) }
                    val bounds = builder.build()
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 200))
                }
            } catch (e: Exception) { }
        }
    }

    fun doSearch(
        query: String = searchQuery,
        typeId: String = selectedCategory.code,
        cat3: String = selectedCategory.cat3,
        area: String = currentAreaCode,
        sigungu: String = currentSigunguCode,
        isRefresh: Boolean = false
    ) {
        isLoading = true
        keyboardController?.hide()
        isKeywordSearchMode = false

        scope.launch {
            searchResults.clear()

            //  새로고침isRefresh 일 경우 1~5페이지 중 랜덤한 페이지를 요청
            // 일반 검색일 경우 1페이지를 요청
            // 이거 안 하면 대장경테마파크 외 19개 똑같은 관광지 잔뜩
            val pageToLoad = if (isRefresh) (1..5).random() else 1

            val data = fetchTourDataFromApi(
                areaCode = area,
                sigunguCode = sigungu,
                keyword = query,
                contentTypeId = typeId,
                cat3 = cat3,
                pageNo = pageToLoad // 랜덤 페이지 전달
            )

            val finalData = if (isRefresh) data.shuffled() else data
            searchResults.addAll(finalData)

            if (searchResults.isNotEmpty()) {
                val targetSpot = if (isRefresh) {
                    searchResults.filter { it.mapX > 0.0 && it.mapY > 0.0 }.randomOrNull()
                } else {
                    searchResults.firstOrNull { it.mapX > 0.0 && it.mapY > 0.0 }
                }

                if (targetSpot != null) {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(LatLng(targetSpot.mapY, targetSpot.mapX), 13f)
                    )
                }
            }

            isLoading = false
        }
    }

    fun searchInCurrentMap() {
        isLoading = true
        val target = cameraPositionState.position.target
        scope.launch {
            searchResults.clear()
            // gps? how?
            val data = fetchLocationBasedSpots(target.longitude, target.latitude, 10000, selectedCategory.code)
            searchResults.addAll(data.shuffled())

            fitCameraToSpots(searchResults)
            isLoading = false
        }
    }

    LaunchedEffect(initialTypeId, initialCat3, currentAreaCode) {
        //  isRefresh = true -> false 하니 대장경테마파크만 나옴
        doSearch(
            query = "",
            typeId = initialTypeId,
            cat3 = initialCat3,
            isRefresh = true
        )
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = sheetPeekHeight,
        sheetContainerColor = Color.White,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetShadowElevation = 10.dp,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            items(categories.size) { index ->
                                val item = categories[index]
                                val isSelected = selectedCategoryIndex == index
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            // 이미 선택된 항목을 다시 누르면 -> 새로고침 (랜덤 페이지)
                                            doSearch(typeId = item.code, cat3 = item.cat3, isRefresh = true)
                                        } else {
                                            // [수정] 새로운 카테고리를 눌러도 1페이지(고정)가 아닌 랜덤 페이지를 보여줘서
                                            // 더 다양한 장소를 탐색할 수 있도록 isRefresh = true로 변경
                                            selectedCategoryIndex = index
                                            doSearch(typeId = item.code, cat3 = item.cat3, isRefresh = true)
                                        }
                                    },
                                    label = { Text(item.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected)
                                )
                            }
                        }
                        HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))
                    }

                    if (isLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (searchResults.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Search, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "이 주변엔 결과가 없네요.",
                                        color = Color.Gray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("지도를 움직여 '현 지도에서 검색'을\n눌러보세요!", fontSize = 12.sp, color = Color.LightGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                    } else {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("주변 추천 장소 ${searchResults.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        items(searchResults) { spot ->
                            SearchVerticalCard(
                                spot = spot,
                                onClick = {
                                    if (spot.mapY > 0.0 && spot.mapX > 0.0) {
                                        scope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newLatLngZoom(LatLng(spot.mapY, spot.mapX), 15f)
                                            )
                                            mapLat = spot.mapY
                                            mapLng = spot.mapX
                                            mapZoom = 15f
                                            scaffoldState.bottomSheetState.partialExpand()
                                        }
                                    }
                                },
                                onDetailClick = { onSpotClick(spot) }
                            )
                            HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
            ) {
                searchResults.forEach { spot ->
                    if (spot.mapY > 0.0 && spot.mapX > 0.0) {
                        MarkerComposable(
                            state = MarkerState(position = LatLng(spot.mapY, spot.mapX)),
                            keys = arrayOf(spot.contentId),
                            onClick = {
                                onSpotClick(spot)
                                false
                            }
                        ) {
                            CustomMapMarker(title = spot.title, isSelected = false)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp).align(Alignment.TopCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(6.dp, RoundedCornerShape(26.dp))
                        .clickable { showRegionSheet = true },
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.List, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (currentAreaCode.isEmpty()) "어디로 떠나볼까요?" else "$selectedAreaName $selectedSigunguName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF333333)
                            )
                            if (currentAreaCode.isEmpty()) {
                                Text("터치하여 지역 선택", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 12.dp))

                        IconButton(
                            onClick = {
                                isKeywordSearchMode = !isKeywordSearchMode
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isKeywordSearchMode) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "상세 검색",
                                tint = Color.Gray
                            )
                        }
                    }
                }

                if (isKeywordSearchMode) {
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        placeholder = { Text("장소명 검색 (예: 시장, 미술관)") },
                        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { doSearch() }),
                        trailingIcon = {
                            IconButton(onClick = { doSearch() }) {
                                Icon(Icons.Default.ArrowForward, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { searchInCurrentMap() },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(4.dp),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("현 지도에서 검색", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    if (showRegionSheet) {
        RegionSelectorSheet(
            onDismiss = { showRegionSheet = false },
            onRegionSelected = { areaCode, areaName, sigunguCode, sigunguName ->
                currentAreaCode = areaCode
                selectedAreaName = areaName
                currentSigunguCode = sigunguCode
                selectedSigunguName = sigunguName
                showRegionSheet = false
                doSearch(area = areaCode, sigungu = sigunguCode)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionSelectorSheet(
    onDismiss: () -> Unit,
    onRegionSelected: (String, String, String, String) -> Unit
) {
    val areas = remember { mutableStateListOf<AreaCode>() }
    val sigungus = remember { mutableStateListOf<AreaCode>() }

    var selectedAreaCode by remember { mutableStateOf<String?>(null) }
    var selectedAreaName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val list = fetchAreaCodes(null)
        areas.clear()
        areas.addAll(list)
    }

    LaunchedEffect(selectedAreaCode) {
        if (selectedAreaCode != null) {
            sigungus.clear()
            sigungus.add(AreaCode("", "전체"))
            val list = fetchAreaCodes(selectedAreaCode)
            sigungus.addAll(list)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().height(500.dp)) {
            Text(
                "지역 선택",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )
            HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))

            Row(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFF7F8FA))
                ) {
                    items(areas) { area ->
                        val isSelected = selectedAreaCode == area.code
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { selectedAreaCode = area.code; selectedAreaName = area.name }
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .padding(vertical = 16.dp, horizontal = 20.dp)
                        ) {
                            Text(area.name, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Medium, color = if(isSelected) MaterialTheme.colorScheme.primary else Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
                Box(modifier = Modifier.weight(1.8f).fillMaxHeight()) {
                    if (selectedAreaCode == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("좌측에서 시/도를\n선택해주세요.", color = Color.LightGray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(sigungus) { sigungu ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { onRegionSelected(selectedAreaCode!!, selectedAreaName, sigungu.code, sigungu.name) }
                                        .padding(vertical = 16.dp, horizontal = 24.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(sigungu.name, color = Color(0xFF333333), fontSize = 15.sp)
                                    Icon(Icons.Default.ArrowForward, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                }
                                HorizontalDivider(color = Color(0xFFFAFAFA))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomMapMarker(title: String, isSelected: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, if(isSelected) Color.White else MaterialTheme.colorScheme.primary)
        ) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 120.dp))
            }
        }
        Icon(Icons.Default.Place, contentDescription = null, tint = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp).offset(y = (-6).dp))
    }
}

@Composable
fun SearchVerticalCard(spot: TourSpot, onClick: () -> Unit, onDetailClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.size(90.dp), elevation = CardDefaults.cardElevation(2.dp)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(spot.imageUrl.ifEmpty { "https://via.placeholder.com/150" })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = Color(0xFFF0F0F0), shape = RoundedCornerShape(4.dp)) {
                    Text(text = if(spot.contentTypeId == "39") "맛집" else if(spot.contentTypeId == "32") "숙소" else "관광", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
                Spacer(modifier = Modifier.width(6.dp))
                if (spot.isFavorite) Icon(Icons.Default.Star, null, tint = Color(0xFFFFD700), modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = spot.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color(0xFF222222))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = spot.address, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onDetailClick) {
            Icon(Icons.Filled.ArrowForward, contentDescription = "상세보기", tint = Color.LightGray)
        }
    }
}