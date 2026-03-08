package com.example.recommandtrip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun MapScreen(
    onBack: () -> Unit,
    onSpotClick: (TourSpot) -> Unit
) {
    // 초기 위치 -> 서울 시청으로
    val initialPos = LatLng(37.5665, 126.9780)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPos, 14f)
    }

    val spots = remember { mutableStateListOf<TourSpot>() }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    // "현 지도에서 검색"
    fun searchInCurrentMap() {
        isLoading = true
        val target = cameraPositionState.position.target
        scope.launch {
            val newSpots = fetchLocationBasedSpots(target.longitude, target.latitude, 3000) // 반경 3km
            spots.clear()
            spots.addAll(newSpots)
            isLoading = false
        }
    }

    // 처음 진입 시 한 번 자동 검색
    LaunchedEffect(Unit) {
        searchInCurrentMap()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            // 마커 표시
            spots.forEach { spot ->
                if (spot.mapY != 0.0 && spot.mapX != 0.0) {
                    Marker(
                        state = MarkerState(position = LatLng(spot.mapY, spot.mapX)),
                        title = spot.title,
                        snippet = spot.address,
                        onClick = {
                            onSpotClick(spot)
                            false
                        }
                    )
                }
            }
        }

        // 상단 뒤로가기 버튼
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .background(Color.White, RoundedCornerShape(8.dp))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
        }

        // "현 지도에서 검색" 버튼
        Button(
            onClick = { searchInCurrentMap() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            elevation = ButtonDefaults.buttonElevation(4.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("검색 중...")
            } else {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("현 지도에서 검색")
            }
        }

        // 하단 안내
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            color = Color.White.copy(alpha = 0.9f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "지도를 움직여 원하는 지역을 찾고\n상단 버튼을 눌러 주변 관광지를 확인하세요!",
                modifier = Modifier.padding(16.dp),
                color = Color.Gray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}