package oleborn.mailsenderresearch.mailsenderconfig;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleborn.mailsenderresearch.dictionary.HtmlEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * Реализация сервиса для отправки электронных писем с использованием Spring Framework.
 * <p>
 * Сервис предоставляет три основных метода для отправки писем различной сложности:
 * от простых текстовых до HTML-писем с вложениями и встроенными изображениями.
 * <p>
 * <b>Используемые классы:</b>
 * <ul>
 *     <li>{@link JavaMailSender} - основной интерфейс Spring для отправки почты</li>
 *     <li>{@link SimpleMailMessage} - класс для простых текстовых сообщений</li>
 *     <li>{@link MimeMessage} - класс для сложных сообщений (MIME) с поддержкой вложений и HTML</li>
 *     <li>{@link MimeMessageHelper} - вспомогательный класс для работы с MimeMessage</li>
 *     <li>{@link FileSystemResource} - представление файла как ресурса для вложений</li>
 *     <li>{@link MessagingException} - исключение при ошибках работы с почтой</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    /**
     * Экземпляр JavaMailSender, предоставляемый Spring Boot Auto-Configuration.
     * <p>
     * Этот компонент автоматически настраивается на основе свойств из application.properties/yml
     * (spring.mail.host, spring.mail.port, spring.mail.username, spring.mail.password и др.)
     * и инкапсулирует всю логику подключения к SMTP-серверу и отправки сообщений.
     */
    private final JavaMailSender mailSender;

    /**
     * Email-адрес отправителя, загружаемый из конфигурации приложения.
     * <p>
     * Значение берется из свойства spring.mail.username, которое должно быть
     * определено в конфигурационных файлах приложения.
     */
    @Value("${spring.mail.username}")
    private String myEmail;

    /**
     * Отправляет простое текстовое письмо на указанный адрес.
     * <p>
     * Используется для базовых уведомлений без форматирования или вложений.
     *
     * @param toAddressEmail email-адрес получателя (не может быть null или пустым)
     * @param messageToAddress текст сообщения (тело письма)
     * @param subject тема письма (заголовок)
     * @throws IllegalArgumentException если параметры toAddressEmail, messageToAddress или subject null/пустые
     */
    @Override
    public void sendMessage(String toAddressEmail, String messageToAddress, String subject) {
        log.info("Начало отправки email на адрес {}", toAddressEmail);

        // Валидация входных параметров
        if (toAddressEmail == null || toAddressEmail.isEmpty()) {
            throw new IllegalArgumentException("Адрес получателя не может быть пустым");
        }

        // Создание и настройка простого текстового сообщения
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toAddressEmail);       // Устанавливаем получателя
        message.setSubject(subject);        // Задаем тему письма
        message.setText(messageToAddress);  // Устанавливаем текст сообщения
        message.setFrom(myEmail);           // Указываем отправителя (из конфигурации)

        // Отправка сообщения через JavaMailSender
        mailSender.send(message);
        log.debug("Email успешно отправлен на адрес {}", toAddressEmail);
    }

    /**
     * Отправляет письмо с вложением на несколько адресов.
     * <p>
     * Поддерживает отправку одного файла-вложения нескольким получателям.
     *
     * @param toAddressEmail массив email-адресов получателей (не может быть null или пустым)
     * @param subject тема письма
     * @param text текст сообщения
     * @param attachmentPath абсолютный путь к файлу для вложения (должен существовать)
     * @throws RuntimeException если:
     *         - возникает ошибка при создании/отправке сообщения (MessagingException)
     *         - файл вложения не существует или недоступен
     *         - параметры toAddressEmail или subject null/пустые
     */
    @Override
    public void sendEmailWithAttachment(String[] toAddressEmail, String subject, String text, String attachmentPath) {
        try {
            log.info("Отправка email с вложением на адреса {}", (Object) toAddressEmail);

            // Создание MIME-сообщения (поддерживает multipart-сообщения)
            MimeMessage message = mailSender.createMimeMessage();

            // Инициализация helper с поддержкой:
            // - multipart (true) - для вложений
            // - UTF-8 - кодировка текста
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Настройка основных параметров письма
            helper.setFrom(myEmail);                // Отправитель
            helper.setTo(toAddressEmail);           // Получатели (массив)
            helper.setSubject(subject);             // Тема
            helper.setText(text);                   // Текст сообщения

            // Создание ресурса для файла-вложения
            File attachmentFile = new File(attachmentPath);
            if (!attachmentFile.exists()) {
                throw new IllegalArgumentException("Файл вложения не существует: " + attachmentPath);
            }
            FileSystemResource file = new FileSystemResource(attachmentFile);

            // Добавление вложения с указанием имени файла для получателя
            helper.addAttachment("Attachment.txt", file);

            // Отправка подготовленного сообщения
            mailSender.send(message);
            log.debug("Email с вложением успешно отправлен на адреса {}", (Object) toAddressEmail);
        } catch (MessagingException e) {
            log.error("Ошибка при отправке email с вложением на адреса {}", (Object) toAddressEmail, e);
            throw new RuntimeException("Не удалось отправить email с вложением", e);
        }
    }

    /**
     * Отправляет HTML-письмо с возможными вложениями и встроенными изображениями.
     * <p>
     * Поддерживает сложные сценарии отправки:
     * - HTML-форматирование
     * - Встроенные изображения (inline)
     * - Файловые вложения
     *
     * @param toAddressEmail массив email-адресов получателей (не может быть null или пустым)
     * @param subject тема письма
     * @param htmlContent HTML-содержимое письма
     * @param attachmentPath путь к файлу вложения (может быть null, если вложение не требуется)
     * @throws RuntimeException если:
     *         - возникает ошибка при создании/отправке сообщения (MessagingException)
     *         - файл логотипа или вложения не существует
     *         - параметры toAddressEmail или subject null/пустые
     */
    @Override
    public void sendHtmlEmailWithAttachment(String[] toAddressEmail, String subject, String htmlContent, String attachmentPath) {
        try {
            log.info("Отправка HTML email с вложением на адреса {}", (Object) toAddressEmail);

            // Создание MIME-сообщения с поддержкой multipart
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Настройка основных параметров письма
            helper.setFrom(myEmail);
            helper.setTo(toAddressEmail);
            helper.setSubject(subject);

            // Подготовка встроенного изображения (логотипа)
            File logoFile = new File("Logo.png");
            if (!logoFile.exists()) {
                throw new IllegalArgumentException("Файл логотипа не существует: Logo.png");
            }
            FileSystemResource logoResource = new FileSystemResource(logoFile);
            String contentId = "logoImage"; // Уникальный ID для ссылки в HTML

            // Форматирование HTML с использованием шаблона
            // Шаблон должен содержать плейсхолдеры для:
            // - темы письма (%1$s)
            // - основного контента (%2$s)
            // - ссылки на изображение (%3$s)
            String formattedHtml = HtmlEnum.HTML_TEMPLATE.getHtmlCode()
                    .formatted(subject, htmlContent, "cid:" + contentId);

            // Установка HTML-контента с флагом true (HTML включен)
            helper.setText(formattedHtml, true);

            // Добавление встроенного изображения с привязкой к contentId
            helper.addInline(contentId, logoResource);

            // Обработка вложения (если указан путь)
            if (attachmentPath != null && !attachmentPath.isEmpty()) {
                File attachmentFile = new File(attachmentPath);
                if (!attachmentFile.exists()) {
                    throw new IllegalArgumentException("Файл вложения не существует: " + attachmentPath);
                }
                FileSystemResource file = new FileSystemResource(attachmentFile);
                helper.addAttachment("Attachment.txt", file);
            }

            // Отправка сообщения
            mailSender.send(message);
            log.debug("HTML email с вложением успешно отправлен на адреса {}", (Object) toAddressEmail);
        } catch (MessagingException e) {
            log.error("Ошибка при отправке HTML email на адреса {}", (Object) toAddressEmail, e);
            throw new RuntimeException("Не удалось отправить HTML email", e);
        }
    }
}