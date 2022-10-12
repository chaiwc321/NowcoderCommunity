package com.nowcoder.communnity.controller;

import com.nowcoder.communnity.alphaservice.AlphaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.*;

@Controller
@RequestMapping("/alpha")
public class AlphaController  {

    @RequestMapping("/Hello")
    @ResponseBody
    public String sayHello(){
        return "Hello, Spring Boot!";
    }
//
    // 定义service成员变量
    @Autowired
    private AlphaService alphaService;

    // 注入
    @RequestMapping("/dao")
    @ResponseBody
    public String daoback(){
        return alphaService.find();
    }

    // 处理GET类型的请求
    @RequestMapping(path = "/student", method = RequestMethod.GET)
    @ResponseBody
    public String getStudent(
            @RequestParam(name = "current", required = false, defaultValue = "孙悟空")
            String name,
            @RequestParam(name = "id", required = false, defaultValue = "-1")
            int id){
        return name + id;
    }

    // 另一种url上的参数抓取
    @RequestMapping(path = "/student/{id}", method = RequestMethod.GET)
    @ResponseBody
    public String getStudentID(
            @PathVariable(name = "id", required = false)
            int id){
        return id + "";
    }

    // 处理POST请求
    @RequestMapping(path = "/student", method = RequestMethod.POST)
    @ResponseBody
    public String saveStudent(String name, int age){
        System.out.println(name);
        System.out.println(age);
        return "success";
    }

    // 响应HTML格式的数据
    @RequestMapping(path = "/teacher", method = RequestMethod.GET)
    public ModelAndView getTeacher(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("name", "张山");
        modelAndView.addObject("age", 123);
        modelAndView.setViewName("demo/view");
        return modelAndView;
    }
    @RequestMapping(path = "/school",method = RequestMethod.GET)
    public String getSchool(Model model){
        model.addAttribute("name", "北京大学");
        model.addAttribute("age", 123);
        return "/demo/view";
    }

    // 响应JSON格式的数据
    @RequestMapping(path = "/emp", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> getEmp(){
        HashMap<String, Object> emp = new HashMap<>();
        emp.put("name", "张山");
        emp.put("id", 123);
        emp.put("age", 47864);
        return emp;
    }
    @RequestMapping(path = "/emps", method = RequestMethod.GET)
    @ResponseBody
    public List<Map<String ,Object>> getEmps(){
        HashMap<String, Object> emp = new HashMap<>();
        List<Map<String, Object>> emps = new ArrayList<>();
        emp.put("name", "张山");
        emp.put("id", 123);
        emp.put("age", 47864);
        emps.add(emp);

        emp = new HashMap<>();
        emp.put("name", "历史");
        emp.put("id", 234);
        emp.put("age", 4535);
        emps.add(emp);

        emp = new HashMap<>();
        emp.put("name", "王五");
        emp.put("id", 3245);
        emp.put("age", 564);
        emps.add(emp);
        return emps;
    }
}
