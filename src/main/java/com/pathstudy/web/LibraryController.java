package com.pathstudy.web;

import com.pathstudy.domain.Bookmark;
import com.pathstudy.domain.User;
import com.pathstudy.service.BookmarkService;
import com.pathstudy.service.CurrentUserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class LibraryController {

    private final BookmarkService bookmarks;
    private final CurrentUserService currentUser;

    public LibraryController(BookmarkService bookmarks, CurrentUserService currentUser) {
        this.bookmarks = bookmarks;
        this.currentUser = currentUser;
    }

    @GetMapping("/library")
    public String library(Model model) {
        User user = currentUser.require();
        List<Bookmark> all = bookmarks.list(user);

        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Bookmark b : all) {
            counts.merge(b.getCategory(), 1, Integer::sum);
        }

        model.addAttribute("bookmarks", all);
        model.addAttribute("counts", counts);
        model.addAttribute("total", all.size());
        return "library/index";
    }

    @PostMapping("/library/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        User user = currentUser.require();
        bookmarks.delete(user, id);
        ra.addFlashAttribute("toast", "Đã xoá khỏi kho lưu trữ.");
        return "redirect:/library";
    }
}
