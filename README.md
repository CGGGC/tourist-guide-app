# ✈️ RecommendTrip (맞춤형 관광지 추천 앱)

## 🛠️ Tech Stack
* **Language:** Kotlin
* **UI Framework:** Jetpack Compose
* **Backend/Baas:** Firebase (Authentication, Firestore)
* **Open API:** 한국관광공사 Tour API 2.0, Google Maps API
* **Image Loader:** Coil

## ✨ Key Features

### 1. 맞춤형 큐레이션 (Home)
* **랜덤 축제 배너:** 현재 진행 중인 지역 축제 정보를 무작위로 추천합니다.
* **카테고리 퀵 메뉴:** 바다, 산, 캠핑, 카페, 테마파크 등 테마별로 여행지를 빠르게 탐색할 수 있습니다.
* **위치 기반 추천:** 사용자의 현재 위치(또는 기준 좌표)를 바탕으로 '내 주변 가볼 만한 곳'을 추천합니다.

### 2. 직관적인 지도 및 지역 검색 (Search & Map)
* **지역별 필터링:** 전국 단위부터 시/군/구 단위까지 상세하게 지역을 설정하여 관광지를 조회할 수 있습니다.
* **현 지도에서 검색:** Google Maps 화면을 이동한 뒤, 해당 반경 내에 있는 관광지나 맛집을 검색/재검색 할 수 있습니다.
* **Bottom Sheet UI:** 지도 화면과 검색 결과 리스트를 부드럽게 오가며 확인할 수 있도록 구현했습니다.

### 3. 여행자 톡 & 찜하기 (Detail & Community)
* **상세 정보 제공:** 관광지의 기본 소개글, 주소, 공식 홈페이지 링크 등을 제공합니다.
* **리뷰 시스템 (Firebase Firestore):** 여행지 방문 후 자신만의 팁이나 후기를 남길 수 있으며, 실시간으로 다른 사용자의 리뷰를 확인할 수 있습니다.
* **주변 추천:** 현재 보고 있는 관광지 근처의 다른 명소나 음식점/카페의 리스트들을 함께 추천해줍니다.
* **찜하기 기능:** 마음에 드는 장소를 저장하고 마이페이지에서 해당 관광지들을 모아볼 수 있습니다.

### 4. 사용자 인증 (My Page)
* **Firebase Auth 연동:** 이메일과 비밀번호를 통한 간편한 회원가입 및 로그인 기능을 지원합니다.
* **내 여행 기록:** 로그인한 사용자가 찜한 관광지 목록을 격자(Grid) 형태로 한 눈에 모아볼 수 있습니다.

## 📂 주요 소스 코드 설명
* `Network.kt`: 한국관광공사 Open API(Retrofit/HttpURLConnection) 통신 및 데이터 파싱 (상세 정보, 지역 코드, 축제 정보 등)
* `Search.kt` & `MapScreen.kt`: Google Maps Compose 연동 및 바텀 시트를 활용한 검색 결과 UI 처리
* `Detail.kt`: 선택한 관광지의 상세 정보 출력, 주변 장소 추천 로직, Firestore를 활용한 실시간 리뷰 CRUD 구현
* `Home.kt`: 메인 화면의 수평 스크롤(LazyRow) 큐레이션 리스트 및 페이저(HorizontalPager) 배너 구현
* `MyPage.kt`: Firebase Authentication 연동 로그인 화면 및 Firestore 기반 찜 목록 조회
* `Models.kt`: API 및 DB 통신에 사용되는 데이터 클래스 구조화

## 🗺️ ScreenShot
<img width="200" height="470" alt="image" src="https://github.com/user-attachments/assets/fb640fe6-3372-425d-b8ed-7d5c0ef249d5" />
<img width="200" height="470" alt="image" src="https://github.com/user-attachments/assets/55b6c950-d6c9-42a2-aeda-5f2d7169921c" />
<img width="200" height="470" alt="image" src="https://github.com/user-attachments/assets/aa41da3a-46aa-4a2b-8cc6-0dcb56f00054" />
<img width="200" height="470" alt="image" src="https://github.com/user-attachments/assets/3697fdce-01e5-4d8c-80e4-22175808e74b" />
<img width="200" height="470" alt="image" src="https://github.com/user-attachments/assets/799d3694-dde8-4a1b-9626-73ef0fd0e75f" />
