package com.example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    // URL base do diretório de demonstrações contábeis da ANS (ano fixado em 2025).
    // Em uma evolução do projeto, seria possível identificar automaticamente
    // o ano mais recente disponível comparando com a data atual.
    private static final String BASE_URL =
            "https://dadosabertos.ans.gov.br/FTP/PDA/demonstracoes_contabeis/2025/";

    public static void main(String[] args) throws Exception {

        // Cria o cliente HTTP reutilizado para realizar as requisições
        HttpClient client = HttpClient.newHttpClient();
        Scanner scanner = new Scanner(System.in);

        //  Acessa o diretório público da ANS
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                // User-Agent utilizado para evitar bloqueios por parte do servidor
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        // Utiliza ofString() para interpretar a resposta HTTP como texto (HTML)
        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        // Extrai os nomes dos arquivos ZIP presentes no HTML
        // Espera encontrar links que terminem com ".zip"
        List<String> zips = extrairZips(response.body());

        if (zips.isEmpty()) {
            System.out.println("Nenhum arquivo ZIP encontrado.");
            return;
        }

        // Identifica os períodos (ano + trimestre) e remove duplicações usando HashSet
        Set<Periodo> periodosUnicos = new HashSet<>(extrairPeriodos(zips));

        // Converte para lista para permitir ordenação
        List<Periodo> periodosOrdenados = new ArrayList<>(periodosUnicos);

        // Ordena os períodos em ordem cronológica crescente
        Collections.sort(periodosOrdenados);

        // Solicita ao usuário a quantidade de últimos trimestres a serem baixados
        System.out.print("Quantos últimos trimestres deseja baixar? ");
        int qtdTrimestres = scanner.nextInt();

        // Calcula o índice inicial considerando os últimos trimestres
        // Math.max evita índices negativos caso o valor informado seja maior que o total
        int inicio = Math.max(0, periodosOrdenados.size() - qtdTrimestres);

        // Seleciona apenas os últimos trimestres solicitados
        List<Periodo> ultimosTrimestres =
                periodosOrdenados.subList(inicio, periodosOrdenados.size());

        // Lista que armazenará apenas os arquivos ZIP compatíveis com os períodos escolhidos
        List<String> zipsParaDownload = new ArrayList<>();

        // Analisa ZIP por ZIP verificando se pertence a algum dos trimestres selecionados
        for (String zip : zips) {
            for (Periodo p : ultimosTrimestres) {
                if (zip.matches(".*" + p.regex() + ".*")) {
                    zipsParaDownload.add(zip);
                    break; // Evita verificações desnecessárias
                }
            }
        }

        // Caso nenhum arquivo corresponda aos períodos informados
        if (zipsParaDownload.isEmpty()) {
            System.out.println("Nenhum arquivo corresponde aos trimestres selecionados.");
            return;
        }

        // Cria o diretório local onde os arquivos serão salvos
        Path pasta = Paths.get("downloads");
        Files.createDirectories(pasta);

        //  Realiza o download dos arquivos selecionados
        for (String zip : zipsParaDownload) {
            String zipUrl = BASE_URL + zip;
            Path destino = pasta.resolve(zip);
            baixarZip(client, zipUrl, destino);
        }

        System.out.println("Download concluído com sucesso.");
        scanner.close();
    }

    // ===================== UTILITÁRIOS =====================

    /**
     * Extrai os nomes dos arquivos ZIP presentes no HTML.
     *
     * @param html conteúdo HTML retornado pelo servidor
     * @return lista com os nomes dos arquivos ZIP encontrados
     */
    static List<String> extrairZips(String html) {

        // Regex que captura qualquer link que termine com ".zip"
        Pattern pattern = Pattern.compile("href=\"([^\"]+\\.zip)\"", Pattern.CASE_INSENSITIVE);

        // Associa o padrão regex ao HTML para posterior busca
        Matcher matcher = pattern.matcher(html);

        // Lista que armazenará os nomes dos arquivos ZIP encontrados
        List<String> zips = new ArrayList<>();

        // Percorre todas as ocorrências encontradas
        while (matcher.find()) {
            zips.add(matcher.group(1));
        }

        return zips;
    }

    /**
     * Extrai ano e trimestre a partir dos nomes dos arquivos ZIP.
     *
     * @param zips lista de nomes de arquivos ZIP
     * @return lista de objetos Periodo (ano + trimestre)
     */
    static List<Periodo> extrairPeriodos(List<String> zips) {

        List<Periodo> periodos = new ArrayList<>();

        // Regex para nomes no formato: 2025_1T, 2025-1T, etc.
        Pattern anoAntes = Pattern.compile("(20\\d{2}).*?(\\d)T");

        // Regex para nomes no formato: 1T2025, 2T2025, etc.
        Pattern triAntes = Pattern.compile("(\\d)T(20\\d{2})");

        // Analisa cada nome de arquivo individualmente
        for (String zip : zips) {

            Matcher m1 = anoAntes.matcher(zip);
            Matcher m2 = triAntes.matcher(zip);

            // Caso o ano apareça antes do trimestre
            if (m1.find()) {
                periodos.add(new Periodo(
                        Integer.parseInt(m1.group(1)), // ano
                        Integer.parseInt(m1.group(2))  // trimestre
                ));
            }
            // Caso o trimestre apareça antes do ano
            else if (m2.find()) {
                periodos.add(new Periodo(
                        Integer.parseInt(m2.group(2)), // ano
                        Integer.parseInt(m2.group(1))  // trimestre
                ));
            }
        }

        return periodos;
    }

    /**
     * Realiza o download binário de um arquivo ZIP e o salva localmente.
     *
     * @param client  cliente HTTP reutilizado
     * @param url     URL completa do arquivo ZIP
     * @param destino caminho local onde o arquivo será salvo
     */
    static void baixarZip(HttpClient client, String url, Path destino)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();

        // Utiliza ofByteArray() para evitar corrupção de arquivos binários
        HttpResponse<byte[]> response =
                client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        // Grava o arquivo ZIP no diretório local
        Files.write(destino, response.body());
    }

    // ===================== MODELO =====================

    /**
     * Representa um período contábil (ano + trimestre).
     */
    static class Periodo implements Comparable<Periodo> {

        int ano;
        int trimestre;

        Periodo(int ano, int trimestre) {
            this.ano = ano;
            this.trimestre = trimestre;
        }

        // Regex que cobre formatos como:
        // 2025_1T, 2025-1T, 1T2025
        String regex() {
            return "(" + ano + ".*" + trimestre + "T)|(" + trimestre + "T.*" + ano + ")";
        }

        // Ordenação cronológica (ano → trimestre)
        @Override
        public int compareTo(Periodo o) {
            if (this.ano != o.ano) {
                return Integer.compare(this.ano, o.ano);
            }
            return Integer.compare(this.trimestre, o.trimestre);
        }

        // Dois períodos são iguais se ano e trimestre forem iguais
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Periodo)) return false;
            Periodo p = (Periodo) o;
            return ano == p.ano && trimestre == p.trimestre;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ano, trimestre);
        }
    }
}
