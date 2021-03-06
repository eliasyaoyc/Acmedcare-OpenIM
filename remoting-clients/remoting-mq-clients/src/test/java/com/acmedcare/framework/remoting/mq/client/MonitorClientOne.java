package com.acmedcare.framework.remoting.mq.client;

import com.acmedcare.framework.remoting.mq.client.AcmedcareMQRemoting.RemotingConnectListener;
import com.acmedcare.framework.remoting.mq.client.biz.bean.Message;
import com.acmedcare.framework.remoting.mq.client.biz.request.AuthRequest.AuthCallback;
import com.acmedcare.framework.remoting.mq.client.events.AcmedcareEvent;
import com.acmedcare.framework.remoting.mq.client.events.AcmedcareEvent.Event;
import com.acmedcare.framework.remoting.mq.client.events.BasicListenerHandler;
import com.acmedcare.framework.remoting.mq.client.exception.NoServerAddressException;
import com.acmedcare.framework.remoting.mq.client.exception.SdkInitException;
import com.acmedcare.tiffany.framework.remoting.android.core.xlnio.XLMRRemotingClient;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

/**
 * MonitorClientOne
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version ${project.version} - 2018-12-25.
 */
public class MonitorClientOne extends BaseClient {

  public static void main(String[] args) {

    System.setProperty(AcmedcareLogger.NON_ANDROID_FLAG, "true");

    RemotingParameters temp =
        RemotingParameters.builder()
            .authCallback(
                new AuthCallback() {
                  @Override
                  public void onSuccess() {
                    System.out.println("授权成功");
                  }

                  @Override
                  public void onFailed(int code, String message) {
                    System.out.println("授权失败,Code=" + code + ", 错误:" + message);
                  }
                })
            .enableSSL(true)
            //            .jksFile(
            //                new File(
            //
            // "/Users/ive/git-acmedcare/Acmedcare-NewIM/remoting-certs/client/keystore.jks"))
            //            .jksPassword("1qaz2wsx")
            .username(KnownParams.passport)
            .accessToken(KnownParams.accessToken)
            .areaNo(KnownParams.areaNo)
            .orgId(KnownParams.orgId)
            .passportId(KnownParams.passportId)
            .deviceId(KnownParams.deviceId)
            .heartbeatPeriod(10)
            .clientType(ClientType.MONITOR)
            .serverAddressHandler(
                new ServerAddressHandler() {
                  @Override
                  public List<RemotingAddress> remotingAddressList() {
                    return Lists.newArrayList(
                        new RemotingAddress(false, "192.168.1.227", 13110, false));
                  }
                })
            .build();

    AcmedcareMQRemoting.getInstance().init(null, temp);

    AcmedcareMQRemoting.getInstance()
        .registerConnectionEventListener(
            new RemotingConnectListener() {
              @Override
              public void onConnect(XLMRRemotingClient client) {
                System.out.println("连接已打开");
              }

              @Override
              public void onClose(XLMRRemotingClient client) {
                System.out.println("连接断开,等待 SDK 重连");
              }

              @Override
              public void onException(XLMRRemotingClient client) {}

              @Override
              public void onIdle(XLMRRemotingClient client) {}
            });

    AcmedcareMQRemoting.getInstance()
        .onMessageEventListener(
            new BasicListenerHandler() {
              @Override
              public void execute(AcmedcareEvent acmedcareEvent) {
                Event event = acmedcareEvent.eventType();
                Object data = acmedcareEvent.data();
                System.out.println("接收到事件:" + event + " ,数据: " + JSON.toJSONString(data));
              }
            });

    AcmedcareMQRemoting.getInstance()
        .registerTopicMessageListener(
            new TopicMessageListener() {
              @Override
              public ConsumeResult onMessages(List<Message> messages) {
                System.out.println("接收到主题消息: " + JSON.toJSONString(messages));
                return ConsumeResult.CONSUMED;
              }
            });

    try {
      AcmedcareMQRemoting.getInstance().run(1000);

    } catch (NoServerAddressException | SdkInitException e) {
      e.printStackTrace();
    }

    while (true) {

      try {
        System.out.print("> ");
        String input =
            new BufferedReader(new InputStreamReader(System.in, Charsets.UTF_8)).readLine();
        if (input == null || input.length() == 0) {
          continue;
        }

        input = input.trim();

        // functions
        String[] inputArgs = input.split("\\s+");

        // TODO

        if ("createTopic".equals(inputArgs[0])) {
          createTopic(
              KnownParams.passport, KnownParams.passportId.toString(), inputArgs[1], inputArgs[2]);
          continue;
        }

        if ("pullTopics".equals(inputArgs[0])) {
          pullTopics(KnownParams.passport, KnownParams.passportId.toString());
          continue;
        }

        if ("pullTopicSubscribeMappings".equals(inputArgs[0])) {
          pullTopicSubscribeMappings(
              KnownParams.passport, KnownParams.passportId.toString(), inputArgs[1]);
          continue;
        }

        // sendTopicMessage
        if ("sendTopicMessage".equals(inputArgs[0])) {
          sendTopicMessage(
              KnownParams.passport,
              KnownParams.passportId.toString(),
              Long.parseLong(inputArgs[1]),
              inputArgs[2],
              inputArgs[3]);
          continue;
        }

        // subscribeTopic 285269691027713
        if ("subscribeTopic".equals(inputArgs[0])) {
          subscribeTopic(KnownParams.passport, KnownParams.passportId.toString(), inputArgs[1]);
          continue;
        }

        // unSubscribeTopic
        if ("unSubscribeTopic".equals(inputArgs[0])) {
          unSubscribeTopic(KnownParams.passport, KnownParams.passportId.toString(), inputArgs[1]);
          continue;
        }

        // removeTopic 285269691027713
        if ("removeTopic".equals(inputArgs[0])) {
          removeTopic(
              KnownParams.passport,
              KnownParams.passportId.toString(),
              Long.parseLong(inputArgs[1]));
          continue;
        }

        if (input.equalsIgnoreCase("quit")) {
          System.exit(0);
        }
      } catch (Exception ignored) {
        ignored.printStackTrace();
      }
    }
  }

  /**
   * 已知参数
   *
   * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
   * @version ${project.version} - 27/11/2018.
   */
  private interface KnownParams {

    String accessToken =
        "eyJhbGciOiJSUzI1NiJ9.eyJfaWQiOiJkMzk3YTYyMDgxYjY0ZWM0YmQwZTc0MTVlZWNlMDQ5NiIsImRhdCI6Ik4vQmtqTkJBelh0Y04rZDdKRExrVU5OOWNXU2JQWDlIcXc5TDdUU0gwVmlLTWNXNUp3RVd0ZXc5Rk12SVFZcGZDMG5CUUhOamVucmMyYndheHNwMk93NXVkSGM1ZllTcGd0a2FxRkV6U29Uck41S0kyaHZKRW52L1RHV0hLeDdFdTJRNEs1V3JrZTZTMjNIaUdhWXdvQ29ua3ZuSlVjWGQxNzNwV3pFbmF0bz0iLCJpYXQiOjE1NDc2MDI0NzUzNjEsImV4cCI6MTU0ODIxNjc4MzM2MSwiYXVkIjpudWxsfQ.nrXwXsGI13w1zK788GgbeZju8bsqT4vgRM3HCMyWpH65KSDs2qJPq5OfHy0OYU0zZTCSLk6zBwYQvVpL45mGuL6DrOoB-kUWSLv2nKbCm6b1CMJ3Fgkr5xn1Ljd0XxYtggMFbcODusaD4cKMiweaHw4vGiyQesohNIOj1EM-qLPaiVwNyYErHkVfxmQCsFi6f8V_z6huyskd7eohxIZwbw6H6A8tcJnHBsmUPrAegVYYZ9dwHV25lxtrSqGJ1PotcMwQNtL4lpF_UB4U3_uRO_JZKXBHwp-mboGt7A6g2GKL6pu7he_PoV-POB9jVDUkRnms2_EFlzbcIi_65UqMrA";

    String areaNo = "320500";

    String orgId = "3910249034228736";

    Long passportId = 3837142362366977L;

    String passport = "13910187666";

    String deviceId = "DEVICE-ID-2";
  }
}
