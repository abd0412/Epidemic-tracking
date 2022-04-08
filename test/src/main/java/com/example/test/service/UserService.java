package com.example.test.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * @author Cyan
 * @Description
 * @create 2020-10-07 19:32
 */
public interface UserService {
    JSONObject errorReturn(String message, int code);
    JSONArray errorArrayReturn(String message, int code);
    JSONObject generateRtn(JSONArray numberChangeArray, JSONArray userId, String message, int code, int[] array, HashMap<String,Integer> hm, HashMap<String,Integer> hn, HashMap<String,Integer> hArea, HashMap<String,Integer> hInfectTime);
    JSONObject latLngObtain(List<String> json, String type);
}

