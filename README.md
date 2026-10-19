# MiuiX Files 📁

Gerenciador de arquivos para Android moderno, fluido e poderoso, construído com **Jetpack Compose** e o design system **HyperOS / MIUIX** ([compose-miuix-ui](https://github.com/compose-miuix-ui/miuix)).

Inspirado na arquitetura, conjunto de ferramentas e fluxo do clássico **ES File Explorer**, porém com código 100% limpo, sem anúncios, sem rastreadores ou bloatware.

---

## ✨ Recursos

- **Visual MIUIX / HyperOS**:
  - Cartões em estilo squircle, tipografia de alta legibilidade, efeitos de desfoque (blur) e suporte total a temas Claro, Escuro e **Monet Dynamic Color** (Android 12+).
- **Dashboard Inicial (Estilo ES File Explorer)**:
  - Cartões de armazenamento (Armazenamento Interno e Cartão SD removível) com barra de progresso em tempo real e visualização de espaço usado/livre.
  - Grade com os 8 atalhos rápidos clássicos: **Imagens**, **Vídeos**, **Músicas**, **Documentos**, **Apps**, **Compactados**, **Downloads** e **Limpeza**.
  - Acesso rápido a arquivos recentes.
- **Explorador de Arquivos Completo**:
  - Barra de navegação em migalhas de pão (*breadcrumbs*) interativa e rolável.
  - Visualização de arquivos e pastas com metadados (tamanho, quantidade de itens, data de modificação).
  - Modo de seleção múltipla (*batch actions*).
  - Operações completas de arquivo: Copiar, Cortar (Mover), Excluir, Renomear, Criar pastas.
  - Compactação em `.zip` e extração nativa.
  - Alternância rápida para exibir/ocultar arquivos ocultos (`.nomedia`, etc.).
- **🧹 Limpeza de Sistema (Cleaner)**:
  - Análise rápida de arquivos temporários (`.tmp`), arquivos de log (`.log`), diretórios órfãos/vazios e cache de miniaturas.
  - Botão de limpeza segura com cálculo de espaço liberado.
- **📱 Gerenciador de Aplicativos (App Manager)**:
  - Listagem de aplicativos instalados (separação entre apps do usuário e do sistema).
  - Extração e **Backup do APK** para `/storage/emulated/0/AppBackup/`.
  - Inicialização e desinstalação direta de apps.

---

## 🛠️ Comandos de Build

| Ação | Comando |
| :--- | :--- |
| **Compilar APK Debug** | `./gradlew assembleDebug` |
| **Compilar APK Release** | `./gradlew assembleRelease` |
| **Instalar no dispositivo** | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| **Limpar build** | `./gradlew clean` |

---

## 📦 Tecnologias

- **Linguagem**: Kotlin 2.4.x
- **UI Toolkit**: Jetpack Compose & Compose Multiplatform
- **Design System**: [compose-miuix-ui](https://github.com/compose-miuix-ui/miuix) (`miuix-ui`, `miuix-icons`, `miuix-blur`, `miuix-preference`)
- **Arquitetura**: MVVM, Coroutines & Kotlin Flow
- **SDK Alvo**: Android 16 (API 36), compatível a partir do Android 8.0 (API 26)
