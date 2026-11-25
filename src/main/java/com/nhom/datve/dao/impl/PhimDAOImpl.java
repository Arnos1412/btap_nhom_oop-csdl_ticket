package com.nhom.datve.dao.impl;

import static com.nhom.datve.dao.impl.JdbcHelper.query;

import com.nhom.datve.dao.PhimDAO;
import com.nhom.datve.model.Phim;
import com.nhom.datve.model.TheLoai;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** PhimDAO bang JDBC (bang Phim, Phim_TheLoai). Moi Phim tra ve deu kem danh sach the loai. */
public class PhimDAOImpl implements PhimDAO {

    private static final String SELECT = """
            SELECT MaPhim, TenPhim, ThoiLuong, DaoDien, NgayKhoiChieu, DoTuoi, TrangThai, Poster, MoTa
            FROM Phim p""";

    @Override
    public List<Phim> findAll() {
        return kemTheLoai(query(SELECT + " ORDER BY MaPhim", PhimDAOImpl::doc));
    }

    @Override
    public Optional<Phim> findById(Integer id) {
        return kemTheLoai(query(SELECT + " WHERE MaPhim = ?", PhimDAOImpl::doc, id)).stream().findFirst();
    }

    @Override
    public List<Phim> findDangChieu() {
        return kemTheLoai(query(SELECT + " WHERE TrangThai = ? ORDER BY NgayKhoiChieu DESC, MaPhim",
                PhimDAOImpl::doc, Phim.DANG_CHIEU));
    }

    @Override
    public List<Phim> search(String tuKhoa, Integer maTL) {
        StringBuilder sql = new StringBuilder(SELECT).append(" WHERE 1 = 1");
        List<Object> ts = new ArrayList<>();
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sql.append(" AND p.TenPhim LIKE ?");
            ts.add("%" + tuKhoa.trim() + "%");
        }
        if (maTL != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM Phim_TheLoai x WHERE x.MaPhim = p.MaPhim AND x.MaTL = ?)");
            ts.add(maTL);
        }
        sql.append(" ORDER BY MaPhim");
        return kemTheLoai(query(sql.toString(), PhimDAOImpl::doc, ts.toArray()));
    }

    @Override
    public Integer insert(Phim p) {
        int ma = JdbcHelper.insert("""
                INSERT INTO Phim (TenPhim, ThoiLuong, DaoDien, NgayKhoiChieu, DoTuoi, TrangThai, Poster, MoTa)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)""",
                p.getTenPhim(), p.getThoiLuong(), p.getDaoDien(), ngayKhoiChieu(p), p.getDoTuoi(),
                p.getTrangThai(), p.getPoster(), p.getMoTa());
        p.setMaPhim(ma);
        return ma;
    }

    @Override
    public boolean update(Phim p) {
        return JdbcHelper.update("""
                UPDATE Phim SET TenPhim = ?, ThoiLuong = ?, DaoDien = ?, NgayKhoiChieu = ?, DoTuoi = ?,
                       TrangThai = ?, Poster = ?, MoTa = ?
                WHERE MaPhim = ?""",
                p.getTenPhim(), p.getThoiLuong(), p.getDaoDien(), ngayKhoiChieu(p), p.getDoTuoi(),
                p.getTrangThai(), p.getPoster(), p.getMoTa(), p.getMaPhim()) > 0;
    }

    @Override
    public boolean delete(Integer id) {
        return JdbcHelper.update("DELETE FROM Phim WHERE MaPhim = ?", id) > 0;
    }

    /** Xoa het the loai cu roi them lai, trong 1 transaction. */
    @Override
    public void capNhatTheLoai(int maPhim, List<Integer> dsMaTL) {
        try (Connection conn = JdbcHelper.ketNoi()) {
            conn.setAutoCommit(false);
            try (PreparedStatement xoa = conn.prepareStatement("DELETE FROM Phim_TheLoai WHERE MaPhim = ?");
                 PreparedStatement them = conn.prepareStatement("INSERT INTO Phim_TheLoai (MaPhim, MaTL) VALUES (?, ?)")) {
                xoa.setInt(1, maPhim);
                xoa.executeUpdate();
                for (Integer maTL : dsMaTL.stream().distinct().toList()) {
                    them.setInt(1, maPhim);
                    them.setInt(2, maTL);
                    them.addBatch();
                }
                them.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw JdbcHelper.loi("cập nhật thể loại phim", e);
        }
    }

    /** Nap the loai cho ca danh sach bang 1 truy van. */
    private static List<Phim> kemTheLoai(List<Phim> ds) {
        if (ds.isEmpty()) {
            return ds;
        }
        Map<Integer, Phim> theoMa = new HashMap<>();
        ds.forEach(p -> theoMa.put(p.getMaPhim(), p));
        query("""
                SELECT pt.MaPhim, tl.MaTL, tl.TenTL FROM Phim_TheLoai pt JOIN TheLoai tl ON tl.MaTL = pt.MaTL
                ORDER BY tl.MaTL""", rs -> {
                    Phim p = theoMa.get(rs.getInt("MaPhim"));
                    if (p != null) {
                        p.getTheLoais().add(new TheLoai(rs.getInt("MaTL"), rs.getString("TenTL")));
                    }
                    return Boolean.TRUE;
                });
        return ds;
    }

    private static LocalDate ngayKhoiChieu(Phim p) {
        return p.getNgayKhoiChieu() == null ? LocalDate.now() : p.getNgayKhoiChieu();
    }

    private static Phim doc(ResultSet rs) throws SQLException {
        Phim p = new Phim(rs.getInt("MaPhim"), rs.getString("TenPhim"), rs.getInt("ThoiLuong"),
                rs.getString("DaoDien"), rs.getString("DoTuoi"));
        p.setNgayKhoiChieu(JdbcHelper.ngay(rs, "NgayKhoiChieu"));
        p.setTrangThai(rs.getString("TrangThai"));
        p.setPoster(rs.getString("Poster"));
        p.setMoTa(rs.getString("MoTa"));
        p.setTheLoais(new ArrayList<>());
        return p;
    }
}
