# CodeForge - Frontend (Coding Challenge Platform)

Dự án frontend hoàn chỉnh, tái hiện chuẩn xác 100% theo các màn hình thiết kế:

---

## Danh sách các màn hình (Đã hoàn thiện)

1. **Dashboard / Tổng quan** (`#dashboard`):
   - **Thống kê người dùng**: Problems solved (48, +8 this week), Current streak (7 days), Acceptance rate (76.2%), Practice time (12.5h).
   - **Thử thách tiếp theo**: `Pair Sum` (#104, Arrays, Hash map, `In progress`), visual thuật toán `target = 9`, `[ 2 ] [ 7 ]`, `O(n) is the goal` cùng nút **`→ Continue solving`** dẫn thẳng vào IDE.
   - **Mục tiêu tuần**: `8 / 10 problems` (thanh tiến độ 80% và 7 ngày từ Thứ Hai đến Chủ Nhật).
   - **Lộ trình học tập** (*Arrays & hashing*, *Two pointers*, *Trees & graphs*) và lịch sử nộp bài gần nhất (*Recent activity*).

2. **Problem Library / Thư viện bài tập** (`#library`):
   - **Thống kê tiến độ**: 48 solved, 20% library (*Easy 32/80*, *Medium 14/120*, *Hard 2/40*).
   - **Nút hành động**: `+ Create problem` và `🎲 Pick a random problem`.
   - **Tabs & Bộ lọc**: Tab `All problems 240`, `Saved 18`, `My problems 3`.
   - **Tìm kiếm**: Tìm nhanh bằng ID hoặc tên bài (`⌘ K`), dropdown lọc theo Độ khó, Chủ đề, Trạng thái.
   - **Bảng bài tập**: Pair Sum, Contains Duplicate, Valid Anagram, Group Anagrams, Top K Frequent Elements... có nút bookmark lưu bài trực tiếp.

3. **Workspace / Problem Solving IDE** (`#ide`):
   - **Thanh subheader**: Nút `← Problem library`, tiêu đề `104. Pair Sum`, badge `In progress`, điều hướng bài toán `< 13 / 20 >`, **đồng hồ bấm giờ thời gian thực** `⏱ 00:12:48` đang chạy, và nút bật/tắt chế độ **`Focus mode`**.
   - **Pane trái**: Tabs *Description*, *Editorial*, *Solutions*, *Submissions*, ví dụ Example 1, Example 2, Constraints.
   - **Pane phải**: Giao diện dark forest-green chuẩn theme JetBrains Mono, line numbers, highlight dòng active (dòng 9), con trỏ `Ln 9, Col 43`.
   - **Nút `▷ Run code`**: Chạy kiểm thử 2 test case mẫu trong 32ms.
   - **Nút `↑ Submit solution`**: Chấm điểm 35 test cases kèm modal chúc mừng Accepted (*Beats 89.4% runtime*).
   - **Console kiểm thử**: Tab *Test cases* và *Test results* (Case 1, Case 2, Input, Your output, Expected).

4. **Make a Great Challenge / Trình tạo bài tập** (`#create-problem`):
   - **Thanh trạng thái**: `☁ All changes saved · Last saved just now`, Live version 1.2, Unpublished changes.
   - **Cột trái**: Form chi tiết bài toán, trình soạn thảo Markdown với toolbar (Đậm, Nghiêng, Link, Danh sách, Code) và chế độ xem **Write / Preview**.
   - **Cột phải**: Cấu hình ngôn ngữ (*Python 3*), nút chọn độ khó (*Easy / Medium / Hard*), chip gắn thẻ Topic tags tương tác (*Arrays x*, *Hash map x* - thêm/xóa tag linh hoạt), giới hạn thời gian (`1,000 ms`) và bộ nhớ (`256 MB`).

5. **Sign in / Đăng nhập** (`#login` hoặc mở file [`login.html`](file:///c:/Users/Aquatic/Downloads/mock-project-code/login.html)):
   - Giao diện form đăng nhập chuẩn Brand CodeForge: Logo, Tiêu đề, Đăng nhập nhanh qua **GitHub** hoặc **Google**.
   - Form nhập Email / Username, Password, Remember me 30 ngày, Quên mật khẩu.
   - Nút **Sign in** với hiệu ứng loading và thông báo đăng nhập thành công.

6. **Sign up / Đăng ký** (`#signup` hoặc mở file [`signup.html`](file:///c:/Users/Aquatic/Downloads/mock-project-code/signup.html)):
   - Form tạo tài khoản mới: Họ tên, Email, Mật khẩu có thanh đo độ mạnh (Password Strength Meter: Good/Strong), chấp thuận điều khoản.
   - Nút **Create account** chuyển thẳng vào Dashboard.

---

## Cấu trúc thư mục

```
mock-project-code/
├── index.html            # File HTML chính chứa toàn bộ 6 trang giao diện
├── login.html            # Phím tắt mở thẳng trang Đăng nhập
├── signup.html           # Phím tắt mở thẳng trang Đăng ký
├── css/
│   └── styles.css        # Styles tùy biến, dark mode editor, highlight dòng
├── js/
│   ├── app.js            # Router điều hướng 6 trang, xác thực login/logout, tìm kiếm
│   ├── data.js           # Dữ liệu bài tập, starter code, test cases, thông tin user
│   ├── ide.js            # Điều khiển bộ đếm giờ, chạy test runner, modal Accepted
│   └── editor.js         # Toolbar markdown, thêm/xóa tag topics, đổi độ khó
└── README.md
```

## Cách mở ứng dụng

Chỉ cần nhấp đúp vào file [`index.html`](file:///c:/Users/Aquatic/Downloads/mock-project-code/index.html) hoặc [`login.html`](file:///c:/Users/Aquatic/Downloads/mock-project-code/login.html) để trải nghiệm ngay trên trình duyệt.
