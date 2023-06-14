package com.revealprecision.revealserver.service;

import com.revealprecision.revealserver.messaging.message.Message;

public interface PublisherService {

  void send(String topic, Message message);

  void send(String topic, String key, Message message);
}
