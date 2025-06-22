package oleborn.mailsenderresearch.mailsenderconfig;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleborn.mailsenderresearch.model.MailSendDto;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Компонент для асинхронной обработки и отправки email-сообщений через очередь.
 * Реализует паттерн "Производитель-Потребитель" с использованием блокирующей очереди
 * и пула потоков для параллельной обработки. Поддерживает graceful shutdown при
 * остановке приложения.
 *
 * <p><b>Архитектурные компоненты:</b>
 * <ul>
 *   <li><b>Блокирующая очередь</b> - буферизирует входящие запросы, обеспечивая
 *       потокобезопасное взаимодействие между производителями и потребителями</li>
 *   <li><b>Пул потоков</b> - выполняет фактическую обработку email-запросов</li>
 *   <li><b>Виртуальные потоки</b> - обеспечивают эффективную диспетчеризацию задач
 *       без значительных накладных расходов</li>
 * </ul>
 *
 * <p><b>Используемые классы:</b>
 * <ul>
 *   <li>{@link BlockingQueue} - потокобезопасная очередь с поддержкой блокирующих операций</li>
 *   <li>{@link LinkedBlockingQueue} - реализация BlockingQueue на связанных узлах</li>
 *   <li>{@link ThreadPoolTaskExecutor} - пул потоков Spring для асинхронного выполнения задач</li>
 *   <li>{@link MailSendDto} - DTO с данными для отправки email (получатель, тема, сообщение)</li>
 *   <li>{@link EmailService} - сервис для отправки email-сообщений</li>
 * </ul>
 *
 * @author [Ваше имя]
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessorEmailQueue {

    /**
     * Блокирующая очередь для хранения email-запросов.
     * <p><b>Характеристики:</b>
     * <ul>
     *   <li>Максимальная емкость: 1000 элементов</li>
     *   <li>Реализация: {@link LinkedBlockingQueue} - оптимальна для высоконагруженных
     *       producer-consumer сценариев</li>
     *   <li>При переполнении новые элементы отклоняются (политика drop)</li>
     * </ul>
     */
    private final BlockingQueue<MailSendDto> emailQueue = new LinkedBlockingQueue<>(1000);

    /**
     * Сервис для отправки email-сообщений.
     * <p>Инкапсулирует логику:
     * <ul>
     *   <li>Формирования MIME-сообщений</li>
     *   <li>Работы с SMTP-сервером</li>
     *   <li>Обработки вложений</li>
     * </ul>
     */
    private final EmailService emailService;

    /**
     * Пул потоков для выполнения задач отправки email.
     * <p><b>Конфигурация:</b>
     * <ul>
     *   <li>Размер пула должен быть настроен в соответствии с нагрузкой</li>
     *   <li>Используется ThreadPoolTaskExecutor для интеграции с Spring</li>
     *   <li>Поддерживает graceful shutdown через {@link #shutdown()}</li>
     * </ul>
     */
    private final ThreadPoolTaskExecutor emailTaskExecutor;

    /**
     * Флаг состояния работы компонента.
     * <p>Используется для корректного завершения потока-диспетчера:
     * <ul>
     *   <li>true - компонент активен и обрабатывает сообщения</li>
     *   <li>false - инициирован процесс остановки</li>
     * </ul>
     * <p><b>Особенности:</b>
     * <ul>
     *   <li>Объявлен как volatile для гарантии видимости изменений между потоками</li>
     *   <li>Изменяется только в методах {@link #init()} и {@link #shutdown()}</li>
     * </ul>
     */
    private volatile boolean running = true;

    /**
     * Инициализирует компонент после создания бина.
     * <p><b>Выполняемые действия:</b>
     * <ul>
     *   <li>Запускает поток-диспетчер в виртуальном потоке</li>
     *   <li>Логирует начало работы компонента</li>
     * </ul>
     *
     * @throws IllegalStateException если не удалось инициализировать поток-диспетчер
     */
    @PostConstruct
    public void init() {
        log.info("Initializing ProcessorEmailQueue with queue capacity: {} and executor: {}",
                emailQueue.remainingCapacity(), emailTaskExecutor.getThreadNamePrefix());
        startDispatcherThread();
    }

    /**
     * Обеспечивает graceful shutdown компонента.
     * <p><b>Последовательность остановки:</b>
     * <ol>
     *   <li>Устанавливает флаг running=false для остановки диспетчера</li>
     *   <li>Завершает работу пула потоков</li>
     *   <li>Логирует завершение работы</li>
     * </ol>
     *
     * <p><b>Обратите внимание:</b> Метод может быть вызван параллельно с обработкой
     * сообщений, поэтому важно корректно обрабатывать прерывания.
     */
    @PreDestroy
    public void shutdown() {
        log.info("Initiating shutdown for ProcessorEmailQueue. Queue size: {}", emailQueue.size());
        running = false; // Сигнал для остановки диспетчера

        // Завершаем пул потоков с ожиданием завершения текущих задач
        emailTaskExecutor.shutdown();
        try {
            if (!emailTaskExecutor.getThreadPoolExecutor().awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("Forcing executor shutdown after timeout");
                emailTaskExecutor.getThreadPoolExecutor().shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Shutdown interrupted", e);
        }

        log.info("Shutdown completed. Pending emails: {}", emailQueue.size());
    }

    /**
     * Добавляет запрос на отправку email в очередь обработки.
     *
     * <p><b>Поведение при переполнении:</b>
     * <ul>
     *   <li>При заполненной очереди возвращает false (неблокирующее поведение)</li>
     *   <li>Логирует предупреждение о переполнении</li>
     * </ul>
     *
     * @param request DTO с данными для отправки email. Не может быть null.
     * @throws IllegalArgumentException если request равен null
     */
    public void enqueue(MailSendDto request) {
        Objects.requireNonNull(request, "Email request cannot be null");

        if (emailQueue.offer(request)) {
            log.debug("Email request enqueued for recipient: {}. Queue size: {}",
                    request.to(), emailQueue.size());
        } else {
            log.warn("Email queue capacity exceeded ({}). Request for {} rejected",
                    emailQueue.size(), request.to());
            // В реальном приложении здесь может быть стратегия повторной попытки
            // или альтернативная обработка
        }
    }

    /**
     * Запускает поток-диспетчер в виртуальном потоке.
     * <p><b>Логика работы диспетчера:</b>
     * <ol>
     *   <li>В бесконечном цикле извлекает сообщения из очереди</li>
     *   <li>Для каждого сообщения создает задачу в пуле потоков</li>
     *   <li>Обрабатывает прерывания и завершает работу при running=false</li>
     * </ol>
     *
     * <p><b>Особенности реализации:</b>
     * <ul>
     *   <li>Использует {@link Thread#ofVirtual()} для эффективной работы</li>
     *   <li>Обрабатывает {@link InterruptedException} корректно</li>
     * </ul>
     */
    private void startDispatcherThread() {
        Thread.ofVirtual().name("email-dispatcher").start(() -> {
            log.info("Dispatcher thread started. Using virtual thread: {}",
                    Thread.currentThread().isVirtual());

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    MailSendDto request = emailQueue.take();
                    log.trace("Dispatching email task for: {}", request.to());

                    emailTaskExecutor.execute(() -> processEmail(request));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Dispatcher thread interrupted during queue take", e);
                }
            }

            log.info("Dispatcher thread stopped. Running: {}, Interrupted: {}",
                    running, Thread.currentThread().isInterrupted());
        });
    }

    /**
     * Обрабатывает запрос на отправку email.
     * <p><b>Этапы обработки:</b>
     * <ol>
     *   <li>Логирование начала обработки</li>
     *   <li>Вызов emailService для отправки сообщения</li>
     *   <li>Обработка и логирование ошибок</li>
     * </ol>
     *
     * <p><b>Обработка ошибок:</b>
     * <ul>
     *   <li>Все исключения перехватываются и логируются</li>
     *   <li>Ошибка не прерывает работу обработчика</li>
     * </ul>
     *
     * @param request DTO с данными для отправки email. Гарантируется не-null.
     */
    private void processEmail(MailSendDto request) {
        final String[] recipient = request.to();
        log.debug("Starting email processing for: {}", recipient);

        try {
            emailService.sendHtmlEmailWithAttachment(
                    recipient,
                    request.subject(),
                    request.message(),
                    request.attachmentPath());

            log.info("Email successfully sent to {}", recipient);
        } catch (MailPreparationException e) {
            log.error("Email preparation failed for {}: {}", recipient, e.getMessage());
        } catch (MailSendException e) {
            log.error("Email sending failed for {}: {}", recipient, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while sending email to {}", recipient, e);
        }
    }
}