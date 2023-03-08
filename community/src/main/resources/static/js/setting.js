$(function() {
    $("#uploadForm").submit(upload);  // 表单提交触发的方法
}) ;

function upload() {
    $.ajax({  // ajax可以传多个keyvalue类型的数据
       url:"http://upload-z2.qiniup.com",  // 上传到七牛云的地址
        method:"post",
        processData: false,  // 上传时，不让文件转为字符串格式
        contentType: false,  // 类似作用？
        data: new FormData($("#uploadForm")[0]),  // 找到静态页面中的dumb数组，第一维就是数据
        success: function (data) {  // 成功触发时
           if(data && data.code == 0) {
               // 更新头像访问路径
               $.post(
                   CONTEXT_PATH + "/user/header/url",
                   {"fileName":$("input[name='key']").val()},
                   function (data) {
                       data = $.parseJSON(data);
                       if(data.code == 0) {
                           window.location.reload();
                       }else {
                           alert(data.msg);
                       }
                   }
               );
           }else {
               alert("上传失败！");
           }
        }
    });
    return false;
}