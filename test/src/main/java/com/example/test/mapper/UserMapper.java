package com.example.test.mapper;

import com.example.test.bean.UserBean;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

/**
 * @author Cyan
 * @Description
 * @create 2020-10-07 19:36
 */
@Mapper
public interface UserMapper {
    String getInfo(UserBean userBean);
    List<String> getUserIdInfo();
    List<String> getStartEndUserIdInfo(Date startTime, Date endTime);
    List<String> getSpecificAddressInfo();
    List<String> getSpecificAreaInfo();
    List<String> getActivityInfo(UserBean userBean);
    List<String> getAllActivityInfo(UserBean userBean);
    void updateOutTime(String userId, String sceneryName, String endOutTime);
    void updateSceneryLatLng(String lat, String lng, String sceneryName);
    void updateAreaLatLng(String lat, String lng, String codes);
    List<String> getUsersInAndOutTimeInfo(String sceneryName);
    List<String> getCityInfo(String userId);
    List<String> getCitySceneryTourTimeInfo(String userId, String city);
    List<String> getStartEndSceneryInfo(Date startTime, Date endTime);
}
