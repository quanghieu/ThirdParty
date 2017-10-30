import java.awt.font.ShapeGraphicAttribute;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.server.SocketSecurityException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javax.swing.plaf.synth.SynthSpinnerUI;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import com.github.opendevl.JFlat;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ThirdParty {

	private static final String thirdParty_location = "resource/";

	private static final String key_location = thirdParty_location + "key/";

	private static final String client_host = "localhost";

	private static final String key_loc = "/var/www/html/piwigo/upload/buffer/user_key.key";

	private static final String key_hom = "/resource/key.txt";

	private static final String temp_file = "/home/hieu/temp_import/";

	private static final String thirdParty_metadata = "resource/Metadata/";

	private static final String thirdParty_object = "resource/Object/";

	private static final String webserver = "http://localhost/piwigo/sieve.php";

	private static String cachedGUID;

	static ServerSocket ss;
	static ServerSocket backupSS;
	static ServerSocket refrSock;
	static ServerSocket updateReq;

	static String currentUser;

	private static final String url = "jdbc:mysql://localhost";

	private static final String user = "root";

	private static final String password = "123";

	static Connection con;

	private static void connectDb() {
		try {
			con = DriverManager.getConnection(url, user, password);
			System.out.println("Success");
			String query1 = "use Sieve_import_daemon;";
			// String query2 = "create table piwigo_encrypt("
			// + "ID int,"
			// + "Filename varchar(255),"
			// + "Storage varchar(255),"
			// + "Metadata varchar(255),"
			// + "Epoch int )";
			Statement stmt = con.createStatement();
			stmt.executeQuery(query1);
		} catch (Exception e) {
			System.out.println("Fail");
			e.printStackTrace();
		}
	}

	public ThirdParty(String host, int port, String file) {
		try {
			String fileName = "photo.jpg";
			Socket s = new Socket(host, port);
			// Upload request
			sendRequest(s, 3);
			requestFilename(s, fileName);
			receiveMetadata(s);
			// int guid = decryptMetadata(); // call decrypt
			// receiveData(s, guid, fileName); // fetch guid and retrieve data
			// int guid = uploadClient(file);
			// sendFile(file);
			// sendMetadata(guid);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// private static void retrieveFile(Socket s, String fileName) throws
	// IOException, InterruptedException {
	// sendRequest(s, 3);
	// // requestFilename(s, fileName);
	// DataInputStream dis = new DataInputStream(s.getInputStream());
	// while (dis.readUTF().equals("Prepare to receive")) {
	// String file = dis.readUTF();
	// System.out.println("Retrieving file "+file);
	// receiveMetadata(s);
	// int guid = decryptMetadata(); // call decrypt
	// receiveData(s, guid, file);
	// decryptFile(file);
	// System.out.println(System.currentTimeMillis());
	// }
	// }

	private static void retrieveMetadata(Socket s) throws IOException {
		File folder = new File(thirdParty_metadata);
		for (File f : folder.listFiles()) {
			f.delete();
		}
		sendRequest(s, 4);
		// send attributes
		sendAttrs(s);
		DataInputStream dis = new DataInputStream(s.getInputStream());
		while (dis.readUTF().equals("receive"))
			receiveMetadata(s);
		s.close();
	}

	private static void sendAttrs(Socket s) throws IOException {
		// TODO Auto-generated method stub
		DataOutputStream dos = new DataOutputStream(s.getOutputStream());

		FileReader fr = null;
		BufferedReader br = null;

		try {
			fr = new FileReader(thirdParty_location + "logFile.txt");
			br = new BufferedReader(fr);
			String attrs = br.readLine();
			dos.writeUTF(attrs);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private static void retrieveFile(Socket s) throws IOException, InterruptedException {

		// requestFilename(s, fileName);

		System.out.println("enter retrieve file");
		File folder = new File(thirdParty_metadata);
		for (File f : folder.listFiles()) {
			int guid = decryptMetadata(f.getPath()); // call decrypt
			if (guid == -1)
				continue;
			System.out.println("File " + f.getPath() + " is processing");
			String fileName = null;
			s = new Socket("localhost", 1988);
			sendRequest(s, 3);
			DataInputStream dis = new DataInputStream(s.getInputStream());
			DataOutputStream os = new DataOutputStream(s.getOutputStream());
			fileName = receiveData(s, guid);
			if (fileName == null)
				continue;
			String tempFile = decryptFile(fileName);
			System.out.println(System.currentTimeMillis());
			s.close();
		}
	}

	private static String decryptFile(String fileName) {
		// TODO Auto-generated method stub
		System.out.println("Enter decrypt file");
		fileName = thirdParty_object.concat(fileName);
		String resultFile = fileName.substring(0, fileName.length() - 4);
		System.out.println("Result File" + resultFile);
		// MyJniFunc.Decrypt(fileName, key_hom, resultFile);
		String resFile = homo_decrypt(fileName, key_hom, resultFile);
		homo_decrypt(fileName, key_hom, resultFile);
		return resFile;
	}

	private static String homo_decrypt(String fileName, String keyHom, String resultFile) {
		// TODO Auto-generated method stub
		Process p;
		try {
			// System.out.println(execCmd("./homo_decrypt.sh "
			// +thirdParty_location+" "+fileName+" "+keyHom+ " "+resultFile));
			p = Runtime.getRuntime().exec(
					"./homo_decrypt.sh " + thirdParty_location + " " + fileName + " " + keyHom + " " + resultFile);
			System.out.println(
					"./homo_decrypt.sh " + thirdParty_location + " " + fileName + " " + keyHom + " " + resultFile);
			p.waitFor();
			// System.out.println(execCmd("./homo_decrypt.sh " +
			// thirdParty_location + " " + fileName + " " + keyHom + " " +
			// resultFile));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resultFile;
	}

	private static String receiveData(Socket s, int guid) throws IOException {
		// TODO Auto-generated method stub
		// Send GUID
		DataInputStream dis = new DataInputStream(s.getInputStream());
		System.out.println("Receiving data");
		DataOutputStream dos = new DataOutputStream(s.getOutputStream());
		InputStream is = s.getInputStream();
		// os.write(1);

		System.out.println("GUID is " + guid);
		dos.writeInt(guid);

		if (dis.readUTF().equals("No file")) {
			System.out.println("No file with the specified GUID");
			return null;
		}

		String fileName = dis.readUTF();

		byte[] buffer = new byte[1024];
		FileOutputStream fos = new FileOutputStream(thirdParty_object + fileName);
		BufferedOutputStream bos = new BufferedOutputStream(fos);

		int n = 0;
		int bytesLeft = dis.readInt();
		System.out.println("File size is " + bytesLeft);
		while (bytesLeft > 0) {
			n = is.read(buffer, 0, Math.min(buffer.length, bytesLeft));
			if (n < 0) {
				throw new EOFException("Expected " + bytesLeft + " more bytes to read");
			}
			bos.write(buffer, 0, n);
			System.out.println("Writing File... ");
			bytesLeft -= n;
			System.out.println(bytesLeft + " bytes left to read");
		}
		bos.close();
		return fileName;
	}

	private static void copyFile(String File, String To) {
		InputStream inStream = null;
		OutputStream outStream = null;
		String[] str = File.split("/");
		String fileName = str[str.length - 1];

		try {

			File afile = new File(File);
			File bfile = new File(To + "/" + fileName);

			inStream = new FileInputStream(afile);
			outStream = new FileOutputStream(bfile);

			byte[] buffer = new byte[1024];

			int length;
			// copy the file content in bytes
			while ((length = inStream.read(buffer)) > 0) {

				outStream.write(buffer, 0, length);

			}

			inStream.close();
			outStream.close();
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public static String execCmd(String cmd) throws java.io.IOException {
		java.util.Scanner s = new java.util.Scanner(Runtime.getRuntime().exec(cmd).getInputStream())
				.useDelimiter("\\A");
		return s.hasNext() ? s.next() : "";
	}

	private static int decryptMetadata(String metadata) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		// Get corresponding key
		String keyValue = "";
		String guidValue = "";
		int guid = 0;

		// purify
		// File id = new File(thirdParty_location + "GUID.txt");
		// if (id.exists())
		// id.delete();
		//
		// File key = new File(thirdParty_location + "key.txt");
		// if (key.exists())
		// key.delete();

		System.out.println("Decrypt metadata");
		String keyFile = thirdParty_location + "user_id/" + currentUser + "/key.txt";// "usr_privCP_Java.key";
		System.out.println("./decrypt_metadata.sh " + keyFile + " " + metadata);
		Process p = Runtime.getRuntime().exec("./decrypt_metadata.sh " + keyFile + " " + metadata);
		p.waitFor();
		String result = execCmd("./decrypt_metadata.sh " + keyFile + " " + metadata);
//		System.out.println(result);
		String[] lines = result.split(System.getProperty("line.separator"));
		for (String s : lines) {
			if (s.contains("Plaintext")) {
				keyValue = s.substring(11, 523);
				guidValue = s.substring(523, s.length());
				break;
			}
		}

		System.out.println("Key is " + keyValue + " and guid is " + guidValue);
		// System.out.println(execCmd("./decrypt_metadata.sh " + keyFile));
		if (guidValue.equals("")) {
			System.out.println("Fail to decrypt metadata");
			return -1;
		}

		try (PrintWriter out = new PrintWriter(thirdParty_location + "key.txt")) {
			out.println(keyValue);
		}

		try (PrintWriter out = new PrintWriter(thirdParty_location + "GUID.txt")) {
			out.println(guidValue);
		}

		FileInputStream fis = new FileInputStream(thirdParty_location + "GUID.txt");

		BufferedReader bis = new BufferedReader(new InputStreamReader(fis));
		String str_guid = bis.readLine();
		guid = Integer.parseInt(str_guid, 16);
		//
		return guid;
	}

	private static void receiveMetadata(Socket s) throws IOException {
		// TODO Auto-generated method stub
		byte[] buffer = new byte[65536];
		System.out.println("Receiving metadata");
		InputStream is = s.getInputStream();
		String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
		FileOutputStream fos = new FileOutputStream(thirdParty_metadata + timestamp + ".cpabe");
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		DataInputStream dis = new DataInputStream(s.getInputStream());
		int fileSize = dis.readInt();
		int byteRead = is.read(buffer, 0, fileSize);
		int current = byteRead;
		System.out.println("Read " + current + " bytes of metadata");
		/*
		 * do { byteRead = is.read(buffer, 0, buffer.length - current);
		 * if(byteRead >= 0) current+=byteRead; } while(byteRead > -1);
		 */

		bos.write(buffer, 0, current);
		bos.flush();
	}

	private static void requestFilename(Socket s, String fileName) throws IOException {
		// TODO Auto-generated method stub
		PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
		pw.println(fileName);
	}

	private static void sendRequest(Socket s, int i) throws IOException {
		// TODO Auto-generated method stub
		DataOutputStream dos = new DataOutputStream(s.getOutputStream());
		dos.write(i);
	}

	private static void storeKey(String user_id, String attrs, String key) {
		String path = thirdParty_location + "user_id/" + user_id + "/";
		File directory = new File(path);
		if (!directory.exists())
			directory.mkdir();
		File policy = new File(path + "policies.txt");
		File keyFile = new File(path + "key.txt");

		try {
			PrintWriter out = new PrintWriter(policy);
			PrintWriter keyOut = new PrintWriter(keyFile);
			out.println(attrs);
			keyOut.println(key);
			out.close();
			keyOut.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static class keyReceiveHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			String response = "This is the response";
			InputStream is = t.getRequestBody();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			String line;
			String result = null;
			while ((line = br.readLine()) != null) {
				result = java.net.URLDecoder.decode(line, "UTF-8");
				System.out.println(result);
			}

			String[] Info = result.split("&");
			String user_id = Info[0].substring(8, Info[0].length());
			String attrs = Info[1].substring(10, Info[1].length());
			String key = Info[2].substring(4, Info[2].length());
			storeKey(user_id, attrs, key);
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();

		}
	}

	private static void test(String user_id) {
		String url = "http://localhost:5000/fileRequest";
		HttpClient httpclient = new DefaultHttpClient();
		HttpPost httppost = new HttpPost(url);

		try {
			List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>();
			nameValuePairs.add(new BasicNameValuePair("user_id", "" + user_id));
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

			HttpResponse response2 = httpclient.execute(httppost);
			System.out.println(response2);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private static void handleReq(String user_id) throws IOException {

		currentUser = user_id;

		// This is first hard fixed then will be change later
		if (user_id != null) {
			File policies = new File(thirdParty_location + "user_id/" + currentUser + "/policies.txt");
			byte[] encoded = Files.readAllBytes(policies.toPath());
			String policy = new String(encoded, StandardCharsets.UTF_8);
			policy = policy.replaceAll("-", "");
			try {
				sendSPReq(policy, "metaRequest");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// String response = "This is the response";
		// byte[] buf = new byte[8192];
		// try {
		// InputStream is = new
		// FileInputStream("/home/hieu/workspace/ThirdParty/resource/Object/random.json");
		// } catch (FileNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		// DefaultHttpClient client = new DefaultHttpClient();
		// HttpPost post = new
		// HttpPost("http:/"+remoteAddr+":5000/"+request);

		// MultipartEntity entity = new MultipartEntity();
		// entity.addPart("size="+new File(metadata).length(), new
		// FileBody(new File(metadata)));
		// post.setEntity(entity);

	}

	static class fileRequestHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {

			InputStream is1 = t.getRequestBody();
			BufferedReader br = new BufferedReader(new InputStreamReader(is1, "UTF-8"));
			String line = "";
			String result = null;
			while ((line = br.readLine()) != null) {
				result = java.net.URLDecoder.decode(line, "UTF-8");
				System.out.println(result);
			}

			String user_id = null;
			if (result.contains("user_id")) {
				user_id = result.substring(8, result.length());
				handleReq(user_id);
				// currentUser = user_id;
			}

			// // This is first hard fixed then will be change later
			// if (user_id != null) {
			// File policies = new
			// File(thirdParty_location+""+currentUser+"/policies.txt");
			// byte[] encoded = Files.readAllBytes(policies.toPath());
			// String policy = new String(encoded, StandardCharsets.UTF_8);
			// try {
			// sendSPReq(policy, "metaRequest");
			// } catch (Exception e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }
			// }
			// // String response = "This is the response";
			// byte[] buf = new byte[8192];
			// InputStream is = new
			// FileInputStream("/home/hieu/workspace/ThirdParty/resource/Object/random.json");
			//
			// t.sendResponseHeaders(200, 10000);
			//
			// // DefaultHttpClient client = new DefaultHttpClient();
			// // HttpPost post = new
			// // HttpPost("http:/"+remoteAddr+":5000/"+request);
			//
			// // MultipartEntity entity = new MultipartEntity();
			// // entity.addPart("size="+new File(metadata).length(), new
			// // FileBody(new File(metadata)));
			// // post.setEntity(entity);
			//
			// OutputStream os = t.getResponseBody();
			// int c = 0;
			//
			// while ((c = is.read(buf, 0, buf.length)) > 0) {
			// os.write(buf, 0, c);
			// os.flush();
			// }
			// // os.write(response.getBytes());
			// os.close();
		}

	}

	public static void sendSPReq(String policies, String request) throws Exception {
		// This address need to be fixed later

		String url = "http://localhost:6000/" + request;
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		String USER_AGENT = "Mozilla/5.0";

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		String data = URLEncoder.encode(policies, "UTF-8");
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(data);

		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
		System.out.println("Inside SPReq");
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + data);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// print result
		System.out.println("Prepare to get response in SPReq");
		System.out.println(response.toString());

	}

	private static String saveUploadFileMeta(String location, HttpExchange t) throws FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		InputStream is = t.getRequestBody();
		System.out.println(t.getRequestHeaders().toString());
		String fileName = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + ".cpabe";
		System.out.println("File name is " + fileName);

		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		for (int i = 0; i < 5; i++)
			br.readLine();

		String metaString = br.readLine();
		try (PrintWriter out = new PrintWriter(location + "/" + fileName)) {
			out.println(metaString);
		}

		// FileOutputStream fos = new FileOutputStream(location+"/"+fileName);
		// BufferedOutputStream bos = new BufferedOutputStream(fos);
		// byte[] buffer = new byte[2048];
		// int read = 0;
		// while((read = is.read(buffer)) != -1){
		// bos.write(buffer, 0, read);
		// }
		// bos.close();
		return location + "/" + fileName;
	}

	private static String saveUploadFileObj(String location, HttpExchange t) throws FileNotFoundException, IOException {
		// TODO Auto-generated method stub
		InputStream is = t.getRequestBody();
		System.out.println(t.getRequestHeaders().toString());
		String fileName = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date()) + ".json_enc";
		System.out.println("File name is " + fileName);

		// BufferedReader br = new BufferedReader(new InputStreamReader(is,
		// "UTF-8"));
		// for(int i = 0; i < 5; i++)
		// br.readLine();

		// String metaString = br.readLine();
		// byte[] b = metaString.getBytes();
		// for(int i = 0; i < b.length; i++)
		// System.out.print(Integer.toHexString(b[i])+" ");
		// try(PrintWriter out = new PrintWriter(location+"/"+fileName)){
		// out.println(metaString);
		// }
		// return fileName;
		FileOutputStream fos = new FileOutputStream(location + "/" + fileName);
		BufferedOutputStream bos = new BufferedOutputStream(fos);
		byte[] buffer = new byte[2048];
		int read = 0;
		while ((read = is.read(buffer)) != -1) {
			bos.write(buffer, 0, read);
		}
		bos.close();

		fileTrimming(location + "/" + fileName);

		String resultFile = decryptFile(fileName);
		System.out.println("Result File "+resultFile);
		// File resFile = new File(location + "/" + resultFile);
		// // create CSV file on each object received
		// String str = new String(Files.readAllBytes(resFile.toPath()));
		//
		// JFlat flatMe = new JFlat(str);
		//
		// // get the 2D representation of JSON document
		// List<Object[]> json2csv = flatMe.json2Sheet().getJsonAsSheet();
		//
		// // write the 2D representation in csv format
		// flatMe.write2csv(location + "/res.csv");
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		s = s + "/" + resultFile;
		System.out.println("Prepare to upload file "+s);
		
		HttpClientExample http = new HttpClientExample();
		http.uploadFileJson(s, "");
		return null;
	}

	private static void fileTrimming(String file) throws IOException {
		// TODO Auto-generated method stub
		File f = new File(file);
		// FileUtils.copyFile(new File(file), new File(file+".temp"));
		FileReader fr = new FileReader(new File(file));
		BufferedReader br = new BufferedReader(fr);
		br.readLine();
		String info = br.readLine();
		br.close();
		int position = info.indexOf("size=") + 5;
		int firstPos = position;
		while (info.charAt(position) != '"') {
			position++;
		}
		int lengthFile = Integer.parseInt(info.substring(firstPos, position));
		System.out.println("File length is " + lengthFile);

		byte[] bFile = Files.readAllBytes(new File(file).toPath());
//		for (int i = 0; i < bFile.length; i++) {
//			System.out.println(i + ":" + Integer.toHexString(bFile[i]) + " ");
//		}
		byte[] destFile = new byte[lengthFile];

		int fileStart = 0;

		for (int i = 0; i < bFile.length; i++) {
			if (bFile[i] == 2 && bFile[i + 1] == 0 && bFile[i + 2] == 0 && bFile[i + 3] == 0 && bFile[i + 4] == 0) {
				fileStart = i;
			}
		}

		System.out.println("File start is " + fileStart);

		for (int i = 0; i < lengthFile; i++) {
			destFile[i] = bFile[i + fileStart];
		}

		f.delete();
		FileOutputStream fos = new FileOutputStream(f);
		fos.write(destFile);
		fos.close();
	}

	private static void retrieveFileHttp(String file) throws Exception {

		// requestFilename(s, fileName);

		System.out.println("enter retrieve file");
		File fileMeta = new File(file);
		
			int guid = decryptMetadata(fileMeta.getPath()); // call decrypt
			if (guid == -1)
				return;
			System.out.println("Line 750: File " + fileMeta.getPath() + " is processing");
			// s = new Socket("localhost", 1988);
			// sendRequest(s, 3);
			// fileName = receiveData(s, guid);
			sendSPReq("" + guid, "objRequest");
		
	}

	static class metadataReceiveHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			// clear metadata
			// FileUtils.cleanDirectory(new File(thirdParty_metadata));
			System.out.println("Prepare to save metadata");
			String file = saveUploadFileMeta(thirdParty_metadata, t);
			String response = "This is the response";
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();

			Thread retr = new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					try {
						retrieveFileHttp(file);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			retr.start();
		}
	}

	static class objReceiveHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange t) throws IOException {
			System.out.println("Prepare to save encrypted object");
			saveUploadFileObj(thirdParty_object, t);
			String response = "This is the response";
			t.sendResponseHeaders(200, response.length());
			OutputStream os = t.getResponseBody();
			os.write(response.getBytes());
			os.close();

			// decrypt
//			decryptFile(fileName);
		}
	}

	public static void main(String[] args) throws IOException {
		System.out.println("Sieve import daemon is running");
		// connectDb();
		// System.out.println("Credentials ::" + credential);

		HttpServer server = HttpServer.create(new InetSocketAddress(5000), 0);
		server.createContext("/keyReceive", new keyReceiveHandler());
		server.createContext("/fileRequest", new fileRequestHandler());
		server.createContext("/metadataReceive", new metadataReceiveHandler());
		server.createContext("/objReceive", new objReceiveHandler());
		server.start();

//		handleReq("1");
		// String str = new String(Files.readAllBytes(new
		// File("/home/hieu/workspace/ThirdParty/resource/Object/random.json").toPath()));
		// BufferedReader br = null;
		// FileReader fr = null;
		// String str = "[";
		// try {
		//
		// //br = new BufferedReader(new FileReader(FILENAME));
		// fr = new
		// FileReader("/home/hieu/workspace/ThirdParty/resource/Object/random.json");
		// br = new BufferedReader(fr);
		//
		// String sCurrentLine;
		//
		// while ((sCurrentLine = br.readLine()) != null) {
		// System.out.println(sCurrentLine);
		// if(sCurrentLine.contains("{"))
		// str = str +""+sCurrentLine+",";
		// //get the 2D representation of JSON document
		// }
		//
		// } catch (IOException e) {
		//
		// e.printStackTrace();
		//
		// }
		// str = str.substring(0,str.length() - 1);
		// str = str + "]";
		//
		//// String str = new String(Files.readAllBytes(new
		// File("/home/hieu/workspace/ThirdParty/resource/Object/random.json").toPath()));
		// JFlat flatMe = new JFlat(str);
		// List<Object[]> json2csv = flatMe.json2Sheet().getJsonAsSheet();
		// flatMe.write2csv("/home/hieu/workspace/ThirdParty/resource/Object/random.csv");

		// write the 2D representation in csv format

		System.out.println("Finish write");

		// try {
		// ss = new ServerSocket(1995);
		// backupSS = new ServerSocket(1999);
		// updateReq = new ServerSocket(1996);
		// refrSock = new ServerSocket(2017);
		//
		// } catch (IOException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }
		// Thread t = new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// // TODO Auto-generated method stub
		//
		// String GUID = args[0];
		// System.out.println(System.currentTimeMillis());
		// int fileGUID = Integer.parseInt(GUID);
		// try {
		// Socket s = new Socket("localhost", 1988);
		// String res = retrieveFile(s, fileGUID);
		//// System.out.println(System.currentTimeMillis());
		// System.out.println(res);
		// } catch (IOException | InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// });
		// t.start();

		Thread key_th = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (true) {

					try {

						Socket s = ss.accept();
						DataInputStream dis = new DataInputStream(s.getInputStream());
						InputStream is = s.getInputStream();
						FileOutputStream fos = new FileOutputStream(thirdParty_location + "usr_privCP_Java.key");
						System.out.println("File name " + thirdParty_location + "usr_privCP_Java.key");
						long fileSize = dis.readLong();
						BufferedOutputStream bos = new BufferedOutputStream(fos);
						DataOutputStream dos = new DataOutputStream(s.getOutputStream());

						int bytesLeft = (int) fileSize;
						System.out.println("File size is " + bytesLeft);
						byte[] buffer = new byte[1024];
						int n = 0;
						while (bytesLeft > 0) {
							n = is.read(buffer, 0, Math.min(buffer.length, bytesLeft));
							if (n < 0) {
								throw new EOFException("Expected " + bytesLeft + " more bytes to read");
							}
							bos.write(buffer, 0, n);
							System.out.println("Writing File... ");
							bytesLeft -= n;
							System.out.println(bytesLeft + " bytes left to read");
						}
						bos.close();
						// UpdateDB(new File(thirdParty_location +
						// "logFile.txt"));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} /*
						 * catch (SQLException e) { // TODO Auto-generated catch
						 * block e.printStackTrace(); }
						 */

				}
			}
		});
		key_th.start();

		Thread key_th2 = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (true) {

					try {
						Socket s = backupSS.accept();
						ReceiveLog(s);
						Socket sMeta = new Socket("localhost", 1988);
						retrieveMetadata(sMeta);
						Socket s1 = null;
						retrieveFile(s1);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		key_th2.start();

		Thread key_th3 = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (true) {
					try {
						Socket s = updateReq.accept();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});

		key_th3.start();

		Thread refreshData = new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (true) {
					try {
						Socket s = refrSock.accept();
						DataInputStream dis = new DataInputStream(s.getInputStream());
						String mess = dis.readUTF();
						if (mess.equals("Update"))
							updateData(s);
						else if (mess.equals("Revoke"))
							revokeData(s);
						s.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}

			private void revokeData(Socket s) {
				// TODO Auto-generated method stub
				HttpClientExample http = new HttpClientExample();
				try {
					// http.sendPost("");
					updateData(s);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
		refreshData.start();
	}

	private static void updateData(Socket s) {
		File log = new File(thirdParty_location + "logFile.txt");
		if (!log.exists()) {
			System.out.println("No policies defined!!!");
			return;
		}
		File key = new File(thirdParty_location + "usr_privCP_Java.key");
		if (!key.exists()) {
			System.out.println("No key");
			return;
		}

		// purify old metadata
		File folder = new File(thirdParty_metadata);
		for (File f : folder.listFiles()) {
			f.delete();
		}

		File ObjFolder = new File(thirdParty_object);
		for (File f : ObjFolder.listFiles()) {
			f.delete();
			System.out.println("File " + f.getName() + " is deleted!!");
		}

		Socket sMeta;
		try {
			sMeta = new Socket("localhost", 1988);
			retrieveMetadata(sMeta);
			Socket s1 = null;
			retrieveFile(s1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Make request to third party update repository
		/*
		 * HttpClientExample http = new HttpClientExample();
		 * System.out.println("\nTesting 2 - Send Http POST request"); try {
		 * updateFile(); } catch (Exception e) { // TODO Auto-generated catch
		 * block e.printStackTrace(); }
		 */
		HttpClientExample http = new HttpClientExample();
		for (File f : ObjFolder.listFiles()) {
			String ext = FilenameUtils.getExtension(f.getName());
			if (ext.contains("_enc"))
				continue;
			try {
				http.uploadFileJson(f.getAbsolutePath(), "");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private static void updateFile() {
		// TODO Auto-generated method stub
		File folder = new File(thirdParty_object);
		for (File f : folder.listFiles()) {
			String ext = FilenameUtils.getExtension(f.getName());
			if (ext.contains("_enc"))
				f.delete();
			else {
				HttpClientExample http = new HttpClientExample();
				try {
					http.uploadFile(f.getAbsolutePath(), webserver);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	// This function should be changed
	private static void UpdateDB(File file) throws SQLException, IOException {
		// TODO Auto-generated method stub
		String sql = "use piwigo";
		Statement stmt = con.createStatement();
		stmt.executeQuery(sql);

		File log = new File(thirdParty_location + "logFile.txt");
		BufferedReader br = new BufferedReader(new FileReader(log));
		String readLine = "";
		while ((readLine = br.readLine()) != null) {
			String[] str = readLine.split(":");
			String name = str[0];
			String path = str[1];
			String sqlIns = "insert into piwigo_images (file, path) values ('" + name + "', '" + path + "')";
			stmt.executeUpdate(sqlIns);
		}
	}

	private static void ReceiveLog(Socket s) throws IOException {
		// TODO Auto-generated method stub
		DataInputStream dis = new DataInputStream(s.getInputStream());
		InputStream is = s.getInputStream();
		File logFile = new File(thirdParty_location + "logFile.txt");
		if (logFile.exists())
			logFile.delete();
		FileOutputStream fos = new FileOutputStream(thirdParty_location + "logFile.txt");
		System.out.println("File name " + thirdParty_location + "logFile.txt");
		long fileSize = dis.readLong();
		BufferedOutputStream bos = new BufferedOutputStream(fos);

		int bytesLeft = (int) fileSize;
		System.out.println("File size is " + bytesLeft);
		byte[] buffer = new byte[1024];
		int n = 0;
		while (bytesLeft > 0) {
			n = is.read(buffer, 0, Math.min(buffer.length, bytesLeft));
			if (n < 0) {
				throw new EOFException("Expected " + bytesLeft + " more bytes to read");
			}
			bos.write(buffer, 0, n);
			System.out.println("Writing File... ");
			bytesLeft -= n;
			System.out.println(bytesLeft + " bytes left to read");
		}
		bos.close();
	}

	private static void saveKey() {
		// TODO Auto-generated method stub
	}

	private static void keyRequest() {
		// TODO Auto-generated method stub

	}

}