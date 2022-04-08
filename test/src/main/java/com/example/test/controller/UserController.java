package com.example.test.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.test.bean.UserBean;
import com.example.test.mapper.UserMapper;
import com.example.test.service.UserService;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * author Cyan
 * Description
 * create 2020-10-07 20:38
 */
@CrossOrigin
@RestController
@RequestMapping(value = "api")
@ResponseBody
public class UserController {
    //将Service注入Web层
    @Autowired
    UserService userService;
    @Autowired(required=false)
    //此处为全局变量
    private UserMapper userMapper;
    JSONArray outJsonArray = new JSONArray();
    JSONArray numberChangeArray = new JSONArray ();
    int[] array=new int[100];
    HashMap<String,Integer> hm = new HashMap();
    HashMap<String,Integer> hn = new HashMap();
    HashMap<String,Integer> hArea = new HashMap();
    HashMap<String,Integer> hInfectTime = new HashMap();
    /**
     * apiDefine Epidemic situation tracking 疫情跟踪
     */
    /**
     * api{get} api/scenery
     * apiVersion 0.0.1
     * apiDescription  游客在某一个景区游览之后被确诊为新冠肺炎患者，通过这个游客来筛查可能被感染的其他人员从而达到疫情跟踪的目的。
     * apiParam(入参) {String} UserId 游客ID
     * apiParamExample {json} 入参样例：
     *  {
     *      "UserId": "1302121067548119040"
     *  }
     *
     * apiSuccess(Success 200) {String} Message 错误消息描述
     * apiSuccess(Success 200) {String} Code 接口返回编码；200-成功；1001-输入JSON解析出错；1004-数据库执行有异常；
     * apiSuccess(Success 200) {List} AllUserId 感染者集合 （若失败，此集合为空，不显示。）
     * apiSuccess(Success 200) {String} UserId 被感染的游客
     * apiSuccess(Success 200) {Int} Level 潜在被感染者级别，数字越大被感染的可能性越小。
     *
     * apiSuccessExample {json} 返回成功样例：
     * {
     * 	"Message": "SUCCESS",
     * 	"Code": "00001",
     *  "AllUserId":
     *      [
     *          {
     *              "UserId": "2377898789024830",
     *              "Level": 1
     *          },
     *          {
     *              "UserId": "2373688789024830",
     *              "Level": 2
     *          },
     *          {
     *              "UserId": "2377898780634830",
     *              "Level": 3
     *          }
     *      ]
     * }
     *
     * apiErrorExample {json} 返回失败样例：
     * {
     * 	"Message": "数据库执行有异常",
     * 	"Code": "1004"
     * }
     */
    @CrossOrigin
    @RequestMapping(value = "scenery", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public JSONObject epidemicQuery(@RequestBody String json) throws Exception {
        System.out.println ("stringJson==>" + json);
        JSONObject jsonObject;
        try {
            jsonObject = JSONObject.parseObject(json);
        } catch(Exception e){
            e.printStackTrace();
            return userService.errorReturn( "输入JSON解析出错", 1001);
        }
        try {
            search ( jsonObject );
        } catch(Exception e) {
            e.printStackTrace();
            return userService.errorReturn( "数据库执行有异常", 1004);
        }
        return userService.generateRtn(numberChangeArray, outJsonArray, "SUCCESS", 200, array, hm, hn, hArea, hInfectTime);
    }
    int i = 0;
    //重要处理方法
    public void search(JSONObject jsonObject) {
        String userId = jsonObject.getString("UserId");
        while(userId.length() != 0){
            UserBean userBean = new UserBean();
            userBean.setInput(jsonObject.toJSONString());//准备好的数据
            String users = userMapper.getInfo(userBean);  //接收从数据库中传来的String类型的信息
            //System.out.println ("dataUsers==>" + users);
            JSONObject usersJsonObject = JSONObject.parseObject(users); // 字符串转json对象
            String data = usersJsonObject.getString("lines"); //获取lines内容
            JSONArray strings = JSONArray.parseArray (data); //并将lines内容取出转为json数组
            if (strings.size () == 0){
                return;
            }
            i++;
            for (int j = 0; j < strings.size(); j++) {     //遍历json数组内容， 单个单个的信息从这里取！！！
                JSONObject oneObject = strings.getJSONObject(j);
                //System.out.println(oneObject.getString("UserId"));
                String m = oneObject.getString("Address");
                String n = oneObject.getString("Province");
                String area = oneObject.getString("Area");
                Date inTime = oneObject.getDate ("InTime");
                //得到每天的感染人数
                SimpleDateFormat sdfDay = new SimpleDateFormat ( "yyyy-MM-dd" );
                String str = sdfDay.format(inTime);
                if(!hInfectTime.containsKey(str)){
                    hInfectTime.put(str,1);
                }else{
                    hInfectTime.put(str, hInfectTime.get(str)+1);
                }
                //计算每一个来源地区（省份）的可能被感染者人数
                //containsKey(n),当c不存在于hn中
                if(!hn.containsKey(n)){
                    hn.put(n,1);
                }else{
                    //否则获得n的值并且加1
                    hn.put(n, hn.get(n)+1);
                }
                //计算每一个具体来源地区的可能被感染人数 hArea
                //containsKey(n),当c不存在于hn中
                if(!hArea.containsKey(area)){
                    hArea.put(area,1);
                }else{
                    //否则获得n的值并且加1
                    hArea.put(area, hArea.get(area)+1);
                }
                //计算每一景区可能被感染的人数
                //containsKey(m),当c不存在于hm中
                if(!hm.containsKey(m)){
                    hm.put(m,1);
                }else{
                    //否则获得m的值并且加1
                    hm.put(m, hm.get(m)+1);
                }
                //计算每一级别的人数
                array[i]++;
                oneObject.put("Level", i);

                outJsonArray.add(oneObject); //把找出来的加进来
                search(strings.getJSONObject(j));
            }
            i =  i - 1;
        }
    }
    //对接腾讯云图的区域地图接口
    @CrossOrigin
    @RequestMapping(value = "address", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public JSONArray addressNumberQuery(@RequestBody String json) throws Exception {
        JSONObject AllJsonObject= epidemicQuery ( json );
        JSONArray addressNumberArray = AllJsonObject.getJSONArray ( "AddressNumber" );
        return addressNumberArray;
    }
    //对接腾讯云图全国省份源地地图的接口
    @CrossOrigin
    @RequestMapping(value = "province", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public JSONArray provinceNumberQuery(@RequestBody String json) throws Exception {
        JSONObject AllJsonObject= epidemicQuery ( json );
        JSONArray provinceNumberArray = AllJsonObject.getJSONArray ( "ProvinceNumber" );
        return provinceNumberArray;
    }
    //潜在被感染者的具体来源地
    @CrossOrigin
    @RequestMapping(value = "area", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public JSONArray areaNumberQuery(@RequestBody String json) throws Exception {
        JSONObject AllJsonObject= epidemicQuery ( json );
        JSONArray areaNumberArray = AllJsonObject.getJSONArray ( "AreaNumber" );
        return areaNumberArray;
    }
    //对接腾讯云图的潜在感染接口
    @CrossOrigin
    @RequestMapping(value = "total", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public JSONArray totalQuery(@RequestBody String json) throws Exception {
        JSONObject AllJsonObject= epidemicQuery ( json );
        String total = AllJsonObject.getString ( "Total" );
        JSONObject totalObject = new JSONObject();
        totalObject.put("Total", total);
        JSONArray jsonArray = new JSONArray ();
        jsonArray.add ( totalObject );
        return jsonArray;
    }
    //对接腾讯云图的Level 1接口
    @CrossOrigin
    @RequestMapping(value = "oneLevel", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public JSONArray oneLevelQuery(@RequestBody String json) throws Exception {
        JSONObject AllJsonObject= epidemicQuery ( json );
        String oneLevel = AllJsonObject.getString ( "numberOfLevel_1" );
        JSONObject oneLevelObject = new JSONObject();
        oneLevelObject.put("numberOfLevel_1", oneLevel);
        JSONArray jsonArray = new JSONArray ();
        jsonArray.add ( oneLevelObject );
        return jsonArray;
    }
    //对接腾讯云图的Level 2接口
    @CrossOrigin
    @RequestMapping(value = "twoLevel", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public JSONArray twoLevelQuery(@RequestBody String json) throws Exception {
        JSONObject AllJsonObject= epidemicQuery ( json );
        String twoLevel = AllJsonObject.getString ( "numberOfLevel_2" );
        JSONObject twoLevelObject = new JSONObject();
        twoLevelObject.put("numberOfLevel_2", twoLevel);
        JSONArray jsonArray = new JSONArray ();
        jsonArray.add ( twoLevelObject );
        return jsonArray;
    }
    //对接腾讯云图的Level 3接口
    @CrossOrigin
    @RequestMapping(value = "threeLevel", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public JSONArray threeLevelQuery(@RequestBody String json) throws Exception {
        JSONObject AllJsonObject= epidemicQuery ( json );
        String threeLevel = AllJsonObject.getString ( "numberOfLevel_3" );
        JSONObject threeLevelObject = new JSONObject();
        threeLevelObject.put("numberOfLevel_3", threeLevel);
        JSONArray jsonArray = new JSONArray ();
        jsonArray.add ( threeLevelObject );
        return jsonArray;
    }
    //对接腾讯云图的游客信息轮播接口
    @CrossOrigin
    @RequestMapping(value = "infected", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public JSONArray infectedInfoQuery(@RequestBody String json) throws Exception {
        JSONObject AllJsonObject= epidemicQuery ( json );
        JSONArray infectedInfoArray = AllJsonObject.getJSONArray ( "AllUserId" );
        return infectedInfoArray;
    }
    //对潜在被感染游客分级展示：一级
    @CrossOrigin
    @RequestMapping(value = "infectedOneLevel", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public JSONArray infectedOneLevelQuery(@RequestBody String json) throws Exception {
        JSONObject AllJsonObject1= epidemicQuery ( json );
        JSONArray infectedInfoArray = AllJsonObject1.getJSONArray ( "AllUserId" );
        JSONArray oneLevelArray = new JSONArray ();
        for(int i = 0; i < infectedInfoArray.size (); i++){
            JSONObject aInfectedObject = infectedInfoArray.getJSONObject ( i );
            int level = aInfectedObject.getInteger ( "Level" );
            if(level == 1){
                oneLevelArray.add ( aInfectedObject );
            }
        }
        return oneLevelArray;
    }
    //对潜在被感染游客分级展示：二级
    @CrossOrigin
    @RequestMapping(value = "infectedTwoLevel", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public JSONArray infectedTwoLevelQuery(@RequestBody String json) throws Exception {
        JSONObject AllJsonObject2= epidemicQuery ( json );
        JSONArray infectedInfoArray = AllJsonObject2.getJSONArray ( "AllUserId" );
        JSONArray twoLevelArray = new JSONArray ();
        for(int i = 0; i < infectedInfoArray.size (); i++){
            JSONObject aInfectedObject = infectedInfoArray.getJSONObject ( i );
            int level = aInfectedObject.getInteger ( "Level" );
            if(level == 2){
                twoLevelArray.add ( aInfectedObject );
            }
        }
        return twoLevelArray;
    }
    //对潜在被感染游客分级展示：三级
    @CrossOrigin
    @RequestMapping(value = "infectedThreeLevel", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public JSONArray infectedThereLevelQuery(@RequestBody String json) throws Exception {
        JSONObject AllJsonObject3= epidemicQuery ( json );
        JSONArray infectedInfoArray = AllJsonObject3.getJSONArray ( "AllUserId" );
        JSONArray threeLevelArray = new JSONArray ();
        for(int i = 0; i < infectedInfoArray.size (); i++){
            JSONObject aInfectedObject = infectedInfoArray.getJSONObject ( i );
            int level = aInfectedObject.getInteger ( "Level" );
            if(level == 3){
                threeLevelArray.add ( aInfectedObject );
            }
        }
        return threeLevelArray;
    }

    //对接腾讯云图的感染人数折线图接口
    @CrossOrigin
    @RequestMapping(value = "numberChange", method = RequestMethod.POST, produces = {"application/json;charset=UTF-8"})
    @ResponseBody
    public JSONArray numberChangeQuery(@RequestBody String json) throws Exception {
        JSONObject AllJsonObject= epidemicQuery ( json );
        JSONArray everydayNumberChangeArray0 = AllJsonObject.getJSONArray ( "NumberChangeArray" ); //每一天新增加的人数
        System.out.println ("everydayNumberChangeArray0==>" + everydayNumberChangeArray0);
        JSONArray NumberChangeArray = new JSONArray ();
        HashMap<String,Integer> hAllInfect = new HashMap();
        HashMap<String,Integer> hAllInfects = new HashMap();
        for (int i = 0; i < everydayNumberChangeArray0.size(); i++) {
            JSONObject oneObject = everydayNumberChangeArray0.getJSONObject ( i );
            oneObject.put ( "S", "DayAdd");
            NumberChangeArray.add ( oneObject );
            String dateOfInfection = oneObject.getString ( "DateOfInfection" );
            int infectNumber = oneObject.getInteger ( "InfectNumber" );
            hAllInfect.put ( dateOfInfection, infectNumber );
        }
        //实现总量的变化
        for(String key1: hAllInfect.keySet()){
            int number = 0;
            SimpleDateFormat sdfDay = new SimpleDateFormat ( "yyyy-MM-dd" );
            Date key1Date = sdfDay.parse(key1);
            for(String key2: hAllInfect.keySet()){
                Date key2Date = sdfDay.parse(key2);
                if(key1Date.getTime () >= key2Date.getTime ()){
                    number += hAllInfect.get ( key2 );
                }
            }
            hAllInfects.put ( key1, number);
        }
        for(String key: hAllInfects.keySet()){
            JSONObject infectObject = new JSONObject ();
            infectObject.put ( "DateOfInfection",  key);
            infectObject.put ( "InfectNumber",  hAllInfects.get(key));
            infectObject.put ( "S", "AllAdd");
            NumberChangeArray.add ( infectObject );
        }
        return NumberChangeArray;
    }

    //查询被感染游客的活动路径
    @CrossOrigin
    @RequestMapping(value = "activity", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public JSONArray activityQuery(@RequestBody String json) throws Exception {
        System.out.println ("userStringJson==>" + json);
        JSONArray jsonArray = new JSONArray();
        try {
            jsonArray = JSONArray.parseArray (json);
        } catch(Exception e){
            e.printStackTrace();
        }
        JSONArray activityArray = new JSONArray();
        try {
            for (int i = 0; i < jsonArray.size(); i++) {
                JSONObject oneObject = jsonArray.getJSONObject(i);
                //System.out.println("userOneObject==>" + oneObject);
                UserBean userBean = new UserBean();
                userBean.setInput(oneObject.toJSONString());//准备好的数据
                List<String> strings = userMapper.getActivityInfo(userBean);  //接收从数据库中传来的String类型的信息
                //System.out.println("activityStrings==>" + strings);
                for (String s : strings) {
                    JSONObject jsonObject = JSONObject.parseObject(s);
                    activityArray.add(jsonObject);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return activityArray;
    }

    //确定或更新所有游客的出口时间
    @CrossOrigin
    @RequestMapping(value = "outTime", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public JSONObject outTimeUpdate() throws Exception{
        try {
            List<String> userIdStrings = userMapper.getUserIdInfo ();  //接收从数据库中传来的出口时间为空的userId
            //System.out.println ( "userIdStrings==>" + userIdStrings );
            for (String userId : userIdStrings) {
                JSONObject sObject = new JSONObject ();
                sObject.put ( "UserId", userId );
                UserBean userBean = new UserBean ();
                userBean.setInput ( sObject.toJSONString () );//准备好的数据
                List<String> oneUserActivityStrings = userMapper.getAllActivityInfo ( userBean );  //接收从数据库中传来的单个游客的活动路径信息
                //System.out.println ( "oneUserActivityStrings==>" + oneUserActivityStrings );
                for (int i = 0; i < oneUserActivityStrings.size () - 1; i++) {
                    Date outTime;
                    String endOutTime;
                    JSONObject fromUserActivityObject = JSONObject.parseObject ( oneUserActivityStrings.get ( i ) );
                    JSONObject toUserActivityObject = JSONObject.parseObject ( oneUserActivityStrings.get ( i + 1 ) );
                    String UserId = fromUserActivityObject.getString ( "UserId" );
                    String SceneryName = fromUserActivityObject.getString ( "SceneryName" );
                    Date fromInTime = fromUserActivityObject.getDate ( "AllInTime" );
                    Date toInTime = toUserActivityObject.getDate ( "AllInTime" );
                    SimpleDateFormat sdfDays = new SimpleDateFormat ( "yyyy-MM-dd" );
                    Date fromInTimeDays = sdfDays.parse(sdfDays.format ( fromInTime )); //将时间格式化为年月日字符串并转为日期
                    Date toInTimeDays = sdfDays.parse(sdfDays.format ( toInTime ));
                    //System.out.println ( "fromInTimeDays==>" + fromInTimeDays );
                    //System.out.println ( "toInTimeDays==>" + toInTimeDays );
                    int days = (int) ((toInTimeDays.getTime () - fromInTimeDays.getTime ()) / (24 * 3600 * 1000));
                    if (days >= 1) {
                        SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss" ); //格式化输出日期
                        long time = 14400 * 1000; //4h
                        outTime = new Date ( fromInTime.getTime () + time ); //出口时间
                        //System.out.println ( "outTime==>" + sdf.format ( outTime ) );
                        endOutTime = sdf.format ( outTime );
                        userMapper.updateOutTime ( UserId, SceneryName, endOutTime ); //if更新数据库中的出口时间
                    } else {
                        String fromLat = fromUserActivityObject.getString ( "Lat" );
                        String fromLng = fromUserActivityObject.getString ( "Lng" );
                        String toLat = toUserActivityObject.getString ( "Lat" );
                        String toLng = toUserActivityObject.getString ( "Lng" );
                        String key = "7WUBZ-2EJY3-VDT3W-YDXBV-HQFK6-ZHB2J"; //调用下面接口的秘钥
                        // 创建Httpclient对象
                        CloseableHttpClient httpclient = HttpClients.createDefault ();
                        // 定义请求的参数
                        URI uri = new URIBuilder ( "https://apis.map.qq.com/ws/distance/v1/?mode=driving" ).setParameter ( "from", fromLat + "," + fromLng ).addParameter ( "to", toLat + "," + toLng ).addParameter ( "key", key ).build ();
                        //System.out.println ( "interfaceUri==>" + uri );
                        // 创建http GET请求
                        HttpGet httpGet = new HttpGet ( uri );
                        //response 对象
                        CloseableHttpResponse response = null;
                        try {
                            // 执行http get请求
                            response = httpclient.execute ( httpGet );
                            // 判断返回状态是否为200
                            if (response.getStatusLine ().getStatusCode () == 200) {
                                String content = EntityUtils.toString ( response.getEntity (), "UTF-8" );
                                JSONObject responseObject = JSONObject.parseObject ( content );
                                //System.out.println ( "responseObject==>" + responseObject );
                                int second = responseObject.getJSONObject ( "result" ).getJSONArray ( "elements" ).getJSONObject ( 0 ).getIntValue ( "duration" );
                                //System.out.println ( "second==>" + second );
                                SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss" );//格式化输出日期
                                long time = second * 1000; //4h
                                outTime = new Date ( toInTime.getTime () - time ); //出口时间
                                //System.out.println ( "outTime==>" + sdf.format ( outTime ) );
                                endOutTime = sdf.format ( outTime );
                                userMapper.updateOutTime ( UserId, SceneryName, endOutTime ); //else更新数据库中的出口时间
                            }
                        } finally {
                            if (response != null) {
                                response.close ();
                            }
                            httpclient.close ();
                        }
                    }
                }
                //对只有一个或最后一个景区做处理
                JSONObject finallyUserActivityObject = JSONObject.parseObject ( oneUserActivityStrings.get ( oneUserActivityStrings.size () - 1 ) );
                String finallyUserId = finallyUserActivityObject.getString ( "UserId" );
                String finallySceneryName = finallyUserActivityObject.getString ( "SceneryName" );
                Date finallyInTime = finallyUserActivityObject.getDate ( "AllInTime" );
                SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss" );//格式化输出日期
                long time = 14400 * 1000;//4h
                Date finallyOutTime = new Date ( finallyInTime.getTime () + time );//出口时间
                String endfinallyOutTime = sdf.format ( finallyOutTime );
                //System.out.println ( "endfinallyOutTime==>" + endfinallyOutTime );
                userMapper.updateOutTime ( finallyUserId, finallySceneryName, endfinallyOutTime ); //更新最后一个行程的数据库中的出口时间
            }
        } catch(Exception e){
            e.printStackTrace();
            return userService.errorReturn("数据库执行有异常", 1004);
        }
        JSONObject returnObject = new JSONObject ();
        returnObject.put ( "Message" , "UPDATE SUCCESS");
        returnObject.put ( "Code" , "200");
        return returnObject;
    }
    //获取全国所有地区（精确到县区）的经纬度
    @CrossOrigin
    @RequestMapping(value = "areaLatLng", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public JSONObject areaLatLngObtain() throws Exception {
        JSONObject returnObject;
        try {
            List<String> specificAreaStrings = userMapper.getSpecificAreaInfo ();  //接收从数据库中传来的所有地区的具体地址
            returnObject = userService.latLngObtain(specificAreaStrings,"area");
        } catch(Exception e) {
            e.printStackTrace ();
            return userService.errorReturn( "数据库执行有异常", 1004);
        }
        return returnObject;
    }
    //获取景区的经纬度
    @CrossOrigin
    @RequestMapping(value = "sceneryLatLng", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public JSONObject sceneryLatLngObtain() throws Exception {
        JSONObject returnObject;
        try {
            List<String> specificAddressStrings = userMapper.getSpecificAddressInfo ();  //接收从数据库中传来的所有景区的具体地
            returnObject = userService.latLngObtain(specificAddressStrings, "scenery");
        } catch(Exception e) {
            e.printStackTrace ();
            return userService.errorReturn( "数据库执行有异常", 1004);
        }
        return returnObject;
    }

    //游客在景区的游览时间分布及平均游览时间
    @CrossOrigin
    @RequestMapping(value = "sceneryTourTime", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public JSONObject sceneryTourTimeQuery(@RequestBody String json) throws Exception {
        System.out.println ("sceneryTourTimeJson==>" + json);
        JSONObject sceneryTourTimeObject;
        try {
            sceneryTourTimeObject = JSONObject.parseObject(json);
        } catch(Exception e){
            e.printStackTrace();
            return userService.errorReturn( "输入JSON解析出错", 1001);
        }
        int averageSecondTime = 0;
        int averageMinuteTime = 0;
        int oneHour = 0;
        int twoHour = 0;
        int threeHour = 0;
        int fourHour = 0;
        try {
            long sumSecondTime = 0;
            String sceneryName = sceneryTourTimeObject.getString ("SceneryName");
            List<String> InAndOutTimeStrings = userMapper.getUsersInAndOutTimeInfo (sceneryName);
            //System.out.println ("InAndOutTimeStrings==>" + InAndOutTimeStrings);
            for(String InAndOutTime : InAndOutTimeStrings ){
                JSONObject InAndOutTimeObject = JSONObject.parseObject ( InAndOutTime );
                Date InTime = InAndOutTimeObject.getDate ( "InTime" );
                Date OutTime = InAndOutTimeObject.getDate ( "OutTime" );
                int second = (int) ((OutTime.getTime () - InTime.getTime ()) / 1000); //得到时间差（秒数）
                //System.out.println ("second==>" + second);
                sumSecondTime += second; //计算游客们的总时间
                //System.out.println ("sumSecondTime==>" + sumSecondTime);
                if(second > 0 && second <= 3600){
                    oneHour++;
                }else if(second <= 7200){
                    twoHour++;
                }else if(second <= 10800){
                    threeHour++;
                }else {
                    fourHour++;
                }
            }
            if(InAndOutTimeStrings.size () != 0){
                averageSecondTime = (int) (sumSecondTime/InAndOutTimeStrings.size ());
                averageMinuteTime = averageSecondTime/60;
            }
        } catch(Exception e) {
            e.printStackTrace();
            return userService.errorReturn( "数据库执行有异常", 1004);
        }
        JSONObject returnObject = new JSONObject ();
        returnObject.put ( "AverageSecondTime", averageSecondTime);
        returnObject.put ( "AverageMinuteTime", averageMinuteTime);
        returnObject.put ( "WithinAnHourNumber", oneHour);
        returnObject.put ( "BetweenOneHourAndTwoHoursNumber", twoHour);
        returnObject.put ( "BetweenTwoHoursAndThreeHoursNumber", threeHour);
        returnObject.put ( "MoreThanThreeHoursNumber", fourHour);
        return returnObject;
    }

    //游客在同一城市不同景区的游览时间及在某个城市的驻留时间
    @CrossOrigin
    @RequestMapping(value = "citySceneryTourTime", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public JSONArray citySceneryTourTimeQuery(@RequestBody String json) throws Exception{
        System.out.println ("citySceneryTourTimeJson==>" + json);
        JSONObject citySceneryTourTimeObject;
        try {
            citySceneryTourTimeObject = JSONObject.parseObject(json);
        } catch(Exception e){
            e.printStackTrace();
            return userService.errorArrayReturn("输入JSON解析出错", 1001);
        }
        JSONArray returnArray = new JSONArray ();
        //游客在同一城市不同景区的游览时间
        try {
            String userId = citySceneryTourTimeObject.getString ( "UserId" );
            List<String> cityStrings = userMapper.getCityInfo (userId);
            //System.out.println ( "cityStrings==>" + cityStrings );
            for(String cityInfo : cityStrings ) {
                JSONArray citySceneryArray = new JSONArray ();
                JSONObject aCityObject = new JSONObject ();
                JSONObject cityObject = JSONObject.parseObject ( cityInfo );
                String city = cityObject.getString ( "City" );
                List<String> citySceneryStrings = userMapper.getCitySceneryTourTimeInfo ( userId, city );
                //System.out.println ( "citySceneryStrings==>" + citySceneryStrings );
                for (String cityScenery : citySceneryStrings) {
                    JSONObject citySceneryObject = JSONObject.parseObject ( cityScenery );
                    String sceneryName = citySceneryObject.getString ( "SceneryName" );
                    Date inTime = citySceneryObject.getDate ( "InTime" );
                    Date outTime = citySceneryObject.getDate ( "OutTime" );
                    int second = (int) ((outTime.getTime () - inTime.getTime ()) / 1000); //得到时间差（秒数）
                    int minute = second / 60;
                    JSONObject oneSceneryObject = new JSONObject ();
                    oneSceneryObject.put ( "SceneryName", sceneryName );
                    oneSceneryObject.put ( "TourSecondTime", second );
                    oneSceneryObject.put ( "TourMinuteTime", minute );
                    oneSceneryObject.put ( "CityName", city );
                    citySceneryArray.add ( oneSceneryObject );
                }
                aCityObject.put ( "CityTourInfo", citySceneryArray );
                //returnObject.put ( "CitySceneryTourTime", AllCityObject );
                //取第一个景区的入口时间和最后一个景区的出口时间，得出至少在城市的停留时间
                JSONObject firstSceneryObject = new JSONObject ();
                JSONObject finalSceneryObject = new JSONObject ();
                if (citySceneryStrings.size () != 0) {
                    firstSceneryObject = JSONObject.parseObject ( citySceneryStrings.get ( 0 ) );
                    finalSceneryObject = JSONObject.parseObject ( citySceneryStrings.get ( citySceneryStrings.size () - 1 ) );
                }
                Date firstInTime = firstSceneryObject.getDate ( "InTime" );
                Date finalOutTime = finalSceneryObject.getDate ( "OutTime" );
                long mimDwellSeconds = 0;
                if (firstInTime != null && finalOutTime != null) {//其应一定有值，正常不需要判断
                    if (finalOutTime == null) {  //如果最后一个景区出口时间为空，则取其入口时间加4h
                        Date finalInTime = finalSceneryObject.getDate ( "InTime" );
                        SimpleDateFormat sdf = new SimpleDateFormat ( "yyyy-MM-dd HH:mm:ss" );//格式化输出日期
                        long time = 14400 * 1000;//4h
                        if (finalInTime != null) { //其应一定有值，正常不需要判断
                            Date middleFinalOutTime = new Date ( finalInTime.getTime () + time );//出口时间
                            finalOutTime = sdf.parse ( sdf.format ( middleFinalOutTime ) );
                        }
                    }
                    //在城市的驻留时长用时分秒表示
                    mimDwellSeconds = (int) ((finalOutTime.getTime () - firstInTime.getTime ()) / 1000);
                }
                int mimDwellHour = (int) (mimDwellSeconds / 3600);
                int mimDwellMinute = (int) (mimDwellSeconds % 3600) / 60;
                int mimDwellSecond = (int) (mimDwellSeconds % 60);
                String MimDwellTime = mimDwellHour + "h" + mimDwellMinute + "m" + mimDwellSecond + "s";
                aCityObject.put ( "MimDwellTime", MimDwellTime );
                returnArray.add ( aCityObject );
            }
        } catch(Exception e){
            e.printStackTrace();
            return userService.errorArrayReturn ("数据库执行有异常", 1004);
        }
        System.out.println ("citySceneryTourTime_ReturnArray==>" + returnArray);
        return returnArray;
    }

    //景区客流流动，游客们从什么时候去了哪个景区并待了多久，又去了哪。
    @CrossOrigin
    @RequestMapping(value = "passengerFlow", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public JSONObject passengerFlowQuery(@RequestBody String json) throws Exception{
        System.out.println ("passengerFlowJson==>" + json);
        JSONObject passengerFlowObject;
        try {
            passengerFlowObject = JSONObject.parseObject(json);
        } catch(Exception e){
            e.printStackTrace();
            return userService.errorReturn("输入JSON解析出错", 1001);
        }
        JSONObject allUsersTourObject = new JSONObject ();
        JSONArray usersTourArray = new JSONArray ();
        try {
            Date startTime = passengerFlowObject.getDate ( "StartTime" );
            Date endTime = passengerFlowObject.getDate ( "EndTime" );
            List<String> userIdStrings = userMapper.getStartEndUserIdInfo (startTime, endTime);  //接收从数据库中传来的所有游客的userId
            //System.out.println ( "userIdStrings==>" + userIdStrings );
            for (String userId : userIdStrings) {
                JSONArray AUserArray = new JSONArray ();
                JSONObject AUserIdObject = new JSONObject ();
                AUserIdObject.put ( "UserId", userId );
                UserBean userBean = new UserBean ();
                userBean.setInput ( AUserIdObject.toJSONString () );//准备好的数据
                List<String> oneUserActivityStrings = userMapper.getAllActivityInfo ( userBean );  //接收从数据库中传来的单个游客的活动路径信息
                //System.out.println ( "oneUserActivityStrings==>" + oneUserActivityStrings );
                //在游客单个景区游览信息中添加游客的游览时间
                for (String oneUserActivity : oneUserActivityStrings) {
                    JSONObject oneUserActivityObject = JSONObject.parseObject ( oneUserActivity );
                    Date inTime = oneUserActivityObject.getDate ( "AllInTime" );
                    Date outTime = oneUserActivityObject.getDate ( "OutTime" );
                    int seconds = (int) ((outTime.getTime () - inTime.getTime ()) / 1000); //得到游园时间（秒数）
                    int minutes = seconds/60;
                    oneUserActivityObject.put ( "TourMinutesTime", minutes );
                    AUserArray.add ( oneUserActivityObject );
                }
                usersTourArray.add ( AUserArray );
            }
            allUsersTourObject.put ( "UsersTour", usersTourArray );
            //System.out.println ("allUsersTourObject==>" + allUsersTourObject);
            //下面实现游客们一般情况下都是第几次去哪个景区。存在问题：只是对数据的一般化处理，并不一定是实际的线！！！
            HashMap<String,Integer> h1 = new HashMap();
            HashMap<String,Integer> h2 = new HashMap();
            HashMap<String,Integer> h3 = new HashMap();
            HashMap<String,Integer> h4 = new HashMap();
            HashMap<String,Integer> h5 = new HashMap();
            HashMap<String,Integer> h6 = new HashMap();
            HashMap<String,Integer> h7 = new HashMap();
            HashMap<String,Integer> h8 = new HashMap();
            HashMap<String,Integer> h9 = new HashMap();
            HashMap<String,Integer> h10 = new HashMap();
            for(int i = 0; i < usersTourArray.size(); i++){
                JSONArray aUserTourArray = usersTourArray.getJSONArray (i);//得到每个游客的数组
                for(int j = 0; j < aUserTourArray.size(); j++){ //遍历每个游客去过的地方
                    JSONObject aScenicSpotObject = aUserTourArray.getJSONObject ( j );
                    String sceneryName = aScenicSpotObject.getString ( "SceneryName" );
                    //统计
                    if (j == 0){
                        if(!h1.containsKey(sceneryName)){
                            h1.put(sceneryName,1);
                        }else{
                            //否则获得n的值并且加1
                            h1.put(sceneryName, h1.get(sceneryName)+1);
                        }
                    }else if (j == 1){
                        if(!h2.containsKey(sceneryName)){
                            h2.put(sceneryName,1);
                        }else{
                            //否则获得n的值并且加1
                            h2.put(sceneryName, h2.get(sceneryName)+1);
                        }
                    }else if (j == 2){
                        if(!h3.containsKey(sceneryName)){
                            h3.put(sceneryName,1);
                        }else{
                            //否则获得n的值并且加1
                            h3.put(sceneryName, h3.get(sceneryName)+1);
                        }
                    }else if (j == 3){
                        if(!h4.containsKey(sceneryName)){
                            h4.put(sceneryName,1);
                        }else{
                            //否则获得n的值并且加1
                            h4.put(sceneryName, h4.get(sceneryName)+1);
                        }
                    }else if (j == 4){
                        if(!h5.containsKey(sceneryName)){
                            h5.put(sceneryName,1);
                        }else{
                            //否则获得n的值并且加1
                            h5.put(sceneryName, h5.get(sceneryName)+1);
                        }
                    }else if (j == 5){
                        if(!h6.containsKey(sceneryName)){
                            h6.put(sceneryName,1);
                        }else{
                            //否则获得n的值并且加1
                            h6.put(sceneryName, h6.get(sceneryName)+1);
                        }
                    }else if (j == 6){
                        if(!h7.containsKey(sceneryName)){
                            h7.put(sceneryName,1);
                        }else{
                            //否则获得n的值并且加1
                            h7.put(sceneryName, h7.get(sceneryName)+1);
                        }
                    }else if (j == 7){
                        if(!h8.containsKey(sceneryName)){
                            h8.put(sceneryName,1);
                        }else{
                            //否则获得n的值并且加1
                            h8.put(sceneryName, h8.get(sceneryName)+1);
                        }
                    }else if (j == 8){
                        if(!h9.containsKey(sceneryName)){
                            h9.put(sceneryName,1);
                        }else{
                            //否则获得n的值并且加1
                            h9.put(sceneryName, h9.get(sceneryName)+1);
                        }
                    }else if (j == 9){
                        if(!h10.containsKey(sceneryName)){
                            h10.put(sceneryName,1);
                        }else{
                            //否则获得n的值并且加1
                            h10.put(sceneryName, h10.get(sceneryName)+1);
                        }
                    }

                }
            }
            //判断h1到h10是否为空，不为空的话将其值放入输出对象里
            if (!h1.isEmpty()) {
                int max1 = 0;
                String[] key1 = new String[100];
                int a = 0;
                for (String key : h1.keySet ()) {
                    if (h1.get ( key ) > max1) {
                        max1 = h1.get ( key );//找到去的第一个景区的最多的次数。
                    }
                }
                for (String key : h1.keySet ()){
                    if (h1.get ( key ) == max1) {
                        key1[a] = key; //找到第一个去的最多的次数对应的景区，并可能存在多个这样的景区。
                        a++;
                    }
                }
                JSONArray mostVisitedScenicSpotsArray1 = new JSONArray ();
                JSONObject mostVisitedScenicSpotsObject1 = new JSONObject ();
                for (int i = 0; i < a; i++) {
                    mostVisitedScenicSpotsArray1.add ( key1[i] );
                }
                mostVisitedScenicSpotsObject1.put ( "Heat", max1 );
                mostVisitedScenicSpotsObject1.put ( "TheFirstVisitScenicSpot", mostVisitedScenicSpotsArray1 );
                allUsersTourObject.put ( "First", mostVisitedScenicSpotsObject1 );
            }

            if (!h2.isEmpty()) {
                int max2 = 0;
                String[] key2 = new String[100];
                int b = 0;
                for (String key : h2.keySet ()) {
                    if (h2.get ( key ) > max2) {
                        max2 = h2.get ( key );//找到去的第二个景区的最多的次数。
                    }
                }
                for (String key : h2.keySet ()) {
                    if (h2.get ( key ) == max2) {
                        key2[b] = key; //找到第二个去的最多的次数对应的景区，并可能存在多个这样的景区。
                        b++;
                    }
                }
                JSONArray mostVisitedScenicSpotsArray2 = new JSONArray ();
                JSONObject mostVisitedScenicSpotsObject2 = new JSONObject ();
                for (int i = 0; i < b; i++) {
                    mostVisitedScenicSpotsArray2.add ( key2[i] );
                }
                mostVisitedScenicSpotsObject2.put ( "Heat", max2 );
                mostVisitedScenicSpotsObject2.put ( "TheSecondVisitScenicSpot", mostVisitedScenicSpotsArray2 );
                allUsersTourObject.put ( "Second", mostVisitedScenicSpotsObject2 );
            }

            if (!h3.isEmpty()) {
                int max3 = 0;
                String[] key3 = new String[100];
                int c = 0;
                for (String key : h3.keySet ()) {
                    if (h3.get ( key ) > max3) {
                        max3 = h3.get ( key );//找到去的第三个景区的最多的次数。
                    }
                }
                for (String key : h3.keySet ()) {
                    if (h3.get ( key ) == max3) {
                        key3[c] = key; //找到第三个去的最多的次数对应的景区，并可能存在多个这样的景区。
                        c++;
                    }
                }
                JSONArray mostVisitedScenicSpotsArray3 = new JSONArray ();
                JSONObject mostVisitedScenicSpotsObject3 = new JSONObject ();
                for (int i = 0; i < c; i++) {
                    mostVisitedScenicSpotsArray3.add ( key3[i] );
                }
                mostVisitedScenicSpotsObject3.put ( "Heat", max3 );
                mostVisitedScenicSpotsObject3.put ( "TheThirdVisitScenicSpot", mostVisitedScenicSpotsArray3 );
                allUsersTourObject.put ( "Third", mostVisitedScenicSpotsObject3 );
            }

            if (!h4.isEmpty()) {
                int max4 = 0;
                String[] key4 = new String[100];
                int d = 0;
                for (String key : h4.keySet ()) {
                    if (h4.get ( key ) > max4) {
                        max4 = h4.get ( key );//找到去的第四个景区的最多的次数。
                    }
                }
                for (String key : h4.keySet ()) {
                    if (h4.get ( key ) == max4) {
                        key4[d] = key; //找到第四个去的最多的次数对应的景区，并可能存在多个这样的景区。
                        d++;
                    }
                }
                JSONArray mostVisitedScenicSpotsArray4 = new JSONArray ();
                JSONObject mostVisitedScenicSpotsObject4 = new JSONObject ();
                for (int i = 0; i < d; i++) {
                    mostVisitedScenicSpotsArray4.add ( key4[i] );
                }
                mostVisitedScenicSpotsObject4.put ( "Heat", max4 );
                mostVisitedScenicSpotsObject4.put ( "TheFourthVisitScenicSpot", mostVisitedScenicSpotsArray4 );
                allUsersTourObject.put ( "Fourth", mostVisitedScenicSpotsObject4 );
            }

            if (!h5.isEmpty()) {
                int max5 = 0;
                String[] key5 = new String[100];
                int e = 0;
                for (String key : h5.keySet ()) {
                    if (h5.get ( key ) > max5) {
                        max5 = h5.get ( key );//找到去的第五个景区的最多的次数。
                    }
                }
                for (String key : h5.keySet ()) {
                    if (h5.get ( key ) == max5) {
                        key5[e] = key; //找到第五个去的最多的次数对应的景区，并可能存在多个这样的景区。
                        e++;
                    }
                }
                JSONArray mostVisitedScenicSpotsArray5 = new JSONArray ();
                JSONObject mostVisitedScenicSpotsObject5 = new JSONObject ();
                for (int i = 0; i < e; i++) {
                    mostVisitedScenicSpotsArray5.add ( key5[i] );
                }
                mostVisitedScenicSpotsObject5.put ( "Heat", max5 );
                mostVisitedScenicSpotsObject5.put ( "TheFifthVisitScenicSpot", mostVisitedScenicSpotsArray5 );
                allUsersTourObject.put ( "Fifth", mostVisitedScenicSpotsObject5 );
            }

            if (!h6.isEmpty()) {
                int max6 = 0;
                String[] key6 = new String[100];
                int f = 0;
                for (String key : h6.keySet ()) {
                    if (h6.get ( key ) > max6) {
                        max6 = h6.get ( key );//找到去的第六个景区的最多的次数。
                    }
                }
                for (String key : h6.keySet ()) {
                    if (h6.get ( key ) == max6) {
                        key6[f] = key; //找到第六个去的最多的次数对应的景区，并可能存在多个这样的景区。
                        f++;
                    }
                }
                JSONArray mostVisitedScenicSpotsArray6 = new JSONArray ();
                JSONObject mostVisitedScenicSpotsObject6 = new JSONObject ();
                for (int i = 0; i < f; i++) {
                    mostVisitedScenicSpotsArray6.add ( key6[i] );
                }
                mostVisitedScenicSpotsObject6.put ( "Heat", max6 );
                mostVisitedScenicSpotsObject6.put ( "TheSixthVisitScenicSpot", mostVisitedScenicSpotsArray6 );
                allUsersTourObject.put ( "Sixth", mostVisitedScenicSpotsObject6 );
            }

            if (!h7.isEmpty()) {
                int max7 = 0;
                String[] key7 = new String[100];
                int g = 0;
                for (String key : h7.keySet ()) {
                    if (h7.get ( key ) > max7) {
                        max7 = h7.get ( key );//找到去的第七个景区的最多的次数。
                    }
                }
                for (String key : h7.keySet ()) {
                    if (h7.get ( key ) == max7) {
                        key7[g] = key; //找到第七个去的最多的次数对应的景区，并可能存在多个这样的景区。
                        g++;
                    }
                }
                JSONArray mostVisitedScenicSpotsArray7 = new JSONArray ();
                JSONObject mostVisitedScenicSpotsObject7 = new JSONObject ();
                for (int i = 0; i < g; i++) {
                    mostVisitedScenicSpotsArray7.add ( key7[i] );
                }
                mostVisitedScenicSpotsObject7.put ( "Heat", max7 );
                mostVisitedScenicSpotsObject7.put ( "TheSeventhVisitScenicSpot", mostVisitedScenicSpotsArray7 );
                allUsersTourObject.put ( "Seventh", mostVisitedScenicSpotsObject7 );
            }

            if (!h8.isEmpty()) {
                int max8 = 0;
                String[] key8 = new String[100];
                int h = 0;
                for (String key : h8.keySet ()) {
                    if (h8.get ( key ) > max8) {
                        max8 = h8.get ( key );//找到去的第八个景区的最多的次数。
                    }
                }
                for (String key : h8.keySet ()) {
                    if (h8.get ( key ) == max8) {
                        key8[h] = key; //找到第八个去的最多的次数对应的景区，并可能存在多个这样的景区。
                        h++;
                    }
                }
                JSONArray mostVisitedScenicSpotsArray8 = new JSONArray ();
                JSONObject mostVisitedScenicSpotsObject8 = new JSONObject ();
                for (int i = 0; i < h; i++) {
                    mostVisitedScenicSpotsArray8.add ( key8[i] );
                }
                mostVisitedScenicSpotsObject8.put ( "Heat", max8 );
                mostVisitedScenicSpotsObject8.put ( "TheEighthVisitScenicSpot", mostVisitedScenicSpotsArray8 );
                allUsersTourObject.put ( "Eighth", mostVisitedScenicSpotsObject8 );
            }

            if (!h9.isEmpty()) {
                int max9 = 0;
                String[] key9 = new String[100];
                int k = 0;
                for (String key : h9.keySet ()) {
                    if (h9.get ( key ) > max9) {
                        max9 = h9.get ( key );//找到去的第九个景区的最多的次数。
                    }
                }
                for (String key : h9.keySet ()) {
                    if (h9.get ( key ) == max9) {
                        key9[k] = key; //找到第九个去的最多的次数对应的景区，并可能存在多个这样的景区。
                        k++;
                    }
                }
                JSONArray mostVisitedScenicSpotsArray9 = new JSONArray ();
                JSONObject mostVisitedScenicSpotsObject9 = new JSONObject ();
                for (int i = 0; i < k; i++) {
                    mostVisitedScenicSpotsArray9.add ( key9[i] );
                }
                mostVisitedScenicSpotsObject9.put ( "Heat", max9 );
                mostVisitedScenicSpotsObject9.put ( "TheNinthVisitScenicSpot", mostVisitedScenicSpotsArray9 );
                allUsersTourObject.put ( "Ninth", mostVisitedScenicSpotsObject9 );
            }

            if (!h10.isEmpty()) {
                int max10 = 0;
                String[] key10 = new String[100];
                int l = 0;
                for (String key : h10.keySet ()) {
                    if (h10.get ( key ) > max10) {
                        max10 = h10.get ( key );//找到去的第十个景区的最多的次数。
                    }
                }
                for (String key : h10.keySet ()) {
                    if (h10.get ( key ) == max10) {
                        key10[l] = key; //找到第十个去的最多的次数对应的景区，并可能存在多个这样的景区。
                        l++;
                    }
                }
                JSONArray mostVisitedScenicSpotsArray10 = new JSONArray ();
                JSONObject mostVisitedScenicSpotsObject10 = new JSONObject ();
                for (int i = 0; i < l; i++) {
                    mostVisitedScenicSpotsArray10.add ( key10[i] );
                }
                mostVisitedScenicSpotsObject10.put ( "Heat", max10 );
                mostVisitedScenicSpotsObject10.put ( "TheTenthVisitScenicSpot", mostVisitedScenicSpotsArray10 );
                allUsersTourObject.put ( "Tenth", mostVisitedScenicSpotsObject10 );
            }

            //实现一般从哪个景区到哪个景区(两个景区的之间的联系)，并将最终的两两的结果连成线
            HashMap<String,Integer> twoScenicSpotsSplicing = new HashMap();
            for(int i = 0; i < usersTourArray.size(); i++) {
                JSONArray aUserTourArray = usersTourArray.getJSONArray ( i );//得到每个游客的数组
                for (int j = 0; j < aUserTourArray.size() - 1; j++) { //遍历每个游客去过的地方,此处排除了只去过一个地方的游客
                    JSONObject fromScenicSpotObject = aUserTourArray.getJSONObject ( j );
                    JSONObject toScenicSpotObject = aUserTourArray.getJSONObject ( (j + 1) );
                    String fromSceneryName = fromScenicSpotObject.getString ( "SceneryName" );//字符串1
                    String toSceneryName = toScenicSpotObject.getString ( "SceneryName" ); //字符串2
                    if(!fromSceneryName.equals ( toSceneryName )) {
                        if (!twoScenicSpotsSplicing.containsKey ( fromSceneryName + "-->" + toSceneryName )) { //拼接字符串制造唯一性
                            twoScenicSpotsSplicing.put ( fromSceneryName + "-->" + toSceneryName, 1 ); //拼接成的字符串不在map里，则放入并计数1
                        } else {
                            twoScenicSpotsSplicing.put ( fromSceneryName + "-->" + toSceneryName, twoScenicSpotsSplicing.get ( fromSceneryName + "-->" + toSceneryName ) + 1 ); //否则计数+1
                        }
                    }
                }
            }

            //将从哪到哪出现10次的景区信息输出
            JSONArray twoScenicSpotsSplicingArray = new JSONArray ();
            for(String key: twoScenicSpotsSplicing.keySet()){
                //if(twoScenicSpotsSplicing.get(key) > 10) { //此数可变
                JSONObject twoScenicSpotsSplicingObject = new JSONObject ();
                twoScenicSpotsSplicingObject.put ( "fromTo", key );
                twoScenicSpotsSplicingObject.put ( "fromToNumber", twoScenicSpotsSplicing.get ( key ) );
                twoScenicSpotsSplicingArray.add ( twoScenicSpotsSplicingObject );
                //}
            }

            //对twoScenicSpotsSplicing有改动,故将其赋值给另一个map：twoScenicSpotsConnection
            allUsersTourObject.put ( "fromToInfo", twoScenicSpotsSplicingArray );
            //System.out.println ("twoScenicSpotsSplicing==>" + twoScenicSpotsSplicing);
            HashMap<String,Integer> twoScenicSpotsConnection = new HashMap();
            for(String key: twoScenicSpotsSplicing.keySet()){
                twoScenicSpotsConnection.put(key, twoScenicSpotsSplicing.get(key));
            }
            //System.out.println ("twoScenicSpotsConnection==>" + twoScenicSpotsConnection);

            //串成多个长游览路径
            //只剩下X景区开头最多的数据
            for(String key0: twoScenicSpotsConnection.keySet()){
                int maxFromToNumber = twoScenicSpotsConnection.get(key0);
                if(maxFromToNumber != 0) {
                    String str0 = key0.substring ( 0, key0.indexOf ( "-" ) );
                    String strKey0 = key0;
                    for (String key1 : twoScenicSpotsConnection.keySet ()) {
                        String str1 = key1.substring ( 0, key1.indexOf ( "-" ) );
                        if (!strKey0.equals(key1) && str0.equals(str1)) {
                            if (twoScenicSpotsConnection.get ( key1 ) > maxFromToNumber) { //也有可能存在多个最大值
                                twoScenicSpotsConnection.put ( strKey0, 0 );
                                strKey0 = key1;
                                str0 = str1;
                                maxFromToNumber = twoScenicSpotsConnection.get ( key1 );
                            } else if(twoScenicSpotsConnection.get ( key1 ) < maxFromToNumber){
                                twoScenicSpotsConnection.put ( key1, 0 );
                            }
                        }
                    }
                }
            }
            //System.out.println ("AlterTwoScenicSpotsConnection==>"+twoScenicSpotsConnection);
            String[] scenicSpotFlow = new String[1000]; //定义一个字符串数组存储路径
            int count = 0;
            for(String key1: twoScenicSpotsConnection.keySet()){
                String strFront1;
                String strMiddle1;
                String strBehind1;
                if(twoScenicSpotsConnection.get ( key1 ) != 0) {
                    strFront1 = key1.substring ( 0, key1.indexOf ( "-" ) );
                    strMiddle1 = key1.substring ( 0, key1.indexOf ( ">" ) );
                    strBehind1 = key1.substring ( strMiddle1.length () + 1, key1.length () );
                    scenicSpotFlow[count] = key1;
                    for(String key2: twoScenicSpotsConnection.keySet()){ //看一下是否存在key1已经变化了，但是还在用
                        if(twoScenicSpotsConnection.get ( key2 ) != 0 && !key1.equals (key2)) {
                            String strFront2 = key2.substring ( 0, key2.indexOf ( "-" ) );
                            String strMiddle2 = key2.substring ( 0, key2.indexOf ( ">" ) );
                            String strBehind2 = key2.substring ( strMiddle2.length () + 1, key2.length () );
                            if (strBehind1.equals(strFront2)) {
                                scenicSpotFlow[count] = scenicSpotFlow[count] + "-->" + strBehind2; //串联景区
                                //值变换，与下面的比较
                                strBehind1 = strBehind2;
                                key1 = key2;
                            } else if(strFront1.equals(strBehind2)){
                                scenicSpotFlow[count] =  strFront2 + "-->" + scenicSpotFlow[count]; //串联景区
                                //值变换，与下面的比较
                                strFront1 = strFront2;
                                key1 = key2;
                            }
                        }
                    }
                    count++;
                }
            }

            //去掉重复的游览路径
            List<String> scenicSpotFlowList0 = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                if(!scenicSpotFlowList0.contains(scenicSpotFlow[i])) {
                    scenicSpotFlowList0.add(scenicSpotFlow[i]);
                }
            }
            List<String> scenicSpotFlowList1 = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                if(!scenicSpotFlowList1.contains(scenicSpotFlow[i])) {
                    scenicSpotFlowList1.add(scenicSpotFlow[i]);
                }
            }
            //去掉长游览路径包含的短游览路径
            for (int i = 0; i < scenicSpotFlowList0.size (); i++) {
                for (int j = 0; j < scenicSpotFlowList0.size (); j++) {
                    if(i != j) {
                        if (scenicSpotFlowList0.get ( i ).indexOf ( scenicSpotFlowList0.get ( j ) ) != -1) {
                            scenicSpotFlowList1.set ( j, "delete" );

                        }
                    }
                }
            }
            List<String> scenicSpotFlowList = new ArrayList<>();
            for (int i = 0; i < scenicSpotFlowList1.size (); i++) {
                if(!(scenicSpotFlowList1.get ( i )).equals ( "delete" )) {
                    scenicSpotFlowList.add(scenicSpotFlowList1.get ( i ));
                }
            }

            JSONArray scenicSpotFlowArray = new JSONArray ();//放各条路径
            for (int i = 0; i < scenicSpotFlowList.size (); i++) {
                //int j = i + 1;
                //JSONObject aScenicSpotFlowObject = new JSONObject ();
                //aScenicSpotFlowObject.put("TourRoute_" + j, scenicSpotFlowList.get ( i ));
                scenicSpotFlowArray.add ( scenicSpotFlowList.get ( i ) );
            }
            System.out.println ("scenicSpotFlowArray==>" + scenicSpotFlowArray);
            //将长串游览路径数组输出
            allUsersTourObject.put ( "TourRoute", scenicSpotFlowArray );
        } catch(Exception e){
            e.printStackTrace();
            return userService.errorReturn("数据库执行有异常", 1004);
        }
        return allUsersTourObject;
    }
    //在一定时间段游客游览量最多的前10名
    @CrossOrigin
    @RequestMapping(value = "volumeRanking", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public JSONArray volumeRankingQuery(@RequestBody String json) throws Exception {
        System.out.println ( "volumeRankingJson==>" + json );
        JSONObject volumeRankingObject;
        try {
            volumeRankingObject = JSONObject.parseObject ( json );
        } catch (Exception e) {
            e.printStackTrace ();
            return userService.errorArrayReturn ( "输入JSON解析出错", 1001 );
        }
        JSONArray returnArray = new JSONArray ();
        try {
            Date startTime = volumeRankingObject.getDate ( "StartTime" );
            Date endTime = volumeRankingObject.getDate ( "EndTime" );
            List<String> sceneryStrings = userMapper.getStartEndSceneryInfo (startTime, endTime);  //接收从数据库中传来的所有景区（重复）
            HashMap<String,Integer> hScenery = new HashMap();
            Map.Entry<String, Integer> hScenery10;
            LinkedHashMap<String,Integer> hSceneryTen = new LinkedHashMap();
            List<Map.Entry<String, Integer>> list = new ArrayList<>();
            for (String scenery : sceneryStrings) {
                JSONObject sceneryObject = JSONObject.parseObject ( scenery );
                String sceneryName = sceneryObject.getString ( "SceneryName" );
                if(!hScenery.containsKey(sceneryName)){
                    hScenery.put(sceneryName,1);
                }else{
                    hScenery.put(sceneryName, hScenery.get(sceneryName)+1);
                }
            }
            for(Map.Entry<String, Integer> entry : hScenery.entrySet()){
                list.add(entry); //将map中的元素放入list中
            }
            list.sort(new Comparator<Map.Entry<String, Integer>>(){
                @Override
                public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                    return o2.getValue()-o1.getValue();}
                //逆序（从大到小）排列，正序为“return o1.getValue()-o2.getValue”
            });

            for(int i = 0; i < 10; i++){
                hScenery10 = list.get ( i );
                hSceneryTen.put ( hScenery10.getKey (), hScenery10.getValue ());
            }
            for (String sceneryName : hSceneryTen.keySet()){
                JSONObject ASceneryObject = new JSONObject ();
                ASceneryObject.put ( "Ranking",  sceneryName);
                ASceneryObject.put ( "Heat", hSceneryTen.get ( sceneryName ));
                returnArray.add ( ASceneryObject );
            }
        } catch (Exception e) {
            e.printStackTrace ();
            return userService.errorArrayReturn ( "数据库执行有异常", 1004 );
        }
        return returnArray;
    }
}

