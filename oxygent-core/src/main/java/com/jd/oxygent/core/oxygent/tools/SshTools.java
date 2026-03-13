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
     * @param oxyRequest SSH channel to use
     * @return Command output
     */
    @Tool(
            name = "ssh_tool",
            description = "A tool for control the ubuntu terminal",
            paramMetas = {
                    @ParamMetaAuto(name = "shell_command", type = "String", description = "The shell command to execute")
            }
    )
    public static String sshTool(String shellCommand, OxyRequest oxyRequest) {
        if (shellCommand == null) {
            return "Error: shellCommand is required";
        }
        if (oxyRequest == null) {
            return "Error: SSH channel not initialized";
        }
        StringBuilder stringBuilder = new StringBuilder();
        try {
            ChannelExec channel = (ChannelExec) oxyRequest.getGlobalData("ssh_channel");
            channel.setCommand(shellCommand);
            log.info("Executing SSH command: {}", shellCommand);
            java.nio.charset.Charset charset = java.nio.charset.StandardCharsets.UTF_8;
            try (InputStream in = channel.getInputStream();
                 InputStream err = channel.getErrStream()) {
                channel.connect();
                byte[] tmp = new byte[1024];
                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i < 0) break;
                        stringBuilder.append(new String(tmp, 0, i, charset));
                    }
                    if (channel.isClosed()) {
                        if (in.available() > 0) continue;
                        stringBuilder.append("\nExit status: " + channel.getExitStatus());
                        break;
                    }
                    Thread.sleep(100);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  stringBuilder.toString();
    }
}
