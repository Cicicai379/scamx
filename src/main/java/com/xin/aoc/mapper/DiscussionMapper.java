package com.xin.aoc.mapper;

import com.xin.aoc.form.ProblemForm;
import com.xin.aoc.model.Discussion;
import com.xin.aoc.model.Problem;
import com.xin.aoc.model.UserInfo;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface DiscussionMapper {
    @Select("select * from discussions where camp_id=#{id} order by cur_date desc")
    List<Discussion> getDiscussionById(int id);

    @Insert("insert into discussions (user_id, nick_name, image, cur_date, camp_id, content) " +
            "values (#{userId},#{nickName},#{image},#{curDate},#{campId},#{content})")
    void addDiscussion(Discussion discussion);


    @Delete("delete from discussions "+
            "where camp_id=#{campId} and user_id=#{userId} and cur_date=#{curDate}")
    void delDiscussion(int campId, int userId, String curDate);
    @Update("update discussions set nick_name=#{nickName}, image=#{image}" +
            "where user_id=#{userId}")
     void updateAll(UserInfo user);

}
