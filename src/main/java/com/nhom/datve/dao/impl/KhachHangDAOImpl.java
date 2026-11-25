package com.nhom.datve.dao.impl;

import static com.nhom.datve.dao.impl.JdbcHelper.exists;
import static com.nhom.datve.dao.impl.JdbcHelper.query;
import static com.nhom.datve.dao.impl.JdbcHelper.queryOne;

import com.nhom.datve.dao.KhachHangDAO;
import com.nhom.datve.model.KhachHang;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** KhachHangDAO bang JDBC (bang KhachHang). */
public class KhachHangDAOImpl implements KhachHangDAO {

    private static final String SELECT =
            "SELECT MaKH, MaTK, HoTen, SoDienThoai, Email, NgaySinh FROM KhachHang";

    @Override
    public List<KhachHang> findAll() {
        return query(SELECT + " ORDER BY MaKH", KhachHangDAOImpl::doc);
    }

    @Override
    public Optional<KhachHang> findById(Integer id) {
        return queryOne(SELECT + " WHERE MaKH = ?", KhachHangDAOImpl::doc, id);
    }

    @Override
    public Optional<KhachHang> findByMaTK(int maTK) {
        return queryOne(SELECT + " WHERE MaTK = ?", KhachHangDAOImpl::doc, maTK);
    }

    @Override
    public boolean existsBySoDienThoai(String soDienThoai) {
        return exists("SELECT 1 FROM KhachHang WHERE SoDienThoai = ?", soDienThoai);
    }

    @Override
    public boolean existsByEmail(String email) {
        return exists("SELECT 1 FROM KhachHang WHERE LOWER(Email) = LOWER(?)", email);
    }

    /** Them khach hang va dong bo HoTen/Email/SoDienThoai sang TaiKhoan trong cung 1 transaction. */
    @Override
    public Integer insert(KhachHang kh) {
        try (Connection conn = JdbcHelper.ketNoi()) {
            conn.setAutoCommit(false);
            try {
                int ma = JdbcHelper.insert(conn,
                        "INSERT INTO KhachHang (MaTK, HoTen, SoDienThoai, Email, NgaySinh) VALUES (?, ?, ?, ?, ?)",
                        kh.getMaTK() == 0 ? null : kh.getMaTK(), kh.getHoTen(), kh.getSoDienThoai(),
                        kh.getEmail(), kh.getNgaySinh());
                if (kh.getMaTK() != 0) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE TaiKhoan SET HoTen = ?, Email = ?, SoDienThoai = ? WHERE MaTK = ?")) {
                        JdbcHelper.ganThamSo(ps, kh.getHoTen(), kh.getEmail(), kh.getSoDienThoai(), kh.getMaTK());
                        ps.executeUpdate();
                    }
                }
                conn.commit();
                kh.setMaKH(ma);
                return ma;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw JdbcHelper.loi("thêm khách hàng", e);
        }
    }

    @Override
    public boolean update(KhachHang kh) {
        return JdbcHelper.update("UPDATE KhachHang SET HoTen = ?, SoDienThoai = ?, Email = ?, NgaySinh = ? WHERE MaKH = ?",
                kh.getHoTen(), kh.getSoDienThoai(), kh.getEmail(), kh.getNgaySinh(), kh.getMaKH()) > 0;
    }

    @Override
    public boolean delete(Integer id) {
        return JdbcHelper.update("DELETE FROM KhachHang WHERE MaKH = ?", id) > 0;
    }

    private static KhachHang doc(ResultSet rs) throws SQLException {
        KhachHang kh = new KhachHang(rs.getInt("MaKH"), rs.getInt("MaTK"), rs.getString("HoTen"),
                rs.getString("SoDienThoai"), rs.getString("Email"));
        kh.setNgaySinh(JdbcHelper.ngay(rs, "NgaySinh"));
        return kh;
    }
}
