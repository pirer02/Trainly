package com.example.trainly.Objeto.Usuario;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailSender {
    private static final String SENDER_EMAIL    = "oculto";
    private static final String SENDER_PASSWORD = "oculto";

    public interface MailCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public static void send(final Context ctx,
                            final String recipientEmail,
                            final String subject,
                            final String bodyHtml,
                            final MailCallback callback) {
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host", "smtp.gmail.com");
                props.put("mail.smtp.port", "587");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                    }
                });

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(
                        Message.RecipientType.TO,
                        InternetAddress.parse(recipientEmail)
                );
                message.setSubject(subject);
                message.setContent(bodyHtml, "text/html; charset=utf-8");

                Transport.send(message);

                new Handler(Looper.getMainLooper()).post(callback::onSuccess);
            } catch (Exception e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e));
            }
        }).start();
    }
}