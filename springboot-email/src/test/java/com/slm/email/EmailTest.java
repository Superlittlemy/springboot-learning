package com.slm.email;

import com.slm.email.utils.EmailUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.mail.MessagingException;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;

@SpringBootTest
public class EmailTest {

    @Autowired
    private EmailUtil emailUtil;

    @Test
    public void sendCommonEmail() throws MessagingException {
        emailUtil.commonEmail("970696542@qq.com", "测试邮件", "你好，这是一条测试邮件，收到后请忽略！");
        emailUtil.htmlEmail("970696542@qq.com", "测试邮件", "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<body>\n" +
                "\t<p>尊敬的用户，你好：</p>\n" +
                "\t<div>你正在网站对电子邮箱进行验证</div>\n" +
                "\t<div>为验证此电子邮件地址属于你，请在验证页面输入下方的验证码：</div>\n" +
                "\t<h1>456789</h1>\n" +
                "\t<div>验证码会在此电子邮件发送后三小时失效</div>\n" +
                "\t<br>\n" +
                "\t<div><strong>你收到此电子邮件的原因：</strong><div>\n" +
                "\t<div>我们需要验证用户是否是邮箱拥有人时对电子邮件发出验证码邮件。</div>\n" +
                "\t<br>\n" +
                "\t<div>若你未提出此请求，可忽略这封电子邮件。我们仅在验证通过后才会进行下一步操作。</div>\n" +
                "\t<br>\n" +
                "\t<div>有任何问题及意见，可通过邮箱 xxx@xxx.com 联系我们</div>\n" +
                "\t<div>此邮件为自动发送，寄信人无法接收邮件，请勿直接回复。</div>\n" +
                "\t<br>\n" +
                "\t<div>祝你一切顺利。</div>\n" +
                "\t<br>\n" +
                "\t<div>管理员</div>\n" +
                "\t<div>2023-09-15</div>\n" +
                "</body>\n" +
                "</html>");
        emailUtil.staticEmail("970696542@qq.com", "测试邮件", "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<body>\n" +
                "\t<div>请你看风景</div>\n" +
                "\t<br>\n" +
                "\t<image width=\"250px\" src=\"cid:p1\">\n" +
                "</body>\n" +
                "</html>", Map.of("p1", new File("D:\\AAycsZm.jpg")));
        emailUtil.attachmentEmail("970696542@qq.com", "测试邮件", "请查看附件", Map.of("Google es-head 扩展文件.crx", new File("D:\\es-head.crx")));
    }

}
