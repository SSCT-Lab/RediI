package io.redit.samples.zookeeper3895;

import io.redit.ReditRunner;
import io.redit.exceptions.RuntimeEngineException;
import org.apache.zookeeper.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.redit.helpers.ZookeeperHelper;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

public class SampleTest {
    private static final Logger logger = LoggerFactory.getLogger(SampleTest.class);
    protected static ReditRunner runner;
    private static ZookeeperHelper helper;
    private static ZooKeeper[] zooKeeper = {null, null, null};

    @BeforeClass
    public static void before() throws RuntimeEngineException, IOException, InterruptedException {
        runner = ReditRunner.run(ReditHelper.getDeployment());
        ReditHelper.startNodes(runner);
        helper = new ZookeeperHelper(runner, ReditHelper.getZookeeperHomeDir(), logger, ReditHelper.getFileRW(), ReditHelper.numOfServers);
        helper.addConfFile();
        logger.info("wait for zookeeper...");
        helper.startServers();
        Thread.sleep(5000);
        helper.checkServersStatus();
    }

    @AfterClass
    public static void after() {
        if (runner != null) {
            runner.stop();
        }
    }

    @Test
    public void testEmptyMultiOp() throws RuntimeEngineException {
        runner.runtime().enforceOrder("E1", () -> {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            try {
                zooKeeper[0] = new ZooKeeper(helper.connectionStr, 4000, watchedEvent -> countDownLatch.countDown());
                countDownLatch.await();
                zooKeeper[0].multi(Collections.emptyList());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        logger.info("completed !!!");
    }
}
