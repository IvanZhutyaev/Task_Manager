# Android client

Нативное приложение (Kotlin) к Task Manager API.

## Требования

- Android Studio Hedgehog+ / SDK 34
- Запущенный backend на ПК (`mvn spring-boot:run`)

## Запуск

1. File → Open → папка `clients/android`
2. Дождитесь Gradle Sync
3. Запустите на **эмуляторе**

Базовый URL по умолчанию: `http://10.0.2.2:8080/api/v1`  
(`10.0.2.2` — это `localhost` хоста с точки зрения эмулятора Android.)

На физическом телефоне укажите IP компьютера в локальной сети, например:

```
http://192.168.0.10:8080/api/v1
```

и убедитесь, что firewall пропускает порт 8080.

## Что умеет

- register / login (JWT)
- список проектов, создание проекта
- просмотр досок → колонок → задач выбранного проекта

Тот же JSON-контракт, что у Web SPA и Python CLI.
