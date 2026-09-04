# Фотоальбом — обёртка-приложение (WebView) для Android TV и телефона

Открывает сайт `https://3.9807381.ru/` в полноэкранном WebView с поддержкой TV-пульта:
- D-pad / OK — навигация по сайту,
- медиа-кнопки (play/pause/next/prev) — передаются на сайт для видео,
- автоплей видео при включении ролика,
- экран не гаснет во время просмотра.

## Сборка APK (GitHub Actions)

1. Создайте репозиторий на GitHub и запушите этот код:
   ```
   git init
   git add .
   git commit -m "PhotoAlbum wrapper"
   git branch -M main
   git remote add origin https://github.com/<ВашЛогин>/PhotoAlbum.git
   git push -u origin main
   ```
2. GitHub Actions сам соберёт APK (или нажмите Run workflow в Actions).
3. APK скачайте в артефактах: **Actions → Build APK → PhotoAlbum-TV-debug**.

## Установка

- **Android TV (Android 10+)**: скопируйте APK на ТВ (флешка / сеть / облако) и установите (разрешите «неизвестные источники»).
- **Телефон (Android 15)**: откройте APK и подтвердите установку.

Приложение наследует вход/сессии сайта — отдельно логиниться в приложении не нужно.

## Изменить адрес сайта

Правьте `app/src/main/res/values/strings.xml`, элемент `app_url`, затем пересоберите.