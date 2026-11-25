package com.nhom.datve.dao.impl;

import com.nhom.datve.exception.DataAccessException;
import com.nhom.datve.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ham dung chung cho cac lop DAO JDBC: mo ket noi bang try-with-resources,
 * gan tham so ? cho PreparedStatement va doi SQLException thanh DataAccessException.
 */
final class JdbcHelper {

    /** Doc 1 dong ResultSet thanh 1 doi tuong. */
    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Loi vi pham khoa ngoai (vd xoa phim dang co suat chieu). */
    static final int LOI_KHOA_NGOAI = 547;

    private JdbcHelper() { }

    static Connection ketNoi() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    static <T> List<T> query(String sql, RowMapper<T> mapper, Object... thamSo) {
        try (Connection conn = ketNoi();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ganThamSo(ps, thamSo);
            try (ResultSet rs = ps.executeQuery()) {
                List<T> ds = new ArrayList<>();
                while (rs.next()) {
                    ds.add(mapper.map(rs));
                }
                return ds;
            }
        } catch (SQLException e) {
            throw loi("đọc dữ liệu", e);
        }
    }

    static <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... thamSo) {
        return query(sql, mapper, thamSo).stream().findFirst();
    }

    static boolean exists(String sql, Object... thamSo) {
        return !query(sql, rs -> Boolean.TRUE, thamSo).isEmpty();
    }

    /** INSERT/UPDATE/DELETE, tra ve so dong bi anh huong. */
    static int update(String sql, Object... thamSo) {
        try (Connection conn = ketNoi();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ganThamSo(ps, thamSo);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw loi("ghi dữ liệu", e);
        }
    }

    /** INSERT vao bang co cot IDENTITY, tra ve khoa vua sinh. */
    static int insert(String sql, Object... thamSo) {
        try (Connection conn = ketNoi()) {
            return insert(conn, sql, thamSo);
        } catch (SQLException e) {
            throw loi("thêm dữ liệu", e);
        }
    }

    /** Ban dung chung Connection, de goi trong 1 transaction. */
    static int insert(Connection conn, String sql, Object... thamSo) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ganThamSo(ps, thamSo);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Không lấy được khóa vừa sinh.");
                }
                return rs.getInt(1);
            }
        }
    }

    static void ganThamSo(PreparedStatement ps, Object... thamSo) throws SQLException {
        for (int i = 0; i < thamSo.length; i++) {
            Object v = thamSo[i];
            if (v instanceof LocalDateTime t) {
                ps.setTimestamp(i + 1, Timestamp.valueOf(t));
            } else if (v instanceof LocalDate d) {
                ps.setDate(i + 1, java.sql.Date.valueOf(d));
            } else if (v instanceof Character c) {
                ps.setString(i + 1, String.valueOf(c));
            } else {
                ps.setObject(i + 1, v);
            }
        }
    }

    static LocalDateTime thoiGian(ResultSet rs, String cot) throws SQLException {
        Timestamp t = rs.getTimestamp(cot);
        return t == null ? null : t.toLocalDateTime();
    }

    static LocalDate ngay(ResultSet rs, String cot) throws SQLException {
        java.sql.Date d = rs.getDate(cot);
        return d == null ? null : d.toLocalDate();
    }

    static DataAccessException loi(String viec, SQLException e) {
        if (e.getErrorCode() == LOI_KHOA_NGOAI) {
            return new DataAccessException(
                    "Không thể " + viec + ": dữ liệu đang được dùng ở bảng khác (vd phim/phòng đã có suất chiếu hoặc vé).", e);
        }
        return new DataAccessException("Lỗi CSDL khi " + viec + ": " + e.getMessage(), e);
    }
}
