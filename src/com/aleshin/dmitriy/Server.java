package com.aleshin.dmitriy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    private static class Handler extends Thread {
        private Socket socket;


        public Handler(Socket socket) {
            this.socket = socket;
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            String userName;
            Message message;

            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                message = connection.receive();
                userName = message.getData();
                if (message.getType() == MessageType.USER_NAME && !userName.isEmpty()
                        && !connectionMap.containsKey(userName)) {
                    connectionMap.put(userName,connection);
                    connection.send(new Message(MessageType.NAME_ACCEPTED));
                    break;
                }
            }
            return userName;
        }

        private void notifyUsers(Connection connection, String userName) throws IOException {
            for (String name : connectionMap.keySet()) {
                if (name.equals(userName)) {
                    continue;
                }
                connection.send(new Message(MessageType.USER_ADDED, name));
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
                    
                } else {
                    ConsoleHelper.writeMessage("Сообщение не является текстовым.");
                }
            }
        }

        @Override
        public void run() {
            try (Connection connection = new Connection(socket)){
                ConsoleHelper.writeMessage("Установленно соединение с адресом " + socket.getRemoteSocketAddress());

                //Выводить сообщение, что установлено новое соединение с удаленным адресом
                ConsoleHelper.writeMessage("Подключение к порту: " + connection.getRemoteSocketAddress());

                //Вызывать метод, реализующий рукопожатие с клиентом, сохраняя имя нового клиента
                String clientName = serverHandshake(connection);

                //Рассылать всем участникам чата информацию об имени присоединившегося участника (сообщение с типом USER_ADDED)
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, clientName));

                //Сообщать новому участнику о существующих участниках
                notifyUsers(connection, clientName);

                //Запускать главный цикл обработки сообщений сервером
                serverMainLoop(connection, clientName);

                connectionMap.remove(clientName);

                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, clientName));

                ConsoleHelper.writeMessage("Соединение с удаленным адресом закрыто");
                
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Ошибка при обмене данными с удаленным адресом");
            } catch (ClassNotFoundException e) {
                ConsoleHelper.writeMessage("Ошибка при обмене данными с удаленным адресом");
            }
        }
    }



    public static void sendBroadcastMessage(Message message) {

        for (String klient : connectionMap.keySet()) {
            try {
                connectionMap.get(klient).send(message);
            } catch (IOException e) {
                System.out.println("Сообщение не отправленно");
            }
        }
    }

    public static void main(String[] args) {
        // create port, @param port
        System.out.println("Укажите порт");
        try (ServerSocket serverSocket = new ServerSocket(ConsoleHelper.readInt())) {
            System.out.println("Server starting");
            while (true) {
                try  {
                    Socket socket = serverSocket.accept();
                    Handler handler = new Handler(socket);
                    handler.start();
                } catch (IOException e) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Error");
        }
    }
}
