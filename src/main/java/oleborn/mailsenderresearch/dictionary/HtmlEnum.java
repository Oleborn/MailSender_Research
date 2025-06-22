package oleborn.mailsenderresearch.dictionary;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HtmlEnum {

    HTML_TEMPLATE("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body {
                    font-family: 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #f9f9f9;
                }
                .email-container {
                    background: #ffffff;
                    padding: 30px;
                    border-radius: 8px;
                    box-shadow: 0 2px 10px rgba(0,0,0,0.05);
                }
                .header {
                    color: #2c3e50;
                    font-size: 22px;
                    font-weight: 600;
                    margin-bottom: 20px;
                }
                .content {
                    font-size: 16px;
                    color: #555;
                    margin-bottom: 25px;
                }
                .divider {
                    height: 1px;
                    background: #eee;
                    margin: 20px 0;
                }
                .highlight {
                    background-color: #f0f7ff;
                    padding: 15px;
                    border-left: 4px solid #4a90e2;
                    margin: 15px 0;
                    border-radius: 0 4px 4px 0;
                }
                .button {
                    display: inline-block;
                    background: #4a90e2;
                    color: white !important;
                    text-decoration: none;
                    padding: 10px 20px;
                    border-radius: 4px;
                    margin: 10px 0;
                }
                .footer {
                    font-size: 14px;
                    color: #888;
                    margin-top: 30px;
                }
                .logo-container {
                    text-align: center;
                    margin: 20px 0;
                }
                .logo {
                    max-width: 200px;
                    height: auto;
                }
            </style>
        </head>
        <body>
            <div class="email-container">
                <!-- Логотип (будет заменен через cid) -->
                <div class="logo-container">
                    <img src="cid:logoImage" class="logo" alt="Company Logo">
                </div>
                
                <!-- Основной заголовок -->
                <div class="header">%s</div>
                
                <!-- Основной текст -->
                <div class="content">%s</div>
                
                <!-- Пример HTML-элементов для наглядности -->
                <div class="divider"></div>
                
                <!-- Блок с выделенным текстом -->
                <div class="highlight">
                    <strong>Важно:</strong> Это пример выделенного блока текста с левой границей.
                </div>
                
                <!-- Пример кнопки -->
                <a href="https://t.me/Java_for_beginner_dev" class="button">Пример кнопки</a>
                
                <!-- Пример списка -->
                <ul style="margin: 15px 0; padding-left: 20px;">
                    <li>Элемент списка 1</li>
                    <li>Элемент списка 2</li>
                    <li>Элемент списка 3</li>
                </ul>
                
                <!-- Подпись -->
                <div class="footer">
                    <p>С уважением,<br>Oleborn</p>
                    <p style="margin-top: 10px;">
                        <a href="https://t.me/Java_for_beginner_dev" style="color: #4a90e2; text-decoration: none;">Наш канал</a> | 
                        <a href="https://t.me/Java_for_beginner_dev" style="color: #4a90e2; text-decoration: none;">Поддержка</a>
                    </p>
                </div>
            </div>
        </body>
        </html>
        """
    );

    private final String htmlCode;

}