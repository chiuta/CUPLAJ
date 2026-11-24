# CUPLAJ — Guía de uso

[English](GUIDE.md) · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · **Español** · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Versión: 1.0.0 · Última actualización: 2026-11-24_

## 1. Antes de la función

- Usa una sola red Wi‑Fi para todos los teléfonos, o el punto de acceso del teléfono de la regiduría. Desactiva el *aislamiento de clientes* en los routers.
- Instala el APK, concede los permisos de micrófono y notificaciones y, en el menú **Aa**, elige *Mantener la app activa* para que el sistema no la duerma.
- Carga todos los teléfonos; la regiduría muestra los niveles de batería y avisa por debajo del 20 %.

## 2. Roles y números

- **Regiduría** — el hub; lo ve todo, envía señales, acopla teléfonos. Número 100.
- **Actor** — necesita el PIN; puede hablar al equipo y llamar a cualquiera. Números desde 201.
- **Espectador** — recibe llamadas, mensajes y señales; solo puede hablar al equipo. Números de seis cifras.
- La regiduría puede cambiar cualquier número y asignar grupos (por ejemplo *palco*, *sótano*).

## 3. Usar el teléfono

- **1985** — descuelga (botón), espera el tono, gira el disco; el número se marca solo tras una breve pausa.
- **1995** — descuelga y usa el teclado; la libreta lista los números añadidos por la regiduría.
- **2003** — escribe el número y pulsa *Llamar*; los mensajes se limitan a 160 caracteres.
- Un teléfono que suena se puede contestar desde la notificación, incluso con la pantalla bloqueada.

## 4. La línea compartida en la práctica

- Selecciona dos dispositivos en la pestaña Regiduría y pulsa *Acoplar 2*, o *Parejas auto* para emparejar todos los teléfonos libres.
- Usa *Escuchar* en la regiduría para vigilar una línea sin ser oído.
- Las opciones permiten desactivar la escucha del vecino, el clic o los mensajes entre teléfonos.

## 5. Señales

- Elige un destino (todos, espectadores, actores, dispositivos seleccionados, un grupo, un número), un tipo y un texto, y pulsa *Enviar la señal*.
- Ejemplos: `Mamá;204455` añade un contacto; `1;Casi es medianoche` deja la pantalla negra con un texto tenue; `Dr. Voss;204455;Venid al sótano` lanza una llamada fantasma que habla al contestar.

## 6. Guion

- Construye la función en pasos con destino, tipo, texto y pausa. **GO** envía el paso siguiente; **PLAY** ejecuta el resto automáticamente desde el paso seleccionado.
- Exporta el guion como JSON e impórtalo en otro teléfono de regiduría.

## 7. Solución de problemas

- *No se encuentra ningún hub* — comprueba que estáis en la misma red, desactiva el aislamiento de clientes o escribe la dirección del hub.
- *PIN / código de sala incorrecto* — la regiduría los muestra en su pantalla de inicio.
- *Puerto ocupado* — cierra cualquier otra app que use el puerto 8470.
- *Llamadas mudas* — revisa el permiso del micrófono y el interruptor del altavoz en la pestaña Voz.

---

[← README](../README.es.md)
