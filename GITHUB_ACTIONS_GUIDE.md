# GitHub Actions Guide для AceTheGame

Этот документ описывает все настроенные GitHub Actions workflows для автоматической сборки, тестирования и публикации проекта AceTheGame.

## 📋 Обзор Workflows

### 1. **Main CI/CD Pipeline** (`main.yml`)
**Триггеры:** Push, Pull Request, Manual trigger, Release
**Описание:** Основной workflow для сборки и тестирования всех компонентов проекта.

**Что делает:**
- Собирает ACE (Linux) в debug и release режимах
- Собирает Modder для Linux и Windows
- Собирает Android приложения (ATG, billing-hack)
- Запускает тесты
- Создает полные release packages
- Публикует GitHub releases

**Артефакты:**
- `ace-debug`, `ace-release` - ACE binaries
- `modder-linux`, `modder-windows` - Modder JAR files
- `atg`, `billing-hack` - Android APK files
- `AceTheGame-full-release` - Полная сборка с Android NDK
- `complete-release-bundle` - Итоговый релиз пакет

### 2. **Release Creation** (`release.yml`)
**Триггеры:** Git tags (v*.*.*), Manual trigger
**Описание:** Автоматическое создание официальных релизов.

**Что делает:**
- Собирает все компоненты в release режиме
- Создает отдельные пакеты для каждого компонента
- Генерирует release notes
- Публикует GitHub release с всеми файлами

**Как использовать:**
```bash
# Автоматически при создании тега
git tag v1.0.0
git push origin v1.0.0

# Или вручную через GitHub Actions UI
```

### 3. **Docker Build** (`docker.yml`)
**Триггеры:** Push, Pull Request, Tags, Manual trigger
**Описание:** Сборка и публикация Docker образов.

**Что делает:**
- Строит multi-platform Docker images (amd64, arm64)
- Публикует в GitHub Container Registry
- Тестирует образы
- Кэширует слои для быстрой сборки

**Использование Docker образа:**
```bash
# Скачать образ
docker pull ghcr.io/kuhakupixel/acethegame:latest

# Запустить контейнер
docker run -it ghcr.io/kuhakupixel/acethegame:latest
```

### 4. **Weekly Development Build** (`weekly-build.yml`)
**Триггеры:** Schedule (каждое воскресенье в 02:00 UTC), Manual trigger
**Описание:** Еженедельные сборки для разработчиков.

**Что делает:**
- Собирает все компоненты с последними изменениями
- Создает multi-arch Android builds
- Генерирует development snapshots
- Автоматически удаляет старые weekly releases (хранит только 4 последних)

**Артефакты:**
- `weekly-snapshot-YYYY-MM-DD` - Еженедельная сборка
- Prerelease с тегом `weekly-YYYY-MM-DD`

### 5. **Security Audit** (`security-audit.yml`)
**Триггеры:** Push, Pull Request, Schedule (каждый понедельник в 09:00 UTC), Manual trigger
**Описание:** Проверка безопасности и качества кода.

**Что делает:**
- Сканирует Python код (Bandit, Safety, Semgrep)
- Анализирует C++ код (CppCheck, Clang-Tidy)
- Проверяет shell скрипты (ShellCheck)
- Проверяет зависимости Gradle
- Анализирует лицензии
- Проверяет качество кода (Black, Flake8, Pylint)

**Артефакты:**
- `security-reports` - Отчеты безопасности
- `code-quality-reports` - Отчеты качества кода
- `license-reports` - Отчеты лицензий
- `final-security-quality-summary` - Итоговый отчет

## 🚀 Как использовать

### Создание релиза

#### Автоматический релиз (рекомендуется)
1. Создайте и push git tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. GitHub Actions автоматически:
   - Соберет все компоненты
   - Создаст GitHub release
   - Опубликует все файлы

#### Ручной релиз
1. Откройте GitHub Actions → "Create Release"
2. Нажмите "Run workflow"
3. Введите версию (например, `v1.0.0`)
4. Нажмите "Run workflow"

### Скачивание файлов

#### Из GitHub Releases
1. Перейдите в [Releases](https://github.com/KuhakuPixel/AceTheGame/releases)
2. Выберите нужную версию
3. Скачайте файлы из секции "Assets"

#### Из GitHub Actions Artifacts
1. Перейдите в [Actions](https://github.com/KuhakuPixel/AceTheGame/actions)
2. Выберите нужный workflow run
3. Скачайте артефакты из секции "Artifacts"

### Доступные файлы

#### Для пользователей
- `AceTheGame-vX.X.X-linux.tar.gz` - Полная сборка
- `ACE-vX.X.X.tar.gz` - Только ACE (memory scanner)
- `Modder-vX.X.X.tar.gz` - Только Modder (APK modifier)
- `AndroidApps-vX.X.X.tar.gz` - Android приложения

#### Для разработчиков
- `ace-debug`, `ace-release` - ACE binaries
- `modder-linux`, `modder-windows` - Modder JAR files
- `weekly-snapshot-YYYY-MM-DD` - Еженедельные сборки
- `security-reports` - Отчеты безопасности

## 🔧 Настройка окружения

### Требования для сборки
- Ubuntu 22.04 (или совместимая система)
- CMake 3.10+
- GCC/G++ 7+
- OpenJDK 17
- Android SDK + NDK r25c
- Python 3.8+

### Локальная сборка
```bash
# Установка зависимостей
sudo apt-get update
sudo apt-get install -y build-essential cmake python3 openjdk-17-jdk-headless apktool

# Сборка ACE
cd ACE
mkdir build && cd build
cmake -DCMAKE_BUILD_TYPE=Release ../
make -j$(nproc)

# Сборка Modder
cd ../../Modder
./gradlew build

# Сборка Android приложений
cd ../ATG
./gradlew assembleRelease
```

## 🔍 Мониторинг и отладка

### Просмотр логов
1. Откройте GitHub Actions
2. Выберите workflow run
3. Нажмите на job для просмотра логов

### Artifacts retention
- Release artifacts: 90 дней
- Development builds: 30 дней
- Security reports: 30 дней
- Weekly snapshots: 30 дней

### Уведомления
- Все workflow failures отправляют уведомления владельцу репозитория
- Security issues создают GitHub Security Alerts
- Weekly builds создают pre-releases

## 🛡️ Безопасность

### Автоматические проверки
- Сканирование кода на уязвимости
- Проверка зависимостей
- Анализ лицензий
- Проверка качества кода

### Permissions
- Workflows имеют минимальные необходимые разрешения
- Secrets используются только для публикации
- Docker images подписываются

## 📊 Статистика

### Время выполнения (приблизительно)
- Main CI/CD: 15-25 минут
- Release creation: 20-30 минут
- Docker build: 10-15 минут
- Weekly build: 25-35 минут
- Security audit: 10-15 минут

### Размер артефактов
- Полная сборка: ~50-100 MB
- ACE binary: ~5-10 MB
- Modder JAR: ~2-5 MB
- Android APKs: ~10-20 MB каждый

## 🆘 Решение проблем

### Частые проблемы

1. **Build failures**
   - Проверьте логи GitHub Actions
   - Убедитесь что все зависимости установлены
   - Проверьте совместимость версий

2. **Android build issues**
   - Проверьте что Android SDK лицензии приняты
   - Убедитесь что NDK установлен корректно
   - Проверьте версии Gradle

3. **Docker build failures**
   - Проверьте Dockerfile
   - Убедитесь что все пути корректны
   - Проверьте доступность базового образа

### Контакты для поддержки
- GitHub Issues: [Create Issue](https://github.com/KuhakuPixel/AceTheGame/issues)
- Discord: [Join Discord](https://discord.gg/8fJh9tPVXb)

---

## 📝 Changelog

### v1.0.0 (2024)
- ✅ Обновлены все actions до последних версий
- ✅ Добавлена поддержка multi-platform Docker builds
- ✅ Настроены автоматические releases
- ✅ Добавлены security audits
- ✅ Созданы weekly development builds
- ✅ Улучшена организация артефактов