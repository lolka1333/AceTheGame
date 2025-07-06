# GitHub Actions Secrets Configuration for APK Signing

## Описание

Настройка GitHub Actions secrets для автоматической подписи APK файлов в режиме release с суффиксом "-signed" в названии.

## Необходимые GitHub Actions Secrets

Для настройки подписи APK через GitHub Actions нужно добавить следующие secrets в репозиторий:

### 1. KEYSTORE_BASE64
Base64-закодированный keystore файл

**Как создать:**
```bash
# Создать keystore файл
keytool -genkey -v -keystore release.keystore -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 -storepass your_store_password -keypass your_key_password -dname "CN=Your Name,O=Your Organization,C=US"

# Конвертировать в base64
base64 -w 0 release.keystore > keystore_base64.txt
```

### 2. KEYSTORE_PASSWORD
Пароль от keystore файла

### 3. KEY_ALIAS
Alias ключа в keystore (например: `androiddebugkey`)

### 4. KEY_PASSWORD
Пароль от ключа в keystore

## Настройка Secrets в GitHub

1. Перейдите в репозиторий на GitHub
2. Откройте **Settings** → **Secrets and variables** → **Actions**
3. Нажмите **New repository secret**
4. Добавьте каждый secret с соответствующим именем и значением

## Как это работает

### Конфигурация build.gradle

```gradle
signingConfigs {
    release {
        if (project.hasProperty('KEYSTORE_FILE')) {
            storeFile file(KEYSTORE_FILE)
            storePassword KEYSTORE_PASSWORD
            keyAlias KEY_ALIAS
            keyPassword KEY_PASSWORD
        } else {
            // Fallback to local keystore for local development
            storeFile file('release.keystore')
            storePassword 'android'
            keyAlias 'androiddebugkey'
            keyPassword 'android'
        }
    }
}

buildTypes {
    release {
        signingConfig signingConfigs.release
        // ... другие настройки
    }
}

// Именование APK файлов
applicationVariants.all { variant ->
    variant.outputs.all { output ->
        if (variant.buildType.name == 'release') {
            outputFileName = "${variant.name}-signed.apk"
        }
    }
}
```

### Workflow Configuration

```yaml
- name: Setup keystore for signing
  if: github.event_name == 'release' || github.event_name == 'workflow_dispatch'
  run: |
    if [ -n "${{ secrets.KEYSTORE_BASE64 }}" ]; then
      echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > ./ATG/app/release.keystore
      echo "Using keystore from secrets"
    else
      echo "No keystore secret found, using local keystore"
    fi

- name: build release
  working-directory: ./ATG
  env:
    KEYSTORE_FILE: release.keystore
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD || 'android' }}
    KEY_ALIAS: ${{ secrets.KEY_ALIAS || 'androiddebugkey' }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD || 'android' }}
  run: ./gradlew assembleRelease
```

## Результат

После настройки secrets:

✅ **При release или workflow_dispatch:**
- APK собирается в режиме **release**
- APK **подписывается** с использованием keystore из secrets
- APK файлы получают суффикс **"-signed"**
- Название файла: `release-signed.apk`

✅ **При обычных push/PR:**
- APK собирается в режиме **debug**
- Используется локальный keystore для разработки
- Название файла: `debug.apk`

## Безопасность

- Keystore файл хранится в GitHub secrets в base64 формате
- Пароли хранятся в secrets и не отображаются в логах
- Локальные keystore файлы остаются для разработки
- Production keystore используется только в GitHub Actions

## Проверка

Чтобы убедиться, что подпись работает правильно:

```bash
# Проверить подпись APK
apksigner verify --verbose release-signed.apk

# Посмотреть информацию о сертификате
keytool -printcert -jarfile release-signed.apk
```

## Создание Production Keystore

Для production использования создайте безопасный keystore:

```bash
keytool -genkey -v -keystore production.keystore \
  -alias production-key \
  -keyalg RSA \
  -keysize 2048 \
  -validity 25000 \
  -storepass "SECURE_STORE_PASSWORD" \
  -keypass "SECURE_KEY_PASSWORD" \
  -dname "CN=Company Name,O=Organization,L=City,ST=State,C=Country"
```

**Важно:** Сохраните keystore файл и пароли в безопасном месте!