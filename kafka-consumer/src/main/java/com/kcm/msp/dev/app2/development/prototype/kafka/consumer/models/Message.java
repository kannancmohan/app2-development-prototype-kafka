package com.kcm.msp.dev.app2.development.prototype.kafka.consumer.models;

import lombok.Builder;

public record Message(String messageId, String message, String timeStamp) {
  @Builder
  public Message {}
}
