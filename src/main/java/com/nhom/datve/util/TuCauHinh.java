package com.nhom.datve.util;

import com.nhom.datve.dao.DAOFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.swing.JOptionPane;

/**
 * Tu cau hinh SQL Server khi chay bang file exe (chi dung khi AppConfig.isChayTuExe()).
 *
 * Lan dau mo exe (chua co %APPDATA%\DatVeXemPhim\db.properties) hoac khi khong ket noi duoc,
 * app hoi nguoi dung roi chay database/tu_cau_hinh.ps1 voi quyen Administrator (hien hop UAC).
 * Script bat TCP/IP cong 1433, tao database tu cac file .sql neu chua co, tao login datve_exe
 * va ghi file cau hinh voi app.mode=jdbc. Xong thi app doc lai cau hinh va chay che do JDBC.
 *
 * Goi o luong main (khong phai luong giao dien) vi script chay mat vai chuc giay.
 */
public final class TuCauHinh {
    private TuCauHinh() { }

    public static void chayNeuCan() {
        if (!AppConfig.isChayTuExe()) {
            return;
        }
        if (AppConfig.coFileCauHinh() && AppConfig.isMockMode()) {
            return; // nguoi dung tu dat app.mode=mock trong file cau hinh -> ton trong lua chon do
        }
        if (AppConfig.coFileCauHinh() && thuKetNoi()) {
            return; // da cau hinh va ket noi duoc
        }

        String loiMo = AppConfig.coFileCauHinh()
                ? "Không kết nối được SQL Server với cấu hình hiện tại."
                : "Đây là lần đầu chạy ứng dụng trên máy này.";
        int chon = JOptionPane.showConfirmDialog(null,
                loiMo + "\n\nỨng dụng sẽ tự cấu hình SQL Server để chạy chế độ JDBC:\n"
                        + "  • bật kết nối TCP/IP cổng 1433\n"
                        + "  • tạo database DatVeXemPhim (nếu chưa có)\n"
                        + "  • tạo tài khoản kết nối và lưu cấu hình\n\n"
                        + "Cần quyền Administrator (Windows sẽ hỏi xác nhận).\n"
                        + "Chọn No để chạy bằng dữ liệu giả.",
                "Cấu hình SQL Server", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (chon != JOptionPane.YES_OPTION) {
            return;
        }

        Path thuMucApp = Paths.get(System.getProperty("datve.appdir", "."));
        Path script = thuMucApp.resolve("database").resolve("tu_cau_hinh.ps1");
        if (!Files.isRegularFile(script)) {
            baoLoi("Không tìm thấy script cấu hình:\n" + script);
            return;
        }
        Path fileCauHinh = AppConfig.fileCauHinhExe();
        Path fileLog = fileCauHinh.resolveSibling("cau_hinh.log");

        int maThoat;
        try {
            maThoat = chayVoiQuyenAdmin(script, fileCauHinh, thuMucApp.resolve("database"));
        } catch (IOException e) {
            baoLoi("Không chạy được PowerShell: " + e.getMessage());
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        AppConfig.reload();
        DBConnection.reset();
        DAOFactory.reset();
        if (maThoat != 0 || !AppConfig.coFileCauHinh() || !thuKetNoi()) {
            baoLoi("Cấu hình SQL Server chưa thành công (mã " + maThoat + ").\n\n"
                    + cuoiLog(fileLog) + "\n\nNhật ký đầy đủ: " + fileLog
                    + "\nỨng dụng sẽ chạy bằng dữ liệu giả.");
        }
    }

    private static boolean thuKetNoi() {
        try {
            DBConnection.reset();
            return DBConnection.getInstance().testConnection();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Chay script bang PowerShell voi Start-Process -Verb RunAs (hien hop UAC), cho den khi xong. */
    private static int chayVoiQuyenAdmin(Path script, Path fileCauHinh, Path thuMucSql)
            throws IOException, InterruptedException {
        String lenh = "try { $p = Start-Process -FilePath powershell.exe -Verb RunAs -Wait -PassThru"
                + " -ArgumentList @('-NoProfile','-ExecutionPolicy','Bypass','-File',"
                + thamSo(script) + ",'-ConfigPath'," + thamSo(fileCauHinh)
                + ",'-SqlDir'," + thamSo(thuMucSql) + "); exit $p.ExitCode }"
                + " catch { Write-Error $_; exit 99 }";
        Process p = new ProcessBuilder("powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-Command", lenh)
                .redirectErrorStream(true)
                .start();
        p.getInputStream().transferTo(System.out);
        return p.waitFor();
    }

    /** Start-Process noi cac tham so bang dau cach, nen duong dan co dau cach phai boc trong "...". */
    private static String thamSo(Path duongDan) {
        return "'\"" + duongDan.toAbsolutePath().toString().replace("'", "''") + "\"'";
    }

    private static String cuoiLog(Path fileLog) {
        try {
            // Doc kieu "de dai" (khong nem loi khi gap byte khong phai UTF-8 tu sqlcmd)
            List<String> dong = new String(Files.readAllBytes(fileLog), StandardCharsets.UTF_8)
                    .lines().filter(s -> !s.isBlank()).toList();
            return String.join("\n", dong.subList(Math.max(0, dong.size() - 8), dong.size()));
        } catch (IOException | RuntimeException e) {
            return "(Không đọc được nhật ký – có thể bạn đã từ chối quyền Administrator.)";
        }
    }

    private static void baoLoi(String thongBao) {
        JOptionPane.showMessageDialog(null, thongBao, "Cấu hình SQL Server", JOptionPane.WARNING_MESSAGE);
    }
}
