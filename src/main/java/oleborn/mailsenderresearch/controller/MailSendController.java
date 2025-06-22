package oleborn.mailsenderresearch.controller;

import lombok.RequiredArgsConstructor;
import oleborn.mailsenderresearch.mailsenderconfig.EmailService;
import oleborn.mailsenderresearch.mailsenderconfig.ProcessorEmailQueue;
import oleborn.mailsenderresearch.model.MailSendDefaultDto;
import oleborn.mailsenderresearch.model.MailSendDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Контроллер для отправки электронных писем различного типа.
 * Поддерживает отправку простых писем, писем с вложениями и HTML-писем.
 */
@RestController
@RequestMapping("/mailsender")
@RequiredArgsConstructor
@Tag(name = "Mail Sender", description = "API для отправки электронных писем")
public class MailSendController {

    private final EmailService service;
    private final ProcessorEmailQueue queue;

    /**
     * Отправляет простое текстовое письмо
     */
    @Operation(
            summary = "Отправить простое письмо",
            description = "Отправляет текстовое письмо без вложений и HTML-форматирования",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Запрос на отправку принят"),
                    @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
                    @ApiResponse(responseCode = "500", description = "Ошибка сервера при отправке")
            }
    )
    @PostMapping
    public ResponseEntity<Void> sendEmail(@RequestBody MailSendDefaultDto dto) {
        service.sendMessage(dto.to(), dto.message(), dto.subject());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Отправляет письмо с вложением
     */
    @Operation(
            summary = "Отправить письмо с вложением",
            description = "Отправляет письмо с прикрепленным файлом",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Запрос на отправку принят"),
                    @ApiResponse(responseCode = "400", description = "Неверные параметры запроса или файл не найден"),
                    @ApiResponse(responseCode = "500", description = "Ошибка сервера при отправке")
            }
    )
    @PostMapping("/withAttachment")
    public ResponseEntity<Void> sendEmailWithAttachment(@RequestBody MailSendDto dto) {
        service.sendEmailWithAttachment(dto.to(), dto.subject(), dto.message(), dto.attachmentPath());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    /**
     * Ставит HTML-письмо в очередь на отправку
     */
    @Operation(
            summary = "Поставить HTML-письмо в очередь",
            description = "Добавляет HTML-письмо в очередь для асинхронной отправки",
            responses = {
                    @ApiResponse(responseCode = "202", description = "Письмо добавлено в очередь"),
                    @ApiResponse(responseCode = "400", description = "Неверные параметры запроса"),
                    @ApiResponse(responseCode = "500", description = "Ошибка сервера при постановке в очередь")
            }
    )
    @PostMapping("/withHtml")
    public ResponseEntity<Void> sendEmailWithHtml(@RequestBody MailSendDto dto) {
        queue.enqueue(dto);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
