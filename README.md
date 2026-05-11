# Tabuada do 10

Android app em Kotlin/Compose para treinar multiplicações de 1x1 até 10x10 com prática livre, notificações locais e repetição espaçada.

## O que o app faz

- Mostra contas redundantes, como `7x8` e `8x7`, como itens separados.
- Agenda perguntas por notificação local, com resposta digitada direto na notificação.
- Começa com 2 notificações por dia e permite ajustar a frequência.
- Dá prioridade às contas que você mais erra no treino livre.
- Se você acerta, a conta sai do dia e volta depois de 2, 3, 5, 8, 13 dias seguindo Fibonacci.
- Se você erra uma vez, a conta aparece mais uma vez no mesmo dia.
- Se você erra duas vezes no dia, ela entra em revisão intensiva com 2 perguntas por dia até você acertar.
- Ao atingir o limite configurável de acertos seguidos, por padrão 100, a conta vira aprendida e sai da sequência normal.
- Mostra estatísticas por multiplicação: vezes exibida, acertos, erros, tentativas recentes e sequência atual.

## Rodando localmente

1. Instale Android Studio, Android SDK recente e JDK 17.
2. Abra esta pasta como projeto.
3. Rode o app em um emulador ou aparelho Android.

## Verificação

```sh
./gradlew test
```

## Release

O projeto inclui GitHub Actions para CI e upload para a Play Console:

- `Android CI`: roda testes em push e pull request.
- `Android Closed Testing Release`: gera o `.aab` assinado e envia para a track `alpha`; pode ser acionado manualmente ou por tag `v*`.
- `Play Store Listing`: envia metadados, ícone, feature graphic e screenshots.

Secrets necessários:

- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_STORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`
- `PLAY_SERVICE_ACCOUNT_JSON`

Veja [play-store/google-play-api-setup.md](play-store/google-play-api-setup.md) para configurar a conta de serviço da Play Console.

Para release automático após configurar os secrets, crie uma tag como `v1.0.0` e faça push. O workflow usa o nome da tag como `versionName` e o número do run como `versionCode`.
