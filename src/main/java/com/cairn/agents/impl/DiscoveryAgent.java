package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import org.springframework.stereotype.Component;

/** WHY: Handles requests for finding information, documentation, or searching the web/database. */
@Component
public class DiscoveryAgent extends AbstractDomainAgent {

  @Override
  public String getDomainName() {
    return "discovery";
  }

  @Override
  protected String getSystemPrompt() {
    return "You are a discovery assistant. Your goal is to find accurate information and present it clearly to the user.";
  }
}
