/*
 * Copyright 1999-2018 Acmedcare+ Holding Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package com.acmedcare.framework.newim.deliver.services;

import com.acmedcare.framework.newim.Message;
import com.acmedcare.framework.newim.storage.api.DelivererRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * {@link DelivererService}
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a>
 * @version ${project.version} - 2019-08-08.
 */
@Service
public class DelivererService {

  private static final Logger log = LoggerFactory.getLogger(DelivererService.class);

  private final DelivererRepository delivererRepository;

  public DelivererService(DelivererRepository delivererRepository) {
    this.delivererRepository = delivererRepository;
  }

  // ====== Post Deliverer Message Method ======

  /**
   * Post Deliverer Message
   * @param half half message flag
   * @param namespace message namespace
   * @param passportId password id
   * @param messageType message type
   * @param message message payload
   */
  public void postDelivererMessage(boolean half, String namespace, String passportId, Message.MessageType messageType, byte[] message) {

    // todo

  }
}
