# Instruções para trabalhar no MiuiX-Files

Repositório oficial do **MiuiX-Files**, gerenciador de arquivos Android em Jetpack Compose com o design system HyperOS / MIUIX (`compose-miuix-ui`), inspirado na estrutura e recursos do ES File Explorer.

## Comunicação e Escopo

- Comunique-se em português brasileiro, com mensagens curtas e objetivas.
- Economize cotas: agrupe operações independentes e evite perguntas redundantes.
- Produção e testes escritos em Kotlin moderno. Sem XML de layouts antigos nem bibliotecas legadas de Views.
- Mantenha o design alinhado aos componentes MIUIX (`MiuixTheme`, `Card`, `SmallTitle`, `TopAppBar`, etc.).

## Estrutura do Projeto

```
.
├── app/
│   ├── build.gradle.kts          # Configuração do módulo Android e dependências MIUIX
│   └── src/main/
│       ├── AndroidManifest.xml   # Permissões de armazenamento e FileProvider
│       ├── res/                  # Strings, temas, mipmaps e drawables
│       └── java/top/yukonga/miuix/files/
│           ├── MainActivity.kt   # Atividade principal e gerenciamento da pilha de telas
│           ├── core/
│           │   ├── model/        # Modelos de dados (FileItem, StorageVolume, FileCategory)
│           │   ├── storage/      # StorageManager (volumes, listagem) e FileOperations (copiar, mover, zip)
│           │   ├── cleaner/      # CleanerEngine (varredura e limpeza de lixo)
│           │   └── apps/         # AppManager (listagem e backup de APKs)
│           └── ui/
│               ├── theme/        # Tema MIUIX e persistência de preferências
│               ├── components/   # StorageCard, CategoryGrid, FileListItem, BreadcrumbBar, FileActionBottomBar
│               └── screens/      # HomeScreen, ExplorerScreen, CategoryScreen, CleanerScreen, AppManagerScreen, SettingsScreen
├── gradle/                       # Wrapper Gradle 9.8.1
├── build.gradle.kts              # Script raiz de build
├── settings.gradle.kts           # Configuração de repositórios e módulos
└── local.properties              # Apontamento do Android SDK local
```
