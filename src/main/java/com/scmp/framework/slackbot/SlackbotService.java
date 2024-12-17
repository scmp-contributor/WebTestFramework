package com.scmp.framework.slackbot;

import com.scmp.framework.context.ApplicationContextProvider;
import com.scmp.framework.context.FrameworkConfigs;
import com.scmp.framework.context.RunTimeContext;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SlackbotService {

    private static final Logger frameworkLogger = LoggerFactory.getLogger(SlackbotService.class);
    private final OkHttpClient client;

    public SlackbotService() {
        ApplicationContext context = ApplicationContextProvider.getApplicationContext();
        client = new OkHttpClient();
    }

    /**
     * This function send a message to Slack channel
     *
     * @param webhookUrl channel id to send message
     * @param message    message to be sent
     * @return true if message is sent successfully, false otherwise
     */
    public boolean sendMessageToSlackChannel(String webhookUrl, String message) {

        try {

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "{\"text\":\"" + message + "\"}");
            Request request = new Request.Builder().url(webhookUrl).post(requestBody).build();
            Response response = client.newCall(request).execute();

            if (response.isSuccessful()) {
                frameworkLogger.info("Message sent to Slack channel successfully");
                return true;
            } else {
                frameworkLogger.info("Message sent to Slack channel failed: " + response.code());
                return false;
            }

        } catch (Exception e) {
            frameworkLogger.info("Message sent to Slack channel failed: " + e.getMessage());
            return false;
        }
    }
}
