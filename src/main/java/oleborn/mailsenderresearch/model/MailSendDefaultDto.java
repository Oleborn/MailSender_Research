package oleborn.mailsenderresearch.model;

public record MailSendDefaultDto(
        String to,
        String subject,
        String message,
        String attachmentPath
) {
}
