import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Main {


    public static void main(String[] args) throws IOException, InterruptedException {

        httpAkamaiHttpClient();
    }


    private static void httpAkamaiHttpClient() {
        System.out.println("running Http/1.1 example...");
        ExecutorService executor = Executors.newFixedThreadPool(6);

        try {

            HttpClient httpClient = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_1_1)
                    .build();
            long start = System.currentTimeMillis();
            HttpRequest mainRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://http2.akamai.com/demo/h2_demo_frame.html"))
                    .build();

            HttpResponse mainResponse = httpClient.send(mainRequest, HttpResponse.BodyHandlers.ofString());

            String responseBody = (String) mainResponse.body();
            List<Future<?>> futures = new ArrayList<>();

// For each image resource in the main HTML, send a request on a separate thread
            responseBody.lines()
                    .filter(line -> line.trim().startsWith("<img height"))
                    .map(line -> line.substring(line.indexOf("src='") + 5, line.indexOf("'/>")))
                    .forEach(image -> {

                        Future imgFuture = executor.submit(() -> {
                            HttpRequest imgRequest = HttpRequest.newBuilder()
                                    .uri(URI.create("https://http2.akamai.com" + image))
                                    .build();
                            try {
                                HttpResponse imageResponse = httpClient.send(imgRequest, HttpResponse.BodyHandlers.ofString());
                                System.out.println("Carregando " + image + ", status code: " + imageResponse.statusCode());
                            } catch (IOException | InterruptedException ex) {
                                System.out.println("Erro durante a requisicao   ");
                            }
                        });
                        futures.add(imgFuture);
                    });

// Wait for all submitted image loads to be completed
            futures.forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException ex) {
                    System.out.println("Esperando imagem carregar");
                }
            });
            long end = System.currentTimeMillis();
            System.out.println("tempo de carregamento total " + (end - start)+"mls");

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
}