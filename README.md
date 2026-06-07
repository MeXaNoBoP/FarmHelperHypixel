# FarmHelperHypixel

FarmHelperHypixel is a client-side Fabric mod for Minecraft 1.21.11 that helps switch into a resource farming control layout quickly and safely.

Author: MeXaNoBoP

[Download on Modrinth](https://modrinth.com/mod/farmhelperhypixel) | [Discord](https://discord.gg/vpZnt78Yfy)

## Languages

The mod is written in **Java 21** using the **Fabric API** framework.

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.19.2
- Fabric API 0.141.4+1.21.11
- Java 21

## Features

- One key toggles farm mode on and off.
- Farm mode saves the current sensitivity and key bindings before changing anything.
- Mouse sensitivity is reduced to the minimum while farm mode is active.
- WASD movement is rebound to the arrow keys.
- Attack is rebound to PgDown.
- PgDown toggles block breaking by imitating a held attack key.
- End sends `/home` while farm mode is active.
- Turning farm mode off restores the saved sensitivity and key bindings.

## Controls

| Action | Default key |
| --- | --- |
| Toggle farm mode | F8 |
| Toggle block breaking | PgDown |
| Send `/home` | End |

The farm mode toggle can be changed in Minecraft's controls menu under `FarmHelperHypixel`.

## Build

```powershell
.\gradlew.bat clean build --no-daemon
```

The built mod jar will be created in:

```text
build/libs/
```

---

# FarmHelperHypixel (Русский)

FarmHelperHypixel — клиентский Fabric мод для Minecraft 1.21.11, который позволяет быстро и безопасно переключаться в раскладку управления для фарма ресурсов.

Автор: MeXaNoBoP

[Скачать на Modrinth](https://modrinth.com/mod/farmhelperhypixel) | [Discord](https://discord.gg/vpZnt78Yfy)

## Языки

Мод написан на **Java 21** с использованием фреймворка **Fabric API**.

## Требования

- Minecraft 1.21.11
- Fabric Loader 0.19.2
- Fabric API 0.141.4+1.21.11
- Java 21

## Возможности

- Одна клавиша включает и выключает режим фарма.
- При включении режима фарма сохраняются текущая чувствительность мыши и привязки клавиш.
- Чувствительность мыши снижается до минимума, пока режим фарма активен.
- WASD переназначается на клавиши-стрелки.
- Атака переназначается на PgDown.
- PgDown переключает непрерывную добычу блоков, имитируя удержание клавиши атаки.
- End отправляет команду `/home`, пока режим фарма активен.
- При выключении режима фарма восстанавливаются сохранённые чувствительность и привязки клавиш.

## Управление

| Действие | Клавиша по умолчанию |
| --- | --- |
| Включить/выключить режим фарма | F8 |
| Включить/выключить добычу блоков | PgDown |
| Отправить `/home` | End |

Клавишу переключения режима фарма можно изменить в меню управления Minecraft в разделе `FarmHelperHypixel`.

## Сборка

```powershell
.\gradlew.bat clean build --no-daemon
```

Собранный jar-файл мода будет находиться в:

```text
build/libs/
```
