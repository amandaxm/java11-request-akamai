import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class Main {
    static ExecutorService executor = Executors.newFixedThreadPool(6, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = newThread(runnable);
            System.out.println("Nova thread criada" + (thread.isDaemon() ? "daemon" : "") + " thread group:: " + thread.getThreadGroup());
            return thread;
        }

    });

    public static void main(String[] args) throws IOException, InterruptedException {


        //connectedAndPrintURLJavaOracle();
        httpAkamaiHttpClient();
    }


    private static void httpAkamaiHttpClient() {
        System.out.println("running Http/1.1 example...");
        try {
            HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).proxy(ProxySelector.getDefault()).build();
            long start = System.currentTimeMillis();

            HttpRequest mainRequest = HttpRequest.newBuilder()
                    .uri(URI.create("https://http2.akamai.com/demo/h2_demo_frame.html"))
                    .build();
            HttpResponse<String> response = httpClient.send(mainRequest, HttpResponse.BodyHandlers.ofString());
            System.out.println("Status code::" + response.statusCode());
            System.out.println("Headers::" + response.headers());

            String responseBody = response.body();
            System.out.println(response.body());


            List<Future<?>> future = new ArrayList<>();
            responseBody.
                    lines()
                    .filter(line -> line.trim().startsWith("<img height"))
                    .map(line -> line.substring(line.indexOf("src='") + 5, line.indexOf("'/>")))
                    .forEach(image -> {
                        Future<?> imgFuture = executor.submit(() -> {

                            HttpRequest imgRequest = HttpRequest.newBuilder()
                                    .uri(URI.create("https://http2.akamai.com" + image))
                                    .build();

                            try {
                                HttpResponse<String> imgResponse = httpClient.send(imgRequest, HttpResponse.BodyHandlers.ofString());
                                System.out.println("Imagem carregada" + image + "Status code::" + imgResponse.statusCode());
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }


                        });
                        future.add(imgFuture);
                        System.out.println("submetido ao futuro para imagem::" + image);
                    });

            future.forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

            });
            long end = System.currentTimeMillis();
            System.out.println("tempo de carregamento total " + (end - start));

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

            executor.shutdown();
        }
    }


    private static void connectedAndPrintURLJavaOracle() throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create("https://docs.oracle.com/javase/10/language/")).build();
//request requisição para o servidor
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status code::" + response.statusCode());
        System.out.println("Headers::" + response.headers());
        System.out.println(response.body());
    }

}