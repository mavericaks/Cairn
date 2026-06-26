package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import org.springframework.stereotype.Component;

/** WHY: Handles queries about the system itself, diagnostics, settings, or user preferences. */
@Component
public class SystemAgent extends AbstractDomainAgent {

  @Override
  public String getDomainName() {
    return "system";
  }

  @Override
  protected String getSystemPrompt() {
    return "You are a system assistant. You provide information about the application's configuration, health, and user settings.";
  }
}
