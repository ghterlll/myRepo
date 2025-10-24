package com.aura.starter.network;

import java.util.List;

public class FoodListResp {
    public int code;
    public String message;
    public Data data;

    public static class Data {
        public List<FoodItem> items;
    }

    public static class FoodItem {
        public long id;
        public String name;
        public String alias;
        public String category;
        public String unitName;
        public int kcalPerUnit;
        public String giLabel;
    }
}


