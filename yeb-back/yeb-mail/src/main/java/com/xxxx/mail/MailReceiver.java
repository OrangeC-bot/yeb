package com.xxxx.mail;

import com.rabbitmq.client.Channel;
import com.xxxx.server.pojo.Employee;
import com.xxxx.server.pojo.MailConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Date;

/***
 *@author:Czy
 *@data:2022/12/24
 ***/
@Component
public class MailReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(MailReceiver.class);

    @Autowired
    private JavaMailSender javaMailSender;
    @Autowired
    private MailProperties mailProperties;
    @Autowired
    private TemplateEngine templateEngine;
    @Autowired
    private RedisTemplate redisTemplate;

    @RabbitListener(queues = MailConstants.MAIL_QUEUE_NAME)
    public void handler(Message message, Channel channel) {
        Employee employee = (Employee) message.getPayload();
        MessageHeaders headers = message.getHeaders();
        //消息序号
        long tag = (long) headers.get(AmqpHeaders.DELIVERY_TAG);
        String megId = (String) headers.get("spring_returned_message_correlation");
        HashOperations hashOperations = redisTemplate.opsForHash();
        try {
            if (hashOperations.entries("mail_log").containsKey(megId)) {
                LOGGER.error("消息已经被消费");
                /**
                 * 手动确认消息
                 * tag：消息序号
                 * multiple:是否确认多条
                 */
                channel.basicAck(tag, false);
                return;
            }
            MimeMessage msg = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg);
            helper.setFrom(mailProperties.getUsername());
            helper.setTo(employee.getEmail());
            //主题
            helper.setSubject("入职欢迎邮件");
            helper.setSentDate(new Date());
            Context context = new Context();
            context.setVariable("name", employee.getName());
            context.setVariable("posName", employee.getPosition().getName());
            context.setVariable("joblevelName", employee.getJoblevel().getName());
            context.setVariable("departmentName", employee.getDepartment().getName());
            String mail = templateEngine.process("mail", context);
            helper.setText(mail, true);
            //发送邮件
            javaMailSender.send(msg);
            LOGGER.info("邮件发送成功!");
            //将消息ID存入redis
            hashOperations.put("mail_log", megId, "OK");
            channel.basicAck(tag, false);
        } catch (Exception e) {
            /**
             * 手动确认消息
             * tag:消息序号
             * multiple:是否确认多条
             * requeue:是否退回到队列
             */
            try {
                channel.basicNack(tag, false, true);
            } catch (IOException ex) {
                LOGGER.error("邮件发送失败 ===========> {}", e.getMessage());
            }
            LOGGER.error("邮件发送失败 ===========> {}", e.getMessage());
        }
    }

}
