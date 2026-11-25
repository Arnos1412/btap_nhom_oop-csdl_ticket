package com.nhom.datve.dao.impl;

import static com.nhom.datve.dao.impl.JdbcHelper.exists;
import static com.nhom.datve.dao.impl.JdbcHelper.query;
import static com.nhom.datve.dao.impl.JdbcHelper.queryOne;

import com.nhom.datve.dao.TaiKhoanDAO;
import com.nhom.datve.model.TaiKhoan;
import com.nhom.datve.model.VaiTro;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** TaiKhoanDAO bang JDBC (bang TaiKhoan). */
public class TaiKhoanDAOImpl implements TaiKhoanDAO {

    private static final String SELECT =
            "SELECT MaTK, TenDangNhap, MatKhau, VaiTro, TrangThai, NgayTao FROM TaiKhoan";

    @Override
    public List<TaiKhoan> findAll() {
        return query(SELECT + " ORDER BY MaTK", TaiKhoanDAOImpl::doc);
    }

    @Override
    public Optional<TaiKhoan> findById(Integer id) {
        return queryOne(SELECT + " WHERE MaTK = ?", TaiKhoanDAOImpl::doc, id);
    }

    @Override
    public Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap) {
        return queryOne(SELECT + " WHERE TenDangNhap = ?", TaiKhoanDAOImpl::doc, tenDangNhap);
    }

    @Override
    public boolean existsByTenDangNhap(String tenDangNhap) {
        return exists("SELECT 1 FROM TaiKhoan WHERE TenDangNhap = ?", tenDangNhap);
    }

    /**
     * Bang TaiKhoan bat buoc HoTen va MaVaiTro. Luc tao tai khoan chua biet ho ten nen tam dung
     * ten dang nhap; KhachHangDAOImpl.insert se cap nhat lai HoTen/Email/SoDienThoai.
     * MaVaiTro tra theo ten vai tro trong bang VaiTro.
     */
    @Override
    public Integer insert(TaiKhoan tk) {
        String vaiTro = tk.getVaiTro() == null ? VaiTro.KHACHHANG.name() : tk.getVaiTro().name();
        int ma = JdbcHelper.insert("""
                INSERT INTO TaiKhoan (TenDangNhap, MatKhau, HoTen, MaVaiTro, VaiTro, TrangThai)
                VALUES (?, ?, ?, (SELECT MaVaiTro FROM VaiTro WHERE TenVaiTro = ?), ?, ?)""",
                tk.getTenDangNhap(), tk.getMatKhau(), tk.getTenDangNhap(), vaiTro, vaiTro, tk.isTrangThai());
        tk.setMaTK(ma);
        return ma;
    }

    @Override
    public boolean update(TaiKhoan tk) {
        return JdbcHelper.update("UPDATE TaiKhoan SET TenDangNhap = ?, MatKhau = ?, VaiTro = ?, TrangThai = ? WHERE MaTK = ?",
                tk.getTenDangNhap(), tk.getMatKhau(), tk.getVaiTro().name(), tk.isTrangThai(), tk.getMaTK()) > 0;
    }

    @Override
    public boolean delete(Integer id) {
        return JdbcHelper.update("DELETE FROM TaiKhoan WHERE MaTK = ?", id) > 0;
    }

    private static TaiKhoan doc(ResultSet rs) throws SQLException {
        TaiKhoan tk = new TaiKhoan(rs.getInt("MaTK"), rs.getString("TenDangNhap"), rs.getString("MatKhau"),
                VaiTro.valueOf(rs.getString("VaiTro").trim()));
        tk.setTrangThai(rs.getBoolean("TrangThai"));
        tk.setNgayTao(JdbcHelper.thoiGian(rs, "NgayTao"));
        return tk;
    }
}
