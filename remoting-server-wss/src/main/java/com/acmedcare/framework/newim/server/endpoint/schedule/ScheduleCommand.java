package com.acmedcare.framework.newim.server.endpoint.schedule;

import com.acmedcare.framework.newim.Message;
import com.acmedcare.framework.newim.protocol.Command.WebSocketClusterCommand;
import com.acmedcare.framework.newim.wss.WssPayload.WssRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Schedule Command
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version ${project.version} - 19/11/2018.
 */
@Getter
public enum ScheduleCommand {

  /** 授权 */
  AUTH(0x30000, AuthRequest.class),
  PULL_ONLINE_SUB_ORGS(0x31001, PullOnlineSubOrgsRequest.class),

  WS_REGISTER(WebSocketClusterCommand.WS_REGISTER, RegisterRequest.class),
  WS_SHUTDOWN(WebSocketClusterCommand.WS_SHUTDOWN, DefaultRequest.class),
  WS_PUSH_MESSAGE(WebSocketClusterCommand.WS_PUSH_MESSAGE, PushMessageRequest.class),
  WS_PULL_MESSAGE(WebSocketClusterCommand.WS_PULL_MESSAGE, PullMessageRequest.class),
  WS_HEARTBEAT(WebSocketClusterCommand.WS_HEARTBEAT, DefaultRequest.class);

  private static final String BIZ_CODE = "bizCode";
  int bizCode;
  Class<?> requestClass;

  ScheduleCommand(int bizCode, Class<?> requestClass) {
    this.bizCode = bizCode;
    this.requestClass = requestClass;
  }

  public static ScheduleCommand parseCommand(String message) {
    JSONObject temp = JSONObject.parseObject(message);
    if (temp.containsKey(BIZ_CODE)) {
      return parseCommand(temp.getInteger(BIZ_CODE));
    }
    throw new IllegalArgumentException("[WSS] 无效的业务请求,为携带协议编码");
  }

  public static ScheduleCommand parseCommand(int bizCode) {
    for (ScheduleCommand value : ScheduleCommand.values()) {
      if (value.getBizCode() == bizCode) {
        return value;
      }
    }
    throw new IllegalArgumentException("[WSS] 无效的业务参数编码:" + bizCode);
  }

  public WssRequest parseRequest(String message) {
    return (WssRequest) JSON.parseObject(message, getRequestClass());
  }

  @Getter
  @Setter
  public static class PullOnlineSubOrgsRequest extends DefaultRequest {}

  @Getter
  @Setter
  public static class AuthRequest extends WssRequest {
    private String accessToken;
    private String wssClientType;
  }

  @Getter
  @Setter
  public static class RegisterRequest extends DefaultRequest {

    /** 机构名称 */
    private String orgName;
    /** 父机构编号 */
    private String parentOrgId;
  }

  @Getter
  @Setter
  public static class DefaultRequest extends WssRequest {

    /** 通行证编号 */
    private String passportId;

    private String areaNo;

    private String orgId;
  }

  /** 拉取消息列表请求对象 */
  @Getter
  @Setter
  public static class PullMessageRequest extends DefaultRequest {

    /** 发送者 */
    private String sender;

    /** 消息类型 */
    private Message.MessageType type = Message.MessageType.GROUP;

    /**
     * 最新的消息 ID
     *
     * <pre></pre>
     */
    private long leastMessageId;

    private long limit = 10;
  }

  /** 发送信息请求对象 */
  @Getter
  @Setter
  public static class PushMessageRequest extends DefaultRequest {
    /** 发送者 */
    private String sender;

    /** 消息内容 */
    private String message = "";

    /** 接受者或者是群 */
    private String receiver;

    /** 消息类型 */
    private Message.MessageType type = Message.MessageType.GROUP;

    /**
     * 消息基础类型
     *
     * @since 2.2.3
     */
    private Message.InnerType innerType = Message.InnerType.NORMAL;

    /**
     * 媒体消息载体
     *
     * @since 2.2.3
     */
    private Payload payload;

    /**
     * Message Payload
     *
     * @since 2.2.3
     */
    @Getter
    @Setter
    public static class Payload implements Serializable {

      /** 媒体文件的编号 */
      private String mediaPayloadKey;

      /** 媒体文件访问连接 */
      private String mediaPayloadAccessUrl;

      /** 文件名称 */
      private String mediaFileName;

      /** 文件后缀 */
      private String mediaFileSuffix;
    }
  }

  @Getter
  @Setter
  @Deprecated
  public static class PushOrderRequest extends DefaultRequest {

    /** 订单详情信息 */
    private String orderDetail;
    /** 接受分站标识 */
    private String subOrgId;
  }

  @Getter
  @Setter
  @NoArgsConstructor
  public static class CustomMediaPayloadWithExt extends MediaPayload {

    private static final long serialVersionUID = -4024513100445536730L;

    private byte[] body;

    public CustomMediaPayloadWithExt(
        String mediaPayloadKey,
        String mediaPayloadAccessUrl,
        String mediaFileName,
        String mediaFileSuffix,
        byte[] body) {
      super(mediaPayloadKey, mediaPayloadAccessUrl, mediaFileName, mediaFileSuffix);
      this.body = body;
    }
  }

  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MediaPayload implements Serializable {

    private static final long serialVersionUID = -1496285586690313202L;

    private String mediaPayloadKey;
    /** 媒体文件访问连接 */
    private String mediaPayloadAccessUrl;

    private String mediaFileName;

    private String mediaFileSuffix;
  }
}
