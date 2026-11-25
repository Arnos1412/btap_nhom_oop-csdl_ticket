package com.nhom.datve.dao.impl;

import static com.nhom.datve.dao.impl.JdbcHelper.query;
import static com.nhom.datve.dao.impl.JdbcHelper.queryOne;

import com.nhom.datve.dao.GheDAO;
import com.nhom.datve.model.Ghe;
import com.nhom.datve.model.LoaiGhe;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** GheDAO bang JDBC (bang Ghe + LoaiGhe). Tao dung lop con GheThuong/GheVIP/GheDoi qua Ghe.taoGhe. */
public class GheDAOImpl implements GheDAO {

    private static final String SELECT = """
            SELECT g.MaGhe, g.MaPhong, g.Hang, g.SoGhe, lg.MaLoaiGhe, lg.TenLoai, lg.HeSoGia
            FROM Ghe g JOIN LoaiGhe lg ON lg.MaLoaiGhe = g.MaLoaiGhe""";
    private static final String SQL_INSERT = "INSERT INTO Ghe (MaPhong, MaLoaiGhe, Hang, SoGhe) VALUES (?, ?, ?, ?)";

    @Override
    public List<Ghe> findAll() {
        return query(SELECT + " ORDER BY g.MaPhong, g.Hang, g.SoGhe", GheDAOImpl::doc);
    }

    @Override
    public Optional<Ghe> findById(Integer id) {
        return queryOne(SELECT + " WHERE g.MaGhe = ?", GheDAOImpl::doc, id);
    }

    @Override
    public List<Ghe> findByPhong(int maPhong) {
        return query(SELECT + " WHERE g.MaPhong = ? ORDER BY g.Hang, g.SoGhe", GheDAOImpl::doc, maPhong);
    }

    @Override
    public Integer insert(Ghe g) {
        int ma = JdbcHelper.insert(SQL_INSERT, g.getMaPhong(), g.getLoaiGhe().getMaLoaiGhe(), g.getHang(), g.getSoGhe());
        g.setMaGhe(ma);
        return ma;
    }

    /** Them nhieu ghe bang addBatch/executeBatch trong 1 transaction. */
    @Override
    public int insertBatch(List<Ghe> dsGhe) {
        if (dsGhe.isEmpty()) {
            return 0;
        }
        try (Connection conn = JdbcHelper.ketNoi()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(SQL_INSERT)) {
                for (Ghe g : dsGhe) {
                    JdbcHelper.ganThamSo(ps, g.getMaPhong(), g.getLoaiGhe().getMaLoaiGhe(), g.getHang(), g.getSoGhe());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
                return dsGhe.size();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw JdbcHelper.loi("thêm ghế", e);
        }
    }

    @Override
    public boolean update(Ghe g) {
        return JdbcHelper.update("UPDATE Ghe SET MaPhong = ?, MaLoaiGhe = ?, Hang = ?, SoGhe = ? WHERE MaGhe = ?",
                g.getMaPhong(), g.getLoaiGhe().getMaLoaiGhe(), g.getHang(), g.getSoGhe(), g.getMaGhe()) > 0;
    }

    @Override
    public boolean delete(Integer id) {
        return JdbcHelper.update("DELETE FROM Ghe WHERE MaGhe = ?", id) > 0;
    }

    private static Ghe doc(ResultSet rs) throws SQLException {
        LoaiGhe lg = new LoaiGhe(rs.getInt("MaLoaiGhe"), rs.getString("TenLoai"), rs.getBigDecimal("HeSoGia"));
        return Ghe.taoGhe(rs.getInt("MaGhe"), rs.getInt("MaPhong"), lg,
                rs.getString("Hang").trim().charAt(0), rs.getInt("SoGhe"));
    }
}
