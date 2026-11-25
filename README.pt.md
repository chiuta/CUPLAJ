# CUPLAJ

[English](README.md) · [Română](README.ro.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Español](README.es.md) · **Português** · [Deutsch](README.de.md) · [Magyar](README.hu.md)

<p align="center"><img src="docs/logo.svg" width="160" alt="CUPLAJ"></p>

> **Telefonia de linha partilhada para teatro imersivo**

_Versão: 1.9.0 · Última atualização: 2026-11-25_

O CUPLAJ transforma uma frota de telemóveis Android comuns numa central telefónica privada para teatro imersivo. Organizadores e atores falam com os espectadores (e entre si) pela rede Wi‑Fi local do espaço — sem nuvem, sem contas, sem anúncios. A sua função distintiva é o *cuplaj*: a **linha partilhada** dos telefones fixos dos anos 80 até cerca de 2006, em que dois assinantes partilhavam a mesma linha, tocavam com cadências diferentes e podiam ouvir-se um ao outro.

**[Descarregar o APK](https://github.com/chiuta/CUPLAJ/releases/download/v1.9.0/CUPLAJ-1.9.0.apk) · [Descarregar para iPhone/iPad (.ipa)](https://github.com/chiuta/CUPLAJ/releases/download/v1.9.0/CUPLAJ-1.9.0-ios-unsigned.ipa) · [Todas as versões](https://github.com/chiuta/CUPLAJ/releases)**

## Funcionalidades

- **Três épocas de telefone** — 1985 disco rotativo (impulsos audíveis), 1995 teclado (tons DTMF), 2003 telemóvel (SMS, 160 carateres). A régie pode mudar de época em direto.
- **Linha partilhada (cuplaj)** — dois telefones partilham uma linha; uma chamada para um faz tocar ambos com a cadência do assinante chamado; o vizinho que atender primeiro fica com a chamada; levantar o auscultador durante a conversa do vizinho permite ouvir e até intervir.
- **Sem infraestrutura** — um telefone (a régie) é o hub. Os outros entram por Wi‑Fi ou pelo hotspot do hub; os hubs são encontrados automaticamente; código de sala e PIN opcionais.
- **Voz** — chamadas reais (8 kHz), anúncio premir‑para‑falar a partir da régie, canal de bastidores para a equipa.
- **Sinais** — texto em ecrã inteiro, vibração, toque, flash, altifalante, mudança de época, novo contacto, voz falada (síntese local), efeitos sonoros, ecrã preto, chamada fantasma.
- **Guião** — lista de sinais com GO e PLAY (temporizado), reordenar, importar/exportar JSON.
- **Ferramentas da régie** — números, grupos, acoplar/desacoplar, pares automáticos, escuta discreta, silenciar, esquecer, níveis de bateria, registo de eventos, estado que sobrevive a reinícios, limitação de fluxo.
- **8 idiomas e acessibilidade** — inglês, romeno, francês, italiano, espanhol, português, alemão, húngaro; tamanho do texto ajustável; atender a partir do ecrã bloqueado; funciona em segundo plano.
- **Internet e dados móveis (1.1)** — ligação com `host` ou `host:porta`; chave de rede AES‑128‑GCM opcional; compressão G.711 automática fora da rede local; keepalive.
- **Chamadas diretas P2P (1.1)** — o áudio vai de telemóvel a telemóvel por UDP com perfuração de NAT, com recurso ao relé do hub; a régie pode desligar, limitar à rede local (predefinição) ou abrir a qualquer lado.
- **Porta VoIP / SIP (1.1)** — o telemóvel da régie regista‑se num fornecedor SIP ou PBX; a equipa marca `0`+número para sair, as chamadas SIP recebidas tocam num número interno.
- **Rede móvel (1.1)** — porta SMS através do SIM do telemóvel da régie (envio e receção), cue de *chamada real* que abre o marcador e indicador Wi‑Fi / móvel.
- **Mais formas de ligação (1.2)** — Bluetooth (sem router, cerca de 7 telemóveis), Wi‑Fi Direct (o telemóvel da régie cria a sua própria rede Wi‑Fi) e descoberta mDNS, além de Wi‑Fi, hotspot, partilha por USB, Ethernet e dados móveis.
- **Plug and play (1.3)** — o telemóvel da régie abre sozinho todas as ligações úteis; os outros tocam em *Encontrar e juntar‑me automaticamente* ou num link de convite. Acrescenta Wi‑Fi Aware.
- **Reforçado e sem rede (1.4)** — auditoria com público hostil e correções, e CUPLAJ‑AIR: três protocolos originais que levam sinais e convites sem qualquer rede (lanterna → câmara, altifalante → microfone, vibração → acelerómetro).
- **Controlo do espetáculo (1.5)** — sinais que disparam no mesmo instante, sinal PÂNICO, cor em ecrã inteiro, sondagens ao público, confirmação de entrega e tecla PTT física. Comparado com Unity Intercom, Zello, Augmented Theatre, QLab/SOLOstudio e Livecube em `docs/COMPARISON.md`.
- **Tudo o que a comparação mostrava e cabe numa app só Android sem ligação (1.6)** — prioridade PA (ducking), OSC de entrada e saída para mesas de luz e som, e sinais com imagem, vídeo e clipe de áudio enviados pelo hub.
- **Espetáculo inteligente (1.7)** — zonas que seguem o público pelo som, regras («quando alguém entra na zona 3, envia‑lhe…»), contador de público, efeitos de voz, legendas, texto multilingue e personalizado, ramificação da história.
- **Ferramentas de cena (1.8)** — luzes Art‑Net/DMX, MIDI, onda de sinais, telefone estragado, segredo fragmentado, gravador fantasma, sinal de segurança (luzes de sala), Morse por vibração, relatório pós‑espetáculo.
- **iPhone e iPad** — cliente completo e anfitrião (régie) em Swift, mesmo protocolo do Android: os dois misturam‑se livremente no mesmo espetáculo; compila‑se a partir do código com o Xcode (`ios/`).

## Início rápido

1. Obtém o APK no separador *Actions* (artefacto `CUPLAJ-apk`) ou na página *Releases* e instala-o em cada telemóvel.
2. No telemóvel da régie ativa o hotspot (ou põe todos os telemóveis no mesmo Wi‑Fi).
3. No telemóvel da régie escolhe **Régie** → *Iniciar o hub*. Anota o PIN dos atores e, se quiseres, um código de sala.
4. Nos outros escolhe **Espectador** ou **Ator**, seleciona o hub na lista (ou escreve o endereço) e liga-te.
5. Cada telemóvel recebe um número (régie 100, atores 201…, espectadores 6 dígitos). Levanta o auscultador e chama.

## As três épocas

- **1985 · disco** — um disco que se roda a sério; os impulsos ouvem-se; dois toques longos.
- **1995 · teclas** — tons DTMF, caderneta de números, mensagens de pager.
- **2003 · telemóvel** — nomes na agenda, identificação de chamadas, SMS limitados a 160 carateres.

## Como funciona a linha partilhada

<p align="center"><img src="docs/partyline.svg" width="620" alt="party line"></p>

1. Dois telefones são *acoplados* pela régie (ou automaticamente em pares). Partilham a linha N, posições 1 e 2.
2. Uma chamada para o assinante 1 faz tocar ambos; o assinante 1 ouve a cadência do seu número (um toque longo), o assinante 2 a do seu (dois curtos).
3. Quem atende fica com a chamada; o outro telefone deixa de tocar.
4. Enquanto a linha está em uso, o vizinho que levanta o auscultador ouve a conversa e os interlocutores ouvem um leve clique.
5. Quem escuta pode *intervir* e falar; uma chamada para um telefone acoplado cuja linha está ocupada recebe o sinal de ocupado.
6. A régie pode monitorizar qualquer linha em silêncio (sem clique).

## Documentação

- [Guia de utilização](docs/GUIDE.pt.md)
- [Protocolo](docs/PROTOCOL.md) *(English)*
- [Auditoria adversarial](docs/AUDIT.md) *(English)*
- [Registo de alterações](CHANGELOG.md) *(English)*
- [Revisão honesta e comparação](docs/REVIEW.md) *(English)*
- [Site de apresentação](https://chiuta.github.io/CUPLAJ/)

## Estado real

A lógica do hub (chamadas, linha partilhada, permissões, limites, persistência) está coberta por mais de 250 verificações automáticas executadas numa JVM. A interface Android e o caminho de áudio passaram a verificação de tipos contra o SDK Android 14, mas **ainda não foram testados em dispositivos reais**; o APK é construído pelo GitHub Actions. Testa com alguns telemóveis antes de um espetáculo. O tráfego da rede local não é cifrado (o PIN e o código de sala afastam estranhos de boa‑fé, não são criptografia). A rede da versão 1.1 (cifra, P2P, SIP, SMS) está coberta por testes do hub contra um servidor SIP simulado, mas **não** foi experimentada em operadores, NAT ou SIM reais. As ligações Bluetooth, Wi‑Fi Direct e mDNS (1.2) partilham a mesma camada de transporte, testada em JVM com canais simulados, mas **não** foram experimentadas em dispositivos reais; o comportamento varia entre fabricantes. O plug and play (ligação automática, convites, Wi‑Fi Aware, 1.3) **não** foi testado em dispositivos; o formato dos convites é coberto por testes JVM. As correções da auditoria 1.4 e os codecs AIR estão cobertos por testes JVM (ruído simulado, deriva de relógio, chave errada, interferência); o código de lanterna, câmara, microfone, altifalante, motor e acelerómetro **não** está testado em telemóveis reais, e o alcance e a velocidade reais serão diferentes. As funções 1.5 estão cobertas por testes JVM (estimativa do relógio, atrasos, regras da sondagem, limites fotossensíveis); as sobreposições de ecrã, o botão do auricular e a precisão real do relógio **não** estão testados em dispositivos. As funções 1.6 estão cobertas por testes JVM (curva de ducking, fuzzing e inundação OSC, integridade das transferências e segurança dos caminhos, relé do hub); a reprodução de imagens, vídeo e clipes, o seletor de ficheiros e as caixas de definições **não** estão testados em dispositivos. Zonas, regras, contador, efeitos de voz e línguas estão cobertos por testes JVM (motor de regras, regras do hub, agregação do contador, filtros de voz medidos em frequência); as balizas sonoras reais em interiores, as medições com microfone e acelerómetro e a qualidade da voz em telemóveis **não** estão testadas em dispositivos. Uma baliza gravada pode ser reproduzida uma vez por número de sequência: as zonas servem para ambiente, não para segurança. Pacotes Art‑Net, fades DMX, mensagens MIDI, padrões Morse, relatório e lógica do hub (onda, telefone estragado, segredo, relatório) estão cobertos por testes JVM; o gravador fantasma, mesas de luz e dispositivos MIDI reais e tudo o que envolva telemóveis reais não estão testados. **iOS:** o núcleo Swift é testado em macOS na CI e a app compila para o simulador, mas **não foi testada em iPhones reais nem com um fornecedor SIP real**.

## Compilação

Requer JDK 17. `./gradlew assembleRelease` produz `app/build/outputs/apk/release/app-release.apk` (assinado com a chave de depuração, instalável diretamente). Testes unitários: `./gradlew test`. O workflow em `.github/workflows` faz ambos, com ferramentas Node ≥ 24 em Ubuntu 26.04.

## Privacidade

Sem estatísticas, sem servidores, sem contas. O áudio e as mensagens circulam apenas na vossa rede local, entre os telemóveis e o hub. O hub guarda números, grupos e acoplamentos no telemóvel da régie para que um reinício não os perca.

## Apoia o projeto

O CUPLAJ é gratuito e trade‑free. Se ajudar o teu espetáculo, podes oferecer um café ao autor: <https://buymeacoffee.com/echo.of.the.strings>.

## Licença

MIT © Alexandru‑Ionuț Chiuță (Alexio).
