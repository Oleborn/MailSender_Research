package oleborn.mailsenderresearch.model;

public record MailSendDto(
        String[] to,
        String subject,
        String message,
        String attachmentPath
) {
}
