package com.nhom.datve.dao.impl;

import static com.nhom.datve.dao.impl.JdbcHelper.exists;
import static com.nhom.datve.dao.impl.JdbcHelper.query;
import static com.nhom.datve.dao.impl.JdbcHelper.queryOne;

import com.nhom.datve.dao.SuatChieuDAO;
import com.nhom.datve.model.SuatChieu;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** SuatChieuDAO bang JDBC (bang SuatChieu, kem ten phim/ten phong de hien thi). */
public class SuatChieuDAOImpl implements SuatChieuDAO {

    private static final String SELECT = """
            SELECT sc.MaSuat, sc.MaPhim, sc.MaPhong, sc.GioBatDau, sc.GioKetThuc, sc.GiaCoBan,
                   p.TenPhim, pc.TenPhong
            FROM SuatChieu sc
                 JOIN Phim p        ON p.MaPhim = sc.MaPhim
                 JOIN PhongChieu pc ON pc.MaPhong = sc.MaPhong""";

    @Override
    public List<SuatChieu> findAll() {
        return query(SELECT + " ORDER BY sc.GioBatDau", SuatChieuDAOImpl::doc);
    }

    @Override
    public Optional<SuatChieu> findById(Integer id) {
        return queryOne(SELECT + " WHERE sc.MaSuat = ?", SuatChieuDAOImpl::doc, id);
    }

    @Override
    public List<SuatChieu> findByPhimVaNgay(int maPhim, LocalDate ngay) {
        return query(SELECT + " WHERE sc.MaPhim = ? AND CAST(sc.GioBatDau AS DATE) = ? ORDER BY sc.GioBatDau",
                SuatChieuDAOImpl::doc, maPhim, ngay);
    }

    @Override
    public List<SuatChieu> findSapChieu(int maPhim, LocalDateTime tu) {
        return query(SELECT + " WHERE sc.MaPhim = ? AND sc.GioBatDau > ? ORDER BY sc.GioBatDau",
                SuatChieuDAOImpl::doc, maPhim, tu);
    }

    @Override
    public boolean biTrungLich(int maPhong, LocalDateTime batDau, LocalDateTime ketThuc, Integer boQuaMaSuat) {
        int boQua = boQuaMaSuat == null ? -1 : boQuaMaSuat;
        return exists("SELECT 1 FROM SuatChieu WHERE MaPhong = ? AND GioBatDau < ? AND GioKetThuc > ? AND MaSuat <> ?",
                maPhong, ketThuc, batDau, boQua);
    }

    @Override
    public Integer insert(SuatChieu s) {
        int ma = JdbcHelper.insert(
                "INSERT INTO SuatChieu (MaPhim, MaPhong, GioBatDau, GioKetThuc, GiaCoBan) VALUES (?, ?, ?, ?, ?)",
                s.getMaPhim(), s.getMaPhong(), s.getGioBatDau(), s.getGioKetThuc(), s.getGiaCoBan());
        s.setMaSuat(ma);
        return ma;
    }

    @Override
    public boolean update(SuatChieu s) {
        return JdbcHelper.update("""
                UPDATE SuatChieu SET MaPhim = ?, MaPhong = ?, GioBatDau = ?, GioKetThuc = ?, GiaCoBan = ?
                WHERE MaSuat = ?""",
                s.getMaPhim(), s.getMaPhong(), s.getGioBatDau(), s.getGioKetThuc(), s.getGiaCoBan(), s.getMaSuat()) > 0;
    }

    @Override
    public boolean delete(Integer id) {
        return JdbcHelper.update("DELETE FROM SuatChieu WHERE MaSuat = ?", id) > 0;
    }

    private static SuatChieu doc(ResultSet rs) throws SQLException {
        SuatChieu s = new SuatChieu(rs.getInt("MaSuat"), rs.getInt("MaPhim"), rs.getInt("MaPhong"),
                JdbcHelper.thoiGian(rs, "GioBatDau"), JdbcHelper.thoiGian(rs, "GioKetThuc"), rs.getBigDecimal("GiaCoBan"));
        s.setTenPhim(rs.getString("TenPhim"));
        s.setTenPhong(rs.getString("TenPhong"));
        return s;
    }
}
