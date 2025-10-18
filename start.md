안드로이드 자기통제 앱 개발 프로젝트
프로젝트 목표
사용자가 스스로를 더 잘 통제할 수 있도록 돕는 안드로이드 앱 개발
핵심 기능 요구사항
1. 기본 동작

사용자가 핸드폰을 사용하는 동안 10분마다 강제 팝업 알림 표시
팝업은 다른 모든 앱 위에 표시되어야 함 (System Alert Window)
사용자는 팝업을 무시할 수 없음 (전체 화면 또는 모달 방식)

2. 팝업 질문 내용
사용자는 다음 질문들에 답변해야 함:

지금 무엇을 하고 있는가?
그 이유는?
앞으로 무엇을 할 것인가?
그 이유는?

3. 강제 응답 메커니즘

답변 제출 버튼은 최소 10초 후에 활성화
10초가 지나기 전까지는 제출 불가
답변을 완료해야만 팝업이 닫히고 원래 사용하던 앱으로 돌아갈 수 있음
영화, 유튜브, 책, SNS 등 어떤 앱을 사용하든 강제로 중단되고 답변해야 함

기술 스택 및 구현 방법
개발 환경

IDE: Android Studio
언어: Kotlin
UI: Jetpack Compose
타겟: Android 10 (API 29) 이상

필요한 핵심 컴포넌트
1. Accessibility Service

사용자가 현재 어떤 앱을 사용 중인지 실시간 감지
앱 전환 이벤트 포착
화면 켜짐/꺼짐 상태 확인

2. System Alert Window (오버레이)

다른 앱 위에 전체 화면 팝업 표시
SYSTEM_ALERT_WINDOW 권한 필요
사용자가 무시할 수 없는 모달 형태

3. Foreground Service

앱이 백그라운드에서도 계속 실행
10분 타이머 관리
배터리 최적화 고려

4. 데이터 저장

사용자 답변 기록 저장
Room DB 또는 SharedPreferences 사용

필요한 권한 (AndroidManifest.xml)
xml<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE"/>
```

## 구현 로직 흐름
```
1. 앱 시작 → Foreground Service 시작
2. Service가 10분 타이머 작동
3. 10분마다:
   - Accessibility Service로 현재 앱 사용 중인지 확인
   - 화면이 켜져있고 앱 사용 중이면
   - System Alert Window로 전체화면 팝업 표시
4. 팝업 화면:
   - 4개 질문 표시 (텍스트 입력 필드)
   - 10초 카운트다운 타이머
   - 타이머 종료 전까지 제출 버튼 비활성화
   - 답변 완료 후에만 팝업 닫힘
5. 답변 데이터를 로컬 DB에 저장
6. 다음 10분 타이머 시작
```

## 프로젝트 구조 (권장)
```
app/
├── MainActivity.kt 
│   └── 메인 화면 (권한 설정 가이드, 앱 시작/중지)
│
├── services/
│   ├── MonitoringService.kt 
│   │   └── Foreground Service (10분 타이머 관리)
│   └── AppUsageAccessibilityService.kt 
│       └── Accessibility Service (앱 사용 감지)
│
├── ui/
│   ├── PopupActivity.kt 
│   │   └── 전체화면 팝업 Activity
│   └── components/
│       └── QuestionForm.kt (Compose UI)
│
├── data/
│   ├── Response.kt (데이터 모델)
│   └── ResponseDatabase.kt (Room DB)
│
└── utils/
    └── PermissionHelper.kt
개발 단계 (우선순위)
Phase 1: 기본 설정 및 권한

Android Studio에서 새 프로젝트 생성 (Kotlin + Compose)
필요한 권한 AndroidManifest.xml에 추가
권한 요청 UI 구현 (MainActivity)

Phase 2: Foreground Service

MonitoringService 생성
10분 타이머 구현
Service 시작/중지 로직

Phase 3: 오버레이 팝업

PopupActivity 생성 (전체 화면 또는 모달)
4개 질문 UI 구현 (Jetpack Compose)
10초 카운트다운 타이머
제출 버튼 활성화/비활성화 로직

Phase 4: Accessibility Service

AppUsageAccessibilityService 구현
현재 앱 사용 감지
화면 상태 체크
MonitoringService와 연동

Phase 5: 데이터 저장

Room Database 설정
답변 저장 로직
(선택) 답변 이력 조회 화면

Phase 6: 테스트 및 최적화

배터리 최적화
엣지 케이스 처리
사용자 경험 개선

주의사항

사용자 권한 필수

Accessibility Service: 설정 > 접근성에서 수동 활성화 필요
오버레이 권한: 설정 > 다른 앱 위에 표시에서 허용 필요
앱에서 해당 설정 화면으로 바로 이동하는 기능 구현 권장


배터리 최적화

Foreground Service가 계속 실행되므로 효율적인 코드 필요
불필요한 작업 최소화


Android 버전 호환성

Android 10 이상 타겟 권장
각 버전별 권한 처리 차이 고려



시작 요청
위 요구사항에 맞춰서 다음 작업을 수행해주세요:

프로젝트 기본 구조 생성
필요한 파일들의 뼈대 코드 작성
각 Phase별로 구현 가능한 순서대로 진행
권한 처리 및 사용자 가이드 포함

목표: 최대한 빠르게 작동하는 프로토타입 완성