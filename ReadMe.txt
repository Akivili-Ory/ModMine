Este projeto é o meu primeiro mod de Minecraft e marca oficialmente a estreia do meu
portfólio de desenvolvimento. Ele nasceu do desejo aprendizado prático de programação, 
aplicando conceitos de [Neo Forge] na construção de novas mecânicas
elementos no jogo. Como primeiro passo na minha trajetória como desenvolvedor, este mod
Ele adiciona função de leilão com valor virtual sem ligação com nada.

My very first Minecraft mod and the starting point of my development portfolio. Built using NeoForge, 
it introduces a purely virtual in-game auction system designed to practice core programming and custom mechanics.

------------------------------------------------------------------------------------------------------------------------------------

Comandos / Commands
/leilao | Abre o Hud dos Leiloes
/leilao criar <valorInicial> <tempo> <unidade de tempo> | Leiloa o item que você está segurando na mão
/leilao lance <id> <valor> |Dá um lance em um leilão
/leilao cancelar <id> | Cancela um leilão e devolve o item ao vendedor
/leilao procurar <busca> | Abre o baú só com os leilões que combinam com a busca


exemplos
/leilao criar 50 1 horas
/leilao lance 5 100
/leilao procurar log mostra tudo que tem “log” no nome.
/leilao procurar madeira

Regras:

1. O lance precisa ser maior que o valor atual.

2. Você não pode dar lance no seu próprio leilão.

3. O dono só pode cancelar se ainda não houver lances. Operadores podem cancelar qualquer leilão.

4. Quando o tempo acaba, o item vai para quem deu o maior lance. Sem lances, volta para o vendedor.

Auction Rules

1 Valid Bids: Each new bid must be higher than the current highest bid.

2 No Self-Bidding: You cannot bid on your own auctions.

3 Cancellations: Sellers can only cancel an auction if no bids have been placed yet. Server operators (OPs) can cancel any auction at any time.

4 Auction Expiration: With bids: The item is automatically delivered to the highest bidder when the timer ends.
Without bids: The item is returned to the seller.
