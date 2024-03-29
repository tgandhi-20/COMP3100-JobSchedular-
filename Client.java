import java.net.*;
import java.util.*;
import java.io.*;

public class Client {

    private static Socket s;

    // setup args
    private static final String hostname = "localhost";
    private static final int serverPort = 50000;

    // streams
    private static BufferedReader bfr;
    private static InputStreamReader din;
    private static DataOutputStream dout;

    // commands
    private static final String HELO = "HELO";
    private static final String OK = "OK";
    private static final String AUTH = "AUTH";
    private static final String REDY = "REDY";
    private static final String JOBN = "JOBN";
    private static final String JCPL = "JCPL";
    private static final String GETS = "GETS";
    private static final String SCHD = "SCHD";
    private static final String NONE = "NONE";
    private static final String QUIT = "QUIT";

    // buffer fields
    private static String stringBuffer; /*
                                         * will hold the current message from the server stored in a string (
                                         */
    private static String[] fieldBuffer; /*
                                          * will hold the current message from the server as an array of strings
                                          * (created from stringBuffer)
                                          */

    private static String scheduleString; // string to be scheduled

    public static void main(String[] args) throws IOException {
        setup();

        try {
            writeBytes(HELO); // client sends HELO

            // server replies with OK

            writeBytes(AUTH + " " + System.getProperty("user.name"));

            stringBuffer = bfr.readLine();

            // server replies with OK after printing out a welcome message and writing
            // system info

            writeBytes(REDY);

            while (!(stringBuffer = bfr.readLine()).contains(NONE)) {

                if (stringBuffer.contains(JOBN)) {
                    // STORE JOB DATA IN JOB OBJECT
                    fieldBuffer = stringBuffer
                            .split(" "); /*
                                          * split String into array of strings (each string being a field of JOBN)
                                          */

                    Job job = new Job(fieldBuffer); // create new Job object with data from fieldBuffer

                    // get list of capable servers
                    writeBytes(GETS + " Capable " + job.core + " " + job.memory + " " + job.disk);

                    // DATA _ _ message
                    stringBuffer = bfr.readLine();

                    fieldBuffer = stringBuffer.split(" ");
                    int numCapableServer = Integer.parseInt(fieldBuffer[1]); // fieldBuffer[1] -> no. of capable servers

                    writeBytes(OK); // confirmation for receiving DATA

                    ArrayList<Server> serverList = new ArrayList<>();

                    for (int i = 0; i < numCapableServer; i++) {

                        stringBuffer = bfr.readLine(); // read single server information

                        fieldBuffer = stringBuffer.split(" ");
                        String type = fieldBuffer[0];
                        int id = Integer.parseInt(fieldBuffer[1]);
                        String state = fieldBuffer[2];
                        int curStartTime = Integer.parseInt(fieldBuffer[3]);
                        int core = Integer.parseInt(fieldBuffer[4]);
                        int memory = Integer.parseInt(fieldBuffer[5]);
                        int disk = Integer.parseInt(fieldBuffer[6]);
                        int wjobs = Integer.parseInt(fieldBuffer[7]);
                        int rjobs = Integer.parseInt(fieldBuffer[8].trim());

                        Server s = new Server(type, id, state, curStartTime, core, memory, disk, wjobs, rjobs);
                        serverList.add(s);
                    }
                    writeBytes(OK);
                    stringBuffer = bfr.readLine();

                    // ALGORITHM FOR JOB SCHEDULING
                    // determines the most optimal server for scheduling
                    Server s = mostEfficientServer(serverList, job.core, job.disk, job.memory);

                    serverList.clear();// for efficient memory management

                    /* SCHEDULE JOB */

                    scheduleString = SCHD + " " + job.id + " " + s.type + " " + s.id;
                    writeBytes(scheduleString);

                    stringBuffer = bfr.readLine();

                    // request new job
                    // send REDY for the next job

                    writeBytes(REDY);

                } else if (stringBuffer.contains(JCPL)) {
                    writeBytes(REDY);
                }
            }

            writeBytes(QUIT);

            close();
        } catch (UnknownHostException e) {
            System.out.println("Unknown Host Exception: " + e.getMessage());
        } catch (EOFException e) {
            System.out.println("End of File Exception: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO Exception: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    public static void setup() throws IOException {
        s = new Socket(hostname, serverPort); // socket with host IP of 127.0.0.1 (localhost), server port of 50000

        din = new InputStreamReader(s.getInputStream());
        bfr = new BufferedReader(din);
        dout = new DataOutputStream(s.getOutputStream());
    }

    public static void writeBytes(String message) throws IOException {
        dout.write((message + "\n").getBytes());
        dout.flush();
    }

    public static void close() throws IOException {
        bfr.close();
        din.close();
        dout.close();
        s.close();
    }

    /******** ALGORITHM *******/
    /* Schedules the job using optimised Best-Fit */
    /*
     * Priority is give as follows: Idle <- Active <- Booting/inactive <- first
     * servers from GETS Capable
     */

    public static Server mostEfficientServer(List<Server> s, int core, int memory, int disk) {
        List<Server> idleList = new ArrayList<Server>();
        List<Server> activeList = new ArrayList<Server>();
        List<Server> tempList = new ArrayList<Server>();
        for (Server server : s) {
            if (server.core >= core && server.memory >= memory && server.disk >= disk) { // check for available
                if (server.state.equals("idle")) { // resources
                    idleList.add(server);
                } else if (server.state.equals("active")) {
                    activeList.add(server);
                } else {
                    tempList.add(server);
                }
            }
        }
        if (idleList.size() > 0) { // give priority to idle server for scheduling
            int bf = Integer.MAX_VALUE; // if more than 0ne idle server, choose using Best-Fit
            Server bestServer = idleList.get(0);
            for (Server server : idleList) {
                if (bf > server.core - core) {
                    bf = server.core - core;
                    bestServer = server;
                }
            }
            return bestServer;
        }
        if (activeList.size() > 0) { // if no idle server found, check active servers
            int bf = Integer.MAX_VALUE; // select using best-Fit and on basis of no.of waiting jobs
            Server bestServer = activeList.get(0);
            for (Server server : activeList) {
                if (bf > server.core - core && server.wjobs < bestServer.wjobs) {
                    bf = server.core - core;
                    bestServer = server;
                }
            }
            return bestServer;
        }
        if (tempList.size() > 0) { // if no idle/active servers, we find optimal server in tempList
            int bf = Integer.MAX_VALUE; // use Best-fit and least waiting jobs for selecting the server
            Server bestServer = tempList.get(0);
            for (Server server : tempList) {
                if (bf > server.core - core && server.wjobs < bestServer.wjobs) {
                    bf = server.core - core;
                    bestServer = server;
                }
            }
            return bestServer;
        }

        return s.get(0); // if no server found, select first server we get from GETS Capable
    }
}
