package com.challer.tpextraction;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.File;
import java.util.Map;

/**
 * Represent the content of an User Story
 *
 * @author Cyril Haller - cyril.haller@gmail.com
 */
public class UserStory {

    private String id;

    private String title;

    private String description;

    private String feature;

    private String creationDate;

    private String lastModificationDate;

    private String state;

    private Map<String, String> customFields;

    private File[] attachments;

    public Map<String, String> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, String> customFields) {
        this.customFields = customFields;
    }

    public File[] getAttachments() {
        return attachments;
    }

    public void setAttachments(File[] attachments) {
        this.attachments = attachments;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFeature() {
        return feature;
    }

    public void setFeature(String feature) {
        this.feature = feature;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getLastModificationDate() {
        return lastModificationDate;
    }

    public void setLastModificationDate(String lastModificationDate) {
        this.lastModificationDate = lastModificationDate;
    }

    public String getTitleHtmlSafe() {
        String temp = title.replace("\\", "");
        temp = temp.replace("\"", "");
        return StringEscapeUtils.escapeHtml4(temp);
    }
}
