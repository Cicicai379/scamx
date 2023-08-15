package com.xin.aoc.controller.main;

import com.xin.aoc.form.UserForm;
import com.xin.aoc.mapper.ContestMapper;
import com.xin.aoc.mapper.DiscussionMapper;
import com.xin.aoc.mapper.LearnMapper;
import com.xin.aoc.mapper.RecordMapper;
import com.xin.aoc.model.UserInfo;
import com.xin.aoc.service.MailService;
import com.xin.aoc.service.UserInfoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.Random;
import java.util.Set;

@Controller
public class UserController {
    static Logger logger = LoggerFactory.getLogger(UserController.class);
    static final String CHECK_CODE = "check_code";
    static final String PASSWD_SUFFIX = "702e4946e6db9b7a74b921fe85e83f32";

    @Autowired
    private Validator validator;

    @Autowired
    private MailService mailService;

    @Autowired
    private UserInfoService userInfoService;
    @Autowired
    private DiscussionMapper discussionMapper;
    @Autowired
    private LearnMapper learnMapper;
    @Autowired
    private ContestMapper contestMapper;
    @Autowired
    private RecordMapper recordMapper;

    @RequestMapping("/login")
    public String login(@RequestParam(required=false,value="username") String username,
                        @RequestParam(required=false,value="password") String password,
                        HttpServletRequest request) {
        if (username != null && password != null) {
            password = getPasswordMd5(password);
            UserInfo userInfo = userInfoService.getUserInfo(username);
            if (userInfo != null && password.equals(userInfo.getPassword())) {
                userInfo.setPassword("");
                HttpSession session = request.getSession();
                session.setAttribute("login_user", userInfo);
                return "redirect:/";
            }
            request.setAttribute("msg", "incorrect username or password");
        }
        return "login";
    }


    @RequestMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return "redirect:/";
    }

    @GetMapping("/register")
    public String register(@ModelAttribute("obj") UserForm user) {
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("obj") @Validated UserForm user,
                           BindingResult rs, HttpServletRequest request, Model model) {
        if (rs.hasErrors()) {
            logger.info("register info invalid");
            return "register";
        }
        String code = (String)request.getSession().getAttribute(CHECK_CODE);
        request.getSession().removeAttribute(CHECK_CODE);
        logger.info(code + " " +user.getCheckCode());
        if (code == null || !code.equals(user.getCheckCode())) {
            model.addAttribute("msg", "verification code not match");
            return "register";
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserName(user.getUserName());
        userInfo.setEmail(user.getEmail());
        userInfo.setNickName(user.getNickName());
        userInfo.setPassword(getPasswordMd5(user.getPassword()));
        if (userInfoService.addUserInfo(userInfo)) {
            model.addAttribute("msg", "succeeded! login now");
        } else {
            model.addAttribute("msg", "failed");
        }
        return "register";
    }

    private String getPasswordMd5(String password) {
        password += PASSWD_SUFFIX;
        return DigestUtils.md5DigestAsHex(password.getBytes());
    }

    private String generateCode(int length) {
        String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuffer code = new StringBuffer();
        for (int i = 0; i < length; i++) {
            int number = random.nextInt(str.length() - 1);
            code.append(str.charAt(number));
        }
        return code.toString();
    }

    @RequestMapping("/get_check_code")
    @ResponseBody
    public String getCheckCode(UserForm user, HttpServletRequest request) {
        Set<ConstraintViolation<UserForm>> error = validator.validateProperty(user, "email");
        if (error.size() > 0) {
            return error.iterator().next().getMessage();
        }
        String code = generateCode(6);
        request.getSession().setAttribute(CHECK_CODE, code.toString());

        String subject = "verification code";
        String bodyText = String.format("<html><h1>%s</h1></html>", code);
        if (mailService.sendMessage(user.getEmail(), subject, bodyText) == false) {
            return "send mail error.";
        }
        return "ok";
    }

    @RequestMapping("/check_username")
    @ResponseBody
    public String checkUserName(@RequestParam(value = "userName") String userName, HttpServletRequest request) {
        UserInfo userInfo = userInfoService.getUserInfo(userName);
        UserInfo oldUser = (UserInfo)request.getSession().getAttribute("login_user");

        if((userInfo != null) && !(oldUser.getUserName().equals(userName))){
            logger.info("exist!");
            System.out.println(userName+""+oldUser.getUserName()+'!');
            System.out.println(userName==oldUser.getUserName());
            return "exist";
        }else{
            return "ok";
        }
    }

    @RequestMapping("/check_username_register")
    @ResponseBody
    public String checkUserNameR(@RequestParam(value = "userName") String userName, HttpServletRequest request) {
        UserInfo userInfo = userInfoService.getUserInfo(userName);

        if((userInfo != null)){
            logger.info("exist!");
            return "exist";
        }else{
            return "ok";
        }
    }

    @RequestMapping("/check_username_not")
    @ResponseBody
    public String checkUserNameNot(@RequestParam(value = "userName") String userName) {
        UserInfo userInfo = userInfoService.getUserInfo(userName);
        return (userInfo == null ? "not exist" : "ok");
    }

    @GetMapping("/forget_password")
    public String forget_password(@ModelAttribute("obj") UserForm user) {
        return "forget_password";
    }

    @PostMapping("/forget_password")
    public String forget_password(@ModelAttribute("obj") @Validated UserForm user,
                           BindingResult rs, HttpServletRequest request, Model model) {
        if (rs.hasErrors()) {
            return "register";
        }
        String code = (String)request.getSession().getAttribute(CHECK_CODE);
        request.getSession().removeAttribute(CHECK_CODE);
        logger.info(code + " " +user.getCheckCode());
        if (code == null || !code.equals(user.getCheckCode())) {
            model.addAttribute("msg", "verification code not match");
            return "forget_password";
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserName(user.getUserName());
        userInfo.setEmail(user.getEmail());
        userInfo.setNickName(user.getNickName());
        userInfo.setPassword(getPasswordMd5(user.getPassword()));
        if (userInfoService.changeUserInfo(userInfo)) {
            model.addAttribute("reset_success", "ok");
        } else {
            model.addAttribute("msg", "password reset failed.");
        }
        return "redirect:/login";
    }

    @GetMapping("/user/setting")
    public String edit(@ModelAttribute("obj") UserForm userInfo, Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();
        UserInfo user = (UserInfo)request.getSession().getAttribute("login_user");
        userInfo.setNickName(user.getNickName());
        userInfo.setEmail(user.getEmail());
        userInfo.setUserName(user.getUserName());
        userInfo.setPassword(user.getPassword());
        session.setAttribute("login_user", user);
        model.addAttribute("username", user.getUserName());
        return "user/setting";
    }
    public void uploadFile(MultipartFile file, UserInfo user){
        String UPLOADED_FOLDER = "./images/";
        File folder = new File(UPLOADED_FOLDER);
        if (!folder.exists())
            folder.mkdirs();

        String originalFilename = file.getOriginalFilename();
        int dotIndex = originalFilename.lastIndexOf(".");

        String fileExtension = originalFilename.substring(dotIndex);
        File newFile = new File(folder, user.getUserId() + fileExtension);

        if (newFile.exists()) {
            newFile.delete();
        }
        try {
            byte[] bytes = file.getBytes();
            String url = "/images/" + user.getUserId() + fileExtension+"?t="+System.currentTimeMillis();
            userInfoService.addImg(user.getUserId(), url);
            user.setImage(url);
            Files.write(newFile.toPath(), bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info(file.getOriginalFilename()+"profile_pic name");
    }
    @PostMapping("/user/setting")
    public String editProfile(@ModelAttribute("obj") @Validated UserForm editUser,
                              BindingResult rs, HttpServletRequest request, Model model,
                              @RequestParam(value="file", required = false) MultipartFile file) {


        if (rs.hasErrors()) {
            logger.info("reset info invalid");
            return "user/setting";
        }

        HttpSession session = request.getSession();
        UserInfo user = (UserInfo)request.getSession().getAttribute("login_user");
        session.setAttribute("login_user", user);

        if (file!=null && file.getOriginalFilename().length()>0) {
            logger.info("file name:" + file.getOriginalFilename());
            uploadFile(file, user);
            model.addAttribute("msg1", "Profile image reset succeeded");
            return "user/setting";
        }else if(file==null){
            logger.info("didn't change profo");
        }else{
            model.addAttribute("msg1", "Profile image reset failed");
        }

        UserInfo newUser = new UserInfo();
        if (editUser!=null) {
            newUser.setUserId(user.getUserId());
            newUser.setImage(user.getImage());
            newUser.setUserName(editUser.getUserName());
            newUser.setEmail(editUser.getEmail());
            newUser.setNickName(editUser.getNickName());
            newUser.setPassword(getPasswordMd5(editUser.getPassword()));
        }
        logger.info(" !!"+newUser.getUserName());
        if (userInfoService.changeAllUserInfo(newUser)) {
            session.setAttribute("login_user", newUser);
            model.addAttribute("msg", "reset success");
            discussionMapper.updateAll(newUser);
            learnMapper.updateAll(newUser);
            contestMapper.updateAll(newUser);
            recordMapper.updateAll(newUser);
        } else {
            model.addAttribute("msg", "reset failed");
        }

        return "user/setting";
    }



    @RequestMapping("/terms")
    public String terms() {
        return "terms";
    }
}
