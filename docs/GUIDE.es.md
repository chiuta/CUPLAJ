# CUPLAJ — Guía de uso

[English](GUIDE.md) · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · **Español** · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Versión: 1.10.0 · Última actualización: 2026-11-26_

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

## 8. Internet, VoIP y red móvil (1.1)

- **Clave de red** — en la pantalla de inicio activa *Enlace cifrado*; la regiduría muestra una clave (xxxx‑xxxx‑xxxx‑xxxx). Escríbela en el campo *Clave de red* de cada otro teléfono. Sin la clave correcta el hub rechaza la conexión. Úsala siempre que el tráfico salga de la red local.
- **Desde internet** — redirige TCP 8470 y UDP 8472 en el router al teléfono de la regiduría e indica la dirección pública (`host` o `host:puerto`). Con datos móviles no hace falta nada más.
- **P2P** — en la pestaña Regiduría la opción *Llamadas directas* alterna desactivadas / solo red local / en cualquier lugar. *En cualquier lugar* deja que dos teléfonos conozcan la IP pública del otro; la regiduría no puede monitorizar, silenciar ni intervenir mientras la llamada es directa (vuelve al relé automáticamente cuando hace falta).
- **Línea SIP** — Ajustes → *Línea VoIP / SIP…*: servidor, usuario, contraseña, dominio opcional y número interno para llamadas entrantes (100 por defecto). Reinicia el hub para aplicar. El equipo marca `0` + número externo; los espectadores no pueden llamar al exterior. Cada llamada se limita a 15 minutos. SIP y RTP no van cifrados.
- **SMS** — Ajustes → *Pasarela SMS* (requiere SIM y permisos SMS). El equipo elige *SMS a un número real* en la pestaña Mensajes; los SMS recibidos aparecen como mensajes de *SMS*. Límite: 20 por minuto. Se aplican las tarifas del operador.
- **Llamada real** — el cue *llamada real* abre el marcador con el número. Android no permite a una app meter una llamada celular en la línea compartida: el audio GSM no se puede enlazar; para eso usa la línea SIP.

## 9. Conectarse sin router (1.2)

- **Mismo hub, cualquier enlace** — llamadas, línea compartida, cues, cifrado y PIN funcionan igual; solo cambia el camino del teléfono hasta el hub.
- **Punto de acceso, anclaje USB, Ethernet** — cualquier enlace que dé a los teléfonos una red IP funciona sin configurar; usa la dirección o deja que se encuentre el hub.
- **Bluetooth** — en el teléfono de la regiduría inicia el hub con *Enlace Bluetooth: SÍ* y pulsa *Hacer visible por Bluetooth*. En los demás pulsa *Buscar por Bluetooth* y elige el teléfono de la regiduría (🔵). El alcance es corto y caben unos 7 teléfonos por hub; la voz se comprime automáticamente. No hace falta emparejar; el acceso sigue protegido por PIN, código de sala y clave de red.
- **Wi‑Fi Direct** — en el teléfono de la regiduría inicia el hub con *Grupo Wi‑Fi Direct: SÍ*. En los demás pulsa *Buscar Wi‑Fi Direct* y elige el teléfono de la regiduría (📡); puede pedir que aceptes cada teléfono. Tras unirse, el hub aparece en la lista. Normalmente los teléfonos no pueden seguir en su Wi‑Fi habitual dentro de un grupo Wi‑Fi Direct, y algunos modelos no admiten ambos a la vez.
- **mDNS** — el hub también se anuncia por mDNS/DNS‑SD (🌐), lo que lo hace visible en redes que bloquean la difusión habitual.
- **No incluidos** — Wi‑Fi Aware (pocos teléfonos) y Nearby Connections de Google (requiere Google Play Services, que esta app trade‑free evita). Bluetooth Low Energy es demasiado lento para voz.

## 10. Plug and play (1.3)

- **Teléfono de la regiduría** — elige *Regiduría* y *Iniciar el hub*. Con *Plug & play* activado (por defecto) CUPLAJ abre Wi‑Fi / punto de acceso, mDNS, Bluetooth y Wi‑Fi Aware, y crea un grupo Wi‑Fi Direct si no hay red Wi‑Fi. Acepta la única petición de permisos. Los ajustes muestran los enlaces activos.
- **Otros teléfonos, lo más fácil** — Ajustes → *Compartir invitación para espectadores* (o *actores*) en el teléfono de la regiduría; envía el enlace por cualquier app. Quien lo toque con CUPLAJ instalado queda configurado (dirección, clave, PIN, sala) y conectado.
- **Otros teléfonos, sin enlace** — elige un rol y pulsa *Buscar y unirme automáticamente*. CUPLAJ busca en todos los enlaces a la vez (Wi‑Fi, mDNS, Bluetooth, Wi‑Fi Direct, Wi‑Fi Aware) y se conecta al mejor hub: primero la red local, al final Bluetooth.
- **Wi‑Fi Aware** — los teléfonos compatibles se encuentran sin router ni configuración. Muchos no lo admiten; no se rompe nada si el tuyo no.
- **Seguridad** — una invitación solo puede crear un espectador o un actor, nunca un teléfono de regiduría. Los enlaces contienen la clave y el PIN; envíalos solo a quien invites.

## 11. Refuerzo + AIR (1.4)

- **Hub más seguro** — PIN aleatorio de 6 cifras, clave nueva en cada conexión, límites a conexiones sin autenticar y al tamaño de mensajes, bloqueo tras PIN erróneos repetidos y confirmación antes de aplicar un enlace de invitación.
- **LUMEN (luz)** — el teléfono de regiduría parpadea con la linterna; otro teléfono mira con la cámara trasera. Sala con poca luz, 1–3 m, linterna apuntando a la cámara. Lento (unos segundos) pero robusto.
- **SONUS (sonido)** — el altavoz reproduce un código de tonos, casi ultrasónico (inaudible para la mayoría de adultos) o audible; el otro teléfono escucha con el micrófono. Lleva señales y una invitación al hub (dirección y PIN).
- **TACTUS (vibración)** — el motor de vibración envía pulsos cortos y largos; otro teléfono apoyado en la misma superficie los siente con el acelerómetro. Funciona en silencio y a oscuras, para señales cortas.
- **Uso** — Ajustes → *Enlaces por aire*: activa las escuchas deseadas (cámara / micrófono / acelerómetro; el teléfono pide permiso) y en el teléfono de regiduría pulsa un botón de envío. Los paquetes llevan una etiqueta corta de autenticación derivada de la clave de red y no pueden repetirse.

## 12. Control del espectáculo (1.5)

- **Mismo instante** — empieza el texto con `@3.5;` para que salte dentro de 3,5 s en todos los teléfonos a la vez. Los teléfonos sincronizan el reloj con el hub al conectar y cada minuto; una señal con más de 10 s de retraso se descarta.
- **PÁNICO** — la señal *PÁNICO: parar todo* detiene timbre, vibración, voz, efectos de sonido, pantallas negras y de color, llamadas falsas y señales programadas. También funciona por AIR.
- **Color** — `#RRGGBB;segundos;pulsos por segundo` llena la pantalla. Los pulsos se limitan a 2 por segundo para el público fotosensible; una pulsación larga la cierra.
- **Encuesta** — `pregunta;opción 1;opción 2;…` pregunta a espectadores y actores; un voto por teléfono (modificable). *Cerrar la encuesta* muestra el resultado a regiduría. Las encuestas pueden ser pasos del guion.
- **Entrega** — tras cada señal, regiduría ve *enviada a N de M teléfonos en línea*. **PTT físico** — Ajustes → *Tecla PTT física* convierte el botón del auricular y volumen + en push‑to‑talk con la app en primer plano.

## 13. Medios, OSC, prioridad (1.6)

- **Prioridad PA** — mientras el teléfono de regiduría (o un actor) habla a todos, el resto de sonidos baja al 30 % y vuelve suavemente. Ajustes → *Prioridad PA* la desactiva.
- **Señales multimedia** — en el teléfono de regiduría, pestaña Señales → *Enviar un archivo…* elige una imagen, un vídeo o un audio (hasta 8 MB) y lo envía por partes a los teléfonos elegidos; cada uno comprueba tamaño y suma de control e ignora un archivo dañado. Luego usa *Imagen en pantalla*, *Vídeo en pantalla* o *Clip de audio* (texto vacío = último archivo, o `código;segundos`). Una pulsación larga cierra la imagen o el vídeo. Los teléfonos que entran después no tienen el archivo: envíalo de nuevo.
- **OSC de salida** — Ajustes → *OSC*: cada señal se envía también como `/cuplaj/cue <a> <tipo> <texto>` por UDP a una mesa de luces o sonido, QLab o Companion.
- **OSC de entrada** — la mesa puede enviar `/cuplaj/<PIN del hub>/go`, `/panic`, `/stop` o `/cue <a> <tipo> <texto>` a regiduría (puerto 8473 por defecto). El PIN en la dirección es la única protección: mantén privada la red del espectáculo. Máximo 20 mensajes por segundo.
- **No incluido** — iOS (solo Android), program feed y más canales que los grupos existentes.

## 14. Espectáculo inteligente (1.7)

- **Zonas** — pon un teléfono en una sala (o en el bolsillo de un personaje), Ajustes → *Enlaces por aire* → *Baliza de zona* y toca hasta ver el número 1–9: cada 7 s emite un código casi ultrasónico, inaudible para la mayoría. Los teléfonos del público con *Escuchar: SONUS* activo informan de la zona que oyen; regiduría ve 📍 junto al nombre. 30 s de silencio = se fueron.
- **Reglas** — teléfono de regiduría, Ajustes → *Reglas*, una por línea: `zone 3 once > self | text | Bienvenido, {name}`, `pollwin 1 > all | color | #00ff00;3`, `meter noise 70 > all | blackout | 1;`, `pollwin 2 > all | goto | Final B`. *self* es quien activó la regla; `goto` salta a un paso del guion por nombre o número: una encuesta puede elegir la historia.
- **Contador de público** — pestaña Señales: *Medir el ruido* o *el movimiento* durante 5 s en los teléfonos elegidos. Cada uno envía solo un número 0–100, nunca sonido. Ves media, pico y cuántos respondieron; las reglas pueden reaccionar.
- **Efectos de voz** — elige teléfonos en la lista de regiduría, *Efecto de voz…*: robot, radio antigua, eco, grave, aguda, lo‑fi. El efecto se aplica en ese teléfono antes de enviar: todos oyen la misma voz.
- **Subtítulos e idiomas** — la señal *Subtítulo* muestra subtítulos grandes; cualquier señal de texto puede llevar `en=Hello|ro=Salut|fr=Bonjour` y cada teléfono usa su idioma (luego inglés, luego el primero). `{name}` y `{number}` pasan a ser el nombre y número del teléfono receptor.

## 15. Herramientas de escena (1.8)

- **Luces y música** — las señales *dmx* (`0;1=255,2-4=128;2000`) y *midi* (`note 60 127`, `cc 7 100`, `pc 5`, `mmc play`) se ejecutan en el teléfono de regiduría: Ajustes → *Salida Art‑Net / DMX* envía Art‑Net (UDP 6454) a un nodo o mesa en la misma Wi‑Fi; el MIDI va al primer dispositivo MIDI USB/Bluetooth encontrado.
- **Ola** — empieza el texto de una señal con `~150;`: el hub la envía a los teléfonos uno tras otro, por orden de número, a 150 ms.
- **Teléfono descompuesto** — *Herramientas → Teléfono descompuesto*: mensaje y números en orden; cada uno ve lo que pasó el anterior, puede cambiarlo y lo pasa. La regiduría recibe el original y todas las deformaciones.
- **Secreto fragmentado** — una frase se reparte palabra a palabra; quien diga primero la frase entera (Ajustes → *Decir la frase secreta*) la resuelve; el disparador `solved` puede lanzar una señal.
- **Grabadora fantasma** — graba hasta 60 s del micrófono de un teléfono en el hub y los reproduce luego a un grupo, como una voz de la nada (fuente reservada 253).
- **Luces de sala** — señal *house*: detiene todo (como PÁNICO), vibra, pantalla blanca y mensaje tranquilo. **Morse** — señal *morse*: vibra un texto en Morse.
- **Informe** — *Herramientas → Informe*: archivo Markdown con totales, llamadas/mensajes/entradas de zona/batería por teléfono y cronología completa.
- **Límites y seguridad (1.8)** — una ola nunca dura más de 20 s (el paso se acorta en salas grandes; máximo 4 olas a la vez); un teléfono que no responde en 90 s en el teléfono descompuesto se salta; un secreto fragmentado exige una frase de al menos 2 palabras y 4 letras latinas o cifras (la comprobación ignora mayúsculas, acentos y puntuación); una sola reproducción de la grabadora a la vez; el Morse se corta a 45 s; Art‑Net no envía nada hasta la primera señal *dmx* (no puede apagar las luces solo por activarse) y admite hasta 8 universos; *dmx* y *midi* solo funcionan en el teléfono de regiduría (señales y guiones), no como acciones de regla.

## 16. Referencia de señales (todos los tipos)

- **Prefijos** — `@3.5;` lanza la señal a los 3,5 s en todos los teléfonos a la vez; `~150;` (tras `@…;`) la envía a los teléfonos uno tras otro, a 150 ms; `en=Hello|ro=Salut` elige el idioma de cada teléfono; `{name}` y `{number}` se sustituyen en cada teléfono.
- **`text`** — Texto en pantalla: Texto mostrado a pantalla completa
- **`vibrate`** — Vibración: Duración en ms (p. ej. 800)
- **`ring`** — Timbre: (no hace falta texto)
- **`flash`** — Flash: Duración en ms (p. ej. 1500)
- **`speaker`** — Altavoz: 1 = activado, 0 = desactivado
- **`contact`** — Añadir contacto: Nombre;número (p. ej. Mamá;204455)
- **`era`** — Cambiar época: 1985, 1995, 2003, 2025, 2045 o 2080 (o una lista: 1985,2080;4000)
- **`say`** — Voz hablada: Texto a pronunciar
- **`sound`** — Efecto de sonido: bell, siren, knock, static (campana, sirena, golpes, estática)
- **`blackout`** — Pantalla negra: 1;texto o 0
- **`fakecall`** — Llamada fantasma: Nombre;número;palabras habladas
- **`realcall`** — Llamada real (abre el marcador): Número a marcar, p. ej. +40721000111
- **`panic`** — PÁNICO: parar todo: (sin texto)
- **`color`** — Color a pantalla completa: #RRGGBB;segundos;pulsos por segundo (máx. 2)
- **`poll`** — Encuesta (voto del público): pregunta;opción 1;opción 2;… (2–6 opciones)
- **`pollend`** — Cerrar la encuesta y mostrar el resultado: (sin texto)
- **`image`** — Imagen en pantalla: código del archivo;segundos (envía antes el archivo; vacío = último)
- **`video`** — Vídeo en pantalla: código del archivo;segundos (envía antes el archivo; vacío = último)
- **`clip`** — Clip de audio: código del archivo;segundos (envía antes el archivo; vacío = último)
- **`caption`** — Subtítulo en pantalla: texto, o en=Hello|ro=Salut|fr=Bonjour (cada teléfono muestra su idioma; {name} = nombre del teléfono)
- **`dmx`** — Luces: Art‑Net/DMX (teléfono de regiduría): universo;canal=valor,rango=valor;fundido ms — p. ej. 0;1=255,2-4=128;2000
- **`midi`** — Salida MIDI (teléfono de regiduría): note 60 127 [canal] · off 60 [canal] · cc 7 100 [canal] · pc 5 [canal] · mmc play|stop|record
- **`morse`** — Código Morse por vibración: texto a transmitir (letras y cifras; ~ 60 caracteres)
- **`house`** — Luces de sala (seguridad: parar + pantalla blanca): (opcional) mensaje en la pantalla blanca
- **Disparadores y acciones de reglas** — Una regla por línea: ‹cuándo› > ‹quién› | ‹tipo de señal› | ‹texto›. Cuándo: `zone 3 [once]`, `pollwin 0` (opción ganadora, desde 0), `meter noise|motion 70` (media ≥ 70). Quién: all, staff, spectators, actors, self (quien la activó), num:/id:/grp:. El tipo `goto` salta a un paso del guion por nombre o número. El texto puede usar {name}. También `solved` (se adivinó un secreto fragmentado).

## 17. Pantallas y botones (exactamente como en la app)

- **Voz** — `ANUNCIO a todos los teléfonos` · `SOLO AL EQUIPO`
- **Regiduría** — `Número` · `Grupo` · `Acoplar 2` · `Desacoplar` · `Parejas auto` · `Escuchar` · `Silenciar` · `Olvidar` · `Registro` · `Llamar` · `Época …` · `Vecino escucha: …` · `Clic al descolgar: …` · `Mensajes entre teléfonos: …` · `Llamadas directas (P2P): …` · `Enviar la señal` · `Medir el ruido (5 s)` · `Medir el movimiento (5 s)` · `Efecto de voz para los teléfonos elegidos…` · `Enviar un archivo (imagen, vídeo, audio) a los teléfonos elegidos…` · `Herramientas: cadena, secreto, grabadora, informe`
- **Guion** — `Añadir paso` · `GO ▶` · `⏮ Inicio` · `Borrar` · `Exportar` · `Importar`
- **Ajustes (menú Aa)** — `TAMAÑO DEL TEXTO` · `Mantener la app activa (optimización de batería)` · `🔍 Buscar y unirme automáticamente` · `Buscar por Bluetooth` · `Buscar Wi‑Fi Direct` · `Hacer visible por Bluetooth (5 min)` · `Compartir invitación para espectadores` · `Compartir invitación para actores` · `Línea VoIP / SIP…` · `Enlaces por aire: luz, sonido, vibración` · `Baliza de zona (este teléfono, altavoz): …` · `OSC: mesas de luces / sonido, QLab…` · `Salida Art‑Net / DMX (luces)` · `Reglas: zona, encuesta, público → señal` · `Prioridad PA: los demás sonidos bajan cuando habla regiduría` · `Tecla PTT física: activada (botón de auricular, volumen +)` · `Decir la frase secreta` · `Restablecer la memoria del hub (números, acoplamientos)` · `Acerca de CUPLAJ` · `Salir`
- Los ajustes y opciones de regiduría muestran también su estado (activado/desactivado).

## 18. iPhone e iPad (iOS)

- La app de iOS es un cliente completo y también puede alojar la función (regiduría). Habla el mismo protocolo: teléfonos Android e iOS se mezclan libremente.
- Compilación: abre `ios/` (XcodeGen: `cd ios && xcodegen generate`), elige tu equipo de firma en Xcode y ejecuta. Aún no hay versión en App Store ni TestFlight.
- Incluye: llamadas y línea compartida, señales, guion, encuestas, reglas, contador, medios, megafonía (PA), Art‑Net, MIDI, OSC, audio P2P directo, pasarela SIP, CUPLAJ‑AIR (luz, sonido, vibración) y audio en segundo plano.
- Imposible en iOS (sin API pública): Bluetooth RFCOMM, Wi‑Fi Direct, pasarela silenciosa de SMS y llamadas. Usa el punto de acceso del iPhone o un Wi‑Fi común; la señal *llamada real* abre el marcador.
- Mantén la app abierta con la pantalla encendida y el teléfono de regiduría enchufado. Hasta ahora solo se han ejecutado compilaciones para simulador y pruebas automáticas — **aún no se ha probado en iPhone reales**.

---

[← README](../README.es.md)
