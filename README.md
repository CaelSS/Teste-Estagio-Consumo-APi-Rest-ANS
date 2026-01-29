# Download de Demonstrações Contábeis da ANS

Aplicação desenvolvida em Java que acessa o portal de dados abertos da ANS,
identifica automaticamente os períodos contábeis disponíveis e realiza o
download dos arquivos ZIP referentes aos últimos trimestres informados pelo usuário.


## 🛠️ Tecnologias utilizadas

- Java 17  
- HTTP Client (`java.net.http`)  
- Expressões regulares (Regex – `Pattern` / `Matcher`)  
- Manipulação de arquivos (`java.nio.file`)  
- Collections (`List`, `Set`, `HashSet`)  


## ▶️ Como executar

1. Clone este repositório
2. Compile o projeto com Java 17 (Se nao tiver, precisar ser baixado no site da oracle)
3. Execute a classe `Main`
4. Informe a quantidade de últimos trimestres desejados quando for requisitado
5. Os arquivos serão baixados para a pasta `downloads/` ( que fica dentro do repositorio que você clonou no seu computador)
   

## 🧠 Funcionamento da aplicação

1. A aplicação acessa o diretório público da ANS
2. Extrai os nomes dos arquivos ZIP disponíveis
3. Identifica ano e trimestre a partir do nome dos arquivos
4. Remove períodos duplicados e ordena cronologicamente
5. Solicita ao usuário a quantidade de últimos trimestres
6. Filtra os arquivos correspondentes aos períodos selecionados
7. Realiza o download dos arquivos ZIP

