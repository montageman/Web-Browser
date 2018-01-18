import java.io.*;
import java.util.Scanner;
import java.net.*;
import java.util.concurrent.TimeUnit;

public class SocketClient {
	static Socket socket = null;
	static PrintWriter out = null;
	static BufferedReader in = null;
	static String response = "";
	static String line = "";
	static String host = "";
	static DataInputStream dis;
	static int port = 80;
	static String urlport = "";
	static String url = "";

	//This function checks for a 404 error
	public static boolean filefound(String file, String temphost) {
		
		line = "";
		//http header request
		out.println("HEAD " + file + " HTTP/1.1\r\nHost: " + temphost + "\r\n");

		//read the request
		while (!line.startsWith("HTTP")) {
			try {
				line = in.readLine();
			}
			catch (NullPointerException e) {
				System.out.println("Null Pointer Exception");
				break;
			}
			catch (IOException e) {
				System.out.println("Read Error!");
				System.exit(1);
			}
		}
		// search for the 404 error
		if(line.startsWith("HTTP/1.1 404")) {
			return false; 	
		} else {
			return true;
		}
	}

	public void communicate(String file, String temphost) {
		line = "";

		//http get request
		out.println("GET " + file + " HTTP/1.1\r\nHost: " + temphost + "\r\n");

		while (!line.startsWith("</html>")) {
			try {
				line = in.readLine();
				response = response + line + "\n";
			} 
			catch (NullPointerException e) {
				System.out.println("Null Pointer Exception");
				break;
			}
			catch (IOException e) {
				System.out.println("Read Error!");
				System.exit(1);
			}
		}
	}
  	
  	//make socket and streams
	public static void listenSocket(String host, int port) {
		try {
			//make the socket and appropriate streams
			socket = new Socket(host, port);
			
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			dis = new DataInputStream(socket.getInputStream());
		}
		catch (UnknownHostException e) {
			System.out.println("Unknown host");
			System.exit(1);
		}
		catch (IOException e) {
			System.out.println("No I/O");
			System.exit(1);
		}
	}

	///download a specified image
	public static void downloadimage(String file) throws IOException {
		
		//work with the given url to split it into the host and path
		int from = file.indexOf("//") + 2;
		int to = file.indexOf("/", from);
		String temphost = file.substring(from, to);
		String path = file.substring(to);

		//make new socket and streams
		listenSocket(temphost,port);

		int buffersize = 2048;
		byte[] b = new byte[buffersize];
		int length=0;
		String[] tokens = file.split("\\/(?=[^\\/]+$)");
		
		//get request
		out.println("GET " + file + " HTTP/1.1\r\nHost: " + temphost + "\r\n");
		
		//sleep for one second
		try{
			TimeUnit.SECONDS.sleep(1);
		}
		catch (Exception e){
			System.out.println("failed to wait");
		}
		OutputStream os = new FileOutputStream(tokens[1]);
		System.out.println("Image: " + tokens[1] + "\n");
		
		boolean tempboolean = true;	
		
		//read the first interval of bytes
		length = dis.read(b, 0, buffersize);
		int i=0;

		//search for the string "\r\n\r\n" and move the index to the postion after the string
		while (true)
		{	
			if (b[i]==13)
			{
				if (b[i+1] ==10)
				{
					if (b[i+2] ==13)
					{
						if (b[i+3] ==10)
						{
							break;
		
						}
					}
				}
			}
			i++;
		}
		i = i+4;

		//write the rest of the buffer after the found index to the file.
		os.write(b, i, buffersize-i);
		os.flush();
		
		//write the rest of the file to the buffer until done.
		while (tempboolean) {
			length = dis.read(b);

			os.write(b, 0, length);
			os.flush();
			tempboolean = (length >= buffersize);
		}
		os.close();
	}

	//this parses the html response.
	public static void parsepage(String sourcecode) {
		int indexfrom = 0;
		int indexto = 0;
		
		indexfrom = sourcecode.indexOf("<title>", indexfrom + 1);
		indexto = sourcecode.indexOf("</title>", indexto + 1);
		if(indexfrom != -1 && indexto != -1) {
			System.out.println(sourcecode.substring(indexfrom + 7, indexto) + "\n\r");
		}

		indexfrom = 0;
		indexto = 0;
		while(true) {
			indexfrom = sourcecode.indexOf("<", indexfrom + 1);
			if(indexfrom != -1) {
				if(sourcecode.substring(indexfrom, indexfrom+6).contains("<p>")) {
					indexto = sourcecode.indexOf("</p>", indexfrom + 2);
					if(indexto != -1) {
						System.out.println(sourcecode.substring(indexfrom + 3, indexto) + "\n\r");
					}
				}
				if(sourcecode.substring(indexfrom, indexfrom+6).contains("<img")) {

					indexto = sourcecode.indexOf("src=\"", indexfrom + 2);
					if(indexto != -1) {
						String imagepath = sourcecode.substring(indexto+5, sourcecode.indexOf("\"", indexto+5));
						if(!imagepath.contains(host)) {
							if(imagepath.contains(":")) {
								
							} else {
								String[] tokens = url.split("\\/(?=[^\\/]+$)");
								host = tokens[0];
								if(host.startsWith("http://")) {
									host = host.substring(host.indexOf("//") + 2);
								}
								if(!host.startsWith("www.")) {
									host = "www." + host;
								}
								if(imagepath.startsWith("/")) {
									imagepath = "http://" + host + imagepath;
								} else {
									if(host.endsWith("/")) {
										imagepath = "http://" + host + imagepath;
									} else {
										imagepath = "http://" + host + "/" + imagepath;
									}
								}
							}
						}
						try {
							downloadimage(imagepath);
						}
						catch(IOException e) {
							
						}
					}
				}

			} else {
				break;
			}

		}
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Use a URL as an argument.");
			System.exit(1);
		}
		SocketClient client = new SocketClient();

		url = args[0];
		int from = url.indexOf("//") + 2;
		int to = url.indexOf("/", from);
		host = url.substring(from, to);
		String path = url.substring(to);
		urlport = "";
		port = 80;
		if(host.contains(":")) {
			String[] parts = host.split(":");
			host = parts[0];
			urlport = parts[1];
			port = Integer.parseInt(urlport);
		} else {
			urlport = "80";
		}

		client.listenSocket(host, port);
		if(filefound(path, host)) {
			client.communicate(path, host);
		} else {
			System.out.println("Error 404 - Page not found");
		}
      		parsepage(response);
		response = "";
	}
}
