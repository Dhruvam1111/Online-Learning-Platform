package com.olp.dto;

public class TakedownReplaceRequest {

    private String sourceAttribution;
    private String notes;

    public TakedownReplaceRequest() {
    }

    public TakedownReplaceRequest(String sourceAttribution, String notes) {
        this.sourceAttribution = sourceAttribution;
        this.notes = notes;
    }

    public String getSourceAttribution() {
        return sourceAttribution;
    }

    public void setSourceAttribution(String sourceAttribution) {
        this.sourceAttribution = sourceAttribution;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

