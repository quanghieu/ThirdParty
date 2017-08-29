import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HttpClientExample {

	private final String USER_AGENT = "Mozilla/5.0";

	public static void main(String[] args) throws Exception {

		HttpClientExample http = new HttpClientExample();

		System.out.println("Testing 1 - Send Http GET request");
		http.sendGet();

		System.out.println("\nTesting 2 - Send Http POST request");
		http.uploadFile("", "");
		

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

		String url = "http://localhost/piwigo/sieve.php";
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("User-Agent", USER_AGENT);
		con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

		String data = URLEncoder.encode("revoke", "UTF-8") + "=" + URLEncoder.encode(updateLocation, "UTF-8");

//		data += "&" + URLEncoder.encode("email", "UTF-8") + "=" + URLEncoder.encode(Email, "UTF-8");
//
//		data += "&" + URLEncoder.encode("user", "UTF-8") + "=" + URLEncoder.encode(Login, "UTF-8");
//
//		data += "&" + URLEncoder.encode("pass", "UTF-8") + "=" + URLEncoder.encode(Pass, "UTF-8");

//		String urlParameters = "sn=C02G8416DRJM&cn=&locale=&caller=&num=12345";

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
	
	public void sendPostFile(String filepath) throws MalformedURLException, IOException{
		HttpURLConnection httpUrlConnection = (HttpURLConnection) new URL("http://localhost/piwigo/sieve.php").openConnection();
        httpUrlConnection.setDoOutput(true);
        httpUrlConnection.setRequestMethod("POST");

        File myFile = new File ("/home/hieu/Downloads/piwigo_thirdParty/Object/animal.jpeg");
        byte [] mybytearray  = new byte [(int)myFile.length()];
        FileInputStream fis = new FileInputStream(myFile);
        OutputStream os = httpUrlConnection.getOutputStream();
        BufferedInputStream bis= new BufferedInputStream(fis);
        bis.read(mybytearray,0,mybytearray.length);

        System.out.println("Sending the file of size:"+ mybytearray.length + " bytes");

        os.write(mybytearray,0,mybytearray.length);

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

}


