package com.flow.platform.cmd.test;

import com.flow.platform.cmd.CmdExecutor;
import com.flow.platform.cmd.Log;
import com.flow.platform.cmd.LogListener;
import com.google.common.collect.Lists;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by gy@fir.im on 16/05/2017.
 */
public class CmdExecutorTest {

    private LogListener logListener = new LogListener() {
        @Override
        public void onLog(Log log) {
            System.out.println(log);
        }

        @Override
        public void onFinish() {

        }
    };

    @Test
    public void should_execute_command_with_correct_event() throws Throwable {
        ClassLoader loader = this.getClass().getClassLoader();
        String path = loader.getResource("test.sh").getFile();
        Runtime.getRuntime().exec("chmod +x " + path);

        // run test.sh and export var start with CMD_RUNNER_TEST_ and OUTPUT_2
        CmdExecutor executor = new CmdExecutor(null,
            logListener,
            null,
            null,
            Lists.newArrayList("CMD_RUNNER_TEST_", "OUTPUT_2"),
            null,
            Lists.newArrayList(String.format("source %s", path)));

        executor.run();

        Assert.assertEquals(0, executor.getExitCode().intValue());


        Assert.assertNotNull(executor.getProcessId());
        Assert.assertNotNull(executor.getDuration());
        Assert.assertNotNull(executor.getStartAt());
        Assert.assertNotNull(executor.getFinishAt());

        Map<String, String> output = executor.getOutput();
        Assert.assertEquals(2, output.size());
        Assert.assertEquals("test1", output.get("CMD_RUNNER_TEST_1"));
        Assert.assertEquals("test2", output.get("OUTPUT_2"));
    }

    @Test
    public void should_not_export_output_when_cmd_got_error() throws Throwable {
        ClassLoader loader = this.getClass().getClassLoader();
        String path = loader.getResource("test_with_cmd_err.sh").getFile();
        Runtime.getRuntime().exec("chmod +x " + path);

        CmdExecutor executor = new CmdExecutor(null,
            logListener,
            null,
            null,
            Lists.newArrayList("CMD_RUNNER_TEST"),
            null,
            Lists.newArrayList(String.format("source %s", path)));

        executor.run();
        Assert.assertEquals(0, executor.getOutput().size());
        Assert.assertNotEquals(0, executor.getExitCode().intValue());
    }
}
