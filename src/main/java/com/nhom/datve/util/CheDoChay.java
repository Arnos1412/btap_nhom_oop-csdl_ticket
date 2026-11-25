package com.nhom.datve.util;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cho biet app dang chay che do nao (MOCK hay JDBC) va vi sao, de hien thong bao cho nguoi dung.
 *
 * Goi kiemTra() 1 lan luc khoi dong (o luong main, KHONG goi tren luong giao dien vi thu ket noi
 * SQL Server co the mat vai giay). Sau do giao dien chi doc ket qua qua dongThongBao().
 */
public final class CheDoChay {
    private static volatile boolean daKiemTra;
    private static volatile boolean ketNoiDuoc;

    private CheDoChay() { }

    /** Thu ket noi (neu o che do jdbc) va in dong thong bao ra console. */
    public static synchronized void kiemTra() {
        if (!AppConfig.isMockMode()) {
            try {
                ketNoiDuoc = DBConnection.getInstance().testConnection();
            } catch (RuntimeException e) {
                System.err.println("Cau hinh ket noi loi: " + e.getMessage());
                ketNoiDuoc = false;
            }
        }
        daKiemTra = true;
        System.out.println("==================================================");
        System.out.println(" " + boDau(dongThongBao()));
        System.out.println(" Ly do: " + boDau(lyDo()));
        System.out.println("==================================================");
    }

    public static boolean isMock() {
        return AppConfig.isMockMode();
    }

    /** true neu dang o che do jdbc nhung khong ket noi duoc SQL Server. */
    public static boolean isLoiKetNoi() {
        return !isMock() && daKiemTra && !ketNoiDuoc;
    }

    /** Dong thong bao ngan, dung cho nhan tren giao dien. */
    public static String dongThongBao() {
        if (isMock()) {
            return "Chế độ MOCK – dữ liệu giả trong RAM, KHÔNG lưu vào SQL Server";
        }
        if (!daKiemTra) {
            return "Chế độ JDBC – SQL Server (" + tenDatabase() + ")";
        }
        return ketNoiDuoc
                ? "Chế độ JDBC – đã kết nối SQL Server (" + tenDatabase() + ")"
                : "Chế độ JDBC – KHÔNG kết nối được SQL Server (" + tenDatabase() + ")";
    }

    /** Giai thich vi sao app dang o che do hien tai. */
    public static String lyDo() {
        if (!AppConfig.coFileCauHinh()) {
            return AppConfig.isChayTuExe()
                    ? "Chưa có cấu hình " + AppConfig.fileCauHinhExe() + " nên app chạy dữ liệu giả."
                    : "Không tìm thấy file db.properties nên app tự chạy dữ liệu giả.";
        }
        String file = AppConfig.nguonCauHinh();
        if (isMock()) {
            return "Trong " + file + " đang đặt app.mode=" + AppConfig.get("app.mode")
                    + " (muốn dùng SQL Server hãy đặt app.mode=jdbc).";
        }
        if (isLoiKetNoi()) {
            return "app.mode=jdbc nhưng không kết nối được. Kiểm tra SQL Server đã bật, cổng 1433, "
                    + "user/mật khẩu trong " + file + ".";
        }
        return "app.mode=jdbc trong " + file + " – dữ liệu được đọc/ghi vào SQL Server.";
    }

    private static String tenDatabase() {
        String url = AppConfig.get("db.url");
        if (url != null) {
            Matcher m = Pattern.compile("databaseName=([^;]+)", Pattern.CASE_INSENSITIVE).matcher(url);
            if (m.find()) {
                return m.group(1);
            }
        }
        return "chưa cấu hình db.url";
    }

    /** Console Windows hay loi font tieng Viet nen bo dau khi in ra console. */
    private static String boDau(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .replace('–', '-');
    }
}
