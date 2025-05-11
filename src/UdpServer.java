import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class UdpServer {
    public static boolean CheckIpArea(DatagramSocket serverSocket){
        byte[] receiveData = new byte[1];
        DatagramPacket receivePacket = new DatagramPacket(
                receiveData,
                receiveData.length
        );
        try{
            serverSocket.receive(receivePacket);
            String message = new String(
                    receivePacket.getData(),
                    0,
                    receivePacket.getLength()
            );
            System.out.println("===>["+new Date()+"] RECV BUF FROM UDP CLIENT: BUF" + message);
            System.out.println("===>["+new Date()+"] RECV BUF FROM UDP CLIENT: BUF" + Arrays.toString(receivePacket.getData()));

            InetAddress clientAddress = receivePacket.getAddress();
            String url = "https://qifu-api.baidubce.com/ip/geo/v1/district?ip="+clientAddress.getHostAddress();
            System.out.printf("===>url:"+url+"\n");
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)  // HTTP/2 preferred
                    .followRedirects(HttpClient.Redirect.NORMAL)  // follow redirects
                    .connectTimeout(Duration.ofSeconds(20))  // timeout
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            try{
                HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println("Status code: " + resp.statusCode());
                System.out.println("Response body: " + resp.body());
                ObjectMapper mapper =new ObjectMapper();
                Map<String,Object> result = mapper.readValue(resp.body(), new TypeReference<Map<String, Object>>() {
                });
                var _code = result.get("code");
                if(_code.equals("Success")){
                    Map<String,Object> _data = (Map<String,Object>)result.get("data");
                    System.out.println("ip data: ===> " +
                            result.get("ip") + " " +
                            _data.get("continent") + " " +
                            _data.get("country") + " " +
                            _data.get("zipcode") + " " +
                            _data.get("owner") + " " +
                            _data.get("isp") + " " +
                            _data.get("adcode") + " " +
                            _data.get("prov") + " " +
                            _data.get("city") + " " +
                            _data.get("district"));
                    if (!_data.get("country").toString().isEmpty() && !_data.get("country").equals("中国")){
                        System.out.println("===>境外IP访问："+result.get("ip"));
                        return true;
                    }
                }
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    public static void main(String[] args) {
        int port = 8850;
        try {
            DatagramSocket serverSocket = new DatagramSocket(port);
            System.out.println("===> udp server listen on port: " + port);
            byte[] receiveData = new byte[1024];

            while (true) {
                if (CheckIpArea(serverSocket)){
                    continue;
                }

                DatagramPacket receivePacket = new DatagramPacket(
                        receiveData,
                        receiveData.length
                );
                serverSocket.receive(receivePacket);
                String message = new String(
                        receivePacket.getData(),
                        0,
                        receivePacket.getLength()
                );
                System.out.println("===>["+new Date()+"] RECV BUF FROM UDP CLIENT: BUF" + message);
                System.out.println("===>["+new Date()+"] RECV BUF FROM UDP CLIENT: BUF" + Arrays.toString(receivePacket.getData()));

                InetAddress clientAddress = receivePacket.getAddress();

                int clientPort = receivePacket.getPort();
                String response = "===>UDP server Data:" + message + " "+ new Date();
                byte[] sendData = response.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(
                        sendData,
                        sendData.length,
                        clientAddress,
                        clientPort
                );
                serverSocket.send(sendPacket);
            }
        } catch (SocketException e) {
            System.err.println("===>Socket error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("===>IO error: " + e.getMessage());
        }
    }
}