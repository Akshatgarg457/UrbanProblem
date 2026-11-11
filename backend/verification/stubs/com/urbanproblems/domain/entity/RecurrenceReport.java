package com.urbanproblems.domain.entity;
public class RecurrenceReport { public Complaint getCurrentComplaint(){return null;} public Integer getHistoricalIssueCount(){return 0;} public static Builder builder(){return new Builder();}
  public static class Builder { public Builder currentComplaint(Complaint c){return this;} public Builder category(Object o){return this;} public Builder location(Object o){return this;}
    public Builder latitude(Object o){return this;} public Builder longitude(Object o){return this;} public Builder proximityRadiusKm(Object o){return this;}
    public Builder historicalIssueCount(Object o){return this;} public Builder relatedComplaintIds(Object o){return this;} public Builder analysisNotes(Object o){return this;}
    public RecurrenceReport build(){return new RecurrenceReport();} } }
