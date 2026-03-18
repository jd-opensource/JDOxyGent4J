package com.jd.oxygent.test.tool;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jd.oxygent.core.oxygent.tools.SshTools;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import com.jd.oxygent.core.oxygent.utils.SmartCharsetReader;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Properties;

@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SshToolsTest {

    private static Session session;

    @BeforeAll
    public static void setUp() throws JSchException {
        String host = EnvUtils.getEnv("SSH_HOST");
        String user = EnvUtils.getEnv("SSH_USER");
        String password = EnvUtils.getEnv("SSH_PASSWORD");
        String keyPath = null;
        JSch jsch = new JSch();
        session = jsch.getSession(user, host, 22);
        session.setPassword(password);
        if (keyPath != null) {
            jsch.addIdentity(keyPath);
        }
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no"); // Skip fingerprint confirmation
        config.put("kex", "curve25519-sha256,diffie-hellman-group-exchange-sha256"); // Prefer modern algorithms
        session.setConfig(config);

        // Set heartbeat to prevent long-running commands (e.g., oxybank migration) from disconnecting
        session.setServerAliveInterval(30000);
        session.connect();
    }

    @Test
    @Order(1)
    void test() {
        String command = "ls -la";
        SshTools.sshTool(command, session);
    }

    @Test
    @Order(2)
    void testLongRunningCommand() {
        String command = "for i in {1..10}; do echo \"Step $i: Processing...\"; sleep 1; done";
        SshTools.sshTool(command, session);
    }

    @Test
    @Order(3)
    void testWget() {
        String command = "wget https://github.com/jd-opensource/JDOxyGent4J/blob/main/CHANGELOG_zh.md";
        SshTools.sshTool(command, session);
    }

    @Test
    @Order(4)
    void testGitCommand() {
        String command = "git clone --progress git@github.com:jd-opensource/JDOxyGent4J.git";
        SshTools.sshTool(command, session);
    }

    @Test
    void testWslIp() throws InterruptedException, IOException {
        String ip = getWslIpAddress();
        log.info("WSL IP: {}", ip);
    }

    public static String getWslIpAddress() throws InterruptedException, IOException {
        // Execute 'wsl hostname -I' to get the internal IP(s)
        ProcessBuilder pb = new ProcessBuilder("wsl", "hostname", "-I");
        Process process = pb.start();

        // Use try-with-resources to read the output
        try (BufferedReader reader = new BufferedReader(new SmartCharsetReader(process.getInputStream()))) {
            String output = reader.readLine();
            if (output != null && !output.isBlank()) {
                // 'hostname -I' may return multiple IPs; we take the first one
                return output.trim().split(" ")[0];
            }
        }

        // Wait for the process to exit
        if (process.waitFor() != 0) {
            throw new IOException("Failed to retrieve WSL IP. Ensure WSL is installed and running.");
        }

        throw new IOException("WSL returned an empty IP address.");
    }
}
