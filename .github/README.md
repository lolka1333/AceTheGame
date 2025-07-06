# GitHub Actions для AceTheGame

## ✅ Настроенные Workflows

### 🔧 [Main CI/CD](.github/workflows/main.yml)
- **Триггеры:** Push, PR, Manual, Release
- **Описание:** Основная сборка и тестирование всех компонентов
- **Артефакты:** ACE binaries, Modder JAR, Android APKs

### 🚀 [Release Creation](.github/workflows/release.yml)
- **Триггеры:** Git tags (v*.*.*), Manual
- **Описание:** Автоматическое создание официальных релизов
- **Результат:** GitHub Release с полными пакетами

### 🐳 [Docker Build](.github/workflows/docker.yml)
- **Триггеры:** Push, PR, Tags, Manual
- **Описание:** Multi-platform Docker образы
- **Результат:** Images в GitHub Container Registry

### 📅 [Weekly Builds](.github/workflows/weekly-build.yml)
- **Триггеры:** Schedule (воскресенье 02:00 UTC), Manual
- **Описание:** Еженедельные development snapshots
- **Результат:** Pre-release с последними изменениями

### 🔐 [Security Audit](.github/workflows/security-audit.yml)
- **Триггеры:** Push, PR, Schedule (понедельник 09:00 UTC), Manual
- **Описание:** Проверка безопасности и качества кода
- **Результат:** Отчеты безопасности и качества

## 🎯 Ключевые возможности

- ✅ **Последние версии Actions** - все используют latest stable versions
- ✅ **Multi-platform builds** - Linux, Windows, Android (multi-arch)
- ✅ **Автоматические релизы** - при создании git tags
- ✅ **Docker support** - multi-platform образы
- ✅ **Security scanning** - автоматические проверки безопасности
- ✅ **Quality checks** - анализ качества кода
- ✅ **Artifacts retention** - все файлы доступны для скачивания
- ✅ **Weekly snapshots** - для разработчиков

## 🚀 Быстрый старт

### Создать релиз:
```bash
git tag v1.0.0
git push origin v1.0.0
```

### Скачать файлы:
- [Releases](https://github.com/KuhakuPixel/AceTheGame/releases) - официальные релизы
- [Actions](https://github.com/KuhakuPixel/AceTheGame/actions) - artifacts из сборок

### Запустить Docker:
```bash
docker pull ghcr.io/kuhakupixel/acethegame:latest
docker run -it ghcr.io/kuhakupixel/acethegame:latest
```

## 📖 Документация

Полная документация: [GITHUB_ACTIONS_GUIDE.md](../GITHUB_ACTIONS_GUIDE.md)

## 🔄 Workflow Status

[![AceTheGame CI](https://github.com/KuhakuPixel/AceTheGame/actions/workflows/main.yml/badge.svg)](https://github.com/KuhakuPixel/AceTheGame/actions/workflows/main.yml)
[![Create Release](https://github.com/KuhakuPixel/AceTheGame/actions/workflows/release.yml/badge.svg)](https://github.com/KuhakuPixel/AceTheGame/actions/workflows/release.yml)
[![Docker Build](https://github.com/KuhakuPixel/AceTheGame/actions/workflows/docker.yml/badge.svg)](https://github.com/KuhakuPixel/AceTheGame/actions/workflows/docker.yml)
[![Weekly Build](https://github.com/KuhakuPixel/AceTheGame/actions/workflows/weekly-build.yml/badge.svg)](https://github.com/KuhakuPixel/AceTheGame/actions/workflows/weekly-build.yml)
[![Security Audit](https://github.com/KuhakuPixel/AceTheGame/actions/workflows/security-audit.yml/badge.svg)](https://github.com/KuhakuPixel/AceTheGame/actions/workflows/security-audit.yml)