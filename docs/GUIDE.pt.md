# CUPLAJ — Guia de utilização

[English](GUIDE.md) · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · **Português** · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Versão: 1.5.0 · Última atualização: 2026-11-24_

## 1. Antes do espetáculo

- Usa uma única rede Wi‑Fi para todos os telemóveis, ou o hotspot do telemóvel da régie. Desativa o *isolamento de clientes* nos routers.
- Instala o APK, concede as permissões de microfone e notificações e, no menu **Aa**, escolhe *Manter a app ativa* para que o sistema não a adormeça.
- Carrega todos os telemóveis; a régie mostra os níveis de bateria e avisa abaixo de 20 %.

## 2. Papéis e números

- **Régie** — o hub; vê tudo, envia sinais, acopla telefones. Número 100.
- **Ator** — precisa do PIN; pode falar para a equipa e chamar qualquer pessoa. Números a partir de 201.
- **Espectador** — recebe chamadas, mensagens e sinais; só pode falar para a equipa. Números de seis dígitos.
- A régie pode mudar qualquer número e atribuir grupos (por exemplo *balcão*, *cave*).

## 3. Usar o telefone

- **1985** — levanta o auscultador (botão), espera o sinal, roda o disco; o número marca-se sozinho após uma breve pausa.
- **1995** — levanta e usa o teclado; a caderneta lista os números adicionados pela régie.
- **2003** — escreve o número e prime *Chamar*; as mensagens limitam-se a 160 carateres.
- Um telefone a tocar pode ser atendido pela notificação, mesmo com o ecrã bloqueado.

## 4. A linha partilhada na prática

- Seleciona dois dispositivos no separador Régie e prime *Acoplar 2*, ou *Pares auto* para emparelhar todos os telefones livres.
- Usa *Escutar* na régie para monitorizar uma linha sem ser ouvido.
- As opções permitem desativar a escuta do vizinho, o clique ou as mensagens entre telefones.

## 5. Sinais

- Escolhe um alvo (todos, espectadores, atores, dispositivos selecionados, um grupo, um número), um tipo e um texto, e prime *Enviar o sinal*.
- Exemplos: `Mãe;204455` adiciona um contacto; `1;É quase meia‑noite` deixa o ecrã preto com um texto discreto; `Dr. Voss;204455;Venham à cave` lança uma chamada fantasma que fala quando atendida.

## 6. Guião

- Constrói o espetáculo em passos com alvo, tipo, texto e pausa. **GO** envia o passo seguinte; **PLAY** executa o resto automaticamente a partir do passo selecionado.
- Exporta o guião como JSON e importa-o noutro telemóvel de régie.

## 7. Resolução de problemas

- *Nenhum hub encontrado* — confirma que estão na mesma rede, desativa o isolamento de clientes ou escreve o endereço do hub.
- *PIN / código de sala errado* — a régie mostra-os no ecrã inicial.
- *Porta ocupada* — fecha qualquer outra app que use a porta 8470.
- *Chamadas mudas* — verifica a permissão do microfone e o interruptor do altifalante no separador Voz.

## 8. Internet, VoIP e rede móvel (1.1)

- **Chave de rede** — no ecrã inicial ativa *Ligação cifrada*; a régie mostra uma chave (xxxx‑xxxx‑xxxx‑xxxx). Escreve‑a no campo *Chave de rede* de cada outro telemóvel. Sem a chave certa o hub recusa a ligação. Usa‑a sempre que o tráfego saia da rede local.
- **Pela internet** — reencaminha TCP 8470 e UDP 8472 no router para o telemóvel da régie e indica o endereço público (`host` ou `host:porta`). Com dados móveis não é preciso mais nada.
- **P2P** — no separador Régie a opção *Chamadas diretas* alterna desligadas / só rede local / em qualquer lado. *Em qualquer lado* deixa dois telemóveis conhecer o IP público um do outro; a régie não pode monitorizar, silenciar nem intervir enquanto a chamada é direta (volta ao relé automaticamente quando precisa).
- **Linha SIP** — Definições → *Linha VoIP / SIP…*: servidor, utilizador, palavra‑passe, domínio opcional e número interno para chamadas recebidas (100 por omissão). Reinicia o hub para aplicar. A equipa marca `0` + número externo; os espectadores não podem chamar para fora. Cada chamada é limitada a 15 minutos. SIP e RTP não são cifrados.
- **SMS** — Definições → *Porta SMS* (requer SIM e permissões SMS). A equipa escolhe *SMS para um número real* no separador Mensagens; os SMS recebidos aparecem como mensagens de *SMS*. Limite: 20 por minuto. Aplicam‑se as tarifas do operador.
- **Chamada real** — o cue *chamada real* abre o marcador com o número. O Android não permite a uma app meter uma chamada celular na linha partilhada: o áudio GSM não pode ser ligado; para isso usa a linha SIP.

## 9. Ligar sem router (1.2)

- **Mesmo hub, qualquer ligação** — chamadas, linha partilhada, cues, cifra e PIN funcionam da mesma maneira; muda só o caminho do telemóvel até ao hub.
- **Hotspot, partilha por USB, Ethernet** — qualquer ligação que dê aos telemóveis uma rede IP funciona sem configuração; usa o endereço ou deixa o hub ser encontrado.
- **Bluetooth** — no telemóvel da régie inicia o hub com *Ligação Bluetooth: SIM* e carrega em *Tornar visível por Bluetooth*. Nos outros carrega em *Procurar por Bluetooth* e escolhe o telemóvel da régie (🔵). O alcance é curto e cabem cerca de 7 telemóveis por hub; a voz é comprimida automaticamente. Não é preciso emparelhar; o acesso continua protegido por PIN, código da sala e chave de rede.
- **Wi‑Fi Direct** — no telemóvel da régie inicia o hub com *Grupo Wi‑Fi Direct: SIM*. Nos outros carrega em *Procurar Wi‑Fi Direct* e escolhe o telemóvel da régie (📡); pode pedir para aceitares cada telemóvel. Depois de entrar, o hub aparece na lista. Normalmente os telemóveis não podem ficar no Wi‑Fi habitual dentro de um grupo Wi‑Fi Direct, e alguns modelos não suportam ambos.
- **mDNS** — o hub também se anuncia por mDNS/DNS‑SD (🌐), o que o torna visível em redes que bloqueiam a difusão habitual.
- **Não incluídos** — Wi‑Fi Aware (poucos telemóveis) e Nearby Connections da Google (exige Google Play Services, que esta app trade‑free evita). O Bluetooth Low Energy é demasiado lento para voz.

## 10. Plug and play (1.3)

- **Telemóvel da régie** — escolhe *Régie* e *Iniciar o hub*. Com *Plug & play* ligado (predefinição) o CUPLAJ abre Wi‑Fi / hotspot, mDNS, Bluetooth e Wi‑Fi Aware, e cria um grupo Wi‑Fi Direct se não houver rede Wi‑Fi. Aceita o único pedido de permissões. As definições mostram as ligações ativas.
- **Outros telemóveis, o mais fácil** — Definições → *Partilhar convite para espectadores* (ou *atores*) no telemóvel da régie; envia o link por qualquer app. Quem lhe tocar, com o CUPLAJ instalado, fica configurado (endereço, chave, PIN, sala) e ligado.
- **Outros telemóveis, sem link** — escolhe um papel e toca em *Encontrar e juntar‑me automaticamente*. O CUPLAJ procura em todas as ligações ao mesmo tempo (Wi‑Fi, mDNS, Bluetooth, Wi‑Fi Direct, Wi‑Fi Aware) e liga‑se ao melhor hub: primeiro a rede local, por último o Bluetooth.
- **Wi‑Fi Aware** — os telemóveis compatíveis encontram‑se sem router nem configuração. Muitos não o suportam; nada se estraga se o teu não suportar.
- **Segurança** — um convite só pode criar um espectador ou um ator, nunca um telemóvel de régie. Os links contêm a chave e o PIN; envia‑os só a quem convidares.

## 11. Reforço + AIR (1.4)

- **Hub mais seguro** — PIN aleatório de 6 dígitos, chave nova em cada ligação, limites para ligações não autenticadas e tamanho das mensagens, bloqueio após PIN errados repetidos e confirmação antes de aplicar um link de convite.
- **LUMEN (luz)** — o telemóvel da Regia pisca a lanterna; outro telemóvel observa com a câmara traseira. Sala com pouca luz, 1–3 m, lanterna apontada à câmara. Lento (alguns segundos) mas robusto.
- **SONUS (som)** — o altifalante reproduz um código de tons, quase ultrassónico (inaudível para a maioria dos adultos) ou audível; o outro telemóvel ouve com o microfone. Leva sinais e um convite ao hub (endereço e PIN).
- **TACTUS (vibração)** — o motor de vibração envia pulsos curtos e longos; outro telemóvel pousado na mesma superfície sente‑os com o acelerómetro. Funciona em silêncio e no escuro, para sinais curtos.
- **Utilização** — Definições → *Ligações pelo ar*: ative as escutas pretendidas (câmara / microfone / acelerómetro; o telemóvel pede permissão) e, no telemóvel da Regia, prima um botão de envio. Os pacotes levam uma etiqueta curta de autenticação derivada da chave de rede e não podem ser repetidos.

## 12. Controlo do espetáculo (1.5)

- **Mesmo instante** — comece o texto com `@3.5;` para disparar daqui a 3,5 s em todos os telemóveis ao mesmo tempo. Os telemóveis sincronizam o relógio com o hub ao ligar e a cada minuto; um sinal com mais de 10 s de atraso é descartado.
- **PÂNICO** — o sinal *PÂNICO: parar tudo* pára toque, vibração, voz, efeitos sonoros, ecrãs pretos e de cor, chamadas falsas e sinais agendados. Também funciona por AIR.
- **Cor** — `#RRGGBB;segundos;pulsos por segundo` preenche o ecrã. Os pulsos são limitados a 2 por segundo para o público fotossensível; um toque longo fecha‑o.
- **Sondagem** — `pergunta;opção 1;opção 2;…` pergunta a espectadores e atores; um voto por telemóvel (alterável). *Terminar a sondagem* mostra o resultado à Regia. As sondagens podem ser passos do guião.
- **Entrega** — após cada sinal, a Regia vê *enviado a N de M telemóveis ligados*. **PTT físico** — Definições → *Tecla PTT física* transforma o botão do auricular e volume + em push‑to‑talk com a app em primeiro plano.

---

[← README](../README.pt.md)
