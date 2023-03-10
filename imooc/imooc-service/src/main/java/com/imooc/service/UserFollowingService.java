package com.imooc.bilibili.service;

import com.imooc.bilibili.dao.UserFollowingDao;
import com.imooc.bilibili.domain.FollowingGroup;
import com.imooc.bilibili.domain.User;
import com.imooc.bilibili.domain.UserFollowing;
import com.imooc.bilibili.domain.UserInfo;
import com.imooc.bilibili.domain.constant.UserConstant;
import com.imooc.bilibili.domain.exception.ConditionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserFollowingService {

    @Autowired
    private UserFollowingDao userFollowingDao;

    @Autowired
    private FollowingGroupService followingGroupService;

    @Autowired
    private UserService userService;

    @Transactional
    public void addUserFollowings(UserFollowing userFollowing) {
        Long groupId = userFollowing.getGroupId();
        if(groupId == null){
            FollowingGroup followingGroup = followingGroupService.getByType(UserConstant.USER_FOLLOWING_GROUP_TYPE_DEFAULT);
            userFollowing.setGroupId(followingGroup.getId());
        }else{
            FollowingGroup followingGroup = followingGroupService.getById(groupId);
            if(followingGroup == null){
                throw new ConditionException("关注分组不存在！");
            }
        }
        Long followingId = userFollowing.getFollowingId();
        User user = userService.getUserById(followingId);
        if(user == null){
            throw new ConditionException("关注的用户不存在！");
        }
        userFollowingDao.deleteUserFollowing(userFollowing.getUserId(), followingId);
        userFollowing.setCreateTime(new Date());
        userFollowingDao.addUserFollowing(userFollowing);
    }

    // 第一步：获取关注的用户列表
    // 第二步：根据关注用户的id查询关注用户的基本信息
    // 第三步：将关注用户按关注分组进行分类
    public List<FollowingGroup> getUserFollowings(Long userId){

        List<UserFollowing> list = userFollowingDao.getUserFollowings(userId);
        //拿到关注用户id的数组
        Set<Long> followingIdSet = list.stream().map(UserFollowing::getFollowingId).collect(Collectors.toSet());
        List<UserInfo> userInfoList = new ArrayList<>();
        if(followingIdSet.size() > 0){
            //返回所有的关注用户id的用户信息拿list接受
            userInfoList = userService.getUserInfoByUserIds(followingIdSet);
        }
        //关注用户id数组集合
        for(UserFollowing userFollowing : list){
            //关注用户id的信息list集合
            for(UserInfo userInfo : userInfoList){
                //如果关注用户id和用户信息list中的id相同
                if(userFollowing.getFollowingId().equals(userInfo.getUserId())){
                    //就给对应的userFollowing添加用户信息
                    userFollowing.setUserInfo(userInfo);
                }
            }
        }
        //获取全部的用户分组
        List<FollowingGroup> groupList = followingGroupService.getByUserId(userId);
        //创建一个全部用户分组
        FollowingGroup allGroup = new FollowingGroup();
        allGroup.setName(UserConstant.USER_FOLLOWING_GROUP_ALL_NAME);
        //将userInfoList所有的用户信息放入 这个全部用户分组类里
        allGroup.setFollowingUserInfoList(userInfoList);
        //创建一个分组集合
        List<FollowingGroup> result = new ArrayList<>();
        //这个是全部用户分组
        result.add(allGroup);
        //遍历用户分组 0 1 2
        for(FollowingGroup group : groupList){
           // 创建对应的list
            List<UserInfo> infoList = new ArrayList<>();
            for(UserFollowing userFollowing : list){
                //如果当前分组的id和用户关注表里面的分组id相同 则表面该用户是当前分组
                if(group.getId().equals(userFollowing.getGroupId())){
                    //将这个用户信息放到当前的list中
                    infoList.add(userFollowing.getUserInfo());
                }

            }
            //给对应的分组放入对应的list
            group.setFollowingUserInfoList(infoList);
            //将当前分组添加到总的list中
            result.add(group);
        }
        //返回了是一个分组的list 里面分为全部用户， 和type为0 ， 1 ，2 的用户分组，并且每个每个分组里面有一个
        //用户信息的list集合 前端可以遍历，达到了分组获取的效果
        return result;
    }

    // 第一步：获取当前用户的粉丝列表
    // 第二步：根据粉丝的用户id查询基本信息
    // 第三步：查询当前用户是否已经关注该粉丝
    public List<UserFollowing> getUserFans(Long userId){
        List<UserFollowing> fanList = userFollowingDao.getUserFans(userId);
        Set<Long> fanIdSet = fanList.stream().map(UserFollowing::getUserId).collect(Collectors.toSet());
        List<UserInfo> userInfoList = new ArrayList<>();
        if(fanIdSet.size() > 0){
            userInfoList = userService.getUserInfoByUserIds(fanIdSet);
        }
        List<UserFollowing> followingList = userFollowingDao.getUserFollowings(userId);
        for(UserFollowing fan : fanList){
            for(UserInfo userInfo : userInfoList){
                if(fan.getUserId().equals(userInfo.getUserId())){
                    userInfo.setFollowed(false);
                    fan.setUserInfo(userInfo);
                }
            }
            for(UserFollowing following : followingList){
                if(following.getFollowingId().equals(fan.getUserId())){
                    fan.getUserInfo().setFollowed(true);
                }
            }
        }
        return fanList;
    }

    public Long addUserFollowingGroups(FollowingGroup followingGroup) {
        followingGroup.setCreateTime(new Date());
        followingGroup.setType(UserConstant.USER_FOLLOWING_GROUP_TYPE_USER);
        followingGroupService.addFollowingGroup(followingGroup);
        return followingGroup.getId();
    }

    public List<FollowingGroup> getUserFollowingGroups(Long userId) {
        return followingGroupService.getUserFollowingGroups(userId);
    }

    public List<UserInfo> checkFollowingStatus(List<UserInfo> userInfoList, Long userId) {
        List<UserFollowing> userFollowingList = userFollowingDao.getUserFollowings(userId);
        for(UserInfo userInfo : userInfoList){
            userInfo.setFollowed(false);
            for(UserFollowing userFollowing : userFollowingList){
                if(userFollowing.getFollowingId().equals(userInfo.getUserId())){
                    userInfo.setFollowed(true);
                }
            }
        }
        return userInfoList;
    }
}
