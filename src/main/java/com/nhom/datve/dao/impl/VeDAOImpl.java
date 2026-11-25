package com.nhom.datve.dao.impl;

import static com.nhom.datve.dao.impl.JdbcHelper.query;

import com.nhom.datve.dao.VeDAO;
import com.nhom.datve.model.Ve;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** VeDAO bang JDBC (bang Ve). */
public class VeDAOImpl implements VeDAO {

    @Override
    public Set<Integer> findMaGheDaDat(int maSuat) {
        return new HashSet<>(query("SELECT MaGhe FROM Ve WHERE MaSuat = ? AND TrangThai <> 'DA_HUY'",
                rs -> rs.getInt("MaGhe"), maSuat));
    }

    @Override
    public List<Ve> findByDatVe(int maDatVe) {
        return query("""
                SELECT v.MaVe, v.MaDatVe, v.MaSuat, v.MaGhe, v.GiaVe, v.TrangThai,
                       g.Hang, g.SoGhe, p.TenPhim, pc.TenPhong, sc.GioBatDau
                FROM Ve v
                     JOIN Ghe g         ON g.MaGhe = v.MaGhe
                     JOIN SuatChieu sc  ON sc.MaSuat = v.MaSuat
                     JOIN Phim p        ON p.MaPhim = sc.MaPhim
                     JOIN PhongChieu pc ON pc.MaPhong = sc.MaPhong
                WHERE v.MaDatVe = ? ORDER BY g.Hang, g.SoGhe""", rs -> {
                    Ve v = new Ve(rs.getInt("MaVe"), rs.getInt("MaDatVe"), rs.getInt("MaSuat"),
                            rs.getInt("MaGhe"), rs.getBigDecimal("GiaVe"));
                    v.setTrangThai(rs.getString("TrangThai"));
                    v.setTenGhe(rs.getString("Hang").trim() + rs.getInt("SoGhe"));
                    v.setTenPhim(rs.getString("TenPhim"));
                    v.setTenPhong(rs.getString("TenPhong"));
                    v.setGioBatDau(JdbcHelper.thoiGian(rs, "GioBatDau"));
                    return v;
                }, maDatVe);
    }
}
