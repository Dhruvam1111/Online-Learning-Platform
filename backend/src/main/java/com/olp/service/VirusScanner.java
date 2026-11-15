package com.olp.service;

import java.io.InputStream;

/**
 * Interface defining pre-storage virus and malware scanning hook (FR-I14).
 */
public interface VirusScanner {

    ScanResult scan(InputStream inputStream, String filename);

    class ScanResult {
        private final boolean clean;
        private final String threatName;

        public ScanResult(boolean clean, String threatName) {
            this.clean = clean;
            this.threatName = threatName;
        }

        public static ScanResult clean() {
            return new ScanResult(true, null);
        }

        public static ScanResult infected(String threatName) {
            return new ScanResult(false, threatName);
        }

        public boolean isClean() {
            return clean;
        }

        public String getThreatName() {
            return threatName;
        }
    }
}

