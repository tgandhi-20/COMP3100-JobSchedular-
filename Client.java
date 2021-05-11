import java.net.*;
import java.util.*;
import java.io.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class Client {
    private static Socket s;

    // s args
    private static final String hostname = "localhost";
    private static final int serverPort = 50000;

    // streams
    private static InputStreamReader din;
    private static DataOutputStream dout;

    // commands
    private static final String HELO = "HELO";
    private static final String OK = "OK";
    private static final String AUTH = "AUTH";
    private static final String REDY = "REDY";
    private static final String JOBN = "JOBN";
    private static final String JCPL = "JCPL";
    private static final String SCHD = "SCHD";
    private static final String NONE = "NONE";
    private static final String QUIT = "QUIT";
    private static final String GETSC = "GETS Capable";
    
    

    // buffer fields
    private static char[] charBuffer;
    private static byte[] byteBuffer; // will hold the current message from the server stored as bytes

    private static String stringBuffer; /* will hold the current message from the server stored in a string
                                                                       (created from charArray)        */
    private static String[] fieldBuffer; /* will hold the current message from the server as an array of strings
                                                                       (created from stringBuffer)     */

    private static String scheduleString; // string to be scheduled

    private static final int CHAR_BUFFER_LENGTH = 80;

    // create server/list objects
    private static List<Server> serverList;
    private static Server largestServer;

    // create file object
    private static File DSsystemXML;

    public static void main(String[] args) throws IOException  {
         // initialise list of servers

        s = new Socket(hostname, serverPort); // socket with host IP of 127.0.0.1 (localhost), server port of 50000

        din = new InputStreamReader(s.getInputStream());
        dout = new DataOutputStream(s.getOutputStream());
         writeBytes(HELO); // client sends HELO

            // server replies with OK

            System.out.println("sent AUTH username");
            writeBytes(AUTH + " " + System.getProperty("user.name"));

            writeBytes(REDY);

            readStringBuffer(); // reset stringBuffer & read job

            while (!(stringBuffer = String.valueOf(charBuffer)).contains(NONE)) {
                System.out.println(stringBuffer);

                if (stringBuffer.contains(JOBN)) {
                    fieldBuffer = stringBuffer.split(" "); /* split String into array of strings
                                                              (each string being a field of JOBN) */

                    Job job = new Job(fieldBuffer); // create new Job object with data from fieldBuffer
                    writeBytes(GETSC+" "+job.core+" "+job.memory+" "+job.disk);
                }
                readStringBuffer();
                System.out.println(stringBuffer);
                writeBytes(OK);

                
                writeBytes(QUIT);
            }
                
            
    }

    public static void writeBytes(String command) throws IOException {
        byteBuffer = command .getBytes();
        dout.write(byteBuffer);
        dout.flush();
    }

    public static void readStringBuffer() throws IOException {
        charBuffer = new char[CHAR_BUFFER_LENGTH];
        din.read(charBuffer);
    }
}