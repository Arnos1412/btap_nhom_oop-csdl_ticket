package com.nhom.datve.dao.impl;

import static com.nhom.datve.dao.impl.JdbcHelper.query;
import static com.nhom.datve.dao.impl.JdbcHelper.queryOne;

import com.nhom.datve.dao.TheLoaiDAO;
import com.nhom.datve.model.TheLoai;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** TheLoaiDAO bang JDBC (bang TheLoai, Phim_TheLoai). */
public class TheLoaiDAOImpl implements TheLoaiDAO {

    @Override
    public List<TheLoai> findAll() {
        return query("SELECT MaTL, TenTL FROM TheLoai ORDER BY MaTL", TheLoaiDAOImpl::doc);
    }

    @Override
    public Optional<TheLoai> findById(Integer id) {
        return queryOne("SELECT MaTL, TenTL FROM TheLoai WHERE MaTL = ?", TheLoaiDAOImpl::doc, id);
    }

    @Override
    public List<TheLoai> findByPhim(int maPhim) {
        return query("""
                SELECT tl.MaTL, tl.TenTL FROM TheLoai tl JOIN Phim_TheLoai pt ON pt.MaTL = tl.MaTL
                WHERE pt.MaPhim = ? ORDER BY tl.MaTL""", TheLoaiDAOImpl::doc, maPhim);
    }

    @Override
    public Integer insert(TheLoai tl) {
        int ma = JdbcHelper.insert("INSERT INTO TheLoai (TenTL) VALUES (?)", tl.getTenTL());
        tl.setMaTL(ma);
        return ma;
    }

    @Override
    public boolean update(TheLoai tl) {
        return JdbcHelper.update("UPDATE TheLoai SET TenTL = ? WHERE MaTL = ?", tl.getTenTL(), tl.getMaTL()) > 0;
    }

    @Override
    public boolean delete(Integer id) {
        return JdbcHelper.update("DELETE FROM TheLoai WHERE MaTL = ?", id) > 0;
    }

    private static TheLoai doc(ResultSet rs) throws SQLException {
        return new TheLoai(rs.getInt("MaTL"), rs.getString("TenTL"));
    }
}
