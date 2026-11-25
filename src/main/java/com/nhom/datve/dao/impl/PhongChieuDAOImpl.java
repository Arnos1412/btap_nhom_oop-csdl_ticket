package com.nhom.datve.dao.impl;

import static com.nhom.datve.dao.impl.JdbcHelper.query;
import static com.nhom.datve.dao.impl.JdbcHelper.queryOne;

import com.nhom.datve.dao.PhongChieuDAO;
import com.nhom.datve.model.PhongChieu;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** PhongChieuDAO bang JDBC (bang PhongChieu). */
public class PhongChieuDAOImpl implements PhongChieuDAO {

    private static final String SELECT = "SELECT MaPhong, TenPhong, SoHang, SoCot, HoatDong FROM PhongChieu";

    @Override
    public List<PhongChieu> findAll() {
        return query(SELECT + " ORDER BY MaPhong", PhongChieuDAOImpl::doc);
    }

    @Override
    public Optional<PhongChieu> findById(Integer id) {
        return queryOne(SELECT + " WHERE MaPhong = ?", PhongChieuDAOImpl::doc, id);
    }

    @Override
    public List<PhongChieu> findDangHoatDong() {
        return query(SELECT + " WHERE HoatDong = 1 ORDER BY MaPhong", PhongChieuDAOImpl::doc);
    }

    @Override
    public Integer insert(PhongChieu pc) {
        int ma = JdbcHelper.insert("INSERT INTO PhongChieu (TenPhong, SoHang, SoCot, HoatDong) VALUES (?, ?, ?, ?)",
                pc.getTenPhong(), pc.getSoHang(), pc.getSoCot(), pc.isHoatDong());
        pc.setMaPhong(ma);
        return ma;
    }

    @Override
    public boolean update(PhongChieu pc) {
        return JdbcHelper.update("UPDATE PhongChieu SET TenPhong = ?, SoHang = ?, SoCot = ?, HoatDong = ? WHERE MaPhong = ?",
                pc.getTenPhong(), pc.getSoHang(), pc.getSoCot(), pc.isHoatDong(), pc.getMaPhong()) > 0;
    }

    @Override
    public boolean delete(Integer id) {
        return JdbcHelper.update("DELETE FROM PhongChieu WHERE MaPhong = ?", id) > 0;
    }

    private static PhongChieu doc(ResultSet rs) throws SQLException {
        PhongChieu pc = new PhongChieu(rs.getInt("MaPhong"), rs.getString("TenPhong"),
                rs.getInt("SoHang"), rs.getInt("SoCot"));
        pc.setHoatDong(rs.getBoolean("HoatDong"));
        return pc;
    }
}
