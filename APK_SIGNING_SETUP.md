# Настройка подписи APK файлов

## Создание keystore с помощью keytool

1. Создайте keystore файл с современными алгоритмами:
```bash
keytool -genkey -v -keystore app-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias app-key -sigalg SHA256withRSA -storetype PKCS12
```

2. Следуйте инструкциям для заполнения информации:
   - Введите пароль для keystore
   - Введите пароль для ключа
   - Заполните информацию о сертификате (имя, организация, город, регион, код страны)

## Настройка GitHub Secrets

После создания keystore файла, добавьте следующие secrets в ваш GitHub repository:

### 1. APP_KEYSTORE_BASE64
Конвертируйте keystore файл в base64:
```bash
base64 -w 0 app-release-key.jks
```
Скопируйте результат и добавьте как secret `APP_KEYSTORE_BASE64`

### 2. APP_KEYSTORE_PASSWORD
Пароль для keystore файла

### 3. APP_KEY_PASSWORD
Пароль для ключа

### 4. APP_KEYSTORE_ALIAS
Alias ключа (например, "app-key")

## Как работает подпись в CI/CD

Подпись APK файлов происходит:
- В job `ATG` и `billing-hack` - только при release или manual dispatch
- В job `android-release` - для всех APK файлов в директории release

### Условия выполнения
```yaml
if: github.event_name == 'release' || github.event_name == 'workflow_dispatch'
```

### Процесс подписи
1. Декодирование base64 keystore в файл
2. Подпись APK с помощью jarsigner
3. Верификация подписи
4. Удаление keystore файла (для безопасности)

## Команды для проверки подписи локально

```bash
# Проверка подписи APK
jarsigner -verify -verbose -certs your-app.apk

# Просмотр информации о сертификате
keytool -list -v -keystore app-release-key.jks
```

## Примечания

- Keystore файл содержит приватные ключи, поэтому храните его в безопасности
- Используйте сильные пароли для keystore и ключей
- Сохраните резервную копию keystore файла
- Не добавляйте keystore файл в git repository

## Решение распространенных проблем

### Ошибка "SHA1 algorithm is considered a security risk"
Эта ошибка возникает при использовании устаревших алгоритмов. Решение:
- Используйте `-sigalg SHA256withRSA -digestalg SHA256` при подписи
- Создавайте keystore с `-sigalg SHA256withRSA`

### Ошибка "invalid SHA-256 signature file digest"
Эта ошибка возникает, когда APK уже подписан. Решение:
1. Удалите существующие подписи перед повторной подписью:
```bash
zip -d app.apk "META-INF/*.SF" "META-INF/*.RSA" "META-INF/*.DSA" "META-INF/MANIFEST.MF"
```
2. Затем подпишите APK заново

### Рекомендации по zipalign
После подписи APK рекомендуется выполнить zipalign:
```bash
zipalign -v 4 app-signed.apk app-final.apk
```