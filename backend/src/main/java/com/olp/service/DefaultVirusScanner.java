package com.olp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Default implementation of the VirusScanner interface (FR-I14).
 * Inspects payload signatures including the EICAR test pattern and disguised
 * executable magic bytes.
 */
@Service
public class DefaultVirusScanner implements VirusScanner {

    private static final Logger logger = LoggerFactory.getLogger(DefaultVirusScanner.class);

    private static final String EICAR_PREFIX = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!";

    @Override
    public ScanResult scan(InputStream inputStream, String filename) {
        if (inputStream == null) {
            return ScanResult.clean();
        }

        try {
            // Read up to 8KB header for signature inspection
            byte[] buffer = new byte[8192];
            int read = inputStream.read(buffer);
            if (read <= 0) {
                return ScanResult.clean();
            }

            String contentPreview = new String(buffer, 0, read, StandardCharsets.ISO_8859_1);
            if (contentPreview.contains(EICAR_PREFIX)
                    || contentPreview.contains("EICAR-STANDARD-ANTIVIRUS-TEST-FILE")) {
                logger.warn("Virus scanner detected EICAR test pattern in file: {}", filename);
                return ScanResult.infected("EICAR-Test-Signature");
            }

            // Check for DOS/PE Executable magic bytes 'MZ' (0x4D, 0x5A) in non-executable
            // files
            if (read >= 2 && buffer[0] == 0x4D && buffer[1] == 0x5A) {
                logger.warn("Disguised executable binary detected (MZ header) in file: {}", filename);
                return ScanResult.infected("Win32.Executable.Payload.Blocked");
            }

            // Check for Linux ELF executable magic bytes (0x7F, 'E', 'L', 'F')
            if (read >= 4 && buffer[0] == 0x7F && buffer[1] == 0x45 && buffer[2] == 0x4C && buffer[3] == 0x46) {
                logger.warn("Disguised ELF executable binary detected in file: {}", filename);
                return ScanResult.infected("Linux.ELF.Payload.Blocked");
            }

            return ScanResult.clean();
        } catch (IOException e) {
            logger.error("Error reading stream during virus scan: {}", e.getMessage());
            return ScanResult.clean();
        }
    }
}
