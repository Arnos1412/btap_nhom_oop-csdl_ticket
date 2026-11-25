package com.nhom.datve.dao.impl;

import static com.nhom.datve.dao.impl.JdbcHelper.query;
import static com.nhom.datve.dao.impl.JdbcHelper.queryOne;

import com.nhom.datve.dao.NhanVienDAO;
import com.nhom.datve.model.NhanVien;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** NhanVienDAO bang JDBC (bang NhanVien). */
public class NhanVienDAOImpl implements NhanVienDAO {

    private static final String SELECT = "SELECT MaNV, MaTK, HoTen, ChucVu, SoDienThoai FROM NhanVien";

    @Override
    public List<NhanVien> findAll() {
        return query(SELECT + " ORDER BY MaNV", NhanVienDAOImpl::doc);
    }

    @Override
    public Optional<NhanVien> findById(Integer id) {
        return queryOne(SELECT + " WHERE MaNV = ?", NhanVienDAOImpl::doc, id);
    }

    @Override
    public Optional<NhanVien> findByMaTK(int maTK) {
        return queryOne(SELECT + " WHERE MaTK = ?", NhanVienDAOImpl::doc, maTK);
    }

    @Override
    public Integer insert(NhanVien nv) {
        int ma = JdbcHelper.insert("INSERT INTO NhanVien (MaTK, HoTen, ChucVu, SoDienThoai) VALUES (?, ?, ?, ?)",
                nv.getMaTK(), nv.getHoTen(), nv.getChucVu() == null ? "Nhân viên" : nv.getChucVu(), nv.getSoDienThoai());
        nv.setMaNV(ma);
        return ma;
    }

    @Override
    public boolean update(NhanVien nv) {
        return JdbcHelper.update("UPDATE NhanVien SET HoTen = ?, ChucVu = ?, SoDienThoai = ? WHERE MaNV = ?",
                nv.getHoTen(), nv.getChucVu(), nv.getSoDienThoai(), nv.getMaNV()) > 0;
    }

    @Override
    public boolean delete(Integer id) {
        return JdbcHelper.update("DELETE FROM NhanVien WHERE MaNV = ?", id) > 0;
    }

    private static NhanVien doc(ResultSet rs) throws SQLException {
        NhanVien nv = new NhanVien(rs.getInt("MaNV"), rs.getInt("MaTK"), rs.getString("HoTen"), rs.getString("ChucVu"));
        nv.setSoDienThoai(rs.getString("SoDienThoai"));
        return nv;
    }
}
