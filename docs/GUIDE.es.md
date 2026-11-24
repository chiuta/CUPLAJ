# CUPLAJ — Guía de uso

[English](GUIDE.md) · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · **Español** · [Português](GUIDE.pt.md) · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Versión: 1.6.0 · Última actualización: 2026-11-24_

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
- **No incluido** — iOS (solo Android), MIDI, program feed y más canales que los grupos existentes.

---

[← README](../README.es.md)
