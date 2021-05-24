import java.net.*;
import java.util.*;
import java.io.*;


public class Client {
   
    private static Socket s;
 
    // s args
    private static final String hostname = "localhost";
    private static final int serverPort = 50000;
 
    // streams
    private static BufferedReader bfr;
    private static InputStreamReader din;
    private static DataOutputStream dout;
 
    // commands
    private static final String HELO = "HELO\n";
    private static final String OK = "OK\n";
    private static final String AUTH = "AUTH";
    private static final String REDY = "REDY\n";
    private static final String JOBN = "JOBN";
    private static final String JCPL = "JCPL";
    private static final String SCHD = "SCHD";
    private static final String NONE = "NONE";
    private static final String QUIT = "QUIT\n";

    // buffer fields
   
    private static byte[] byteBuffer; // will hold the current message from the server stored as bytes
 
    private static String stringBuffer; /* will hold the current message from the server stored in a string
                                                                       (created from charArray)        */
    private static String[] fieldBuffer; /* will hold the current message from the server as an array of strings
                                                                       (created from stringBuffer)     */  
    private static String scheduleString; // string to be scheduled

    // create server/list objects
    private static List<Server> serverList;
    
 
 
    public static void main(String[] args) throws IOException {
        setup();
 
        try {
            writeBytes(HELO); // client sends HELO
 
            // server replies with OK
 
            System.out.println("sent AUTH username");
            writeBytes(AUTH + " " + System.getProperty("user.name") + "\n");
 
            // server replies with OK after printing out a welcome message and writing system info
 
            // setLargestServer();
 
            System.out.println("Sending REDY ...");

            writeBytes(REDY);
            
            System.out.println("REDY sent.");
 
            readStringBuffer(); // reset stringBuffer & read job
 
            while (!stringBuffer.contains(NONE)){
                
                if (stringBuffer.contains(JOBN)) {
                    fieldBuffer = stringBuffer.split(" "); /* split String into array of strings
                                                              (each string being a field of JOBN) */
 
                    Job job = new Job(fieldBuffer); // create new Job object with data from fieldBuffer
                    
                    System.out.println(stringBuffer);

                    writeBytes("GETS Capable " + job.core + " " + job.memory + " " + job.disk + "\n");
                    
                    writeBytes(OK);
                    
                    readStringBuffer();
                    System.out.println("DATA received : " + stringBuffer);
 
                    fieldBuffer = stringBuffer.split(" "); // fieldBuffer[1] -> no. of capable servers
                    int numCapableServer = Integer.parseInt(fieldBuffer[1]);
                    System.out.println("dtainfo"+" "+numCapableServer);

                    writeBytes(OK); // send list of capable server (one at a time)
                    
                    
                   for(int i=0;i<numCapableServer;i++){
                    readStringBuffer();
                    fieldBuffer=stringBuffer.split(" "); 
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
                    System.out.println(stringBuffer);
                   }

                   
                    writeBytes(OK);
                   
                   Server s = mostEfficientServer(serverList, job.core,job.disk,job.memory);
                   System.out.println(s.type+" "+s.id);
                   scheduleString = SCHD +" "+job.id+" "+s.type+" "+s.id+"\n";
                   writeBytes(scheduleString);
                   
                   serverList.clear();// delete all items to read for next list from GETS Capable

                   readStringBuffer();
                   
                   writeBytes(REDY);

                   readStringBuffer();
                }
                if (stringBuffer.contains(JCPL)) {
                    writeBytes(REDY); // send REDY for the next job
                }

                //    if (stringBuffer.contains("OK")) {
                //     readStringBuffer(); // reset stringBuffer & read next job
                // } 
                readStringBuffer();
            
        }
                
                
            System.out.println("TERMINATING CONNECTION ...");
            
            writeBytes(QUIT);
 
            System.out.println("CONNECTION TERMINATED.");
 
            close();
    }   
         catch (UnknownHostException e) {
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
        serverList = new ArrayList<>(); // initialise list of servers
 
        s = new Socket(hostname, serverPort); // socket with host IP of 127.0.0.1 (localhost), server port of 50000
 
        din = new InputStreamReader(s.getInputStream());
        bfr = new BufferedReader(din);
        dout = new DataOutputStream(s.getOutputStream());
    }
 
    public static void writeBytes(String command) throws IOException {
        byteBuffer = command .getBytes();
        dout.write(byteBuffer);
        dout.flush();
    }

 
    //Algorithm used to select the serverList -> modification of first fit
    //We check servers available resources and than give preferance to servers that are already active or idle
    //if no capable server is active, we schedule job to first capable servers
    /*******  ALGORITHM *********/
    public static Server mostEfficientServer(List<Server> s, int core, int memory, int disk){
        List<Server> tempList = new ArrayList<Server>();
        for(Server server:s){
            if(server.core>=core && server.memory>=memory && server.disk>=disk ){
                tempList.add(server);
            }
        }
        for(Server server:tempList){
            if(server.state.equals("active") || server.state.equals("idle")){
                return server;
            }
        }
        return tempList.get(0);
    }
       

    public static void readStringBuffer() throws IOException {
        stringBuffer = bfr.readLine();
    }
 
    
    public static void close() throws IOException {
        bfr.close();
        din.close();
        dout.close();
        s.close();
    }




}
