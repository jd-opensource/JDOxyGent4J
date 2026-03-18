package com.jd.oxygent.core.oxygent.tools;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.utils.EnvUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j
public class SshTools extends FunctionHub {

    public SshTools() {
        super("ssh_tools");
        this.setDesc("The shell command to execute");
    }

    /**
     * A tool for control the ubuntu terminal
     *
     * @param command The shell command to execute
     * @param session      SSH channel to use
     * @return Command output
     */
    @Tool(
            name = "ssh_tool",
            description = "A tool for control the ubuntu terminal",
            paramMetas = {
                    @ParamMetaAuto(name = "shell_command", type = "String", description = "The shell command to execute"),
                    @ParamMetaAuto(name = "session", type = "com.jcraft.jsch.Session", description = "ssh session object")
            }
    )
    public static String sshTool(String command, Session session) {
        if (command == null) {
            return "Error: shellCommand is required";
        }
        if (session == null) {
            return "Error: SSH session not initialized";
        }
        StringBuilder stringBuilder = new StringBuilder();
        long timer = System.currentTimeMillis();
        try {
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            // Core fix: Merge stderr to stdout to prevent buffer overflow causing commands like git clone to hang
            channel.setErrStream(System.out, true);

            log.info("Executing SSH command: {}", command);
            InputStream in = channel.getInputStream();
            channel.connect();

            // Real-time, non-blocking byte reading for real-time log printing
            byte[] buffer = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, 1024);
                    if (i < 0) break;
                    System.out.print(new String(buffer, 0, i, StandardCharsets.UTF_8));
                    System.out.flush();
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    break;
                }
                Thread.sleep(50);
            }

            int exitStatus = channel.getExitStatus();
            log.info("Executed SSH command: {} cost: {}ms", command, System.currentTimeMillis() - timer);
            in.close();
            channel.disconnect();
        } catch (Exception e) {
            log.error("Error executing SSH command: {}", command, e);
        }
        return stringBuilder.toString();
    }
}
