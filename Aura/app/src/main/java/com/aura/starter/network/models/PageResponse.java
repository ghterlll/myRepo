package com.aura.starter.network.models;

import java.util.List;

public class PageResponse<T> {
    private List<T> items;
    private Long nextCursor;
    private Boolean hasMore;

    public List<T> getItems() { return items; }
    public void setItems(List<T> items) { this.items = items; }
    
    public Long getNextCursor() { return nextCursor; }
    public void setNextCursor(Long nextCursor) { this.nextCursor = nextCursor; }
    
    public Boolean getHasMore() { return hasMore; }
    public void setHasMore(Boolean hasMore) { this.hasMore = hasMore; }
}
