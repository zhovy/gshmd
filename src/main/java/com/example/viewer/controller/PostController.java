package com.example.viewer.controller;

import com.example.viewer.entity.Post;
import com.example.viewer.mapper.PostMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostMapper mapper;

    @GetMapping
    public String list(String kw, Model model) {
        model.addAttribute("list",
                kw == null || kw.isBlank()
                        ? mapper.listRoot()
                        : mapper.search(kw)
        );
        return "list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        Post post = mapper.findById(id);
        model.addAttribute("post", post);
        model.addAttribute("comments", mapper.listComments(id));
        return "detail";
    }
}
