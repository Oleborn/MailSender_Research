package oleborn.mailsenderresearch.mailsenderconfig;

public interface EmailService {

    void sendMessage(String toAddressEmail, String messageToAddress, String subject);

    void sendEmailWithAttachment(String[] toAddressEmail, String subject, String text, String attachmentPath);

    void sendHtmlEmailWithAttachment(String[] toAddressEmail, String subject, String htmlContent, String attachmentPath);

}
