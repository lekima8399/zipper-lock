# Hướng dẫn thêm Style mới cho Zipper Animation

## Cách thức hoạt động

App sử dụng các file JSON có sẵn trong `assets/row_json/` để thay đổi style khóa kéo. Mỗi file JSON đã chứa sẵn animation với hình ảnh khóa kéo khác nhau.

## Cấu trúc thư mục

```
app/src/main/assets/
├── row.json              (animation mặc định)
├── wallpaper.json
├── zipper.json
├── row/                  (Thư mục chứa THUMBNAIL để hiển thị preview)
│   ├── thumbnail_big_row_1.png
│   ├── thumbnail_big_row_2.png
│   ├── ...
│   └── thumbnail_big_row_20.png
└── row_json/             (Thư mục chứa FILE JSON với animation thực)
    ├── video_row_1.json
    ├── video_row_2.json
    ├── ...
    └── video_row_21.json
```

## Cách thêm Style mới

### Bước 1: Tạo file JSON animation

Có 3 cách để tạo file JSON mới:

#### Cách 1: Sử dụng LottieFiles Editor (Khuyến nghị)
1. Truy cập https://lottiefiles.com/
2. Tải file `row.json` lên editor
3. Thay thế hình ảnh khóa kéo trong animation bằng hình ảnh mới
4. Export file JSON mới
5. Đặt tên file theo format: `video_row_X.json` (X là số thứ tự tiếp theo)

#### Cách 2: Chỉnh sửa trực tiếp file JSON
1. Copy một file JSON có sẵn (ví dụ: `video_row_1.json`)
2. Mở bằng text editor
3. Tìm phần `"assets"` → tìm asset có `"id": "image_0"`
4. Thay thế chuỗi base64 trong `"p": "data:image/png;base64,..."` bằng ảnh mới
5. Lưu với tên mới: `video_row_X.json`

#### Cách 3: Tạo animation mới từ đầu
1. Sử dụng Adobe After Effects với plugin Bodymovin
2. Tạo animation mới với hình ảnh khóa kéo của bạn
3. Export sang Lottie JSON
4. Đặt tên: `video_row_X.json`

### Bước 2: Tạo thumbnail

1. Tạo ảnh preview (PNG) kích thước khuyến nghị: 200x200 pixels
2. Đặt tên theo format: `thumbnail_big_row_X.png` (X phải khớp với số trong JSON)
3. Đặt vào thư mục `app/src/main/assets/row/`

### Bước 3: Đặt file vào đúng thư mục

```
app/src/main/assets/
├── row/
│   └── thumbnail_big_row_22.png    (Thumbnail mới)
└── row_json/
    └── video_row_22.json            (JSON animation mới)
```

### Bước 4: Sử dụng trong app

1. Build và chạy app
2. Mở app → nhấn "Settings"
3. Danh sách style sẽ tự động hiển thị tất cả file trong `row_json/`
4. Chọn style mới → nhấn "Áp dụng"
5. Quay lại và nhấn "Start Zipper Overlay"

## Quy tắc đặt tên

**QUAN TRỌNG**: Tên file phải khớp với nhau!

- JSON file: `video_row_X.json`
- Thumbnail: `thumbnail_big_row_X.png`
- X phải là cùng một số

Ví dụ:
- `video_row_22.json` ↔ `thumbnail_big_row_22.png` ✅
- `video_row_22.json` ↔ `thumbnail_big_row_23.png` ❌

## Kỹ thuật bên trong

Class `LottieImageReplacer` thực hiện:
1. Quét thư mục `row_json/` để lấy danh sách style
2. Khi user chọn style, copy file JSON từ assets vào internal storage
3. Load file JSON từ internal storage khi start overlay
4. Thumbnail được load từ `row/` để hiển thị preview

## Lưu ý

- File JSON không nên quá lớn (< 1MB) để tránh lag
- Kích thước hình ảnh khóa kéo trong JSON nên giữ tương tự với gốc (55x25px)
- Thumbnail chỉ để hiển thị, không ảnh hưởng đến animation
- App tự động sắp xếp danh sách style theo tên file

## Troubleshooting

**Không thấy style mới trong danh sách?**
- Kiểm tra file JSON có đúng trong `assets/row_json/` không
- Đảm bảo tên file đúng format: `video_row_X.json`
- Clean và rebuild project

**Không thấy thumbnail?**
- Kiểm tra file PNG có đúng trong `assets/row/` không
- Đảm bảo tên file khớp: `thumbnail_big_row_X.png`
- Số X phải giống với số trong JSON file

**App bị crash khi chọn style?**
- Kiểm tra file JSON có đúng format Lottie không
- Mở file JSON bằng text editor xem có lỗi syntax không
- Kiểm tra Logcat để xem lỗi chi tiết
