package com.example.recommandtrip

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
// [수정] 설정 아이콘 import 제거 (사용 안함)
// import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun MyPageScreen(
    isLoggedIn: Boolean,
    userName: String,
    onLogin: (String) -> Unit,
    onLogout: () -> Unit,
    onSpotClick: (TourSpot) -> Unit
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    // 찜 목록 상태
    val myFavorites = remember { mutableStateListOf<TourSpot>() }

    // 찜 목록 실시간 동기화
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                db.collection("travelers").document(userId).collection("favorites")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) return@addSnapshotListener

                        if (snapshot != null) {
                            myFavorites.clear()
                            for (document in snapshot) {
                                try {
                                    val spot = document.toObject(TourSpot::class.java)
                                    myFavorites.add(spot)
                                } catch (e: Exception) { }
                            }
                        }
                    }
            }
        } else {
            myFavorites.clear()
        }
    }

    if (!isLoggedIn) {
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("나만의 여행 기록을 시작하세요", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("이메일") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("비밀번호 (6자리 이상)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        if (email.isNotEmpty() && password.isNotEmpty()) {
                            isLoading = true
                            auth.signInWithEmailAndPassword(email, password)
                                .addOnCompleteListener { task ->
                                    isLoading = false
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser
                                        onLogin(user?.email ?: "사용자")
                                        Toast.makeText(context, "로그인 성공", Toast.LENGTH_SHORT).show()
                                    } else {
                                        auth.createUserWithEmailAndPassword(email, password)
                                            .addOnCompleteListener { createTask ->
                                                if (createTask.isSuccessful) {
                                                    val user = auth.currentUser
                                                    onLogin(user?.email ?: "사용자")
                                                    Toast.makeText(context, "회원가입 및 로그인 성공", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "로그인/가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    }
                                }
                        } else {
                            Toast.makeText(context, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("로그인 / 회원가입", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F8FA))) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                // 배경 그라데이션
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            shadowElevation = 8.dp,
                            color = Color.White,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AccountCircle,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "반가워요, 여행자님! 👋",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = userName.substringBefore("@"),
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(
                            onClick = {
                                auth.signOut()
                                onLogout()
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text("로그아웃", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 컨텐츠 영역
            Surface(
                modifier = Modifier.fillMaxSize().offset(y = (-20).dp),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color(0xFFF7F8FA) // 배경색 일치
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "나의 찜 목록",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (myFavorites.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("텅 비어있어요", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("마음에 드는 여행지를 저장해보세요!", color = Color.LightGray)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(myFavorites) { spot ->
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(180.dp).clickable { onSpotClick(spot) },
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Box {
                                        AsyncImage(
                                            model = spot.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)), startY = 200f)))
                                        Text(
                                            spot.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                                            fontSize = 14.sp,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}