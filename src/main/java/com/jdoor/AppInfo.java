package com.jdoor;

public final class AppInfo {
    public static final String NAME = "JDoor Assist";
    public static final int DEFAULT_PORT = 8_443;

    private AppInfo() {}

    public static String version() {
        String implementationVersion = AppInfo.class.getPackage().getImplementationVersion();
        return implementationVersion == null || implementationVersion.isBlank() ? "development" : implementationVersion;
    }
}
