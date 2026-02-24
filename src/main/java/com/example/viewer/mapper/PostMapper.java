package com.example.viewer.mapper;

import com.example.viewer.entity.Post;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PostMapper {

    List<Post> listRoot();

    List<Post> search(String kw);

    Post findById(String id);

    List<Post> listComments(String rootId);
}
