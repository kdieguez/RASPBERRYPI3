package com.aerolineas.dto;

import java.util.Map;

public class ConfigDTO {
    private String section;         
    private Map<String, String> data; 

    public ConfigDTO() {}
    public ConfigDTO(String section, Map<String, String> data) {
        this.section = section;
        this.data = data;
    }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public Map<String, String> getData() { return data; }
    public void setData(Map<String, String> data) { this.data = data; }
}
