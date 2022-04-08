package com.example.test.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.test.mapper.UserMapper;
import com.example.test.service.UserService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.HashMap;
import java.util.List;

/**
 * @author Cyan
 * @Description
 * @create 2020-10-07 20:42
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserService userService;
    @Autowired(required=false)
    //此处为全局变量
    private UserMapper userMapper;
    //一般的报错处理
    @Override
    public JSONObject errorReturn(String message, int code){
        JSONObject jsonObject = new JSONObject ();
        jsonObject.put("Message", message);
        jsonObject.put("Code", code);
        return jsonObject;
    }

    @Override
    public JSONArray errorArrayReturn(String message, int code){
        JSONArray jsonArray = new JSONArray ();
        JSONObject jsonObject = new JSONObject ();
        jsonObject.put("Message", message);
        jsonObject.put("Code", code);
        jsonArray.add ( jsonObject );
        return jsonArray;
    }
    @Override
    public JSONObject generateRtn(JSONArray numberChangeArray, JSONArray userId, String message, int code, int[] array, HashMap<String,Integer> hm, HashMap<String,Integer> hn, HashMap<String,Integer> hArea, HashMap<String,Integer> hInfectTime) {
        int sum = 0;
        JSONArray addressArray = new JSONArray();
        JSONArray provinceArray = new JSONArray();
        JSONArray areaArray = new JSONArray();
        JSONObject jsonObject = new JSONObject ();
        jsonObject.put("Message", message);
        jsonObject.put("Code", code);
        jsonObject.put("AllUserId", userId);
        //将每天感染的人数信息放在JSON数组里
        for (String key : hInfectTime.keySet ()){
            JSONObject infectObject = new JSONObject ();
            infectObject.put ( "DateOfInfection",  key);
            infectObject.put ( "InfectNumber",  hInfectTime.get(key));
            numberChangeArray.add ( infectObject );
        }
        //得出每一省份来源地可能被感染的人数
        for(String key: hn.keySet()){
            JSONObject provinceObject = new JSONObject ();
            //hn.keySet()代表所有键的集合,进行格式化输出
            provinceObject.put("Province", key);
            provinceObject.put("Number", hn.get(key));
            provinceArray.add(provinceObject);
        }
        //得出每一具体来源地可能被感染的人数
        for(String key: hArea.keySet()){
            JSONObject areaObject = new JSONObject ();
            areaObject.put("Area", key);
            areaObject.put("Number", hArea.get(key));
            for (int j = 0; j < userId.size(); j++) {
                JSONObject userIdObject = userId.getJSONObject(j);
                String area = userIdObject.getString("Area");
                if(key == area){
                    String lat= userIdObject.getString("AreaLat");
                    areaObject.put("AreaLat", lat);
                    String lng= userIdObject.getString("AreaLng");
                    areaObject.put("AreaLng", lng);
                    break;
                }
            }
            areaArray.add(areaObject);
        }
        //得出每一景区可能被感染的人数
        for(String key: hm.keySet()){
            JSONObject addressObject = new JSONObject ();
            addressObject.put("Address", key);
            addressObject.put("Number", hm.get(key));
            for (int j = 0; j < userId.size(); j++) {
                JSONObject userIdObject = userId.getJSONObject(j);
                String address= userIdObject.getString("Address");
                if(key == address){
                    String lat= userIdObject.getString("Lat");
                    addressObject.put("Lat", lat);
                    String lng= userIdObject.getString("Lng");
                    addressObject.put("Lng", lng);
                    break;
                }
            }
            addressArray.add(addressObject);
        }
        //得出每一级别可能被感染的人数
        for(int k=0;k<array.length;k++){
            if(array[k] != 0){
                jsonObject.put("numberOfLevel_" + k, array[k]);
                sum += array[k]; //计算出被感染的总人数
            }
        }
        jsonObject.put("Total", sum);
        jsonObject.put ( "AddressNumber", addressArray );
        jsonObject.put ( "ProvinceNumber", provinceArray );
        jsonObject.put ( "AreaNumber", areaArray );
        jsonObject.put("NumberChangeArray", numberChangeArray);
        System.out.println ("endJsonObject==>" + jsonObject);

        return jsonObject;
    }

    @Override
    public JSONObject latLngObtain(List<String> json, String type){
        System.out.println ("开始查询经纬度");
        try {
            String key = "7WUBZ-2EJY3-VDT3W-YDXBV-HQFK6-ZHB2J"; //调用下面接口的秘钥
            for (String specificAddress : json) {
                String Name = null;
                String address = null;
                JSONObject aSpecificAddressObject = JSONObject.parseObject ( specificAddress );
                if(type.equals ("scenery")) {
                    Name = aSpecificAddressObject.getString ( "SceneryName" );
                    address = aSpecificAddressObject.getString ( "SpecificAddress" );
                } else if(type.equals ("area")){
                    Name = aSpecificAddressObject.getString ( "Codes" );
                    address = aSpecificAddressObject.getString ( "Area" );
                }
                // 创建Httpclient对象
                CloseableHttpClient latLngHttpclient = HttpClients.createDefault ();
                // 定义请求的参数
                System.out.println ( "address==>" + address );
                URI uri = new URIBuilder ( "https://apis.map.qq.com/ws/geocoder/v1/" ).setParameter ( "address", address ).addParameter ( "key", key ).build ();
                // 创建http GET请求
                HttpGet httpGet = new HttpGet ( uri );
                //response 对象
                CloseableHttpResponse response = null;
                try {
                    // 执行http get请求
                    response = latLngHttpclient.execute ( httpGet );
                    // 判断返回状态是否为200
                    if (response.getStatusLine ().getStatusCode () == 200) {
                        String content = EntityUtils.toString ( response.getEntity (), "UTF-8" );
                        JSONObject responseObject = JSONObject.parseObject ( content );
                        System.out.println ( "responseObject==>" + responseObject );
                        String lat = responseObject.getJSONObject ( "result" ).getJSONObject ( "location" ).getString ( "lat" );
                        String lng = responseObject.getJSONObject ( "result" ).getJSONObject ( "location" ).getString ( "lng" );
                        if(type.equals ("scenery")) {
                            userMapper.updateSceneryLatLng ( lat, lng, Name );//更新插入景区的经纬度
                        }else if(type.equals ("area")){
                            userMapper.updateAreaLatLng ( lat, lng, Name );//更新插入全国所有地区的经纬度
                        }
                    }
                } finally {
                    if (response != null) {
                        response.close ();
                    }
                    latLngHttpclient.close ();
                }
            }
        } catch (Exception e) {
            e.printStackTrace ();
            return userService.errorReturn ( "数据库执行有异常", 1004 );
        }
        JSONObject returnObject = new JSONObject ();
        returnObject.put ( "Message" , "OBTAIN SUCCESS");
        returnObject.put ( "Code" , "200");
        return returnObject;
    }
}
