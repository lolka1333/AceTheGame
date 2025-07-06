# 🔧 Исправление Docker: Repository Name Lowercase

## ❌ Проблема:

```bash
docker: invalid reference format: repository name (lolka1333/AceTheGame) must be lowercase
```

**Причина:** Docker требует, чтобы имена репозиториев были в нижнем регистре, но GitHub Actions использовал переменную `${{ github.repository }}` которая содержит заглавные буквы (`lolka1333/AceTheGame`).

## ✅ Исправление:

### 1. Добавлен step для преобразования в lowercase

**В каждый Docker workflow добавлен:**
```yaml
- name: Set lowercase repository name
  run: |
    echo "REPO_NAME=$(echo ${{ github.repository }} | tr '[:upper:]' '[:lower:]')" >> $GITHUB_ENV
```

**Результат:** `lolka1333/AceTheGame` → `lolka1333/acethegame`

### 2. Заменены все ссылки на image names

**Было:**
```yaml
images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
docker run --rm ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ steps.meta.outputs.version }}
```

**Стало:**
```yaml
images: ${{ env.REGISTRY }}/${{ env.REPO_NAME }}
docker run --rm ${{ env.REGISTRY }}/${{ env.REPO_NAME }}:${{ steps.meta.outputs.version }}
```

### 3. Удалена ненужная переменная

**Удалено из env:**
```yaml
env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}  # ❌ Удалено
```

**Теперь:**
```yaml
env:
  REGISTRY: ghcr.io
```

## 📁 Исправленные файлы:

1. **`.github/workflows/docker.yml`**
   - Добавлен step для lowercase conversion
   - Заменены все ссылки на `${{ env.REPO_NAME }}`
   - Удалена переменная `IMAGE_NAME`

2. **`.github/workflows/docker-multiplatform.yml`**
   - Добавлен step для lowercase conversion
   - Заменены все ссылки на `${{ env.REPO_NAME }}`
   - Исправлены примеры использования в documentation

3. **Документация:**
   - `.github/README.md`
   - `GITHUB_ACTIONS_GUIDE.md`
   - `SETUP_COMPLETE.md`

## 🎯 Результат:

### ✅ Теперь работает:
```bash
# Основные образы
docker pull ghcr.io/lolka1333/acethegame:latest
docker run -it ghcr.io/lolka1333/acethegame:latest

# Multi-platform образы
docker pull ghcr.io/lolka1333/acethegame:latest-multiplatform
docker run -it --platform linux/amd64 ghcr.io/lolka1333/acethegame:latest-multiplatform
docker run -it --platform linux/arm64 ghcr.io/lolka1333/acethegame:latest-multiplatform
```

### 🏷️ Доступные теги:
- `latest` - последняя сборка из main branch
- `v1.0.0` - tagged releases
- `pr-123` - pull request builds
- `latest-multiplatform` - multi-platform builds
- `sha-abcd123` - commit-specific builds

## 🔍 Проверка исправления:

После применения исправлений Docker команды должны работать без ошибок:

```bash
# Тест основного образа
docker pull ghcr.io/lolka1333/acethegame:latest
docker run --rm ghcr.io/lolka1333/acethegame:latest python3 --version

# Тест multi-platform образа (если доступен)
docker pull ghcr.io/lolka1333/acethegame:latest-multiplatform
docker run --rm ghcr.io/lolka1333/acethegame:latest-multiplatform python3 --version
```

## 📝 Примечания:

1. **Автоматическое преобразование:** Все будущие builds будут автоматически использовать lowercase имена
2. **Обратная совместимость:** Старые образы с неправильными именами не будут работать
3. **Консистентность:** Все workflow теперь используют одинаковый подход для именования

🎉 **Проблема решена!** Docker образы теперь собираются и используются с правильными lowercase именами.