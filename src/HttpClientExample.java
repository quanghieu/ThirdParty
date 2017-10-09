import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.Header;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HttpClientExample {

	private final String USER_AGENT = "Mozilla/5.0";
	private final String listControl = "/home/hieu/Downloads/piwigo_thirdParty/listControl.txt";

	public static void main(String[] args) throws Exception {

		HttpClientExample http = new HttpClientExample();

		System.out.println("Testing 1 - Send Http GET request");
		http.sendGet();

		System.out.println("\nTesting 2 - Send Http POST request");
		// http.uploadFile("", "");
		// http.sendPost("");

		http.uploadFileJson("", "");
	}

	// HTTP GET request
	private void sendGet() throws Exception {

		String url = "http://localhost/piwigo";

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// optional default is GET
		con.setRequestMethod("GET");

		// add request header
		con.setRequestProperty("User-Agent", USER_AGENT);

		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// print result
		System.out.println(response.toString());

	}

	// HTTP POST request
	public void sendPost(String updateLocation) throws Exception {

		String url = "http://localhost:8000/echoPost";
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		String data = URLEncoder.encode("revoke", "UTF-8") + "=" + URLEncoder.encode(updateLocation, "UTF-8");

		// data += "&" + URLEncoder.encode("email", "UTF-8") + "=" +
		// URLEncoder.encode(Email, "UTF-8");
		//
		// data += "&" + URLEncoder.encode("user", "UTF-8") + "=" +
		// URLEncoder.encode(Login, "UTF-8");
		//
		// data += "&" + URLEncoder.encode("pass", "UTF-8") + "=" +
		// URLEncoder.encode(Pass, "UTF-8");

		// String urlParameters =
		// "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";

		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(data);
		wr.flush();
		wr.close();

		int responseCode = con.getResponseCode();
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
		System.out.println(response.toString());

	}

	public void sendPostFile(String filepath) throws MalformedURLException, IOException {
		HttpURLConnection httpUrlConnection = (HttpURLConnection) new URL("http://localhost/piwigo/sieve.php")
				.openConnection();
		httpUrlConnection.setDoOutput(true);
		httpUrlConnection.setRequestMethod("POST");

		File myFile = new File("/home/hieu/Downloads/piwigo_thirdParty/Object/animal.jpeg");
		byte[] mybytearray = new byte[(int) myFile.length()];
		FileInputStream fis = new FileInputStream(myFile);
		OutputStream os = httpUrlConnection.getOutputStream();
		BufferedInputStream bis = new BufferedInputStream(fis);
		bis.read(mybytearray, 0, mybytearray.length);

		System.out.println("Sending the file of size:" + mybytearray.length + " bytes");

		os.write(mybytearray, 0, mybytearray.length);

		System.out.println("File sent.");
		BufferedReader in = new BufferedReader(new InputStreamReader(httpUrlConnection.getInputStream()));

		String s = null;
		while ((s = in.readLine()) != null) {
			System.out.println(s);
		}
		os.flush();
		bis.close();
		os.close();
		fis.close();
		in.close();
	}

	public void uploadFile(String filePath, String serverURL) throws IOException {
		String fileName = filePath;
		HttpPost post = new HttpPost(serverURL);
		HttpClient client = new DefaultHttpClient();
		File file = new File(fileName);
		String message = "This is a multipart post";
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		builder.addBinaryBody("upfile", file, ContentType.DEFAULT_BINARY, fileName);
		builder.addTextBody("text", message, ContentType.DEFAULT_BINARY);
		//
		HttpEntity entity = builder.build();
		post.setEntity(entity);
		HttpResponse response = client.execute(post);
		System.out.println(response);
	}

	static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	// Json file

	public void uploadFileJson(String filePath, String serverURL) throws IOException {
		truncatePadding(filePath);
		
//		String jsonContent = readFile("/home/hieu/newTest.json", Charset.defaultCharset());
		cacheDataPointID(filePath);
//		System.out.println(jsonContent);
		try (BufferedReader br = new BufferedReader(new FileReader(new File(filePath)))) {
			String line;
			while ((line = br.readLine()) != null) {
				// process the line.
				HttpClient client = new DefaultHttpClient();
				String host = "192.168.99.100";
				String port = "8083";
				String apiVersion = "1.0.M1";
				String postCreateDataPointAddr = "http://" + host + ":" + port + "/v" + apiVersion + "/dataPoints";
				HttpPost post = new HttpPost(postCreateDataPointAddr);
				String jsonContent = line;
			//	cacheDataPointID("/home/hieu/Downloads/piwigo_thirdParty/Object/newTest2.json");
				System.out.println(jsonContent);
				StringEntity input = new StringEntity(jsonContent);
				post.setHeader("Authorization", "Bearer 25172308-ea89-41f2-b59e-26b52d1f2c2c");
				input.setContentType("application/json");

				post.setEntity(input);

				long startTime = System.nanoTime();
				HttpResponse response = client.execute(post);
				long elapsedTime = System.nanoTime() - startTime;
				System.out.println("Total execution time to create 1000K objects in Java in millis: " + elapsedTime / 1000000);
				BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

				String lineLog = "";

				while ((lineLog = rd.readLine()) != null) {
					System.out.println(lineLog);
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	private void cacheDataPointID(String string) {
		// TODO Auto-generated method stub
		ArrayList<String> listID = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(new File(string)))) {
			String line;
			while ((line = br.readLine()) != null) {
				// process the line.
				JSONParser parser = new JSONParser();
				JSONObject jsonOBJ = (JSONObject) parser.parse(line);
				String id = (String) jsonOBJ.get("id");
				listID.add(id);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("Error parsing with file "+string);
		}

		append(listID);
	}

	private void append(ArrayList<String> listID) {
		ArrayList<String> revokeList = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader(new File(listControl)))) {
			String line;
			for(String s : listID){
				System.out.println("List id is "+s);
			}
			while ((line = br.readLine()) != null) {
				// process the line.
				System.out.println("Line is "+line);
				if (!listID.contains(line))
					revokeList.add(line);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

		File listControlFile = new File(listControl);
		if (listControlFile.exists())
			listControlFile.delete();
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(listControl), true));
			for (String s : listID) {
				bw.write(s);
				bw.newLine();
			}
			bw.close();
		} catch (Exception e) {
		}
		// delete revoke data point
		try {
			deleteRevoke(revokeList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void deleteRevoke(ArrayList<String> revokeList) throws ClientProtocolException, IOException {
		// TODO Auto-generated method stub
		HttpClient client = new DefaultHttpClient();
		String host = "192.168.99.100";
		String port = "8083";
		String apiVersion = "1.0.M1";
		for (String s : revokeList) {
			System.out.println("Revoke access of data point "+s);
			String postCreateDataPointAddr = "http://" + host + ":" + port + "/v" + apiVersion + "/dataPoints/" + s;
			HttpDelete delete = new HttpDelete(postCreateDataPointAddr);
			delete.setHeader("Authorization", "Bearer 25172308-ea89-41f2-b59e-26b52d1f2c2c");

			long startTime = System.nanoTime();
			HttpResponse response = client.execute(delete);
			long elapsedTime = System.nanoTime() - startTime;
			System.out.println(
					"Total execution time to create 1000K objects in Java in millis: " + elapsedTime / 1000000);
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

			String line = "";

			while ((line = rd.readLine()) != null) {
				System.out.println(line);
			}
		}

	}

	private void truncatePadding(String filePath) {
		// TODO Auto-generated method stub
		File truncFile = new File(filePath);
		long fileLength = truncFile.length();
		long actualSize = fileLength;
		try {
			byte[] buffer = Files.readAllBytes(truncFile.toPath());
			for (int i = buffer.length - 1; i > 0; i--) {
				if (buffer[i] == 0)
					actualSize--;
				else
					break;
			}

			FileChannel outChan = new FileOutputStream(truncFile, true).getChannel();
			outChan.truncate(actualSize);
			System.out.println("Actual size is " + actualSize);
			outChan.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
