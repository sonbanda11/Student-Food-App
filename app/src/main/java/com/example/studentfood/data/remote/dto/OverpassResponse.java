package com.example.studentfood.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO cho Overpass API response
 */
public class OverpassResponse {
    
    @SerializedName("version")
    private double version;
    
    @SerializedName("generator")
    private String generator;
    
    @SerializedName("osm3s")
    private Osm3s osm3s;
    
    @SerializedName("elements")
    private List<OverpassElement> elements;
    
    // Getters and setters
    public double getVersion() {
        return version;
    }
    
    public void setVersion(double version) {
        this.version = version;
    }
    
    public String getGenerator() {
        return generator;
    }
    
    public void setGenerator(String generator) {
        this.generator = generator;
    }
    
    public Osm3s getOsm3s() {
        return osm3s;
    }
    
    public void setOsm3s(Osm3s osm3s) {
        this.osm3s = osm3s;
    }
    
    public List<OverpassElement> getElements() {
        return elements;
    }
    
    public void setElements(List<OverpassElement> elements) {
        this.elements = elements;
    }
    
    /**
     * Inner class for OSM3S metadata
     */
    public static class Osm3s {
        @SerializedName("timestamp_osm_base")
        private String timestampOsmBase;
        
        @SerializedName("copyright")
        private String copyright;
        
        public String getTimestampOsmBase() {
            return timestampOsmBase;
        }
        
        public void setTimestampOsmBase(String timestampOsmBase) {
            this.timestampOsmBase = timestampOsmBase;
        }
        
        public String getCopyright() {
            return copyright;
        }
        
        public void setCopyright(String copyright) {
            this.copyright = copyright;
        }
    }
}
