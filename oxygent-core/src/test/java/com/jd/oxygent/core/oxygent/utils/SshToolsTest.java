package com.jd.oxygent.core.oxygent.utils;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SshToolsTest {

    @Test
    void test() {
        String command = "ls -la";
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
            session.connect(15000);

            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            ((ChannelExec) channel).setErrStream(System.err);
            channel.connect();
            try (SmartCharsetReader reader = new SmartCharsetReader(channel.getInputStream())) {
                BufferedReader br = new BufferedReader(reader);

                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("[WSL Output]: " + line);
                }
            }

            channel.disconnect();
            session.disconnect();

        } catch (Exception e) {
            System.err.println("Error connecting to WSL: " + e.getMessage());
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
