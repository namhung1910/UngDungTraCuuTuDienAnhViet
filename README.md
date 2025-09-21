<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
   ỨNG DỤNG TRA CỨU TỪ ĐIỂN ANH VIỆT SỬ DỤNG GIAO THỨC TCP
</h2>
<div align="center">
    <p align="center">
        <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="AIoTLab Logo" width="180"/>
        <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>

## 1. 📖 GIỚI THIỆU
Từ Điển Anh-Việt là ứng dụng từ điển trực tuyến được phát triển theo mô hình Client-Server, cho phép người dùng tra cứu từ vựng Anh-Việt và Việt-Anh một cách nhanh chóng và hiệu quả. Ứng dụng hỗ trợ tính năng gợi ý từ khóa thông minh, hiển thị đầy đủ thông tin bao gồm nghĩa, từ loại và ví dụ minh họa.

### ✨ Tính năng nổi bật:

- Tra cứu từ Anh sang Việt và Việt sang Anh

- Gợi ý từ khóa khi nhập

- Giao diện đồ họa thân thiện

- Log hoạt động chi tiết
- Quản lý từ trên giao diện

- Lưu trữ từ và lịch sử trong cơ sở dữ liệu

## 2. 💻 CÔNG NGHỆ SỬ DỤNG
<p align="center">
  <img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white"/>
  <img src="https://img.shields.io/badge/Java_Swing-6DB33F?style=for-the-badge&logo=java&logoColor=white"/>
  <img src="https://img.shields.io/badge/Socket_Programming-00599C?style=for-the-badge&logo=java&logoColor=white"/>
  <img src="https://img.shields.io/badge/Multithreading-FF6F00?style=for-the-badge&logo=java&logoColor=white"/>
  <img src="https://img.shields.io/badge/MongoDB-4EA94B?style=for-the-badge&logo=mongodb&logoColor=white"/>
</p>


## 3. 🚀 HƯỚNG DẪN CÀI ĐẶT
### 📋 Điều kiện tiên quyết
- JDK 8 trở lên

- Restore lại CSDL để chương trình hoạt động đúng cách

### 🔧 Các bước cài đặt

1. **Clone repository:**
    ```bash
    git clone <https://github.com/namhung1910/UngDungTraCuuTuDienAnhViet.git>

    cd TuDienAnhViet
2. **Chuẩn bị dữ liệu:**
- Cài đặt MongoDB nếu chưa có.
- Khởi động MongoDB và đảm bảo đang hoạt động tại `mongodb://localhost:27017/`.
- Khôi phục cơ sở dữ liệu từ bản sao lưu:

        mongorestore --db AttendanceDB "đường-dẫn-đến-thư-mục-TuDienAnhVietDbBackUp\TuDienAnhViet"
- Ví dụ:

        mongorestore --db TuDienAnhViet "C:\Users\LENOVO\Documents\BTL_LTM_Nguyen_NamHung\src\TuDienAnhVietDbBackUp\TuDienAnhViet"

📌 Lưu ý:
-	Tránh trùng lặp cơ sở dữ liệu: Trước khi thực hiện restore, hãy kiểm tra xem MongoDB đã có cơ sở dữ liệu tên AttendanceDB chưa. Nếu có, bạn có thể gặp lỗi hoặc dữ liệu cũ có thể bị ghi đè.
-	Đảm bảo MongoDB đang chạy: Nếu MongoDB chưa được khởi động, lệnh mongorestore sẽ không hoạt động.
3. **Biên dịch chương trình:**
    ```bash
    javac -d bin TuDienAnhViet/*.java
4. **Khởi động Server:**
    ```bash
    java -cp bin TuDienAnhViet.Server
5. **Khởi động Client:**
    ```bash
    java -cp bin TuDienAnhViet.Client

## 4. 📸 HÌNH ẢNH CHƯƠNG TRÌNH
### 🖼️ Giao diện Client
- Giao diện chính của Client:
    <p align="center">
    <img src="docs/ClientHome.png" alt="Client Home " width="800"/>
    </p>
- Giao diện gợi ý của Client:
    <p align="center">
    <img src="docs/ClientGoiY.png" alt="Goi Y " width="800"/>
    </p>
- Giao diện dịch từ tiếng Anh sang tiếng Việt:
    <p align="center">
    <img src="docs/ClientE2V.png" alt="Anh Viet " width="800"/>
    </p>
- Giao diện dịch từ tiếng Việt sang tiếng Anh:
    <p align="center">
    <img src="docs/ClientV2E.png" alt="Viet Anh " width="800"/>
    </p>
- Giao diện lịch sử dịch:
    <p align="center">
    <img src="docs/ClientLichSuDich.png" alt="Server Lich Su Dich" width="800"/>
    </p>
### 🖥️ Giao diện Server
- Giao diện quản lý từ:
    <p align="center">
    <img src="docs/ServerHome.png" alt="Server Home" width="800"/>
    </p>
- Giao diện quản lý log:
    <p align="center">
    <img src="docs/ServerLog.png" alt="Server Log" width="800"/>
    </p>

- Giao diện thêm, sửa, xóa từ:  
<p align="center">
  <img src="docs/ServerThemTu.png" alt="Server Them Tu" width="266"/>
  <img src="docs/ServerSuaTu.png" alt="Server Sua Tu" width="266"/>
  <img src="docs/ServerXoaTu.png" alt="Server Xoa Tu" width="266"/>
</p>

### 📞 Liên hệ: 
Nếu có thắc mắc hoặc góp ý, vui lòng liên hệ qua namhung1910@gmail.com

---
© 2025 Nguyễn Nam Hưng. All rights reserved.
