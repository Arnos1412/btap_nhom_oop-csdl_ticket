package com.nhom.datve.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Doc file cau hinh db.properties.
 *  - Chay bang mvn / IDE: doc src/main/resources/db.properties (tren classpath).
 *  - Chay bang file exe (co -Ddatve.exe=true): doc %APPDATA%\DatVeXemPhim\db.properties,
 *    file nay do app tu tao lan dau qua script tu_cau_hinh.ps1 (xem TuCauHinh).
 * Neu khong co file -> chay che do mock (du lieu gia) de code giao dien khong can SQL Server.
 */
public final class AppConfig {
    private static final Properties PROPS = new Properties();
    private static volatile boolean coFile;
    private static volatile String nguonCauHinh;

    static {
        reload();
    }

    private AppConfig() { }

    /** true neu app dang chay tu file exe dong goi bang jpackage. */
    public static boolean isChayTuExe() {
        return Boolean.getBoolean("datve.exe");
    }

    /** File cau hinh rieng cua ban exe (nam ngoai file exe, khong bi dong goi kem mat khau). */
    public static Path fileCauHinhExe() {
        String appData = System.getenv("APPDATA");
        Path goc = appData != null ? Paths.get(appData) : Paths.get(System.getProperty("user.home"));
        return goc.resolve("DatVeXemPhim").resolve("db.properties");
    }

    /** Doc lai cau hinh (dung sau khi script tu cau hinh vua ghi file moi). */
    public static synchronized void reload() {
        PROPS.clear();
        coFile = false;
        nguonCauHinh = null;
        if (isChayTuExe()) {
            Path f = fileCauHinhExe();
            if (Files.isRegularFile(f)) {
                try (InputStream in = Files.newInputStream(f)) {
                    PROPS.load(in);
                    coFile = true;
                    nguonCauHinh = f.toString();
                } catch (IOException e) {
                    System.err.println("Khong doc duoc " + f + ": " + e.getMessage());
                }
            }
            return;
        }
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                PROPS.load(in);
                coFile = true;
                nguonCauHinh = "src/main/resources/db.properties";
            }
        } catch (IOException e) {
            System.err.println("Khong doc duoc db.properties: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return PROPS.getProperty(key);
    }

    /** true neu co file cau hinh. */
    public static boolean coFileCauHinh() {
        return coFile;
    }

    /** Duong dan file cau hinh dang dung (null neu khong co). */
    public static String nguonCauHinh() {
        return nguonCauHinh;
    }

    /** true neu chay voi du lieu gia. Mac dinh la mock khi chua co db.properties. */
    public static boolean isMockMode() {
        return !coFile || !"jdbc".equalsIgnoreCase(PROPS.getProperty("app.mode", "mock").trim());
    }

    public static void setMockMode(boolean mock) {
        PROPS.setProperty("app.mode", mock ? "mock" : "jdbc");
    }
}
