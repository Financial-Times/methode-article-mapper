package com.ft.methodearticlemapper.transformation;

import com.fasterxml.jackson.annotation.JsonValue;
import com.ft.methodearticlemapper.exception.InvalidSubscriptionLevelException;

public enum SubscriptionLevel {
  FOLLOW_USUAL_RULES(0),
  SHOWCASE(2),
  PREMIUM(3);

  private int subscriptionLevel;

  SubscriptionLevel(int subscriptionLevel) {
    this.subscriptionLevel = subscriptionLevel;
  }

  public static SubscriptionLevel fromInt(int subscriptionLevel) {
    for (SubscriptionLevel level : SubscriptionLevel.values()) {
      if (subscriptionLevel == level.getSubscriptionLevel()) {
        return level;
      }
    }

    throw new InvalidSubscriptionLevelException(
        String.format("Cannot return subscription level for value %d", subscriptionLevel));
  }

  @JsonValue
  public int getSubscriptionLevel() {
    return subscriptionLevel;
  }
}
