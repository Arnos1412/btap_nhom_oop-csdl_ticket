package com.nhom.datve.dao.impl;

import static com.nhom.datve.dao.impl.JdbcHelper.query;

import com.nhom.datve.dao.ThongKeDAO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/** ThongKeDAO bang JDBC. Tong doanh thu goi ham dbo.fn_DoanhThu trong 03_procedure_trigger.sql. */
public class ThongKeDAOImpl implements ThongKeDAO {

    @Override
    public BigDecimal tongDoanhThu(LocalDate tu, LocalDate den) {
        return query("SELECT dbo.fn_DoanhThu(?, ?) AS Tong", rs -> rs.getBigDecimal("Tong"), tu, den)
                .stream().findFirst().orElse(BigDecimal.ZERO);
    }

    @Override
    public Map<String, BigDecimal> doanhThuTheoPhim(LocalDate tu, LocalDate den) {
        Map<String, BigDecimal> kq = new LinkedHashMap<>();
        query("""
                SELECT p.TenPhim, SUM(v.GiaVe) AS DoanhThu
                FROM Ve v
                     JOIN DatVe dv     ON dv.MaDatVe = v.MaDatVe
                     JOIN SuatChieu sc ON sc.MaSuat = v.MaSuat
                     JOIN Phim p       ON p.MaPhim = sc.MaPhim
                WHERE v.TrangThai = 'HIEU_LUC' AND CAST(dv.NgayDat AS DATE) BETWEEN ? AND ?
                GROUP BY p.TenPhim
                ORDER BY DoanhThu DESC""", rs -> kq.put(rs.getString("TenPhim"), rs.getBigDecimal("DoanhThu")), tu, den);
        return kq;
    }

    @Override
    public Map<LocalDate, BigDecimal> doanhThuTheoNgay(LocalDate tu, LocalDate den) {
        Map<LocalDate, BigDecimal> kq = new TreeMap<>();
        query("""
                SELECT CAST(NgayThanhToan AS DATE) AS Ngay, SUM(SoTien) AS DoanhThu
                FROM ThanhToan
                WHERE TrangThai = 'THANH_CONG' AND CAST(NgayThanhToan AS DATE) BETWEEN ? AND ?
                GROUP BY CAST(NgayThanhToan AS DATE)""",
                rs -> kq.put(JdbcHelper.ngay(rs, "Ngay"), rs.getBigDecimal("DoanhThu")), tu, den);
        return kq;
    }
}
