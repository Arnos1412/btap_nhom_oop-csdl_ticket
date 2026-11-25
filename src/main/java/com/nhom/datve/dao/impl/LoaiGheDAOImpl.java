package com.nhom.datve.dao.impl;

import static com.nhom.datve.dao.impl.JdbcHelper.query;
import static com.nhom.datve.dao.impl.JdbcHelper.queryOne;

import com.nhom.datve.dao.LoaiGheDAO;
import com.nhom.datve.model.LoaiGhe;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** LoaiGheDAO bang JDBC (bang LoaiGhe). */
public class LoaiGheDAOImpl implements LoaiGheDAO {

    @Override
    public List<LoaiGhe> findAll() {
        return query("SELECT MaLoaiGhe, TenLoai, HeSoGia FROM LoaiGhe ORDER BY MaLoaiGhe", LoaiGheDAOImpl::doc);
    }

    @Override
    public Optional<LoaiGhe> findById(Integer id) {
        return queryOne("SELECT MaLoaiGhe, TenLoai, HeSoGia FROM LoaiGhe WHERE MaLoaiGhe = ?", LoaiGheDAOImpl::doc, id);
    }

    @Override
    public Integer insert(LoaiGhe lg) {
        int ma = JdbcHelper.insert("INSERT INTO LoaiGhe (TenLoai, HeSoGia) VALUES (?, ?)", lg.getTenLoai(), lg.getHeSoGia());
        lg.setMaLoaiGhe(ma);
        return ma;
    }

    @Override
    public boolean update(LoaiGhe lg) {
        return JdbcHelper.update("UPDATE LoaiGhe SET TenLoai = ?, HeSoGia = ? WHERE MaLoaiGhe = ?",
                lg.getTenLoai(), lg.getHeSoGia(), lg.getMaLoaiGhe()) > 0;
    }

    @Override
    public boolean delete(Integer id) {
        return JdbcHelper.update("DELETE FROM LoaiGhe WHERE MaLoaiGhe = ?", id) > 0;
    }

    private static LoaiGhe doc(ResultSet rs) throws SQLException {
        return new LoaiGhe(rs.getInt("MaLoaiGhe"), rs.getString("TenLoai"), rs.getBigDecimal("HeSoGia"));
    }
}
