package com.jd.oxygent.core.oxygent.utils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Slf4j
public class SshToolsTest {

    @Test
    void test() {
        String command = "ls -la";
        connect(EnvUtils.getEnv("SSH_USER"), EnvUtils.getEnv("SSH_HOST"), EnvUtils.getEnv("SSH_PASSWORD"), command);
    }

//    @Test
//    void testLongRunningCommand() {
//        String command = "for i in {1..10}; do echo \"Step $i: Processing...\"; sleep 1; done";
//        connect(EnvUtils.getEnv("SSH_USER"), EnvUtils.getEnv("SSH_HOST"), EnvUtils.getEnv("SSH_PASSWORD"), command);
//    }

    @Test
    void testGitCommand() {
        String command = "git clone https://github.com/jd-opensource/OxyGent.git\n";
        connect(EnvUtils.getEnv("SSH_USER"), EnvUtils.getEnv("SSH_HOST"), EnvUtils.getEnv("SSH_PASSWORD"), command);
    }

    public void connect(String user, String ip, String password, String command) {
        try {
            if (ip == null) {
                ip = getWslIpAddress();
                System.out.println("Detected WSL IP: " + ip);
            }
            JSch.setLogger(new Logger() {
                @Override public boolean isEnabled(int level) { return true; }
                @Override public void log(int level, String message) {
                    System.out.println("[JSch Log] " + message);
                }
            });
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, ip, 22);
            session.setPassword(password);
            session.setConfig("kex", "curve25519-sha256,curve25519-sha256@libssh.org,diffie-hellman-group-exchange-sha256"); // Prioritize modern algorithms in JSch
            session.setConfig("StrictHostKeyChecking", "no"); // to mute error com.jcraft.jsch.JSchUnknownHostKeyException: UnknownHostKey:
            session.setConfig("compression.s2c", "none");
            session.connect(15000);

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setPty(true);
            long timer = System.currentTimeMillis();
            try {
                channel.setCommand(command);
                log.info("Executing SSH command: {}", command);
                StringBuilder stringBuilder = new StringBuilder();
                java.nio.charset.Charset charset = java.nio.charset.StandardCharsets.UTF_8;
                String line;
                try (InputStream in = channel.getInputStream();
                     InputStream err = channel.getErrStream()) {
                    channel.connect();
                    byte[] buffer = new byte[1024];
                    while (true) {
                        while (in.available() > 0) {
                            int i = in.read(buffer, 0, 1024);
                            if (i < 0) break;
                            // Output to the console in real-time without waiting for a newline character.
                            line = new String(buffer, 0, i, charset);
                            System.out.print(line);
                            stringBuilder.append(line);
                            System.out.flush();
                        }
                        if (channel.isClosed()) {
                            if (in.available() > 0) continue;
                            break;
                        }
                        Thread.sleep(50); // to low cpu usage
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            log.info("Executed SSH command: {} cost: {}ms", command, System.currentTimeMillis() - timer);
            channel.disconnect();
            session.disconnect();

        } catch (Exception e) {
            System.err.println("Error connecting: " + e.getMessage());
            e.printStackTrace();
        }
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
