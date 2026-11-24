# CUPLAJ

[English](README.md) · [Română](README.ro.md) · [Français](README.fr.md) · [Italiano](README.it.md) · [Español](README.es.md) · **Português** · [Deutsch](README.de.md) · [Magyar](README.hu.md)

<p align="center"><img src="docs/logo.svg" width="160" alt="CUPLAJ"></p>

> **Telefonia de linha partilhada para teatro imersivo**

_Versão: 1.0.0 · Última atualização: 2026-11-24_

O CUPLAJ transforma uma frota de telemóveis Android comuns numa central telefónica privada para teatro imersivo. Organizadores e atores falam com os espectadores (e entre si) pela rede Wi‑Fi local do espaço — sem nuvem, sem contas, sem anúncios. A sua função distintiva é o *cuplaj*: a **linha partilhada** dos telefones fixos dos anos 80 até cerca de 2006, em que dois assinantes partilhavam a mesma linha, tocavam com cadências diferentes e podiam ouvir-se um ao outro.

## Funcionalidades

- **Três épocas de telefone** — 1985 disco rotativo (impulsos audíveis), 1995 teclado (tons DTMF), 2003 telemóvel (SMS, 160 carateres). A régie pode mudar de época em direto.
- **Linha partilhada (cuplaj)** — dois telefones partilham uma linha; uma chamada para um faz tocar ambos com a cadência do assinante chamado; o vizinho que atender primeiro fica com a chamada; levantar o auscultador durante a conversa do vizinho permite ouvir e até intervir.
- **Sem infraestrutura** — um telefone (a régie) é o hub. Os outros entram por Wi‑Fi ou pelo hotspot do hub; os hubs são encontrados automaticamente; código de sala e PIN opcionais.
- **Voz** — chamadas reais (8 kHz), anúncio premir‑para‑falar a partir da régie, canal de bastidores para a equipa.
- **Sinais** — texto em ecrã inteiro, vibração, toque, flash, altifalante, mudança de época, novo contacto, voz falada (síntese local), efeitos sonoros, ecrã preto, chamada fantasma.
- **Guião** — lista de sinais com GO e PLAY (temporizado), reordenar, importar/exportar JSON.
- **Ferramentas da régie** — números, grupos, acoplar/desacoplar, pares automáticos, escuta discreta, silenciar, esquecer, níveis de bateria, registo de eventos, estado que sobrevive a reinícios, limitação de fluxo.
- **8 idiomas e acessibilidade** — inglês, romeno, francês, italiano, espanhol, português, alemão, húngaro; tamanho do texto ajustável; atender a partir do ecrã bloqueado; funciona em segundo plano.

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
- [Site de apresentação](https://chiuta.github.io/CUPLAJ/)

## Estado real

A lógica do hub (chamadas, linha partilhada, permissões, limites, persistência) está coberta por ~55 verificações automáticas executadas numa JVM. A interface Android e o caminho de áudio passaram a verificação de tipos contra o SDK Android 14, mas **ainda não foram testados em dispositivos reais**; o APK é construído pelo GitHub Actions. Testa com alguns telemóveis antes de um espetáculo. O tráfego da rede local não é cifrado (o PIN e o código de sala afastam estranhos de boa‑fé, não são criptografia).

## Compilação

Requer JDK 17. `./gradlew assembleRelease` produz `app/build/outputs/apk/release/app-release.apk` (assinado com a chave de depuração, instalável diretamente). Testes unitários: `./gradlew test`. O workflow em `.github/workflows` faz ambos, com ferramentas Node ≥ 24 em Ubuntu 26.04.

## Privacidade

Sem estatísticas, sem servidores, sem contas. O áudio e as mensagens circulam apenas na vossa rede local, entre os telemóveis e o hub. O hub guarda números, grupos e acoplamentos no telemóvel da régie para que um reinício não os perca.

## Apoia o projeto

O CUPLAJ é gratuito e trade‑free. Se ajudar o teu espetáculo, podes oferecer um café ao autor: <https://buymeacoffee.com/echo.of.the.strings>.

## Licença

MIT © Alexandru‑Ionuț Chiuță (Alexio).
