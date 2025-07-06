# Генерация ключей через keytool в Termux (JDK21)

## Установка необходимых пакетов

```bash
# Обновляем пакеты
pkg update && pkg upgrade

# Устанавливаем OpenJDK 21
pkg install openjdk-21

# Проверяем установку
java -version
keytool -help
```

## Генерация keystore

### Базовая команда
```bash
keytool -genkey -v -keystore app-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias app-key
```

### Подробная команда с параметрами
```bash
keytool -genkeypair \
  -v \
  -keystore app-release-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias app-key \
  -storetype JKS \
  -dname "CN=YourName, OU=YourOrgUnit, O=YourOrg, L=YourCity, ST=YourState, C=YourCountry"
```

## Интерактивная генерация

При выполнении команды keytool запросит следующую информацию:

```
Enter keystore password: [введите пароль для keystore]
Re-enter new password: [повторите пароль]

What is your first and last name?
  [Unknown]: John Doe

What is the name of your organizational unit?
  [Unknown]: IT Department

What is the name of your organization?
  [Unknown]: My Company

What is the name of your City or Locality?
  [Unknown]: New York

What is the name of your State or Province?
  [Unknown]: NY

What is the two-letter country code for this unit?
  [Unknown]: US

Is CN=John Doe, OU=IT Department, O=My Company, L=New York, ST=NY, C=US correct?
  [no]: yes

Enter key password for <app-key>
        (RETURN if same as keystore password): [введите пароль для ключа или нажмите Enter]
```

## Примеры команд

### Для личного использования
```bash
keytool -genkeypair \
  -v \
  -keystore my-app-key.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias my-app \
  -dname "CN=Developer, OU=Mobile, O=MyCompany, L=Moscow, ST=Moscow, C=RU"
```

### Для корпоративного использования
```bash
keytool -genkeypair \
  -v \
  -keystore company-app-key.jks \
  -keyalg RSA \
  -keysize 4096 \
  -validity 25000 \
  -alias company-app \
  -dname "CN=Company Developer, OU=Mobile Development, O=Company Ltd, L=City, ST=Region, C=RU"
```

## Параметры команды

- `-genkeypair` - генерирует пару ключей (приватный и публичный)
- `-v` - подробный вывод
- `-keystore` - имя файла keystore
- `-keyalg RSA` - алгоритм шифрования
- `-keysize 2048` - размер ключа в битах (2048 или 4096)
- `-validity 10000` - срок действия в днях (~27 лет)
- `-alias` - псевдоним для ключа
- `-storetype JKS` - тип keystore (JKS по умолчанию)
- `-dname` - Distinguished Name для сертификата

## Проверка созданного keystore

```bash
# Просмотр содержимого keystore
keytool -list -v -keystore app-release-key.jks

# Просмотр конкретного ключа
keytool -list -v -keystore app-release-key.jks -alias app-key
```

## Экспорт сертификата

```bash
# Экспорт публичного сертификата
keytool -export -alias app-key -keystore app-release-key.jks -file app-certificate.crt

# Просмотр сертификата
keytool -printcert -file app-certificate.crt
```

## Конвертация в base64 для GitHub Secrets

```bash
# Конвертация keystore в base64
base64 -w 0 app-release-key.jks > keystore_base64.txt

# Просмотр результата
cat keystore_base64.txt
```

## Полезные команды

```bash
# Смена пароля keystore
keytool -storepasswd -keystore app-release-key.jks

# Смена пароля ключа
keytool -keypasswd -alias app-key -keystore app-release-key.jks

# Удаление ключа
keytool -delete -alias app-key -keystore app-release-key.jks

# Импорт сертификата
keytool -import -alias trusted-cert -file certificate.crt -keystore app-release-key.jks
```

## Рекомендации

1. **Пароли**: Используйте сильные пароли (минимум 8 символов)
2. **Backup**: Обязательно создайте резервную копию keystore файла
3. **Безопасность**: Не передавайте keystore по незащищенным каналам
4. **Срок действия**: Устанавливайте долгий срок действия (25+ лет)
5. **Размер ключа**: Используйте 2048 или 4096 бит для безопасности

## Пример полного процесса

```bash
# 1. Генерация keystore
keytool -genkeypair -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-app-key

# 2. Проверка
keytool -list -v -keystore my-release-key.jks

# 3. Конвертация в base64
base64 -w 0 my-release-key.jks

# 4. Копирование результата в GitHub Secrets
```

## Troubleshooting

### Если keytool не найден:
```bash
# Проверьте переменную JAVA_HOME
echo $JAVA_HOME

# Установите если нужно
export JAVA_HOME=$PREFIX/opt/openjdk-21
export PATH=$JAVA_HOME/bin:$PATH
```

### Если нет прав на запись:
```bash
# Создайте директорию для ключей
mkdir -p ~/keys
cd ~/keys

# Генерируйте ключ в этой директории
keytool -genkeypair -v -keystore ./my-app-key.jks ...
```