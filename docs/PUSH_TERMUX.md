# Como enviar a atualização para o GitHub pelo Termux

Não cole seu token em chats. Use o token apenas no seu Termux.

## 1. Preparar Termux

```bash
pkg update -y
pkg install git unzip -y
git config --global user.name "VouizS"
git config --global user.email "scared7j@gmail.com"
```

## 2. Baixar/receber o ZIP do projeto

Coloque o ZIP em `/storage/emulated/0/Download/HubSyncBr_v0_1.zip`.

Depois rode:

```bash
cd ~
rm -rf HubSyncBr
unzip /storage/emulated/0/Download/HubSyncBr_v0_1.zip -d ~
cd ~/HubSyncBr_v0_1
```

## 3. Conectar com o repositório vazio

```bash
git init
git branch -M main
git remote add origin https://github.com/VouizS/HubSyncBr.git
git add .
git commit -m "HubSyncBr 0.1 Dual View"
```

## 4. Push com token sem expor no histórico

```bash
read -s GITHUB_TOKEN
```

Cole seu token e aperte Enter. O texto não aparece na tela.

Agora rode:

```bash
git push https://VouizS:${GITHUB_TOKEN}@github.com/VouizS/HubSyncBr.git main
unset GITHUB_TOKEN
```

## 5. Baixar o APK

No GitHub:

1. Abra o repositório HubSyncBr.
2. Entre em **Actions**.
3. Abra o workflow **Android Debug APK**.
4. Espere ficar verde.
5. Baixe o artifact **HubSyncBr-debug-apk**.
6. Dentro dele estará o APK debug.
