# CUPLAJ

[English](README.md) · [Română](README.ro.md) · [Français](README.fr.md) · [Italiano](README.it.md) · **Español** · [Português](README.pt.md) · [Deutsch](README.de.md) · [Magyar](README.hu.md)

<p align="center"><img src="docs/logo.svg" width="160" alt="CUPLAJ"></p>

> **Telefonía de línea compartida para teatro inmersivo**

_Versión: 1.9.0 · Última actualización: 2026-11-25_

CUPLAJ convierte una flota de teléfonos Android corrientes en una centralita telefónica privada para teatro inmersivo. Organizadores y actores hablan con el público (y entre sí) por el Wi‑Fi local del recinto — sin nube, sin cuentas, sin anuncios. Su función distintiva es el *cuplaj*: la **línea compartida** de los teléfonos fijos desde los años 80 hasta cerca de 2006, en la que dos abonados compartían una línea, sonaban con cadencias distintas y podían oírse el uno al otro.

**[Descargar el APK](https://github.com/chiuta/CUPLAJ/releases/download/v1.9.1/CUPLAJ-1.9.1.apk) · [Descargar para iPhone/iPad (.ipa)](https://github.com/chiuta/CUPLAJ/releases/download/v1.9.1/CUPLAJ-1.9.1-ios-unsigned.ipa) · [Todas las versiones](https://github.com/chiuta/CUPLAJ/releases)**

## Funciones

- **Tres épocas de teléfono** — 1985 disco rotatorio (pulsos audibles), 1995 teclado (tonos DTMF), 2003 móvil (SMS, 160 caracteres). La regiduría puede cambiar de época en directo.
- **Línea compartida (cuplaj)** — dos teléfonos comparten una línea; una llamada a uno hace sonar ambos con la cadencia del abonado llamado; el vecino que descuelga primero se lleva la llamada; descolgar durante la conversación del vecino permite escuchar e incluso intervenir.
- **Sin infraestructura** — un teléfono (la regiduría) es el hub. Los demás entran por Wi‑Fi o por su punto de acceso; los hubs se encuentran solos; código de sala y PIN opcionales.
- **Voz** — llamadas reales (8 kHz), megafonía pulsar‑para‑hablar desde la regiduría, canal de bambalinas para el equipo.
- **Señales** — texto a pantalla completa, vibración, timbre, flash, altavoz, cambio de época, contacto nuevo, voz hablada (síntesis local), efectos de sonido, pantalla negra, llamada fantasma.
- **Guion** — lista de señales con GO y PLAY (temporizado), reordenar, importar/exportar JSON.
- **Herramientas de regiduría** — números, grupos, acoplar/desacoplar, parejas automáticas, escucha discreta, silenciar, olvidar, niveles de batería, registro de eventos, estado que sobrevive a reinicios, limitación de flujo.
- **8 idiomas y accesibilidad** — inglés, rumano, francés, italiano, español, portugués, alemán, húngaro; tamaño de texto ajustable; respuesta desde la pantalla de bloqueo; funciona en segundo plano.
- **Internet y datos móviles (1.1)** — conexión con `host` o `host:puerto`; clave de red AES‑128‑GCM opcional; compresión G.711 automática fuera de la red local; keepalive.
- **Llamadas directas P2P (1.1)** — el audio va de teléfono a teléfono por UDP con perforación de NAT, con respaldo en el relé del hub; la regiduría puede desactivarlo, limitarlo a la red local (por defecto) o abrirlo a cualquier lugar.
- **Pasarela VoIP / SIP (1.1)** — el teléfono de la regiduría se registra en un proveedor SIP o una PBX; el equipo marca `0`+número para salir, las llamadas SIP entrantes suenan en un número interno.
- **Red móvil (1.1)** — pasarela SMS mediante la SIM del teléfono de la regiduría (envío y recepción), cue de *llamada real* que abre el marcador e indicador Wi‑Fi / móvil.
- **Más formas de conectarse (1.2)** — Bluetooth (sin router, unos 7 teléfonos), Wi‑Fi Direct (el teléfono de la regiduría crea su propia red Wi‑Fi) y descubrimiento mDNS, además de Wi‑Fi, punto de acceso, anclaje USB, Ethernet y datos móviles.
- **Plug and play (1.3)** — el teléfono de la regiduría abre solo todos los enlaces útiles; los demás pulsan *Buscar y unirme automáticamente* o un enlace de invitación. Añade Wi‑Fi Aware.
- **Reforzado y sin red (1.4)** — auditoría con público hostil y correcciones, y CUPLAJ‑AIR: tres protocolos originales que llevan señales e invitaciones sin ninguna red (linterna → cámara, altavoz → micrófono, vibración → acelerómetro).
- **Control del espectáculo (1.5)** — señales que saltan en el mismo instante, señal PÁNICO, color a pantalla completa, encuestas al público, confirmación de entrega y tecla PTT física. Comparado con Unity Intercom, Zello, Augmented Theatre, QLab/SOLOstudio y Livecube en `docs/COMPARISON.md`.
- **Todo lo de la comparación que cabe en una app solo Android sin conexión (1.6)** — prioridad PA (ducking), OSC de entrada y salida para mesas de luces y sonido, y señales con imagen, vídeo y clip de audio enviadas por el hub.
- **Espectáculo inteligente (1.7)** — zonas que siguen al público por sonido, reglas («cuando alguien entre en la zona 3, envíale…»), contador de público, efectos de voz, subtítulos, texto multilingüe y personalizado, ramificación de la historia.
- **Herramientas de escena (1.8)** — luces Art‑Net/DMX, MIDI, ola de señales, teléfono descompuesto, secreto fragmentado, grabadora fantasma, señal de seguridad (luces de sala), Morse por vibración, informe posfunción.
- **iPhone y iPad** — cliente completo y anfitrión (regiduría) en Swift, mismo protocolo que Android: ambos se mezclan libremente en una misma función; se compila desde el código con Xcode (`ios/`).

## Inicio rápido

1. Obtén el APK en la pestaña *Actions* (artefacto `CUPLAJ-apk`) o en la página *Releases* e instálalo en cada teléfono.
2. En el teléfono de la regiduría activa el punto de acceso (o pon todos los teléfonos en el mismo Wi‑Fi).
3. En el teléfono de la regiduría elige **Regiduría** → *Iniciar el hub*. Anota el PIN de los actores y, si quieres, un código de sala.
4. En los demás elige **Espectador** o **Actor**, selecciona el hub de la lista (o escribe su dirección) y conéctate.
5. Cada teléfono recibe un número (regiduría 100, actores 201…, espectadores 6 cifras). Descuelga y llama.

## Las tres épocas

- **1985 · disco** — un disco que se gira de verdad; los pulsos se oyen; dos timbrazos largos.
- **1995 · teclas** — tonos DTMF, libreta de números, mensajes de busca.
- **2003 · móvil** — nombres en la agenda, identificación de llamada, SMS limitados a 160 caracteres.

## Cómo funciona la línea compartida

<p align="center"><img src="docs/partyline.svg" width="620" alt="party line"></p>

1. La regiduría *acopla* dos teléfonos (o lo hace automáticamente por parejas). Comparten la línea N, posiciones 1 y 2.
2. Una llamada al abonado 1 hace sonar ambos; el abonado 1 oye la cadencia de su número (un timbrazo largo), el abonado 2 la suya (dos cortos).
3. Quien contesta se lleva la llamada; el otro teléfono deja de sonar.
4. Mientras la línea está en uso, el vecino que descuelga oye la conversación y los interlocutores oyen un leve clic.
5. Quien escucha puede *intervenir* y hablar; una llamada a un teléfono acoplado cuya línea está ocupada recibe el tono de ocupado.
6. La regiduría puede vigilar cualquier línea en silencio (sin clic).

## Documentación

- [Guía de uso](docs/GUIDE.es.md)
- [Protocolo](docs/PROTOCOL.md) *(English)*
- [Auditoría adversarial](docs/AUDIT.md) *(English)*
- [Registro de cambios](CHANGELOG.md) *(English)*
- [Reseña honesta y comparación](docs/REVIEW.md) *(English)*
- [Sitio de presentación](https://chiuta.github.io/CUPLAJ/)

## Estado real

La lógica del hub (llamadas, línea compartida, permisos, límites, persistencia) está cubierta por más de 250 comprobaciones automáticas que se ejecutan en una JVM. La interfaz Android y la ruta de audio han pasado la comprobación de tipos frente al SDK de Android 14, pero **aún no se han probado en dispositivos reales**; el APK lo construye GitHub Actions. Prueba con unos pocos teléfonos antes de la función. El tráfico de la red local no está cifrado (el PIN y el código de sala frenan a extraños de buena fe, no son criptografía). La red de la versión 1.1 (cifrado, P2P, SIP, SMS) está cubierta por pruebas del hub contra un servidor SIP simulado, pero **no** se ha probado con operadores, NAT ni SIM reales. Los enlaces Bluetooth, Wi‑Fi Direct y mDNS (1.2) comparten la misma capa de transporte, probada en JVM con canales simulados, pero **no** se han probado en dispositivos reales; el comportamiento varía entre fabricantes. El plug and play (conexión automática, invitaciones, Wi‑Fi Aware, 1.3) **no** se ha probado en dispositivos; el formato de las invitaciones está cubierto por pruebas JVM. Las correcciones de la auditoría 1.4 y los códecs AIR están cubiertos por pruebas JVM (ruido simulado, deriva de reloj, clave errónea, interferencia); el código de linterna, cámara, micrófono, altavoz, motor y acelerómetro **no** está probado en teléfonos reales, y el alcance y la velocidad reales serán distintos. Las funciones 1.5 están cubiertas por pruebas JVM (estimación del reloj, retrasos, reglas de la encuesta, límites fotosensibles); las superposiciones de pantalla, el botón del auricular y la precisión real del reloj **no** están probados en dispositivos. Las funciones 1.6 están cubiertas por pruebas JVM (curva de ducking, fuzzing e inundación OSC, integridad de transferencias y seguridad de rutas, relé del hub); la reproducción de imágenes, vídeo y clips, el selector de archivos y los diálogos de ajustes **no** están probados en dispositivos. Zonas, reglas, contador, efectos de voz e idiomas están cubiertos por pruebas JVM (motor de reglas, reglas del hub, agregación del contador, filtros de voz medidos en frecuencia); las balizas sonoras reales en interiores, las mediciones con micrófono y acelerómetro y la calidad de voz en teléfonos **no** están probadas en dispositivos. Una baliza grabada puede reproducirse una vez por número de secuencia: las zonas son para ambientar, no para seguridad. Paquetes Art‑Net, fundidos DMX, mensajes MIDI, patrones Morse, informe y lógica del hub (ola, teléfono descompuesto, secreto, informe) están cubiertos por pruebas JVM; la grabadora fantasma, mesas de luces y dispositivos MIDI reales y todo lo que implique teléfonos reales no están probados. **iOS:** el núcleo Swift se prueba en macOS en CI y la app compila para el simulador, pero **no se ha probado en iPhone reales ni con un proveedor SIP real**.

## Compilación

Requiere JDK 17. `./gradlew assembleRelease` genera `app/build/outputs/apk/release/app-release.apk` (firmado con la clave de depuración, instalable directamente). Pruebas unitarias: `./gradlew test`. El workflow de `.github/workflows` hace ambas cosas, con herramientas Node ≥ 24 en Ubuntu 26.04.

## Privacidad

Sin analítica, sin servidores, sin cuentas. El audio y los mensajes solo viajan por vuestra red local, entre los teléfonos y el hub. El hub guarda números, grupos y acoplamientos en el teléfono de la regiduría para que un reinicio no los pierda.

## Apoya el proyecto

CUPLAJ es gratuito y trade‑free. Si ayuda a tu función, puedes invitar al autor a un café: <https://buymeacoffee.com/echo.of.the.strings>.

## Licencia

MIT © Alexandru‑Ionuț Chiuță (Alexio).
