package com.aura.starter.network.models;

public class MediaItem {
    private String url;
    private Integer width;
    private Integer height;
    private Integer sortOrder;
    private String objectKey;
    private String mimeType;
    private Integer bytes;
    private String checksum;
    private String blurhash;

    public MediaItem(String url, Integer width, Integer height) {
        this.url = url;
        this.width = width;
        this.height = height;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }
    
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    
    public String getObjectKey() { return objectKey; }
    public void setObjectKey(String objectKey) { this.objectKey = objectKey; }
    
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    
    public Integer getBytes() { return bytes; }
    public void setBytes(Integer bytes) { this.bytes = bytes; }
    
    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    
    public String getBlurhash() { return blurhash; }
    public void setBlurhash(String blurhash) { this.blurhash = blurhash; }
}
