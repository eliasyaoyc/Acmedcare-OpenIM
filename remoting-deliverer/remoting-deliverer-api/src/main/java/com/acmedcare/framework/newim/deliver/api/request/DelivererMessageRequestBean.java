/*
 * Copyright 1999-2018 Acmedcare+ Holding Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.acmedcare.framework.newim.deliver.api.request;

import com.acmedcare.framework.newim.Message;
import lombok.*;

import java.io.Serializable;

/**
 * {@link DelivererMessageRequestBean}
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version ${project.version} - 2019-08-02.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DelivererMessageRequestBean implements Serializable {

  private static final long serialVersionUID = 3879854064426725649L;

  /** 时间戳 */
  @Builder.Default private long timestamp = System.currentTimeMillis();

  /** 半状态标记 */
  private boolean half;

  /** 名称空间 */
  private String namespace;

  /** 接收人通行证标识 */
  private String passportId;

  @Builder.Default private String clientType = "DEFAULT";

  /** 消息类型 */
  private Message.MessageType messageType;

  /** 消息内容 */
  private byte[] message;

}
