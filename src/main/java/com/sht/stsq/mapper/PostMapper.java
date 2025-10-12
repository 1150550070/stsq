package com.sht.stsq.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sht.stsq.model.entity.Post;
import java.util.Date;
import java.util.List;

/**
 * 帖子数据库操作
 *
 * @author <a href="https://gitee.com/ht115055/stsq">刷题神器</a>
 * 
 */
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 查询帖子列表（包括已被删除的数据）
     */
    List<Post> listPostWithDelete(Date minUpdateTime);

}




