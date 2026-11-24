# CUPLAJ — Guia de utilização

[English](GUIDE.md) · [Română](GUIDE.ro.md) · [Français](GUIDE.fr.md) · [Italiano](GUIDE.it.md) · [Español](GUIDE.es.md) · **Português** · [Deutsch](GUIDE.de.md) · [Magyar](GUIDE.hu.md)

_Versão: 1.1.0 · Última atualização: 2026-11-24_

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

---

[← README](../README.pt.md)
