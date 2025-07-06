# Руководство по настройке подписи APK в GitHub Actions

## Проблема с подписью APK

Если при установке APK на Android появляется ошибка о несовместимости подписи, это означает, что подпись APK не совпадает с уже установленной версией приложения.

## Настройка GitHub Secrets

Для правильной работы подписи APK в GitHub Actions нужно настроить следующие secrets:

### 1. KEYSTORE_BASE64
- Кодированный в base64 файл keystore
- Создание: `base64 -w 0 your-keystore.jks`
- Добавить в Settings > Secrets and variables > Actions

### 2. KEYSTORE_PASSWORD
- Пароль от keystore файла
- По умолчанию: `android`

### 3. KEY_ALIAS
- Алиас ключа в keystore
- По умолчанию: `androiddebugkey`

### 4. KEY_PASSWORD
- Пароль от ключа
- По умолчанию: `android`

## Проверка существующих keystore файлов

Для проверки алиасов в существующих keystore файлах:

```bash
# Для ATG проекта
keytool -list -v -keystore ATG/app/release.keystore -storepass android

# Для billing-hack проекта
keytool -list -v -keystore billing-hack/app/release.keystore -storepass android
```

## Исправления в коде

Внесены следующие изменения:

1. **build.gradle файлы**: Настроены для использования правильного алиаса `androiddebugkey`
2. **main.yml workflow**: Добавлена отладочная информация для диагностики проблем
3. **Согласованность**: Все настройки теперь используют одинаковые значения

## Рекомендации

1. **Для production**: Создайте собственный keystore с уникальными паролями
2. **Для testing**: Используйте существующие debug keystore файлы
3. **Безопасность**: Никогда не коммитьте production keystore файлы в репозиторий

## Отладка

Workflow теперь выводит отладочную информацию:
- Информация о keystore файле
- Параметры подписи (пароли скрыты)
- Используемые алиасы

Эта информация поможет диагностировать проблемы с подписью APK.