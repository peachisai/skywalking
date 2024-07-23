

package org.apache.skywalking.oap.server.receiver.datadog.provider.constants;

public enum DDSpanVersion {
    V4("/0.4"),
    V5("/0.5");

    private final String versionPath;

    DDSpanVersion(String versionPath) {
        this.versionPath = versionPath;
    }

    public static DDSpanVersion getVersion(String uri) {
        for (DDSpanVersion version : values()) {
            if (uri.contains(version.versionPath)) {
                return version;
            }
        }
        return null;
    }
}
