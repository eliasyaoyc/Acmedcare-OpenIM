package com.acmedcare.tiffany.framework.remoting.jlib;

import android.content.Context;
import com.acmedcare.framework.kits.jre.http.HttpRequest;
import com.acmedcare.tiffany.framework.remoting.android.HandlerMessageListener;
import com.acmedcare.tiffany.framework.remoting.android.core.IoSessionEventListener;
import com.acmedcare.tiffany.framework.remoting.android.core.protocol.RemotingCommand;
import com.acmedcare.tiffany.framework.remoting.android.core.protocol.RequestCode;
import com.acmedcare.tiffany.framework.remoting.android.core.xlnio.XLMRClientConfig;
import com.acmedcare.tiffany.framework.remoting.android.core.xlnio.XLMRRemotingClient;
import com.acmedcare.tiffany.framework.remoting.android.core.xlnio.XLMRRequestProcessor;
import com.acmedcare.tiffany.framework.remoting.android.nio.core.future.IoFuture;
import com.acmedcare.tiffany.framework.remoting.android.nio.core.future.IoFutureListener;
import com.acmedcare.tiffany.framework.remoting.android.nio.core.session.IoSession;
import com.acmedcare.tiffany.framework.remoting.android.utils.RemotingHelper;
import com.acmedcare.tiffany.framework.remoting.jlib.biz.BizCode;
import com.acmedcare.tiffany.framework.remoting.jlib.biz.request.AuthRequest;
import com.acmedcare.tiffany.framework.remoting.jlib.events.AcmedcareEvent;
import com.acmedcare.tiffany.framework.remoting.jlib.events.BasicListenerHandler;
import com.acmedcare.tiffany.framework.remoting.jlib.exception.NoServerAddressException;
import com.acmedcare.tiffany.framework.remoting.jlib.jre.JREBizExectuor;
import com.acmedcare.tiffany.framework.remoting.jlib.processor.ServerPushMessageProcessor;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.eventbus.AsyncEventBus;
import java.io.Serializable;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;

/**
 * Acmedcare+ Remoting SDK Main Class
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version alpha - 26/07/2018.
 */
public final class AcmedcareRemoting implements Serializable {
  @Deprecated private static final String TAG = AcmedcareRemoting.class.getSimpleName();
  private static final AcmedcareRemoting INSTANCE = InstanceHolder.INSTANCE;
  private static final long serialVersionUID = -9029081624617687982L;
  private static int reConnectRetryTimes = 5; // 5 time
  private static long reConnectPeriod = 8; // 8 s
  /** Init Flag */
  private static volatile boolean inited = false;

  private static volatile boolean shutdowned = false;

  private static volatile boolean focusLogout = false;

  private static AtomicBoolean initedOnce = new AtomicBoolean(false);
  /** Running Flag */
  private static volatile boolean running = false;

  private static AtomicBoolean runOnce = new AtomicBoolean(false);
  /** Connection Flag */
  @Getter private static volatile boolean connecting = false;

  private static AsyncEventBus eventBus;
  private static ScheduledExecutorService connectWatcher;
  private static XLMRClientConfig clientConfig;
  private static RemotingParameters parameters;
  @Getter private static XLMRRemotingClient remotingClient;
  @Getter private static List<String> addresses = Lists.newArrayList();
  @Getter private volatile String currentLoginName;
  private long delay;
  /** Biz Executor Api */
  private BizExecutor bizExecutor;

  @Getter @Setter private String currentRemotingAddress;
  private RemotingConnectListener listener;
  /** Heartbeat config */
  @Deprecated private ScheduledExecutorService heartbeatExecutor;

  @Deprecated private IoSession remotingSession;

  private AcmedcareRemoting() {
    String buffer =
        "\r\n============================================================"
            + "\r\n\t\tAcmedcare IM SDK Version    :"
            + Version.get()
            + "\r\n\t\tSDK LogCat Filter TAG       : "
            + AcmedcareLogger.SDK_LOG_TAG
            + "\r\n============================================================";
    AcmedcareLogger.i(null, buffer);
  }

  /**
   * Get Acmedcare Remoting Instance Static Method
   *
   * @return instance
   */
  public static AcmedcareRemoting getInstance() {
    return InstanceHolder.INSTANCE;
  }

  public BizExecutor executor() {
    return bizExecutor;
  }

  /**
   * Get Event Bus Instance
   *
   * @return event bus
   */
  public AsyncEventBus eventBus() {
    return eventBus;
  }

  public void setCurrentLoginName(String username) {
    this.currentLoginName = username;
  }

  public synchronized void init(Context context, RemotingParameters parameters) {

    if (initedOnce.get()) {
      return;
    }

    if (initedOnce.compareAndSet(false, true)) {
      AcmedcareLogger.i(TAG, "Start to init-ing remoting client.");

      if (inited) {
        AcmedcareLogger.i(TAG, "Remoting Client already init-ed. // ignore invoke.");
        return;
      }

      if (parameters != null) {
        AcmedcareRemoting.parameters = parameters;
      }
      inited = true;

      // register bizExecutor
      this.bizExecutor = new JREBizExectuor(this);

      eventBus = new AsyncEventBus(Executors.newFixedThreadPool(8));

      AcmedcareLogger.i(TAG, "Remoting Client init-ed.");
    }
  }

  /** Register Connection Status Change Listener */
  public void registerConnectionEventListener(RemotingConnectListener listener) {
    if (this.listener == null) {
      this.listener = listener;
    }
  }

  /**
   * Active Remoting Client (first start / re-start)
   *
   * @param delay client connect delay time (unit: ms)
   * @throws NoServerAddressException no server address exception
   */
  public synchronized void run(final long delay) throws NoServerAddressException {

    if (runOnce.get()) {
      return;
    }
    try {

      if (runOnce.compareAndSet(false, true)) {
        if (running) {
          AcmedcareLogger.i(TAG, "Remoting Client already running. // ignore invoke.");
          return;
        }

        running = true;

        this.delay = delay;
        assert AcmedcareRemoting.parameters != null
            && AcmedcareRemoting.parameters.getServerAddressHandler() != null;

        // ssl
        if (AcmedcareRemoting.parameters.isEnableSSL()) {
          System.setProperty("tiffany.quantum.encrypt.enable", "true");
        }

        // processor
        processorParameters();

        if (AcmedcareRemoting.remotingClient == null) {
          // random a remote address
          newRemotingClient();

          assert AcmedcareRemoting.remotingClient != null;
          AcmedcareRemoting.remotingClient.updateNameServerAddressList(AcmedcareRemoting.addresses);

          // connect
          doConnect(false);
        }
      }
    } catch (Throwable e) {
      AcmedcareLogger.e(TAG, e, "Remoting Active failed.");
      if (e instanceof NoServerAddressException) {
        throw e;
      }
    }
  }

  private void processorParameters() throws NoServerAddressException {
    // 设置重连间隔
    reConnectPeriod = AcmedcareRemoting.parameters.getReConnectPeriod();
    reConnectRetryTimes = AcmedcareRemoting.parameters.getReConnectRetryTimes();

    // address list
    List<ServerAddressHandler.RemotingAddress> masterAddresses =
        AcmedcareRemoting.parameters.getServerAddressHandler().remotingAddressList();

    if (masterAddresses != null && masterAddresses.size() > 0) {
      List<String> clusterServers = Lists.newArrayList();
      for (ServerAddressHandler.RemotingAddress address : masterAddresses) {
        try {
          String url =
              (address.isHttps() ? "https://" : "http://")
                  + address.toString()
                  + "/master/available-cluster-servers";
          AcmedcareLogger.i(TAG, "获取可用服务器请求地址: " + url);
          HttpRequest request = HttpRequest.get(url);
          if (request.ok()) {
            String body = request.body("UTF-8");
            AcmedcareLogger.i(TAG, "获取可用服务器请求返回值: " + body);
            if (!Strings.isNullOrEmpty(body)) {
              List<String> temp = JSON.parseObject(body, new TypeReference<List<String>>() {});
              if (temp != null && temp.size() > 0) {
                clusterServers.addAll(temp);
                break;
              }
            }
          }
        } catch (Exception e) {
          AcmedcareLogger.i(TAG, "从主服务器:" + address.toString() + "获取可用通讯服务器地址失败");
        }
      }

      AcmedcareLogger.i(TAG, " 可用通讯服务器地址:" + JSON.toJSONString(clusterServers));
      if (clusterServers.isEmpty()) {
        AcmedcareLogger.w(TAG, "无可用的通讯服务器");
        throw new NoServerAddressException();
      }

      AcmedcareRemoting.addresses.clear();
      AcmedcareRemoting.addresses.addAll(clusterServers);

      Random indexRandom = new Random();
      int index = indexRandom.nextInt(AcmedcareRemoting.addresses.size());
      this.currentRemotingAddress = AcmedcareRemoting.addresses.get(index);

      // assert address must not be null
      assert this.currentRemotingAddress != null;
    } else {
      throw new NoServerAddressException("No found remote server address .");
    }
  }

  /** Build new Remoting Client Instacne */
  private void newRemotingClient() {
    if (AcmedcareRemoting.clientConfig == null) {
      AcmedcareLogger.i(null, "build new client config with default setting");
      AcmedcareRemoting.clientConfig = new XLMRClientConfig();
    }

    AcmedcareLogger.i(null, "build new remoting client instance for connect");
    AcmedcareRemoting.remotingClient =
        new XLMRRemotingClient(
            AcmedcareRemoting.clientConfig,
            new IoSessionEventListener() {
              @Override
              public void onSessionConnect(IoSession ioSession) {

                if (ioSession.isConnected()) {
                  if (AcmedcareRemoting.this.listener != null) {
                    AcmedcareRemoting.this.listener.onConnect(
                        AcmedcareRemoting.getRemotingClient());
                  }

                  try {
                    // 计算PING
                    final long start = System.currentTimeMillis();
                    RemotingCommand ping =
                        RemotingCommand.createRequestCommand(
                            RequestCode.SYSTEM_HEARTBEAT_CODE, null);
                    ioSession
                        .write(ping)
                        .addListener(
                            new IoFutureListener<IoFuture>() {
                              @Override
                              public void operationComplete(IoFuture future) {
                                if (future.isDone()) {
                                  AcmedcareLogger.i(
                                      null,
                                      ">>>>> Client & Remote Server Ping : "
                                          + (System.currentTimeMillis() - start)
                                          + " ms");
                                }
                              }
                            });
                  } catch (Exception e) {
                    AcmedcareLogger.w(null, "SDK Tester Ping process failed, ignore~");
                  }

                  AcmedcareRemoting.this.remotingSession = ioSession;
                  // auth automatic
                  AcmedcareRemoting.this.bizExecutor.auth(
                      AuthRequest.builder()
                          .username(AcmedcareRemoting.parameters.getUsername())
                          .build(),
                      AcmedcareRemoting.parameters.getAuthCallback());
                }
              }

              @Override
              public void onSessionClose(IoSession ioSession) {
                if (AcmedcareRemoting.this.listener != null) {
                  AcmedcareRemoting.this.listener.onClose(AcmedcareRemoting.getRemotingClient());
                }
                AcmedcareLogger.i(null, "Connection is closed");

                // release
                releaseResources();

                if (!AcmedcareRemoting.shutdowned && !focusLogout) {
                  AcmedcareLogger.i(
                      null, "Start new thread<Acmedcare-Dog-Thread> to retry connecting...");
                  new Thread(
                          new Runnable() {
                            @Override
                            public void run() {
                              AcmedcareRemoting.this.reConnect();
                            }
                          },
                          "Acmedcare-Dog-Thread")
                      .start();
                }
              }

              @Override
              public void onSessionException(IoSession ioSession, Throwable throwable) {
                AcmedcareLogger.e(null, throwable, "Connection is exception ");
              }

              @Override
              public void onSessionIdle(IoSession ioSession) {
                AcmedcareLogger.i(null, "Connection is idle");
                if (AcmedcareRemoting.this.listener != null) {
                  AcmedcareRemoting.this.listener.onIdle(AcmedcareRemoting.getRemotingClient());
                }
              }
            });

    // register processor
    AcmedcareRemoting.remotingClient.registerProcessor(
        BizCode.SERVER_PUSH_MESSAGE, new ServerPushMessageProcessor(this), null);

    // register focus logout processor
    AcmedcareRemoting.remotingClient.registerProcessor(
        BizCode.SERVER_PUSH_FOCUS_LOGOUT,
        new XLMRRequestProcessor() {
          @Override
          public RemotingCommand processRequest(
              IoSession session, RemotingCommand request, HandlerMessageListener listener)
              throws Exception {

            AcmedcareRemoting.focusLogout = true;
            AcmedcareLogger.i(null, "收到服务端推送的下线通知,账号在其他设备登录.");
            AcmedcareRemoting.this
                .eventBus()
                .post(
                    new AcmedcareEvent() {
                      @Override
                      public Event eventType() {
                        return SystemEvent.LOGIN_ON_OTHER_DEVICE;
                      }

                      @Override
                      public Object data() {
                        return null;
                      }
                    });

            // shutdown self
            AcmedcareRemoting.this.shutdownNow();

            // ignore response
            return null;
          }
        },
        null);
  }

  private void doConnect(final boolean now) {
    // async start thread
    Thread startThread =
        new Thread(
            new Runnable() {
              @Override
              public void run() {
                try {
                  if (!now) {
                    Thread.sleep(delay);
                  }
                  AcmedcareLogger.i(TAG, "start to connect server..");
                  remotingClient.start();
                } catch (Throwable e) {
                  AcmedcareLogger.e(TAG, e, "Async Connection Thread execute failed.");
                }
              }
            },
            "remoting-client-network-connect-thread");
    startThread.start();
  }

  /** Shutdown Now */
  public void shutdownNow() {

    AcmedcareLogger.i(null, "Ready to stop sdk-remoting connection~");
    AcmedcareRemoting.shutdowned = true;
    // release first
    if (AcmedcareRemoting.remotingClient != null) {
      AcmedcareRemoting.remotingClient.shutdown();
    }

    // shutdownNow
    if (eventBus != null) {
      try {
        eventBus.unregister(this);
        eventBus = null;
      } catch (Exception ignore) {
      }
    }

    AcmedcareLogger.i(null, "unregister event bus ~");

    releaseResources();
    initedOnce.compareAndSet(true, false);

    AcmedcareLogger.i(null, "reset cached values settings ~");

    if (connectWatcher != null) {
      try {
        connectWatcher.shutdownNow();
      } catch (Exception e) {
      }
    }

    //
    inited = false;
    running = false;

    currentLoginName = null;
    parameters = null;
    bizExecutor = null;
    remotingClient = null;
    addresses.clear();
    listener = null;
    remotingSession = null;
    currentRemotingAddress = null;

    AcmedcareLogger.i(null, "release remoting's instances ~");
  }

  private void reConnect0() throws NoServerAddressException {

    AcmedcareLogger.i(TAG, "Try to re-connecting");
    // re-shutdown
    try {
      if (AcmedcareRemoting.remotingClient != null) {
        AcmedcareRemoting.remotingClient.shutdown();
      }
    } catch (Exception ignore) {
      AcmedcareLogger.w(null, "shutdown remoting client exception <ignore~> ");
    }

    // release remoting client
    AcmedcareRemoting.remotingClient = null;

    AcmedcareLogger.i(null, "re-process user parameters .");
    processorParameters();
    // re-new remoting client
    newRemotingClient();

    // update remoting address list
    AcmedcareRemoting.remotingClient.updateNameServerAddressList(AcmedcareRemoting.addresses);

    // connect
    try {
      AcmedcareLogger.i(TAG, "re-Connecting...");

      remotingClient.start();

    } catch (Exception e) {
      AcmedcareLogger.e(TAG, e, "Re-Connect Failed");
    }
  }

  /**
   * Re-Connect Api Method
   *
   * <pre>
   *
   *   <li>Only when system lose connect with remoting server ,then can invoke this method;
   *
   * </pre>
   */
  public void reConnect() {
    try {
      AcmedcareLogger.i(
          null, "Ready to re-connect to remoting server with " + reConnectRetryTimes + " times");
      if (!AcmedcareRemoting.connecting) {
        int times = reConnectRetryTimes;
        while (--times >= 0) {
          if (connecting) {
            AcmedcareLogger.i(null, "network is connect successed , break re-connect loop;");
            break;
          }

          try {
            AcmedcareRemoting.getInstance().reConnect0();
            Thread.sleep(reConnectPeriod * 1000 + (reConnectRetryTimes - times) * 1000);
          } catch (InterruptedException ignored) {

          } catch (NoServerAddressException e) {
            AcmedcareLogger.e(null, e, "[ERROR] remoting server address is <null> ,failed.");
          }
        }

        if (!AcmedcareRemoting.connecting) {
          AcmedcareLogger.w(
              null,
              "Retry "
                  + reConnectRetryTimes
                  + " times , network no working yet, post <RE_CONNECT_FAILED> for user to process self.");
          // ignore
          eventBus()
              .post(
                  new AcmedcareEvent() {
                    @Override
                    public Event eventType() {
                      return SystemEvent.RE_CONNECT_FAILED;
                    }

                    @Nullable
                    @Override
                    public Object data() {
                      return null;
                    }
                  });
        }
      }

    } catch (Exception e) {
      AcmedcareLogger.e(null, e, "re-connect thread execute exception");
    }
  }

  public void updateConnectStatus() {
    AcmedcareRemoting.connecting = true;
    AcmedcareLogger.i(null, "update connection status with : " + connecting);
    AcmedcareRemoting.shutdowned = false;
    AcmedcareLogger.i(null, "update connection shutdown flag with : " + shutdowned);
    AcmedcareRemoting.focusLogout = false;
    AcmedcareLogger.i(
        null,
        "Connect is established: ["
            + RemotingHelper.parseSocketAddressAddr(remotingSession.getLocalAddress())
            + "] -> ["
            + currentRemotingAddress
            + "]");
  }

  private void releaseResources() {
    AcmedcareLogger.i(null, "Ready to release sdk framework resouces ~");
    AcmedcareRemoting.connecting = false;

    runOnce.compareAndSet(true, false);

    if (heartbeatExecutor != null) {
      try {
        this.heartbeatExecutor.shutdownNow();
      } catch (Exception ignore) {
      }
    }
  }

  /** Register Message Handler */
  public void onMessageEventListener(BasicListenerHandler eventHandler) {
    if (eventHandler != null) {
      // register event bus handler
      eventBus().register(eventHandler);
      AcmedcareLogger.i(null, "application register event listener handler :" + eventHandler);
    }
  }

  /** Remoting Connection Status Listener */
  public interface RemotingConnectListener {

    /**
     * Invoke When Connect is establish;
     *
     * @param client client
     */
    void onConnect(XLMRRemotingClient client);

    /**
     * Invoked when closed;
     *
     * @param client client
     */
    void onClose(XLMRRemotingClient client);

    /**
     * Invoked When exception;
     *
     * @param client client
     */
    void onException(XLMRRemotingClient client);

    /**
     * Invoked When Idle;
     *
     * @param client client
     */
    void onIdle(XLMRRemotingClient client);
  }

  private static class InstanceHolder {
    private static AcmedcareRemoting INSTANCE = new AcmedcareRemoting();
  }
}
