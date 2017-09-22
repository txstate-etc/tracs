package org.sakaiproject.gradebookng.tool.model;

import java.io.Serializable;

public class RescaleAnswer implements Serializable {
    private String answer;

    public RescaleAnswer(String answer) {
        this.answer = answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getAnswer() {
        return this.answer;
    }
}