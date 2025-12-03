package orimeister.phatnong;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

@Service
public class TrafficViolationService {

    private final Tesseract tesseract;
    private final TrafficViolationExtractor extractor;

    @Autowired
    public TrafficViolationService(TrafficViolationExtractor extractor, Tesseract tesseract) {
        this.extractor = extractor;
        this.tesseract = tesseract;
    }

    private static final String BASE_URL = "https://www.csgt.vn";
    private static final String CAPTCHA_PATH = "/lib/captcha/captcha.class.php";
    private static final String FORM_ENDPOINT = "/?mod=contact&task=tracuu_post&ajax";
    private static final String RESULTS_URL = "https://www.csgt.vn/tra-cuu-phuong-tien-vi-pham.html";
    private static final int MAX_RETRIES = 5;
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    public List<TrafficViolation> getTrafficViolations (String plate, String vehicleType) throws IOException{
        validateVehicleType(vehicleType);
        return callAPI(plate,vehicleType, MAX_RETRIES);
    }

    private void validateVehicleType(String vehicleType) throws IOException {
        if (!vehicleType.matches("[123]")) {
            throw new IOException("Invalid vehicle type. Must be 1, 2, or 3");
        }
    }

    private List<TrafficViolation> callAPI(String plate,String vehicleType, int retries) throws IOException{
        System.out.println("Fetching traffic violations for plate: " + plate);
        CookieStore cookieStore = new BasicCookieStore();

        try(CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build()){
            String captcha = getCaptcha(httpClient);
            String formResponse = postDataForm(httpClient, plate, vehicleType, captcha);

            if ("404".equals(formResponse)) {
                if (retries > 0) {
                    System.out.println("Captcha verification failed: " + captcha +
                            ". Retrying... (" + (MAX_RETRIES - retries + 1) + "/" + MAX_RETRIES + ")");
                    return callAPI(plate, vehicleType, retries - 1);
                } else {
                    throw new IOException("Maximum retry attempts reached. Could not verify captcha.");
                }
            }

            // Wait for server to process results before fetching
            try {
                Thread.sleep(1500); // Wait 1.5 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Request interrupted", e);
            }
            String resultsHtml = getViolationResults(httpClient, plate,vehicleType);
            if (resultsHtml.contains("Không tìm thấy kết quả !")) {
                System.out.println("No violations found for plate: " + plate);
                return new ArrayList<>();
            }

            return extractor.extractTrafficViolations(resultsHtml);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error fetching traffic violations for plate " + plate + ": " + e.getMessage());
            throw new IOException("Failed to fetch traffic violations", e);
        }
    }

    private String getCaptcha(CloseableHttpClient httpClient) throws IOException {
        HttpGet request = new HttpGet(BASE_URL + CAPTCHA_PATH);
        request.setHeader("User-Agent", USER_AGENT);

        byte[] imageBytes = httpClient.execute(request, response ->
                EntityUtils.toByteArray(response.getEntity())
        );


        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));

        //Run OCR
        try {
            String captchaText = tesseract.doOCR(image);
            return captchaText.trim();
        } catch (TesseractException e) {
            throw new IOException("Failed to process captcha: " + e.getMessage(), e);
        }
    }



    private String postDataForm(CloseableHttpClient httpClient, String plate,String vehicleType, String captcha) throws IOException{

        HttpPost request = new HttpPost(BASE_URL + FORM_ENDPOINT);
        request.setHeader("User-Agent", USER_AGENT);
        request.setHeader("Content-Type", "application/x-www-form-urlencoded");
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("BienKS", plate));
        params.add(new BasicNameValuePair("Xe", vehicleType));
        params.add(new BasicNameValuePair("captcha", captcha));
        params.add(new BasicNameValuePair("ipClient", "9.9.9.91"));
        params.add(new BasicNameValuePair("cUrl", "1"));

        request.setEntity(new UrlEncodedFormEntity(params, StandardCharsets.UTF_8));

        return httpClient.execute(request, response -> EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));

    }

    private String getViolationResults(CloseableHttpClient httpClient,String plate, String vehicleType) throws IOException{
        String url = RESULTS_URL + "?&LoaiXe="+vehicleType+"&BienKiemSoat=" + plate;
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", USER_AGENT);
        return httpClient.execute(request, response -> EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
    }


}
