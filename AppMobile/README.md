# AI Screen Solver (Mobile Android)

Ứng dụng Android chạy nền dạng **Cửa sổ nổi (Floating HUD / Overlay Window)** sử dụng mô hình trí tuệ nhân tạo (Vision Multimodal qua 9router) để quan sát màn hình, đọc câu hỏi từ bất kỳ ứng dụng bên thứ 3 nào (Kahoot, Quizizz, Canvas, bài thi trắc nghiệm, game show, trình duyệt web) và **hiển thị đáp án chính xác ngay lập tức để người dùng tự bấm**.

---

## 🌟 Tính năng nổi bật

1. **Cửa sổ nổi (Floating Bubble & Compact Pill)**:
   - Chạy đè lên tất cả ứng dụng bên thứ 3 (`SYSTEM_ALERT_WINDOW`).
   - Bong bóng tròn nhỏ có thể kéo thả di chuyển tự do trên màn hình, tự động hít vào cạnh viền.
   - Khi có đáp án, bung ra thanh viên thuốc nhỏ gọn: `👉 [B] Thủ đô Hà Nội` trong 8 giây rồi tự thu gọn, không che khuất màn hình.
   - Nút mở rộng xem nhanh giải thích 1 câu ngắn gọn.

2. **2 Chế độ hoạt động linh hoạt**:
   - **Chế độ Chạm thủ công (Manual Tap)**: Chạm vào bong bóng khi câu hỏi xuất hiện -> Chụp và giải ngay.
   - **Chế độ Tự động liên tục (Auto-Loop)**: Sử dụng thuật toán nhận diện ảnh siêu nhanh `dHash` (Difference Hash 64-bit) để theo dõi màn hình. Khi bạn chuyển sang câu hỏi mới, app tự động nhận biết và gọi AI giải ngay mà bạn không cần chạm vào bong bóng. Có thể bật/tắt nhanh bằng cách **nhấn giữ bong bóng nổi**.

3. **Tích hợp 9router API (OpenAI-compatible Vision)**:
   - Tương thích 100% chuẩn OpenAI Chat Completions với Vision (`image_url` base64).
   - Tự do nhập API Key 9router và chọn bất kỳ model Vision nào:
     - `gemini-1.5-flash`: Tốc độ nhanh nhất (< 1.2s), chi phí cực thấp, giải ảnh xuất sắc (Khuyên dùng).
     - `gpt-4o-mini`: Rất thông minh và chuẩn xác.
     - `claude-3-5-sonnet`: Khả năng suy luận logic và bài toán phức tạp cao.
   - Hỗ trợ nút "Kiểm tra kết nối" ngay trong giao diện Cài đặt.

4. **An toàn & Bền bỉ**:
   - Chỉ hiển thị đáp án để người dùng tự tay bấm -> Không sử dụng quyền Accessibility tự click nên không lo bị hệ thống thi phát hiện gian lận hay macro bot.
   - Dịch vụ nền `ForegroundService` với `mediaProjection` chuẩn Android 14+, không bị hệ điều hành tắt khi mở ứng dụng nặng.

---

## 📁 Cấu trúc thư mục dự án

```
D:\VisualStudioCode\DuAnLinhTinh\AppMobile
├── app
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src
│       └── main
│           ├── AndroidManifest.xml
│           ├── java/com/screensolver/ai
│           │   ├── ScreenSolverApp.kt
│           │   ├── data
│           │   │   ├── local/PreferencesManager.kt       # Lưu trữ bảo mật API Key
│           │   │   ├── model/SolverModels.kt              # Models dữ liệu & OpenAI format
│           │   │   └── remote/AiSolverRepository.kt       # Gọi 9router Vision API
│           │   ├── service
│           │   │   ├── FloatingOverlayService.kt          # Dịch vụ cửa sổ nổi & điều phối
│           │   │   ├── ScreenCaptureManager.kt            # Chụp màn hình MediaProjection
│           │   │   └── ImageDifferenceDetector.kt         # Thuật toán dHash phát hiện câu mới
│           │   └── ui
│           │       ├── MainActivity.kt                    # Màn hình chính & xin quyền
│           │       ├── SettingsScreen.kt                  # Giao diện Jetpack Compose
│           │       └── theme/ (Color, Theme, Type)
│           └── res
│               ├── drawable/                              # Icon, background bo tròn
│               ├── layout/                                # layout_floating_bubble, layout_floating_pill
│               └── values/ (strings, colors, themes)
├── gradle/wrapper/gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Hướng dẫn mở và chạy ứng dụng

### Cách 1: Mở bằng Android Studio (Khuyên dùng)
1. Mở **Android Studio**.
2. Chọn **File -> Open...** và duyệt đến thư mục:
   `D:\VisualStudioCode\DuAnLinhTinh\AppMobile`
3. Đợi Android Studio đồng bộ Gradle (Sync Project with Gradle Files).
4. Kết nối điện thoại Android của bạn qua cáp USB (hoặc chạy máy ảo Android Emulator):
   - Đảm bảo đã bật **Gỡ lỗi USB (USB Debugging)** trong Tuỳ chọn nhà phát triển.
5. Nhấn nút **Run (Tam giác xanh)** hoặc tổ hợp phím `Shift + F10` để cài đặt lên máy.

### Cách 2: Biên dịch file APK bằng lệnh Terminal
Mở PowerShell hoặc Command Prompt tại thư mục `D:\VisualStudioCode\DuAnLinhTinh\AppMobile`:
```powershell
# Trên Windows
gradlew assembleDebug
```
File APK cài đặt sẽ được tạo tại:
`app\build\outputs\apk\debug\app-debug.apk`

---

## 📱 Hướng dẫn sử dụng trên điện thoại

1. **Mở ứng dụng lần đầu**:
   - Nhập **9router API Key** của bạn.
   - Chọn Model (mặc định khuyên dùng `gemini-1.5-flash`).
   - Nhấn **"Kiểm tra kết nối API"** để đảm bảo API Key hoạt động tốt.
2. **Cấp quyền hệ thống**:
   - Nhấn **"Cấp quyền"** cho mục *Hiển thị đè lên ứng dụng khác*.
3. **Bật trợ lý**:
   - Nhấn nút lớn **"BẬT TRỢ LÝ NỔI"**.
   - Hệ thống sẽ hiện hộp thoại hỏi cho phép ghi hình màn hình -> Chọn **"Bắt đầu ngay / Start now"**.
4. **Sử dụng trên app khác**:
   - Bong bóng tròn sẽ xuất hiện trên màn hình. Bạn có thể mở ứng dụng bài thi (Kahoot, Quizizz, đề thi trên web...).
   - **Chạm 1 lần vào bong bóng**: Trợ lý chụp màn hình, phân tích và hiển thị đáp án nổi `👉 [Đáp án]` ngay lập tức.
   - **Nhấn giữ bong bóng**: Bật/tắt chế độ tự động giải (hiện chữ `AUTO`).
