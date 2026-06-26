package com.cairn.agents.impl;

import com.cairn.agents.AbstractDomainAgent;
import org.springframework.stereotype.Component;

/** WHY: Handles queries requiring data aggregation, filtering, or analytical breakdowns. */
@Component
public class AnalyticalAgent extends AbstractDomainAgent {

  @Override
  public String getDomainName() {
    return "analytical";
  }

  @Override
  protected String getSystemPrompt() {
    return "You are an analytical assistant. Your task is to breakdown data, identify trends, and summarize numerical information.";
  }
}
