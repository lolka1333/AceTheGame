# Решение ошибки Docker: repository name must be lowercase

## Проблема

При выполнении команды:
```bash
docker run --rm ghcr.io/lolka1333/AceTheGame:pr-1 python3 --version
```

Получаете ошибку:
```
docker: invalid reference format: repository name (lolka1333/AceTheGame) must be lowercase
```

## Причина

Docker требует, чтобы имена репозиториев содержали **только строчные буквы**. В вашем случае:
- `AceTheGame` содержит заглавные буквы `A`, `T` и `G`
- Это нарушает правила именования Docker-репозиториев

## Решение

Замените имя репозитория на строчные буквы:

### Неправильно ❌
```bash
docker run --rm ghcr.io/lolka1333/AceTheGame:pr-1 python3 --version
```

### Правильно ✅
```bash
docker run --rm ghcr.io/lolka1333/acethegame:pr-1 python3 --version
```

## Примечание

После исправления имени репозитория команда корректно пытается загрузить образ, но может потребоваться:
1. Аутентификация в GitHub Container Registry
2. Проверка, что репозиторий существует и доступен
3. Правильные права доступа к репозиторию

Если репозиторий приватный, нужно выполнить:
```bash
docker login ghcr.io
```

И ввести токен доступа GitHub.