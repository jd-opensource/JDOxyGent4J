package com.jd.oxygent.core.oxygent.tools;

import com.jcraft.jsch.ChannelExec;
import com.jd.oxygent.core.oxygent.oxy.function_tools.FunctionHub;
import com.jd.oxygent.core.oxygent.schemas.oxy.OxyRequest;
import com.jd.oxygent.core.oxygent.utils.OSUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SshTools extends FunctionHub {

    public SshTools() {
        super("ssh_tools");
        this.setDesc("The shell command to execute");
    }

    /**
     * A tool for control the ubuntu terminal
     *
     * @param shellCommand The shell command to execute
     * @param channel SSH channel to use
     * @return Command output
     */
    @Tool(
            name = "ssh_tool",
            description = "A tool for control the ubuntu terminal",
            paramMetas = {
                    @ParamMetaAuto(name = "shell_command", type = "String", description = "The shell command to execute"),
                    @ParamMetaAuto(name = "channel", type = "com.jcraft.jsch.ChannelExec", description = "ssh channel object")
            }
    )
    public static String sshTool(String shellCommand, ChannelExec channel) {
        if (shellCommand == null) {
            return "Error: shellCommand is required";
        }
        if (channel == null) {
            return "Error: SSH channel not initialized";
        }
        StringBuilder stringBuilder = new StringBuilder();
        long timer = System.currentTimeMillis();
        try {
            channel.setCommand(shellCommand);
            log.info("Executing SSH command: {}", shellCommand);
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
        log.info("Executed SSH command: {} cost: {}ms", shellCommand, System.currentTimeMillis() - timer);
        return stringBuilder.toString();
    }
}
